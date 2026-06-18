package de.lgstool.geo.model;

import de.lgstool.geo.expr.BuiltinFunctions;
import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.Evaluator;
import de.lgstool.geo.expr.Expr;
import de.lgstool.geo.expr.Value;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The dynamic-geometry document: an ordered set of named {@link GeoObject}s plus a
 * dependency graph. Defining an object computes its dependencies (the existing
 * object names its definition references), rejects cycles, and re-evaluates all
 * dependent objects in topological order so the construction stays consistent.
 *
 * <p>Built-in geometry/maths functions are injected as {@link BuiltinFunctions}, so
 * this package depends only on the expression engine, never on the command layer.
 */
public final class Construction {

    private final BuiltinFunctions builtins;
    private final LinkedHashMap<String, GeoObject> objects = new LinkedHashMap<>();

    public Construction(BuiltinFunctions builtins) {
        this.builtins = builtins;
    }

    public boolean contains(String name) {
        return objects.containsKey(name);
    }

    public GeoObject get(String name) {
        return objects.get(name);
    }

    public List<String> names() {
        return new ArrayList<>(objects.keySet());
    }

    public List<GeoObject> objects() {
        return new ArrayList<>(objects.values());
    }

    public int size() {
        return objects.size();
    }

    /**
     * Defines or redefines an object. Evaluates eagerly; on a cycle or evaluation
     * error throws {@link EvalException} and leaves the construction unchanged.
     */
    public GeoObject define(String name, String source, Expr ast, List<String> params) {
        boolean isFunction = params != null && !params.isEmpty();
        List<String> deps = computeDependencies(name, ast, params);
        GeoObject.Kind kind = isFunction ? GeoObject.Kind.FUNCTION
                : deps.isEmpty() ? GeoObject.Kind.FREE : GeoObject.Kind.DEPENDENT;
        rejectCycles(name, deps);

        Value value = kind == GeoObject.Kind.FUNCTION
                ? new Value.FunctionV(params, ast)
                : new Evaluator(this::resolve, builtins).eval(ast);

        GeoObject existing = objects.get(name);
        GeoObject obj = new GeoObject(name, kind, ast, deps, params, source, value);
        if (existing != null) {
            obj.setVisible(existing.isVisible());
            obj.setColorHex(existing.colorHex());
        }
        objects.put(name, obj);
        recomputeAll();
        return obj;
    }

    /** Evaluates a standalone expression against the current objects (e.g. for auto-naming). */
    public Value evaluate(Expr ast) {
        return new Evaluator(this::resolve, builtins).eval(ast);
    }

    /**
     * Inserts a free object with a precomputed value that has no expression definition
     * (e.g. a sampled locus). Style is carried over when redefining.
     */
    public GeoObject defineLiteral(String name, String source, Value value) {
        GeoObject existing = objects.get(name);
        GeoObject obj = new GeoObject(name, GeoObject.Kind.FREE, new Expr.Num(0), List.of(), List.of(), source, value);
        if (existing != null) {
            obj.setVisible(existing.isVisible());
            obj.setColorHex(existing.colorHex());
        }
        objects.put(name, obj);
        recomputeAll();
        return obj;
    }

    /** Overrides a {@link GeoObject.Kind#FREE} object's value (point drag / slider move). */
    public void setFreeValue(String name, Value value) {
        GeoObject obj = objects.get(name);
        if (obj == null) {
            throw new EvalException("Unbekanntes Objekt '" + name + "'.");
        }
        if (obj.kind() != GeoObject.Kind.FREE) {
            throw new EvalException("'" + name + "' ist nicht frei und kann nicht direkt gesetzt werden.");
        }
        obj.setValue(value);
        recomputeAll();
    }

    /** Deletes an object together with everything that (transitively) depends on it. */
    public List<String> delete(String name) {
        if (!objects.containsKey(name)) {
            return List.of();
        }
        Set<String> doomed = new LinkedHashSet<>();
        collectDependents(name, doomed);
        for (String n : doomed) {
            objects.remove(n);
        }
        recomputeAll();
        return new ArrayList<>(doomed);
    }

    public void clear() {
        objects.clear();
    }

    public Construction copy() {
        Construction c = new Construction(builtins);
        for (GeoObject o : objects.values()) {
            c.objects.put(o.name(), o.copy());
        }
        return c;
    }

    // ---- internals -------------------------------------------------------

    private Value resolve(String name) {
        GeoObject o = objects.get(name);
        return o == null ? null : o.value();
    }

    private List<String> computeDependencies(String name, Expr ast, List<String> params) {
        Set<String> refs = new HashSet<>();
        Expr.collectNames(ast, refs);
        List<String> deps = new ArrayList<>();
        for (String r : refs) {
            if (params != null && params.contains(r)) {
                continue;
            }
            if (objects.containsKey(r)) {
                deps.add(r);
            }
        }
        return deps;
    }

    private void recomputeAll() {
        Evaluator evaluator = new Evaluator(this::resolve, builtins);
        for (String n : topologicalOrder()) {
            GeoObject o = objects.get(n);
            if (o.kind() == GeoObject.Kind.DEPENDENT) {
                o.setValue(evaluator.eval(o.definition()));
            }
        }
    }

    /** Names ordered so every object comes after the objects it depends on. */
    private List<String> topologicalOrder() {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (GeoObject o : objects.values()) {
            graph.put(o.name(), o.dependencies());
        }
        return topologicalOrder(graph);
    }

    /** Kahn's algorithm over a name → dependency-names graph; throws on a cycle. */
    private static List<String> topologicalOrder(Map<String, List<String>> graph) {
        Map<String, Integer> indegree = new LinkedHashMap<>();
        for (String n : graph.keySet()) {
            indegree.put(n, 0);
        }
        for (Map.Entry<String, List<String>> e : graph.entrySet()) {
            for (String dep : e.getValue()) {
                if (graph.containsKey(dep)) {
                    indegree.merge(e.getKey(), 1, Integer::sum);
                }
            }
        }
        Deque<String> ready = new ArrayDeque<>();
        indegree.forEach((n, d) -> {
            if (d == 0) {
                ready.add(n);
            }
        });
        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String n = ready.poll();
            order.add(n);
            for (Map.Entry<String, List<String>> e : graph.entrySet()) {
                if (e.getValue().contains(n)) {
                    int d = indegree.merge(e.getKey(), -1, Integer::sum);
                    if (d == 0) {
                        ready.add(e.getKey());
                    }
                }
            }
        }
        if (order.size() != graph.size()) {
            throw new EvalException("Zyklische Abhängigkeit erkannt.");
        }
        return order;
    }

    private void rejectCycles(String name, List<String> deps) {
        // Build the prospective graph (new/updated entry applied) and require an order.
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (GeoObject o : objects.values()) {
            graph.put(o.name(), o.dependencies());
        }
        graph.put(name, deps);
        try {
            topologicalOrder(graph);
        } catch (EvalException cycle) {
            throw new EvalException("Definition von '" + name + "' erzeugt einen Zyklus.");
        }
    }

    private void collectDependents(String target, Set<String> acc) {
        if (!acc.add(target)) {
            return;
        }
        for (GeoObject o : objects.values()) {
            if (o.dependencies().contains(target)) {
                collectDependents(o.name(), acc);
            }
        }
    }
}
