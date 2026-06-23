package de.lgstool.geo.command;

import de.lgstool.config.I18n;
import de.lgstool.geo.expr.BuiltinFunctions;
import de.lgstool.geo.expr.CasBuiltins;
import de.lgstool.geo.expr.ComplexBuiltins;
import de.lgstool.geo.expr.CompositeBuiltins;
import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.Expr;
import de.lgstool.geo.expr.ListBuiltins;
import de.lgstool.geo.expr.MathBuiltins;
import de.lgstool.geo.expr.MatrixBuiltins;
import de.lgstool.geo.expr.Parser;
import de.lgstool.geo.expr.StatsBuiltins;
import de.lgstool.geo.expr.Value;
import de.lgstool.geo.io.GeoStorage;
import de.lgstool.geo.model.Construction;
import de.lgstool.geo.model.GeoObject;
import de.lgstool.geo.model.GeoType;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private static final Pattern SAVE = Pattern.compile("(?i)(?:save|speichern)\\s*\\(\\s*(\\w+)\\s*\\)");
    private static final Pattern LOAD = Pattern.compile("(?i)(?:load|laden)\\s*\\(\\s*(\\w+)\\s*\\)");

    private Construction construction = newConstruction();
    private final Deque<Construction> undoStack = new ArrayDeque<>();
    private final Deque<Construction> redoStack = new ArrayDeque<>();
    private GeoView view = GeoView.NONE;

    private static Construction newConstruction() {
        BuiltinFunctions builtins = new CompositeBuiltins(new GeoBuiltins(),
                new CompositeBuiltins(new MatrixBuiltins(),
                        new CompositeBuiltins(new StatsBuiltins(),
                                new CompositeBuiltins(new CasBuiltins(),
                                        new CompositeBuiltins(new ComplexBuiltins(),
                                                new CompositeBuiltins(new ListBuiltins(), new MathBuiltins()))))));
        return new Construction(builtins);
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
            return GeoResult.error(I18n.t("geo.emptyInput"));
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
        Matcher save = SAVE.matcher(s);
        if (save.matches()) {
            return save(save.group(1));
        }
        Matcher load = LOAD.matcher(s);
        if (load.matches()) {
            return load(load.group(1));
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
            return GeoResult.error(I18n.t("geo.missingDef"));
        }
        Matcher fn = FUNCTION_DEF.matcher(left);
        if (fn.matches() && allSimpleParams(fn.group(2))) {
            return defineFunction(fn.group(1), splitParams(fn.group(2)), right);
        }
        // A plain identifier on the left is an assignment — unless it is a plotting
        // variable x/y (not already an object) and the right side actually uses x or y,
        // in which case "y = x^2" is an implicit curve (GeoGebra-style). "y = 4" stays a
        // plain assignment so existing number/object semantics are preserved.
        boolean reservedPlotVar = (left.equals("x") || left.equals("y"))
                && !construction.contains(left) && referencesPlotVar(right);
        if (IDENT.matcher(left).matches() && !reservedPlotVar) {
            Expr ast = Parser.parse(right);
            if (asSlider(ast) != null) {
                return defineSlider(left, right, (Expr.Call) ast);
            }
            if (asLocus(ast) != null) {
                return defineLocus(left, right, (Expr.Call) ast);
            }
            snapshot();
            GeoObject obj = construction.define(left, right, ast, null);
            return committed(obj);
        }
        // Otherwise treat "LHS = RHS" as an implicit equation in x and y.
        return defineImplicit(left, right);
    }

    /** Builds an implicit curve {@code LHS - RHS = 0} (must involve the plot variables x/y). */
    private GeoResult defineImplicit(String left, String right) {
        Expr lhs = Parser.parse(left);
        Expr rhs = Parser.parse(right);
        Expr diff = new Expr.Bin('-', lhs, rhs);
        Set<String> names = new HashSet<>();
        Expr.collectNames(diff, names);
        if (!names.contains("x") && !names.contains("y")) {
            return GeoResult.error(I18n.t("geo.notEquationXY"));
        }
        snapshot();
        String label = left + " = " + right;
        GeoObject obj = construction.defineLiteral(autoName(false), label, new Value.ImplicitV(diff, label));
        return committed(obj);
    }

    /** Whether an expression source references the plot variables x or y (parse-tolerant). */
    private static boolean referencesPlotVar(String source) {
        try {
            Set<String> names = new HashSet<>();
            Expr.collectNames(Parser.parse(source), names);
            return names.contains("x") || names.contains("y");
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean allSimpleParams(String inside) {
        for (String p : inside.split(",")) {
            if (!IDENT.matcher(p.trim()).matches()) {
                return false;
            }
        }
        return true;
    }

    private GeoResult defineFunction(String name, List<String> params, String right) {
        Expr ast = Parser.parse(right);
        snapshot();
        GeoObject obj = construction.define(name, name + "(" + String.join(", ", params) + ") = " + right, ast, params);
        view.structureChanged();
        return GeoResult.ok(obj.name() + "(" + String.join(", ", params) + ") = " + right);
    }

    private GeoResult defineBare(String s) {
        Expr ast = Parser.parse(s);
        if (asLocus(ast) != null) {
            return defineLocus(autoName(false), s, (Expr.Call) ast);
        }
        Value value = construction.evaluate(ast);
        String name = autoName(GeoType.of(value) == GeoType.POINT);
        snapshot();
        GeoObject obj = construction.define(name, s, ast, null);
        return committed(obj);
    }

    private GeoResult defineLocus(String name, String source, Expr.Call call) {
        if (call.args().size() != 2
                || !(call.args().get(0) instanceof Expr.Ref pRef)
                || !(call.args().get(1) instanceof Expr.Ref sRef)) {
            return GeoResult.error(I18n.t("geo.locusArgs"));
        }
        GeoObject p = construction.get(pRef.name());
        GeoObject s = construction.get(sRef.name());
        if (p == null || !(p.value() instanceof Value.PointV)) {
            return GeoResult.error(I18n.t("geo.locusNotPoint", pRef.name()));
        }
        if (s == null || !s.isSlider()) {
            return GeoResult.error(I18n.t("geo.locusNotSlider", sRef.name()));
        }
        List<Value.PointV> pts = new ArrayList<>();
        int steps = 200;
        double min = s.sliderMin();
        double max = s.sliderMax();
        Construction work = construction.copy();
        for (int i = 0; i <= steps; i++) {
            double val = min + (max - min) * i / steps;
            try {
                work.setFreeValue(sRef.name(), new Value.NumberV(val));
                if (work.get(pRef.name()).value() instanceof Value.PointV pv && !pv.is3D()
                        && Double.isFinite(pv.x()) && Double.isFinite(pv.y())) {
                    pts.add(pv);
                }
            } catch (RuntimeException ignored) {
                // skip parameter values where the construction is undefined
            }
        }
        if (pts.isEmpty()) {
            return GeoResult.error(I18n.t("geo.locusEmpty"));
        }
        snapshot();
        GeoObject obj = construction.defineLiteral(name, source, new Value.LocusV(List.copyOf(pts)));
        return committed(obj);
    }

    private GeoResult defineSlider(String name, String source, Expr.Call call) {
        List<Value> args = call.args().stream().map(construction::evaluate).toList();
        if (args.size() < 2 || args.size() > 3) {
            return GeoResult.error(I18n.t("geo.sliderArgs"));
        }
        double min = GeoBuiltins.num(args.get(0));
        double max = GeoBuiltins.num(args.get(1));
        double step = args.size() == 3 ? GeoBuiltins.num(args.get(2)) : (max - min) / 100.0;
        if (max <= min) {
            return GeoResult.error(I18n.t("geo.sliderMaxMin"));
        }
        snapshot();
        construction.define(name, source, new Expr.Num(min), null);
        construction.get(name).configureSlider(min, max, step);
        view.structureChanged();
        return GeoResult.ok(I18n.t("geo.sliderCreated", name, GeoFormat.num(min), GeoFormat.num(max)));
    }

    // ---- meta commands ---------------------------------------------------

    private GeoResult delete(String name) {
        if (!construction.contains(name)) {
            return GeoResult.error(I18n.t("geo.notExists", name));
        }
        snapshot();
        List<String> removed = construction.delete(name);
        view.structureChanged();
        return GeoResult.ok(I18n.t("geo.deleted", String.join(", ", removed)));
    }

    private GeoResult clear() {
        if (construction.size() == 0) {
            return GeoResult.ok(I18n.t("geo.alreadyEmpty"));
        }
        snapshot();
        construction.clear();
        view.structureChanged();
        return GeoResult.ok(I18n.t("geo.cleared"));
    }

    private GeoResult list() {
        if (construction.size() == 0) {
            return GeoResult.ok(I18n.t("geo.noObjects"));
        }
        List<String> out = new ArrayList<>();
        out.add(I18n.t("geo.objectCount", construction.size()));
        for (GeoObject o : construction.objects()) {
            out.add("  " + o.name() + " = " + GeoFormat.value(o.value()));
        }
        return GeoResult.ok(out);
    }

    private GeoResult save(String name) {
        try {
            var path = GeoStorage.save(exportLines(), name);
            return GeoResult.ok(I18n.t("geo.saved", path.toAbsolutePath().toString()));
        } catch (IOException e) {
            return GeoResult.error(I18n.t("geo.saveFailed", e.getMessage()));
        }
    }

    private GeoResult load(String name) {
        List<String> lines;
        try {
            lines = GeoStorage.load(name);
        } catch (IOException e) {
            return GeoResult.error(I18n.t("geo.loadFailed", e.getMessage()));
        }
        GeoEngine temp = new GeoEngine();
        for (String l : lines) {
            temp.execute(l);
        }
        snapshot();
        construction = temp.construction;
        view.structureChanged();
        return GeoResult.ok(I18n.t("geo.loaded", name, construction.size()));
    }

    /** The editable input line that recreates an object (used by the algebra view). */
    public String editableLine(GeoObject o) {
        return replayLine(o);
    }

    /**
     * Renames an object, rewriting every reference to it. Implemented by replaying the
     * whole construction with the identifier substituted, so dependents stay intact;
     * colours are preserved by name.
     */
    public GeoResult rename(String oldName, String newName) {
        String nn = newName == null ? "" : newName.trim();
        if (!construction.contains(oldName)) {
            return GeoResult.error(I18n.t("geo.notExists", oldName));
        }
        if (!IDENT.matcher(nn).matches()) {
            return GeoResult.error(I18n.t("geo.invalidName", nn));
        }
        if (nn.equals(oldName)) {
            return GeoResult.ok(nn);
        }
        if (construction.contains(nn)) {
            return GeoResult.error(I18n.t("geo.exists", nn));
        }
        Map<String, String> colors = new HashMap<>();
        for (GeoObject o : construction.objects()) {
            colors.put(o.name().equals(oldName) ? nn : o.name(), o.colorHex());
        }
        List<String> lines = new ArrayList<>();
        for (String l : exportLines()) {
            lines.add(replaceIdentifier(l, oldName, nn));
        }
        snapshot();
        GeoEngine temp = new GeoEngine();
        for (String l : lines) {
            temp.execute(l);
        }
        construction = temp.construction;
        for (GeoObject o : construction.objects()) {
            String c = colors.get(o.name());
            if (c != null) {
                o.setColorHex(c);
            }
        }
        view.structureChanged();
        return GeoResult.ok(I18n.t("geo.renamed", oldName, nn));
    }

    private static String replaceIdentifier(String line, String oldName, String newName) {
        return Pattern.compile("\\b" + Pattern.quote(oldName) + "\\b")
                .matcher(line).replaceAll(Matcher.quoteReplacement(newName));
    }

    private List<String> exportLines() {
        List<String> out = new ArrayList<>();
        for (GeoObject o : construction.objects()) {
            out.add(replayLine(o));
        }
        return out;
    }

    private static String replayLine(GeoObject o) {
        if (o.kind() == GeoObject.Kind.FUNCTION) {
            return o.source();
        }
        if (o.isSlider()) {
            return o.name() + " = Slider(" + GeoFormat.num(o.sliderMin()) + ", "
                    + GeoFormat.num(o.sliderMax()) + ", " + GeoFormat.num(o.sliderStep()) + ")";
        }
        if (o.value() instanceof Value.ImplicitV) {
            // Implicit curves are auto-named bare equations ("x^2 + y^2 = 1"); replay the
            // source as-is so re-execution recreates the implicit curve (not "name = …=…").
            return o.source();
        }
        if (o.kind() == GeoObject.Kind.FREE) {
            // Points/numbers store their (possibly dragged) current value; other free
            // objects (e.g. a locus) replay from their original source command.
            if (o.value() instanceof Value.PointV || o.value() instanceof Value.NumberV) {
                return o.name() + " = " + GeoFormat.value(o.value());
            }
            return o.name() + " = " + o.source();
        }
        return o.name() + " = " + o.source();
    }

    private GeoResult undo() {
        if (undoStack.isEmpty()) {
            return GeoResult.ok(I18n.t("geo.nothingUndo"));
        }
        redoStack.push(construction.copy());
        construction = undoStack.pop();
        view.structureChanged();
        return GeoResult.ok(I18n.t("geo.undone"));
    }

    private GeoResult redo() {
        if (redoStack.isEmpty()) {
            return GeoResult.ok(I18n.t("geo.nothingRedo"));
        }
        undoStack.push(construction.copy());
        construction = redoStack.pop();
        view.structureChanged();
        return GeoResult.ok(I18n.t("geo.redone"));
    }

    // ---- UI-driven mutation (drag / slider) ------------------------------

    /** Records a single undo snapshot at the start of a drag/slider interaction. */
    public void beginInteraction() {
        snapshot();
    }

    /** Live update of a free value (no undo snapshot) — used during a drag. */
    public void setFreeValueLive(String name, Value value) {
        construction.setFreeValue(name, value);
        view.valuesChanged();
    }

    /** Commits a free-value change (records undo) — used at drag end / on slider release. */
    public void setFreeValue(String name, Value value) {
        snapshot();
        construction.setFreeValue(name, value);
        view.valuesChanged();
    }

    // ---- helpers ---------------------------------------------------------

    private GeoResult committed(GeoObject obj) {
        view.structureChanged();
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
        throw new EvalException(I18n.t("geo.noNames"));
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

    private static Expr.Call asLocus(Expr ast) {
        if (ast instanceof Expr.Call c) {
            String n = c.name().toLowerCase(Locale.ROOT);
            if (n.equals("locus") || n.equals("ortslinie")) {
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
                    throw new EvalException(I18n.t("geo.badParam", t));
                }
                params.add(t);
            }
        }
        if (params.isEmpty()) {
            throw new EvalException(I18n.t("geo.needParam"));
        }
        return params;
    }

    private static int topLevelEquals(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '(' || ch == '{') {
                depth++;
            } else if (ch == ')' || ch == '}') {
                depth--;
            } else if (ch == '=' && depth == 0 && isLoneEquals(s, i)) {
                return i;
            }
        }
        return -1;
    }

    /** Whether the {@code =} at {@code i} is a lone assignment, not part of {@code == <= >= !=}. */
    private static boolean isLoneEquals(String s, int i) {
        char prev = i > 0 ? s.charAt(i - 1) : ' ';
        char next = i + 1 < s.length() ? s.charAt(i + 1) : ' ';
        return prev != '<' && prev != '>' && prev != '!' && prev != '=' && next != '=';
    }
}
