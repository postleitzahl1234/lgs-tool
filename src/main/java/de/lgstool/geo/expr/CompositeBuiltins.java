package de.lgstool.geo.expr;

import java.util.List;

/**
 * Chains two {@link BuiltinFunctions} registries: the {@code primary} is consulted
 * first, falling back to the {@code fallback}. Used to layer geometry constructors
 * over the {@link MathBuiltins} maths library.
 */
public final class CompositeBuiltins implements BuiltinFunctions {

    private final BuiltinFunctions primary;
    private final BuiltinFunctions fallback;

    public CompositeBuiltins(BuiltinFunctions primary, BuiltinFunctions fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public boolean has(String name) {
        return primary.has(name) || fallback.has(name);
    }

    @Override
    public Value call(String name, List<Value> args) {
        if (primary.has(name)) {
            return primary.call(name, args);
        }
        return fallback.call(name, args);
    }
}
