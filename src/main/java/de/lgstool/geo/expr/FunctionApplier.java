package de.lgstool.geo.expr;

import java.util.List;

/**
 * Applies a {@link Value.FunctionV} to argument values. Passed to
 * {@link BuiltinFunctions#call} so geometry/calculus built-ins (e.g. {@code Tangent},
 * {@code Integral}) can evaluate a function at a point without their own evaluator.
 */
@FunctionalInterface
public interface FunctionApplier {
    Value apply(Value.FunctionV f, List<Value> args);
}
