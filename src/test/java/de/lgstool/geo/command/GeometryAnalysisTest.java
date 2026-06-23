package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Conic descriptors and richer geometry: Center, circumcircle, Centroid, ClosestPoint, Curvature, function Intersect. */
class GeometryAnalysisTest {

    private GeoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GeoEngine();
    }

    private void ok(String line) {
        GeoResult r = engine.execute(line);
        assertTrue(r.ok(), () -> "expected ok for '" + line + "' but got: " + r.messages());
    }

    private Value v(String name) {
        return engine.construction().get(name).value();
    }

    private Value.PointV point(String name) {
        return (Value.PointV) v(name);
    }

    @Test
    @DisplayName("Circle through three points has the expected centre")
    void circumcircle() {
        ok("c = Circle((1,0), (0,1), (-1,0))");      // unit circle centred at origin
        Value.CircleV circle = (Value.CircleV) v("c");
        assertEquals(0.0, circle.center().x(), 1e-6);
        assertEquals(0.0, circle.center().y(), 1e-6);
        assertEquals(1.0, circle.r(), 1e-6);
    }

    @Test
    @DisplayName("Center of an ellipse from two foci")
    void ellipseCenter() {
        ok("e = Ellipse((-3,0), (3,0), 5)");
        ok("m = Center(e)");
        assertEquals(0.0, point("m").x(), 1e-6);
        assertEquals(0.0, point("m").y(), 1e-6);
    }

    @Test
    @DisplayName("Centroid of a triangle")
    void centroid() {
        ok("p = Polygon((0,0), (3,0), (0,3))");
        ok("g = Centroid(p)");
        assertEquals(1.0, point("g").x(), 1e-6);
        assertEquals(1.0, point("g").y(), 1e-6);
    }

    @Test
    @DisplayName("ClosestPoint on the x-axis drops the y coordinate")
    void closestPoint() {
        ok("g = Line((0,0), (1,0))");
        ok("q = ClosestPoint((2,5), g)");
        assertEquals(2.0, point("q").x(), 1e-6);
        assertEquals(0.0, point("q").y(), 1e-6);
    }

    @Test
    @DisplayName("Curvature of a circle-like function and of a parabola at its vertex")
    void curvature() {
        ok("f(x) = x^2");
        ok("k = Curvature(0, f)");
        assertEquals(2.0, ((Value.NumberV) v("k")).v(), 1e-4);   // 2 at the vertex of y=x^2
    }

    @Test
    @DisplayName("Intersect of two functions finds a crossing")
    void functionIntersect() {
        ok("f(x) = x^2");
        ok("g(x) = x + 2");
        ok("P = Intersect(f, g, 2)");           // crossings at x=-1 and x=2; pick the 2nd
        assertEquals(2.0, point("P").x(), 1e-4);
        assertEquals(4.0, point("P").y(), 1e-4);
    }

    @Test
    @DisplayName("Roots returns all zeros of a function in the window")
    void rootsPlural() {
        ok("f(x) = x^2 - 1");
        ok("R = Roots(f)");
        assertEquals(2, ((Value.ListV) v("R")).items().size());
    }
}
