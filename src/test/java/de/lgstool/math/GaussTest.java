package de.lgstool.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GaussTest {

    private static Fraction[][] m(long[]... rows) {
        Fraction[][] out = new Fraction[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            out[r] = new Fraction[rows[r].length];
            for (int c = 0; c < rows[r].length; c++) {
                out[r][c] = Fraction.of(rows[r][c]);
            }
        }
        return out;
    }

    private static Fraction[] v(long... values) {
        Fraction[] out = new Fraction[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = Fraction.of(values[i]);
        }
        return out;
    }

    @Test
    void uniqueSolution() {
        // x + y = 3 ; x - y = 1  ->  x = 2, y = 1
        Solution s = Gauss.solve(m(new long[]{1, 1}, new long[]{1, -1}), v(3, 1));
        assertEquals(Solution.Kind.UNIQUE, s.kind());
        assertEquals(Fraction.of(2), s.particular()[0]);
        assertEquals(Fraction.of(1), s.particular()[1]);
    }

    @Test
    void noSolution() {
        // x + y = 1 ; x + y = 2
        Solution s = Gauss.solve(m(new long[]{1, 1}, new long[]{1, 1}), v(1, 2));
        assertEquals(Solution.Kind.NONE, s.kind());
    }

    @Test
    void infinitelyManySolutions() {
        // x + 2y = 3  ->  x = 3 - 2t, y = t
        Solution s = Gauss.solve(m(new long[]{1, 2}), v(3));
        assertEquals(Solution.Kind.INFINITE, s.kind());
        assertEquals(1, s.freeParameterCount());
        assertEquals(Fraction.of(3), s.particular()[0]);
        assertEquals(Fraction.ZERO, s.particular()[1]);
        assertTrue(s.freeVariableIndices().contains(1));
        String joined = String.join(" | ", s.describe(new String[]{"x", "y"}, true));
        assertTrue(joined.contains("frei"), joined);
    }

    @Test
    void determinant() {
        assertEquals(Fraction.of(-2), Gauss.determinant(m(new long[]{1, 2}, new long[]{3, 4})));
        assertEquals(Fraction.ZERO, Gauss.determinant(m(new long[]{1, 2}, new long[]{2, 4})));
    }

    @Test
    void rank() {
        assertEquals(2, Gauss.rank(m(new long[]{1, 2}, new long[]{3, 4})));
        assertEquals(1, Gauss.rank(m(new long[]{1, 2}, new long[]{2, 4})));
    }

    @Test
    void inverse() {
        Fraction[][] inv = Gauss.inverse(m(new long[]{1, 2}, new long[]{3, 4}));
        assertEquals(Fraction.of(-2), inv[0][0]);
        assertEquals(Fraction.of(1), inv[0][1]);
        assertEquals(Fraction.of(3, 2), inv[1][0]);
        assertEquals(Fraction.of(-1, 2), inv[1][1]);
    }

    @Test
    void stepsAreRecordedWhenRequested() {
        Gauss.Detailed d = Gauss.solveDetailed(m(new long[]{2, 0}, new long[]{0, 2}), v(4, 6), true);
        assertFalse(d.steps().isEmpty());
        assertEquals(Solution.Kind.UNIQUE, d.solution().kind());
        assertEquals(Fraction.of(2), d.solution().particular()[0]);
        assertEquals(Fraction.of(3), d.solution().particular()[1]);
    }

    @Test
    void twoFreeParameters() {
        // x + y + z = 1  ->  two free parameters
        Solution s = Gauss.solve(m(new long[]{1, 1, 1}), v(1));
        assertEquals(Solution.Kind.INFINITE, s.kind());
        assertEquals(2, s.freeParameterCount());
        assertEquals(Fraction.of(1), s.particular()[0]);
        long freeMarkers = s.describe(new String[]{"x", "y", "z"}, true).stream()
                .filter(line -> line.contains("(frei)")).count();
        assertEquals(2, freeMarkers);
    }

    @Test
    void determinantThreeByThree() {
        assertEquals(Fraction.of(24),
                Gauss.determinant(m(new long[]{2, 0, 0}, new long[]{0, 3, 0}, new long[]{0, 0, 4})));
        assertEquals(Fraction.of(-3),
                Gauss.determinant(m(new long[]{1, 2, 3}, new long[]{4, 5, 6}, new long[]{7, 8, 10})));
    }

    @Test
    void inverseTimesMatrixIsIdentity() {
        Fraction[][] a = m(new long[]{2, 1}, new long[]{1, 1});
        Fraction[][] inv = Gauss.inverse(a);
        // a * inv should be identity
        assertEquals(Fraction.ONE, a[0][0].multiply(inv[0][0]).add(a[0][1].multiply(inv[1][0])));
        assertEquals(Fraction.ZERO, a[0][0].multiply(inv[0][1]).add(a[0][1].multiply(inv[1][1])));
    }
}
