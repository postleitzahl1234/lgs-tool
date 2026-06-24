package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import de.lgstool.geo.expr.MatrixBuiltins;
import de.lgstool.geo.expr.Value;
import de.lgstool.math.Fraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The unique solution of a linear system, and how it tracks an animated (slider) parameter. */
class SolutionTrackingTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private Value.MatrixV matrix(String name) {
        return (Value.MatrixV) engine.construction().get(name).value();
    }

    @Test
    @DisplayName("uniqueSolution solves a 2-variable system and rejects parallel lines")
    void uniqueSolutionBasics() {
        double[] s = MatrixBuiltins.uniqueSolution(new Fraction[][]{
                {Fraction.of(1), Fraction.of(1), Fraction.of(3)},
                {Fraction.of(1), Fraction.of(-1), Fraction.of(1)}});
        assertEquals(2.0, s[0], 1e-9);
        assertEquals(1.0, s[1], 1e-9);

        assertNull(MatrixBuiltins.uniqueSolution(new Fraction[][]{   // parallel -> no unique point
                {Fraction.of(1), Fraction.of(1), Fraction.of(1)},
                {Fraction.of(1), Fraction.of(1), Fraction.of(2)}}));
    }

    @Test
    @DisplayName("The solution follows a slider-coupled right-hand side")
    void solutionTracksSlider() {
        engine.execute("k = Slider(0, 10, 1)");
        engine.execute("M = {{1, 0, k}, {0, 1, 2}}");   // x = k, y = 2

        double[] s0 = MatrixBuiltins.uniqueSolution(matrix("M").rows());
        assertEquals(0.0, s0[0], 1e-9);     // k = 0
        assertEquals(2.0, s0[1], 1e-9);

        engine.setFreeValueLive("k", new Value.NumberV(7));   // animate the parameter
        double[] s1 = MatrixBuiltins.uniqueSolution(matrix("M").rows());
        assertEquals(7.0, s1[0], 1e-9);     // solution moved with k
        assertEquals(2.0, s1[1], 1e-9);
    }
}
