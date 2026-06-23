package de.lgstool.geo.command;

import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.Value;
import java.util.List;

/**
 * Measurement operators on geometry objects, all returning a {@link Value.NumberV}:
 * generalized {@code Distance} (point/line/plane combinations, incl. skew 3D lines),
 * {@code Angle} (three points, two lines, two planes, line and plane) and the scalar
 * measures {@code Length}, {@code Area}, {@code Perimeter} and {@code Slope}.
 */
final class GeoMeasure {

    private static final double EPS = 1e-12;

    private GeoMeasure() {
    }

    // ---- distance --------------------------------------------------------

    static Value distance(List<Value> args) {
        GeoBuiltins.require(args, 2, "Distance");
        Value a = args.get(0);
        Value b = args.get(1);
        Double d = distanceBetween(a, b);
        if (d == null) {
            d = distanceBetween(b, a);
        }
        if (d == null) {
            throw new EvalException("Distance unterstützt Punkt/Gerade/Ebene-Kombinationen.");
        }
        return new Value.NumberV(d);
    }

    private static Double distanceBetween(Value a, Value b) {
        if (a instanceof Value.PointV p && b instanceof Value.PointV q) {
            return Math.sqrt(sq(q.x() - p.x()) + sq(q.y() - p.y()) + sq(q.z() - p.z()));
        }
        if (a instanceof Value.PointV p && b instanceof Value.LineV l) {
            return Math.abs(l.a() * p.x() + l.b() * p.y() - l.c()) / Math.hypot(l.a(), l.b());
        }
        if (a instanceof Value.PointV p && b instanceof Value.PlaneV pl) {
            double n = Math.sqrt(sq(pl.a()) + sq(pl.b()) + sq(pl.c()));
            if (n < EPS) {
                throw new EvalException("Ungültige Ebene.");
            }
            return Math.abs(pl.a() * p.x() + pl.b() * p.y() + pl.c() * p.z() - pl.d()) / n;
        }
        if (a instanceof Value.PointV p && b instanceof Value.Line3DV l) {
            return pointLine3D(p, l);
        }
        if (a instanceof Value.LineV la && b instanceof Value.LineV lb) {
            return lineLine2D(la, lb);
        }
        if (a instanceof Value.Line3DV la && b instanceof Value.Line3DV lb) {
            return lineLine3D(la, lb);
        }
        return null;
    }

    private static double pointLine3D(Value.PointV p, Value.Line3DV l) {
        double[] d = {l.dir().x(), l.dir().y(), l.dir().z()};
        double[] w = {p.x() - l.point().x(), p.y() - l.point().y(), p.z() - l.point().z()};
        double[] cr = cross(w, d);
        double dl = norm(d);
        if (dl < EPS) {
            throw new EvalException("Ungültige Gerade (Richtung 0).");
        }
        return norm(cr) / dl;
    }

    private static double lineLine2D(Value.LineV p, Value.LineV q) {
        double det = p.a() * q.b() - q.a() * p.b();
        if (Math.abs(det) > EPS) {
            return 0;                                  // not parallel -> they cross
        }
        double np = Math.hypot(p.a(), p.b());
        double nq = Math.hypot(q.a(), q.b());
        if (np < EPS || nq < EPS) {
            throw new EvalException("Ungültige Gerade.");
        }
        // Re-scale q to p's normal so the constants are comparable.
        double scale = (p.a() * q.a() + p.b() * q.b()) < 0 ? -np / nq : np / nq;
        return Math.abs(p.c() - q.c() * scale) / np;
    }

    private static double lineLine3D(Value.Line3DV g, Value.Line3DV h) {
        double[] d1 = {g.dir().x(), g.dir().y(), g.dir().z()};
        double[] d2 = {h.dir().x(), h.dir().y(), h.dir().z()};
        double[] r = {h.point().x() - g.point().x(), h.point().y() - g.point().y(), h.point().z() - g.point().z()};
        double[] n = cross(d1, d2);
        double nl = norm(n);
        if (nl < EPS) {                                // parallel -> point-to-line distance
            return pointLine3D(h.point(), g);
        }
        return Math.abs(dot(r, n)) / nl;
    }

    // ---- angle -----------------------------------------------------------

    static Value angle(List<Value> args) {
        if (args.size() == 3 && args.stream().allMatch(v -> v instanceof Value.PointV)) {
            return threePointAngle(args);
        }
        if (args.size() == 2) {
            double[] u = direction(args.get(0));
            double[] v = direction(args.get(1));
            boolean aPlane = args.get(0) instanceof Value.PlaneV;
            boolean bPlane = args.get(1) instanceof Value.PlaneV;
            if (aPlane != bPlane) {                    // one line, one plane
                double[] dir = aPlane ? v : u;
                double[] nrm = aPlane ? u : v;
                double s = Math.abs(dot(dir, nrm)) / (norm(dir) * norm(nrm));
                return new Value.NumberV(Math.toDegrees(Math.asin(clamp(s))));
            }
            double cos = Math.abs(dot(u, v)) / (norm(u) * norm(v));
            return new Value.NumberV(Math.toDegrees(Math.acos(clamp(cos))));
        }
        throw new EvalException("Angle erwartet 3 Punkte, zwei Geraden/Ebenen oder Gerade und Ebene.");
    }

    private static Value threePointAngle(List<Value> args) {
        Value.PointV a = (Value.PointV) args.get(0);
        Value.PointV b = (Value.PointV) args.get(1);
        Value.PointV c = (Value.PointV) args.get(2);
        double[] ba = {a.x() - b.x(), a.y() - b.y(), a.z() - b.z()};
        double[] bc = {c.x() - b.x(), c.y() - b.y(), c.z() - b.z()};
        double mag = norm(ba) * norm(bc);
        if (mag < EPS) {
            throw new EvalException("Winkel nicht definiert (Punkte fallen zusammen).");
        }
        return new Value.NumberV(Math.toDegrees(Math.acos(clamp(dot(ba, bc) / mag))));
    }

    /** A direction (lines) or normal (planes) vector for the angle operators. */
    private static double[] direction(Value v) {
        return switch (v) {
            case Value.LineV l -> new double[]{-l.b(), l.a(), 0};
            case Value.Line3DV l -> new double[]{l.dir().x(), l.dir().y(), l.dir().z()};
            case Value.PlaneV p -> new double[]{p.a(), p.b(), p.c()};
            default -> throw new EvalException("Angle erwartet Geraden oder Ebenen.");
        };
    }

    // ---- scalar measures -------------------------------------------------

    static Value length(List<Value> args) {
        if (args.size() == 2 && args.get(0) instanceof Value.PointV && args.get(1) instanceof Value.PointV) {
            return distance(args);
        }
        GeoBuiltins.require(args, 1, "Length");
        return switch (args.get(0)) {
            case Value.SegmentV s -> new Value.NumberV(Math.sqrt(
                    sq(s.q().x() - s.p().x()) + sq(s.q().y() - s.p().y()) + sq(s.q().z() - s.p().z())));
            case Value.VectorV v -> new Value.NumberV(norm(new double[]{v.x(), v.y(), v.z()}));
            case Value.ListV l -> new Value.NumberV(l.items().size());
            default -> throw new EvalException("Length erwartet eine Strecke, einen Vektor, eine Liste oder zwei Punkte.");
        };
    }

    static Value area(List<Value> args) {
        GeoBuiltins.require(args, 1, "Area");
        return switch (args.get(0)) {
            case Value.PolygonV poly -> new Value.NumberV(shoelace(poly.vertices()));
            case Value.CircleV c -> new Value.NumberV(Math.PI * c.r() * c.r());
            default -> throw new EvalException("Area erwartet ein Vieleck oder einen Kreis.");
        };
    }

    static Value perimeter(List<Value> args) {
        GeoBuiltins.require(args, 1, "Perimeter");
        return switch (args.get(0)) {
            case Value.PolygonV poly -> new Value.NumberV(polygonPerimeter(poly.vertices()));
            case Value.CircleV c -> new Value.NumberV(2 * Math.PI * c.r());
            default -> throw new EvalException("Perimeter erwartet ein Vieleck oder einen Kreis.");
        };
    }

    static Value slope(List<Value> args) {
        GeoBuiltins.require(args, 1, "Slope");
        if (!(args.get(0) instanceof Value.LineV l)) {
            throw new EvalException("Slope erwartet eine Gerade (2D).");
        }
        if (Math.abs(l.b()) < EPS) {
            throw new EvalException("Senkrechte Gerade hat keine Steigung.");
        }
        return new Value.NumberV(-l.a() / l.b());
    }

    private static double shoelace(List<Value.PointV> vs) {
        double sum = 0;
        for (int i = 0; i < vs.size(); i++) {
            Value.PointV p = vs.get(i);
            Value.PointV q = vs.get((i + 1) % vs.size());
            sum += p.x() * q.y() - q.x() * p.y();
        }
        return Math.abs(sum) / 2;
    }

    private static double polygonPerimeter(List<Value.PointV> vs) {
        double sum = 0;
        for (int i = 0; i < vs.size(); i++) {
            Value.PointV p = vs.get(i);
            Value.PointV q = vs.get((i + 1) % vs.size());
            sum += Math.hypot(q.x() - p.x(), q.y() - p.y());
        }
        return sum;
    }

    // ---- vector helpers --------------------------------------------------

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] u, double[] v) {
        return new double[]{
                u[1] * v[2] - u[2] * v[1],
                u[2] * v[0] - u[0] * v[2],
                u[0] * v[1] - u[1] * v[0]};
    }

    private static double norm(double[] v) {
        return Math.sqrt(dot(v, v));
    }

    private static double clamp(double x) {
        return Math.max(-1, Math.min(1, x));
    }

    private static double sq(double x) {
        return x * x;
    }
}
