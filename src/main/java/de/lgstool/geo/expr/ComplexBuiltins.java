package de.lgstool.geo.expr;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Accessors for complex numbers: {@code Real}, {@code Imaginary}, {@code Conjugate},
 * {@code Arg} (argument/phase) and {@code Modulus} (absolute value). Complex arithmetic
 * itself ({@code + − * /}) is handled in the {@link Evaluator}; the imaginary unit is the
 * constant {@code i}.
 */
public final class ComplexBuiltins implements BuiltinFunctions {

    private static final Set<String> NAMES = Set.of(
            "real", "imaginary", "conjugate", "arg", "modulus");

    @Override
    public boolean has(String name) {
        return NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public Value call(String name, List<Value> args, FunctionApplier applier) {
        double[] z = complex(args, name);
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "real" -> new Value.NumberV(z[0]);
            case "imaginary" -> new Value.NumberV(z[1]);
            case "conjugate" -> new Value.ComplexV(z[0], -z[1]);
            case "arg" -> new Value.NumberV(Math.atan2(z[1], z[0]));
            case "modulus" -> new Value.NumberV(Math.hypot(z[0], z[1]));
            default -> throw new EvalException("Unbekannte Funktion '" + name + "'.");
        };
    }

    private static double[] complex(List<Value> args, String where) {
        if (args.size() != 1) {
            throw new EvalException(where + " erwartet eine (komplexe) Zahl.");
        }
        return switch (args.get(0)) {
            case Value.ComplexV z -> new double[]{z.re(), z.im()};
            case Value.NumberV n -> new double[]{n.v(), 0};
            default -> throw new EvalException(where + " erwartet eine Zahl.");
        };
    }
}
