package de.lgstool.model;

import de.lgstool.math.Fraction;

/** A small library of sample systems demonstrating the solution classifications. */
public final class Examples {

    private Examples() {
    }

    public static String[] names() {
        return new String[]{"unique2", "unique3", "infinite", "nosolution", "illcond"};
    }

    /** Builds the named example, or {@code null} if unknown. */
    public static Lgs build(String name) {
        return switch (name) {
            case "unique2" -> fill(Lgs.create("unique2", 2, 2), new long[][]{
                    {1, 1, 3},
                    {1, -1, 1}});
            case "unique3" -> fill(Lgs.create("unique3", 3, 3), new long[][]{
                    {1, 1, 1, 6},
                    {0, 2, 5, -4},
                    {2, 5, -1, 27}});
            case "infinite" -> fill(Lgs.create("infinite", 3, 1), new long[][]{
                    {1, 1, 1, 1}});
            case "nosolution" -> fill(Lgs.create("nosolution", 2, 2), new long[][]{
                    {1, 1, 1},
                    {1, 1, 2}});
            case "illcond" -> fillFractions(Lgs.create("illcond", 2, 2), new Fraction[][]{
                    {Fraction.of(1), Fraction.of(1, 2), Fraction.of(1)},
                    {Fraction.of(1, 2), Fraction.of(1, 3), Fraction.of(1)}});
            default -> null;
        };
    }

    private static Lgs fill(Lgs lgs, long[][] values) {
        for (int r = 0; r < values.length; r++) {
            for (int c = 0; c < values[r].length; c++) {
                lgs.setCell(r + 1, c, Fraction.of(values[r][c]));
            }
        }
        return lgs;
    }

    private static Lgs fillFractions(Lgs lgs, Fraction[][] values) {
        for (int r = 0; r < values.length; r++) {
            for (int c = 0; c < values[r].length; c++) {
                lgs.setCell(r + 1, c, values[r][c]);
            }
        }
        return lgs;
    }
}
