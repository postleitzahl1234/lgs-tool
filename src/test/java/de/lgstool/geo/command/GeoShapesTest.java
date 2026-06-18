package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeoShapesTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private Value valueOf(String name) {
        return engine.construction().get(name).value();
    }

    @Test
    @DisplayName("Polygon collects its vertices")
    void polygon() {
        engine.execute("A = (0, 0)");
        engine.execute("B = (4, 0)");
        engine.execute("C = (0, 3)");
        engine.execute("p = Polygon(A, B, C)");
        assertTrue(valueOf("p") instanceof Value.PolygonV poly && poly.vertices().size() == 3);
    }

    @Test
    @DisplayName("Angle at the vertex is measured in degrees")
    void angle() {
        engine.execute("w = Angle((1, 0), (0, 0), (0, 1))");
        assertEquals(90, ((Value.NumberV) valueOf("w")).v(), 1e-6);
    }

    @Test
    @DisplayName("Vector(A, B) carries its base point and direction")
    void vectorWithBase() {
        engine.execute("A = (1, 1)");
        engine.execute("B = (4, 5)");
        engine.execute("v = Vector(A, B)");
        assertTrue(valueOf("v") instanceof Value.VectorV vec
                && vec.x() == 3 && vec.y() == 4
                && vec.base() != null && vec.base().x() == 1 && vec.base().y() == 1);
    }
}
