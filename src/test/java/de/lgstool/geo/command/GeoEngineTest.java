package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import de.lgstool.geo.model.GeoObject;
import de.lgstool.geo.model.GeoType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeoEngineTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private void ok(String line) {
        GeoResult r = engine.execute(line);
        assertTrue(r.ok(), () -> "expected ok for '" + line + "' but got: " + r.messages());
    }

    private Value valueOf(String name) {
        return engine.construction().get(name).value();
    }

    @Test
    @DisplayName("point assignment creates a free draggable point")
    void pointAssignment() {
        ok("A = (1, 2)");
        GeoObject a = engine.construction().get("A");
        assertEquals(GeoType.POINT, a.type());
        assertTrue(a.isDraggable());
    }

    @Test
    @DisplayName("dependent circle follows its centre and radius")
    void dependentCircle() {
        ok("A = (0, 0)");
        ok("r = 5");
        ok("c = Circle(A, r)");
        assertTrue(valueOf("c") instanceof Value.CircleV cv && cv.r() == 5);

        engine.setFreeValue("A", Value.PointV.of2D(3, 4));
        assertTrue(valueOf("c") instanceof Value.CircleV cv && cv.center().x() == 3 && cv.center().y() == 4);
    }

    @Test
    @DisplayName("Midpoint updates live when a base point is dragged")
    void midpointUpdates() {
        ok("A = (0, 0)");
        ok("B = (4, 0)");
        ok("M = Midpoint(A, B)");
        assertTrue(valueOf("M") instanceof Value.PointV p && p.x() == 2 && p.y() == 0);

        engine.setFreeValueLive("B", Value.PointV.of2D(10, 0));
        assertTrue(valueOf("M") instanceof Value.PointV p && p.x() == 5);
    }

    @Test
    @DisplayName("two lines intersect in the expected point")
    void intersectLines() {
        ok("g = Line((0, 0), (1, 1))");
        ok("h = Line((0, 2), (1, 1))");
        ok("S = Intersect(g, h)");
        assertTrue(valueOf("S") instanceof Value.PointV p
                && Math.abs(p.x() - 1) < 1e-9 && Math.abs(p.y() - 1) < 1e-9);
    }

    @Test
    @DisplayName("Slider creates a free number with range metadata")
    void slider() {
        ok("a = Slider(0, 10)");
        GeoObject a = engine.construction().get("a");
        assertTrue(a.isSlider());
        assertEquals(0, ((Value.NumberV) a.value()).v(), 1e-9);
        assertEquals(10, a.sliderMax(), 1e-9);
    }

    @Test
    @DisplayName("function definition is stored as a FUNCTION object")
    void functionDefinition() {
        ok("f(x) = x^2 + 1");
        assertEquals(GeoType.FUNCTION, engine.construction().get("f").type());
    }

    @Test
    @DisplayName("3D point and plane are created in space")
    void threeD() {
        ok("P = (1, 1, 1)");
        ok("E = Plane((0,0,0), (1,0,0), (0,1,0))");
        assertTrue(valueOf("P") instanceof Value.PointV p && p.is3D());
        assertTrue(valueOf("E") instanceof Value.PlaneV);
    }

    @Test
    @DisplayName("a bare command is auto-named by type (points upper, others lower)")
    void autoNaming() {
        ok("(2, 3)");
        ok("Line((0,0), (1,0))");
        assertTrue(engine.construction().contains("A"));   // point -> uppercase
        assertTrue(engine.construction().contains("a"));   // line  -> lowercase
    }

    @Test
    @DisplayName("Delete cascades and undo restores the construction")
    void deleteAndUndo() {
        ok("A = (0, 0)");
        ok("c = Circle(A, 3)");
        ok("Delete(A)");
        assertFalse(engine.construction().contains("A"));
        assertFalse(engine.construction().contains("c"));

        ok("undo");
        assertTrue(engine.construction().contains("A"));
        assertTrue(engine.construction().contains("c"));
    }

    @Test
    @DisplayName("errors are reported, not thrown")
    void errorsReported() {
        assertFalse(engine.execute("c = Circle(Nope, 3)").ok());  // unknown reference
        assertFalse(engine.execute("2 +").ok());                  // syntax error
        ok("A = (1, 2)");
        assertFalse(engine.execute("A = A + 1").ok());            // self-cycle
    }
}
