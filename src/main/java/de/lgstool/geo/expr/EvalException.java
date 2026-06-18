package de.lgstool.geo.expr;

/** Thrown for lexing, parsing and evaluation errors in the expression language. */
public final class EvalException extends RuntimeException {

    public EvalException(String message) {
        super(message);
    }
}
