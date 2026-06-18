package de.lgstool.geo.command;

import de.lgstool.geo.expr.Value;
import java.util.Locale;

/** Renders a {@link Value} as a short human-readable string for logs and the algebra view. */
public final class GeoFormat {

    private GeoFormat() {
    }

    public static String value(Value v) {
        return switch (v) {
            case Value.NumberV n -> num(n.v());
            case Value.PointV p -> p.is3D()
                    ? "(" + num(p.x()) + ", " + num(p.y()) + ", " + num(p.z()) + ")"
                    : "(" + num(p.x()) + ", " + num(p.y()) + ")";
            case Value.VectorV vec -> vec.is3D()
                    ? "→(" + num(vec.x()) + ", " + num(vec.y()) + ", " + num(vec.z()) + ")"
                    : "→(" + num(vec.x()) + ", " + num(vec.y()) + ")";
            case Value.LineV l -> num(l.a()) + "·x + " + num(l.b()) + "·y = " + num(l.c());
            case Value.Line3DV l -> "Gerade " + value(l.point()) + " + t·" + value(l.dir());
            case Value.SegmentV s -> "Strecke " + value(s.p()) + "–" + value(s.q());
            case Value.CircleV c -> "Kreis M=" + value(c.center()) + ", r=" + num(c.r());
            case Value.FunctionV f -> "Funktion";
            case Value.PlaneV p -> num(p.a()) + "·x + " + num(p.b()) + "·y + " + num(p.c()) + "·z = " + num(p.d());
            case Value.SphereV s -> "Kugel M=" + value(s.center()) + ", r=" + num(s.r());
        };
    }

    /** Formats a double with up to 4 decimals, trimming trailing zeros. */
    public static String num(double d) {
        if (d == Math.rint(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        String s = String.format(Locale.US, "%.4f", d);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
