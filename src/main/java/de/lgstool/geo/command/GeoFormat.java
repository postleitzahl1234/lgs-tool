package de.lgstool.geo.command;

import de.lgstool.geo.expr.Value;
import java.util.ArrayList;
import java.util.List;
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
        return matrixLiteralWith(m, -1, -1, null);
    }

    /**
     * The brace literal of {@code m} with the single entry at ({@code row}, {@code col})
     * replaced by {@code entry} (a blank entry becomes 0). Pass {@code row = col = -1} for
     * the unchanged literal. Used by the algebra view to redefine a matrix after a cell edit.
     */
    public static String matrixLiteralWith(Value.MatrixV m, int row, int col, String entry) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < m.rowCount(); i++) {
            sb.append(i == 0 ? "{" : ", {");
            for (int j = 0; j < m.colCount(); j++) {
                String value = (i == row && j == col)
                        ? normalizedMatrixEntry(entry)
                        : m.rows()[i][j].toString();
                sb.append(j == 0 ? "" : ", ").append(value);
            }
            sb.append("}");
        }
        return sb.append("}").toString();
    }

    /**
     * Like {@link #matrixLiteralWith(Value.MatrixV, int, int, String)}, but keeps the
     * source text of untouched cells when the matrix was originally defined as a brace
     * literal. This preserves entries such as {@code a} or {@code rhs} when another
     * cell is edited in the algebra view.
     */
    public static String matrixLiteralWithSource(String source, Value.MatrixV m, int row, int col, String entry) {
        List<List<String>> cells = matrixSourceCells(source, m.rowCount(), m.colCount());
        if (cells == null) {
            return matrixLiteralWith(m, row, col, entry);
        }
        cells.get(row).set(col, normalizedMatrixEntry(entry));
        return matrixLiteral(cells);
    }

    /** Returns the editable text for one matrix entry, preferring source text over the current value. */
    public static String matrixEntryText(String source, Value.MatrixV m, int row, int col) {
        List<List<String>> cells = matrixSourceCells(source, m.rowCount(), m.colCount());
        return cells == null ? m.rows()[row][col].toString() : cells.get(row).get(col);
    }

    private static String matrixLiteral(List<List<String>> cells) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < cells.size(); i++) {
            sb.append(i == 0 ? "{" : ", {");
            List<String> row = cells.get(i);
            for (int j = 0; j < row.size(); j++) {
                sb.append(j == 0 ? "" : ", ").append(row.get(j));
            }
            sb.append("}");
        }
        return sb.append("}").toString();
    }

    private static String normalizedMatrixEntry(String entry) {
        return entry == null || entry.isBlank() ? "0" : entry.trim();
    }

    private static List<List<String>> matrixSourceCells(String source, int rows, int cols) {
        List<String> rowTexts = braceItems(source);
        if (rowTexts == null || rowTexts.size() != rows) {
            return null;
        }
        List<List<String>> parsed = new ArrayList<>();
        for (String rowText : rowTexts) {
            List<String> entries = braceItems(rowText);
            if (entries == null || entries.size() != cols) {
                return null;
            }
            parsed.add(entries);
        }
        return parsed;
    }

    private static List<String> braceItems(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim();
        if (s.length() < 2 || s.charAt(0) != '{' || s.charAt(s.length() - 1) != '}') {
            return null;
        }
        String body = s.substring(1, s.length() - 1).trim();
        List<String> items = new ArrayList<>();
        if (body.isEmpty()) {
            return items;
        }
        int depth = 0;
        boolean inString = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == '"' && !isEscaped(body, i)) {
                inString = !inString;
            }
            if (!inString) {
                if (ch == ',' && depth == 0) {
                    items.add(current.toString().trim());
                    current.setLength(0);
                    continue;
                }
                if (ch == '(' || ch == '{') {
                    depth++;
                } else if (ch == ')' || ch == '}') {
                    depth--;
                    if (depth < 0) {
                        return null;
                    }
                }
            }
            current.append(ch);
        }
        if (depth != 0 || inString) {
            return null;
        }
        items.add(current.toString().trim());
        return items;
    }

    private static boolean isEscaped(String s, int index) {
        int backslashes = 0;
        for (int i = index - 1; i >= 0 && s.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes % 2 == 1;
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
