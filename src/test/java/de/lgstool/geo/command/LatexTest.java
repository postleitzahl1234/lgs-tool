package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LatexTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private Value value(String line) {
        engine.execute(line);
        String name = line.substring(0, line.indexOf('=')).replaceAll("\\(.*", "").trim();
        return engine.construction().get(name).value();
    }

    @Test
    @DisplayName("matrix renders as a pmatrix with exact fractions")
    void matrix() {
        String tex = Latex.of(value("M = {{1, 1/2}, {3, 4}}"));
        assertTrue(tex.startsWith("\\begin{pmatrix}"), tex);
        assertTrue(tex.contains("\\frac{1}{2}"), tex);
        assertTrue(tex.endsWith("\\end{pmatrix}"), tex);
    }

    @Test
    @DisplayName("function body renders with \\frac and ^{}")
    void function() {
        String tex = Latex.of(value("f(x) = x^2 / 2"));
        assertEquals("\\frac{x^{2}}{2}", tex);
    }

    @Test
    @DisplayName("point and list render")
    void pointAndList() {
        assertEquals("\\left(1,\\ 2\\right)", Latex.of(value("A = (1, 2)")));
        String list = Latex.of(value("L = {1, 2, 3}"));
        assertTrue(list.startsWith("\\left\\{") && list.contains("1,\\ 2,\\ 3"), list);
    }

    @Test
    @DisplayName("line renders as an equation")
    void line() {
        String tex = Latex.of(value("g = Line((0,0), (1,1))"));
        assertTrue(tex.contains("x") && tex.contains("y") && tex.contains("="), tex);
    }
}
