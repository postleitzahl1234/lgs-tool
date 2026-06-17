package de.lgstool.math;

import de.lgstool.config.I18n;
import de.lgstool.config.Settings;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of solving a linear system: unique, infinitely many (parametric) or no solution.
 */
public final class Solution {

    public enum Kind {
        UNIQUE, INFINITE, NONE
    }

    private static final String[] PARAM_NAMES = {"t", "s", "r", "u", "v", "w"};

    private final Kind kind;
    private final int varCount;
    private final Fraction[] particular;
    private final List<Integer> freeVars;
    private final List<Fraction[]> basis;

    private Solution(Kind kind, int varCount, Fraction[] particular, List<Integer> freeVars, List<Fraction[]> basis) {
        this.kind = kind;
        this.varCount = varCount;
        this.particular = particular;
        this.freeVars = freeVars;
        this.basis = basis;
    }

    public static Solution none(int varCount) {
        return new Solution(Kind.NONE, varCount, null, List.of(), List.of());
    }

    public static Solution unique(Fraction[] values) {
        return new Solution(Kind.UNIQUE, values.length, values.clone(), List.of(), List.of());
    }

    public static Solution infinite(Fraction[] particular, List<Integer> freeVars, List<Fraction[]> basis) {
        return new Solution(Kind.INFINITE, particular.length, particular.clone(), List.copyOf(freeVars), List.copyOf(basis));
    }

    public Kind kind() {
        return kind;
    }

    public int freeParameterCount() {
        return freeVars.size();
    }

    /** Particular solution values (UNIQUE: the solution; INFINITE: free vars set to 0). */
    public Fraction[] particular() {
        return particular == null ? null : particular.clone();
    }

    public List<Integer> freeVariableIndices() {
        return freeVars;
    }

    static String paramName(int index) {
        return index < PARAM_NAMES.length ? PARAM_NAMES[index] : "t" + (index + 1);
    }

    /**
     * Produces human-readable solution lines using the given variable names.
     *
     * @param varNames   variable labels, length == varCount
     * @param asFraction true for fraction display, false for decimal
     */
    public List<String> describe(String[] varNames, boolean asFraction) {
        List<String> lines = new ArrayList<>();
        switch (kind) {
            case NONE -> lines.add(I18n.t("s.none"));
            case UNIQUE -> {
                lines.add(I18n.t("s.unique"));
                for (int i = 0; i < varCount; i++) {
                    lines.add("  " + varNames[i] + " = " + fmt(particular[i], asFraction));
                }
            }
            case INFINITE -> {
                lines.add(I18n.t("s.infinite", freeVars.size()));
                for (int i = 0; i < varCount; i++) {
                    String expr = expression(i, asFraction);
                    String suffix = freeVars.contains(i) ? "  " + I18n.t("s.free") : "";
                    lines.add("  " + varNames[i] + " = " + expr + suffix);
                }
            }
            default -> {
            }
        }
        return lines;
    }

    private String expression(int variable, boolean asFraction) {
        StringBuilder sb = new StringBuilder();
        boolean wrote = false;
        Fraction constant = particular[variable];
        if (!constant.isZero()) {
            sb.append(fmt(constant, asFraction));
            wrote = true;
        }
        for (int k = 0; k < freeVars.size(); k++) {
            Fraction coeff = basis.get(k)[variable];
            if (coeff.isZero()) {
                continue;
            }
            String param = paramName(k);
            Fraction mag = coeff.abs();
            String coeffStr = mag.isOne() ? "" : fmt(mag, asFraction) + "·";
            if (!wrote) {
                if (coeff.signum() < 0) {
                    sb.append("−");
                }
                sb.append(coeffStr).append(param);
                wrote = true;
            } else {
                sb.append(coeff.signum() < 0 ? " − " : " + ").append(coeffStr).append(param);
            }
        }
        if (!wrote) {
            sb.append('0');
        }
        return sb.toString();
    }

    private static String fmt(Fraction f, boolean asFraction) {
        return asFraction ? f.toString() : f.toDecimalString(Settings.decimalPrecision);
    }
}
