package de.lgstool.command;

import de.lgstool.config.I18n;
import de.lgstool.model.AppState;
import de.lgstool.model.Lgs;
import de.lgstool.model.UndoManager;

/**
 * Mutable core shared by every command handler group: the {@link AppState}, the
 * {@link UndoManager} and the {@link UiBridge}.
 *
 * <p>Handlers must read these live (never cache them) because {@code state} is
 * replaced wholesale by undo/redo and {@code ui} is injected after construction.
 */
final class EngineContext {

    private AppState state = new AppState();
    private final UndoManager undo = new UndoManager();
    private UiBridge ui;

    EngineContext(UiBridge ui) {
        this.ui = ui;
    }

    AppState state() {
        return state;
    }

    void setState(AppState state) {
        this.state = state;
    }

    UndoManager undo() {
        return undo;
    }

    UiBridge ui() {
        return ui;
    }

    void setUi(UiBridge ui) {
        this.ui = ui;
    }

    void recordUndo() {
        undo.record(state);
    }

    /** Resolves the LGS targeted by an [LGS]-screen command, or fails with a localized error. */
    Lgs lgsOf(CommandContext ctx) throws CommandException {
        if (ctx.screen() != Screen.LGS || ctx.lgsName() == null) {
            throw new CommandException(I18n.t("m.onlyLgs"));
        }
        Lgs lgs = state.get(ctx.lgsName());
        if (lgs == null) {
            throw new CommandException(I18n.t("m.noOpenLgs"));
        }
        return lgs;
    }
}
