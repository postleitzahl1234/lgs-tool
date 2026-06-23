package de.lgstool.geo.command;

import de.lgstool.geo.expr.Expr;
import de.lgstool.geo.expr.Value;
import de.lgstool.math.Fraction;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Renders any {@link Value} (and the {@link Expr} body of a function) as a LaTeX string
 * for the action/LaTeX log and clipboard export. Exact {@link Fraction} entries become
 * {@code \frac{}{}}; doubles use the same trimmed formatting as the algebra view.
 */
public final class Latex {

    private Latex() {
    }

    public static String of(Value v) {
        return switch (v) {
            case Value.NumberV n -> num(n.v());
            case Value.BooleanV b -> b.value() ? "\\text{true}" : "\\text{false}";
            case Value.ComplexV z -> GeoFormat.value(z).replace("i", "\\mathrm{i}");
            case Value.PointV p -> p.is3D()
                    ? "\\left(" + num(p.x()) + ",\\ " + num(p.y()) + ",\\ " + num(p.z()) + "\\right)"
                    : "\\left(" + num(p.x()) + ",\\ " + num(p.y()) + "\\right)";
            case Value.VectorV vec -> column(vec.is3D()
                    ? new String[]{num(vec.x()), num(vec.y()), num(vec.z())}
                    : new String[]{num(vec.x()), num(vec.y())});
            case Value.LineV l -> num(l.a()) + "x " + plus(l.b()) + "y = " + num(l.c());
            case Value.Line3DV l -> of(l.point()) + " + t\\," + of(l.dir());
            case Value.SegmentV s -> "\\overline{" + of(s.p()) + of(s.q()) + "}";
            case Value.CircleV c -> circle(c.center(), c.r());
            case Value.FunctionV f -> expr(f.body());
            case Value.PlaneV p -> num(p.a()) + "x " + plus(p.b()) + "y " + plus(p.c()) + "z = " + num(p.d());
            case Value.SphereV s -> circle(s.center(), s.r());
            case Value.PolygonV poly -> polygon(poly);
            case Value.ConicV con -> conic(con);
            case Value.LocusV loc -> "\\text{Ortslinie}\\ (" + loc.points().size() + ")";
            case Value.MatrixV m -> matrix(m);
            case Value.ListV list -> list(list);
            case Value.TextV t -> "\\text{" + t.text() + "}";
            case Value.ImplicitV im -> expr(im.expr()) + " = 0";
        };
    }

    /** Renders an expression tree (used for function bodies). */
    public static String expr(Expr e) {
        return switch (e) {
            case Expr.Num n -> n.text() != null ? n.text() : num(n.value());
            case Expr.Str s -> "\\text{" + s.value() + "}";
            case Expr.Ref r -> r.name();
            case Expr.Unary u -> u.op() == '-' ? "-" + expr(u.operand()) : expr(u.operand());
            case Expr.Bin b -> bin(b);
            case Expr.Call c -> call(c);
            case Expr.Tuple t -> {
                StringJoiner j = new StringJoiner(",\\ ", "\\left(", "\\right)");
                t.components().forEach(comp -> j.add(expr(comp)));
                yield j.toString();
            }
            case Expr.ListLit l -> {
                StringJoiner j = new StringJoiner(",\\ ", "\\left\\{", "\\right\\}");
                l.items().forEach(item -> j.add(expr(item)));
                yield j.toString();
            }
        };
    }

    // ---- expression helpers ----------------------------------------------

    private static String bin(Expr.Bin b) {
        return switch (b.op()) {
            case '/' -> "\\frac{" + expr(b.left()) + "}{" + expr(b.right()) + "}";
            case '*' -> group(b.left()) + " \\cdot " + group(b.right());
            case '^' -> group(b.left()) + "^{" + expr(b.right()) + "}";
            case '+' -> expr(b.left()) + " + " + expr(b.right());
            case '<' -> expr(b.left()) + " < " + expr(b.right());
            case '>' -> expr(b.left()) + " > " + expr(b.right());
            case 'l' -> expr(b.left()) + " \\le " + expr(b.right());
            case 'g' -> expr(b.left()) + " \\ge " + expr(b.right());
            case 'e' -> expr(b.left()) + " = " + expr(b.right());
            case 'n' -> expr(b.left()) + " \\ne " + expr(b.right());
            case '&' -> expr(b.left()) + " \\wedge " + expr(b.right());
            case '|' -> expr(b.left()) + " \\vee " + expr(b.right());
            default -> expr(b.left()) + " - " + group(b.right());
        };
    }

    private static String group(Expr e) {
        return e instanceof Expr.Bin ? "\\left(" + expr(e) + "\\right)" : expr(e);
    }

    private static String call(Expr.Call c) {
        String name = c.name().toLowerCase(Locale.ROOT);
        String arg = c.args().isEmpty() ? "" : expr(c.args().get(0));
        return switch (name) {
            case "sqrt" -> "\\sqrt{" + arg + "}";
            case "sin", "cos", "tan", "ln", "log", "exp", "abs" -> "\\" + (name.equals("abs") ? "left|" + arg + "\\right|"
                    : name + "\\left(" + arg + "\\right)");
            default -> {
                StringJoiner j = new StringJoiner(",\\ ", c.name() + "\\left(", "\\right)");
                c.args().forEach(a -> j.add(expr(a)));
                yield j.toString();
            }
        };
    }

    // ---- value helpers ---------------------------------------------------

    private static String matrix(Value.MatrixV m) {
        StringBuilder sb = new StringBuilder("\\begin{pmatrix}");
        for (int i = 0; i < m.rowCount(); i++) {
            for (int j = 0; j < m.colCount(); j++) {
                sb.append(j == 0 ? "" : " & ").append(frac(m.rows()[i][j]));
            }
            sb.append(i == m.rowCount() - 1 ? "" : " \\\\ ");
        }
        return sb.append("\\end{pmatrix}").toString();
    }

    private static String column(String[] entries) {
        StringBuilder sb = new StringBuilder("\\begin{pmatrix}");
        for (int i = 0; i < entries.length; i++) {
            sb.append(entries[i]).append(i == entries.length - 1 ? "" : " \\\\ ");
        }
        return sb.append("\\end{pmatrix}").toString();
    }

    private static String list(Value.ListV list) {
        StringJoiner j = new StringJoiner(",\\ ", "\\left\\{", "\\right\\}");
        list.items().forEach(item -> j.add(of(item)));
        return j.toString();
    }

    private static String polygon(Value.PolygonV poly) {
        StringJoiner j = new StringJoiner(",\\ ", "\\text{Polygon}\\left(", "\\right)");
        poly.vertices().forEach(p -> j.add(of(p)));
        return j.toString();
    }

    private static String circle(Value.PointV c, double r) {
        return "\\left(x" + minus(c.x()) + "\\right)^2 + \\left(y" + minus(c.y()) + "\\right)^2 = " + num(r * r);
    }

    private static String conic(Value.ConicV c) {
        return num(c.a()) + "x^2 " + plus(c.b()) + "xy " + plus(c.c()) + "y^2 "
                + plus(c.d()) + "x " + plus(c.e()) + "y " + plus(c.f()) + " = 0";
    }

    private static String frac(Fraction f) {
        if (f.isInteger()) {
            return f.numerator().toString();
        }
        Fraction a = f.abs();
        String body = "\\frac{" + a.numerator() + "}{" + a.denominator() + "}";
        return f.signum() < 0 ? "-" + body : body;
    }

    /** A signed term joiner: " + 3" or " - 3" (for building polynomials/equations). */
    private static String plus(double v) {
        return v < 0 ? "- " + num(-v) : "+ " + num(v);
    }

    /** " - 3" / " + 3" for (x - c) style factors. */
    private static String minus(double v) {
        return v < 0 ? " + " + num(-v) : " - " + num(v);
    }

    private static String num(double v) {
        return GeoFormat.num(v);
    }
}
