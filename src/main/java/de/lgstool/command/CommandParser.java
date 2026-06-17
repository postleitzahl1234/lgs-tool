package de.lgstool.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses command lines of the form {@code name(arg1, arg2, ...);}. The trailing
 * semicolon is optional. Arguments are split on top-level commas; each command
 * interprets its own arguments (some use spaces or {@code =} internally).
 */
public final class CommandParser {

    private CommandParser() {
    }

    public static ParsedCommand parse(String line) throws CommandException {
        if (line == null) {
            throw new CommandException("Leerer Befehl.");
        }
        String s = line.trim();
        if (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        if (s.isEmpty()) {
            throw new CommandException("Leerer Befehl.");
        }
        int open = s.indexOf('(');
        int close = s.lastIndexOf(')');
        if (open < 0 || close < 0 || close < open) {
            throw new CommandException("Erwarte Form  name(argumente);  – Klammern fehlen.");
        }
        String name = s.substring(0, open).trim();
        if (name.isEmpty() || !name.chars().allMatch(Character::isLetter)) {
            throw new CommandException("Ungültiger Befehlsname: \"" + name + "\".");
        }
        String inside = s.substring(open + 1, close).trim();
        List<String> args = new ArrayList<>();
        if (!inside.isEmpty()) {
            for (String part : inside.split(",")) {
                args.add(part.trim());
            }
        }
        return new ParsedCommand(name.toLowerCase(), args, inside);
    }
}
