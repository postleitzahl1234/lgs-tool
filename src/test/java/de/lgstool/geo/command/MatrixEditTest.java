package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The matrix-cell edit path: GeoFormat.matrixLiteralWith builds a re-parseable literal that redefines the matrix. */
class MatrixEditTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private Value.MatrixV matrix(String name) {
        return (Value.MatrixV) engine.construction().get(name).value();
    }

    @Test
    @DisplayName("matrixLiteralWith replaces one entry and leaves the rest intact")
    void literalWith() {
        Value.MatrixV m = new Value.MatrixV(new de.lgstool.math.Fraction[][]{
                {de.lgstool.math.Fraction.of(1), de.lgstool.math.Fraction.of(2)},
                {de.lgstool.math.Fraction.of(3), de.lgstool.math.Fraction.of(4)}});
        assertEquals("{{1, 9}, {3, 4}}", GeoFormat.matrixLiteralWith(m, 0, 1, "9"));
        assertEquals("{{1, 2}, {3, 4}}", GeoFormat.matrixLiteral(m));
        assertEquals("{{1, 2}, {0, 4}}", GeoFormat.matrixLiteralWith(m, 1, 0, "  "));   // blank -> 0
    }

    @Test
    @DisplayName("Editing a matrix cell (via the literal) redefines the matrix and updates dependents")
    void editCellRedefines() {
        engine.execute("create(M, 3*2)");          // 2 rows, 3 variables + RHS
        engine.execute("d = Det({{1,2},{3,4}})");  // unrelated dependent, must survive
        Value.MatrixV m = matrix("M");

        // Simulate an algebra-view cell edit of the RHS column (col 3) in row 0.
        String literal = GeoFormat.matrixLiteralWith(m, 0, 3, "7");
        assertTrue(engine.execute("M = " + literal).ok());

        assertEquals("7", matrix("M").rows()[0][3].toString());
        assertEquals("0", matrix("M").rows()[0][0].toString());
        assertEquals(2, matrix("M").rowCount());
        assertEquals(4, matrix("M").colCount());
    }

    @Test
    @DisplayName("RHS-column edit on a fraction matrix parses fractions back exactly")
    void fractionRhsEdit() {
        engine.execute("A = {{1, 2, 3}, {4, 5, 6}}");
        Value.MatrixV m = matrix("A");
        engine.execute("A = " + GeoFormat.matrixLiteralWith(m, 1, 2, "1/2"));
        assertEquals("1/2", matrix("A").rows()[1][2].toString());
    }

    @Test
    @DisplayName("editing the RHS to a new variable creates a free number that can become a slider")
    void rhsEditCreatesVariableForSlider() {
        engine.execute("create(M, 3*1)");
        engine.editMatrixCell("M", 0, 3, "3");

        assertTrue(engine.editMatrixCell("M", 0, 3, "rhs").ok());

        assertTrue(engine.construction().contains("rhs"));
        assertEquals(3.0, ((Value.NumberV) engine.construction().get("rhs").value()).v(), 1e-9);
        assertEquals("3", matrix("M").rows()[0][3].toString());
        assertTrue(engine.editableLine(engine.construction().get("M")).contains("rhs"));

        assertTrue(engine.execute("rhs = Slider(0, 10, 1)").ok());
        engine.setFreeValueLive("rhs", new Value.NumberV(8));

        assertEquals("8", matrix("M").rows()[0][3].toString());
    }

    @Test
    @DisplayName("editing one matrix cell keeps existing coefficient variables live")
    void matrixCellEditPreservesExistingVariables() {
        engine.execute("a = Slider(0, 10, 1)");
        engine.execute("b = 3");
        engine.execute("M = {{a, 2, 3}}");

        assertTrue(engine.editMatrixCell("M", 0, 2, "b").ok());
        engine.setFreeValueLive("a", new Value.NumberV(7));
        engine.setFreeValueLive("b", new Value.NumberV(9));

        assertEquals("7", matrix("M").rows()[0][0].toString());
        assertEquals("9", matrix("M").rows()[0][2].toString());
        assertTrue(engine.editableLine(engine.construction().get("M")).contains("{{a, 2, b}}"));
    }
}
