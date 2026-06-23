package de.lgstool.geo.model;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates a stable, pleasant random colour (as a {@code #RRGGBB} hex string) for each
 * new {@link GeoObject}. Saturation and lightness are fixed so the colour reads on both
 * the light and dark themes; only the hue is random. Stored on the object so it stays
 * constant across redraws and undo.
 */
public final class Palette {

    private static final double SATURATION = 0.62;
    private static final double LIGHTNESS = 0.55;

    private Palette() {
    }

    public static String randomHex() {
        return hsl(ThreadLocalRandom.current().nextDouble(), SATURATION, LIGHTNESS);
    }

    /** A lighter/darker shade of a hex colour by a lightness delta in [-1, 1] (for matrix rows). */
    public static String shade(String hex, double deltaL) {
        double[] hslv = toHsl(hex);
        double l = Math.max(0.18, Math.min(0.9, hslv[2] + deltaL));
        return hsl(hslv[0], hslv[1], l);
    }

    private static String hsl(double h, double s, double l) {
        double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
        double p = 2 * l - q;
        int r = (int) Math.round(hue(p, q, h + 1.0 / 3) * 255);
        int g = (int) Math.round(hue(p, q, h) * 255);
        int b = (int) Math.round(hue(p, q, h - 1.0 / 3) * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static double hue(double p, double q, double t) {
        if (t < 0) {
            t += 1;
        }
        if (t > 1) {
            t -= 1;
        }
        if (t < 1.0 / 6) {
            return p + (q - p) * 6 * t;
        }
        if (t < 1.0 / 2) {
            return q;
        }
        if (t < 2.0 / 3) {
            return p + (q - p) * (2.0 / 3 - t) * 6;
        }
        return p;
    }

    private static double[] toHsl(String hex) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        double rd = r / 255.0;
        double gd = g / 255.0;
        double bd = b / 255.0;
        double max = Math.max(rd, Math.max(gd, bd));
        double min = Math.min(rd, Math.min(gd, bd));
        double l = (max + min) / 2;
        double h = 0;
        double s = 0;
        if (max != min) {
            double d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            if (max == rd) {
                h = (gd - bd) / d + (gd < bd ? 6 : 0);
            } else if (max == gd) {
                h = (bd - rd) / d + 2;
            } else {
                h = (rd - gd) / d + 4;
            }
            h /= 6;
        }
        return new double[]{h, s, l};
    }
}
