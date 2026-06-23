package de.lgstool.geo.command;

import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.FunctionApplier;
import de.lgstool.geo.expr.Value;
import java.util.List;

/**
 * Intersection maths for {@code Intersect(a, b [, n])}: 2D line∩line, line∩circle and
 * circle∩circle, plus 3D line∩plane and plane∩plane (which yields a line). For the
 * two-point 2D cases an optional index {@code n} (1 or 2) selects which point.
 */
final class GeoIntersections {

    private static final double EPS = 1e-9;

    private GeoIntersections() {
    }

    static Value intersect(List<Value> args, FunctionApplier applier) {
        if (args.size() < 2 || args.size() > 3) {
            throw new EvalException("Intersect erwartet (a, b) oder (a, b, n).");
        }
        int index = args.size() == 3 ? (int) Math.round(GeoBuiltins.num(args.get(2))) : 1;
        Value a = args.get(0);
        Value b = args.get(1);
        if (a instanceof Value.FunctionV fa && b instanceof Value.FunctionV fb) {
            return funcFunc(fa, fb, applier, index);
        }
        if (a instanceof Value.FunctionV fa && b instanceof Value.LineV lb) {
            return funcLine(fa, lb, applier, index);
        }
        if (a instanceof Value.LineV la && b instanceof Value.FunctionV fb) {
            return funcLine(fb, la, applier, index);
        }
        if (a instanceof Value.LineV la && b instanceof Value.LineV lb) {
            return lineLine(la, lb);
        }
        if (a instanceof Value.LineV la && b instanceof Value.CircleV cb) {
            return lineCircle(la, cb, index);
        }
        if (a instanceof Value.CircleV ca && b instanceof Value.LineV lb) {
            return lineCircle(lb, ca, index);
        }
        if (a instanceof Value.CircleV ca && b instanceof Value.CircleV cb) {
            return lineCircle(radicalLine(ca, cb), ca, index);
        }
        if (a instanceof Value.Line3DV la && b instanceof Value.PlaneV pb) {
            return linePlane(la, pb);
        }
        if (a instanceof Value.PlaneV pa && b instanceof Value.Line3DV lb) {
            return linePlane(lb, pa);
        }
        if (a instanceof Value.PlaneV pa && b instanceof Value.PlaneV pb) {
            return planePlane(pa, pb);
        }
        throw new EvalException("Intersect unterstützt Gerade/Kreis (2D) und Gerade/Ebene (3D).");
    }

    // ---- function intersections (numeric) --------------------------------

    private static Value funcFunc(Value.FunctionV f, Value.FunctionV g, FunctionApplier applier, int index) {
        double x = crossing(t -> sample(applier, f, t) - sample(applier, g, t), index);
        return Value.PointV.of2D(x, sample(applier, f, x));
    }

    private static Value funcLine(Value.FunctionV f, Value.LineV line, FunctionApplier applier, int index) {
        if (Math.abs(line.b()) < EPS) {
            double x = line.c() / line.a();      // vertical line x = c/a
            return Value.PointV.of2D(x, sample(applier, f, x));
        }
        double x = crossing(t -> sample(applier, f, t) - (line.c() - line.a() * t) / line.b(), index);
        return Value.PointV.of2D(x, sample(applier, f, x));
    }

    private interface Fn {
        double at(double x);
    }

    /** The {@code index}-th sign-change crossing of {@code h} in [-50, 50]. */
    private static double crossing(Fn h, int index) {
        double lo = -50;
        double hi = 50;
        int steps = 10000;
        double step = (hi - lo) / steps;
        double off = step * 0.5;   // cell centres so integer crossings aren't skipped
        int found = 0;
        double prevX = lo + off;
        double prevY = h.at(prevX);
        for (int i = 1; i < steps; i++) {
            double x = lo + off + i * step;
            double y = h.at(x);
            if (Double.isFinite(prevY) && Double.isFinite(y) && prevY * y < 0) {
                found++;
                if (found == index) {
                    return bisect(h, prevX, x);
                }
            }
            prevX = x;
            prevY = y;
        }
        throw new EvalException("Kein " + index + ". Schnittpunkt im Bereich [-50, 50] gefunden.");
    }

    private static double bisect(Fn h, double lo, double hi) {
        double flo = h.at(lo);
        for (int i = 0; i < 80; i++) {
            double mid = (lo + hi) / 2;
            double fmid = h.at(mid);
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

    private static Value lineLine(Value.LineV p, Value.LineV q) {
        double det = p.a() * q.b() - q.a() * p.b();
        if (Math.abs(det) < EPS) {
            throw new EvalException("Die Geraden sind parallel — kein Schnittpunkt.");
        }
        double x = (p.c() * q.b() - q.c() * p.b()) / det;
        double y = (p.a() * q.c() - q.a() * p.c()) / det;
        return Value.PointV.of2D(x, y);
    }

    private static Value lineCircle(Value.LineV line, Value.CircleV circle, int index) {
        double a = line.a();
        double b = line.b();
        double c = line.c();
        double cx = circle.center().x();
        double cy = circle.center().y();
        double r = circle.r();

        double norm2 = a * a + b * b;
        if (norm2 < EPS) {
            throw new EvalException("Ungültige Gerade.");
        }
        double dist = (a * cx + b * cy - c) / Math.sqrt(norm2);
        if (Math.abs(dist) > r + EPS) {
            throw new EvalException("Gerade und Kreis schneiden sich nicht.");
        }
        double footX = cx - a * (a * cx + b * cy - c) / norm2;
        double footY = cy - b * (a * cx + b * cy - c) / norm2;
        double half = Math.sqrt(Math.max(0, r * r - dist * dist));
        double dirLen = Math.sqrt(norm2);
        double dx = -b / dirLen;
        double dy = a / dirLen;
        double sign = index == 2 ? -1 : 1;
        return Value.PointV.of2D(footX + sign * dx * half, footY + sign * dy * half);
    }

    private static Value.LineV radicalLine(Value.CircleV c1, Value.CircleV c2) {
        double x1 = c1.center().x();
        double y1 = c1.center().y();
        double x2 = c2.center().x();
        double y2 = c2.center().y();
        double a = 2 * (x2 - x1);
        double b = 2 * (y2 - y1);
        double c = (x2 * x2 - x1 * x1) + (y2 * y2 - y1 * y1) + (c1.r() * c1.r() - c2.r() * c2.r());
        if (Math.abs(a) < EPS && Math.abs(b) < EPS) {
            throw new EvalException("Die Kreise schneiden sich nicht.");
        }
        return new Value.LineV(a, b, c);
    }

    private static Value linePlane(Value.Line3DV line, Value.PlaneV plane) {
        double[] n = {plane.a(), plane.b(), plane.c()};
        double[] d = {line.dir().x(), line.dir().y(), line.dir().z()};
        double nd = n[0] * d[0] + n[1] * d[1] + n[2] * d[2];
        if (Math.abs(nd) < EPS) {
            throw new EvalException("Gerade und Ebene sind parallel — kein eindeutiger Schnittpunkt.");
        }
        double[] p = {line.point().x(), line.point().y(), line.point().z()};
        double np = n[0] * p[0] + n[1] * p[1] + n[2] * p[2];
        double t = (plane.d() - np) / nd;
        return Value.PointV.of3D(p[0] + t * d[0], p[1] + t * d[1], p[2] + t * d[2]);
    }

    private static Value planePlane(Value.PlaneV p1, Value.PlaneV p2) {
        double[] n1 = {p1.a(), p1.b(), p1.c()};
        double[] n2 = {p2.a(), p2.b(), p2.c()};
        double[] dir = cross(n1, n2);
        double len2 = dot(dir, dir);
        if (len2 < EPS) {
            throw new EvalException("Die Ebenen sind parallel — keine Schnittgerade.");
        }
        // Point on both planes: (d1·(n2×dir) + d2·(dir×n1)) / |dir|².
        double[] a = scale(cross(n2, dir), p1.d());
        double[] b = scale(cross(dir, n1), p2.d());
        double[] point = scale(add(a, b), 1 / len2);
        Value.PointV base = Value.PointV.of3D(point[0], point[1], point[2]);
        return new Value.Line3DV(base, new Value.VectorV(dir[0], dir[1], dir[2], true));
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] u, double[] v) {
        return new double[]{
                u[1] * v[2] - u[2] * v[1],
                u[2] * v[0] - u[0] * v[2],
                u[0] * v[1] - u[1] * v[0]};
    }

    private static double[] add(double[] a, double[] b) {
        return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] scale(double[] a, double k) {
        return new double[]{a[0] * k, a[1] * k, a[2] * k};
    }
}
