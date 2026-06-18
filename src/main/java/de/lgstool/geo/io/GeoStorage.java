package de.lgstool.geo.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists a construction as a {@code .geo} text file: a header followed by one
 * replayable input line per object. Loading just returns those lines, which the
 * {@link de.lgstool.geo.command.GeoEngine} re-executes to rebuild the construction.
 * Files live in a portable {@code geo/} directory, falling back to the user home.
 */
public final class GeoStorage {

    private static final String HEADER = "# LGS-Tool geo file v1";

    private GeoStorage() {
    }

    public static Path directory() {
        Path local = Path.of("geo");
        try {
            Files.createDirectories(local);
            return local;
        } catch (IOException e) {
            Path home = Path.of(System.getProperty("user.home"), ".lgstool", "geo");
            try {
                Files.createDirectories(home);
                return home;
            } catch (IOException ex) {
                throw new UncheckedIOException("Speicherordner konnte nicht angelegt werden.", ex);
            }
        }
    }

    public static Path fileFor(String name) {
        return directory().resolve(name + ".geo");
    }

    public static Path save(List<String> lines, String name) throws IOException {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (String line : lines) {
            sb.append(line).append('\n');
        }
        Path file = fileFor(name);
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        return file;
    }

    public static List<String> load(String name) throws IOException {
        Path file = fileFor(name);
        if (!Files.exists(file)) {
            throw new IOException("Datei nicht gefunden: " + file);
        }
        List<String> out = new ArrayList<>();
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                out.add(line);
            }
        }
        return out;
    }
}
