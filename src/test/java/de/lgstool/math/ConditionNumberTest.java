package de.lgstool.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConditionNumberTest {

    private static Fraction[][] diag(long a, long b) {
        return new Fraction[][]{
                {Fraction.of(a), Fraction.ZERO},
                {Fraction.ZERO, Fraction.of(b)}
        };
    }

    @Test
    void identityHasConditionOne() {
        assertEquals(1.0, ConditionNumber.of(diag(1, 1), ConditionNumber.NORM_INFINITY), 1e-9);
        assertEquals(1.0, ConditionNumber.of(diag(1, 1), ConditionNumber.NORM_TWO), 1e-6);
    }

    @Test
    void diagonalConditionIsRatioOfMagnitudes() {
        assertEquals(100.0, ConditionNumber.of(diag(1, 100), ConditionNumber.NORM_INFINITY), 1e-9);
        assertEquals(100.0, ConditionNumber.of(diag(1, 100), ConditionNumber.NORM_TWO), 1e-4);
    }

    @Test
    void singularMatrixThrows() {
        Fraction[][] singular = {
                {Fraction.of(1), Fraction.of(2)},
                {Fraction.of(2), Fraction.of(4)}
        };
        assertThrows(ArithmeticException.class,
                () -> ConditionNumber.of(singular, ConditionNumber.NORM_INFINITY));
    }
}
