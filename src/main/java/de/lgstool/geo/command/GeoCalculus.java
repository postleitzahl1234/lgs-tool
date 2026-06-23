package de.lgstool.geo.command;

import de.lgstool.geo.expr.Differentiator;
import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.FunctionApplier;
import de.lgstool.geo.expr.Value;
import java.util.List;

/**
 * Curve-analysis ("Kurvendiskussion") constructors built on the symbolic
 * {@link Differentiator} and numeric root finding: {@code Root}, {@code Extremum},
 * {@code InflectionPoint} and {@code Normal}. Each searches a default domain and
 * returns the first feature found, so calling with a start value or repeated calls
 * lets the user reach further features.
 */
final class GeoCalculus {

    private static final double SCAN_MIN = -10;
    private static final double SCAN_MAX = 10;
    private static final int SCAN_STEPS = 2000;
    private static final double NEWTON_TOL = 1e-12;
    private static final int NEWTON_ITERS = 60;

    private GeoCalculus() {
    }

    /** {@code Root(f)} (first zero in the scan domain) or {@code Root(f, x0)} (Newton from x0). */
    static Value root(List<Value> args, FunctionApplier applier) {
        if (args.isEmpty() || !(args.get(0) instanceof Value.FunctionV f) || f.params().size() != 1) {
            throw new EvalException("Root erwartet eine Funktion f(x).");
        }
        if (args.size() == 2) {
            double x0 = GeoBuiltins.num(args.get(1));
            double r = newton(f, derivativeOf(f), x0, applier);
            if (!Double.isFinite(r)) {
                throw new EvalException("Keine Nullstelle nahe x0 gefunden.");
            }
            return Value.PointV.of2D(r, 0);
        }
        GeoBuiltins.require(args, 1, "Root");
        double r = firstRoot(f, applier);
        if (!Double.isFinite(r)) {
            throw new EvalException("Keine Nullstelle im Bereich [-10, 10] gefunden.");
        }
        return Value.PointV.of2D(r, 0);
    }

    /** {@code Extremum(f)}: first point where f'(x) = 0 (local max/min). */
    static Value extremum(List<Value> args, FunctionApplier applier) {
        Value.FunctionV f = function(args, "Extremum");
        Value.FunctionV df = derivativeOf(f);
        double x = firstRoot(df, applier);
        if (!Double.isFinite(x)) {
            throw new EvalException("Kein Extrempunkt im Bereich [-10, 10] gefunden.");
        }
        return Value.PointV.of2D(x, sample(f, x, applier));
    }

    /** {@code InflectionPoint(f)}: first point where f''(x) = 0. */
    static Value inflection(List<Value> args, FunctionApplier applier) {
        Value.FunctionV f = function(args, "InflectionPoint");
        Value.FunctionV ddf = derivativeOf(derivativeOf(f));
        double x = firstRoot(ddf, applier);
        if (!Double.isFinite(x)) {
            throw new EvalException("Kein Wendepunkt im Bereich [-10, 10] gefunden.");
        }
        return Value.PointV.of2D(x, sample(f, x, applier));
    }

    /** {@code Normal(x0, f)}: the line perpendicular to the tangent of f at x0. */
    static Value normal(List<Value> args, FunctionApplier applier) {
        GeoBuiltins.require(args, 2, "Normal");
        if (!(args.get(0) instanceof Value.NumberV at) || !(args.get(1) instanceof Value.FunctionV f)
                || f.params().size() != 1) {
            throw new EvalException("Normal erwartet (x0, f).");
        }
        double x0 = at.v();
        double y0 = sample(f, x0, applier);
        double slope = sample(derivativeOf(f), x0, applier);
        if (Math.abs(slope) < 1e-12) {
            return new Value.LineV(1, 0, x0);          // tangent horizontal -> normal vertical x = x0
        }
        double m = -1 / slope;
        // y = y0 + m·(x - x0)  ->  m·x - y = m·x0 - y0
        return new Value.LineV(m, -1, m * x0 - y0);
    }

    // ---- helpers ---------------------------------------------------------

    private static Value.FunctionV function(List<Value> args, String where) {
        GeoBuiltins.require(args, 1, where);
        if (!(args.get(0) instanceof Value.FunctionV f) || f.params().size() != 1) {
            throw new EvalException(where + " erwartet eine Funktion f(x).");
        }
        return f;
    }

    private static Value.FunctionV derivativeOf(Value.FunctionV f) {
        return new Value.FunctionV(f.params(), Differentiator.differentiate(f.body(), f.params().get(0)));
    }

    /** Scans the default domain for the first sign change and refines it by bisection. */
    private static double firstRoot(Value.FunctionV f, FunctionApplier applier) {
        double step = (SCAN_MAX - SCAN_MIN) / SCAN_STEPS;
        double prevX = SCAN_MIN;
        double prevY = safeSample(f, prevX, applier);
        for (int i = 1; i <= SCAN_STEPS; i++) {
            double x = SCAN_MIN + i * step;
            double y = safeSample(f, x, applier);
            if (Double.isFinite(prevY) && Double.isFinite(y)) {
                if (prevY == 0) {
                    return prevX;
                }
                if (prevY < 0 != y < 0) {
                    return bisect(f, prevX, x, applier);
                }
            }
            prevX = x;
            prevY = y;
        }
        return Double.NaN;
    }

    private static double bisect(Value.FunctionV f, double lo, double hi, FunctionApplier applier) {
        double fLo = sample(f, lo, applier);
        for (int i = 0; i < NEWTON_ITERS; i++) {
            double mid = (lo + hi) / 2;
            double fMid = safeSample(f, mid, applier);
            if (!Double.isFinite(fMid) || Math.abs(fMid) < NEWTON_TOL || (hi - lo) / 2 < NEWTON_TOL) {
                return mid;
            }
            if (fLo < 0 != fMid < 0) {
                hi = mid;
            } else {
                lo = mid;
                fLo = fMid;
            }
        }
        return (lo + hi) / 2;
    }

    private static double newton(Value.FunctionV f, Value.FunctionV df, double x, FunctionApplier applier) {
        for (int i = 0; i < NEWTON_ITERS; i++) {
            double y = safeSample(f, x, applier);
            double dy = safeSample(df, x, applier);
            if (!Double.isFinite(y) || !Double.isFinite(dy) || Math.abs(dy) < 1e-14) {
                return Double.NaN;
            }
            double next = x - y / dy;
            if (Math.abs(next - x) < NEWTON_TOL) {
                return next;
            }
            x = next;
        }
        return Math.abs(safeSample(f, x, applier)) < 1e-7 ? x : Double.NaN;
    }

    private static double sample(Value.FunctionV f, double x, FunctionApplier applier) {
        Value v = applier.apply(f, List.of(new Value.NumberV(x)));
        if (v instanceof Value.NumberV n) {
            return n.v();
        }
        throw new EvalException("Funktion liefert keine Zahl.");
    }

    private static double safeSample(Value.FunctionV f, double x, FunctionApplier applier) {
        try {
            Value v = applier.apply(f, List.of(new Value.NumberV(x)));
            return v instanceof Value.NumberV n ? n.v() : Double.NaN;
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }
}
