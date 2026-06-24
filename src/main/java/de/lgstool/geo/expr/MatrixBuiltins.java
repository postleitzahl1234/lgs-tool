package de.lgstool.geo.expr;

import de.lgstool.math.Fraction;
import de.lgstool.math.Gauss;
import de.lgstool.math.Solution;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Exact matrix operations as expression built-ins, all wrapping the shared
 * {@link Gauss} engine so results stay {@link Fraction}-exact: {@code RREF}, {@code REF},
 * {@code Inverse}, {@code Transpose}, {@code Identity}, {@code Solve} (of an augmented
 * matrix) and {@code Nullspace} return matrices/lists; {@code Det}, {@code Rank},
 * {@code Trace} and {@code Dimension} return numbers. Matrix {@code + - *} and
 * scalar·matrix are handled in {@link Evaluator}, reusing the helpers here.
 */
public final class MatrixBuiltins implements BuiltinFunctions {

    private static final Set<String> NAMES = Set.of(
            "rref", "ref", "inverse", "transpose", "identity", "det", "rank",
            "trace", "dimension", "solve", "nullspace");

    @Override
    public boolean has(String name) {
        return NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public Value call(String name, List<Value> args, FunctionApplier applier) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "rref" -> new Value.MatrixV(Gauss.rref(matrix(args, "RREF"), false).matrix());
            case "ref" -> new Value.MatrixV(Gauss.ref(matrix(args, "REF")));
            case "inverse" -> new Value.MatrixV(Gauss.inverse(matrix(args, "Inverse")));
            case "transpose" -> new Value.MatrixV(transpose(matrix(args, "Transpose")));
            case "identity" -> identity(args);
            case "det" -> new Value.NumberV(Gauss.determinant(matrix(args, "Det")).doubleValue());
            case "rank" -> new Value.NumberV(Gauss.rank(matrix(args, "Rank")));
            case "trace" -> new Value.NumberV(trace(matrix(args, "Trace")).doubleValue());
            case "dimension" -> dimension(args);
            case "solve" -> solve(args);
            case "nullspace" -> nullspace(args);
            default -> throw new EvalException("Unbekannte Funktion '" + name + "'.");
        };
    }

    // ---- builtins --------------------------------------------------------

    private static Value identity(List<Value> args) {
        if (args.size() != 1 || !(args.get(0) instanceof Value.NumberV n)) {
            throw new EvalException("Identity erwartet eine Größe n.");
        }
        int dim = (int) Math.round(n.v());
        if (dim < 1) {
            throw new EvalException("Identity: n muss mindestens 1 sein.");
        }
        Fraction[][] m = new Fraction[dim][dim];
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                m[i][j] = i == j ? Fraction.ONE : Fraction.ZERO;
            }
        }
        return new Value.MatrixV(m);
    }

    private static Value dimension(List<Value> args) {
        Value.MatrixV m = matrixValue(args, "Dimension");
        return new Value.ListV(List.of(new Value.NumberV(m.rowCount()), new Value.NumberV(m.colCount())));
    }

    /** Treats the argument as an augmented matrix [A|b] and returns the exact solution column. */
    private static Value solve(List<Value> args) {
        Fraction[][] aug = matrix(args, "Solve");
        int n = aug[0].length - 1;
        if (n < 1) {
            throw new EvalException("Solve erwartet eine erweiterte Matrix [A|b].");
        }
        Fraction[][] a = new Fraction[aug.length][n];
        Fraction[] b = new Fraction[aug.length];
        for (int r = 0; r < aug.length; r++) {
            System.arraycopy(aug[r], 0, a[r], 0, n);
            b[r] = aug[r][n];
        }
        Solution s = Gauss.solve(a, b);
        return switch (s.kind()) {
            case UNIQUE -> column(s.particular());
            case NONE -> throw new EvalException("Keine Lösung (inkonsistent).");
            case INFINITE -> throw new EvalException(
                    "Unendlich viele Lösungen (" + s.freeParameterCount() + " freie Parameter) — nutze RREF/Nullspace.");
        };
    }

    private static Value nullspace(List<Value> args) {
        List<Fraction[]> basis = Gauss.nullspace(matrix(args, "Nullspace"));
        if (basis.isEmpty()) {
            throw new EvalException("Nur die triviale Lösung (Nullvektor).");
        }
        return new Value.ListV(basis.stream().<Value>map(MatrixBuiltins::column).toList());
    }

    /**
     * The unique solution of the augmented system {@code [A|b]} as doubles, or {@code null}
     * if the system has no solution or infinitely many. Used by the views to show the live
     * intersection point of a linear system (e.g. while an animated parameter changes).
     */
    public static double[] uniqueSolution(Fraction[][] aug) {
        if (aug.length == 0 || aug[0].length < 2) {
            return null;
        }
        int n = aug[0].length - 1;
        Fraction[][] a = new Fraction[aug.length][n];
        Fraction[] b = new Fraction[aug.length];
        for (int r = 0; r < aug.length; r++) {
            System.arraycopy(aug[r], 0, a[r], 0, n);
            b[r] = aug[r][n];
        }
        Solution s = Gauss.solve(a, b);
        if (s.kind() != Solution.Kind.UNIQUE) {
            return null;
        }
        Fraction[] p = s.particular();
        double[] sol = new double[n];
        for (int i = 0; i < n; i++) {
            sol[i] = p[i].doubleValue();
        }
        return sol;
    }

    // ---- shared matrix maths (also used by Evaluator) --------------------

    static Fraction[][] add(Fraction[][] a, Fraction[][] b) {
        requireSameShape(a, b);
        Fraction[][] r = new Fraction[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                r[i][j] = a[i][j].add(b[i][j]);
            }
        }
        return r;
    }

    static Fraction[][] subtract(Fraction[][] a, Fraction[][] b) {
        requireSameShape(a, b);
        Fraction[][] r = new Fraction[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                r[i][j] = a[i][j].subtract(b[i][j]);
            }
        }
        return r;
    }

    static Fraction[][] multiply(Fraction[][] a, Fraction[][] b) {
        if (a.length == 0 || b.length == 0 || a[0].length != b.length) {
            throw new EvalException("Matrixprodukt: Spaltenzahl von A muss Zeilenzahl von B entsprechen.");
        }
        int n = a.length;
        int m = b[0].length;
        int k = b.length;
        Fraction[][] r = new Fraction[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                Fraction sum = Fraction.ZERO;
                for (int t = 0; t < k; t++) {
                    sum = sum.add(a[i][t].multiply(b[t][j]));
                }
                r[i][j] = sum;
            }
        }
        return r;
    }

    static Fraction[][] scale(Fraction[][] a, Fraction k) {
        Fraction[][] r = new Fraction[a.length][a.length == 0 ? 0 : a[0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                r[i][j] = a[i][j].multiply(k);
            }
        }
        return r;
    }

    // ---- helpers ---------------------------------------------------------

    private static Fraction[][] transpose(Fraction[][] a) {
        int rows = a.length;
        int cols = rows == 0 ? 0 : a[0].length;
        Fraction[][] t = new Fraction[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                t[j][i] = a[i][j];
            }
        }
        return t;
    }

    private static Fraction trace(Fraction[][] a) {
        if (a.length == 0 || a.length != a[0].length) {
            throw new EvalException("Spur nur für quadratische Matrizen.");
        }
        Fraction sum = Fraction.ZERO;
        for (int i = 0; i < a.length; i++) {
            sum = sum.add(a[i][i]);
        }
        return sum;
    }

    private static Value.MatrixV column(Fraction[] v) {
        Fraction[][] m = new Fraction[v.length][1];
        for (int i = 0; i < v.length; i++) {
            m[i][0] = v[i];
        }
        return new Value.MatrixV(m);
    }

    private static Value.MatrixV matrixValue(List<Value> args, String where) {
        if (args.size() != 1 || !(args.get(0) instanceof Value.MatrixV m)) {
            throw new EvalException(where + " erwartet eine Matrix.");
        }
        return m;
    }

    private static Fraction[][] matrix(List<Value> args, String where) {
        return matrixValue(args, where).rows();
    }

    private static void requireSameShape(Fraction[][] a, Fraction[][] b) {
        if (a.length != b.length || (a.length > 0 && a[0].length != b[0].length)) {
            throw new EvalException("Matrizen müssen die gleiche Größe haben.");
        }
    }
}
