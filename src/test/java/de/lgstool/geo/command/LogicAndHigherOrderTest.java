package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Booleans, comparison/logical operators, If, and lazy higher-order list functions. */
class LogicAndHigherOrderTest {

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

    private boolean bool(String name) {
        return ((Value.BooleanV) v(name)).value();
    }

    private double num(String name) {
        return ((Value.NumberV) v(name)).v();
    }

    private java.util.List<Value> list(String name) {
        return ((Value.ListV) v(name)).items();
    }

    @Test
    @DisplayName("Comparison and logical operators produce booleans")
    void booleans() {
        ok("a = 3 > 2");
        ok("b = 2 == 2");
        ok("c = (1 < 2) & (5 > 9)");
        assertTrue(bool("a"));
        assertTrue(bool("b"));
        assertFalse(bool("c"));
    }

    @Test
    @DisplayName("If returns the matching branch without evaluating the other")
    void ifThenElse() {
        ok("r = If(3 > 2, 10, 20)");
        assertEquals(10.0, num("r"), 1e-9);
        ok("s = If(1 > 2, 10, 20)");
        assertEquals(20.0, num("s"), 1e-9);
    }

    @Test
    @DisplayName("Sequence with an expression and a bound variable")
    void sequenceExpr() {
        ok("L = Sequence(k^2, k, 1, 4)");
        assertEquals(4, list("L").size());
        assertEquals(16.0, ((Value.NumberV) list("L").get(3)).v(), 1e-9);
    }

    @Test
    @DisplayName("Map applies an expression over a list")
    void map() {
        ok("L = {1, 2, 3}");
        ok("M = Map(2*n, n, L)");
        assertEquals(6.0, ((Value.NumberV) list("M").get(2)).v(), 1e-9);
    }

    @Test
    @DisplayName("KeepIf filters and CountIf counts by a predicate")
    void keepCount() {
        ok("L = {1, 2, 3, 4, 5, 6}");
        ok("E = KeepIf(n - 2*floor(n/2) == 0, n, L)");   // even numbers
        ok("c = CountIf(n > 3, n, L)");
        assertEquals(3, list("E").size());
        assertEquals(3.0, num("c"), 1e-9);
    }

    @Test
    @DisplayName("Take, Union and Intersection")
    void setOps() {
        ok("L = {1, 2, 3, 4, 5}");
        ok("t = Take(L, 2, 4)");
        ok("u = Union({1,2}, {2,3})");
        ok("i = Intersection({1,2,3}, {2,3,4})");
        assertEquals(3, list("t").size());
        assertEquals(3, list("u").size());
        assertEquals(2, list("i").size());
    }
}
