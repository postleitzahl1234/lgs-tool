package de.lgstool.geo.expr;

import java.util.List;
import java.util.Set;

/**
 * Abstract syntax tree for the expression language. {@code Tuple} represents a
 * parenthesized coordinate list {@code (a, b)} or {@code (a, b, c)}; {@code Call}
 * is a function or geometry-command invocation {@code Name(args...)}.
 */
public sealed interface Expr {

    /** A numeric literal. {@code text} keeps the exact source ({@code "1/2"}, {@code "0.25"}) for
     *  exact rational evaluation; it is {@code null} for numbers synthesized in code. */
    record Num(double value, String text) implements Expr {
        public Num(double value) {
            this(value, null);
        }
    }

    record Ref(String name) implements Expr {
    }

    /** A string literal {@code "..."} — evaluates to a text object. */
    record Str(String value) implements Expr {
    }

    record Unary(char op, Expr operand) implements Expr {
    }

    record Bin(char op, Expr left, Expr right) implements Expr {
    }

    record Call(String name, List<Expr> args) implements Expr {
    }

    record Tuple(List<Expr> components) implements Expr {
    }

    /** A brace literal {@code {a, b, c}} — a list, or (when its items are equal-length numeric
     *  lists) a matrix. */
    record ListLit(List<Expr> items) implements Expr {
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
            case Str ignored -> {
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
            case ListLit l -> {
                for (Expr item : l.items()) {
                    collectNames(item, out);
                }
            }
        }
    }
}
