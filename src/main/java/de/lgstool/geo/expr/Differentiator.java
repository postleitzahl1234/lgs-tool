package de.lgstool.geo.expr;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Symbolic differentiation of an {@link Expr} with respect to a single variable.
 * Handles arithmetic (sum, product, quotient, power) and the standard maths functions
 * via the chain rule. Light algebraic simplification keeps the result compact.
 */
public final class Differentiator {

    private Differentiator() {
    }

    public static Expr differentiate(Expr e, String var) {
        return switch (e) {
            case Expr.Num ignored -> num(0);
            case Expr.Str ignored -> throw new EvalException("Ableitung von Text ist nicht definiert.");
            case Expr.Ref r -> num(r.name().equals(var) ? 1 : 0);
            case Expr.Unary u -> u.op() == '-' ? neg(differentiate(u.operand(), var)) : differentiate(u.operand(), var);
            case Expr.Bin b -> bin(b, var);
            case Expr.Call c -> call(c, var);
            case Expr.Tuple ignored -> throw new EvalException("Ableitung eines Punktes ist nicht definiert.");
            case Expr.ListLit ignored -> throw new EvalException("Ableitung einer Liste/Matrix ist nicht definiert.");
        };
    }

    private static Expr bin(Expr.Bin b, String var) {
        Expr u = b.left();
        Expr v = b.right();
        Expr du = differentiate(u, var);
        Expr dv = differentiate(v, var);
        return switch (b.op()) {
            case '+' -> add(du, dv);
            case '-' -> sub(du, dv);
            case '*' -> add(mul(du, v), mul(u, dv));
            case '/' -> div(sub(mul(du, v), mul(u, dv)), mul(v, v));
            case '^' -> power(u, v, du, dv, var);
            default -> throw new EvalException("Ableitung von '" + b.op() + "' nicht unterstützt.");
        };
    }

    private static Expr power(Expr u, Expr v, Expr du, Expr dv, String var) {
        boolean expHasVar = dependsOn(v, var);
        boolean baseHasVar = dependsOn(u, var);
        if (!expHasVar) {
            // d(u^c) = c·u^(c-1)·u'
            return mul(mul(v, pow(u, sub(v, num(1)))), du);
        }
        if (!baseHasVar) {
            // d(c^v) = c^v·ln(c)·v'
            return mul(mul(pow(u, v), call("ln", u)), dv);
        }
        // d(u^v) = u^v·(v'·ln(u) + v·u'/u)
        return mul(pow(u, v), add(mul(dv, call("ln", u)), div(mul(v, du), u)));
    }

    private static Expr call(Expr.Call c, String var) {
        if (c.args().size() != 1) {
            throw new EvalException("Ableitung von '" + c.name() + "' nicht unterstützt.");
        }
        Expr g = c.args().get(0);
        Expr dg = differentiate(g, var);
        Expr outer = switch (c.name().toLowerCase(Locale.ROOT)) {
            case "sin" -> call("cos", g);
            case "cos" -> neg(call("sin", g));
            case "tan" -> div(num(1), pow(call("cos", g), num(2)));
            case "exp" -> call("exp", g);
            case "ln" -> div(num(1), g);
            case "log" -> div(num(1), mul(g, call("ln", num(10))));
            case "sqrt" -> div(num(1), mul(num(2), call("sqrt", g)));
            case "asin" -> div(num(1), call("sqrt", sub(num(1), mul(g, g))));
            case "acos" -> neg(div(num(1), call("sqrt", sub(num(1), mul(g, g)))));
            case "atan" -> div(num(1), add(num(1), mul(g, g)));
            case "abs" -> call("sign", g);
            default -> throw new EvalException("Ableitung von '" + c.name() + "' nicht unterstützt.");
        };
        return mul(outer, dg);
    }

    // ---- simplifying constructors ----------------------------------------

    private static Expr num(double v) {
        return new Expr.Num(v);
    }

    private static boolean isNum(Expr e, double v) {
        return e instanceof Expr.Num n && n.value() == v;
    }

    private static Expr neg(Expr e) {
        if (isNum(e, 0)) {
            return num(0);
        }
        return new Expr.Unary('-', e);
    }

    private static Expr add(Expr a, Expr b) {
        if (isNum(a, 0)) {
            return b;
        }
        if (isNum(b, 0)) {
            return a;
        }
        return new Expr.Bin('+', a, b);
    }

    private static Expr sub(Expr a, Expr b) {
        if (isNum(b, 0)) {
            return a;
        }
        if (isNum(a, 0)) {
            return neg(b);
        }
        return new Expr.Bin('-', a, b);
    }

    private static Expr mul(Expr a, Expr b) {
        if (isNum(a, 0) || isNum(b, 0)) {
            return num(0);
        }
        if (isNum(a, 1)) {
            return b;
        }
        if (isNum(b, 1)) {
            return a;
        }
        return new Expr.Bin('*', a, b);
    }

    private static Expr div(Expr a, Expr b) {
        if (isNum(a, 0)) {
            return num(0);
        }
        if (isNum(b, 1)) {
            return a;
        }
        return new Expr.Bin('/', a, b);
    }

    private static Expr pow(Expr a, Expr b) {
        if (isNum(b, 1)) {
            return a;
        }
        if (isNum(b, 0)) {
            return num(1);
        }
        return new Expr.Bin('^', a, b);
    }

    private static Expr call(String name, Expr arg) {
        return new Expr.Call(name, List.of(arg));
    }

    private static boolean dependsOn(Expr e, String var) {
        Set<String> names = new HashSet<>();
        Expr.collectNames(e, names);
        return names.contains(var);
    }
}
