package de.lgstool.viz;

import java.util.Locale;

/**
 * Pure 2D world↔screen mapping and axis-tick helpers shared by the LGS
 * {@link Visualization} and the geometry {@code CoordinatePlane2D}. A view supplies
 * its world bounds {@code [vx0,vx1] × [vy0,vy1]} and pixel size; these functions do
 * the rest. Extracted so both views agree on the maths and neither duplicates it.
 */
public final class Plane2DMath {

    private Plane2DMath() {
    }

    /** World x → screen x. */
    public static double sx(double x, double vx0, double vx1, double w) {
        return (x - vx0) / (vx1 - vx0) * w;
    }

    /** World y → screen y (y axis points up). */
    public static double sy(double y, double vy0, double vy1, double h) {
        return h - (y - vy0) / (vy1 - vy0) * h;
    }

    /** Screen x → world x (inverse of {@link #sx}). */
    public static double wx(double px, double vx0, double vx1, double w) {
        return vx0 + px / w * (vx1 - vx0);
    }

    /** Screen y → world y (inverse of {@link #sy}). */
    public static double wy(double py, double vy0, double vy1, double h) {
        return vy0 + (h - py) / h * (vy1 - vy0);
    }

    /** A "nice" axis step (1, 2, 5 × 10ⁿ) close to {@code raw}. */
    public static double niceStep(double raw) {
        if (raw <= 0) {
            return 1;
        }
        double pow = Math.pow(10, Math.floor(Math.log10(raw)));
        double frac = raw / pow;
        double nice = frac < 1.5 ? 1 : frac < 3 ? 2 : frac < 7 ? 5 : 10;
        return nice * pow;
    }

    /** Formats an axis-tick value: integers exactly, otherwise 2 significant figures. */
    public static String trim(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format(Locale.US, "%.2g", v);
    }
}
