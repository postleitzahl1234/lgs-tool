package de.lgstool.math;

/**
 * Numeric condition number {@code cond(A) = ||A|| · ||A⁻¹||} as a measure of
 * how sensitive the solution is to perturbations of the right-hand side.
 *
 * <p>Supports the infinity norm (default), the 1-norm and the spectral 2-norm.
 * The inverse is computed exactly with {@link Gauss}; norms are evaluated in
 * double precision.
 */
public final class ConditionNumber {

    public static final int NORM_INFINITY = 0;
    public static final int NORM_ONE = 1;
    public static final int NORM_TWO = 2;

    private ConditionNumber() {
    }

    /**
     * @param a    square coefficient matrix
     * @param norm one of {@link #NORM_INFINITY}, {@link #NORM_ONE}, {@link #NORM_TWO}
     * @throws ArithmeticException if not square or singular
     */
    public static double of(Fraction[][] a, int norm) {
        int n = a.length;
        if (n == 0 || a[0].length != n) {
            throw new ArithmeticException("Konditionszahl nur für quadratische Matrizen definiert.");
        }
        Fraction[][] inverse = Gauss.inverse(a); // throws if singular
        double[][] ad = toDouble(a);
        double[][] aid = toDouble(inverse);
        return switch (norm) {
            case NORM_ONE -> oneNorm(ad) * oneNorm(aid);
            case NORM_TWO -> spectralNorm(ad) * spectralNorm(aid);
            default -> infinityNorm(ad) * infinityNorm(aid);
        };
    }

    private static double[][] toDouble(Fraction[][] a) {
        double[][] d = new double[a.length][a[0].length];
        for (int r = 0; r < a.length; r++) {
            for (int c = 0; c < a[0].length; c++) {
                d[r][c] = a[r][c].doubleValue();
            }
        }
        return d;
    }

    /** Maximum absolute row sum. */
    public static double infinityNorm(double[][] a) {
        double max = 0;
        for (double[] row : a) {
            double sum = 0;
            for (double v : row) {
                sum += Math.abs(v);
            }
            max = Math.max(max, sum);
        }
        return max;
    }

    /** Maximum absolute column sum. */
    public static double oneNorm(double[][] a) {
        int cols = a[0].length;
        double max = 0;
        for (int c = 0; c < cols; c++) {
            double sum = 0;
            for (double[] row : a) {
                sum += Math.abs(row[c]);
            }
            max = Math.max(max, sum);
        }
        return max;
    }

    /** Spectral norm = sqrt(largest eigenvalue of AᵀA) via power iteration. */
    public static double spectralNorm(double[][] a) {
        int n = a[0].length;
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            v[i] = 1.0;
        }
        normalize(v);
        double eigen = 0;
        for (int iter = 0; iter < 300; iter++) {
            double[] av = multiply(a, v);
            double[] w = multiplyTranspose(a, av); // AᵀA v
            double norm = norm2(w);
            if (norm < 1e-300) {
                return 0;
            }
            for (int i = 0; i < n; i++) {
                w[i] /= norm;
            }
            double newEigen = dot(w, multiplyTranspose(a, multiply(a, w)));
            if (Math.abs(newEigen - eigen) < 1e-12 * Math.max(1, Math.abs(newEigen))) {
                eigen = newEigen;
                break;
            }
            eigen = newEigen;
            v = w;
        }
        return Math.sqrt(Math.max(0, eigen));
    }

    private static double[] multiply(double[][] a, double[] x) {
        double[] y = new double[a.length];
        for (int r = 0; r < a.length; r++) {
            double sum = 0;
            for (int c = 0; c < x.length; c++) {
                sum += a[r][c] * x[c];
            }
            y[r] = sum;
        }
        return y;
    }

    private static double[] multiplyTranspose(double[][] a, double[] x) {
        int cols = a[0].length;
        double[] y = new double[cols];
        for (int c = 0; c < cols; c++) {
            double sum = 0;
            for (int r = 0; r < a.length; r++) {
                sum += a[r][c] * x[r];
            }
            y[c] = sum;
        }
        return y;
    }

    private static double dot(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private static double norm2(double[] v) {
        return Math.sqrt(dot(v, v));
    }

    private static void normalize(double[] v) {
        double norm = norm2(v);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) {
                v[i] /= norm;
            }
        }
    }
}
