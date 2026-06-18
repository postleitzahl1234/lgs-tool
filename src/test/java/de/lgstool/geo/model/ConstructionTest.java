package de.lgstool.geo.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.MathBuiltins;
import de.lgstool.geo.expr.Parser;
import de.lgstool.geo.expr.Value;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConstructionTest {

    private Construction con() {
        return new Construction(new MathBuiltins());
    }

    private GeoObject define(Construction c, String name, String src) {
        return c.define(name, src, Parser.parse(src), null);
    }

    private static double num(GeoObject o) {
        return ((Value.NumberV) o.value()).v();
    }

    @Test
    @DisplayName("a dependent number recomputes when its free input changes")
    void dependentNumberRecomputes() {
        Construction c = con();
        define(c, "a", "2");
        GeoObject b = define(c, "b", "a + 1");
        assertEquals(GeoObject.Kind.FREE, c.get("a").kind());
        assertEquals(GeoObject.Kind.DEPENDENT, b.kind());
        assertEquals(3, num(b));

        c.setFreeValue("a", new Value.NumberV(10));
        assertEquals(11, num(c.get("b")));
    }

    @Test
    @DisplayName("a dependent point updates when its base point is dragged")
    void dependentPointUpdatesOnDrag() {
        Construction c = con();
        define(c, "A", "(1, 2)");
        define(c, "B", "A + (1, 1)");
        assertTrue(c.get("B").value() instanceof Value.PointV p && p.x() == 2 && p.y() == 3);

        c.setFreeValue("A", Value.PointV.of2D(10, 10));
        assertTrue(c.get("B").value() instanceof Value.PointV p && p.x() == 11 && p.y() == 11);
    }

    @Test
    @DisplayName("a definition that would create a cycle is rejected and leaves state intact")
    void cycleRejected() {
        Construction c = con();
        define(c, "a", "2");
        define(c, "b", "a + 1");
        assertThrows(EvalException.class, () -> define(c, "a", "b + 1"));
        assertEquals(GeoObject.Kind.FREE, c.get("a").kind());
        assertEquals(2, num(c.get("a")));
    }

    @Test
    @DisplayName("deleting an object cascades to everything depending on it")
    void deleteCascades() {
        Construction c = con();
        define(c, "a", "2");
        define(c, "b", "a + 1");
        define(c, "d", "b + 1");
        List<String> removed = c.delete("a");
        assertTrue(removed.containsAll(List.of("a", "b", "d")));
        assertEquals(0, c.size());
    }

    @Test
    @DisplayName("a function definition has FUNCTION kind and a FunctionV value")
    void functionKind() {
        Construction c = con();
        GeoObject f = c.define("f", "x ^ 2", Parser.parse("x ^ 2"), List.of("x"));
        assertEquals(GeoObject.Kind.FUNCTION, f.kind());
        assertEquals(GeoType.FUNCTION, f.type());
    }

    @Test
    @DisplayName("copy() is a deep snapshot independent of later edits")
    void copyIsDeep() {
        Construction c = con();
        define(c, "a", "2");
        define(c, "b", "a + 1");
        Construction snapshot = c.copy();

        c.setFreeValue("a", new Value.NumberV(100));
        assertEquals(3, num(snapshot.get("b")));
        assertEquals(101, num(c.get("b")));
    }

    @Test
    @DisplayName("setFreeValue on a dependent object is rejected")
    void cannotSetDependent() {
        Construction c = con();
        define(c, "a", "2");
        define(c, "b", "a + 1");
        assertThrows(EvalException.class, () -> c.setFreeValue("b", new Value.NumberV(5)));
        assertFalse(c.get("b").kind() == GeoObject.Kind.FREE);
    }
}
