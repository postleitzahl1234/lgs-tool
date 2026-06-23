package de.lgstool.geo.command;

import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.Value;
import java.util.ArrayList;
import java.util.List;

/**
 * 2D geometric transformations: {@code Reflect} (across a point or line), {@code Rotate}
 * (about a centre, degrees), {@code Translate} (by a vector) and {@code Dilate} (scale
 * from a centre). Each maps every defining point of the object through the same plane
 * map, so points, segments, polygons, circles, vectors and lines all transform
 * consistently. 3D objects are rejected — these operators are planar.
 */
final class GeoTransform {

    /** A point map in the plane. */
    private interface Plane {
        double[] at(double x, double y);
    }

    private GeoTransform() {
    }

    static Value reflect(List<Value> args) {
        GeoBuiltins.require(args, 2, "Reflect");
        Value mirror = args.get(1);
        if (mirror instanceof Value.PointV c) {
            requirePlanar(c);
            return apply(args.get(0), (x, y) -> new double[]{2 * c.x() - x, 2 * c.y() - y}, 1);
        }
        if (mirror instanceof Value.LineV l) {
            double n2 = l.a() * l.a() + l.b() * l.b();
            if (n2 < 1e-12) {
                throw new EvalException("Ungültige Spiegelachse.");
            }
            return apply(args.get(0), (x, y) -> {
                double d = (l.a() * x + l.b() * y - l.c()) / n2;
                return new double[]{x - 2 * l.a() * d, y - 2 * l.b() * d};
            }, 1);
        }
        throw new EvalException("Reflect erwartet (Objekt, Punkt oder Gerade).");
    }

    static Value rotate(List<Value> args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new EvalException("Rotate erwartet (Objekt, Winkel°) oder (Objekt, Winkel°, Zentrum).");
        }
        double rad = Math.toRadians(GeoBuiltins.num(args.get(1)));
        double cx = 0;
        double cy = 0;
        if (args.size() == 3) {
            Value.PointV c = GeoBuiltins.point(args.get(2), "Rotate");
            requirePlanar(c);
            cx = c.x();
            cy = c.y();
        }
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double fx = cx;
        double fy = cy;
        return apply(args.get(0), (x, y) -> {
            double dx = x - fx;
            double dy = y - fy;
            return new double[]{fx + dx * cos - dy * sin, fy + dx * sin + dy * cos};
        }, 1);
    }

    static Value translate(List<Value> args) {
        GeoBuiltins.require(args, 2, "Translate");
        if (!(args.get(1) instanceof Value.VectorV v)) {
            throw new EvalException("Translate erwartet (Objekt, Vektor).");
        }
        return apply(args.get(0), (x, y) -> new double[]{x + v.x(), y + v.y()}, 1);
    }

    static Value dilate(List<Value> args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new EvalException("Dilate erwartet (Objekt, Faktor) oder (Objekt, Faktor, Zentrum).");
        }
        double k = GeoBuiltins.num(args.get(1));
        double cx = 0;
        double cy = 0;
        if (args.size() == 3) {
            Value.PointV c = GeoBuiltins.point(args.get(2), "Dilate");
            requirePlanar(c);
            cx = c.x();
            cy = c.y();
        }
        double fx = cx;
        double fy = cy;
        return apply(args.get(0), (x, y) -> new double[]{fx + k * (x - fx), fy + k * (y - fy)}, Math.abs(k));
    }

    // ---- object mapping --------------------------------------------------

    private static Value apply(Value obj, Plane t, double radiusScale) {
        return switch (obj) {
            case Value.PointV p -> mapPoint(p, t);
            case Value.SegmentV s -> new Value.SegmentV(mapPoint(s.p(), t), mapPoint(s.q(), t));
            case Value.PolygonV poly -> {
                List<Value.PointV> out = new ArrayList<>();
                for (Value.PointV v : poly.vertices()) {
                    out.add(mapPoint(v, t));
                }
                yield new Value.PolygonV(List.copyOf(out));
            }
            case Value.CircleV c -> new Value.CircleV(mapPoint(c.center(), t), c.r() * radiusScale);
            case Value.VectorV v -> mapVector(v, t);
            case Value.LineV l -> mapLine(l, t);
            default -> throw new EvalException("Transformation für dieses Objekt nicht unterstützt.");
        };
    }

    private static Value.PointV mapPoint(Value.PointV p, Plane t) {
        requirePlanar(p);
        double[] r = t.at(p.x(), p.y());
        return Value.PointV.of2D(r[0], r[1]);
    }

    private static Value mapVector(Value.VectorV v, Plane t) {
        if (v.is3D()) {
            throw new EvalException("Transformation für dieses Objekt nicht unterstützt.");
        }
        double bx = v.base() != null ? v.base().x() : 0;
        double by = v.base() != null ? v.base().y() : 0;
        double[] base = t.at(bx, by);
        double[] tip = t.at(bx + v.x(), by + v.y());
        Value.PointV newBase = v.base() != null ? Value.PointV.of2D(base[0], base[1]) : null;
        return new Value.VectorV(tip[0] - base[0], tip[1] - base[1], 0, false, newBase);
    }

    private static Value mapLine(Value.LineV l, Plane t) {
        double[] p1;
        double[] p2;
        if (Math.abs(l.b()) > 1e-12) {
            p1 = t.at(0, l.c() / l.b());
            p2 = t.at(1, (l.c() - l.a()) / l.b());
        } else {
            p1 = t.at(l.c() / l.a(), 0);
            p2 = t.at(l.c() / l.a(), 1);
        }
        double na = p2[1] - p1[1];
        double nb = p1[0] - p2[0];
        return new Value.LineV(na, nb, na * p1[0] + nb * p1[1]);
    }

    private static void requirePlanar(Value.PointV p) {
        if (p.is3D()) {
            throw new EvalException("Transformationen sind nur in 2D verfügbar.");
        }
    }
}
