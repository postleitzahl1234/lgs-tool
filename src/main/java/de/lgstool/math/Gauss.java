package de.lgstool.math;

import java.util.ArrayList;
import java.util.List;

/**
 * Exact Gaussian elimination over {@link Fraction}: RREF (optionally with
 * recorded steps), rank, determinant, inverse and full system solving with
 * classification into unique / infinite (parametric) / no solution.
 */
public final class Gauss {

    private Gauss() {
    }

    /** Result of reducing a matrix to reduced row echelon form. */
    public record Rref(Fraction[][] matrix, List<Integer> pivotColumns, List<SolveStep> steps) {
    }

    public static Fraction[][] copy(Fraction[][] m) {
        Fraction[][] out = new Fraction[m.length][];
        for (int r = 0; r < m.length; r++) {
            out[r] = m[r].clone();
        }
        return out;
    }

    /**
     * Reduces a copy of {@code input} to RREF.
     *
     * @param recordSteps when true, each swap / scale / elimination is captured as a {@link SolveStep}
     */
    public static Rref rref(Fraction[][] input, boolean recordSteps) {
        Fraction[][] m = copy(input);
        int rows = m.length;
        int cols = rows == 0 ? 0 : m[0].length;
        List<Integer> pivots = new ArrayList<>();
        List<SolveStep> steps = new ArrayList<>();
        int pivotRow = 0;
        for (int col = 0; col < cols && pivotRow < rows; col++) {
            int sel = -1;
            for (int r = pivotRow; r < rows; r++) {
                if (!m[r][col].isZero()) {
                    sel = r;
                    break;
                }
            }
            if (sel < 0) {
                continue;
            }
            if (sel != pivotRow) {
                Fraction[] tmp = m[sel];
                m[sel] = m[pivotRow];
                m[pivotRow] = tmp;
                if (recordSteps) {
                    steps.add(new SolveStep(Roman.of(sel + 1) + " ↔ " + Roman.of(pivotRow + 1),
                            new FractionMatrix(m)));
                }
            }
            Fraction pivot = m[pivotRow][col];
            if (!pivot.isOne()) {
                for (int c = 0; c < cols; c++) {
                    m[pivotRow][c] = m[pivotRow][c].divide(pivot);
                }
                if (recordSteps) {
                    steps.add(new SolveStep(Roman.of(pivotRow + 1) + " × " + pivot.reciprocal(),
                            new FractionMatrix(m)));
                }
            }
            for (int r = 0; r < rows; r++) {
                if (r != pivotRow && !m[r][col].isZero()) {
                    Fraction factor = m[r][col];
                    for (int c = 0; c < cols; c++) {
                        m[r][c] = m[r][c].subtract(factor.multiply(m[pivotRow][c]));
                    }
                    if (recordSteps) {
                        steps.add(new SolveStep(Roman.of(r + 1) + " − (" + factor + ") · " + Roman.of(pivotRow + 1),
                                new FractionMatrix(m)));
                    }
                }
            }
            pivots.add(col);
            pivotRow++;
        }
        return new Rref(m, pivots, steps);
    }

    /** Rank of an arbitrary matrix. */
    public static int rank(Fraction[][] m) {
        return rref(m, false).pivotColumns().size();
    }

    /** Forward elimination only — row echelon form (pivots not normalized to 1). */
    public static Fraction[][] ref(Fraction[][] input) {
        Fraction[][] m = copy(input);
        int rows = m.length;
        int cols = rows == 0 ? 0 : m[0].length;
        int pivotRow = 0;
        for (int col = 0; col < cols && pivotRow < rows; col++) {
            int sel = -1;
            for (int r = pivotRow; r < rows; r++) {
                if (!m[r][col].isZero()) {
                    sel = r;
                    break;
                }
            }
            if (sel < 0) {
                continue;
            }
            if (sel != pivotRow) {
                Fraction[] tmp = m[sel];
                m[sel] = m[pivotRow];
                m[pivotRow] = tmp;
            }
            Fraction pivot = m[pivotRow][col];
            for (int r = pivotRow + 1; r < rows; r++) {
                if (!m[r][col].isZero()) {
                    Fraction f = m[r][col].divide(pivot);
                    for (int c = col; c < cols; c++) {
                        m[r][c] = m[r][c].subtract(f.multiply(m[pivotRow][c]));
                    }
                }
            }
            pivotRow++;
        }
        return m;
    }

    /** Basis of the null space (kernel) of {@code a}: all x with {@code A x = 0}. */
    public static List<Fraction[]> nullspace(Fraction[][] a) {
        int n = a.length == 0 ? 0 : a[0].length;
        Rref rref = rref(a, false);
        List<Integer> pivots = rref.pivotColumns();
        boolean[] isPivotCol = new boolean[n];
        for (int col : pivots) {
            if (col < n) {
                isPivotCol[col] = true;
            }
        }
        List<Fraction[]> basis = new ArrayList<>();
        for (int free = 0; free < n; free++) {
            if (isPivotCol[free]) {
                continue;
            }
            Fraction[] v = new Fraction[n];
            for (int i = 0; i < n; i++) {
                v[i] = Fraction.ZERO;
            }
            v[free] = Fraction.ONE;
            for (int k = 0; k < pivots.size(); k++) {
                int col = pivots.get(k);
                if (col < n) {
                    v[col] = rref.matrix()[k][free].negate();
                }
            }
            basis.add(v);
        }
        return basis;
    }

    /**
     * Determinant of a square matrix via fraction elimination.
     *
     * @throws ArithmeticException if not square
     */
    public static Fraction determinant(Fraction[][] input) {
        int n = input.length;
        if (n == 0) {
            return Fraction.ONE;
        }
        if (input[0].length != n) {
            throw new ArithmeticException("Determinante nur für quadratische Matrizen definiert.");
        }
        Fraction[][] m = copy(input);
        Fraction det = Fraction.ONE;
        for (int col = 0; col < n; col++) {
            int sel = -1;
            for (int r = col; r < n; r++) {
                if (!m[r][col].isZero()) {
                    sel = r;
                    break;
                }
            }
            if (sel < 0) {
                return Fraction.ZERO;
            }
            if (sel != col) {
                Fraction[] tmp = m[sel];
                m[sel] = m[col];
                m[col] = tmp;
                det = det.negate();
            }
            det = det.multiply(m[col][col]);
            Fraction pivot = m[col][col];
            for (int r = col + 1; r < n; r++) {
                if (!m[r][col].isZero()) {
                    Fraction f = m[r][col].divide(pivot);
                    for (int c = col; c < n; c++) {
                        m[r][c] = m[r][c].subtract(f.multiply(m[col][c]));
                    }
                }
            }
        }
        return det;
    }

    /**
     * Inverse of a square, invertible matrix via Gauss-Jordan on [A|I].
     *
     * @throws ArithmeticException if not square or singular
     */
    public static Fraction[][] inverse(Fraction[][] input) {
        int n = input.length;
        if (n == 0 || input[0].length != n) {
            throw new ArithmeticException("Inverse nur für quadratische Matrizen definiert.");
        }
        Fraction[][] m = new Fraction[n][2 * n];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                m[r][c] = input[r][c];
                m[r][n + c] = r == c ? Fraction.ONE : Fraction.ZERO;
            }
        }
        for (int col = 0; col < n; col++) {
            int sel = -1;
            for (int r = col; r < n; r++) {
                if (!m[r][col].isZero()) {
                    sel = r;
                    break;
                }
            }
            if (sel < 0) {
                throw new ArithmeticException("Matrix ist singulär – keine Inverse.");
            }
            if (sel != col) {
                Fraction[] tmp = m[sel];
                m[sel] = m[col];
                m[col] = tmp;
            }
            Fraction pivot = m[col][col];
            for (int c = 0; c < 2 * n; c++) {
                m[col][c] = m[col][c].divide(pivot);
            }
            for (int r = 0; r < n; r++) {
                if (r != col && !m[r][col].isZero()) {
                    Fraction f = m[r][col];
                    for (int c = 0; c < 2 * n; c++) {
                        m[r][c] = m[r][c].subtract(f.multiply(m[col][c]));
                    }
                }
            }
        }
        Fraction[][] inv = new Fraction[n][n];
        for (int r = 0; r < n; r++) {
            System.arraycopy(m[r], n, inv[r], 0, n);
        }
        return inv;
    }

    /** Solves {@code A x = b}; A is m×n, b length m. */
    public static Solution solve(Fraction[][] a, Fraction[] b) {
        return solveDetailed(a, b, false).solution();
    }

    /** Solution plus optional recorded steps (for step-by-step display). */
    public record Detailed(Solution solution, List<SolveStep> steps) {
    }

    public static Detailed solveDetailed(Fraction[][] a, Fraction[] b, boolean recordSteps) {
        int rows = a.length;
        int n = rows == 0 ? 0 : a[0].length;
        Fraction[][] aug = new Fraction[rows][n + 1];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(a[r], 0, aug[r], 0, n);
            aug[r][n] = b[r];
        }
        Rref rref = rref(aug, recordSteps);
        List<Integer> pivots = rref.pivotColumns();
        boolean inconsistent = pivots.contains(n);
        if (inconsistent) {
            return new Detailed(Solution.none(n), rref.steps());
        }
        Fraction[] particular = new Fraction[n];
        for (int i = 0; i < n; i++) {
            particular[i] = Fraction.ZERO;
        }
        boolean[] isPivotCol = new boolean[n];
        for (int k = 0; k < pivots.size(); k++) {
            int col = pivots.get(k);
            if (col < n) {
                particular[col] = rref.matrix()[k][n];
                isPivotCol[col] = true;
            }
        }
        int rankA = 0;
        for (boolean p : isPivotCol) {
            if (p) {
                rankA++;
            }
        }
        if (rankA == n) {
            return new Detailed(Solution.unique(particular), rref.steps());
        }
        List<Integer> freeVars = new ArrayList<>();
        for (int c = 0; c < n; c++) {
            if (!isPivotCol[c]) {
                freeVars.add(c);
            }
        }
        List<Fraction[]> basis = new ArrayList<>();
        for (int free : freeVars) {
            Fraction[] v = new Fraction[n];
            for (int i = 0; i < n; i++) {
                v[i] = Fraction.ZERO;
            }
            v[free] = Fraction.ONE;
            for (int k = 0; k < pivots.size(); k++) {
                int col = pivots.get(k);
                if (col < n) {
                    v[col] = rref.matrix()[k][free].negate();
                }
            }
            basis.add(v);
        }
        return new Detailed(Solution.infinite(particular, freeVars, basis), rref.steps());
    }
}
