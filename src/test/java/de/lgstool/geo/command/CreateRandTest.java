package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** create(name, variables*rows) and rand([name,] lo, hi). */
class CreateRandTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private void ok(String line) {
        assertTrue(engine.execute(line).ok(), () -> "failed: " + line + " -> " + engine.execute(line).messages());
    }

    private Value.MatrixV matrix(String name) {
        return (Value.MatrixV) engine.construction().get(name).value();
    }

    @Test
    @DisplayName("create makes an editable augmented LGS table with an extra RHS column")
    void createZeroMatrix() {
        ok("create(M, 3*2)");
        Value.MatrixV m = matrix("M");
        assertEquals(2, m.rowCount());   // rows
        assertEquals(4, m.colCount());   // 3 variables + RHS
        assertEquals("0", m.rows()[0][0].toString());
        assertEquals("0", m.rows()[0][3].toString());
        assertTrue(engine.construction().get("M").isLinearSystem());
    }

    @Test
    @DisplayName("rand fills the last-created matrix with values inside [lo, hi]")
    void randFillsLastMatrix() {
        ok("create(M, 2*2)");
        ok("rand(1, 6)");                // fills M (the last created matrix)
        Value.MatrixV m = matrix("M");
        for (int i = 0; i < m.rowCount(); i++) {
            for (int j = 0; j < m.colCount(); j++) {
                double v = m.rows()[i][j].doubleValue();
                assertTrue(v >= 1 && v <= 6, "value out of range: " + v);
            }
        }
    }

    @Test
    @DisplayName("rand can target a named matrix and keeps it the same shape")
    void randNamedMatrix() {
        ok("create(A, 4*1)");
        ok("rand(A, 0, 0)");            // all zeros
        Value.MatrixV m = matrix("A");
        assertEquals(1, m.rowCount());
        assertEquals(5, m.colCount());
        assertEquals("0", m.rows()[0][4].toString());
    }

    @Test
    @DisplayName("rand without a matrix fails cleanly")
    void randNoMatrix() {
        assertFalse(engine.execute("rand(1, 9)").ok());
    }
}
