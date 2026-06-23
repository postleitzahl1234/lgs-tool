package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import de.lgstool.math.Fraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatrixOpsTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private void ok(String line) {
        GeoResult r = engine.execute(line);
        assertTrue(r.ok(), () -> "expected ok for '" + line + "' but got: " + r.messages());
    }

    private Value.MatrixV matrix(String name) {
        return (Value.MatrixV) engine.construction().get(name).value();
    }

    private double num(String name) {
        return ((Value.NumberV) engine.construction().get(name).value()).v();
    }

    @Test
    @DisplayName("a matrix literal keeps its entries exact (1/3 stays 1/3)")
    void exactEntries() {
        ok("M = {{1, 1/2}, {1/3, 4}}");
        Fraction[][] m = matrix("M").rows();
        assertEquals(Fraction.ONE, m[0][0]);
        assertEquals(Fraction.of(1, 2), m[0][1]);
        assertEquals(Fraction.of(1, 3), m[1][0]);
        assertEquals(Fraction.of(4), m[1][1]);
    }

    @Test
    @DisplayName("Det, Rank and Trace")
    void scalars() {
        ok("A = {{1, 2}, {3, 4}}");
        ok("d = Det(A)");
        ok("r = Rank(A)");
        ok("t = Trace(A)");
        assertEquals(-2.0, num("d"), 1e-9);
        assertEquals(2.0, num("r"), 1e-9);
        assertEquals(5.0, num("t"), 1e-9);
    }

    @Test
    @DisplayName("Inverse is exact")
    void inverse() {
        ok("A = {{1, 2}, {3, 4}}");
        ok("I = Inverse(A)");
        Fraction[][] inv = matrix("I").rows();
        assertEquals(Fraction.of(-2), inv[0][0]);
        assertEquals(Fraction.ONE, inv[0][1]);
        assertEquals(Fraction.of(3, 2), inv[1][0]);
        assertEquals(Fraction.of(-1, 2), inv[1][1]);
    }

    @Test
    @DisplayName("Transpose and Identity")
    void transposeIdentity() {
        ok("A = {{1, 2}, {3, 4}}");
        ok("T = Transpose(A)");
        Fraction[][] t = matrix("T").rows();
        assertEquals(Fraction.of(3), t[0][1]);
        assertEquals(Fraction.of(2), t[1][0]);

        ok("Id = Identity(3)");
        assertEquals(3, matrix("Id").rowCount());
        assertEquals(Fraction.ONE, matrix("Id").rows()[1][1]);
        assertEquals(Fraction.ZERO, matrix("Id").rows()[0][1]);
    }

    @Test
    @DisplayName("Solve an augmented matrix gives an exact column vector")
    void solve() {
        ok("M = {{1, 1, 3}, {1, -1, 1}}");   // x + y = 3, x - y = 1  ->  x=2, y=1
        ok("s = Solve(M)");
        Fraction[][] s = matrix("s").rows();
        assertEquals(2, matrix("s").rowCount());
        assertEquals(1, matrix("s").colCount());
        assertEquals(Fraction.of(2), s[0][0]);
        assertEquals(Fraction.ONE, s[1][0]);
    }

    @Test
    @DisplayName("matrix product and sum are exact")
    void arithmetic() {
        ok("A = {{1, 2}, {3, 4}}");
        ok("B = {{0, 1}, {1, 0}}");
        ok("P = A * B");
        Fraction[][] p = matrix("P").rows();
        assertEquals(Fraction.of(2), p[0][0]);   // 1*0+2*1
        assertEquals(Fraction.ONE, p[0][1]);     // 1*1+2*0
        ok("S = A + B");
        assertEquals(Fraction.of(3), matrix("S").rows()[0][1]);   // 2+1
    }

    @Test
    @DisplayName("RREF reduces a system")
    void rref() {
        ok("M = {{2, 4}, {1, 3}}");
        ok("R = RREF(M)");
        Fraction[][] r = matrix("R").rows();
        assertEquals(Fraction.ONE, r[0][0]);
        assertEquals(Fraction.ZERO, r[0][1]);
        assertEquals(Fraction.ZERO, r[1][0]);
        assertEquals(Fraction.ONE, r[1][1]);
    }

    @Test
    @DisplayName("a singular inverse is reported, not thrown")
    void singular() {
        ok("A = {{1, 2}, {2, 4}}");
        assertFalse(engine.execute("I = Inverse(A)").ok());
    }
}
