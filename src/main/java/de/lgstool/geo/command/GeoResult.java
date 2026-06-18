package de.lgstool.geo.command;

import java.util.List;

/** Outcome of running one input line through the {@link GeoEngine}. */
public record GeoResult(boolean ok, List<String> messages) {

    public static GeoResult ok(String message) {
        return new GeoResult(true, List.of(message));
    }

    public static GeoResult ok(List<String> messages) {
        return new GeoResult(true, List.copyOf(messages));
    }

    public static GeoResult error(String message) {
        return new GeoResult(false, List.of(message));
    }
}
