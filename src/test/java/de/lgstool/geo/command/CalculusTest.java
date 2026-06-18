package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Parser;
import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculusTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private double eval(String expr) {
        return ((Value.NumberV) engine.construction().evaluate(Parser.parse(expr))).v();
    }

    @Test
    @DisplayName("Derivative produces a usable function")
    void derivative() {
        engine.execute("f(x) = x^2");
        engine.execute("g = Derivative(f)");
        assertEquals(8, eval("g(4)"), 1e-6);
    }

    @Test
    @DisplayName("Integral computes a definite integral numerically")
    void integral() {
        engine.execute("f(x) = x^2");
        engine.execute("a = Integral(f, 0, 3)");   // x^3/3 from 0..3 = 9
        assertEquals(9, ((Value.NumberV) engine.construction().get("a").value()).v(), 1e-4);
    }

    @Test
    @DisplayName("Tangent returns the tangent line at a point")
    void tangent() {
        engine.execute("f(x) = x^2");
        engine.execute("t = Tangent(1, f)");        // y = 1 + 2(x-1) -> 2x - y = 1
        assertTrue(engine.construction().get("t").value() instanceof Value.LineV l
                && Math.abs(l.a() - 2) < 1e-6 && Math.abs(l.b() + 1) < 1e-6 && Math.abs(l.c() - 1) < 1e-6);
    }
}
