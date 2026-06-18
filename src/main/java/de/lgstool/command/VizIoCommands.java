package de.lgstool.command;

import static de.lgstool.command.CommandUtil.checkRow;
import static de.lgstool.command.CommandUtil.need;
import static de.lgstool.command.CommandUtil.reqDouble;
import static de.lgstool.command.CommandUtil.reqFrac;
import static de.lgstool.command.CommandUtil.reqInt;

import de.lgstool.config.I18n;
import de.lgstool.config.Settings;
import de.lgstool.io.LgsStorage;
import de.lgstool.math.Fraction;
import de.lgstool.math.Roman;
import de.lgstool.model.Lgs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

/**
 * Commands with side effects on the view or the file system: highlight, fit,
 * animate, axis ranges, label toggling, save/CSV/LaTeX export, plot and log
 * export, and running a command script.
 */
final class VizIoCommands {

    private final EngineContext core;
    private final BiFunction<String, CommandContext, CommandResult> runner;

    VizIoCommands(EngineContext core, BiFunction<String, CommandContext, CommandResult> runner) {
        this.core = core;
        this.runner = runner;
    }

    CommandResult highlight(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        if (c.argCount() < 1) {
            throw new CommandException(I18n.t("m.highlightNeedsRow"));
        }
        int row = reqInt(c.arg(0), I18n.t("w.row"));
        checkRow(lgs, row);
        String hex = c.argCount() >= 2 ? c.arg(1) : null;
        if (hex != null && !hex.matches("#?[0-9a-fA-F]{6}")) {
            throw new CommandException(I18n.t("m.badHex", hex));
        }
        core.ui().highlightRow(lgs.name(), row, hex);
        return CommandResult.ok(I18n.t("m.highlighted", Roman.of(row)));
    }

    CommandResult fit(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        core.ui().fitView(lgs.name());
        return CommandResult.ok(I18n.t("m.centered"));
    }

    CommandResult animate(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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
        core.ui().animate(lgs.name(), row, from, to, duration);
        return CommandResult.ok(I18n.t("m.animateStarted", Roman.of(row), from.toString(), to.toString()));
    }

    CommandResult axis(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        if (c.argCount() == 0) {
            core.ui().setAxisRange(lgs.name(), -1, 0, 0);
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
        core.ui().setAxisRange(lgs.name(), axisIndex, min, max);
        return CommandResult.ok(I18n.t("m.axisSet", String.valueOf("xyz".charAt(axisIndex)),
                String.valueOf(min), String.valueOf(max)));
    }

    CommandResult labels(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        core.ui().toggleLabels(lgs.name());
        return CommandResult.ok(I18n.t("m.labelsToggled"));
    }

    CommandResult save(ParsedCommand c, CommandContext ctx) throws CommandException, IOException {
        Lgs lgs = core.lgsOf(ctx);
        need(c, 1);
        var path = LgsStorage.save(lgs, c.arg(0));
        return CommandResult.ok(I18n.t("m.saved", path.toAbsolutePath().toString()));
    }

    CommandResult csv(ParsedCommand c, CommandContext ctx) throws CommandException, IOException {
        Lgs lgs = core.lgsOf(ctx);
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

    CommandResult latex(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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
        core.ui().copyToClipboard(sb.toString());
        List<String> out = new ArrayList<>();
        for (String line : sb.toString().split("\n")) {
            out.add(line);
        }
        out.add(I18n.t("m.latexCopied"));
        return CommandResult.ok(out);
    }

    CommandResult runScript(ParsedCommand c, CommandContext ctx) throws CommandException, IOException {
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
            out.addAll(runner.apply(trimmed, ctx).messages());
        }
        return CommandResult.ok(out);
    }

    CommandResult savelog(ParsedCommand c, CommandContext ctx) throws CommandException {
        need(c, 1);
        Path file = Paths.get("lgs").resolve(c.arg(0) + ".txt");
        core.ui().saveLog(file.toAbsolutePath().toString());
        return CommandResult.ok(I18n.t("m.logSaved", file.toAbsolutePath().toString()));
    }

    CommandResult copylog(ParsedCommand c, CommandContext ctx) {
        core.ui().copyLog();
        return CommandResult.ok(I18n.t("m.logCopied"));
    }

    CommandResult saveplot(ParsedCommand c, CommandContext ctx) throws CommandException {
        need(c, 1);
        Path file = Paths.get("lgs").resolve(c.arg(0) + ".png");
        core.ui().savePlot(file.toAbsolutePath().toString());
        return CommandResult.ok(I18n.t("m.plotSaved", file.toAbsolutePath().toString()));
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
}
