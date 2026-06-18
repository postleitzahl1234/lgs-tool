package de.lgstool.command;

import de.lgstool.config.I18n;
import de.lgstool.math.Fraction;
import de.lgstool.model.Lgs;
import java.util.Locale;

/**
 * Stateless argument parsing, validation and number-formatting helpers shared by
 * the command handler groups. All methods are pure: they depend only on their
 * arguments, never on engine state.
 */
final class CommandUtil {

    private CommandUtil() {
    }

    static void need(ParsedCommand c, int n) throws CommandException {
        if (c.argCount() != n) {
            throw new CommandException(I18n.t("m.expectsArgs", c.name(), n, c.argCount()));
        }
    }

    static void requireScreen(CommandContext ctx, Screen screen, String cmd) throws CommandException {
        if (ctx.screen() != screen) {
            String where = screen == Screen.HAUPT ? I18n.t("m.mainScreen") : I18n.t("m.lgsWindow");
            throw new CommandException(I18n.t("m.onlyIn", cmd, where));
        }
    }

    static void checkRow(Lgs lgs, int row) throws CommandException {
        if (row < 1 || row > lgs.height()) {
            throw new CommandException(I18n.t("m.rowNotExist", row, lgs.height()));
        }
    }

    static int reqInt(String s, String what) throws CommandException {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new CommandException(I18n.t("m.notInt", what, s.trim()));
        }
    }

    static Fraction reqFrac(String s, String what) throws CommandException {
        try {
            return Fraction.parse(s);
        } catch (RuntimeException e) {
            throw new CommandException(I18n.t("m.notNumber", what, s.trim()));
        }
    }

    static double reqDouble(String s, String what) throws CommandException {
        try {
            return Fraction.parse(s).doubleValue();
        } catch (RuntimeException e) {
            throw new CommandException(I18n.t("m.notNumber", what, s.trim()));
        }
    }

    static int[] dims(String s) throws CommandException {
        String[] parts = s.split("\\*");
        if (parts.length != 2) {
            throw new CommandException(I18n.t("m.dims"));
        }
        return new int[]{reqInt(parts[0], I18n.t("w.width")), reqInt(parts[1], I18n.t("w.height"))};
    }

    /** Builds the augmented matrix [A | b] of a system. */
    static Fraction[][] augmented(Lgs lgs) {
        Fraction[][] a = lgs.coefficientMatrix();
        Fraction[] b = lgs.rightHandSide();
        Fraction[][] aug = new Fraction[a.length][lgs.width() + 1];
        for (int r = 0; r < a.length; r++) {
            System.arraycopy(a[r], 0, aug[r], 0, lgs.width());
            aug[r][lgs.width()] = b[r];
        }
        return aug;
    }

    static String fmt(Fraction f, boolean asFraction) {
        return asFraction ? f.toString() : f.toDecimalString(10);
    }

    static String formatDouble(double v) {
        if (Double.isInfinite(v)) {
            return "∞";
        }
        return String.format(Locale.US, "%.6g", v);
    }
}
