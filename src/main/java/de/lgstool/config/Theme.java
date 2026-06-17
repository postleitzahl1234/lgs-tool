package de.lgstool.config;

/**
 * Central color palette. Values are CSS-ready hex strings for use in JavaFX inline
 * styles and stylesheets. Supports a light (default) and a dark theme; switching
 * reassigns the fields, so newly constructed UI and the next canvas redraw pick up
 * the change.
 */
public final class Theme {

    private Theme() {
    }

    /** Window / surface background. */
    public static String BACKGROUND = "#E8E6DF";

    /** Primary accent (petrol). */
    public static String ACCENT_PETROL = "#1F5E63";

    /** Secondary accent (sage green). */
    public static String ACCENT_SAGE = "#8FAF9A";

    /** Default text color. */
    public static String TEXT = "#1A1A1A";

    private static boolean dark;

    public static boolean isDark() {
        return dark;
    }

    public static void setDark(boolean enabled) {
        dark = enabled;
        if (enabled) {
            BACKGROUND = "#1E2A2B";
            ACCENT_PETROL = "#6FB3B8";
            ACCENT_SAGE = "#A7C6B3";
            TEXT = "#ECEAE3";
        } else {
            BACKGROUND = "#E8E6DF";
            ACCENT_PETROL = "#1F5E63";
            ACCENT_SAGE = "#8FAF9A";
            TEXT = "#1A1A1A";
        }
    }
}
