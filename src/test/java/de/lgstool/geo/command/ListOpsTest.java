package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListOpsTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private void ok(String line) {
        GeoResult r = engine.execute(line);
        assertTrue(r.ok(), () -> "expected ok for '" + line + "' but got: " + r.messages());
    }

    private double num(String name) {
        return ((Value.NumberV) engine.construction().get(name).value()).v();
    }

    private Value.ListV list(String name) {
        return (Value.ListV) engine.construction().get(name).value();
    }

    @Test
    @DisplayName("list literal, Sum, Mean, Element and Length")
    void basics() {
        ok("L = {1, 2, 3, 4}");
        assertEquals(4, list("L").items().size());
        ok("s = Sum(L)");
        ok("m = Mean(L)");
        ok("e = Element(L, 2)");
        ok("n = Length(L)");
        assertEquals(10.0, num("s"), 1e-9);
        assertEquals(2.5, num("m"), 1e-9);
        assertEquals(2.0, num("e"), 1e-9);
        assertEquals(4.0, num("n"), 1e-9);
    }

    @Test
    @DisplayName("Sequence builds a numeric range")
    void sequence() {
        ok("R = Sequence(1, 5)");
        assertEquals(5, list("R").items().size());
        ok("s = Sum(R)");
        assertEquals(15.0, num("s"), 1e-9);
    }

    @Test
    @DisplayName("Sort orders a list ascending")
    void sort() {
        ok("S = Sort({3, 1, 2})");
        Value.ListV s = list("S");
        assertEquals(1.0, ((Value.NumberV) s.items().get(0)).v(), 1e-9);
        assertEquals(3.0, ((Value.NumberV) s.items().get(2)).v(), 1e-9);
    }

    @Test
    @DisplayName("a list of points is a list, not a matrix")
    void listOfPoints() {
        ok("P = {(1,2), (3,4)}");
        assertTrue(list("P").items().get(0) instanceof Value.PointV);
    }
}
