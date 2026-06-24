package de.lgstool.geo.command;

import de.lgstool.config.I18n;
import de.lgstool.geo.expr.BuiltinFunctions;
import de.lgstool.geo.expr.CasBuiltins;
import de.lgstool.geo.expr.ComplexBuiltins;
import de.lgstool.geo.expr.CompositeBuiltins;
import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.Evaluator;
import de.lgstool.geo.expr.Expr;
import de.lgstool.geo.expr.ListBuiltins;
import de.lgstool.geo.expr.MathBuiltins;
import de.lgstool.geo.expr.MatrixBuiltins;
import de.lgstool.geo.expr.NameResolver;
import de.lgstool.geo.expr.Parser;
import de.lgstool.geo.expr.StatsBuiltins;
import de.lgstool.geo.expr.Value;
import de.lgstool.geo.io.GeoStorage;
import de.lgstool.math.Fraction;
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
    private static final Pattern CREATE = Pattern.compile(
            "(?i)(?:create|erstelle)\\s*\\(\\s*([A-Za-z][A-Za-z0-9_]*)\\s*,\\s*(\\d+)\\s*\\*\\s*(\\d+)\\s*\\)");
    private static final Pattern RAND = Pattern.compile(
            "(?i)(?:rand|zufall)\\s*\\(\\s*(?:([A-Za-z][A-Za-z0-9_]*)\\s*,\\s*)?(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)");

    private Construction construction = newConstruction();
    private final Deque<Construction> undoStack = new ArrayDeque<>();
    private final Deque<Construction> redoStack = new ArrayDeque<>();
    private GeoView view = GeoView.NONE;
    private String lastMatrix;   // most recently created/defined matrix, the default target for rand(lo, hi)

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
        Matcher create = CREATE.matcher(s);
        if (create.matches()) {
            return createMatrix(create.group(1),
                    Integer.parseInt(create.group(2)), Integer.parseInt(create.group(3)));
        }
        Matcher rand = RAND.matcher(s);
        if (rand.matches()) {
            return randomFill(rand.group(1),
                    Integer.parseInt(rand.group(2)), Integer.parseInt(rand.group(3)));
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
        // variable x/y/z (not already an object) and the right side actually uses a plot
        // variable, in which case "y = x^2" is a curve and "z = x + y" is a plane
        // (GeoGebra-style). "y = 4" stays a plain assignment (number) so existing semantics hold.
        boolean reservedPlotVar = (left.equals("x") || left.equals("y") || left.equals("z"))
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
            if (obj.value() instanceof Value.MatrixV) {
                lastMatrix = obj.name();
            }
            return committed(obj);
        }
        // Otherwise treat "LHS = RHS" as an implicit equation in x and y.
        return defineImplicit(left, right);
    }

    /**
     * Builds the geometric object for an equation {@code LHS = RHS}. An equation that is
     * linear and involves {@code z} becomes a 3D plane; otherwise it is an implicit curve
     * in x/y (drawn in 2D). Must involve at least one plot variable x/y/z.
     */
    private GeoResult defineImplicit(String left, String right) {
        Expr lhs = Parser.parse(left);
        Expr rhs = Parser.parse(right);
        Expr diff = new Expr.Bin('-', lhs, rhs);
        Set<String> names = new HashSet<>();
        Expr.collectNames(diff, names);
        if (!names.contains("x") && !names.contains("y") && !names.contains("z")) {
            return GeoResult.error(I18n.t("geo.notEquationXY"));
        }
        String label = left + " = " + right;
        // A linear equation that uses z is a plane a·x + b·y + c·z = d (shown in 3D).
        if (names.contains("z")) {
            Value.PlaneV plane = asPlane(diff);
            if (plane != null) {
                snapshot();
                return committed(construction.defineLiteral(autoName(false), label, plane));
            }
        }
        snapshot();
        GeoObject obj = construction.defineLiteral(autoName(false), label, new Value.ImplicitV(diff, label));
        return committed(obj);
    }

    /** Extracts a plane from {@code diff = a·x + b·y + c·z + e}; null if not linear in x,y,z. */
    private Value.PlaneV asPlane(Expr diff) {
        double e = sampleXYZ(diff, 0, 0, 0);
        double a = sampleXYZ(diff, 1, 0, 0) - e;
        double b = sampleXYZ(diff, 0, 1, 0) - e;
        double c = sampleXYZ(diff, 0, 0, 1) - e;
        double probe = sampleXYZ(diff, 2, 3, 5);
        double predicted = e + 2 * a + 3 * b + 5 * c;
        boolean linear = Double.isFinite(e) && Double.isFinite(a) && Double.isFinite(b) && Double.isFinite(c)
                && Double.isFinite(probe) && Math.abs(probe - predicted) < 1e-7;
        if (!linear || (Math.abs(a) < 1e-12 && Math.abs(b) < 1e-12 && Math.abs(c) < 1e-12)) {
            return null;
        }
        return new Value.PlaneV(a, b, c, -e);   // a·x + b·y + c·z = -e
    }

    /** Evaluates an expression with x, y, z bound to the given values (NaN on failure). */
    private double sampleXYZ(Expr e, double x, double y, double z) {
        NameResolver scope = name -> switch (name) {
            case "x" -> new Value.NumberV(x);
            case "y" -> new Value.NumberV(y);
            case "z" -> new Value.NumberV(z);
            default -> construction.valueOf(name);
        };
        try {
            return new Evaluator(scope, construction.builtins()).eval(e) instanceof Value.NumberV n
                    ? n.v() : Double.NaN;
        } catch (RuntimeException ex) {
            return Double.NaN;
        }
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

    /**
     * {@code create(name, variables*rows)}: a new editable linear system — an augmented
     * matrix with {@code rows} equations and {@code variables + 1} columns (the extra
     * right-hand-side scalar column). All entries start at 0; capped at 100×100.
     */
    private GeoResult createMatrix(String name, int variables, int rows) {
        if (variables < 1 || rows < 1) {
            return GeoResult.error(I18n.t("geo.createSize"));
        }
        if (variables > 100 || rows > 100) {
            return GeoResult.error(I18n.t("geo.createMax"));
        }
        int cols = variables + 1;   // coefficients + the right-hand-side column
        Fraction[][] m = new Fraction[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                m[r][c] = Fraction.ZERO;
            }
        }
        snapshot();
        Value.MatrixV mv = new Value.MatrixV(m);
        GeoObject o = construction.defineLiteral(name, GeoFormat.matrixLiteral(mv), mv);
        o.setLinearSystem(true);
        lastMatrix = name;
        return committed(o);
    }

    /** {@code rand([name,] lo, hi)}: fills a matrix with random integers in [lo, hi]. */
    private GeoResult randomFill(String name, int lo, int hi) {
        String target = name != null ? name : lastMatrix;
        if (target == null) {
            return GeoResult.error(I18n.t("geo.noMatrix"));
        }
        GeoObject existing = construction.get(target);
        if (existing == null || !(existing.value() instanceof Value.MatrixV cur)) {
            return GeoResult.error(I18n.t("geo.notMatrix", target));
        }
        int low = Math.min(lo, hi);
        int span = Math.abs(hi - lo) + 1;
        Fraction[][] m = new Fraction[cur.rowCount()][cur.colCount()];
        for (int r = 0; r < cur.rowCount(); r++) {
            for (int c = 0; c < cur.colCount(); c++) {
                m[r][c] = Fraction.of(low + (long) (Math.random() * span));
            }
        }
        snapshot();
        Value.MatrixV mv = new Value.MatrixV(m);
        GeoObject o = construction.defineLiteral(target, GeoFormat.matrixLiteral(mv), mv);
        lastMatrix = target;
        return committed(o);
    }

    /** The editable input line that recreates an object (used by the algebra view). */
    public String editableLine(GeoObject o) {
        return replayLine(o);
    }

    /**
     * Edits a single matrix cell while preserving source expressions in the other
     * cells. A new bare identifier (for example {@code rhs}) is created as a free
     * number with the cell's current value, so the matrix can immediately depend on
     * it and the user may later redefine that number as a slider.
     */
    public GeoResult editMatrixCell(String matrixName, int rowZeroBased, int colZeroBased, String entry) {
        try {
            return editMatrixCellOrThrow(matrixName, rowZeroBased, colZeroBased, entry);
        } catch (EvalException | IllegalArgumentException | ArithmeticException e) {
            return GeoResult.error(e.getMessage());
        }
    }

    private GeoResult editMatrixCellOrThrow(String matrixName, int rowZeroBased, int colZeroBased, String entry) {
        GeoObject current = construction.get(matrixName);
        if (current == null) {
            return GeoResult.error(I18n.t("geo.notExists", matrixName));
        }
        if (!(current.value() instanceof Value.MatrixV matrix)) {
            return GeoResult.error(I18n.t("geo.notMatrix", matrixName));
        }
        if (rowZeroBased < 0 || rowZeroBased >= matrix.rowCount()
                || colZeroBased < 0 || colZeroBased >= matrix.colCount()) {
            return GeoResult.error("Matrixzelle existiert nicht.");
        }
        String normalized = entry == null || entry.isBlank() ? "0" : entry.trim();
        String previousText = GeoFormat.matrixEntryText(current.source(), matrix, rowZeroBased, colZeroBased).trim();
        if (previousText.equals(normalized)) {
            return GeoResult.ok(current.name() + " = " + GeoFormat.value(current.value()));
        }

        String literal = GeoFormat.matrixLiteralWithSource(
                current.source(), matrix, rowZeroBased, colZeroBased, normalized);
        snapshot();
        if (isNewBareVariable(normalized)) {
            Fraction initial = matrix.rows()[rowZeroBased][colZeroBased];
            construction.define(normalized, initial.toString(), Parser.parse(initial.toString()), null);
        }
        GeoObject obj = construction.define(matrixName, literal, Parser.parse(literal), null);
        lastMatrix = matrixName;
        return committed(obj);
    }

    private boolean isNewBareVariable(String entry) {
        return IDENT.matcher(entry).matches() && !construction.contains(entry);
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
