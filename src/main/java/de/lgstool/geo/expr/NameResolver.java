package de.lgstool.geo.expr;

/** Resolves an identifier to the value of a named object, or {@code null} if unknown. */
@FunctionalInterface
public interface NameResolver {
    Value resolve(String name);
}
