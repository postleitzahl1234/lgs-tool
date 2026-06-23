package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CAS-lite: Solve, Limit, TaylorPolynomial, Substitute, Expand, Factor, indefinite Integral. */
class CasTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private void ok(String line) {
        GeoResult r = engine.execute(line);
        assertTrue(r.ok(), () -> "expected ok for '" + line + "' but got: " + r.messages());
    }

    private Value v(String name) {
        return engine.construction().get(name).value();
    }

    private double num(String name) {
        return ((Value.NumberV) v(name)).v();
    }

    private List<Value> list(String name) {
        return ((Value.ListV) v(name)).items();
    }

    private double eval(String fn, double x) {
        ok("tmpEval = " + fn + "(" + x + ")");
        return num("tmpEval");
    }

    @Test
    @DisplayName("Solve finds both roots of a quadratic equation")
    void solveQuadratic() {
        ok("s = Solve(x^2 - 4 = 0)");
        List<Value> roots = list("s");
        assertEquals(2, roots.size());
        assertEquals(-2.0, ((Value.NumberV) roots.get(0)).v(), 1e-6);
        assertEquals(2.0, ((Value.NumberV) roots.get(1)).v(), 1e-6);
    }

    @Test
    @DisplayName("Solve still routes a matrix argument to the matrix solver")
    void solveMatrixStillWorks() {
        ok("A = Solve({{1, 1, 3}, {1, -1, 1}})");   // augmented system -> unique solution column
        assertTrue(v("A") instanceof Value.MatrixV);
    }

    @Test
    @DisplayName("Limit of sin(x)/x at 0 is 1")
    void limit() {
        ok("f(x) = sin(x) / x");
        ok("L = Limit(f, 0)");
        assertEquals(1.0, num("L"), 1e-4);
    }

    @Test
    @DisplayName("TaylorPolynomial of exp around 0 evaluates close to exp")
    void taylor() {
        ok("f(x) = exp(x)");
        ok("t = TaylorPolynomial(f, 0, 6)");
        assertEquals(Math.exp(1), eval("t", 1), 1e-2);
    }

    @Test
    @DisplayName("Indefinite Integral of x^2 is x^3/3")
    void indefiniteIntegral() {
        ok("f(x) = x^2");
        ok("F = Integral(f)");
        assertEquals(9.0, eval("F", 3), 1e-6);   // 3^3/3 = 9
    }

    @Test
    @DisplayName("Area between f and g")
    void areaBetween() {
        ok("f(x) = x^2 + 4");
        ok("g(x) = x^2");
        ok("a = Integral(f, g, 0, 2)");          // ∫ 4 dx over [0,2] = 8
        assertEquals(8.0, num("a"), 1e-6);
    }

    @Test
    @DisplayName("Expand and Substitute")
    void expandSubstitute() {
        ok("e = Expand((x + 1)^2)");             // x^2 + 2x + 1
        assertEquals(9.0, eval("e", 2), 1e-9);   // 4 + 4 + 1
        ok("v = Substitute(x^2 + 1, x, 3)");
        assertEquals(10.0, num("v"), 1e-9);
    }

    @Test
    @DisplayName("Factor recovers roots of x^2 - 1")
    void factor() {
        ok("p = Factor(x^2 - 1)");               // (x-1)(x+1) -> still 0 at ±1
        assertEquals(0.0, eval("p", 1), 1e-6);
        assertEquals(0.0, eval("p", -1), 1e-6);
        assertEquals(3.0, eval("p", 2), 1e-6);   // (2-1)(2+1) = 3
    }
}
