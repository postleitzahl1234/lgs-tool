package de.lgstool.geo.expr;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Symbolic antiderivative of an {@link Expr} with respect to a single variable. Any
 * polynomial integrates exactly; beyond that the standard forms with a <em>linear</em>
 * inner argument are handled (sin, cos, exp, 1/x, power rule). Unsupported integrands
 * raise an {@link EvalException} so the caller can report it.
 */
public final class SymbolicIntegrator {

    private SymbolicIntegrator() {
    }

    public static Expr integrate(Expr e, String var) {
        try {
            return Polynomials.toExpr(integratePoly(Polynomials.toPoly(e, var)), var);
        } catch (EvalException notPolynomial) {
            return nonPoly(e, var);
        }
    }

    /** ∫(Σ cᵢ xⁱ) dx = Σ cᵢ/(i+1) xⁱ⁺¹. */
    private static double[] integratePoly(double[] c) {
        double[] r = new double[c.length + 1];
        for (int i = 0; i < c.length; i++) {
            r[i + 1] = c[i] / (i + 1);
        }
        return r;
    }

    private static Expr nonPoly(Expr e, String var) {
        return switch (e) {
            case Expr.Unary u when u.op() == '-' -> new Expr.Unary('-', integrate(u.operand(), var));
            case Expr.Bin b -> bin(b, var);
            case Expr.Call c -> call(c, var);
            default -> throw unsupported();
        };
    }

    private static Expr bin(Expr.Bin b, String var) {
        return switch (b.op()) {
            case '+' -> new Expr.Bin('+', integrate(b.left(), var), integrate(b.right(), var));
            case '-' -> new Expr.Bin('-', integrate(b.left(), var), integrate(b.right(), var));
            case '*' -> product(b.left(), b.right(), var);
            case '/' -> quotient(b.left(), b.right(), var);
            case '^' -> powerRule(b.left(), b.right(), var);
            default -> throw unsupported();
        };
    }

    private static Expr product(Expr l, Expr r, String var) {
        if (!dependsOn(l, var)) {
            return new Expr.Bin('*', l, integrate(r, var));
        }
        if (!dependsOn(r, var)) {
            return new Expr.Bin('*', r, integrate(l, var));
        }
        throw unsupported();
    }

    private static Expr quotient(Expr l, Expr r, String var) {
        if (!dependsOn(r, var)) {
            return new Expr.Bin('/', integrate(l, var), r);
        }
        if (l instanceof Expr.Num n && n.value() == 1 && r instanceof Expr.Ref ref && ref.name().equals(var)) {
            return lnAbs(new Expr.Ref(var));   // ∫ 1/x dx = ln|x|
        }
        throw unsupported();
    }

    private static Expr powerRule(Expr base, Expr exp, String var) {
        if (!(base instanceof Expr.Ref ref) || !ref.name().equals(var) || !(exp instanceof Expr.Num n)) {
            throw unsupported();
        }
        double k = n.value();
        if (Math.abs(k + 1) < 1e-12) {
            return lnAbs(new Expr.Ref(var));   // ∫ x⁻¹ dx = ln|x|
        }
        // ∫ xᵏ dx = x^(k+1)/(k+1)
        return new Expr.Bin('/', new Expr.Bin('^', new Expr.Ref(var), new Expr.Num(k + 1)), new Expr.Num(k + 1));
    }

    /** Standard functions with a linear inner argument a·x + b: divide by a. */
    private static Expr call(Expr.Call c, String var) {
        if (c.args().size() != 1) {
            throw unsupported();
        }
        Expr g = c.args().get(0);
        double a = linearSlope(g, var);
        Expr base = switch (c.name().toLowerCase(Locale.ROOT)) {
            case "sin" -> new Expr.Unary('-', new Expr.Call("cos", List.of(g)));
            case "cos" -> new Expr.Call("sin", List.of(g));
            case "exp" -> new Expr.Call("exp", List.of(g));
            default -> throw unsupported();
        };
        return Math.abs(a - 1) < 1e-12 ? base : new Expr.Bin('/', base, new Expr.Num(a));
    }

    /** The slope a of a strictly linear argument a·x + b (throws if not linear or a = 0). */
    private static double linearSlope(Expr g, String var) {
        double[] c = Polynomials.toPoly(g, var);   // throws if not polynomial
        if (c.length > 2) {
            throw unsupported();
        }
        double a = c.length == 2 ? c[1] : 0;
        if (Math.abs(a) < 1e-12) {
            throw unsupported();
        }
        return a;
    }

    private static Expr lnAbs(Expr inner) {
        return new Expr.Call("ln", List.of(new Expr.Call("abs", List.of(inner))));
    }

    private static boolean dependsOn(Expr e, String var) {
        Set<String> names = new HashSet<>();
        Expr.collectNames(e, names);
        return names.contains(var);
    }

    private static EvalException unsupported() {
        return new EvalException("Stammfunktion: dieser Integrand wird (noch) nicht unterstützt.");
    }
}
