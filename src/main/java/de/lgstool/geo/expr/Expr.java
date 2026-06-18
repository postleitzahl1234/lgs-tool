package de.lgstool.geo.expr;

import java.util.List;
import java.util.Set;

/**
 * Abstract syntax tree for the expression language. {@code Tuple} represents a
 * parenthesized coordinate list {@code (a, b)} or {@code (a, b, c)}; {@code Call}
 * is a function or geometry-command invocation {@code Name(args...)}.
 */
public sealed interface Expr {

    record Num(double value) implements Expr {
    }

    record Ref(String name) implements Expr {
    }

    record Unary(char op, Expr operand) implements Expr {
    }

    record Bin(char op, Expr left, Expr right) implements Expr {
    }

    record Call(String name, List<Expr> args) implements Expr {
    }

    record Tuple(List<Expr> components) implements Expr {
    }

    /**
     * Collects every identifier referenced by the tree — both bare {@link Ref}s and
     * {@link Call} names. A construction intersects this with its existing object
     * names to derive an object's dependencies (built-ins simply never match).
     */
    static void collectNames(Expr e, Set<String> out) {
        switch (e) {
            case Num ignored -> {
            }
            case Ref r -> out.add(r.name());
            case Unary u -> collectNames(u.operand(), out);
            case Bin b -> {
                collectNames(b.left(), out);
                collectNames(b.right(), out);
            }
            case Call c -> {
                out.add(c.name());
                for (Expr a : c.args()) {
                    collectNames(a, out);
                }
            }
            case Tuple t -> {
                for (Expr comp : t.components()) {
                    collectNames(comp, out);
                }
            }
        }
    }
}
