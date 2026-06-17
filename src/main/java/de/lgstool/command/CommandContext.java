package de.lgstool.command;

/**
 * Where a command was entered. For {@link Screen#LGS} the {@code lgsName} is the
 * system shown in that detail window.
 */
public record CommandContext(Screen screen, String lgsName) {

    public static CommandContext main() {
        return new CommandContext(Screen.HAUPT, null);
    }

    public static CommandContext lgs(String name) {
        return new CommandContext(Screen.LGS, name);
    }
}
