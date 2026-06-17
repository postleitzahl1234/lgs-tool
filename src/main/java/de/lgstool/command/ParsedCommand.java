package de.lgstool.command;

import java.util.List;

/**
 * A parsed command line: the command name, its comma-separated arguments and the
 * raw argument string (for commands whose arguments use spaces or {@code =}).
 */
public record ParsedCommand(String name, List<String> args, String raw) {

    public int argCount() {
        return args.size();
    }

    public String arg(int index) {
        return args.get(index);
    }
}
