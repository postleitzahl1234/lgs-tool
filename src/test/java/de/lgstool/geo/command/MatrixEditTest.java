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
        engine.execute("create(M, 3*2)");          // 2x3 augmented system, all zeros
        engine.execute("d = Det({{1,2},{3,4}})");  // unrelated dependent, must survive
        Value.MatrixV m = matrix("M");

        // Simulate an algebra-view cell edit of the RHS column (col 2) in row 0.
        String literal = GeoFormat.matrixLiteralWith(m, 0, 2, "7");
        assertTrue(engine.execute("M = " + literal).ok());

        assertEquals("7", matrix("M").rows()[0][2].toString());
        assertEquals("0", matrix("M").rows()[0][0].toString());
        assertEquals(2, matrix("M").rowCount());
        assertEquals(3, matrix("M").colCount());
    }

    @Test
    @DisplayName("RHS-column edit on a fraction matrix parses fractions back exactly")
    void fractionRhsEdit() {
        engine.execute("A = {{1, 2, 3}, {4, 5, 6}}");
        Value.MatrixV m = matrix("A");
        engine.execute("A = " + GeoFormat.matrixLiteralWith(m, 1, 2, "1/2"));
        assertEquals("1/2", matrix("A").rows()[1][2].toString());
    }
}
