package de.lgstool.geo.expr;

import java.util.ArrayList;
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
            case Expr.Str s -> new Value.TextV(s.value(), null);
            case Expr.Ref r -> resolveRef(r.name());
            case Expr.Unary u -> evalUnary(u);
            case Expr.Bin b -> evalBin(b);
            case Expr.Tuple t -> evalTuple(t);
            case Expr.Call c -> evalCall(c);
            case Expr.ListLit l -> evalListLit(l);
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
            case "i" -> new Value.ComplexV(0, 1);
            case "true", "wahr" -> new Value.BooleanV(true);
            case "false", "falsch" -> new Value.BooleanV(false);
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
            case Value.ComplexV z -> new Value.ComplexV(-z.re(), -z.im());
            case Value.VectorV vec -> new Value.VectorV(-vec.x(), -vec.y(), -vec.z(), vec.is3D());
            case Value.PointV p -> new Value.PointV(-p.x(), -p.y(), -p.z(), p.is3D());
            default -> throw new EvalException("Negation für diesen Operanden nicht definiert.");
        };
    }

    private Value evalBin(Expr.Bin b) {
        char op = b.op();
        if (op == '&' || op == '|') {
            return logic(op, eval(b.left()), eval(b.right()));
        }
        Value l = eval(b.left());
        Value r = eval(b.right());
        if (op == '<' || op == '>' || op == 'l' || op == 'g' || op == 'e' || op == 'n') {
            return compare(op, l, r);
        }
        if (op == '^') {
            return new Value.NumberV(Math.pow(number(l, "Basis"), number(r, "Exponent")));
        }
        if (l instanceof Value.NumberV ln && r instanceof Value.NumberV rn) {
            return new Value.NumberV(arithmetic(op, ln.v(), rn.v()));
        }
        Value complex = tryComplex(op, l, r);
        if (complex != null) {
            return complex;
        }
        Value scaled = tryScale(op, l, r);
        if (scaled != null) {
            return scaled;
        }
        Value matrix = tryMatrix(op, l, r);
        if (matrix != null) {
            return matrix;
        }
        if (op == '+' || op == '-') {
            Value combined = tryCombine(op, l, r);
            if (combined != null) {
                return combined;
            }
        }
        throw new EvalException("Operation '" + op + "' ist für diese Operanden nicht definiert.");
    }

    private static Value compare(char op, Value l, Value r) {
        if (op == 'e' || op == 'n') {
            boolean eq = valuesEqual(l, r);
            return new Value.BooleanV(op == 'e' ? eq : !eq);
        }
        double a = number(l, "Vergleich");
        double b = number(r, "Vergleich");
        boolean result = switch (op) {
            case '<' -> a < b;
            case '>' -> a > b;
            case 'l' -> a <= b;
            case 'g' -> a >= b;
            default -> false;
        };
        return new Value.BooleanV(result);
    }

    private static boolean valuesEqual(Value l, Value r) {
        if (l instanceof Value.NumberV a && r instanceof Value.NumberV b) {
            return Math.abs(a.v() - b.v()) < 1e-9;
        }
        if (l instanceof Value.BooleanV a && r instanceof Value.BooleanV b) {
            return a.value() == b.value();
        }
        return l.equals(r);
    }

    private static Value logic(char op, Value l, Value r) {
        if (!(l instanceof Value.BooleanV a) || !(r instanceof Value.BooleanV b)) {
            throw new EvalException("Logische Operatoren erwarten Wahrheitswerte.");
        }
        return new Value.BooleanV(op == '&' ? a.value() && b.value() : a.value() || b.value());
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

    /** Complex {@code + − * /} (promoting a real operand); returns {@code null} if neither is complex. */
    private static Value tryComplex(char op, Value l, Value r) {
        double[] a = asComplex(l);
        double[] b = asComplex(r);
        if (a == null || b == null || !(l instanceof Value.ComplexV || r instanceof Value.ComplexV)) {
            return null;
        }
        return switch (op) {
            case '+' -> complex(a[0] + b[0], a[1] + b[1]);
            case '-' -> complex(a[0] - b[0], a[1] - b[1]);
            case '*' -> complex(a[0] * b[0] - a[1] * b[1], a[0] * b[1] + a[1] * b[0]);
            case '/' -> {
                double d = b[0] * b[0] + b[1] * b[1];
                if (d == 0) {
                    throw new EvalException("Division durch Null.");
                }
                yield complex((a[0] * b[0] + a[1] * b[1]) / d, (a[1] * b[0] - a[0] * b[1]) / d);
            }
            default -> null;
        };
    }

    private static double[] asComplex(Value v) {
        if (v instanceof Value.NumberV n) {
            return new double[]{n.v(), 0};
        }
        if (v instanceof Value.ComplexV z) {
            return new double[]{z.re(), z.im()};
        }
        return null;
    }

    /** Collapses a complex result to a real number when its imaginary part vanishes. */
    private static Value complex(double re, double im) {
        return Math.abs(im) < 1e-12 ? new Value.NumberV(re) : new Value.ComplexV(re, im);
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

    private Value evalListLit(Expr.ListLit l) {
        if (isMatrixLiteral(l)) {
            return buildMatrix(l);
        }
        return new Value.ListV(l.items().stream().map(this::eval).toList());
    }

    /** A brace literal is a matrix when every item is a non-empty, equal-length brace list. */
    private static boolean isMatrixLiteral(Expr.ListLit l) {
        if (l.items().isEmpty()) {
            return false;
        }
        int cols = -1;
        for (Expr item : l.items()) {
            if (!(item instanceof Expr.ListLit row) || row.items().isEmpty()) {
                return false;
            }
            if (cols == -1) {
                cols = row.items().size();
            } else if (cols != row.items().size()) {
                return false;
            }
        }
        return true;
    }

    private Value buildMatrix(Expr.ListLit l) {
        de.lgstool.math.Fraction[][] m = new de.lgstool.math.Fraction[l.items().size()][];
        for (int i = 0; i < l.items().size(); i++) {
            Expr.ListLit row = (Expr.ListLit) l.items().get(i);
            de.lgstool.math.Fraction[] r = new de.lgstool.math.Fraction[row.items().size()];
            for (int j = 0; j < r.length; j++) {
                r[j] = Rational.eval(row.items().get(j), scope);
            }
            m[i] = r;
        }
        return new Value.MatrixV(m);
    }

    /** Exact matrix {@code + - *} and scalar·matrix; returns {@code null} if not applicable. */
    private static Value tryMatrix(char op, Value l, Value r) {
        if (l instanceof Value.MatrixV a && r instanceof Value.MatrixV b) {
            return switch (op) {
                case '+' -> new Value.MatrixV(MatrixBuiltins.add(a.rows(), b.rows()));
                case '-' -> new Value.MatrixV(MatrixBuiltins.subtract(a.rows(), b.rows()));
                case '*' -> new Value.MatrixV(MatrixBuiltins.multiply(a.rows(), b.rows()));
                default -> null;
            };
        }
        if (op == '*' && l instanceof Value.NumberV n && r instanceof Value.MatrixV b) {
            return new Value.MatrixV(MatrixBuiltins.scale(b.rows(), Rational.fromDouble(n.v())));
        }
        if (op == '*' && l instanceof Value.MatrixV a && r instanceof Value.NumberV n) {
            return new Value.MatrixV(MatrixBuiltins.scale(a.rows(), Rational.fromDouble(n.v())));
        }
        return null;
    }

    private Value evalCall(Expr.Call c) {
        Value lazy = evalLazy(c);
        if (lazy != null) {
            return lazy;
        }
        List<Value> args = c.args().stream().map(this::eval).toList();
        Value fn = scope.resolve(c.name());
        if (fn instanceof Value.FunctionV f) {
            return applyFunction(f, args);
        }
        if (fn != null) {
            throw new EvalException("'" + c.name() + "' ist keine Funktion.");
        }
        if (builtins.has(c.name())) {
            return builtins.call(c.name(), args, this::applyFunction);
        }
        throw new EvalException("Unbekannte Funktion '" + c.name() + "'.");
    }

    /**
     * Functions whose arguments must NOT be eagerly evaluated: {@code If} (short-circuit
     * branches) and the higher-order list builders that bind a variable over an
     * expression ({@code Sequence(expr,var,a,b)}, {@code Map}, {@code KeepIf},
     * {@code CountIf}, {@code Filter}, {@code Zip}). Returns {@code null} if {@code c} is
     * not one of these (so it falls through to the normal eager call path).
     */
    private Value evalLazy(Expr.Call c) {
        List<Expr> a = c.args();
        return switch (c.name().toLowerCase(Locale.ROOT)) {
            case "if", "wenn" -> ifThenElse(a);
            case "sequence", "folge" -> a.size() >= 4 ? sequenceExpr(a) : null;
            case "map" -> mapExpr(a);
            case "keepif", "filter" -> keepIf(a);
            case "countif" -> countIf(a);
            case "zip" -> zip(a);
            case "solve", "lös", "loese" -> solve(a);
            case "substitute", "subst" -> substitute(a);
            case "expand", "expandiere" -> polyFunction(a, "Expand");
            case "simplify", "vereinfache" -> polyFunction(a, "Simplify");
            case "factor", "faktorisiere" -> factor(a);
            default -> null;
        };
    }

    private Value ifThenElse(List<Expr> a) {
        if (a.size() < 2 || a.size() > 3) {
            throw new EvalException("If erwartet (Bedingung, dann [, sonst]).");
        }
        if (!(eval(a.get(0)) instanceof Value.BooleanV cond)) {
            throw new EvalException("If: erste Eingabe muss ein Wahrheitswert sein.");
        }
        if (cond.value()) {
            return eval(a.get(1));
        }
        return a.size() == 3 ? eval(a.get(2)) : new Value.NumberV(Double.NaN);
    }

    private Value sequenceExpr(List<Expr> a) {
        String var = refName(a.get(1), "Sequence");
        double from = evalNumber(a.get(2));
        double to = evalNumber(a.get(3));
        double step = a.size() >= 5 ? evalNumber(a.get(4)) : 1;
        if (step == 0) {
            throw new EvalException("Sequence: Schritt darf nicht 0 sein.");
        }
        List<Value> out = new ArrayList<>();
        if (step > 0) {
            for (double k = from; k <= to + 1e-9; k += step) {
                out.add(withVar(var, new Value.NumberV(k), a.get(0)));
            }
        } else {
            for (double k = from; k >= to - 1e-9; k += step) {
                out.add(withVar(var, new Value.NumberV(k), a.get(0)));
            }
        }
        return new Value.ListV(List.copyOf(out));
    }

    private Value mapExpr(List<Expr> a) {
        if (a.size() != 3) {
            throw new EvalException("Map erwartet (Ausdruck, Variable, Liste).");
        }
        String var = refName(a.get(1), "Map");
        List<Value> out = new ArrayList<>();
        for (Value item : listItems(a.get(2), "Map")) {
            out.add(withVar(var, item, a.get(0)));
        }
        return new Value.ListV(List.copyOf(out));
    }

    private Value keepIf(List<Expr> a) {
        if (a.size() != 3) {
            throw new EvalException("KeepIf erwartet (Bedingung, Variable, Liste).");
        }
        String var = refName(a.get(1), "KeepIf");
        List<Value> out = new ArrayList<>();
        for (Value item : listItems(a.get(2), "KeepIf")) {
            if (truth(withVar(var, item, a.get(0)))) {
                out.add(item);
            }
        }
        return new Value.ListV(List.copyOf(out));
    }

    private Value countIf(List<Expr> a) {
        if (a.size() != 3) {
            throw new EvalException("CountIf erwartet (Bedingung, Variable, Liste).");
        }
        String var = refName(a.get(1), "CountIf");
        int n = 0;
        for (Value item : listItems(a.get(2), "CountIf")) {
            if (truth(withVar(var, item, a.get(0)))) {
                n++;
            }
        }
        return new Value.NumberV(n);
    }

    private Value zip(List<Expr> a) {
        if (a.size() != 5) {
            throw new EvalException("Zip erwartet (Ausdruck, Var1, Liste1, Var2, Liste2).");
        }
        String v1 = refName(a.get(1), "Zip");
        String v2 = refName(a.get(3), "Zip");
        List<Value> l1 = listItems(a.get(2), "Zip");
        List<Value> l2 = listItems(a.get(4), "Zip");
        int n = Math.min(l1.size(), l2.size());
        List<Value> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Value item1 = l1.get(i);
            Value item2 = l2.get(i);
            NameResolver child = name -> v1.equals(name) ? item1 : v2.equals(name) ? item2 : scope.resolve(name);
            out.add(new Evaluator(child, builtins).eval(a.get(0)));
        }
        return new Value.ListV(List.copyOf(out));
    }

    // ---- CAS-lite (lazy: operate on the raw expression) ------------------

    /**
     * {@code Solve(expr)} or {@code Solve(expr = rhs)}: returns the real solutions (a list)
     * in x or y. Falls through (returns {@code null}) for {@code Solve(matrix)}, which the
     * matrix built-ins handle. Polynomial equations use exact-ish root formulas; otherwise
     * a numeric scan is used.
     */
    private Value solve(List<Expr> a) {
        if (a.size() != 1) {
            return null;
        }
        Expr arg = a.get(0);
        boolean equation = arg instanceof Expr.Bin bin && bin.op() == 'e';
        java.util.Set<String> names = new java.util.HashSet<>();
        Expr.collectNames(arg, names);
        boolean usesPlotVar = names.contains("x") || names.contains("y");
        if (!equation && !usesPlotVar) {
            return null;   // e.g. Solve(M) — let the matrix built-ins handle it
        }
        String var = names.contains("x") ? "x" : "y";
        Expr diff = equation
                ? new Expr.Bin('-', ((Expr.Bin) arg).left(), ((Expr.Bin) arg).right())
                : arg;
        List<Double> roots;
        try {
            roots = Polynomials.roots(Polynomials.toPoly(diff, var));
        } catch (EvalException notPoly) {
            roots = numericRoots(diff, var);
        }
        return new Value.ListV(roots.stream().<Value>map(Value.NumberV::new).toList());
    }

    /** Numeric root scan for a non-polynomial single-variable equation. */
    private List<Double> numericRoots(Expr diff, String var) {
        List<Double> out = new ArrayList<>();
        double lo = -100;
        double hi = 100;
        int steps = 20000;
        double step = (hi - lo) / steps;
        double off = step * 0.5;   // cell centres so integer roots aren't missed
        double prevX = lo + off;
        double prevY = sampleAt(diff, var, prevX);
        for (int i = 1; i < steps; i++) {
            double x = lo + off + i * step;
            double y = sampleAt(diff, var, x);
            if (Double.isFinite(prevY) && Double.isFinite(y) && prevY * y < 0) {
                double a = prevX;
                double b = x;
                for (int it = 0; it < 80; it++) {
                    double m = (a + b) / 2;
                    double fm = sampleAt(diff, var, m);
                    if (sampleAt(diff, var, a) * fm <= 0) {
                        b = m;
                    } else {
                        a = m;
                    }
                }
                double r = (a + b) / 2;
                if (out.stream().noneMatch(e -> Math.abs(e - r) < 1e-5)) {
                    out.add(r);
                }
            }
            prevX = x;
            prevY = y;
        }
        return out;
    }

    private double sampleAt(Expr diff, String var, double x) {
        try {
            return withVar(var, new Value.NumberV(x), diff) instanceof Value.NumberV n ? n.v() : Double.NaN;
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    /** {@code Substitute(expr, var, value)} → expr with {@code var} replaced by {@code value}. */
    private Value substitute(List<Expr> a) {
        if (a.size() != 3) {
            throw new EvalException("Substitute erwartet (Ausdruck, Variable, Wert).");
        }
        return withVar(refName(a.get(1), "Substitute"), eval(a.get(2)), a.get(0));
    }

    /** {@code Expand}/{@code Simplify}: canonical polynomial form returned as a function f(x). */
    private Value polyFunction(List<Expr> a, String where) {
        if (a.size() != 1) {
            throw new EvalException(where + " erwartet einen Ausdruck.");
        }
        String var = expressionVar(a.get(0));
        double[] coeffs = Polynomials.toPoly(a.get(0), var);
        return new Value.FunctionV(List.of(var), Polynomials.toExpr(coeffs, var));
    }

    /** {@code Factor}: factored form (over found real roots) returned as a function f(x). */
    private Value factor(List<Expr> a) {
        if (a.size() != 1) {
            throw new EvalException("Factor erwartet einen Ausdruck.");
        }
        String var = expressionVar(a.get(0));
        double[] coeffs = Polynomials.toPoly(a.get(0), var);
        return new Value.FunctionV(List.of(var), Polynomials.factor(coeffs, var));
    }

    private static String expressionVar(Expr e) {
        java.util.Set<String> names = new java.util.HashSet<>();
        Expr.collectNames(e, names);
        return names.contains("y") && !names.contains("x") ? "y" : "x";
    }

    private Value withVar(String var, Value val, Expr body) {
        NameResolver child = name -> var.equals(name) ? val : scope.resolve(name);
        return new Evaluator(child, builtins).eval(body);
    }

    private List<Value> listItems(Expr e, String where) {
        if (!(eval(e) instanceof Value.ListV l)) {
            throw new EvalException(where + ": erwartet eine Liste.");
        }
        return l.items();
    }

    private static String refName(Expr e, String where) {
        if (!(e instanceof Expr.Ref r)) {
            throw new EvalException(where + ": Variablenname erwartet.");
        }
        return r.name();
    }

    private static boolean truth(Value v) {
        return v instanceof Value.BooleanV b && b.value();
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
