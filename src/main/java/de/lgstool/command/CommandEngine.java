package de.lgstool.command;

import de.lgstool.config.I18n;
import de.lgstool.config.Settings;
import de.lgstool.io.LgsStorage;
import de.lgstool.math.ConditionNumber;
import de.lgstool.math.Fraction;
import de.lgstool.math.FractionMatrix;
import de.lgstool.math.Gauss;
import de.lgstool.math.Roman;
import de.lgstool.math.Solution;
import de.lgstool.math.SolveStep;
import de.lgstool.model.AppState;
import de.lgstool.model.Examples;
import de.lgstool.model.Lgs;
import de.lgstool.model.UndoManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Parses and executes the LGS-Tool command language against the {@link AppState}.
 * Pure logic with no JavaFX dependency: UI effects are delegated through {@link UiBridge}.
 * All user-facing text is localized via {@link I18n}.
 */
public final class CommandEngine {

    private AppState state = new AppState();
    private final UndoManager undo = new UndoManager();
    private UiBridge ui;

    public CommandEngine() {
        this(UiBridge.NONE);
    }

    public CommandEngine(UiBridge ui) {
        this.ui = ui;
    }

    public AppState state() {
        return state;
    }

    public void setUi(UiBridge ui) {
        this.ui = ui;
    }

    public UndoManager undoManager() {
        return undo;
    }

    /** Sets a single cell (used by interactive editing in the matrix view); records undo. */
    public CommandResult editCell(String lgsName, int rowOneBased, int colZeroBased, Fraction value) {
        Lgs lgs = state.get(lgsName);
        if (lgs == null) {
            return CommandResult.error(I18n.t("m.noOpenLgs"));
        }
        recordUndo();
        lgs.setCell(rowOneBased, colZeroBased, value);
        ui.lgsChanged(lgsName);
        return CommandResult.ok(I18n.t("m.valueSet"));
    }

    public CommandResult execute(String line, CommandContext ctx) {
        ParsedCommand cmd;
        try {
            cmd = CommandParser.parse(line);
        } catch (CommandException e) {
            return CommandResult.error(e.getMessage());
        }
        try {
            return dispatch(cmd, ctx);
        } catch (CommandException | IllegalArgumentException | ArithmeticException | IOException e) {
            return CommandResult.error(e.getMessage());
        }
    }

    private CommandResult dispatch(ParsedCommand c, CommandContext ctx) throws CommandException, IOException {
        return switch (c.name()) {
            case "create" -> create(c, ctx);
            case "add" -> add(c, ctx);
            case "solve" -> solve(c, ctx);
            case "del" -> del(c, ctx);
            case "swap" -> swap(c, ctx);
            case "scale" -> scale(c, ctx);
            case "combine" -> combine(c, ctx);
            case "det" -> det(c, ctx);
            case "rank" -> rank(c, ctx);
            case "cond" -> cond(c, ctx);
            case "show" -> show(c, ctx);
            case "clear" -> clear(c, ctx);
            case "undo" -> undo(c, ctx);
            case "rename" -> rename(c, ctx);
            case "highlight" -> highlight(c, ctx);
            case "resize" -> resize(c, ctx);
            case "setvar" -> setvar(c, ctx);
            case "save" -> save(c, ctx);
            case "load" -> load(c, ctx);
            case "open" -> open(c, ctx);
            case "close" -> close(c, ctx);
            case "list" -> list(c, ctx);
            case "fit" -> fit(c, ctx);
            case "verify" -> verify(c, ctx);
            case "inverse" -> inverse(c, ctx);
            case "subst" -> subst(c, ctx);
            case "frac" -> display(c, ctx, true);
            case "dec" -> display(c, ctx, false);
            case "animate" -> animate(c, ctx);
            case "axis" -> axis(c, ctx);
            case "rand" -> rand(c, ctx);
            case "redo" -> redo(c, ctx);
            case "rref" -> rref(c, ctx);
            case "ref" -> ref(c, ctx);
            case "transpose" -> transpose(c, ctx);
            case "trace" -> trace(c, ctx);
            case "nullspace" -> nullspace(c, ctx);
            case "cramer" -> cramer(c, ctx);
            case "latex" -> latex(c, ctx);
            case "csv" -> csv(c, ctx);
            case "loadcsv" -> loadcsv(c, ctx);
            case "run" -> runScript(c, ctx);
            case "examples" -> examples(c, ctx);
            case "savelog" -> savelog(c, ctx);
            case "copylog" -> copylog(c, ctx);
            case "saveplot" -> saveplot(c, ctx);
            default -> CommandResult.error(I18n.t("m.unknownCmd", c.name()));
        };
    }

    // ---- [HAUPT] commands ------------------------------------------------

    private CommandResult create(ParsedCommand c, CommandContext ctx) throws CommandException {
        requireScreen(ctx, Screen.HAUPT, "create");
        need(c, 2);
        String name = c.arg(0);
        int[] dim = dims(c.arg(1));
        if (name.isEmpty()) {
            throw new CommandException(I18n.t("m.nameEmpty"));
        }
        if (state.contains(name)) {
            throw new CommandException(I18n.t("m.exists", name));
        }
        recordUndo();
        state.put(Lgs.create(name, dim[0], dim[1]));
        ui.systemsChanged();
        return CommandResult.ok(I18n.t("m.created", name, dim[0], dim[1]));
    }

    private CommandResult load(ParsedCommand c, CommandContext ctx) throws CommandException, IOException {
        requireScreen(ctx, Screen.HAUPT, "load");
        need(c, 1);
        Lgs lgs = LgsStorage.load(c.arg(0));
        if (state.contains(lgs.name())) {
            throw new CommandException(I18n.t("m.exists", lgs.name()));
        }
        recordUndo();
        state.put(lgs);
        ui.systemsChanged();
        return CommandResult.ok(I18n.t("m.loaded", lgs.name()));
    }

    private CommandResult open(ParsedCommand c, CommandContext ctx) throws CommandException {
        requireScreen(ctx, Screen.HAUPT, "open");
        need(c, 1);
        String name = c.arg(0);
        if (!state.contains(name)) {
            throw new CommandException(I18n.t("m.notExists", name));
        }
        state.setOpenName(name);
        ui.openWindow(name);
        return CommandResult.ok(I18n.t("m.opened", name));
    }

    private CommandResult list(ParsedCommand c, CommandContext ctx) throws CommandException {
        requireScreen(ctx, Screen.HAUPT, "list");
        List<String> names = state.names();
        if (names.isEmpty()) {
            return CommandResult.ok(I18n.t("m.noSystems"));
        }
        List<String> out = new ArrayList<>();
        out.add(I18n.t("m.systemsCount", names.size()));
        for (String n : names) {
            out.add("  • " + n);
        }
        return CommandResult.ok(out);
    }

    // ---- shared (HAUPT and LGS) -----------------------------------------

    private CommandResult del(ParsedCommand c, CommandContext ctx) throws CommandException {
        need(c, 1);
        if (ctx.screen() == Screen.HAUPT) {
            String name = c.arg(0);
            if (!state.contains(name)) {
                throw new CommandException(I18n.t("m.notExists", name));
            }
            recordUndo();
            state.remove(name);
            ui.closeWindow(name);
            ui.systemsChanged();
            return CommandResult.ok(I18n.t("m.deletedSystem", name));
        }
        Lgs lgs = lgsOf(ctx);
        int row = reqInt(c.arg(0), I18n.t("w.row"));
        checkRow(lgs, row);
        recordUndo();
        lgs.deleteRow(row);
        ui.lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.deletedRow", row));
    }

    private CommandResult undo(ParsedCommand c, CommandContext ctx) {
        AppState prev = undo.undo(state);
        if (prev == null) {
            return CommandResult.ok(I18n.t("m.nothingUndo"));
        }
        state = prev;
        ui.systemsChanged();
        return CommandResult.ok(I18n.t("m.undone"));
    }

    // ---- [LGS] commands --------------------------------------------------

    private CommandResult add(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (c.argCount() < 1) {
            throw new CommandException(I18n.t("m.addNeedsEq"));
        }
        String eq = c.arg(0);
        int eqIndex = eq.indexOf('=');
        if (eqIndex < 0) {
            throw new CommandException(I18n.t("m.eqNeedsEquals"));
        }
        String lhs = eq.substring(0, eqIndex).trim();
        Fraction rhs = reqFrac(eq.substring(eqIndex + 1), I18n.t("w.rhs"));
        String[] tokens = lhs.isEmpty() ? new String[0] : lhs.split("\\s+");
        if (tokens.length != lgs.width()) {
            throw new CommandException(I18n.t("m.wrongCoeffCount", lgs.width(), tokens.length));
        }
        Fraction[] coeffs = new Fraction[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            coeffs[i] = reqFrac(tokens[i], I18n.t("w.coeff", i + 1));
        }
        Integer rowIdx = null;
        if (c.argCount() >= 2) {
            rowIdx = reqInt(c.arg(1), I18n.t("w.row"));
            if (rowIdx < 1 || rowIdx > lgs.height()) {
                throw new CommandException(I18n.t("m.rowNotExist", rowIdx, lgs.height()));
            }
        }
        recordUndo();
        lgs.addEquation(coeffs, rhs, rowIdx);
        ui.lgsChanged(lgs.name());
        return CommandResult.ok(rowIdx == null ? I18n.t("m.eqEntered") : I18n.t("m.rowOverwritten", rowIdx));
    }

    private CommandResult solve(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (lgs.height() == 0) {
            throw new CommandException(I18n.t("m.noEquations"));
        }
        boolean frac = lgs.isFractionDisplay();
        Gauss.Detailed d = Gauss.solveDetailed(lgs.coefficientMatrix(), lgs.rightHandSide(), true);
        List<String> out = new ArrayList<>();
        if (c.argCount() >= 1) {
            int n = reqInt(c.arg(0), I18n.t("w.steps"));
            List<SolveStep> steps = d.steps();
            int show = Math.min(Math.max(n, 0), steps.size());
            out.add(I18n.t("m.steps", show, steps.size()));
            for (int i = 0; i < show; i++) {
                out.add(I18n.t("m.stepN", i + 1, steps.get(i).description()));
                for (String mLine : steps.get(i).snapshot().toText(frac, lgs.width()).split("\n")) {
                    out.add("   " + mLine);
                }
            }
            out.add(I18n.t("m.result"));
        }
        out.addAll(d.solution().describe(lgs.variableNames(), frac));
        return CommandResult.ok(out);
    }

    private CommandResult swap(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        need(c, 2);
        int i = reqInt(c.arg(0), I18n.t("w.rowI"));
        int j = reqInt(c.arg(1), I18n.t("w.rowJ"));
        checkRow(lgs, i);
        checkRow(lgs, j);
        recordUndo();
        lgs.swapRows(i, j);
        ui.lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.swapDone", Roman.of(i), Roman.of(j)));
    }

    private CommandResult scale(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        need(c, 2);
        int i = reqInt(c.arg(0), I18n.t("w.row"));
        Fraction factor = reqFrac(c.arg(1), I18n.t("w.factor"));
        checkRow(lgs, i);
        if (factor.isZero()) {
            throw new CommandException(I18n.t("m.factorZero"));
        }
        recordUndo();
        lgs.scaleRow(i, factor);
        ui.lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.scaleDone", Roman.of(i), factor.toString()));
    }

    private CommandResult combine(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        need(c, 3);
        int i = reqInt(c.arg(0), I18n.t("w.rowI"));
        Fraction factor = reqFrac(c.arg(1), I18n.t("w.factor"));
        int j = reqInt(c.arg(2), I18n.t("w.rowJ"));
        checkRow(lgs, i);
        checkRow(lgs, j);
        recordUndo();
        lgs.combineRows(i, factor, j);
        ui.lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.combineDone", Roman.of(i), factor.toString(), Roman.of(j)));
    }

    private CommandResult det(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (lgs.height() != lgs.width()) {
            throw new CommandException(I18n.t("m.detNotSquare", lgs.height(), lgs.width()));
        }
        Fraction det = Gauss.determinant(lgs.coefficientMatrix());
        List<String> out = new ArrayList<>();
        out.add(I18n.t("m.detResult", fmt(det, lgs.isFractionDisplay())));
        if (det.isZero()) {
            out.add(I18n.t("m.detZero"));
        }
        return CommandResult.ok(out);
    }

    private CommandResult rank(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (lgs.height() == 0) {
            throw new CommandException(I18n.t("m.noEquations"));
        }
        int rankA = Gauss.rank(lgs.coefficientMatrix());
        int rankAb = Gauss.rank(toAugmented(lgs));
        int n = lgs.width();
        List<String> out = new ArrayList<>();
        out.add(I18n.t("m.rankLine", rankA, rankAb, n));
        if (rankA < rankAb) {
            out.add(I18n.t("m.rankNone"));
        } else if (rankA == n) {
            out.add(I18n.t("m.rankUnique"));
        } else {
            out.add(I18n.t("m.rankInfinite", n - rankA));
        }
        return CommandResult.ok(out);
    }

    private CommandResult cond(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        int norm = ConditionNumber.NORM_INFINITY;
        String label = I18n.t("m.normInf");
        if (c.argCount() >= 1) {
            int n = reqInt(c.arg(0), I18n.t("w.norm"));
            switch (n) {
                case 1 -> {
                    norm = ConditionNumber.NORM_ONE;
                    label = I18n.t("m.norm1");
                }
                case 2 -> {
                    norm = ConditionNumber.NORM_TWO;
                    label = I18n.t("m.norm2");
                }
                default -> throw new CommandException(I18n.t("m.normNeeds"));
            }
        }
        if (lgs.height() != lgs.width()) {
            throw new CommandException(I18n.t("m.condNotSquare"));
        }
        double cond = ConditionNumber.of(lgs.coefficientMatrix(), norm);
        return CommandResult.ok(I18n.t("m.condResult", label, formatDouble(cond)));
    }

    private CommandResult show(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        List<String> out = new ArrayList<>();
        out.add(I18n.t("m.matrixOf", lgs.name(), String.join(" ", lgs.variableNames())));
        if (lgs.height() == 0) {
            out.add(I18n.t("m.emptyParen"));
        } else {
            for (String line : lgs.augmentedMatrix().toText(lgs.isFractionDisplay(), lgs.width()).split("\n")) {
                out.add(line);
            }
        }
        return CommandResult.ok(out);
    }

    private CommandResult clear(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        recordUndo();
        lgs.clear();
        ui.lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.cleared"));
    }

    private CommandResult rename(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        need(c, 1);
        String newName = c.arg(0);
        if (newName.isEmpty()) {
            throw new CommandException(I18n.t("m.newNameEmpty"));
        }
        if (!newName.equals(lgs.name()) && state.contains(newName)) {
            throw new CommandException(I18n.t("m.exists", newName));
        }
        recordUndo();
        state.rename(lgs.name(), newName);
        ui.systemsChanged();
        return CommandResult.ok(I18n.t("m.renamed", newName));
    }

    private CommandResult highlight(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (c.argCount() < 1) {
            throw new CommandException(I18n.t("m.highlightNeedsRow"));
        }
        int row = reqInt(c.arg(0), I18n.t("w.row"));
        checkRow(lgs, row);
        String hex = c.argCount() >= 2 ? c.arg(1) : null;
        if (hex != null && !hex.matches("#?[0-9a-fA-F]{6}")) {
            throw new CommandException(I18n.t("m.badHex", hex));
        }
        ui.highlightRow(lgs.name(), row, hex);
        return CommandResult.ok(I18n.t("m.highlighted", Roman.of(row)));
    }

    private CommandResult resize(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        need(c, 1);
        int[] dim = dims(c.arg(0));
        recordUndo();
        boolean discarded = lgs.resize(dim[0], dim[1]);
        ui.lgsChanged(lgs.name());
        List<String> out = new ArrayList<>();
        out.add(I18n.t("m.resized", dim[0], dim[1]));
        if (discarded) {
            out.add(I18n.t("m.resizeDiscard"));
        }
        return CommandResult.ok(out);
    }

    private CommandResult setvar(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        need(c, 2);
        int col = reqInt(c.arg(0), I18n.t("w.column"));
        String name = c.arg(1);
        if (name.isEmpty()) {
            throw new CommandException(I18n.t("m.varNameEmpty"));
        }
        recordUndo();
        lgs.setVariableName(col, name);
        ui.lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.colRenamed", col, name));
    }

    private CommandResult save(ParsedCommand c, CommandContext ctx) throws CommandException, IOException {
        Lgs lgs = lgsOf(ctx);
        need(c, 1);
        var path = LgsStorage.save(lgs, c.arg(0));
        return CommandResult.ok(I18n.t("m.saved", path.toAbsolutePath().toString()));
    }

    private CommandResult close(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (lgs.name().equals(state.openName())) {
            state.setOpenName(null);
        }
        ui.closeWindow(lgs.name());
        return CommandResult.ok(I18n.t("m.windowClosed"));
    }

    private CommandResult fit(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        ui.fitView(lgs.name());
        return CommandResult.ok(I18n.t("m.centered"));
    }

    private CommandResult verify(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (lgs.height() == 0) {
            throw new CommandException(I18n.t("m.noEquations"));
        }
        Solution s = Gauss.solve(lgs.coefficientMatrix(), lgs.rightHandSide());
        if (s.kind() == Solution.Kind.NONE) {
            return CommandResult.ok(I18n.t("m.noSolutionCheck"));
        }
        Fraction[] sol = s.particular();
        boolean frac = lgs.isFractionDisplay();
        List<String> out = new ArrayList<>();
        out.add(s.kind() == Solution.Kind.INFINITE ? I18n.t("m.verifyInfinite") : I18n.t("m.verifyHeader"));
        boolean allOk = true;
        for (int r = 1; r <= lgs.height(); r++) {
            Fraction[] row = lgs.row(r);
            Fraction lhs = Fraction.ZERO;
            for (int col = 0; col < lgs.width(); col++) {
                lhs = lhs.add(row[col].multiply(sol[col]));
            }
            Fraction rhs = row[lgs.width()];
            boolean okRow = lhs.equals(rhs);
            allOk &= okRow;
            out.add("  " + I18n.t("m.rowLabel", Roman.of(r)) + ": " + fmt(lhs, frac) + " = " + fmt(rhs, frac)
                    + (okRow ? "  ✓" : "  ✗"));
        }
        out.add(allOk ? I18n.t("m.allSatisfied") : I18n.t("m.notAllSatisfied"));
        return CommandResult.ok(out);
    }

    private CommandResult inverse(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (lgs.height() != lgs.width()) {
            throw new CommandException(I18n.t("m.inverseNotSquare"));
        }
        Fraction[][] inv = Gauss.inverse(lgs.coefficientMatrix());
        List<String> out = new ArrayList<>();
        out.add(I18n.t("m.inverseHeader"));
        for (String line : new FractionMatrix(inv).toText(lgs.isFractionDisplay(), -1).split("\n")) {
            out.add(line);
        }
        return CommandResult.ok(out);
    }

    private CommandResult subst(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        need(c, 2);
        String varToken = c.arg(0);
        int index = lgs.variableIndex(varToken);
        if (index < 0) {
            try {
                int col = Integer.parseInt(varToken.trim());
                if (col >= 1 && col <= lgs.width()) {
                    index = col - 1;
                }
            } catch (NumberFormatException ignored) {
                // handled below
            }
        }
        if (index < 0) {
            throw new CommandException(I18n.t("m.varNotFound", varToken));
        }
        Fraction value = reqFrac(c.arg(1), I18n.t("w.value"));
        Fraction[] coeffs = new Fraction[lgs.width()];
        for (int i = 0; i < coeffs.length; i++) {
            coeffs[i] = i == index ? Fraction.ONE : Fraction.ZERO;
        }
        recordUndo();
        lgs.addEquation(coeffs, value, null);
        ui.lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.substAdded", lgs.variableNames()[index], fmt(value, lgs.isFractionDisplay())));
    }

    private CommandResult display(ParsedCommand c, CommandContext ctx, boolean fraction) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        lgs.setFractionDisplay(fraction);
        ui.lgsChanged(lgs.name());
        return CommandResult.ok(fraction ? I18n.t("m.displayFrac") : I18n.t("m.displayDec"));
    }

    private CommandResult animate(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (c.argCount() < 3 || c.argCount() > 4) {
            throw new CommandException(I18n.t("m.animateArgs"));
        }
        int row = reqInt(c.arg(0), I18n.t("w.row"));
        checkRow(lgs, row);
        Fraction from = reqFrac(c.arg(1), I18n.t("w.from"));
        Fraction to = reqFrac(c.arg(2), I18n.t("w.to"));
        double duration = 5.0;
        if (c.argCount() == 4) {
            try {
                duration = Double.parseDouble(c.arg(3).trim());
            } catch (NumberFormatException e) {
                throw new CommandException(I18n.t("m.durNumber"));
            }
            if (duration <= 0) {
                throw new CommandException(I18n.t("m.durPositive"));
            }
        }
        ui.animate(lgs.name(), row, from, to, duration);
        return CommandResult.ok(I18n.t("m.animateStarted", Roman.of(row), from.toString(), to.toString()));
    }

    private CommandResult axis(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (c.argCount() == 0) {
            ui.setAxisRange(lgs.name(), -1, 0, 0);
            return CommandResult.ok(I18n.t("m.axisAuto"));
        }
        if (c.argCount() != 3) {
            throw new CommandException(I18n.t("m.axisArgs"));
        }
        int axisIndex = axisIndexOf(lgs, c.arg(0));
        double min = reqDouble(c.arg(1), I18n.t("w.min"));
        double max = reqDouble(c.arg(2), I18n.t("w.max"));
        if (max <= min) {
            throw new CommandException(I18n.t("m.axisMaxMin"));
        }
        ui.setAxisRange(lgs.name(), axisIndex, min, max);
        return CommandResult.ok(I18n.t("m.axisSet", String.valueOf("xyz".charAt(axisIndex)),
                String.valueOf(min), String.valueOf(max)));
    }

    private CommandResult rand(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (c.argCount() != 2 && c.argCount() != 3) {
            throw new CommandException(I18n.t("m.randArgs"));
        }
        int min = reqInt(c.arg(0), I18n.t("w.min"));
        int max = reqInt(c.arg(1), I18n.t("w.max"));
        if (max < min) {
            throw new CommandException(I18n.t("m.randRange"));
        }
        recordUndo();
        if (c.argCount() == 3) {
            int row = reqInt(c.arg(2), I18n.t("w.row"));
            checkRow(lgs, row);
            fillRow(lgs, row, min, max);
        } else {
            for (int r = 1; r <= lgs.height(); r++) {
                fillRow(lgs, r, min, max);
            }
        }
        ui.lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.randFilled", String.valueOf(min), String.valueOf(max)));
    }

    private static void fillRow(Lgs lgs, int row, int min, int max) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int col = 0; col <= lgs.width(); col++) {
            lgs.setCell(row, col, Fraction.of(rnd.nextInt(min, max + 1)));
        }
    }

    private static int axisIndexOf(Lgs lgs, String token) throws CommandException {
        int limit = Math.min(lgs.width(), 3);
        int byName = lgs.variableIndex(token.trim());
        int index = -1;
        if (byName >= 0) {
            index = byName;
        } else {
            switch (token.trim().toLowerCase(Locale.ROOT)) {
                case "x" -> index = 0;
                case "y" -> index = 1;
                case "z" -> index = 2;
                default -> {
                    try {
                        int n = Integer.parseInt(token.trim());
                        if (n >= 1 && n <= 3) {
                            index = n - 1;
                        }
                    } catch (NumberFormatException ignored) {
                        index = -1;
                    }
                }
            }
        }
        if (index < 0 || index >= limit) {
            throw new CommandException(I18n.t("m.axisUnknown", token, limit >= 3 ? ", z" : ""));
        }
        return index;
    }

    private CommandResult redo(ParsedCommand c, CommandContext ctx) {
        AppState next = undo.redo(state);
        if (next == null) {
            return CommandResult.ok(I18n.t("m.nothingRedo"));
        }
        state = next;
        ui.systemsChanged();
        return CommandResult.ok(I18n.t("m.redone"));
    }

    private CommandResult rref(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (lgs.height() == 0) {
            throw new CommandException(I18n.t("m.noEquations"));
        }
        FractionMatrix rref = new FractionMatrix(Gauss.rref(augmented(lgs), false).matrix());
        return matrixResult("m.rrefHeader", rref, lgs);
    }

    private CommandResult ref(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (lgs.height() == 0) {
            throw new CommandException(I18n.t("m.noEquations"));
        }
        FractionMatrix ref = new FractionMatrix(Gauss.ref(augmented(lgs)));
        return matrixResult("m.refHeader", ref, lgs);
    }

    private CommandResult matrixResult(String headerKey, FractionMatrix m, Lgs lgs) {
        List<String> out = new ArrayList<>();
        out.add(I18n.t(headerKey));
        for (String line : m.toText(lgs.isFractionDisplay(), lgs.width()).split("\n")) {
            out.add(line);
        }
        return CommandResult.ok(out);
    }

    private CommandResult transpose(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        Fraction[][] a = lgs.coefficientMatrix();
        int rows = a.length;
        Fraction[][] t = new Fraction[lgs.width()][rows];
        for (int r = 0; r < rows; r++) {
            for (int col = 0; col < lgs.width(); col++) {
                t[col][r] = a[r][col];
            }
        }
        List<String> out = new ArrayList<>();
        out.add(I18n.t("m.transposeHeader"));
        for (String line : new FractionMatrix(t).toText(lgs.isFractionDisplay(), -1).split("\n")) {
            out.add(line);
        }
        return CommandResult.ok(out);
    }

    private CommandResult trace(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (lgs.height() != lgs.width()) {
            throw new CommandException(I18n.t("m.traceNotSquare"));
        }
        Fraction[][] a = lgs.coefficientMatrix();
        Fraction tr = Fraction.ZERO;
        for (int i = 0; i < lgs.width(); i++) {
            tr = tr.add(a[i][i]);
        }
        return CommandResult.ok(I18n.t("m.traceResult", fmt(tr, lgs.isFractionDisplay())));
    }

    private CommandResult nullspace(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        List<Fraction[]> basis = Gauss.nullspace(lgs.coefficientMatrix());
        if (basis.isEmpty()) {
            return CommandResult.ok(I18n.t("m.nullspaceTrivial"));
        }
        boolean frac = lgs.isFractionDisplay();
        List<String> out = new ArrayList<>();
        out.add(I18n.t("m.nullspaceHeader", basis.size()));
        for (int k = 0; k < basis.size(); k++) {
            out.add("  v" + (k + 1) + " = " + vectorText(basis.get(k), frac));
        }
        return CommandResult.ok(out);
    }

    private CommandResult cramer(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        if (lgs.height() != lgs.width()) {
            throw new CommandException(I18n.t("m.cramerNotRegular"));
        }
        Fraction[][] a = lgs.coefficientMatrix();
        Fraction[] b = lgs.rightHandSide();
        Fraction detA = Gauss.determinant(a);
        if (detA.isZero()) {
            throw new CommandException(I18n.t("m.cramerNotRegular"));
        }
        boolean frac = lgs.isFractionDisplay();
        List<String> out = new ArrayList<>();
        out.add(I18n.t("m.cramerHeader", fmt(detA, frac)));
        String[] names = lgs.variableNames();
        for (int i = 0; i < lgs.width(); i++) {
            Fraction[][] ai = Gauss.copy(a);
            for (int r = 0; r < a.length; r++) {
                ai[r][i] = b[r];
            }
            Fraction detI = Gauss.determinant(ai);
            Fraction xi = detI.divide(detA);
            out.add("  " + names[i] + " = " + fmt(detI, frac) + " / " + fmt(detA, frac) + " = " + fmt(xi, frac));
        }
        return CommandResult.ok(out);
    }

    private CommandResult latex(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = lgsOf(ctx);
        boolean frac = lgs.isFractionDisplay();
        StringBuilder sb = new StringBuilder();
        sb.append("\\left[\\begin{array}{").append("c".repeat(lgs.width())).append("|c}\n");
        for (int r = 1; r <= lgs.height(); r++) {
            Fraction[] row = lgs.row(r);
            StringBuilder line = new StringBuilder();
            for (int col = 0; col <= lgs.width(); col++) {
                line.append(latexNum(row[col], frac));
                if (col < lgs.width()) {
                    line.append(" & ");
                } else {
                    line.append(" \\\\");
                }
            }
            sb.append(line).append('\n');
        }
        sb.append("\\end{array}\\right]");
        ui.copyToClipboard(sb.toString());
        List<String> out = new ArrayList<>();
        for (String line : sb.toString().split("\n")) {
            out.add(line);
        }
        out.add(I18n.t("m.latexCopied"));
        return CommandResult.ok(out);
    }

    private CommandResult csv(ParsedCommand c, CommandContext ctx) throws CommandException, IOException {
        Lgs lgs = lgsOf(ctx);
        need(c, 1);
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", lgs.variableNames())).append(",b\n");
        for (int r = 1; r <= lgs.height(); r++) {
            Fraction[] row = lgs.row(r);
            String[] cells = new String[row.length];
            for (int i = 0; i < row.length; i++) {
                cells[i] = row[i].toString();
            }
            sb.append(String.join(",", cells)).append('\n');
        }
        Path dir = Paths.get("lgs");
        Files.createDirectories(dir);
        Path file = dir.resolve(c.arg(0) + ".csv");
        Files.writeString(file, sb.toString());
        return CommandResult.ok(I18n.t("m.csvSaved", file.toAbsolutePath().toString()));
    }

    private CommandResult loadcsv(ParsedCommand c, CommandContext ctx) throws CommandException, IOException {
        requireScreen(ctx, Screen.HAUPT, "loadcsv");
        need(c, 1);
        String name = c.arg(0);
        Path file = Paths.get("lgs").resolve(name + ".csv");
        if (!Files.exists(file)) {
            throw new CommandException(I18n.t("m.fileNotFound", file.toString()));
        }
        List<String> lines = Files.readAllLines(file);
        List<Fraction[]> dataRows = new ArrayList<>();
        String[] header = null;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(",");
            if (header == null && !isNumeric(parts[0])) {
                header = parts;
                continue;
            }
            Fraction[] row = new Fraction[parts.length];
            for (int i = 0; i < parts.length; i++) {
                row[i] = reqFrac(parts[i], I18n.t("w.value"));
            }
            dataRows.add(row);
        }
        if (dataRows.isEmpty()) {
            throw new CommandException(I18n.t("m.csvBadFormat"));
        }
        int width = dataRows.get(0).length - 1;
        if (width < 1) {
            throw new CommandException(I18n.t("m.csvBadFormat"));
        }
        if (state.contains(name)) {
            throw new CommandException(I18n.t("m.exists", name));
        }
        recordUndo();
        Lgs lgs = Lgs.create(name, width, dataRows.size());
        if (header != null && header.length >= width) {
            for (int col = 1; col <= width; col++) {
                lgs.setVariableName(col, header[col - 1]);
            }
        }
        for (int r = 0; r < dataRows.size(); r++) {
            Fraction[] row = dataRows.get(r);
            for (int col = 0; col < row.length; col++) {
                lgs.setCell(r + 1, col, row[col]);
            }
        }
        state.put(lgs);
        ui.systemsChanged();
        return CommandResult.ok(I18n.t("m.csvLoaded", name));
    }

    private CommandResult runScript(ParsedCommand c, CommandContext ctx) throws CommandException, IOException {
        need(c, 1);
        Path file = Paths.get(c.arg(0));
        if (!Files.exists(file)) {
            file = Paths.get("lgs").resolve(c.arg(0));
        }
        if (!Files.exists(file)) {
            throw new CommandException(I18n.t("m.fileNotFound", c.arg(0)));
        }
        List<String> out = new ArrayList<>();
        out.add(I18n.t("m.runFile", file.getFileName().toString()));
        for (String line : Files.readAllLines(file)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
                continue;
            }
            out.add("> " + trimmed);
            out.addAll(execute(trimmed, ctx).messages());
        }
        return CommandResult.ok(out);
    }

    private CommandResult examples(ParsedCommand c, CommandContext ctx) throws CommandException {
        requireScreen(ctx, Screen.HAUPT, "examples");
        if (c.argCount() == 0) {
            List<String> out = new ArrayList<>();
            out.add(I18n.t("m.examplesHeader"));
            for (String name : Examples.names()) {
                out.add("  • " + name);
            }
            return CommandResult.ok(out);
        }
        String name = c.arg(0);
        Lgs lgs = Examples.build(name);
        if (lgs == null) {
            throw new CommandException(I18n.t("m.exampleUnknown", name));
        }
        if (state.contains(lgs.name())) {
            throw new CommandException(I18n.t("m.exists", lgs.name()));
        }
        recordUndo();
        state.put(lgs);
        ui.systemsChanged();
        return CommandResult.ok(I18n.t("m.exampleLoaded", lgs.name()));
    }

    private CommandResult savelog(ParsedCommand c, CommandContext ctx) throws CommandException {
        need(c, 1);
        Path file = Paths.get("lgs").resolve(c.arg(0) + ".txt");
        ui.saveLog(file.toAbsolutePath().toString());
        return CommandResult.ok(I18n.t("m.logSaved", file.toAbsolutePath().toString()));
    }

    private CommandResult copylog(ParsedCommand c, CommandContext ctx) {
        ui.copyLog();
        return CommandResult.ok(I18n.t("m.logCopied"));
    }

    private CommandResult saveplot(ParsedCommand c, CommandContext ctx) throws CommandException {
        need(c, 1);
        Path file = Paths.get("lgs").resolve(c.arg(0) + ".png");
        ui.savePlot(file.toAbsolutePath().toString());
        return CommandResult.ok(I18n.t("m.plotSaved", file.toAbsolutePath().toString()));
    }

    private static String vectorText(Fraction[] v, boolean frac) {
        String[] parts = new String[v.length];
        for (int i = 0; i < v.length; i++) {
            parts[i] = frac ? v[i].toString() : v[i].toDecimalString(Settings.decimalPrecision);
        }
        return "(" + String.join(", ", parts) + ")";
    }

    private static String latexNum(Fraction f, boolean frac) {
        if (!frac) {
            return f.toDecimalString(Settings.decimalPrecision);
        }
        String s = f.toString();
        if (!s.contains("/")) {
            return s;
        }
        boolean neg = s.startsWith("-");
        if (neg) {
            s = s.substring(1);
        }
        String[] pq = s.split("/");
        return (neg ? "-" : "") + "\\frac{" + pq[0] + "}{" + pq[1] + "}";
    }

    private static boolean isNumeric(String s) {
        try {
            Fraction.parse(s);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static Fraction[][] augmented(Lgs lgs) {
        Fraction[][] a = lgs.coefficientMatrix();
        Fraction[] b = lgs.rightHandSide();
        Fraction[][] aug = new Fraction[a.length][lgs.width() + 1];
        for (int r = 0; r < a.length; r++) {
            System.arraycopy(a[r], 0, aug[r], 0, lgs.width());
            aug[r][lgs.width()] = b[r];
        }
        return aug;
    }

    // ---- helpers ---------------------------------------------------------

    private void recordUndo() {
        undo.record(state);
    }

    private Lgs lgsOf(CommandContext ctx) throws CommandException {
        if (ctx.screen() != Screen.LGS || ctx.lgsName() == null) {
            throw new CommandException(I18n.t("m.onlyLgs"));
        }
        Lgs lgs = state.get(ctx.lgsName());
        if (lgs == null) {
            throw new CommandException(I18n.t("m.noOpenLgs"));
        }
        return lgs;
    }

    private void requireScreen(CommandContext ctx, Screen screen, String cmd) throws CommandException {
        if (ctx.screen() != screen) {
            String where = screen == Screen.HAUPT ? I18n.t("m.mainScreen") : I18n.t("m.lgsWindow");
            throw new CommandException(I18n.t("m.onlyIn", cmd, where));
        }
    }

    private static void need(ParsedCommand c, int n) throws CommandException {
        if (c.argCount() != n) {
            throw new CommandException(I18n.t("m.expectsArgs", c.name(), n, c.argCount()));
        }
    }

    private static void checkRow(Lgs lgs, int row) throws CommandException {
        if (row < 1 || row > lgs.height()) {
            throw new CommandException(I18n.t("m.rowNotExist", row, lgs.height()));
        }
    }

    private static int reqInt(String s, String what) throws CommandException {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new CommandException(I18n.t("m.notInt", what, s.trim()));
        }
    }

    private static Fraction reqFrac(String s, String what) throws CommandException {
        try {
            return Fraction.parse(s);
        } catch (RuntimeException e) {
            throw new CommandException(I18n.t("m.notNumber", what, s.trim()));
        }
    }

    private static double reqDouble(String s, String what) throws CommandException {
        try {
            return Fraction.parse(s).doubleValue();
        } catch (RuntimeException e) {
            throw new CommandException(I18n.t("m.notNumber", what, s.trim()));
        }
    }

    private static int[] dims(String s) throws CommandException {
        String[] parts = s.split("\\*");
        if (parts.length != 2) {
            throw new CommandException(I18n.t("m.dims"));
        }
        return new int[]{reqInt(parts[0], I18n.t("w.width")), reqInt(parts[1], I18n.t("w.height"))};
    }

    private static Fraction[][] toAugmented(Lgs lgs) {
        Fraction[][] a = lgs.coefficientMatrix();
        Fraction[] b = lgs.rightHandSide();
        Fraction[][] aug = new Fraction[a.length][lgs.width() + 1];
        for (int r = 0; r < a.length; r++) {
            System.arraycopy(a[r], 0, aug[r], 0, lgs.width());
            aug[r][lgs.width()] = b[r];
        }
        return aug;
    }

    private static String fmt(Fraction f, boolean asFraction) {
        return asFraction ? f.toString() : f.toDecimalString(10);
    }

    private static String formatDouble(double v) {
        if (Double.isInfinite(v)) {
            return "∞";
        }
        return String.format(Locale.US, "%.6g", v);
    }
}
