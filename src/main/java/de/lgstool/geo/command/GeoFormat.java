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
            case Value.BooleanV b -> b.value() ? "true" : "false";
            case Value.ComplexV z -> complex(z);
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
            case Value.PolygonV poly -> "Vieleck (" + poly.vertices().size() + " Ecken)";
            case Value.ConicV con -> "Kegelschnitt";
            case Value.LocusV loc -> "Ortslinie (" + loc.points().size() + " Punkte)";
            case Value.MatrixV m -> matrix(m);
            case Value.ListV l -> list(l);
            case Value.TextV t -> "“" + t.text() + "”";
            case Value.ImplicitV im -> im.label();
        };
    }

    /** A re-parseable brace literal {@code {{a, b}, {c, d}}} (used to redefine an edited matrix). */
    public static String matrixLiteral(Value.MatrixV m) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < m.rowCount(); i++) {
            sb.append(i == 0 ? "{" : ", {");
            for (int j = 0; j < m.colCount(); j++) {
                sb.append(j == 0 ? "" : ", ").append(m.rows()[i][j]);
            }
            sb.append("}");
        }
        return sb.append("}").toString();
    }

    private static String complex(Value.ComplexV z) {
        if (Math.abs(z.im()) < 1e-12) {
            return num(z.re());
        }
        if (Math.abs(z.re()) < 1e-12) {
            return num(z.im()) + "i";
        }
        return num(z.re()) + (z.im() < 0 ? " - " : " + ") + num(Math.abs(z.im())) + "i";
    }

    private static String matrix(Value.MatrixV m) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < m.rowCount(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            for (int j = 0; j < m.colCount(); j++) {
                if (j > 0) {
                    sb.append(", ");
                }
                sb.append(m.rows()[i][j]);
            }
        }
        return sb.append("]").toString();
    }

    private static String list(Value.ListV l) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < l.items().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(value(l.items().get(i)));
        }
        return sb.append("}").toString();
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
