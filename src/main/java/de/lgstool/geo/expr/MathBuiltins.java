package de.lgstool.geo.expr;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The maths function library: unary functions ({@code sin}, {@code cos}, {@code sqrt},
 * {@code abs}, {@code ln}, …) and a few binary ones ({@code min}, {@code max},
 * {@code mod}, {@code atan2}). All operate on {@link Value.NumberV} and return one.
 *
 * <p>Geometry constructors are layered on top via a {@link CompositeBuiltins} so this
 * class stays free of any geometry dependency and is independently testable.
 */
public final class MathBuiltins implements BuiltinFunctions {

    private interface Unary {
        double apply(double a);
    }

    private interface Binary {
        double apply(double a, double b);
    }

    private final Map<String, Unary> unary = Map.ofEntries(
            Map.entry("sin", Math::sin),
            Map.entry("cos", Math::cos),
            Map.entry("tan", Math::tan),
            Map.entry("asin", Math::asin),
            Map.entry("acos", Math::acos),
            Map.entry("atan", Math::atan),
            Map.entry("sqrt", Math::sqrt),
            Map.entry("cbrt", Math::cbrt),
            Map.entry("abs", Math::abs),
            Map.entry("exp", Math::exp),
            Map.entry("ln", Math::log),
            Map.entry("log", Math::log10),
            Map.entry("floor", Math::floor),
            Map.entry("ceil", Math::ceil),
            Map.entry("round", (double a) -> (double) Math.round(a)),
            Map.entry("sign", Math::signum));

    private final Map<String, Binary> binary = Map.ofEntries(
            Map.entry("min", Math::min),
            Map.entry("max", Math::max),
            Map.entry("mod", (a, b) -> a % b),
            Map.entry("atan2", Math::atan2),
            Map.entry("pow", Math::pow));

    @Override
    public boolean has(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        return unary.containsKey(key) || binary.containsKey(key);
    }

    @Override
    public Value call(String name, List<Value> args, FunctionApplier applier) {
        String key = name.toLowerCase(Locale.ROOT);
        Unary u = unary.get(key);
        if (u != null) {
            requireArity(name, args, 1);
            return new Value.NumberV(u.apply(num(name, args.get(0))));
        }
        Binary b = binary.get(key);
        if (b != null) {
            requireArity(name, args, 2);
            return new Value.NumberV(b.apply(num(name, args.get(0)), num(name, args.get(1))));
        }
        throw new EvalException("Unbekannte Funktion '" + name + "'.");
    }

    private static void requireArity(String name, List<Value> args, int n) {
        if (args.size() != n) {
            throw new EvalException(name + " erwartet " + n + " Argument(e), erhalten " + args.size() + ".");
        }
    }

    private static double num(String name, Value v) {
        if (v instanceof Value.NumberV n) {
            return n.v();
        }
        throw new EvalException(name + " erwartet eine Zahl.");
    }
}
