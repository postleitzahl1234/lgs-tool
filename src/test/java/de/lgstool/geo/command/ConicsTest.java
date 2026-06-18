package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConicsTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private Value.ConicV conic(String name) {
        return (Value.ConicV) engine.construction().get(name).value();
    }

    private static double q(Value.ConicV c, double x, double y) {
        return c.a() * x * x + c.b() * x * y + c.c() * y * y + c.d() * x + c.e() * y + c.f();
    }

    @Test
    @DisplayName("ellipse passes through its vertices and excludes its centre")
    void ellipse() {
        // foci (±3, 0), a = 5  ->  b = 4, vertices (±5,0), co-vertices (0,±4)
        engine.execute("c = Ellipse((-3, 0), (3, 0), 5)");
        Value.ConicV e = conic("c");
        assertEquals(0, q(e, 5, 0), 1e-9);
        assertEquals(0, q(e, 0, 4), 1e-9);
        assertTrue(q(e, 0, 0) < 0);   // centre is inside
    }

    @Test
    @DisplayName("parabola y = x^2/4 passes through its vertex and a known point")
    void parabola() {
        engine.execute("d = Line((-5, -1), (5, -1))");   // directrix y = -1
        engine.execute("p = Parabola((0, 1), d)");        // focus (0,1)
        Value.ConicV par = conic("p");
        assertEquals(0, q(par, 0, 0), 1e-6);
        assertEquals(0, q(par, 2, 1), 1e-6);
    }
}
