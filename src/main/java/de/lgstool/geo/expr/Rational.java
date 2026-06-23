package de.lgstool.geo.expr;

import de.lgstool.math.Fraction;

/**
 * Evaluates the exact subset of an {@link Expr} to a {@link Fraction}: numeric literals
 * (kept verbatim, so {@code 1/2} and {@code 0.25} stay exact), unary minus, the four
 * rational operators, and references that resolve to a number. Used to build the
 * {@link Value.MatrixV} entries without going through the {@code double} evaluator, so a
 * matrix typed as {@code {{1/3, 2}, {3, 4}}} keeps its exact fractions.
 */
public final class Rational {

    private Rational() {
    }

    public static Fraction eval(Expr e, NameResolver scope) {
        return switch (e) {
            case Expr.Num n -> n.text() != null ? Fraction.parse(n.text()) : fromDouble(n.value());
            case Expr.Unary u -> u.op() == '-' ? eval(u.operand(), scope).negate() : eval(u.operand(), scope);
            case Expr.Bin b -> bin(b, scope);
            case Expr.Ref r -> ref(r.name(), scope);
            default -> throw new EvalException("Nur exakte Zahlen (+ - * /) sind in Matrizen erlaubt.");
        };
    }

    private static Fraction bin(Expr.Bin b, NameResolver scope) {
        Fraction l = eval(b.left(), scope);
        Fraction r = eval(b.right(), scope);
        return switch (b.op()) {
            case '+' -> l.add(r);
            case '-' -> l.subtract(r);
            case '*' -> l.multiply(r);
            case '/' -> l.divide(r);
            default -> throw new EvalException("Operator '" + b.op() + "' ist für exakte Zahlen nicht erlaubt.");
        };
    }

    private static Fraction ref(String name, NameResolver scope) {
        Value v = scope == null ? null : scope.resolve(name);
        if (v instanceof Value.NumberV n) {
            return fromDouble(n.v());
        }
        throw new EvalException("'" + name + "' ist keine exakte Zahl.");
    }

    /** Converts a {@code double} to an exact {@link Fraction} via its decimal text. */
    public static Fraction fromDouble(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new EvalException("Ungültige Zahl.");
        }
        if (d == Math.rint(d)) {
            return Fraction.of((long) d);
        }
        return Fraction.parse(Double.toString(d));
    }
}
