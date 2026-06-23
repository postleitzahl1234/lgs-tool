package de.lgstool.geo.command;

import de.lgstool.geo.expr.Differentiator;
import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.FunctionApplier;
import de.lgstool.geo.expr.Value;
import java.util.ArrayList;
import java.util.List;

/**
 * Analytic-geometry descriptors that derive new objects from existing ones:
 * {@code Center}/{@code Eccentricity} of a conic, {@code Circle} through three points
 * (circumcircle), {@code Centroid} of points/a polygon, {@code ClosestPoint} on a line
 * or circle, {@code Curvature} of a function, and {@code Roots} (all zeros in a window).
 */
final class GeoConicAnalysis {

    private static final double EPS = 1e-9;
    private static final double SCAN_MIN = -20;
    private static final double SCAN_MAX = 20;
    private static final int SCAN_STEPS = 4000;

    private GeoConicAnalysis() {
    }

    /** Centre of a circle, or of a central conic via the linear system [2A B; B 2C]·c = −[D;E]. */
    static Value center(List<Value> args) {
        GeoBuiltins.require(args, 1, "Center");
        return switch (args.get(0)) {
            case Value.CircleV c -> c.center();
            case Value.ConicV q -> conicCenter(q);
            default -> throw new EvalException("Center erwartet einen Kegelschnitt oder Kreis.");
        };
    }

    private static Value conicCenter(Value.ConicV q) {
        double det = 4 * q.a() * q.c() - q.b() * q.b();
        if (Math.abs(det) < EPS) {
            throw new EvalException("Kegelschnitt hat keinen eindeutigen Mittelpunkt (z. B. Parabel).");
        }
        double x = (-2 * q.c() * q.d() + q.b() * q.e()) / det;
        double y = (-2 * q.a() * q.e() + q.b() * q.d()) / det;
        return Value.PointV.of2D(x, y);
    }

    /** Numerical eccentricity from the eigenvalues of the quadratic part [[A, B/2],[B/2, C]]. */
    static Value eccentricity(List<Value> args) {
        GeoBuiltins.require(args, 1, "Eccentricity");
        if (args.get(0) instanceof Value.CircleV) {
            return new Value.NumberV(0);
        }
        if (!(args.get(0) instanceof Value.ConicV q)) {
            throw new EvalException("Eccentricity erwartet einen Kegelschnitt.");
        }
        double[] eig = eigenvalues2x2(q.a(), q.b() / 2, q.c());
        double l1 = eig[0];
        double l2 = eig[1];
        if (l1 * l2 > 0) {
            // ellipse: e = sqrt(1 − min/max) of the axis squares (∝ 1/λ)
            double major = 1 / Math.min(Math.abs(l1), Math.abs(l2));
            double minor = 1 / Math.max(Math.abs(l1), Math.abs(l2));
            return new Value.NumberV(Math.sqrt(Math.max(0, 1 - minor / major)));
        }
        // hyperbola: e = sqrt(1 + b²/a²) with a²,b² ∝ 1/|λ|
        double a2 = 1 / Math.abs(l1);
        double b2 = 1 / Math.abs(l2);
        return new Value.NumberV(Math.sqrt(1 + b2 / a2));
    }

    private static double[] eigenvalues2x2(double a, double b, double c) {
        double tr = a + c;
        double disc = Math.sqrt(Math.max(0, tr * tr - 4 * (a * c - b * b)));
        return new double[]{(tr - disc) / 2, (tr + disc) / 2};
    }

    /** The unique circle through three non-collinear points. */
    static Value circleThrough(Value.PointV a, Value.PointV b, Value.PointV c) {
        if (a.is3D() || b.is3D() || c.is3D()) {
            throw new EvalException("Circle durch drei Punkte ist 2D.");
        }
        double ax = a.x();
        double ay = a.y();
        double d = 2 * (ax * (b.y() - c.y()) + b.x() * (c.y() - ay) + c.x() * (ay - b.y()));
        if (Math.abs(d) < EPS) {
            throw new EvalException("Die drei Punkte liegen auf einer Geraden.");
        }
        double a2 = ax * ax + ay * ay;
        double b2 = b.x() * b.x() + b.y() * b.y();
        double c2 = c.x() * c.x() + c.y() * c.y();
        double ux = (a2 * (b.y() - c.y()) + b2 * (c.y() - ay) + c2 * (ay - b.y())) / d;
        double uy = (a2 * (c.x() - b.x()) + b2 * (ax - c.x()) + c2 * (b.x() - ax)) / d;
        Value.PointV centre = Value.PointV.of2D(ux, uy);
        return new Value.CircleV(centre, Math.hypot(ax - ux, ay - uy));
    }

    /** Centroid (arithmetic mean) of a polygon's vertices or a list of points. */
    static Value centroid(List<Value> args) {
        GeoBuiltins.require(args, 1, "Centroid");
        List<Value.PointV> pts = new ArrayList<>();
        switch (args.get(0)) {
            case Value.PolygonV poly -> pts.addAll(poly.vertices());
            case Value.ListV list -> {
                for (Value v : list.items()) {
                    if (v instanceof Value.PointV p) {
                        pts.add(p);
                    }
                }
            }
            default -> throw new EvalException("Centroid erwartet ein Vieleck oder eine Punktliste.");
        }
        if (pts.isEmpty()) {
            throw new EvalException("Centroid: keine Punkte.");
        }
        double sx = 0;
        double sy = 0;
        for (Value.PointV p : pts) {
            sx += p.x();
            sy += p.y();
        }
        return Value.PointV.of2D(sx / pts.size(), sy / pts.size());
    }

    /** The point on a line or circle closest to a given point. */
    static Value closestPoint(List<Value> args) {
        GeoBuiltins.require(args, 2, "ClosestPoint");
        Value.PointV p = GeoBuiltins.point(args.get(0), "ClosestPoint");
        return switch (args.get(1)) {
            case Value.LineV line -> {
                double norm2 = line.a() * line.a() + line.b() * line.b();
                double t = (line.a() * p.x() + line.b() * p.y() - line.c()) / norm2;
                yield Value.PointV.of2D(p.x() - line.a() * t, p.y() - line.b() * t);
            }
            case Value.CircleV circle -> {
                double dx = p.x() - circle.center().x();
                double dy = p.y() - circle.center().y();
                double len = Math.hypot(dx, dy);
                if (len < EPS) {
                    yield Value.PointV.of2D(circle.center().x() + circle.r(), circle.center().y());
                }
                yield Value.PointV.of2D(circle.center().x() + circle.r() * dx / len,
                        circle.center().y() + circle.r() * dy / len);
            }
            default -> throw new EvalException("ClosestPoint erwartet (Punkt, Gerade|Kreis).");
        };
    }

    /** Curvature of f at x0: |f''| / (1 + f'²)^{3/2}. */
    static Value curvature(List<Value> args, FunctionApplier applier) {
        if (args.size() != 2 || !(args.get(0) instanceof Value.NumberV at)
                || !(args.get(1) instanceof Value.FunctionV f) || f.params().size() != 1) {
            throw new EvalException("Curvature erwartet (x0, f).");
        }
        String var = f.params().get(0);
        Value.FunctionV df = new Value.FunctionV(f.params(), Differentiator.differentiate(f.body(), var));
        Value.FunctionV ddf = new Value.FunctionV(f.params(), Differentiator.differentiate(df.body(), var));
        double d1 = sample(applier, df, at.v());
        double d2 = sample(applier, ddf, at.v());
        return new Value.NumberV(Math.abs(d2) / Math.pow(1 + d1 * d1, 1.5));
    }

    /** All zeros of a function within the scan window, as a list of points (Roots, plural). */
    static Value roots(List<Value> args, FunctionApplier applier) {
        if (args.isEmpty() || !(args.get(0) instanceof Value.FunctionV f) || f.params().size() != 1) {
            throw new EvalException("Roots erwartet eine Funktion f(x).");
        }
        double step = (SCAN_MAX - SCAN_MIN) / SCAN_STEPS;
        double off = step * 0.5;   // sample at cell centres so integer roots aren't missed
        List<Value> out = new ArrayList<>();
        double prevX = SCAN_MIN + off;
        double prevY = sample(applier, f, prevX);
        for (int i = 1; i < SCAN_STEPS; i++) {
            double x = SCAN_MIN + off + i * step;
            double y = sample(applier, f, x);
            if (Double.isFinite(prevY) && Double.isFinite(y) && prevY * y < 0) {
                double r = bisect(applier, f, prevX, x);
                out.add(Value.PointV.of2D(r, 0));
            }
            prevX = x;
            prevY = y;
        }
        if (out.isEmpty()) {
            throw new EvalException("Keine Nullstelle im Bereich [-20, 20].");
        }
        return new Value.ListV(out);
    }

    private static double bisect(FunctionApplier applier, Value.FunctionV f, double lo, double hi) {
        double flo = sample(applier, f, lo);
        for (int i = 0; i < 80; i++) {
            double mid = (lo + hi) / 2;
            double fmid = sample(applier, f, mid);
            if (flo * fmid <= 0) {
                hi = mid;
            } else {
                lo = mid;
                flo = fmid;
            }
        }
        return (lo + hi) / 2;
    }

    private static double sample(FunctionApplier applier, Value.FunctionV f, double x) {
        Value v = applier.apply(f, List.of(new Value.NumberV(x)));
        return v instanceof Value.NumberV n ? n.v() : Double.NaN;
    }
}
