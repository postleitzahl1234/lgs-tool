package de.lgstool.geo.command;

import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.Value;
import java.util.List;

/**
 * Classic straight-edge-and-compass constructions in 2D: {@code PerpendicularLine}
 * and {@code ParallelLine} through a point, the {@code PerpendicularBisector}
 * (Mittelsenkrechte) of two points, the {@code AngleBisector} (Winkelhalbierende) of
 * three points, and {@code PointOn} placing a point at a parameter along a path.
 */
final class GeoConstruct {

    private static final double EPS = 1e-12;

    private GeoConstruct() {
    }

    /** Line through P perpendicular to the 2D line g. */
    static Value perpendicularLine(List<Value> args) {
        GeoBuiltins.require(args, 2, "PerpendicularLine");
        Value.PointV p = GeoBuiltins.point(args.get(0), "PerpendicularLine");
        Value.LineV g = line2D(args.get(1), "PerpendicularLine");
        // g has normal (a,b); the perpendicular line has normal (b,-a).
        return new Value.LineV(g.b(), -g.a(), g.b() * p.x() - g.a() * p.y());
    }

    /** Line through P parallel to the 2D line g. */
    static Value parallelLine(List<Value> args) {
        GeoBuiltins.require(args, 2, "ParallelLine");
        Value.PointV p = GeoBuiltins.point(args.get(0), "ParallelLine");
        Value.LineV g = line2D(args.get(1), "ParallelLine");
        return new Value.LineV(g.a(), g.b(), g.a() * p.x() + g.b() * p.y());
    }

    /** Perpendicular bisector (Mittelsenkrechte) of segment AB. */
    static Value perpendicularBisector(List<Value> args) {
        GeoBuiltins.require(args, 2, "PerpendicularBisector");
        Value.PointV a = GeoBuiltins.point(args.get(0), "PerpendicularBisector");
        Value.PointV b = GeoBuiltins.point(args.get(1), "PerpendicularBisector");
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        if (Math.hypot(dx, dy) < EPS) {
            throw new EvalException("Mittelsenkrechte nicht definiert (Punkte fallen zusammen).");
        }
        double mx = (a.x() + b.x()) / 2;
        double my = (a.y() + b.y()) / 2;
        return new Value.LineV(dx, dy, dx * mx + dy * my);
    }

    /** Angle bisector (Winkelhalbierende) of the angle at B in A-B-C. */
    static Value angleBisector(List<Value> args) {
        GeoBuiltins.require(args, 3, "AngleBisector");
        Value.PointV a = GeoBuiltins.point(args.get(0), "AngleBisector");
        Value.PointV b = GeoBuiltins.point(args.get(1), "AngleBisector");
        Value.PointV c = GeoBuiltins.point(args.get(2), "AngleBisector");
        double[] ba = unit(a.x() - b.x(), a.y() - b.y());
        double[] bc = unit(c.x() - b.x(), c.y() - b.y());
        double dx = ba[0] + bc[0];
        double dy = ba[1] + bc[1];
        if (Math.hypot(dx, dy) < EPS) {
            throw new EvalException("Winkelhalbierende nicht definiert (Strahlen gegenläufig).");
        }
        // Line through B with direction (dx,dy) -> normal (dy,-dx).
        return new Value.LineV(dy, -dx, dy * b.x() - dx * b.y());
    }

    /**
     * Point at parameter t along a path. Segment and circle use {@code t in [0,1]} (a
     * fraction of the segment / once around the circle); a line uses t as the signed
     * multiple of its unit direction; a function f uses t as the x value: (t, f(t)).
     */
    static Value pointOn(List<Value> args) {
        GeoBuiltins.require(args, 2, "PointOn");
        double t = GeoBuiltins.num(args.get(1));
        return switch (args.get(0)) {
            case Value.SegmentV s -> Value.PointV.of2D(
                    s.p().x() + t * (s.q().x() - s.p().x()),
                    s.p().y() + t * (s.q().y() - s.p().y()));
            case Value.CircleV c -> Value.PointV.of2D(
                    c.center().x() + c.r() * Math.cos(2 * Math.PI * t),
                    c.center().y() + c.r() * Math.sin(2 * Math.PI * t));
            case Value.LineV l -> pointOnLine(l, t);
            default -> throw new EvalException("PointOn erwartet eine Strecke, Gerade, Kreis oder Funktion.");
        };
    }

    private static Value pointOnLine(Value.LineV l, double t) {
        double len = Math.hypot(l.a(), l.b());
        if (len < EPS) {
            throw new EvalException("Ungültige Gerade.");
        }
        // A point on the line, then step along the unit direction (-b, a)/|n|.
        double x0;
        double y0;
        if (Math.abs(l.b()) > EPS) {
            x0 = 0;
            y0 = l.c() / l.b();
        } else {
            x0 = l.c() / l.a();
            y0 = 0;
        }
        return Value.PointV.of2D(x0 + (-l.b() / len) * t, y0 + (l.a() / len) * t);
    }

    private static Value.LineV line2D(Value v, String where) {
        if (v instanceof Value.LineV l) {
            return l;
        }
        throw new EvalException(where + " erwartet eine Gerade (2D).");
    }

    private static double[] unit(double x, double y) {
        double n = Math.hypot(x, y);
        if (n < EPS) {
            throw new EvalException("Richtung nicht definiert (Punkte fallen zusammen).");
        }
        return new double[]{x / n, y / n};
    }
}
