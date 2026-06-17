package de.lgstool.command;

/** Thrown when a command line cannot be parsed or its arguments are invalid. */
public class CommandException extends Exception {

    public CommandException(String message) {
        super(message);
    }
}
