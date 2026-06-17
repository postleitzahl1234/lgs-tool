package de.lgstool.model;

import de.lgstool.config.Settings;
import de.lgstool.math.Fraction;
import de.lgstool.math.FractionMatrix;
import java.util.ArrayList;
import java.util.List;

/**
 * A single linear equation system: a name, named variable columns and a list of
 * equation rows. Each row stores {@code width} coefficients followed by the
 * right-hand side, all as exact {@link Fraction} values.
 *
 * <p>The model is mutable; {@link #copy()} produces a deep clone used by the
 * undo system.
 */
public final class Lgs {

    private String name;
    private int width;
    private final List<String> varNames = new ArrayList<>();
    private final List<Fraction[]> rows = new ArrayList<>();
    private boolean fractionDisplay = true;

    private Lgs(String name, int width) {
        this.name = name;
        this.width = width;
    }

    /** Creates a system with {@code width} variables and {@code height} empty rows. */
    public static Lgs create(String name, int width, int height) {
        if (width < 1) {
            throw new IllegalArgumentException("Breite muss mindestens 1 sein.");
        }
        if (height < 0) {
            throw new IllegalArgumentException("Höhe darf nicht negativ sein.");
        }
        Lgs lgs = new Lgs(name, width);
        lgs.fractionDisplay = Settings.fractionDefault;
        for (String n : defaultNames(width)) {
            lgs.varNames.add(n);
        }
        for (int i = 0; i < height; i++) {
            lgs.rows.add(emptyRow(width));
        }
        return lgs;
    }

    private static Fraction[] emptyRow(int width) {
        Fraction[] row = new Fraction[width + 1];
        for (int i = 0; i <= width; i++) {
            row[i] = Fraction.ZERO;
        }
        return row;
    }

    private static List<String> defaultNames(int width) {
        List<String> names = new ArrayList<>();
        if (width <= 3) {
            String[] xyz = {"x", "y", "z"};
            for (int i = 0; i < width; i++) {
                names.add(xyz[i]);
            }
        } else {
            for (int i = 0; i < width; i++) {
                names.add("x" + (i + 1));
            }
        }
        return names;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int width() {
        return width;
    }

    public int height() {
        return rows.size();
    }

    public boolean isFractionDisplay() {
        return fractionDisplay;
    }

    public void setFractionDisplay(boolean fractionDisplay) {
        this.fractionDisplay = fractionDisplay;
    }

    public String[] variableNames() {
        return varNames.toArray(new String[0]);
    }

    public void setVariableName(int columnOneBased, String newName) {
        if (columnOneBased < 1 || columnOneBased > width) {
            throw new IllegalArgumentException("Spalte " + columnOneBased + " existiert nicht (1.." + width + ").");
        }
        varNames.set(columnOneBased - 1, newName);
    }

    public int variableIndex(String varName) {
        return varNames.indexOf(varName);
    }

    /** Full row (coefficients + rhs), copy. */
    public Fraction[] row(int oneBased) {
        checkRow(oneBased);
        return rows.get(oneBased - 1).clone();
    }

    public void setCell(int rowOneBased, int colZeroBased, Fraction value) {
        checkRow(rowOneBased);
        if (colZeroBased < 0 || colZeroBased > width) {
            throw new IllegalArgumentException("Spalte existiert nicht.");
        }
        rows.get(rowOneBased - 1)[colZeroBased] = value;
    }

    /**
     * Adds or overwrites an equation.
     *
     * @param coeffs   exactly {@code width} coefficients
     * @param rhs      right-hand side
     * @param rowOrNull 1-based row to overwrite, or {@code null} to fill the next
     *                  empty row (appending a new row when all are full)
     */
    public void addEquation(Fraction[] coeffs, Fraction rhs, Integer rowOrNull) {
        if (coeffs.length != width) {
            throw new IllegalArgumentException(
                    "Falsche Anzahl Koeffizienten: erwartet " + width + ", erhalten " + coeffs.length + ".");
        }
        Fraction[] full = new Fraction[width + 1];
        System.arraycopy(coeffs, 0, full, 0, width);
        full[width] = rhs;
        if (rowOrNull != null) {
            checkRow(rowOrNull);
            rows.set(rowOrNull - 1, full);
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (isEmptyRow(rows.get(i))) {
                rows.set(i, full);
                return;
            }
        }
        rows.add(full);
    }

    private static boolean isEmptyRow(Fraction[] row) {
        for (Fraction f : row) {
            if (!f.isZero()) {
                return false;
            }
        }
        return true;
    }

    public void deleteRow(int oneBased) {
        checkRow(oneBased);
        rows.remove(oneBased - 1);
    }

    public void swapRows(int i, int j) {
        checkRow(i);
        checkRow(j);
        Fraction[] tmp = rows.get(i - 1);
        rows.set(i - 1, rows.get(j - 1));
        rows.set(j - 1, tmp);
    }

    public void scaleRow(int oneBased, Fraction factor) {
        checkRow(oneBased);
        Fraction[] row = rows.get(oneBased - 1);
        for (int c = 0; c <= width; c++) {
            row[c] = row[c].multiply(factor);
        }
    }

    /** Row i := row i + factor · row j. */
    public void combineRows(int i, Fraction factor, int j) {
        checkRow(i);
        checkRow(j);
        Fraction[] ri = rows.get(i - 1);
        Fraction[] rj = rows.get(j - 1);
        for (int c = 0; c <= width; c++) {
            ri[c] = ri[c].add(factor.multiply(rj[c]));
        }
    }

    public void clear() {
        for (int i = 0; i < rows.size(); i++) {
            rows.set(i, emptyRow(width));
        }
    }

    /**
     * Changes dimensions. Truncated coefficients/rows are discarded.
     *
     * @return true if any non-zero value was discarded (caller may warn)
     */
    public boolean resize(int newWidth, int newHeight) {
        if (newWidth < 1 || newHeight < 0) {
            throw new IllegalArgumentException("Ungültige Dimensionen.");
        }
        boolean discardedNonZero = false;
        List<Fraction[]> newRows = new ArrayList<>();
        for (int r = 0; r < newHeight; r++) {
            Fraction[] newRow = emptyRow(newWidth);
            if (r < rows.size()) {
                Fraction[] old = rows.get(r);
                int copy = Math.min(width, newWidth);
                for (int c = 0; c < copy; c++) {
                    newRow[c] = old[c];
                }
                newRow[newWidth] = old[width];
                for (int c = copy; c < width; c++) {
                    if (!old[c].isZero()) {
                        discardedNonZero = true;
                    }
                }
            }
            newRows.add(newRow);
        }
        for (int r = newHeight; r < rows.size(); r++) {
            Fraction[] old = rows.get(r);
            for (Fraction f : old) {
                if (!f.isZero()) {
                    discardedNonZero = true;
                }
            }
        }
        List<String> oldNames = new ArrayList<>(varNames);
        varNames.clear();
        List<String> defaults = defaultNames(newWidth);
        for (int c = 0; c < newWidth; c++) {
            varNames.add(c < oldNames.size() ? oldNames.get(c) : defaults.get(c));
        }
        rows.clear();
        rows.addAll(newRows);
        this.width = newWidth;
        return discardedNonZero;
    }

    public Fraction[][] coefficientMatrix() {
        Fraction[][] a = new Fraction[rows.size()][width];
        for (int r = 0; r < rows.size(); r++) {
            System.arraycopy(rows.get(r), 0, a[r], 0, width);
        }
        return a;
    }

    public Fraction[] rightHandSide() {
        Fraction[] b = new Fraction[rows.size()];
        for (int r = 0; r < rows.size(); r++) {
            b[r] = rows.get(r)[width];
        }
        return b;
    }

    public FractionMatrix augmentedMatrix() {
        Fraction[][] data = new Fraction[rows.size()][width + 1];
        for (int r = 0; r < rows.size(); r++) {
            System.arraycopy(rows.get(r), 0, data[r], 0, width + 1);
        }
        return new FractionMatrix(data);
    }

    public Lgs copy() {
        Lgs c = new Lgs(name, width);
        c.varNames.addAll(varNames);
        c.fractionDisplay = fractionDisplay;
        for (Fraction[] row : rows) {
            c.rows.add(row.clone());
        }
        return c;
    }

    private void checkRow(int oneBased) {
        if (oneBased < 1 || oneBased > rows.size()) {
            throw new IllegalArgumentException("Zeile " + oneBased + " existiert nicht (1.." + rows.size() + ").");
        }
    }
}
