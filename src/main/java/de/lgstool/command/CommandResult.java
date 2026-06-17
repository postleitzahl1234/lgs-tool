package de.lgstool.command;

import de.lgstool.config.I18n;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Outcome of executing a command: a success flag plus log lines for display. */
public final class CommandResult {

    private final boolean ok;
    private final List<String> lines;

    private CommandResult(boolean ok, List<String> lines) {
        this.ok = ok;
        this.lines = lines;
    }

    public static CommandResult ok(String... lines) {
        return new CommandResult(true, new ArrayList<>(Arrays.asList(lines)));
    }

    public static CommandResult ok(List<String> lines) {
        return new CommandResult(true, new ArrayList<>(lines));
    }

    public static CommandResult error(String message) {
        List<String> l = new ArrayList<>();
        l.add(I18n.t("m.errorPrefix") + message);
        return new CommandResult(false, l);
    }

    public boolean isOk() {
        return ok;
    }

    public List<String> messages() {
        return lines;
    }
}
