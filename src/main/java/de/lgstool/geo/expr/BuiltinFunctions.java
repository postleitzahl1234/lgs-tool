package de.lgstool.geo.expr;

import java.util.List;

/**
 * A registry of named built-in functions the {@link Evaluator} can call: maths
 * functions ({@code sin}, {@code sqrt}, …) and geometry constructors ({@code Line},
 * {@code Circle}, …). Implementations are looked up case-insensitively.
 */
public interface BuiltinFunctions {

    /** Whether a function with this name (case-insensitive) exists. */
    boolean has(String name);

    /**
     * Invokes the function with already-evaluated arguments. {@code applier} can apply
     * a {@link Value.FunctionV} at a point (used by calculus built-ins).
     */
    Value call(String name, List<Value> args, FunctionApplier applier);
}
