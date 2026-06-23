package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Spreadsheet cells are ordinary named objects (A1, B2 …) wired through the dependency graph. */
class SpreadsheetCellTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private void ok(String line) {
        assertTrue(engine.execute(line).ok(), () -> "failed: " + line);
    }

    private double num(String name) {
        return ((Value.NumberV) engine.construction().get(name).value()).v();
    }

    @Test
    @DisplayName("A cell formula referencing other cells recomputes when an input cell changes")
    void cellReferences() {
        ok("A1 = 5");
        ok("A2 = 7");
        ok("B1 = A1 + A2");
        ok("B2 = B1 * 2");
        assertEquals(12.0, num("B1"), 1e-9);
        assertEquals(24.0, num("B2"), 1e-9);

        ok("A1 = 10");                 // redefine an input cell (10 + 7 = 17)
        assertEquals(17.0, num("B1"), 1e-9);   // dependents update
        assertEquals(34.0, num("B2"), 1e-9);
    }
}
