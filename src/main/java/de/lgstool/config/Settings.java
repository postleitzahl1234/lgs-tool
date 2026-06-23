package de.lgstool.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Global application settings. Mutable static fields hold the live values; {@link #load()}
 * reads them from {@code ~/.lgstool/settings.properties} at startup and {@link #save()}
 * writes them back, so preferences (language, theme, display defaults, …) persist
 * between runs. All persistence is best-effort: I/O failures never crash the app.
 */
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

    /** Whether dragging a free point snaps it to the nearest grid line. */
    public static boolean snapToGrid = false;

    private static final Path FILE = Path.of(System.getProperty("user.home"), ".lgstool", "settings.properties");

    /** Loads settings from disk and applies the persisted language and theme. */
    public static void load() {
        if (!Files.exists(FILE)) {
            return;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
        } catch (IOException e) {
            return;                                    // keep defaults on any read error
        }
        fractionDefault = bool(p, "fractionDefault", fractionDefault);
        startFullscreen = bool(p, "startFullscreen", startFullscreen);
        suggestionsEnabled = bool(p, "suggestionsEnabled", suggestionsEnabled);
        snapToGrid = bool(p, "snapToGrid", snapToGrid);
        decimalPrecision = clampInt(p.getProperty("decimalPrecision"), decimalPrecision, 1, 15);
        Theme.setDark(bool(p, "darkMode", Theme.isDark()));
        String lang = p.getProperty("language");
        if (lang != null) {
            I18n.setLanguage(lang.trim());
        }
    }

    /** Persists the current settings (incl. live language and theme) to disk. */
    public static void save() {
        Properties p = new Properties();
        p.setProperty("fractionDefault", Boolean.toString(fractionDefault));
        p.setProperty("startFullscreen", Boolean.toString(startFullscreen));
        p.setProperty("suggestionsEnabled", Boolean.toString(suggestionsEnabled));
        p.setProperty("snapToGrid", Boolean.toString(snapToGrid));
        p.setProperty("decimalPrecision", Integer.toString(decimalPrecision));
        p.setProperty("darkMode", Boolean.toString(Theme.isDark()));
        p.setProperty("language", I18n.currentCode());
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "LGS-Tool settings");
            }
        } catch (IOException e) {
            // best-effort; preferences simply will not persist this time
        }
    }

    private static boolean bool(Properties p, String key, boolean fallback) {
        String v = p.getProperty(key);
        return v == null ? fallback : Boolean.parseBoolean(v.trim());
    }

    private static int clampInt(String value, int fallback, int lo, int hi) {
        if (value == null) {
            return fallback;
        }
        try {
            return Math.max(lo, Math.min(hi, Integer.parseInt(value.trim())));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
