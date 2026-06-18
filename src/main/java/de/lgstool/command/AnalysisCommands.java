package de.lgstool.command;

import static de.lgstool.command.CommandUtil.augmented;
import static de.lgstool.command.CommandUtil.fmt;
import static de.lgstool.command.CommandUtil.formatDouble;
import static de.lgstool.command.CommandUtil.reqInt;

import de.lgstool.config.I18n;
import de.lgstool.config.Settings;
import de.lgstool.math.ConditionNumber;
import de.lgstool.math.Fraction;
import de.lgstool.math.FractionMatrix;
import de.lgstool.math.Gauss;
import de.lgstool.math.Roman;
import de.lgstool.math.Solution;
import de.lgstool.math.SolveStep;
import de.lgstool.model.Lgs;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only analysis commands: solving (with optional steps), determinant, rank,
 * condition number, matrix display, verification, inverse, RREF/REF, transpose,
 * trace, null space and Cramer's rule. These never mutate a system.
 */
final class AnalysisCommands {

    private final EngineContext core;

    AnalysisCommands(EngineContext core) {
        this.core = core;
    }

    CommandResult solve(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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

    CommandResult det(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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

    CommandResult rank(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        if (lgs.height() == 0) {
            throw new CommandException(I18n.t("m.noEquations"));
        }
        int rankA = Gauss.rank(lgs.coefficientMatrix());
        int rankAb = Gauss.rank(augmented(lgs));
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

    CommandResult cond(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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

    CommandResult show(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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

    CommandResult verify(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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

    CommandResult inverse(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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

    CommandResult rref(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        if (lgs.height() == 0) {
            throw new CommandException(I18n.t("m.noEquations"));
        }
        FractionMatrix rref = new FractionMatrix(Gauss.rref(augmented(lgs), false).matrix());
        return matrixResult("m.rrefHeader", rref, lgs);
    }

    CommandResult ref(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
        if (lgs.height() == 0) {
            throw new CommandException(I18n.t("m.noEquations"));
        }
        FractionMatrix ref = new FractionMatrix(Gauss.ref(augmented(lgs)));
        return matrixResult("m.refHeader", ref, lgs);
    }

    CommandResult transpose(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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

    CommandResult trace(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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

    CommandResult nullspace(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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

    CommandResult cramer(ParsedCommand c, CommandContext ctx) throws CommandException {
        Lgs lgs = core.lgsOf(ctx);
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

    private CommandResult matrixResult(String headerKey, FractionMatrix m, Lgs lgs) {
        List<String> out = new ArrayList<>();
        out.add(I18n.t(headerKey));
        for (String line : m.toText(lgs.isFractionDisplay(), lgs.width()).split("\n")) {
            out.add(line);
        }
        return CommandResult.ok(out);
    }

    private static String vectorText(Fraction[] v, boolean frac) {
        String[] parts = new String[v.length];
        for (int i = 0; i < v.length; i++) {
            parts[i] = frac ? v[i].toString() : v[i].toDecimalString(Settings.decimalPrecision);
        }
        return "(" + String.join(", ", parts) + ")";
    }
}
