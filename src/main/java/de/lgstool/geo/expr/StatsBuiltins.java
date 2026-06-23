package de.lgstool.geo.expr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Descriptive statistics on numeric {@link Value.ListV}s plus least-squares regression on
 * point lists: {@code Median}, {@code Q1}, {@code Q3}, {@code Variance} (population),
 * {@code SD}/{@code Stdev}, {@code Mode}; {@code FitLine} (returns a {@link Value.LineV})
 * and {@code FitPoly(points, degree)} (returns a polynomial {@link Value.FunctionV}).
 */
public final class StatsBuiltins implements BuiltinFunctions {

    private static final Set<String> NAMES = Set.of(
            "median", "q1", "q3", "variance", "sd", "stdev", "mode", "fitline", "fitpoly");

    @Override
    public boolean has(String name) {
        return NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public Value call(String name, List<Value> args, FunctionApplier applier) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "median" -> new Value.NumberV(median(sorted(args, "Median")));
            case "q1" -> new Value.NumberV(quartile(sorted(args, "Q1"), 1));
            case "q3" -> new Value.NumberV(quartile(sorted(args, "Q3"), 3));
            case "variance" -> new Value.NumberV(variance(numbers(args, "Variance")));
            case "sd", "stdev" -> new Value.NumberV(Math.sqrt(variance(numbers(args, "SD"))));
            case "mode" -> new Value.NumberV(mode(numbers(args, "Mode")));
            case "fitline" -> fitLine(args);
            case "fitpoly" -> fitPoly(args);
            default -> throw new EvalException("Unbekannte Funktion '" + name + "'.");
        };
    }

    // ---- descriptive statistics ------------------------------------------

    private static double median(double[] s) {
        int n = s.length;
        return n % 2 == 1 ? s[n / 2] : (s[n / 2 - 1] + s[n / 2]) / 2.0;
    }

    /** Tukey-style quartile: the median of the lower (q=1) or upper (q=3) half. */
    private static double quartile(double[] s, int q) {
        int n = s.length;
        int half = n / 2;
        double[] part = q == 1
                ? java.util.Arrays.copyOfRange(s, 0, half)
                : java.util.Arrays.copyOfRange(s, n - half, n);
        if (part.length == 0) {
            return s[0];
        }
        return median(part);
    }

    private static double variance(double[] v) {
        double mean = 0;
        for (double x : v) {
            mean += x;
        }
        mean /= v.length;
        double sum = 0;
        for (double x : v) {
            sum += (x - mean) * (x - mean);
        }
        return sum / v.length;
    }

    private static double mode(double[] v) {
        Map<Double, Integer> counts = new HashMap<>();
        double best = v[0];
        int bestCount = 0;
        for (double x : v) {
            int c = counts.merge(x, 1, Integer::sum);
            if (c > bestCount) {
                bestCount = c;
                best = x;
            }
        }
        return best;
    }

    // ---- regression ------------------------------------------------------

    /** Least-squares straight line through a point list, returned as {@code m·x − y = −b}. */
    private static Value fitLine(List<Value> args) {
        double[][] pts = points(args, "FitLine");
        if (pts.length < 2) {
            throw new EvalException("FitLine erwartet mindestens 2 Punkte.");
        }
        double[] c = leastSquares(pts, 1);    // c[0] + c[1]·x
        return new Value.LineV(c[1], -1, -c[0]);
    }

    /** Least-squares polynomial of the given degree, returned as a function f(x). */
    private static Value fitPoly(List<Value> args) {
        if (args.size() != 2 || !(args.get(0) instanceof Value.ListV) || !(args.get(1) instanceof Value.NumberV d)) {
            throw new EvalException("FitPoly erwartet (Punktliste, Grad).");
        }
        int degree = (int) Math.round(d.v());
        if (degree < 1) {
            throw new EvalException("FitPoly: Grad muss mindestens 1 sein.");
        }
        double[][] pts = points(List.of(args.get(0)), "FitPoly");
        if (pts.length < degree + 1) {
            throw new EvalException("FitPoly: braucht mindestens Grad+1 Punkte.");
        }
        double[] c = leastSquares(pts, degree);
        return new Value.FunctionV(List.of("x"), polynomial(c));
    }

    /** Solves the normal equations for a degree-d polynomial fit; returns coefficients c0..cd. */
    private static double[] leastSquares(double[][] pts, int degree) {
        int k = degree + 1;
        double[][] ata = new double[k][k];
        double[] aty = new double[k];
        for (double[] p : pts) {
            double[] pow = new double[2 * degree + 1];
            pow[0] = 1;
            for (int e = 1; e < pow.length; e++) {
                pow[e] = pow[e - 1] * p[0];
            }
            for (int r = 0; r < k; r++) {
                for (int col = 0; col < k; col++) {
                    ata[r][col] += pow[r + col];
                }
                aty[r] += p[1] * pow[r];
            }
        }
        return solveLinear(ata, aty);
    }

    private static Expr polynomial(double[] c) {
        Expr body = new Expr.Num(c[0]);
        for (int i = 1; i < c.length; i++) {
            Expr power = i == 1 ? new Expr.Ref("x")
                    : new Expr.Bin('^', new Expr.Ref("x"), new Expr.Num(i));
            Expr term = new Expr.Bin('*', new Expr.Num(c[i]), power);
            body = new Expr.Bin('+', body, term);
        }
        return body;
    }

    /** Gaussian elimination with partial pivoting for a small dense system. */
    private static double[] solveLinear(double[][] a, double[] b) {
        int n = b.length;
        for (int p = 0; p < n; p++) {
            int max = p;
            for (int i = p + 1; i < n; i++) {
                if (Math.abs(a[i][p]) > Math.abs(a[max][p])) {
                    max = i;
                }
            }
            double[] tmp = a[p];
            a[p] = a[max];
            a[max] = tmp;
            double tb = b[p];
            b[p] = b[max];
            b[max] = tb;
            if (Math.abs(a[p][p]) < 1e-12) {
                throw new EvalException("Regression: singuläres System (entartete Punkte).");
            }
            for (int i = p + 1; i < n; i++) {
                double f = a[i][p] / a[p][p];
                b[i] -= f * b[p];
                for (int j = p; j < n; j++) {
                    a[i][j] -= f * a[p][j];
                }
            }
        }
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double s = b[i];
            for (int j = i + 1; j < n; j++) {
                s -= a[i][j] * x[j];
            }
            x[i] = s / a[i][i];
        }
        return x;
    }

    // ---- argument helpers ------------------------------------------------

    private static double[] sorted(List<Value> args, String where) {
        double[] v = numbers(args, where);
        java.util.Arrays.sort(v);
        return v;
    }

    private static double[] numbers(List<Value> args, String where) {
        if (args.size() != 1 || !(args.get(0) instanceof Value.ListV l) || l.items().isEmpty()) {
            throw new EvalException(where + " erwartet eine nicht-leere Zahlenliste.");
        }
        double[] v = new double[l.items().size()];
        for (int i = 0; i < v.length; i++) {
            if (!(l.items().get(i) instanceof Value.NumberV n)) {
                throw new EvalException(where + ": alle Elemente müssen Zahlen sein.");
            }
            v[i] = n.v();
        }
        return v;
    }

    private static double[][] points(List<Value> args, String where) {
        if (args.size() != 1 || !(args.get(0) instanceof Value.ListV l) || l.items().isEmpty()) {
            throw new EvalException(where + " erwartet eine Liste von Punkten.");
        }
        List<double[]> out = new ArrayList<>();
        for (Value item : l.items()) {
            if (!(item instanceof Value.PointV p)) {
                throw new EvalException(where + ": alle Elemente müssen Punkte sein.");
            }
            out.add(new double[]{p.x(), p.y()});
        }
        return out.toArray(new double[0][]);
    }
}
