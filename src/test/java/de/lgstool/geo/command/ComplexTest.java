package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Complex numbers: the imaginary unit, arithmetic, and accessors. */
class ComplexTest {

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

    @Test
    @DisplayName("i squared collapses to the real number -1")
    void iSquared() {
        ok("z = i * i");
        assertEquals(-1.0, num("z"), 1e-12);
    }

    @Test
    @DisplayName("Complex addition and multiplication")
    void arithmetic() {
        ok("z = (3 + 4i) * (1 - 2i)");   // = 11 - 2i
        Value.ComplexV c = (Value.ComplexV) v("z");
        assertEquals(11.0, c.re(), 1e-9);
        assertEquals(-2.0, c.im(), 1e-9);
    }

    @Test
    @DisplayName("Modulus, Real, Imaginary and Conjugate")
    void accessors() {
        ok("z = 3 + 4i");
        ok("m = Modulus(z)");
        ok("re = Real(z)");
        ok("im = Imaginary(z)");
        ok("c = Conjugate(z)");
        assertEquals(5.0, num("m"), 1e-9);
        assertEquals(3.0, num("re"), 1e-9);
        assertEquals(4.0, num("im"), 1e-9);
        assertInstanceOf(Value.ComplexV.class, v("c"));
        assertEquals(-4.0, ((Value.ComplexV) v("c")).im(), 1e-9);
    }

    @Test
    @DisplayName("true/false produce boolean objects")
    void booleanLiterals() {
        ok("b = true");
        assertInstanceOf(Value.BooleanV.class, v("b"));
        assertTrue(((Value.BooleanV) v("b")).value());
    }
}
