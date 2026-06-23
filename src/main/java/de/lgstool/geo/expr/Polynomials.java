package de.lgstool.geo.expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Univariate-polynomial CAS helpers (variable {@code x} by default): parse an
 * {@link Expr} into a coefficient array, rebuild a canonical expression (Expand/Simplify),
 * find real roots (Solve), and factor over found real roots (Factor). Coefficients are
 * {@code double}s indexed by power: {@code c[i]} is the coefficient of {@code x^i}.
 *
 * <p>Non-polynomial expressions (trig, division by a variable, unknown symbols) raise an
 * {@link EvalException}; callers fall back to numeric methods where appropriate.
 */
public final class Polynomials {

    private static final double EPS = 1e-9;

    private Polynomials() {
    }

    /** Parses {@code e} as a polynomial in {@code var}; throws if it is not polynomial. */
    public static double[] toPoly(Expr e, String var) {
        return switch (e) {
            case Expr.Num n -> new double[]{n.value()};
            case Expr.Ref r -> ref(r, var);
            case Expr.Unary u -> u.op() == '-' ? scale(toPoly(u.operand(), var), -1) : toPoly(u.operand(), var);
            case Expr.Bin b -> bin(b, var);
            default -> throw notPoly();
        };
    }

    private static double[] ref(Expr.Ref r, String var) {
        if (r.name().equals(var)) {
            return new double[]{0, 1};
        }
        return switch (r.name().toLowerCase(Locale.ROOT)) {
            case "pi" -> new double[]{Math.PI};
            case "tau" -> new double[]{2 * Math.PI};
            case "e" -> new double[]{Math.E};
            default -> throw notPoly();
        };
    }

    private static double[] bin(Expr.Bin b, String var) {
        return switch (b.op()) {
            case '+' -> add(toPoly(b.left(), var), toPoly(b.right(), var));
            case '-' -> add(toPoly(b.left(), var), scale(toPoly(b.right(), var), -1));
            case '*' -> multiply(toPoly(b.left(), var), toPoly(b.right(), var));
            case '/' -> divideByConstant(toPoly(b.left(), var), toPoly(b.right(), var));
            case '^' -> power(toPoly(b.left(), var), b.right());
            default -> throw notPoly();
        };
    }

    // ---- coefficient arithmetic ------------------------------------------

    private static double[] add(double[] a, double[] b) {
        double[] r = new double[Math.max(a.length, b.length)];
        for (int i = 0; i < a.length; i++) {
            r[i] += a[i];
        }
        for (int i = 0; i < b.length; i++) {
            r[i] += b[i];
        }
        return r;
    }

    private static double[] scale(double[] a, double k) {
        double[] r = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = a[i] * k;
        }
        return r;
    }

    private static double[] multiply(double[] a, double[] b) {
        double[] r = new double[a.length + b.length - 1];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < b.length; j++) {
                r[i + j] += a[i] * b[j];
            }
        }
        return r;
    }

    private static double[] divideByConstant(double[] a, double[] b) {
        double[] tb = trim(b);
        if (tb.length != 1) {
            throw notPoly();   // division by a non-constant is not polynomial
        }
        if (Math.abs(tb[0]) < EPS) {
            throw new EvalException("Division durch Null.");
        }
        return scale(a, 1 / tb[0]);
    }

    private static double[] power(double[] base, Expr exp) {
        if (!(exp instanceof Expr.Num n) || n.value() != Math.rint(n.value()) || n.value() < 0) {
            throw notPoly();
        }
        double[] r = {1};
        for (int i = 0; i < (int) n.value(); i++) {
            r = multiply(r, base);
        }
        return r;
    }

    private static double[] trim(double[] c) {
        int deg = c.length - 1;
        while (deg > 0 && Math.abs(c[deg]) < EPS) {
            deg--;
        }
        double[] r = new double[deg + 1];
        System.arraycopy(c, 0, r, 0, deg + 1);
        return r;
    }

    // ---- expression rebuild (Expand / Simplify) --------------------------

    /** Builds a canonical {@code c0 + c1·x + c2·x^2 + …} expression from coefficients. */
    public static Expr toExpr(double[] coeffs, String var) {
        double[] c = trim(coeffs);
        Expr sum = null;
        for (int i = 0; i < c.length; i++) {
            if (Math.abs(c[i]) < EPS) {
                continue;
            }
            Expr term = monomial(c[i], i, var);
            sum = sum == null ? term : new Expr.Bin('+', sum, term);
        }
        return sum == null ? new Expr.Num(0) : sum;
    }

    private static Expr monomial(double coeff, int power, String var) {
        if (power == 0) {
            return new Expr.Num(coeff);
        }
        Expr xPow = power == 1 ? new Expr.Ref(var) : new Expr.Bin('^', new Expr.Ref(var), new Expr.Num(power));
        if (Math.abs(coeff - 1) < EPS) {
            return xPow;
        }
        return new Expr.Bin('*', new Expr.Num(coeff), xPow);
    }

    // ---- roots (Solve) ---------------------------------------------------

    /** Real roots of the polynomial (analytic for degree ≤ 2, numeric above), sorted. */
    public static List<Double> roots(double[] coeffs) {
        double[] c = trim(coeffs);
        int deg = c.length - 1;
        List<Double> out = new ArrayList<>();
        if (deg <= 0) {
            return out;   // constant: no (or all) solutions
        }
        if (deg == 1) {
            out.add(-c[0] / c[1]);
            return out;
        }
        if (deg == 2) {
            double a = c[2];
            double b = c[1];
            double disc = b * b - 4 * a * c[0];
            if (disc < -EPS) {
                return out;
            }
            double sq = Math.sqrt(Math.max(0, disc));
            out.add((-b - sq) / (2 * a));
            if (sq > EPS) {
                out.add((-b + sq) / (2 * a));
            }
            out.sort(Double::compare);
            return out;
        }
        return numericRoots(c);
    }

    /** Sign-change scan + bisection over a wide window; returns distinct real roots. */
    private static List<Double> numericRoots(double[] c) {
        List<Double> out = new ArrayList<>();
        double lo = -1000;
        double hi = 1000;
        int steps = 40000;
        double step = (hi - lo) / steps;
        double prevX = lo;
        double prevY = evalPoly(c, lo);
        for (int i = 1; i <= steps; i++) {
            double x = lo + i * step;
            double y = evalPoly(c, x);
            if (prevY == 0) {
                addDistinct(out, prevX);
            } else if (prevY * y < 0) {
                addDistinct(out, bisect(c, prevX, x));
            }
            prevX = x;
            prevY = y;
        }
        return out;
    }

    private static double bisect(double[] c, double lo, double hi) {
        double flo = evalPoly(c, lo);
        for (int i = 0; i < 100; i++) {
            double mid = (lo + hi) / 2;
            double fmid = evalPoly(c, mid);
            if (Math.abs(fmid) < 1e-12) {
                return mid;
            }
            if (flo * fmid < 0) {
                hi = mid;
            } else {
                lo = mid;
                flo = fmid;
            }
        }
        return (lo + hi) / 2;
    }

    private static void addDistinct(List<Double> out, double r) {
        for (double e : out) {
            if (Math.abs(e - r) < 1e-6) {
                return;
            }
        }
        out.add(r);
    }

    public static double evalPoly(double[] c, double x) {
        double r = 0;
        for (int i = c.length - 1; i >= 0; i--) {
            r = r * x + c[i];
        }
        return r;
    }

    // ---- factoring -------------------------------------------------------

    /**
     * Factors over found real roots: {@code lead·(x − r1)·(x − r2)·…·(remaining)}.
     * Roots close to a simple rational are rounded; any non-real-rootable remainder is
     * kept as a polynomial factor. Approximate — a full CAS factorer is out of scope.
     */
    public static Expr factor(double[] coeffs, String var) {
        double[] c = trim(coeffs);
        int deg = c.length - 1;
        if (deg <= 1) {
            return toExpr(c, var);
        }
        double lead = c[deg];
        double[] work = scale(c, 1 / lead);   // monic
        List<Double> factors = new ArrayList<>();
        for (List<Double> r = roots(work); !r.isEmpty() && work.length > 2; r = roots(work)) {
            double root = round(r.get(0));
            if (Math.abs(evalPoly(work, root)) > 1e-6) {
                break;   // not an exact enough root to divide out cleanly
            }
            work = deflate(work, root);
            factors.add(root);
        }
        Expr product = factors.isEmpty() ? null : null;
        for (double root : factors) {
            Expr fac = new Expr.Bin('-', new Expr.Ref(var), new Expr.Num(root));
            product = product == null ? fac : new Expr.Bin('*', product, fac);
        }
        Expr remainder = trim(work).length > 1 ? toExpr(work, var) : null;
        Expr body = product;
        if (remainder != null) {
            body = body == null ? remainder : new Expr.Bin('*', body, group(remainder));
        }
        if (body == null) {
            return toExpr(c, var);
        }
        if (Math.abs(lead - 1) > EPS) {
            body = new Expr.Bin('*', new Expr.Num(lead), group(body));
        }
        return body;
    }

    private static Expr group(Expr e) {
        return e;   // grouping is handled visually by the LaTeX renderer
    }

    /** Synthetic division of a monic polynomial by (x − root). */
    private static double[] deflate(double[] c, double root) {
        int n = c.length - 1;
        double[] q = new double[n];
        double carry = c[n];
        for (int i = n - 1; i >= 0; i--) {
            q[i] = carry;
            carry = c[i] + carry * root;
        }
        return q;
    }

    private static double round(double r) {
        double nearest = Math.rint(r);
        return Math.abs(r - nearest) < 1e-6 ? nearest : r;
    }

    private static EvalException notPoly() {
        return new EvalException("Ausdruck ist kein Polynom in der Variablen.");
    }
}
