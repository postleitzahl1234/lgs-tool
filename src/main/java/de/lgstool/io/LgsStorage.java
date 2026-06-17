package de.lgstool.io;

import de.lgstool.math.Fraction;
import de.lgstool.model.Lgs;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes systems in the simple {@code .lgs} text format under a
 * {@code lgs/} directory next to the application (portable). Falls back to the
 * user home directory when the working directory is not writable.
 */
public final class LgsStorage {

    private static final String HEADER = "# LGS-Tool file v1";

    private LgsStorage() {
    }

    /** Directory where systems are stored, created on demand. */
    public static Path directory() {
        Path local = Path.of("lgs");
        try {
            Files.createDirectories(local);
            return local;
        } catch (IOException e) {
            Path home = Path.of(System.getProperty("user.home"), ".lgstool", "lgs");
            try {
                Files.createDirectories(home);
                return home;
            } catch (IOException ex) {
                throw new UncheckedIOException("Speicherordner konnte nicht angelegt werden.", ex);
            }
        }
    }

    public static Path fileFor(String fileName) {
        return directory().resolve(fileName + ".lgs");
    }

    public static Path save(Lgs lgs, String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append('\n');
        sb.append("name ").append(lgs.name()).append('\n');
        sb.append("width ").append(lgs.width()).append('\n');
        sb.append("vars ").append(String.join(",", lgs.variableNames())).append('\n');
        sb.append("display ").append(lgs.isFractionDisplay() ? "frac" : "dec").append('\n');
        for (int r = 1; r <= lgs.height(); r++) {
            Fraction[] row = lgs.row(r);
            sb.append("row");
            for (Fraction f : row) {
                sb.append(' ').append(f.toString());
            }
            sb.append('\n');
        }
        Path file = fileFor(fileName);
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        return file;
    }

    public static Lgs load(String fileName) throws IOException {
        Path file = fileFor(fileName);
        if (!Files.exists(file)) {
            throw new IOException("Datei nicht gefunden: " + file);
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String name = fileName;
        int width = 0;
        String[] vars = null;
        boolean fraction = true;
        List<Fraction[]> rows = new ArrayList<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int sp = line.indexOf(' ');
            String key = sp < 0 ? line : line.substring(0, sp);
            String value = sp < 0 ? "" : line.substring(sp + 1).trim();
            switch (key) {
                case "name" -> name = value;
                case "width" -> width = Integer.parseInt(value);
                case "vars" -> vars = value.isEmpty() ? new String[0] : value.split(",");
                case "display" -> fraction = !value.equalsIgnoreCase("dec");
                case "row" -> rows.add(parseRow(value));
                default -> {
                    // ignore unknown keys for forward compatibility
                }
            }
        }
        if (width < 1) {
            throw new IOException("Ungültige Datei: Breite fehlt.");
        }
        Lgs lgs = Lgs.create(name, width, rows.size());
        if (vars != null) {
            for (int c = 0; c < Math.min(vars.length, width); c++) {
                lgs.setVariableName(c + 1, vars[c]);
            }
        }
        for (int r = 0; r < rows.size(); r++) {
            Fraction[] row = rows.get(r);
            if (row.length != width + 1) {
                throw new IOException("Ungültige Zeile in Datei (erwarte " + (width + 1) + " Werte).");
            }
            for (int c = 0; c <= width; c++) {
                lgs.setCell(r + 1, c, row[c]);
            }
        }
        lgs.setFractionDisplay(fraction);
        return lgs;
    }

    private static Fraction[] parseRow(String value) {
        String[] tokens = value.split("\\s+");
        Fraction[] row = new Fraction[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            row[i] = Fraction.parse(tokens[i]);
        }
        return row;
    }
}
