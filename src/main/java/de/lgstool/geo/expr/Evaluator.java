package de.lgstool.geo.expr;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Evaluates an {@link Expr} against a {@link NameResolver} (named objects) and a
 * {@link BuiltinFunctions} registry. Numbers are {@code double}; tuples evaluate to
 * {@link Value.PointV}; arithmetic also supports point/vector combinations.
 */
public final class Evaluator {

    private final NameResolver scope;
    private final BuiltinFunctions builtins;

    public Evaluator(NameResolver scope, BuiltinFunctions builtins) {
        this.scope = scope;
        this.builtins = builtins;
    }

    public Value eval(Expr e) {
        return switch (e) {
            case Expr.Num n -> new Value.NumberV(n.value());
            case Expr.Ref r -> resolveRef(r.name());
            case Expr.Unary u -> evalUnary(u);
            case Expr.Bin b -> evalBin(b);
            case Expr.Tuple t -> evalTuple(t);
            case Expr.Call c -> evalCall(c);
        };
    }

    /** Convenience: evaluate to a number or fail with a clear error. */
    public double evalNumber(Expr e) {
        return number(eval(e), "Ausdruck");
    }

    private Value resolveRef(String name) {
        Value v = scope.resolve(name);
        if (v != null) {
            return v;
        }
        Value c = constant(name);
        if (c != null) {
            return c;
        }
        throw new EvalException("Unbekanntes Objekt '" + name + "'.");
    }

    private static Value constant(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "pi" -> new Value.NumberV(Math.PI);
            case "tau" -> new Value.NumberV(2 * Math.PI);
            case "e" -> new Value.NumberV(Math.E);
            default -> null;
        };
    }

    private Value evalUnary(Expr.Unary u) {
        Value v = eval(u.operand());
        if (u.op() == '+') {
            return v;
        }
        return switch (v) {
            case Value.NumberV n -> new Value.NumberV(-n.v());
            case Value.VectorV vec -> new Value.VectorV(-vec.x(), -vec.y(), -vec.z(), vec.is3D());
            case Value.PointV p -> new Value.PointV(-p.x(), -p.y(), -p.z(), p.is3D());
            default -> throw new EvalException("Negation für diesen Operanden nicht definiert.");
        };
    }

    private Value evalBin(Expr.Bin b) {
        Value l = eval(b.left());
        Value r = eval(b.right());
        char op = b.op();
        if (op == '^') {
            return new Value.NumberV(Math.pow(number(l, "Basis"), number(r, "Exponent")));
        }
        if (l instanceof Value.NumberV ln && r instanceof Value.NumberV rn) {
            return new Value.NumberV(arithmetic(op, ln.v(), rn.v()));
        }
        Value scaled = tryScale(op, l, r);
        if (scaled != null) {
            return scaled;
        }
        if (op == '+' || op == '-') {
            Value combined = tryCombine(op, l, r);
            if (combined != null) {
                return combined;
            }
        }
        throw new EvalException("Operation '" + op + "' ist für diese Operanden nicht definiert.");
    }

    private static double arithmetic(char op, double a, double b) {
        return switch (op) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> {
                if (b == 0) {
                    throw new EvalException("Division durch Null.");
                }
                yield a / b;
            }
            default -> throw new EvalException("Unbekannter Operator '" + op + "'.");
        };
    }

    /** Scalar · point/vector and point/vector ÷ scalar. */
    private static Value tryScale(char op, Value l, Value r) {
        if (op == '*' && l instanceof Value.NumberV n && isTuple(r)) {
            return scale(r, n.v());
        }
        if (op == '*' && isTuple(l) && r instanceof Value.NumberV n) {
            return scale(l, n.v());
        }
        if (op == '/' && isTuple(l) && r instanceof Value.NumberV n) {
            if (n.v() == 0) {
                throw new EvalException("Division durch Null.");
            }
            return scale(l, 1 / n.v());
        }
        return null;
    }

    /** Componentwise +/- of points and vectors, following GeoGebra's type rules. */
    private static Value tryCombine(char op, Value l, Value r) {
        if (!isTuple(l) || !isTuple(r)) {
            return null;
        }
        double[] a = xyz(l);
        double[] b = xyz(r);
        double sign = op == '-' ? -1 : 1;
        double x = a[0] + sign * b[0];
        double y = a[1] + sign * b[1];
        double z = a[2] + sign * b[2];
        boolean is3D = is3D(l) || is3D(r);
        boolean pointMinusPoint = op == '-' && l instanceof Value.PointV && r instanceof Value.PointV;
        boolean anyPoint = l instanceof Value.PointV || r instanceof Value.PointV;
        if (pointMinusPoint || !anyPoint) {
            return new Value.VectorV(x, y, z, is3D);
        }
        return new Value.PointV(x, y, z, is3D);
    }

    private Value evalTuple(Expr.Tuple t) {
        List<Expr> comps = t.components();
        if (comps.size() == 2) {
            return Value.PointV.of2D(evalNumber(comps.get(0)), evalNumber(comps.get(1)));
        }
        if (comps.size() == 3) {
            return Value.PointV.of3D(evalNumber(comps.get(0)), evalNumber(comps.get(1)), evalNumber(comps.get(2)));
        }
        throw new EvalException("Ein Punkt braucht 2 oder 3 Koordinaten, erhalten " + comps.size() + ".");
    }

    private Value evalCall(Expr.Call c) {
        List<Value> args = c.args().stream().map(this::eval).toList();
        Value fn = scope.resolve(c.name());
        if (fn instanceof Value.FunctionV f) {
            return applyFunction(f, args);
        }
        if (fn != null) {
            throw new EvalException("'" + c.name() + "' ist keine Funktion.");
        }
        if (builtins.has(c.name())) {
            return builtins.call(c.name(), args);
        }
        throw new EvalException("Unbekannte Funktion '" + c.name() + "'.");
    }

    private Value applyFunction(Value.FunctionV f, List<Value> args) {
        if (args.size() != f.params().size()) {
            throw new EvalException("Funktion erwartet " + f.params().size() + " Argument(e), erhalten " + args.size() + ".");
        }
        Map<String, Value> bound = new HashMap<>();
        for (int i = 0; i < args.size(); i++) {
            bound.put(f.params().get(i), args.get(i));
        }
        NameResolver child = name -> bound.containsKey(name) ? bound.get(name) : scope.resolve(name);
        return new Evaluator(child, builtins).eval(f.body());
    }

    private static boolean isTuple(Value v) {
        return v instanceof Value.PointV || v instanceof Value.VectorV;
    }

    private static double[] xyz(Value v) {
        if (v instanceof Value.PointV p) {
            return new double[]{p.x(), p.y(), p.z()};
        }
        Value.VectorV vec = (Value.VectorV) v;
        return new double[]{vec.x(), vec.y(), vec.z()};
    }

    private static boolean is3D(Value v) {
        return v instanceof Value.PointV p ? p.is3D() : ((Value.VectorV) v).is3D();
    }

    private static Value scale(Value v, double k) {
        double[] c = xyz(v);
        if (v instanceof Value.PointV p) {
            return new Value.PointV(c[0] * k, c[1] * k, c[2] * k, p.is3D());
        }
        Value.VectorV vec = (Value.VectorV) v;
        return new Value.VectorV(c[0] * k, c[1] * k, c[2] * k, vec.is3D());
    }

    private static double number(Value v, String what) {
        if (v instanceof Value.NumberV n) {
            return n.v();
        }
        throw new EvalException(what + " muss eine Zahl sein.");
    }
}
