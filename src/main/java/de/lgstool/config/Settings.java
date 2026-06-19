package de.lgstool.config;

/** Global, in-memory application settings adjustable from the settings dialog. */
public final class Settings {

    private Settings() {
    }

    /** Default display mode for newly created systems (true = fractions). */
    public static boolean fractionDefault = true;

    /**
     * Whether the application starts in exclusive fullscreen (default true): the app
     * covers the whole screen and the Windows taskbar is hidden. Toggle with F11;
     * leaving fullscreen falls back to a maximized window (taskbar visible).
     */
    public static boolean startFullscreen = true;

    /** Number of significant digits used when displaying decimals. */
    public static int decimalPrecision = 6;

    /** Whether the command line shows live command suggestions while typing. */
    public static boolean suggestionsEnabled = true;
}
