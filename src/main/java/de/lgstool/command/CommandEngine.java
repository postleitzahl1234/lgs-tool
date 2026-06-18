package de.lgstool.command;

import de.lgstool.config.I18n;
import de.lgstool.math.Fraction;
import de.lgstool.model.AppState;
import de.lgstool.model.Lgs;
import de.lgstool.model.UndoManager;
import java.io.IOException;

/**
 * Parses and executes the LGS-Tool command language against the {@link AppState}.
 * Pure logic with no JavaFX dependency: UI effects are delegated through {@link UiBridge}
 * and all user-facing text is localized via {@link I18n}.
 *
 * <p>This class owns the {@link EngineContext} (shared mutable state) and routes each
 * parsed command to one of the cohesive handler groups: {@link SystemCommands},
 * {@link EditCommands}, {@link AnalysisCommands} and {@link VizIoCommands}.
 */
public final class CommandEngine {

    private final EngineContext core;
    private final SystemCommands system;
    private final EditCommands edit;
    private final AnalysisCommands analysis;
    private final VizIoCommands vizIo;

    public CommandEngine() {
        this(UiBridge.NONE);
    }

    public CommandEngine(UiBridge ui) {
        this.core = new EngineContext(ui);
        this.system = new SystemCommands(core);
        this.edit = new EditCommands(core);
        this.analysis = new AnalysisCommands(core);
        this.vizIo = new VizIoCommands(core, this::execute);
    }

    public AppState state() {
        return core.state();
    }

    public void setUi(UiBridge ui) {
        core.setUi(ui);
    }

    public UndoManager undoManager() {
        return core.undo();
    }

    /** Sets a single cell (used by interactive editing in the matrix view); records undo. */
    public CommandResult editCell(String lgsName, int rowOneBased, int colZeroBased, Fraction value) {
        Lgs lgs = core.state().get(lgsName);
        if (lgs == null) {
            return CommandResult.error(I18n.t("m.noOpenLgs"));
        }
        core.recordUndo();
        lgs.setCell(rowOneBased, colZeroBased, value);
        core.ui().lgsChanged(lgsName);
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
            // ---- system / lifecycle ----
            case "create" -> system.create(c, ctx);
            case "load" -> system.load(c, ctx);
            case "open" -> system.open(c, ctx);
            case "list" -> system.list(c, ctx);
            case "del" -> system.del(c, ctx);
            case "undo" -> system.undo(c, ctx);
            case "redo" -> system.redo(c, ctx);
            case "rename" -> system.rename(c, ctx);
            case "close" -> system.close(c, ctx);
            case "examples" -> system.examples(c, ctx);
            case "loadcsv" -> system.loadcsv(c, ctx);

            // ---- editing a single system ----
            case "add" -> edit.add(c, ctx);
            case "swap" -> edit.swap(c, ctx);
            case "scale" -> edit.scale(c, ctx);
            case "combine" -> edit.combine(c, ctx);
            case "clear" -> edit.clear(c, ctx);
            case "resize" -> edit.resize(c, ctx);
            case "setvar" -> edit.setvar(c, ctx);
            case "subst" -> edit.subst(c, ctx);
            case "rand" -> edit.rand(c, ctx);
            case "frac" -> edit.display(c, ctx, true);
            case "dec" -> edit.display(c, ctx, false);

            // ---- read-only analysis ----
            case "solve" -> analysis.solve(c, ctx);
            case "det" -> analysis.det(c, ctx);
            case "rank" -> analysis.rank(c, ctx);
            case "cond" -> analysis.cond(c, ctx);
            case "show" -> analysis.show(c, ctx);
            case "verify" -> analysis.verify(c, ctx);
            case "inverse" -> analysis.inverse(c, ctx);
            case "rref" -> analysis.rref(c, ctx);
            case "ref" -> analysis.ref(c, ctx);
            case "transpose" -> analysis.transpose(c, ctx);
            case "trace" -> analysis.trace(c, ctx);
            case "nullspace" -> analysis.nullspace(c, ctx);
            case "cramer" -> analysis.cramer(c, ctx);

            // ---- view effects and I/O ----
            case "highlight" -> vizIo.highlight(c, ctx);
            case "fit" -> vizIo.fit(c, ctx);
            case "animate" -> vizIo.animate(c, ctx);
            case "axis" -> vizIo.axis(c, ctx);
            case "labels" -> vizIo.labels(c, ctx);
            case "save" -> vizIo.save(c, ctx);
            case "csv" -> vizIo.csv(c, ctx);
            case "latex" -> vizIo.latex(c, ctx);
            case "run" -> vizIo.runScript(c, ctx);
            case "savelog" -> vizIo.savelog(c, ctx);
            case "copylog" -> vizIo.copylog(c, ctx);
            case "saveplot" -> vizIo.saveplot(c, ctx);

            default -> CommandResult.error(I18n.t("m.unknownCmd", c.name()));
        };
    }
}
