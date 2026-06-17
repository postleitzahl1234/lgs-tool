package de.lgstool.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable, exact rational number backed by {@link BigInteger}.
 *
 * <p>Always stored in canonical form: denominator strictly positive and
 * numerator/denominator reduced by their greatest common divisor. Decimal
 * inputs are stored exactly (e.g. {@code 1.2} becomes {@code 6/5}).
 */
public final class Fraction implements Comparable<Fraction> {

    public static final Fraction ZERO = new Fraction(BigInteger.ZERO, BigInteger.ONE);
    public static final Fraction ONE = new Fraction(BigInteger.ONE, BigInteger.ONE);
    public static final Fraction MINUS_ONE = new Fraction(BigInteger.valueOf(-1), BigInteger.ONE);

    private final BigInteger num;
    private final BigInteger den;

    private Fraction(BigInteger num, BigInteger den) {
        this.num = num;
        this.den = den;
    }

    public static Fraction of(long value) {
        return of(BigInteger.valueOf(value), BigInteger.ONE);
    }

    public static Fraction of(long numerator, long denominator) {
        return of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public static Fraction of(BigInteger numerator, BigInteger denominator) {
        if (denominator.signum() == 0) {
            throw new ArithmeticException("Division durch null (Nenner = 0)");
        }
        BigInteger n = numerator;
        BigInteger d = denominator;
        if (d.signum() < 0) {
            n = n.negate();
            d = d.negate();
        }
        BigInteger g = n.gcd(d);
        if (g.signum() != 0 && !g.equals(BigInteger.ONE)) {
            n = n.divide(g);
            d = d.divide(g);
        }
        return new Fraction(n, d);
    }

    /**
     * Parses an integer ({@code 5}), decimal ({@code 1.2}, {@code -0.25}) or
     * fraction ({@code p/q}). Decimals are stored exactly.
     *
     * @throws NumberFormatException if the text is not a valid number
     */
    public static Fraction parse(String text) {
        if (text == null) {
            throw new NumberFormatException("Zahl fehlt");
        }
        String s = text.trim();
        if (s.isEmpty()) {
            throw new NumberFormatException("Zahl fehlt");
        }
        int slash = s.indexOf('/');
        if (slash >= 0) {
            String np = s.substring(0, slash).trim();
            String dp = s.substring(slash + 1).trim();
            return of(parseIntegerLike(np), parseIntegerLike(dp));
        }
        if (s.indexOf('.') >= 0 || s.indexOf('e') >= 0 || s.indexOf('E') >= 0) {
            BigDecimal bd = new BigDecimal(s);
            BigInteger unscaled = bd.unscaledValue();
            int scale = bd.scale();
            if (scale >= 0) {
                return of(unscaled, BigInteger.TEN.pow(scale));
            }
            return of(unscaled.multiply(BigInteger.TEN.pow(-scale)), BigInteger.ONE);
        }
        return of(parseIntegerLike(s), BigInteger.ONE);
    }

    private static BigInteger parseIntegerLike(String s) {
        return new BigInteger(s.trim());
    }

    public Fraction add(Fraction o) {
        return of(num.multiply(o.den).add(o.num.multiply(den)), den.multiply(o.den));
    }

    public Fraction subtract(Fraction o) {
        return of(num.multiply(o.den).subtract(o.num.multiply(den)), den.multiply(o.den));
    }

    public Fraction multiply(Fraction o) {
        return of(num.multiply(o.num), den.multiply(o.den));
    }

    public Fraction divide(Fraction o) {
        if (o.isZero()) {
            throw new ArithmeticException("Division durch null");
        }
        return of(num.multiply(o.den), den.multiply(o.num));
    }

    public Fraction negate() {
        return new Fraction(num.negate(), den);
    }

    public Fraction reciprocal() {
        if (isZero()) {
            throw new ArithmeticException("Kehrwert von null nicht definiert");
        }
        return of(den, num);
    }

    public Fraction abs() {
        return num.signum() < 0 ? negate() : this;
    }

    public int signum() {
        return num.signum();
    }

    public boolean isZero() {
        return num.signum() == 0;
    }

    public boolean isOne() {
        return num.equals(BigInteger.ONE) && den.equals(BigInteger.ONE);
    }

    public boolean isInteger() {
        return den.equals(BigInteger.ONE);
    }

    public BigInteger numerator() {
        return num;
    }

    public BigInteger denominator() {
        return den;
    }

    public double doubleValue() {
        return new BigDecimal(num).divide(new BigDecimal(den), MathContext.DECIMAL64).doubleValue();
    }

    @Override
    public int compareTo(Fraction o) {
        return num.multiply(o.den).compareTo(o.num.multiply(den));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Fraction other)) {
            return false;
        }
        return num.equals(other.num) && den.equals(other.den);
    }

    @Override
    public int hashCode() {
        return Objects.hash(num, den);
    }

    /** Canonical fraction text: {@code "n"} when integer, otherwise {@code "n/d"}. */
    @Override
    public String toString() {
        return den.equals(BigInteger.ONE) ? num.toString() : num + "/" + den;
    }

    /**
     * Decimal text for display. Terminating decimals are exact; repeating
     * decimals are rounded to {@code significantDigits} significant figures with
     * trailing zeros stripped.
     */
    public String toDecimalString(int significantDigits) {
        if (isInteger()) {
            return num.toString();
        }
        BigDecimal value = new BigDecimal(num)
                .divide(new BigDecimal(den), new MathContext(Math.max(1, significantDigits), RoundingMode.HALF_UP));
        value = value.stripTrailingZeros();
        return value.toPlainString();
    }
}
