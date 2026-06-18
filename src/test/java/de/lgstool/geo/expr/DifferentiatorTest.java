package de.lgstool.geo.expr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DifferentiatorTest {

    private static final double EPS = 1e-6;

    private static double slopeAt(String expr, double x) {
        Expr d = Differentiator.differentiate(Parser.parse(expr), "x");
        Evaluator ev = new Evaluator(n -> "x".equals(n) ? new Value.NumberV(x) : null, new MathBuiltins());
        return ev.evalNumber(d);
    }

    @Test
    @DisplayName("power rule")
    void powerRule() {
        assertEquals(6, slopeAt("x^2", 3), EPS);
        assertEquals(12, slopeAt("x^3", 2), EPS);
        assertEquals(5, slopeAt("x^2 + 3*x", 1), EPS);
    }

    @Test
    @DisplayName("quotient and constants")
    void quotient() {
        assertEquals(-0.25, slopeAt("1 / x", 2), EPS);   // d(1/x) = -1/x^2
        assertEquals(0, slopeAt("7", 5), EPS);
    }

    @Test
    @DisplayName("chain rule for transcendental functions")
    void chainRule() {
        assertEquals(1, slopeAt("sin(x)", 0), EPS);       // cos(0)
        assertEquals(1, slopeAt("exp(x)", 0), EPS);       // exp(0)
        assertEquals(1, slopeAt("ln(x)", 1), EPS);        // 1/1
        assertEquals(2 * Math.cos(0), slopeAt("sin(2*x)", 0), EPS); // chain: 2cos(2x)
    }
}
