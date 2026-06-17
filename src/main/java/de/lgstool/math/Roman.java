package de.lgstool.math;

/** Roman numeral formatting for 1-based row labels (I, II, III, ...). */
public final class Roman {

    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private Roman() {
    }

    /** @param oneBased a value &gt;= 1 */
    public static String of(int oneBased) {
        if (oneBased <= 0) {
            return String.valueOf(oneBased);
        }
        StringBuilder sb = new StringBuilder();
        int n = oneBased;
        for (int i = 0; i < VALUES.length; i++) {
            while (n >= VALUES[i]) {
                n -= VALUES[i];
                sb.append(SYMBOLS[i]);
            }
        }
        return sb.toString();
    }
}
