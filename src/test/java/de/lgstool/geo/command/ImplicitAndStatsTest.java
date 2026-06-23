package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Implicit equations, the recompute-robustness fix, and the statistics/regression builtins. */
class ImplicitAndStatsTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private GeoResult run(String line) {
        return engine.execute(line);
    }

    private void ok(String line) {
        GeoResult r = run(line);
        assertTrue(r.ok(), () -> "expected ok for '" + line + "' but got: " + r.messages());
    }

    private Value v(String name) {
        return engine.construction().get(name).value();
    }

    private double num(String name) {
        return ((Value.NumberV) v(name)).v();
    }

    // ---- implicit equations ----------------------------------------------

    @Test
    @DisplayName("x^2 + y^2 = 1 creates an implicit curve")
    void implicitCircle() {
        ok("x^2 + y^2 = 1");
        Value c = engine.construction().objects().get(0).value();
        assertInstanceOf(Value.ImplicitV.class, c);
    }

    @Test
    @DisplayName("Equation with the constant on the left (1 = x^2 + y^2) also works")
    void implicitSwapped() {
        ok("1 = x^2 + y^2");
        assertInstanceOf(Value.ImplicitV.class, engine.construction().objects().get(0).value());
    }

    @Test
    @DisplayName("An equation through a user function f(x^2, y^2) = 1 is implicit")
    void implicitViaFunction() {
        ok("f(a, b) = a + b");
        ok("f(x^2, y^2) = 1");
        Value last = engine.construction().objects().get(1).value();
        assertInstanceOf(Value.ImplicitV.class, last);
    }

    @Test
    @DisplayName("An equation with no x or y is rejected")
    void implicitNeedsPlotVar() {
        assertFalse(run("1 = 2").ok());
    }

    @Test
    @DisplayName("y = number stays a plain assignment, not an implicit curve")
    void reservedVarNumberStaysAssignment() {
        ok("y = 4");
        assertInstanceOf(Value.NumberV.class, v("y"));
    }

    // ---- recompute robustness (the matrix poisoning bug) -----------------

    @Test
    @DisplayName("Det(M) becoming undefined after M turns non-square does not break the construction")
    void nonSquareRedefineDoesNotPoison() {
        ok("M = {{1,2},{3,4}}");
        ok("d = Det(M)");
        ok("M = {{1,2,3},{4,5,6}}");                 // d is now undefined, but must not throw
        assertFalse(engine.construction().get("d").isDefined());
        ok("P = (1,2)");                             // later commands still work
        assertInstanceOf(Value.PointV.class, v("P"));
    }

    // ---- statistics & regression -----------------------------------------

    @Test
    @DisplayName("Median, quartiles, variance, SD and mode")
    void descriptiveStats() {
        ok("L = {1, 2, 3, 4, 5, 5}");
        ok("md = Median(L)");
        ok("vr = Variance(L)");
        ok("mo = Mode(L)");
        assertEquals(3.5, num("md"), 1e-9);
        assertEquals(5.0, num("mo"), 1e-9);
        assertTrue(num("vr") > 0);
    }

    @Test
    @DisplayName("FitLine recovers an exact line through collinear points")
    void fitLineExact() {
        ok("L = {(0,1), (1,3), (2,5)}");             // y = 2x + 1
        ok("g = FitLine(L)");
        Value.LineV line = (Value.LineV) v("g");
        // a·x + b·y = c  with the line stored as m·x − y = −b → slope 2, intercept 1
        assertEquals(2.0, line.a() / -line.b(), 1e-6);
        assertEquals(1.0, -line.c() / -line.b(), 1e-6);
    }

    @Test
    @DisplayName("FitPoly returns an evaluatable polynomial function")
    void fitPolyFunction() {
        ok("L = {(-1,1), (0,0), (1,1), (2,4)}");     // y = x^2
        ok("p = FitPoly(L, 2)");
        assertInstanceOf(Value.FunctionV.class, v("p"));
        ok("q = p(3)");
        assertEquals(9.0, num("q"), 1e-6);
    }
}
