package de.lgstool.geo.expr;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Numeric/symbolic analysis built-ins that need a {@link FunctionApplier}: {@code Limit}
 * (two-sided numeric limit of a function at a point) and {@code TaylorPolynomial} (the
 * degree-n Taylor expansion about a point, built from the symbolic {@link Differentiator}).
 */
public final class CasBuiltins implements BuiltinFunctions {

    private static final Set<String> NAMES = Set.of("limit", "taylorpolynomial", "taylor");

    @Override
    public boolean has(String name) {
        return NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public Value call(String name, List<Value> args, FunctionApplier applier) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "limit" -> limit(args, applier);
            default -> taylor(args, applier);
        };
    }

    private static Value limit(List<Value> args, FunctionApplier applier) {
        if (args.size() != 2 || !(args.get(0) instanceof Value.FunctionV f) || f.params().size() != 1
                || !(args.get(1) instanceof Value.NumberV x0)) {
            throw new EvalException("Limit erwartet (Funktion f(x), Stelle).");
        }
        double left = sample(applier, f, x0.v() - 1e-6);
        double right = sample(applier, f, x0.v() + 1e-6);
        if (Double.isFinite(left) && Double.isFinite(right) && Math.abs(left - right) < 1e-4) {
            return new Value.NumberV((left + right) / 2);
        }
        throw new EvalException("Grenzwert existiert nicht (oder ist unendlich).");
    }

    private static Value taylor(List<Value> args, FunctionApplier applier) {
        if (args.size() != 3 || !(args.get(0) instanceof Value.FunctionV f) || f.params().size() != 1
                || !(args.get(1) instanceof Value.NumberV at) || !(args.get(2) instanceof Value.NumberV deg)) {
            throw new EvalException("TaylorPolynomial erwartet (Funktion f(x), Stelle, Grad).");
        }
        String var = f.params().get(0);
        int n = (int) Math.round(deg.v());
        if (n < 0) {
            throw new EvalException("TaylorPolynomial: Grad muss ≥ 0 sein.");
        }
        double x0 = at.v();
        Expr derivative = f.body();
        double factorial = 1;
        Expr body = null;
        for (int k = 0; k <= n; k++) {
            if (k > 0) {
                factorial *= k;
            }
            double fk = sample(applier, new Value.FunctionV(f.params(), derivative), x0);
            double coeff = fk / factorial;
            if (Math.abs(coeff) > 1e-12) {
                Expr term = term(coeff, x0, k, var);
                body = body == null ? term : new Expr.Bin('+', body, term);
            }
            derivative = Differentiator.differentiate(derivative, var);
        }
        return new Value.FunctionV(f.params(), body == null ? new Expr.Num(0) : body);
    }

    private static Expr term(double coeff, double x0, int k, String var) {
        if (k == 0) {
            return new Expr.Num(coeff);
        }
        Expr base = Math.abs(x0) < 1e-12
                ? new Expr.Ref(var)
                : new Expr.Bin('-', new Expr.Ref(var), new Expr.Num(x0));
        Expr power = k == 1 ? base : new Expr.Bin('^', base, new Expr.Num(k));
        return Math.abs(coeff - 1) < 1e-12 ? power : new Expr.Bin('*', new Expr.Num(coeff), power);
    }

    private static double sample(FunctionApplier applier, Value.FunctionV f, double x) {
        Value v = applier.apply(f, List.of(new Value.NumberV(x)));
        return v instanceof Value.NumberV n ? n.v() : Double.NaN;
    }
}
