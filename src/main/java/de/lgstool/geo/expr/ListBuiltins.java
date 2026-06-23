package de.lgstool.geo.expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * List operations on {@link Value.ListV} objects: {@code Sequence} (numeric range),
 * {@code Sum}, {@code Product}, {@code Mean}, {@code Element} (1-based), {@code Sort},
 * {@code Reverse} and {@code Append}. List length is provided by {@code Length} in the
 * geometry built-ins (which also covers segments and vectors).
 */
public final class ListBuiltins implements BuiltinFunctions {

    private static final Set<String> NAMES = Set.of(
            "sequence", "sum", "product", "mean", "element", "sort", "reverse", "append",
            "indexof", "take", "union", "intersection", "first", "last");

    @Override
    public boolean has(String name) {
        return NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public Value call(String name, List<Value> args, FunctionApplier applier) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "sequence" -> sequence(args);
            case "sum" -> new Value.NumberV(reduce(list(args, "Sum"), 0, Double::sum));
            case "product" -> new Value.NumberV(reduce(list(args, "Product"), 1, (a, b) -> a * b));
            case "mean" -> mean(args);
            case "element" -> element(args);
            case "sort" -> sort(args);
            case "reverse" -> reverse(args);
            case "append" -> append(args);
            case "indexof" -> indexOf(args);
            case "take" -> take(args);
            case "union" -> union(args);
            case "intersection" -> intersection(args);
            case "first" -> first(args);
            case "last" -> last(args);
            default -> throw new EvalException("Unbekannte Funktion '" + name + "'.");
        };
    }

    private static Value sequence(List<Value> args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new EvalException("Sequence erwartet (von, bis) oder (von, bis, Schritt).");
        }
        double from = num(args.get(0));
        double to = num(args.get(1));
        double step = args.size() == 3 ? num(args.get(2)) : 1;
        if (step == 0) {
            throw new EvalException("Sequence: Schritt darf nicht 0 sein.");
        }
        List<Value> out = new ArrayList<>();
        if (step > 0) {
            for (double x = from; x <= to + 1e-9; x += step) {
                out.add(new Value.NumberV(x));
            }
        } else {
            for (double x = from; x >= to - 1e-9; x += step) {
                out.add(new Value.NumberV(x));
            }
        }
        return new Value.ListV(List.copyOf(out));
    }

    private static Value mean(List<Value> args) {
        List<Value> items = list(args, "Mean");
        if (items.isEmpty()) {
            throw new EvalException("Mean: leere Liste.");
        }
        return new Value.NumberV(reduce(items, 0, Double::sum) / items.size());
    }

    private static Value element(List<Value> args) {
        if (args.size() != 2 || !(args.get(0) instanceof Value.ListV l) || !(args.get(1) instanceof Value.NumberV n)) {
            throw new EvalException("Element erwartet (Liste, Index).");
        }
        int i = (int) Math.round(n.v());
        if (i < 1 || i > l.items().size()) {
            throw new EvalException("Element: Index " + i + " außerhalb 1.." + l.items().size() + ".");
        }
        return l.items().get(i - 1);
    }

    private static Value sort(List<Value> args) {
        List<Value> items = new ArrayList<>(list(args, "Sort"));
        items.sort((a, b) -> Double.compare(num(a), num(b)));
        return new Value.ListV(List.copyOf(items));
    }

    private static Value reverse(List<Value> args) {
        List<Value> items = new ArrayList<>(list(args, "Reverse"));
        java.util.Collections.reverse(items);
        return new Value.ListV(List.copyOf(items));
    }

    private static Value append(List<Value> args) {
        if (args.size() != 2 || !(args.get(0) instanceof Value.ListV l)) {
            throw new EvalException("Append erwartet (Liste, Element).");
        }
        List<Value> items = new ArrayList<>(l.items());
        items.add(args.get(1));
        return new Value.ListV(List.copyOf(items));
    }

    private static Value indexOf(List<Value> args) {
        if (args.size() != 2 || !(args.get(1) instanceof Value.ListV l)) {
            throw new EvalException("IndexOf erwartet (Element, Liste).");
        }
        for (int i = 0; i < l.items().size(); i++) {
            if (l.items().get(i).equals(args.get(0))) {
                return new Value.NumberV(i + 1);   // 1-based
            }
        }
        return new Value.NumberV(0);   // not found
    }

    private static Value take(List<Value> args) {
        if (args.size() != 3 || !(args.get(0) instanceof Value.ListV l)
                || !(args.get(1) instanceof Value.NumberV m) || !(args.get(2) instanceof Value.NumberV n)) {
            throw new EvalException("Take erwartet (Liste, von, bis) mit 1-basierten Indizes.");
        }
        int from = Math.max(1, (int) Math.round(m.v()));
        int to = Math.min(l.items().size(), (int) Math.round(n.v()));
        List<Value> out = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            out.add(l.items().get(i - 1));
        }
        return new Value.ListV(List.copyOf(out));
    }

    private static Value union(List<Value> args) {
        List<Value> out = new ArrayList<>(twoLists(args, "Union")[0]);
        for (Value v : twoLists(args, "Union")[1]) {
            if (!out.contains(v)) {
                out.add(v);
            }
        }
        return new Value.ListV(List.copyOf(out));
    }

    private static Value intersection(List<Value> args) {
        List<Value>[] pair = twoLists(args, "Intersection");
        List<Value> out = new ArrayList<>();
        for (Value v : pair[0]) {
            if (pair[1].contains(v) && !out.contains(v)) {
                out.add(v);
            }
        }
        return new Value.ListV(List.copyOf(out));
    }

    private static Value first(List<Value> args) {
        List<Value> items = list(args, "First");
        if (items.isEmpty()) {
            throw new EvalException("First: leere Liste.");
        }
        return items.get(0);
    }

    private static Value last(List<Value> args) {
        List<Value> items = list(args, "Last");
        if (items.isEmpty()) {
            throw new EvalException("Last: leere Liste.");
        }
        return items.get(items.size() - 1);
    }

    @SuppressWarnings("unchecked")
    private static List<Value>[] twoLists(List<Value> args, String where) {
        if (args.size() != 2 || !(args.get(0) instanceof Value.ListV a) || !(args.get(1) instanceof Value.ListV b)) {
            throw new EvalException(where + " erwartet zwei Listen.");
        }
        return new List[]{a.items(), b.items()};
    }

    private interface DoubleOp {
        double apply(double a, double b);
    }

    private static double reduce(List<Value> items, double identity, DoubleOp op) {
        double acc = identity;
        for (Value v : items) {
            acc = op.apply(acc, num(v));
        }
        return acc;
    }

    private static List<Value> list(List<Value> args, String where) {
        if (args.size() != 1 || !(args.get(0) instanceof Value.ListV l)) {
            throw new EvalException(where + " erwartet eine Liste.");
        }
        return l.items();
    }

    private static double num(Value v) {
        if (v instanceof Value.NumberV n) {
            return n.v();
        }
        throw new EvalException("Zahl erwartet.");
    }
}
