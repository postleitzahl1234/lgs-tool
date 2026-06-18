package de.lgstool.geo.expr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EvaluatorTest {

    private static final double EPS = 1e-9;

    private static Value eval(String src, Map<String, Value> scope) {
        return new Evaluator(scope::get, new MathBuiltins()).eval(Parser.parse(src));
    }

    private static double num(String src) {
        return ((Value.NumberV) eval(src, Map.of())).v();
    }

    @Test
    @DisplayName("operator precedence and associativity")
    void precedence() {
        assertEquals(14, num("2 + 3 * 4"), EPS);
        assertEquals(20, num("(2 + 3) * 4"), EPS);
        assertEquals(512, num("2 ^ 3 ^ 2"), EPS);   // right associative: 2^(3^2)
        assertEquals(-4, num("-2 ^ 2"), EPS);        // unary minus below ^: -(2^2)
        assertEquals(0.25, num("2 ^ -2"), EPS);
        assertEquals(1.2, num("6 / 5"), EPS);
    }

    @Test
    @DisplayName("division by zero is rejected")
    void divisionByZero() {
        assertThrows(EvalException.class, () -> num("1 / 0"));
    }

    @Test
    @DisplayName("tuples evaluate to 2D and 3D points")
    void points() {
        Value p2 = eval("(1, 2)", Map.of());
        assertTrue(p2 instanceof Value.PointV pv && !pv.is3D() && pv.x() == 1 && pv.y() == 2);
        Value p3 = eval("(1, 2, 3)", Map.of());
        assertTrue(p3 instanceof Value.PointV pv && pv.is3D() && pv.z() == 3);
    }

    @Test
    @DisplayName("point/vector arithmetic follows GeoGebra type rules")
    void pointArithmetic() {
        assertTrue(eval("(1, 2) + (3, 4)", Map.of()) instanceof Value.PointV p && p.x() == 4 && p.y() == 6);
        assertTrue(eval("(3, 4) - (1, 1)", Map.of()) instanceof Value.VectorV v && v.x() == 2 && v.y() == 3);
        assertTrue(eval("3 * (1, 2)", Map.of()) instanceof Value.PointV p && p.x() == 3 && p.y() == 6);
    }

    @Test
    @DisplayName("named objects resolve from the scope")
    void scopeLookup() {
        Map<String, Value> scope = Map.of("a", new Value.NumberV(10), "b", new Value.NumberV(4));
        assertEquals(14, ((Value.NumberV) eval("a + b", scope)).v(), EPS);
    }

    @Test
    @DisplayName("user functions apply with bound parameters")
    void userFunction() {
        Value.FunctionV f = new Value.FunctionV(List.of("x"), Parser.parse("x ^ 2 + 1"));
        Map<String, Value> scope = Map.of("f", f);
        assertEquals(10, ((Value.NumberV) eval("f(3)", scope)).v(), EPS);
    }

    @Test
    @DisplayName("maths builtins and constants")
    void mathBuiltins() {
        assertEquals(3, num("sqrt(9)"), EPS);
        assertEquals(0, num("sin(0)"), EPS);
        assertEquals(5, num("max(2, 5)"), EPS);
        assertEquals(Math.PI, num("pi"), EPS);
    }

    @Test
    @DisplayName("unknown identifiers and functions are rejected")
    void unknownNames() {
        assertThrows(EvalException.class, () -> num("unknownVar + 1"));
        assertThrows(EvalException.class, () -> num("Bogus(1, 2)"));
    }
}
