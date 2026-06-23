package de.lgstool.command;

import static de.lgstool.command.CommandUtil.checkRow;
import static de.lgstool.command.CommandUtil.dims;
import static de.lgstool.command.CommandUtil.fmt;
import static de.lgstool.command.CommandUtil.need;
import static de.lgstool.command.CommandUtil.reqFrac;
import static de.lgstool.command.CommandUtil.reqInt;

import de.lgstool.config.I18n;
import de.lgstool.math.Fraction;
import de.lgstool.math.LinearEquation;
import de.lgstool.math.Roman;
import de.lgstool.model.Lgs;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Commands that mutate a single system in place: add/edit equations, elementary
 * row operations, variable renaming, resizing, substitution, randomization and
 * fraction/decimal display mode.
 */
final class EditCommands {

    private final EngineContext core;

    EditCommands(EngineContext core) {
        this.core = core;
    }

    CommandResult add(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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
        core.recordUndo();
        lgs.addEquation(coeffs, rhs, rowIdx);
        core.ui().lgsChanged(lgs.name());
        return CommandResult.ok(rowIdx == null ? I18n.t("m.eqEntered") : I18n.t("m.rowOverwritten", rowIdx));
    }

    CommandResult eq(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        if (c.argCount() < 1) {
            throw new CommandException(I18n.t("m.eqNeedsExpr"));
        }
        LinearEquation.Parsed parsed;
        try {
            parsed = LinearEquation.parse(c.arg(0), lgs.variableNames());
        } catch (IllegalArgumentException e) {
            throw new CommandException(e.getMessage());
        }
        Integer rowIdx = null;
        if (c.argCount() >= 2) {
            rowIdx = reqInt(c.arg(1), I18n.t("w.row"));
            if (rowIdx < 1 || rowIdx > lgs.height()) {
                throw new CommandException(I18n.t("m.rowNotExist", rowIdx, lgs.height()));
            }
        }
        core.recordUndo();
        lgs.addEquation(parsed.coefficients(), parsed.rhs(), rowIdx);
        core.ui().lgsChanged(lgs.name());
        return CommandResult.ok(rowIdx == null ? I18n.t("m.eqEntered") : I18n.t("m.rowOverwritten", rowIdx));
    }

    CommandResult swap(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        need(c, 2);
        int i = reqInt(c.arg(0), I18n.t("w.rowI"));
        int j = reqInt(c.arg(1), I18n.t("w.rowJ"));
        checkRow(lgs, i);
        checkRow(lgs, j);
        core.recordUndo();
        lgs.swapRows(i, j);
        core.ui().lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.swapDone", Roman.of(i), Roman.of(j)));
    }

    CommandResult scale(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        need(c, 2);
        int i = reqInt(c.arg(0), I18n.t("w.row"));
        Fraction factor = reqFrac(c.arg(1), I18n.t("w.factor"));
        checkRow(lgs, i);
        if (factor.isZero()) {
            throw new CommandException(I18n.t("m.factorZero"));
        }
        core.recordUndo();
        lgs.scaleRow(i, factor);
        core.ui().lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.scaleDone", Roman.of(i), factor.toString()));
    }

    CommandResult combine(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        need(c, 3);
        int i = reqInt(c.arg(0), I18n.t("w.rowI"));
        Fraction factor = reqFrac(c.arg(1), I18n.t("w.factor"));
        int j = reqInt(c.arg(2), I18n.t("w.rowJ"));
        checkRow(lgs, i);
        checkRow(lgs, j);
        core.recordUndo();
        lgs.combineRows(i, factor, j);
        core.ui().lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.combineDone", Roman.of(i), factor.toString(), Roman.of(j)));
    }

    CommandResult clear(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        core.recordUndo();
        lgs.clear();
        core.ui().lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.cleared"));
    }

    CommandResult resize(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        need(c, 1);
        int[] dim = dims(c.arg(0));
        core.recordUndo();
        boolean discarded = lgs.resize(dim[0], dim[1]);
        core.ui().lgsChanged(lgs.name());
        List<String> out = new ArrayList<>();
        out.add(I18n.t("m.resized", dim[0], dim[1]));
        if (discarded) {
            out.add(I18n.t("m.resizeDiscard"));
        }
        return CommandResult.ok(out);
    }

    CommandResult setvar(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        need(c, 2);
        int col = reqInt(c.arg(0), I18n.t("w.column"));
        String name = c.arg(1);
        if (name.isEmpty()) {
            throw new CommandException(I18n.t("m.varNameEmpty"));
        }
        core.recordUndo();
        lgs.setVariableName(col, name);
        core.ui().lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.colRenamed", col, name));
    }

    CommandResult subst(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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
        core.recordUndo();
        lgs.addEquation(coeffs, value, null);
        core.ui().lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.substAdded", lgs.variableNames()[index], fmt(value, lgs.isFractionDisplay())));
    }

    CommandResult rand(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        if (c.argCount() != 2 && c.argCount() != 3) {
            throw new CommandException(I18n.t("m.randArgs"));
        }
        int min = reqInt(c.arg(0), I18n.t("w.min"));
        int max = reqInt(c.arg(1), I18n.t("w.max"));
        if (max < min) {
            throw new CommandException(I18n.t("m.randRange"));
        }
        core.recordUndo();
        if (c.argCount() == 3) {
            int row = reqInt(c.arg(2), I18n.t("w.row"));
            checkRow(lgs, row);
            fillRow(lgs, row, min, max);
        } else {
            for (int r = 1; r <= lgs.height(); r++) {
                fillRow(lgs, r, min, max);
            }
        }
        core.ui().lgsChanged(lgs.name());
        return CommandResult.ok(I18n.t("m.randFilled", String.valueOf(min), String.valueOf(max)));
    }

    CommandResult display(ParsedCommand c, CommandContext ctx, boolean fraction) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        lgs.setFractionDisplay(fraction);
        core.ui().lgsChanged(lgs.name());
        return CommandResult.ok(fraction ? I18n.t("m.displayFrac") : I18n.t("m.displayDec"));
    }

    private static void fillRow(Lgs lgs, int row, int min, int max) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int col = 0; col <= lgs.width(); col++) {
            lgs.setCell(row, col, Fraction.of(rnd.nextInt(min, max + 1)));
        }
    }
}
