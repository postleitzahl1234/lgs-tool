package de.lgstool.ui;

/**
 * One command-line suggestion: {@code name} is matched against what the user is
 * typing, {@code signature} is shown in the popup (e.g. {@code cond([norm])}), and
 * {@code insert} is what replaces the typed prefix on autocomplete (e.g. {@code cond(}).
 */
public record Suggestion(String name, String signature, String insert) {

    /** A command taking arguments: completing inserts {@code name(} so the user adds them. */
    public static Suggestion of(String name, String signature) {
        return new Suggestion(name, signature, name + "(");
    }

    /** A bare keyword with no parentheses (e.g. {@code undo}). */
    public static Suggestion word(String name) {
        return new Suggestion(name, name, name);
    }
}
