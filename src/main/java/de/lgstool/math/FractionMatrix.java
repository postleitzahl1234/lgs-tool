package de.lgstool.math;

import de.lgstool.config.Settings;

/**
 * Immutable dense matrix of {@link Fraction} values. Used for snapshots in
 * step-by-step solving and for rendering matrices as aligned text.
 */
public final class FractionMatrix {

    private final Fraction[][] data;
    private final int rows;
    private final int cols;

    public FractionMatrix(Fraction[][] data) {
        this.rows = data.length;
        this.cols = rows == 0 ? 0 : data[0].length;
        this.data = new Fraction[rows][cols];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(data[r], 0, this.data[r], 0, cols);
        }
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public Fraction get(int r, int c) {
        return data[r][c];
    }

    /**
     * Renders the matrix as a column-aligned block. When {@code augmentedAt} is
     * a valid column index a separator {@code |} is drawn before it.
     *
     * @param asFraction true for fraction display, false for decimal
     * @param augmentedAt column index of the right-hand side, or -1 for none
     */
    public String toText(boolean asFraction, int augmentedAt) {
        String[][] cells = new String[rows][cols];
        int[] widths = new int[cols];
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                String s = asFraction ? data[r][c].toString() : data[r][c].toDecimalString(Settings.decimalPrecision);
                cells[r][c] = s;
                widths[c] = Math.max(widths[c], s.length());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            sb.append(Roman.of(r + 1)).append(":  ");
            for (int c = 0; c < cols; c++) {
                if (c == augmentedAt) {
                    sb.append("| ");
                }
                sb.append(pad(cells[r][c], widths[c]));
                if (c < cols - 1) {
                    sb.append("  ");
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String pad(String s, int width) {
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i < width; i++) {
            sb.append(' ');
        }
        sb.append(s);
        return sb.toString();
    }
}
