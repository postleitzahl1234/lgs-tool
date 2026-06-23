package de.lgstool.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LinearEquationTest {

    private static final String[] XY = {"x", "y"};

    @Test
    @DisplayName("parses a simple two-variable equation")
    void simple() {
        LinearEquation.Parsed p = LinearEquation.parse("2x + 3y = 5", XY);
        assertArrayEquals(new Fraction[]{Fraction.of(2), Fraction.of(3)}, p.coefficients());
        assertEquals(Fraction.of(5), p.rhs());
    }

    @Test
    @DisplayName("keeps fractions exact")
    void exactFractions() {
        LinearEquation.Parsed p = LinearEquation.parse("x - 1/2 y = 1", XY);
        assertArrayEquals(new Fraction[]{Fraction.ONE, Fraction.of(-1, 2)}, p.coefficients());
        assertEquals(Fraction.ONE, p.rhs());
    }

    @Test
    @DisplayName("expands parentheses and moves terms across the equals sign")
    void parenthesesAndMove() {
        LinearEquation.Parsed p = LinearEquation.parse("2(x - 1) = y", XY);   // 2x - y = 2
        assertArrayEquals(new Fraction[]{Fraction.of(2), Fraction.of(-1)}, p.coefficients());
        assertEquals(Fraction.of(2), p.rhs());
    }

    @Test
    @DisplayName("collects variables appearing on both sides")
    void bothSides() {
        LinearEquation.Parsed p = LinearEquation.parse("2x = x + 4", XY);     // x = 4
        assertArrayEquals(new Fraction[]{Fraction.ONE, Fraction.ZERO}, p.coefficients());
        assertEquals(Fraction.of(4), p.rhs());
    }

    @Test
    @DisplayName("rejects an unknown variable")
    void unknownVariable() {
        assertThrows(IllegalArgumentException.class, () -> LinearEquation.parse("2x + z = 1", XY));
    }

    @Test
    @DisplayName("rejects a non-linear term")
    void nonLinear() {
        assertThrows(IllegalArgumentException.class, () -> LinearEquation.parse("x*y = 1", XY));
    }

    @Test
    @DisplayName("requires an equals sign")
    void needsEquals() {
        assertThrows(IllegalArgumentException.class, () -> LinearEquation.parse("2x + 3y", XY));
    }
}
