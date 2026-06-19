package de.lgstool.ui;

import java.util.List;

/**
 * Static catalogues of command suggestions for the command lines: one for the LGS
 * screens ([HAUPT]/[LGS]) and one for the graphing input bar ([GEO]).
 */
public final class Suggestions {

    private Suggestions() {
    }

    public static List<Suggestion> lgs() {
        return List.of(
                Suggestion.of("create", "create(name, b*h)"),
                Suggestion.of("add", "add(c1 … cn = b [, row])"),
                Suggestion.of("solve", "solve([n])"),
                Suggestion.of("del", "del(arg)"),
                Suggestion.of("swap", "swap(i, j)"),
                Suggestion.of("scale", "scale(i, factor)"),
                Suggestion.of("combine", "combine(i, factor, j)"),
                Suggestion.of("det", "det()"),
                Suggestion.of("rank", "rank()"),
                Suggestion.of("cond", "cond([norm])"),
                Suggestion.of("show", "show()"),
                Suggestion.of("clear", "clear()"),
                Suggestion.word("undo"),
                Suggestion.word("redo"),
                Suggestion.of("rename", "rename(new)"),
                Suggestion.of("highlight", "highlight(i [, hex])"),
                Suggestion.of("resize", "resize(b*h)"),
                Suggestion.of("setvar", "setvar(col, name)"),
                Suggestion.of("save", "save(name)"),
                Suggestion.of("load", "load(name)"),
                Suggestion.of("open", "open(name)"),
                Suggestion.of("close", "close()"),
                Suggestion.of("list", "list()"),
                Suggestion.of("fit", "fit()"),
                Suggestion.of("verify", "verify()"),
                Suggestion.of("inverse", "inverse()"),
                Suggestion.of("subst", "subst(var, value)"),
                Suggestion.of("frac", "frac()"),
                Suggestion.of("dec", "dec()"),
                Suggestion.of("animate", "animate(row, from, to [, dur])"),
                Suggestion.of("axis", "axis(axis, min, max)"),
                Suggestion.of("rand", "rand(min, max [, row])"),
                Suggestion.of("rref", "rref()"),
                Suggestion.of("ref", "ref()"),
                Suggestion.of("transpose", "transpose()"),
                Suggestion.of("trace", "trace()"),
                Suggestion.of("nullspace", "nullspace()"),
                Suggestion.of("cramer", "cramer()"),
                Suggestion.of("latex", "latex()"),
                Suggestion.of("csv", "csv(name)"),
                Suggestion.of("loadcsv", "loadcsv(name)"),
                Suggestion.of("run", "run(file)"),
                Suggestion.of("examples", "examples([name])"),
                Suggestion.of("savelog", "savelog(name)"),
                Suggestion.of("copylog", "copylog()"),
                Suggestion.of("saveplot", "saveplot(name)"),
                Suggestion.of("labels", "labels()"));
    }

    public static List<Suggestion> geo() {
        return List.of(
                Suggestion.of("Point", "Point(x, y [, z])"),
                Suggestion.of("Vector", "Vector(A, B)"),
                Suggestion.of("Midpoint", "Midpoint(A, B)"),
                Suggestion.of("Line", "Line(A, B)"),
                Suggestion.of("Segment", "Segment(A, B)"),
                Suggestion.of("Circle", "Circle(M, r)"),
                Suggestion.of("Distance", "Distance(A, B)"),
                Suggestion.of("Intersect", "Intersect(a, b)"),
                Suggestion.of("Polygon", "Polygon(A, B, C, …)"),
                Suggestion.of("Angle", "Angle(A, B, C)"),
                Suggestion.of("Derivative", "Derivative(f)"),
                Suggestion.of("Integral", "Integral(f, a, b)"),
                Suggestion.of("Tangent", "Tangent(x0, f)"),
                Suggestion.of("Ellipse", "Ellipse(F1, F2, a)"),
                Suggestion.of("Hyperbola", "Hyperbola(F1, F2, a)"),
                Suggestion.of("Parabola", "Parabola(F, line)"),
                Suggestion.of("Locus", "Locus(P, s)"),
                Suggestion.of("Slider", "Slider(min, max [, step])"),
                Suggestion.of("Plane", "Plane(A, B, C)"),
                Suggestion.of("Sphere", "Sphere(M, r)"),
                Suggestion.of("Delete", "Delete(obj)"),
                Suggestion.of("save", "save(name)"),
                Suggestion.of("load", "load(name)"),
                Suggestion.word("clear"),
                Suggestion.word("undo"),
                Suggestion.word("redo"),
                Suggestion.word("list"));
    }
}
