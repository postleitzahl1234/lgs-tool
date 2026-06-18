package de.lgstool.command;

import static de.lgstool.command.CommandUtil.checkRow;
import static de.lgstool.command.CommandUtil.dims;
import static de.lgstool.command.CommandUtil.need;
import static de.lgstool.command.CommandUtil.reqFrac;
import static de.lgstool.command.CommandUtil.reqInt;
import static de.lgstool.command.CommandUtil.requireScreen;

import de.lgstool.config.I18n;
import de.lgstool.io.LgsStorage;
import de.lgstool.math.Fraction;
import de.lgstool.model.AppState;
import de.lgstool.model.Examples;
import de.lgstool.model.Lgs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Commands that manage the set of systems and undo history: create/load/open,
 * list, delete, rename, close, undo/redo, examples and CSV import.
 */
final class SystemCommands {

    private final EngineContext core;

    SystemCommands(EngineContext core) {
        this.core = core;
    }

    CommandResult create(ParsedCommand c, CommandContext ctx) throws CommandException {
        requireScreen(ctx, Screen.HAUPT, "create");
        need(c, 2);
        String name = c.arg(0);
        int[] dim = dims(c.arg(1));
        if (name.isEmpty()) {
            throw new CommandException(I18n.t("m.nameEmpty"));
        }
        if (core.state().contains(name)) {
            throw new CommandException(I18n.t("m.exists", name));
        }
        core.recordUndo();
        core.state().put(Lgs.create(name, dim[0], dim[1]));
        core.ui().systemsChanged();
        return CommandResult.ok(I18n.t("m.created", name, dim[0], dim[1]));
    }

    CommandResult load(ParsedCommand c, CommandContext ctx) throws CommandException, IOException {
        requireScreen(ctx, Screen.HAUPT, "load");
        need(c, 1);
        Lgs lgs = LgsStorage.load(c.arg(0));
        if (core.state().contains(lgs.name())) {
            throw new CommandException(I18n.t("m.exists", lgs.name()));
        }
        core.recordUndo();
        core.state().put(lgs);
        core.ui().systemsChanged();
        return CommandResult.ok(I18n.t("m.loaded", lgs.name()));
    }

    CommandResult open(ParsedCommand c, CommandContext ctx) throws CommandException {
        requireScreen(ctx, Screen.HAUPT, "open");
        need(c, 1);
        String name = c.arg(0);
        if (!core.state().contains(name)) {
            throw new CommandException(I18n.t("m.notExists", name));
        }
        core.state().setOpenName(name);
        core.ui().openWindow(name);
        return CommandResult.ok(I18n.t("m.opened", name));
    }

    CommandResult list(ParsedCommand c, CommandContext ctx) throws CommandException {
        requireScreen(ctx, Screen.HAUPT, "list");
        List<String> names = core.state().names();
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

    CommandResult del(ParsedCommand c, CommandContext ctx) throws CommandException {
        need(c, 1);
        if (ctx.screen() == Screen.HAUPT) {
            String name = c.arg(0);
            if (!core.state().contains(name)) {
                throw new CommandException(I18n.t("m.notExists", name));
            }
            core.recordUndo();
            core.state().remove(name);
            core.ui().closeWindow(name);
            core.ui().systemsChanged();
            return CommandResult.ok(I18n.t("m.deletedSystem", name));
        }
        Lgs lgs = core.lgsOf(ctx);
        int row = reqInt(c.arg(0), I18n.t("w.row"));
        checkRow(lgs, row);
        core.recordUndo();
        lgs.deleteRow(row);
        core.ui().lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.deletedRow", row));
    }

    CommandResult undo(ParsedCommand c, CommandContext ctx) {
        AppState prev = core.undo().undo(core.state());
        if (prev == null) {
            return CommandResult.ok(I18n.t("m.nothingUndo"));
        }
        core.setState(prev);
        core.ui().systemsChanged();
        return CommandResult.ok(I18n.t("m.undone"));
    }

    CommandResult redo(ParsedCommand c, CommandContext ctx) {
        AppState next = core.undo().redo(core.state());
        if (next == null) {
            return CommandResult.ok(I18n.t("m.nothingRedo"));
        }
        core.setState(next);
        core.ui().systemsChanged();
        return CommandResult.ok(I18n.t("m.redone"));
    }

    CommandResult rename(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        need(c, 1);
        String newName = c.arg(0);
        if (newName.isEmpty()) {
            throw new CommandException(I18n.t("m.newNameEmpty"));
        }
        if (!newName.equals(lgs.name()) && core.state().contains(newName)) {
            throw new CommandException(I18n.t("m.exists", newName));
        }
        core.recordUndo();
        core.state().rename(lgs.name(), newName);
        core.ui().systemsChanged();
        return CommandResult.ok(I18n.t("m.renamed", newName));
    }

    CommandResult close(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        if (lgs.name().equals(core.state().openName())) {
            core.state().setOpenName(null);
        }
        core.ui().closeWindow(lgs.name());
        return CommandResult.ok(I18n.t("m.windowClosed"));
    }

    CommandResult examples(ParsedCommand c, CommandContext ctx) throws CommandException {
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
        if (core.state().contains(lgs.name())) {
            throw new CommandException(I18n.t("m.exists", lgs.name()));
        }
        core.recordUndo();
        core.state().put(lgs);
        core.ui().systemsChanged();
        return CommandResult.ok(I18n.t("m.exampleLoaded", lgs.name()));
    }

    CommandResult loadcsv(ParsedCommand c, CommandContext ctx) throws CommandException, IOException {
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
        if (core.state().contains(name)) {
            throw new CommandException(I18n.t("m.exists", name));
        }
        core.recordUndo();
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
        core.state().put(lgs);
        core.ui().systemsChanged();
        return CommandResult.ok(I18n.t("m.csvLoaded", name));
    }

    private static boolean isNumeric(String s) {
        try {
            Fraction.parse(s);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
