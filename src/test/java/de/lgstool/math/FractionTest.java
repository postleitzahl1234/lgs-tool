package de.lgstool.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FractionTest {

    @Test
    void reducesToCanonicalForm() {
        assertEquals("2/3", Fraction.of(4, 6).toString());
        assertEquals("2", Fraction.of(6, 3).toString());
    }

    @Test
    void keepsDenominatorPositive() {
        assertEquals("-1/2", Fraction.of(1, -2).toString());
        assertEquals("1/2", Fraction.of(-1, -2).toString());
    }

    @Test
    void parsesIntegerDecimalAndFraction() {
        assertEquals(Fraction.of(5), Fraction.parse("5"));
        assertEquals(Fraction.of(6, 5), Fraction.parse("1.2"));
        assertEquals(Fraction.of(-1, 4), Fraction.parse("-0.25"));
        assertEquals(Fraction.of(1, 3), Fraction.parse("1/3"));
        assertEquals(Fraction.of(-2, 3), Fraction.parse("2/-3"));
    }

    @Test
    void decimalIsStoredExactly() {
        assertEquals("6/5", Fraction.parse("1.2").toString());
    }

    @Test
    void arithmeticIsExact() {
        Fraction third = Fraction.of(1, 3);
        assertEquals(Fraction.ONE, third.add(third).add(third));
        assertEquals(Fraction.of(1, 6), Fraction.of(1, 2).multiply(third));
        assertEquals(Fraction.of(3, 2), Fraction.of(1, 2).divide(third));
        assertEquals(Fraction.of(1, 6), Fraction.of(1, 2).subtract(third));
    }

    @Test
    void comparisonAndSignum() {
        assertTrue(Fraction.of(1, 3).compareTo(Fraction.of(1, 2)) < 0);
        assertEquals(0, Fraction.of(2, 4).compareTo(Fraction.of(1, 2)));
        assertEquals(-1, Fraction.of(-5, 7).signum());
    }

    @Test
    void decimalDisplay() {
        assertEquals("0.5", Fraction.of(1, 2).toDecimalString(6));
        assertEquals("0.333333", Fraction.of(1, 3).toDecimalString(6));
        assertEquals("3", Fraction.of(3).toDecimalString(6));
    }

    @Test
    void divisionByZeroThrows() {
        assertThrows(ArithmeticException.class, () -> Fraction.ONE.divide(Fraction.ZERO));
        assertThrows(ArithmeticException.class, () -> Fraction.of(1, 0));
    }
}
