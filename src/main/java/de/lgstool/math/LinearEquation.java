package de.lgstool.math;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses a human-written linear equation such as {@code 2x + 3y = 5} or
 * {@code x - 1/2 y + z = 2(x - 1)} into exact {@link Fraction} coefficients for a
 * given set of variable names, plus the right-hand side. Everything is moved to the
 * left so the result represents {@code Σ aᵢ·xᵢ = b}.
 *
 * <p>Supports {@code + - * /}, parentheses, decimals, fractions ({@code a/b}) and
 * implicit multiplication ({@code 2x}, {@code 3(y+1)}). Non-linear terms (a variable
 * times a variable, division by a variable, powers) are rejected.
 */
public final class LinearEquation {

    /** Parsed coefficients (one per variable, in column order) and the right-hand side. */
    public record Parsed(Fraction[] coefficients, Fraction rhs) {
    }

    private final String src;
    private int pos;

    private LinearEquation(String src) {
        this.src = src;
    }

    public static Parsed parse(String equation, String[] variableNames) {
        if (equation == null || equation.indexOf('=') < 0) {
            throw new IllegalArgumentException("Gleichung braucht ein '=', z. B. 2x + 3y = 5.");
        }
        int eq = topLevelEquals(equation);
        if (eq < 0) {
            throw new IllegalArgumentException("Gleichung braucht ein '=' auf oberster Ebene.");
        }
        LinearForm left = new LinearEquation(equation.substring(0, eq)).parseSide();
        LinearForm right = new LinearEquation(equation.substring(eq + 1)).parseSide();
        LinearForm form = left.subtract(right);          // = 0

        Fraction[] coeffs = new Fraction[variableNames.length];
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < variableNames.length; i++) {
            index.put(variableNames[i], i);
            coeffs[i] = Fraction.ZERO;
        }
        for (Map.Entry<String, Fraction> e : form.terms.entrySet()) {
            Integer i = index.get(e.getKey());
            if (i == null) {
                throw new IllegalArgumentException("Unbekannte Variable: '" + e.getKey() + "'.");
            }
            coeffs[i] = e.getValue();
        }
        return new Parsed(coeffs, form.constant.negate());   // Σ a·x = -const
    }

    private LinearForm parseSide() {
        LinearForm form = parseExpr();
        skipSpaces();
        if (pos < src.length()) {
            throw new IllegalArgumentException("Unerwartetes Zeichen: '" + src.charAt(pos) + "'.");
        }
        return form;
    }

    // ---- recursive descent (returns a linear form) -----------------------

    private LinearForm parseExpr() {
        LinearForm form = parseTerm();
        while (true) {
            skipSpaces();
            char c = peek();
            if (c == '+') {
                pos++;
                form = form.add(parseTerm());
            } else if (c == '-') {
                pos++;
                form = form.subtract(parseTerm());
            } else {
                return form;
            }
        }
    }

    private LinearForm parseTerm() {
        LinearForm form = parseFactor();
        while (true) {
            skipSpaces();
            char c = peek();
            if (c == '*') {
                pos++;
                form = form.multiply(parseFactor());
            } else if (c == '/') {
                pos++;
                form = form.divide(parseFactor());
            } else if (startsFactor(c)) {
                form = form.multiply(parseFactor());     // implicit multiplication: 2x, 3(y+1)
            } else {
                return form;
            }
        }
    }

    private LinearForm parseFactor() {
        skipSpaces();
        char c = peek();
        if (c == '-') {
            pos++;
            return parseFactor().negate();
        }
        if (c == '+') {
            pos++;
            return parseFactor();
        }
        if (c == '(') {
            pos++;
            LinearForm inner = parseExpr();
            skipSpaces();
            if (peek() != ')') {
                throw new IllegalArgumentException("Fehlende schließende Klammer.");
            }
            pos++;
            return inner;
        }
        if (Character.isLetter(c)) {
            return LinearForm.variable(readIdentifier());
        }
        if (Character.isDigit(c) || c == '.') {
            return LinearForm.constant(Fraction.parse(readNumber()));
        }
        throw new IllegalArgumentException(c == '\0'
                ? "Unerwartetes Ende der Gleichung."
                : "Unerwartetes Zeichen: '" + c + "'.");
    }

    private static boolean startsFactor(char c) {
        return Character.isLetterOrDigit(c) || c == '(' || c == '.';
    }

    private String readIdentifier() {
        int start = pos;
        while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
            pos++;
        }
        return src.substring(start, pos);
    }

    private String readNumber() {
        int start = pos;
        boolean dot = false;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isDigit(c)) {
                pos++;
            } else if (c == '.' && !dot) {
                dot = true;
                pos++;
            } else {
                break;
            }
        }
        return src.substring(start, pos);
    }

    private char peek() {
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private void skipSpaces() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }

    private static int topLevelEquals(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
            } else if (ch == '=' && depth == 0) {
                return i;
            }
        }
        return -1;
    }

    /** A linear form: a constant plus a map of variable → coefficient. */
    private static final class LinearForm {
        private Fraction constant;
        private final Map<String, Fraction> terms = new LinkedHashMap<>();

        private LinearForm(Fraction constant) {
            this.constant = constant;
        }

        static LinearForm constant(Fraction c) {
            return new LinearForm(c);
        }

        static LinearForm variable(String name) {
            LinearForm f = new LinearForm(Fraction.ZERO);
            f.terms.put(name, Fraction.ONE);
            return f;
        }

        LinearForm add(LinearForm o) {
            LinearForm r = new LinearForm(constant.add(o.constant));
            r.terms.putAll(terms);
            o.terms.forEach((k, val) -> r.terms.merge(k, val, Fraction::add));
            r.terms.values().removeIf(Fraction::isZero);
            return r;
        }

        LinearForm subtract(LinearForm o) {
            return add(o.negate());
        }

        LinearForm negate() {
            return scale(Fraction.MINUS_ONE);
        }

        LinearForm multiply(LinearForm o) {
            if (terms.isEmpty()) {
                return o.scale(constant);
            }
            if (o.terms.isEmpty()) {
                return scale(o.constant);
            }
            throw new IllegalArgumentException("Nicht-linearer Term (Variable mal Variable).");
        }

        LinearForm divide(LinearForm o) {
            if (!o.terms.isEmpty()) {
                throw new IllegalArgumentException("Division durch eine Variable ist nicht linear.");
            }
            if (o.constant.isZero()) {
                throw new IllegalArgumentException("Division durch null.");
            }
            return scale(o.constant.reciprocal());
        }

        private LinearForm scale(Fraction k) {
            LinearForm r = new LinearForm(constant.multiply(k));
            terms.forEach((name, coeff) -> r.terms.put(name, coeff.multiply(k)));
            r.terms.values().removeIf(Fraction::isZero);
            return r;
        }
    }
}
