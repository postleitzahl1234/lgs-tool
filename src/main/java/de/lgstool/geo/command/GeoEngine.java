package de.lgstool.geo.command;

import de.lgstool.geo.expr.CompositeBuiltins;
import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.Expr;
import de.lgstool.geo.expr.MathBuiltins;
import de.lgstool.geo.expr.Parser;
import de.lgstool.geo.expr.Value;
import de.lgstool.geo.model.Construction;
import de.lgstool.geo.model.GeoObject;
import de.lgstool.geo.model.GeoType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and runs GeoGebra-style input lines against a {@link Construction}.
 * Three input forms are recognised:
 * <ul>
 *   <li>assignment {@code A = (1,2)} / {@code c = Circle(A, 3)} / {@code s = Slider(0, 5)};</li>
 *   <li>function definition {@code f(x) = x^2};</li>
 *   <li>a bare command/expression {@code Line(A, B)} (auto-named).</li>
 * </ul>
 * Plus the meta-commands {@code undo}, {@code redo}, {@code clear}, {@code list} and
 * {@code Delete(name)}. Multi-level undo snapshots the whole construction.
 */
public final class GeoEngine {

    private static final int UNDO_LIMIT = 50;
    private static final Pattern IDENT = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
    private static final Pattern FUNCTION_DEF = Pattern.compile("([A-Za-z][A-Za-z0-9_]*)\\s*\\(([^)]*)\\)");
    private static final Pattern DELETE = Pattern.compile(
            "(?i)(?:delete|del|lösche|loesche)\\s*\\(\\s*([A-Za-z][A-Za-z0-9_]*)\\s*\\)");

    private Construction construction = newConstruction();
    private final Deque<Construction> undoStack = new ArrayDeque<>();
    private final Deque<Construction> redoStack = new ArrayDeque<>();
    private GeoView view = GeoView.NONE;

    private static Construction newConstruction() {
        return new Construction(new CompositeBuiltins(new GeoBuiltins(), new MathBuiltins()));
    }

    public Construction construction() {
        return construction;
    }

    public void setView(GeoView view) {
        this.view = view == null ? GeoView.NONE : view;
    }

    public GeoResult execute(String line) {
        String s = line == null ? "" : line.trim();
        if (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        if (s.isEmpty()) {
            return GeoResult.error("Leere Eingabe.");
        }
        try {
            return dispatch(s);
        } catch (EvalException | IllegalArgumentException | ArithmeticException e) {
            return GeoResult.error(e.getMessage());
        }
    }

    private GeoResult dispatch(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        switch (lower) {
            case "undo" -> {
                return undo();
            }
            case "redo" -> {
                return redo();
            }
            case "clear" -> {
                return clear();
            }
            case "list" -> {
                return list();
            }
            default -> {
                // fall through to the structured forms
            }
        }
        Matcher del = DELETE.matcher(s);
        if (del.matches()) {
            return delete(del.group(1));
        }
        int eq = topLevelEquals(s);
        if (eq >= 0) {
            return defineWithLabel(s.substring(0, eq).trim(), s.substring(eq + 1).trim());
        }
        return defineBare(s);
    }

    // ---- definition forms ------------------------------------------------

    private GeoResult defineWithLabel(String left, String right) {
        if (right.isEmpty()) {
            return GeoResult.error("Fehlende Definition nach '='.");
        }
        Matcher fn = FUNCTION_DEF.matcher(left);
        if (fn.matches()) {
            return defineFunction(fn.group(1), splitParams(fn.group(2)), right);
        }
        if (!IDENT.matcher(left).matches()) {
            return GeoResult.error("Ungültiger Name links von '=': '" + left + "'.");
        }
        Expr ast = Parser.parse(right);
        if (asSlider(ast) != null) {
            return defineSlider(left, right, (Expr.Call) ast);
        }
        snapshot();
        GeoObject obj = construction.define(left, right, ast, null);
        return committed(obj);
    }

    private GeoResult defineFunction(String name, List<String> params, String right) {
        Expr ast = Parser.parse(right);
        snapshot();
        GeoObject obj = construction.define(name, name + "(" + String.join(", ", params) + ") = " + right, ast, params);
        return GeoResult.ok(obj.name() + "(" + String.join(", ", params) + ") = " + right);
    }

    private GeoResult defineBare(String s) {
        Expr ast = Parser.parse(s);
        Value value = construction.evaluate(ast);
        String name = autoName(GeoType.of(value) == GeoType.POINT);
        snapshot();
        GeoObject obj = construction.define(name, s, ast, null);
        return committed(obj);
    }

    private GeoResult defineSlider(String name, String source, Expr.Call call) {
        List<Value> args = call.args().stream().map(construction::evaluate).toList();
        if (args.size() < 2 || args.size() > 3) {
            return GeoResult.error("Slider erwartet (min, max) oder (min, max, schritt).");
        }
        double min = GeoBuiltins.num(args.get(0));
        double max = GeoBuiltins.num(args.get(1));
        double step = args.size() == 3 ? GeoBuiltins.num(args.get(2)) : (max - min) / 100.0;
        if (max <= min) {
            return GeoResult.error("Slider: max muss größer als min sein.");
        }
        snapshot();
        construction.define(name, source, new Expr.Num(min), null);
        construction.get(name).configureSlider(min, max, step);
        return GeoResult.ok(name + " = " + GeoFormat.num(min) + "  (Slider " + GeoFormat.num(min) + "…" + GeoFormat.num(max) + ")");
    }

    // ---- meta commands ---------------------------------------------------

    private GeoResult delete(String name) {
        if (!construction.contains(name)) {
            return GeoResult.error("Objekt '" + name + "' existiert nicht.");
        }
        snapshot();
        List<String> removed = construction.delete(name);
        view.constructionChanged();
        return GeoResult.ok("Gelöscht: " + String.join(", ", removed));
    }

    private GeoResult clear() {
        if (construction.size() == 0) {
            return GeoResult.ok("Bereits leer.");
        }
        snapshot();
        construction.clear();
        view.constructionChanged();
        return GeoResult.ok("Zeichnung geleert.");
    }

    private GeoResult list() {
        if (construction.size() == 0) {
            return GeoResult.ok("Keine Objekte.");
        }
        List<String> out = new ArrayList<>();
        out.add(construction.size() + " Objekt(e):");
        for (GeoObject o : construction.objects()) {
            out.add("  " + o.name() + " = " + GeoFormat.value(o.value()));
        }
        return GeoResult.ok(out);
    }

    private GeoResult undo() {
        if (undoStack.isEmpty()) {
            return GeoResult.ok("Nichts rückgängig zu machen.");
        }
        redoStack.push(construction.copy());
        construction = undoStack.pop();
        view.constructionChanged();
        return GeoResult.ok("Rückgängig.");
    }

    private GeoResult redo() {
        if (redoStack.isEmpty()) {
            return GeoResult.ok("Nichts zu wiederholen.");
        }
        undoStack.push(construction.copy());
        construction = redoStack.pop();
        view.constructionChanged();
        return GeoResult.ok("Wiederhergestellt.");
    }

    // ---- UI-driven mutation (drag / slider) ------------------------------

    /** Live update of a free value (no undo snapshot) — used during a drag. */
    public void setFreeValueLive(String name, Value value) {
        construction.setFreeValue(name, value);
        view.constructionChanged();
    }

    /** Commits a free-value change (records undo) — used at drag end / on slider release. */
    public void setFreeValue(String name, Value value) {
        snapshot();
        construction.setFreeValue(name, value);
        view.constructionChanged();
    }

    // ---- helpers ---------------------------------------------------------

    private GeoResult committed(GeoObject obj) {
        view.constructionChanged();
        return GeoResult.ok(obj.name() + " = " + GeoFormat.value(obj.value()));
    }

    private void snapshot() {
        undoStack.push(construction.copy());
        if (undoStack.size() > UNDO_LIMIT) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    private String autoName(boolean upper) {
        String alphabet = upper ? "ABCDEFGHIJKLMNOPQRSTUVWXYZ" : "abcdefghijklmnopqrstuvwxyz";
        for (int round = 0; round < 1000; round++) {
            for (int i = 0; i < alphabet.length(); i++) {
                String candidate = round == 0 ? String.valueOf(alphabet.charAt(i)) : "" + alphabet.charAt(i) + round;
                if (!construction.contains(candidate)) {
                    return candidate;
                }
            }
        }
        throw new EvalException("Keine freien Namen mehr.");
    }

    private static Expr.Call asSlider(Expr ast) {
        if (ast instanceof Expr.Call c) {
            String n = c.name().toLowerCase(Locale.ROOT);
            if (n.equals("slider") || n.equals("schieberegler")) {
                return c;
            }
        }
        return null;
    }

    private static List<String> splitParams(String inside) {
        List<String> params = new ArrayList<>();
        for (String p : inside.split(",")) {
            String t = p.trim();
            if (!t.isEmpty()) {
                if (!IDENT.matcher(t).matches()) {
                    throw new EvalException("Ungültiger Parametername: '" + t + "'.");
                }
                params.add(t);
            }
        }
        if (params.isEmpty()) {
            throw new EvalException("Funktion braucht mindestens einen Parameter.");
        }
        return params;
    }

    private static int topLevelEquals(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
            } else if (ch == '=' && depth == 0) {
                return i;
            }
        }
        return -1;
    }
}
