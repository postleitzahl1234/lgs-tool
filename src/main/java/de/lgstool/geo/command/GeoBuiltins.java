package de.lgstool.geo.command;

import de.lgstool.geo.expr.BuiltinFunctions;
import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.Value;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Geometry constructors for the expression language: {@code Point}, {@code Vector},
 * {@code Midpoint}, {@code Line}, {@code Segment}, {@code Circle}, {@code Distance},
 * {@code Plane}, {@code Sphere} and {@code Intersect}. Operates purely on
 * {@link Value}s (no model dependency). German command aliases are accepted.
 *
 * <p>Intersection maths lives in {@link GeoIntersections} to keep this class focused.
 */
public final class GeoBuiltins implements BuiltinFunctions {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("punkt", "point"),
            Map.entry("vektor", "vector"),
            Map.entry("mittelpunkt", "midpoint"),
            Map.entry("gerade", "line"),
            Map.entry("strecke", "segment"),
            Map.entry("kreis", "circle"),
            Map.entry("abstand", "distance"),
            Map.entry("ebene", "plane"),
            Map.entry("kugel", "sphere"),
            Map.entry("schnitt", "intersect"));

    private static final Set<String> NAMES = Set.of(
            "point", "vector", "midpoint", "line", "segment", "circle",
            "distance", "plane", "sphere", "intersect");

    @Override
    public boolean has(String name) {
        return NAMES.contains(canonical(name));
    }

    @Override
    public Value call(String name, List<Value> args) {
        return switch (canonical(name)) {
            case "point" -> point(args);
            case "vector" -> vector(args);
            case "midpoint" -> midpoint(args);
            case "line" -> line(args);
            case "segment" -> segment(args);
            case "circle" -> circle(args);
            case "distance" -> distance(args);
            case "plane" -> plane(args);
            case "sphere" -> sphere(args);
            case "intersect" -> GeoIntersections.intersect(args);
            default -> throw new EvalException("Unbekannte Funktion '" + name + "'.");
        };
    }

    private static String canonical(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return ALIASES.getOrDefault(lower, lower);
    }

    // ---- constructors ----------------------------------------------------

    private static Value point(List<Value> args) {
        if (args.size() == 2) {
            return Value.PointV.of2D(num(args.get(0)), num(args.get(1)));
        }
        if (args.size() == 3) {
            return Value.PointV.of3D(num(args.get(0)), num(args.get(1)), num(args.get(2)));
        }
        throw new EvalException("Point erwartet 2 oder 3 Zahlen.");
    }

    private static Value vector(List<Value> args) {
        if (args.size() == 1 && args.get(0) instanceof Value.PointV p) {
            return new Value.VectorV(p.x(), p.y(), p.z(), p.is3D());
        }
        if (args.size() == 2 && args.get(0) instanceof Value.PointV a && args.get(1) instanceof Value.PointV b) {
            return new Value.VectorV(b.x() - a.x(), b.y() - a.y(), b.z() - a.z(), a.is3D() || b.is3D());
        }
        if (args.size() == 2) {
            return new Value.VectorV(num(args.get(0)), num(args.get(1)), 0, false);
        }
        if (args.size() == 3) {
            return new Value.VectorV(num(args.get(0)), num(args.get(1)), num(args.get(2)), true);
        }
        throw new EvalException("Vector erwartet Koordinaten oder zwei Punkte.");
    }

    private static Value midpoint(List<Value> args) {
        require(args, 2, "Midpoint");
        Value.PointV a = point(args.get(0), "Midpoint");
        Value.PointV b = point(args.get(1), "Midpoint");
        boolean d3 = a.is3D() || b.is3D();
        return new Value.PointV((a.x() + b.x()) / 2, (a.y() + b.y()) / 2, (a.z() + b.z()) / 2, d3);
    }

    private static Value line(List<Value> args) {
        require(args, 2, "Line");
        if (args.get(0) instanceof Value.PointV a && args.get(1) instanceof Value.PointV b) {
            if (a.is3D() || b.is3D()) {
                return new Value.Line3DV(a, new Value.VectorV(b.x() - a.x(), b.y() - a.y(), b.z() - a.z(), true));
            }
            double na = b.y() - a.y();
            double nb = a.x() - b.x();
            return new Value.LineV(na, nb, na * a.x() + nb * a.y());
        }
        if (args.get(0) instanceof Value.PointV p && args.get(1) instanceof Value.VectorV v) {
            if (p.is3D() || v.is3D()) {
                return new Value.Line3DV(p, v);
            }
            double na = v.y();
            double nb = -v.x();
            return new Value.LineV(na, nb, na * p.x() + nb * p.y());
        }
        throw new EvalException("Line erwartet zwei Punkte oder Punkt und Richtungsvektor.");
    }

    private static Value segment(List<Value> args) {
        require(args, 2, "Segment");
        return new Value.SegmentV(point(args.get(0), "Segment"), point(args.get(1), "Segment"));
    }

    private static Value circle(List<Value> args) {
        require(args, 2, "Circle");
        Value.PointV center = point(args.get(0), "Circle");
        if (center.is3D()) {
            throw new EvalException("Circle ist 2D — für 3D nutze Sphere.");
        }
        double r;
        if (args.get(1) instanceof Value.NumberV n) {
            r = n.v();
        } else if (args.get(1) instanceof Value.PointV on) {
            r = Math.hypot(on.x() - center.x(), on.y() - center.y());
        } else {
            throw new EvalException("Circle erwartet Radius oder Punkt auf dem Kreis.");
        }
        if (r < 0) {
            throw new EvalException("Radius darf nicht negativ sein.");
        }
        return new Value.CircleV(center, r);
    }

    private static Value distance(List<Value> args) {
        require(args, 2, "Distance");
        Value.PointV a = point(args.get(0), "Distance");
        Value.PointV b = point(args.get(1), "Distance");
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double dz = b.z() - a.z();
        return new Value.NumberV(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    private static Value plane(List<Value> args) {
        if (args.size() == 3 && args.stream().allMatch(v -> v instanceof Value.PointV)) {
            Value.PointV a = (Value.PointV) args.get(0);
            Value.PointV b = (Value.PointV) args.get(1);
            Value.PointV c = (Value.PointV) args.get(2);
            double[] ab = {b.x() - a.x(), b.y() - a.y(), b.z() - a.z()};
            double[] ac = {c.x() - a.x(), c.y() - a.y(), c.z() - a.z()};
            double[] n = cross(ab, ac);
            if (n[0] == 0 && n[1] == 0 && n[2] == 0) {
                throw new EvalException("Die drei Punkte liegen auf einer Geraden.");
            }
            return new Value.PlaneV(n[0], n[1], n[2], n[0] * a.x() + n[1] * a.y() + n[2] * a.z());
        }
        if (args.size() == 2 && args.get(0) instanceof Value.PointV p && args.get(1) instanceof Value.VectorV n) {
            return new Value.PlaneV(n.x(), n.y(), n.z(), n.x() * p.x() + n.y() * p.y() + n.z() * p.z());
        }
        throw new EvalException("Plane erwartet drei Punkte oder Punkt und Normalenvektor.");
    }

    private static Value sphere(List<Value> args) {
        require(args, 2, "Sphere");
        Value.PointV center = point(args.get(0), "Sphere");
        double r;
        if (args.get(1) instanceof Value.NumberV n) {
            r = n.v();
        } else if (args.get(1) instanceof Value.PointV on) {
            r = Math.sqrt(sq(on.x() - center.x()) + sq(on.y() - center.y()) + sq(on.z() - center.z()));
        } else {
            throw new EvalException("Sphere erwartet Radius oder Punkt auf der Kugel.");
        }
        if (r < 0) {
            throw new EvalException("Radius darf nicht negativ sein.");
        }
        return new Value.SphereV(new Value.PointV(center.x(), center.y(), center.z(), true), r);
    }

    // ---- helpers ---------------------------------------------------------

    static Value.PointV point(Value v, String where) {
        if (v instanceof Value.PointV p) {
            return p;
        }
        throw new EvalException(where + " erwartet einen Punkt.");
    }

    static double num(Value v) {
        if (v instanceof Value.NumberV n) {
            return n.v();
        }
        throw new EvalException("Zahl erwartet.");
    }

    static void require(List<Value> args, int n, String where) {
        if (args.size() != n) {
            throw new EvalException(where + " erwartet " + n + " Argument(e), erhalten " + args.size() + ".");
        }
    }

    private static double[] cross(double[] u, double[] v) {
        return new double[]{
                u[1] * v[2] - u[2] * v[1],
                u[2] * v[0] - u[0] * v[2],
                u[0] * v[1] - u[1] * v[0]};
    }

    private static double sq(double x) {
        return x * x;
    }
}
