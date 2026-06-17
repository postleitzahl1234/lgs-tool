package de.lgstool.config;

/** Global, in-memory application settings adjustable from the settings dialog. */
public final class Settings {

    private Settings() {
    }

    /** Default display mode for newly created systems (true = fractions). */
    public static boolean fractionDefault = true;

    /** Whether the application starts in fullscreen. */
    public static boolean startFullscreen = true;

    /** Number of significant digits used when displaying decimals. */
    public static int decimalPrecision = 6;
}
