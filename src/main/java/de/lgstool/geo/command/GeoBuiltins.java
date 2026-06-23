package de.lgstool.geo.command;

import de.lgstool.geo.expr.BuiltinFunctions;
import de.lgstool.geo.expr.Differentiator;
import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.FunctionApplier;
import de.lgstool.geo.expr.SymbolicIntegrator;
import de.lgstool.geo.expr.Value;
import java.util.ArrayList;
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
            Map.entry("schnitt", "intersect"),
            Map.entry("ableitung", "derivative"),
            Map.entry("tangente", "tangent"),
            Map.entry("vieleck", "polygon"),
            Map.entry("winkel", "angle"),
            Map.entry("ellipse", "ellipse"),
            Map.entry("hyperbel", "hyperbola"),
            Map.entry("parabel", "parabola"),
            // curve analysis
            Map.entry("nullstelle", "root"),
            Map.entry("extrempunkt", "extremum"),
            Map.entry("wendepunkt", "inflectionpoint"),
            Map.entry("normale", "normal"),
            // measurements
            Map.entry("länge", "length"),
            Map.entry("laenge", "length"),
            Map.entry("fläche", "area"),
            Map.entry("flaeche", "area"),
            Map.entry("flächeninhalt", "area"),
            Map.entry("umfang", "perimeter"),
            Map.entry("steigung", "slope"),
            // transformations
            Map.entry("spiegle", "reflect"),
            Map.entry("spiegelung", "reflect"),
            Map.entry("drehe", "rotate"),
            Map.entry("drehung", "rotate"),
            Map.entry("verschiebe", "translate"),
            Map.entry("verschiebung", "translate"),
            Map.entry("streckung", "dilate"),
            // constructions
            Map.entry("senkrechte", "perpendicularline"),
            Map.entry("lot", "perpendicularline"),
            Map.entry("parallele", "parallelline"),
            Map.entry("mittelsenkrechte", "perpendicularbisector"),
            Map.entry("winkelhalbierende", "anglebisector"),
            Map.entry("punktauf", "pointon"),
            // conic / geometry analysis
            Map.entry("zentrum", "center"),
            Map.entry("exzentrizität", "eccentricity"),
            Map.entry("exzentrizitaet", "eccentricity"),
            Map.entry("schwerpunkt", "centroid"),
            Map.entry("krümmung", "curvature"),
            Map.entry("kruemmung", "curvature"),
            Map.entry("nullstellen", "roots"));

    private static final Set<String> NAMES = Set.of(
            "point", "vector", "midpoint", "line", "segment", "circle",
            "distance", "plane", "sphere", "intersect",
            "derivative", "integral", "tangent", "polygon", "angle",
            "ellipse", "hyperbola", "parabola",
            "root", "extremum", "inflectionpoint", "normal",
            "length", "area", "perimeter", "slope",
            "reflect", "rotate", "translate", "dilate",
            "perpendicularline", "parallelline", "perpendicularbisector",
            "anglebisector", "pointon", "text",
            "center", "eccentricity", "centroid", "closestpoint", "curvature", "roots");

    @Override
    public boolean has(String name) {
        return NAMES.contains(canonical(name));
    }

    @Override
    public Value call(String name, List<Value> args, FunctionApplier applier) {
        return switch (canonical(name)) {
            case "point" -> point(args);
            case "vector" -> vector(args);
            case "midpoint" -> midpoint(args);
            case "line" -> line(args);
            case "segment" -> segment(args);
            case "circle" -> circle(args);
            case "distance" -> GeoMeasure.distance(args);
            case "plane" -> plane(args);
            case "sphere" -> sphere(args);
            case "intersect" -> GeoIntersections.intersect(args, applier);
            case "derivative" -> derivative(args);
            case "integral" -> integral(args, applier);
            case "tangent" -> tangent(args, applier);
            case "polygon" -> polygon(args);
            case "angle" -> GeoMeasure.angle(args);
            case "ellipse" -> conicFromFoci(args, "Ellipse", true);
            case "hyperbola" -> conicFromFoci(args, "Hyperbola", false);
            case "parabola" -> parabola(args);
            case "root" -> GeoCalculus.root(args, applier);
            case "extremum" -> GeoCalculus.extremum(args, applier);
            case "inflectionpoint" -> GeoCalculus.inflection(args, applier);
            case "normal" -> GeoCalculus.normal(args, applier);
            case "length" -> GeoMeasure.length(args);
            case "area" -> GeoMeasure.area(args);
            case "perimeter" -> GeoMeasure.perimeter(args);
            case "slope" -> GeoMeasure.slope(args);
            case "reflect" -> GeoTransform.reflect(args);
            case "rotate" -> GeoTransform.rotate(args);
            case "translate" -> GeoTransform.translate(args);
            case "dilate" -> GeoTransform.dilate(args);
            case "perpendicularline" -> GeoConstruct.perpendicularLine(args);
            case "parallelline" -> GeoConstruct.parallelLine(args);
            case "perpendicularbisector" -> GeoConstruct.perpendicularBisector(args);
            case "anglebisector" -> GeoConstruct.angleBisector(args);
            case "pointon" -> GeoConstruct.pointOn(args);
            case "center" -> GeoConicAnalysis.center(args);
            case "eccentricity" -> GeoConicAnalysis.eccentricity(args);
            case "centroid" -> GeoConicAnalysis.centroid(args);
            case "closestpoint" -> GeoConicAnalysis.closestPoint(args);
            case "curvature" -> GeoConicAnalysis.curvature(args, applier);
            case "roots" -> GeoConicAnalysis.roots(args, applier);
            case "text" -> text(args);
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
            return new Value.VectorV(b.x() - a.x(), b.y() - a.y(), b.z() - a.z(), a.is3D() || b.is3D(), a);
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
        // Circle through three points (circumcircle).
        if (args.size() == 3 && args.stream().allMatch(v -> v instanceof Value.PointV)) {
            return GeoConicAnalysis.circleThrough(
                    point(args.get(0), "Circle"), point(args.get(1), "Circle"), point(args.get(2), "Circle"));
        }
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

    private static Value polygon(List<Value> args) {
        if (args.size() < 3) {
            throw new EvalException("Polygon braucht mindestens 3 Punkte.");
        }
        List<Value.PointV> verts = new ArrayList<>();
        for (Value v : args) {
            verts.add(point(v, "Polygon"));
        }
        return new Value.PolygonV(List.copyOf(verts));
    }

    private static Value conicFromFoci(List<Value> args, String where, boolean ellipse) {
        require(args, 3, where);
        Value.PointV f1 = point(args.get(0), where);
        Value.PointV f2 = point(args.get(1), where);
        double a = num(args.get(2));
        return ellipse ? GeoConics.ellipse(f1, f2, a) : GeoConics.hyperbola(f1, f2, a);
    }

    private static Value text(List<Value> args) {
        if (args.isEmpty() || !(args.get(0) instanceof Value.TextV t)) {
            throw new EvalException("Text erwartet einen Text in Anführungszeichen.");
        }
        if (args.size() == 2 && args.get(1) instanceof Value.PointV p) {
            return new Value.TextV(t.text(), p);
        }
        return new Value.TextV(t.text(), null);
    }

    private static Value parabola(List<Value> args) {
        require(args, 2, "Parabola");
        Value.PointV focus = point(args.get(0), "Parabola");
        if (!(args.get(1) instanceof Value.LineV directrix)) {
            throw new EvalException("Parabola erwartet (Brennpunkt, Leitgerade).");
        }
        return GeoConics.parabola(focus, directrix);
    }

    // ---- calculus --------------------------------------------------------

    private static Value derivative(List<Value> args) {
        require(args, 1, "Derivative");
        if (!(args.get(0) instanceof Value.FunctionV f) || f.params().size() != 1) {
            throw new EvalException("Derivative erwartet eine Funktion f(x).");
        }
        String var = f.params().get(0);
        return new Value.FunctionV(f.params(), Differentiator.differentiate(f.body(), var));
    }

    private static Value integral(List<Value> args, FunctionApplier applier) {
        if (args.isEmpty() || !(args.get(0) instanceof Value.FunctionV f) || f.params().size() != 1) {
            throw new EvalException("Integral erwartet eine Funktion f(x).");
        }
        // Integral(f): symbolic antiderivative (a function).
        if (args.size() == 1) {
            String var = f.params().get(0);
            return new Value.FunctionV(f.params(), SymbolicIntegrator.integrate(f.body(), var));
        }
        // Integral(f, g, a, b): area between two curves on [a, b].
        if (args.size() == 4 && args.get(1) instanceof Value.FunctionV g && g.params().size() == 1) {
            double a = num(args.get(2));
            double b = num(args.get(3));
            return new Value.NumberV(simpson(applier, f, a, b) - simpson(applier, g, a, b));
        }
        // Integral(f, a, b): definite integral.
        require(args, 3, "Integral");
        return new Value.NumberV(simpson(applier, f, num(args.get(1)), num(args.get(2))));
    }

    private static double simpson(FunctionApplier applier, Value.FunctionV f, double a, double b) {
        int n = 1000;                       // even number of Simpson sub-intervals
        double h = (b - a) / n;
        double sum = sample(applier, f, a) + sample(applier, f, b);
        for (int i = 1; i < n; i++) {
            sum += (i % 2 == 0 ? 2 : 4) * sample(applier, f, a + i * h);
        }
        return sum * h / 3;
    }

    private static Value tangent(List<Value> args, FunctionApplier applier) {
        require(args, 2, "Tangent");
        if (!(args.get(0) instanceof Value.NumberV at) || !(args.get(1) instanceof Value.FunctionV f)
                || f.params().size() != 1) {
            throw new EvalException("Tangent erwartet (x0, f).");
        }
        double x0 = at.v();
        double y0 = sample(applier, f, x0);
        Value.FunctionV df = new Value.FunctionV(f.params(), Differentiator.differentiate(f.body(), f.params().get(0)));
        double slope = sample(applier, df, x0);
        // y = y0 + slope·(x - x0)  →  slope·x − y = slope·x0 − y0
        return new Value.LineV(slope, -1, slope * x0 - y0);
    }

    private static double sample(FunctionApplier applier, Value.FunctionV f, double x) {
        Value v = applier.apply(f, List.of(new Value.NumberV(x)));
        if (v instanceof Value.NumberV n) {
            return n.v();
        }
        throw new EvalException("Funktion liefert keine Zahl.");
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
