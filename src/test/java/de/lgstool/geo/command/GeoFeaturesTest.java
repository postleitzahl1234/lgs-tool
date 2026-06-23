package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Covers the curve-analysis, measurement, transformation, construction and 3D-intersection builtins. */
class GeoFeaturesTest {

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

    private double num(String name) {
        return ((Value.NumberV) v(name)).v();
    }

    private Value.PointV point(String name) {
        return (Value.PointV) v(name);
    }

    // ---- Package A: curve analysis ---------------------------------------

    @Test
    @DisplayName("Root finds a zero of f(x) = x^2 - 4")
    void root() {
        ok("f(x) = x^2 - 4");
        ok("R = Root(f)");
        assertEquals(2.0, Math.abs(point("R").x()), 1e-6);
        assertEquals(0.0, point("R").y(), 1e-9);
    }

    @Test
    @DisplayName("Root(f, x0) uses Newton from a start value")
    void rootFromStart() {
        ok("f(x) = x^2 - 4");
        ok("R = Root(f, 3)");
        assertEquals(2.0, point("R").x(), 1e-6);
    }

    @Test
    @DisplayName("Extremum of x^2 - 4 is the vertex (0, -4)")
    void extremum() {
        ok("f(x) = x^2 - 4");
        ok("E = Extremum(f)");
        assertEquals(0.0, point("E").x(), 1e-6);
        assertEquals(-4.0, point("E").y(), 1e-6);
    }

    @Test
    @DisplayName("InflectionPoint of x^3 is the origin")
    void inflection() {
        ok("f(x) = x^3");
        ok("W = InflectionPoint(f)");
        assertEquals(0.0, point("W").x(), 1e-6);
        assertEquals(0.0, point("W").y(), 1e-6);
    }

    @Test
    @DisplayName("Normal to x^2 at x0=1 has slope -1/2")
    void normal() {
        ok("f(x) = x^2");
        ok("n = Normal(1, f)");
        ok("m = Slope(n)");
        assertEquals(-0.5, num("m"), 1e-9);
    }

    @Test
    @DisplayName("Derivative yields a renderable 1-arg function that can be evaluated")
    void derivativeIsRenderableFunction() {
        ok("f(x) = x^2");
        ok("g = Derivative(f)");
        Value gv = v("g");
        assertInstanceOf(Value.FunctionV.class, gv);
        assertEquals(1, ((Value.FunctionV) gv).params().size());   // the guard the plot relies on
        ok("y = g(2)");                                            // derivative 2x evaluated at 2
        assertEquals(4.0, num("y"), 1e-9);
    }

    // ---- Package B: measurements -----------------------------------------

    @Test
    @DisplayName("Distance from a point to a line")
    void distancePointLine() {
        ok("g = Line((0,0), (1,0))");      // the x-axis
        ok("d = Distance((0,5), g)");
        assertEquals(5.0, num("d"), 1e-9);
    }

    @Test
    @DisplayName("Angle between perpendicular lines is 90 degrees")
    void anglePerpendicular() {
        ok("g = Line((0,0), (1,0))");
        ok("h = Line((0,0), (0,1))");
        ok("a = Angle(g, h)");
        assertEquals(90.0, num("a"), 1e-6);
    }

    @Test
    @DisplayName("Length, Area, Perimeter and Slope")
    void scalarMeasures() {
        ok("s = Segment((0,0), (3,4))");
        ok("L = Length(s)");
        assertEquals(5.0, num("L"), 1e-9);

        ok("p = Polygon((0,0), (4,0), (4,3), (0,3))");
        ok("A = Area(p)");
        ok("U = Perimeter(p)");
        assertEquals(12.0, num("A"), 1e-9);
        assertEquals(14.0, num("U"), 1e-9);

        ok("g = Line((0,0), (1,2))");
        ok("k = Slope(g)");
        assertEquals(2.0, num("k"), 1e-9);
    }

    @Test
    @DisplayName("Distance from a point to a plane (3D)")
    void distancePointPlane() {
        ok("E = Plane((0,0,0), (1,0,0), (0,1,0))");   // z = 0
        ok("d = Distance((0,0,7), E)");
        assertEquals(7.0, num("d"), 1e-9);
    }

    // ---- Package C: transformations & constructions ----------------------

    @Test
    @DisplayName("Reflect a point across the y-axis")
    void reflect() {
        ok("g = Line((0,0), (0,1))");      // the y-axis
        ok("B = Reflect((1,0), g)");
        assertEquals(-1.0, point("B").x(), 1e-9);
        assertEquals(0.0, point("B").y(), 1e-9);
    }

    @Test
    @DisplayName("Rotate a point 90 degrees about the origin")
    void rotate() {
        ok("B = Rotate((1,0), 90)");
        assertEquals(0.0, point("B").x(), 1e-9);
        assertEquals(1.0, point("B").y(), 1e-9);
    }

    @Test
    @DisplayName("Translate and Dilate a point")
    void translateDilate() {
        ok("T = Translate((1,1), Vector((0,0), (2,3)))");
        assertEquals(3.0, point("T").x(), 1e-9);
        assertEquals(4.0, point("T").y(), 1e-9);

        ok("D = Dilate((2,2), 3)");
        assertEquals(6.0, point("D").x(), 1e-9);
        assertEquals(6.0, point("D").y(), 1e-9);
    }

    @Test
    @DisplayName("ParallelLine and PerpendicularBisector")
    void constructions() {
        ok("g = Line((0,0), (1,0))");
        ok("p = ParallelLine((0,5), g)");
        ok("k = Slope(p)");
        assertEquals(0.0, num("k"), 1e-9);

        ok("m = PerpendicularBisector((0,0), (2,0))");
        ok("d = Distance((1,9), m)");   // (1,9) lies on x = 1
        assertEquals(0.0, num("d"), 1e-9);
    }

    @Test
    @DisplayName("AngleBisector of the right angle at the origin has slope 1")
    void angleBisector() {
        ok("w = AngleBisector((1,0), (0,0), (0,1))");
        ok("k = Slope(w)");
        assertEquals(1.0, num("k"), 1e-9);
    }

    @Test
    @DisplayName("PointOn places a point along a circle and a segment")
    void pointOn() {
        ok("c = Circle((0,0), 1)");
        ok("P = PointOn(c, 0)");
        assertEquals(1.0, point("P").x(), 1e-9);
        assertEquals(0.0, point("P").y(), 1e-9);
        ok("Q = PointOn(c, 0.25)");
        assertEquals(0.0, point("Q").x(), 1e-9);
        assertEquals(1.0, point("Q").y(), 1e-9);

        ok("s = Segment((0,0), (4,0))");
        ok("M = PointOn(s, 0.5)");
        assertEquals(2.0, point("M").x(), 1e-9);
    }

    @Test
    @DisplayName("PointOn(circle, slider) is dependent and follows the slider")
    void pointOnFollowsSlider() {
        ok("c = Circle((0,0), 1)");
        ok("t = Slider(0, 1)");
        ok("P = PointOn(c, t)");
        assertEquals(1.0, point("P").x(), 1e-9);          // t = 0
        engine.setFreeValueLive("t", new Value.NumberV(0.25));
        assertEquals(1.0, point("P").y(), 1e-9);          // t = 0.25 -> (0,1)
    }

    // ---- Package B: 3D intersections -------------------------------------

    @Test
    @DisplayName("Intersect a line and a plane in space")
    void intersectLinePlane() {
        ok("E = Plane((0,0,0), (1,0,0), (0,1,0))");   // z = 0
        ok("g = Line((0,0,5), (0,0,6))");             // vertical line through (0,0,*)
        ok("S = Intersect(g, E)");
        Value.PointV s = point("S");
        assertEquals(0.0, s.x(), 1e-9);
        assertEquals(0.0, s.y(), 1e-9);
        assertEquals(0.0, s.z(), 1e-9);
    }

    @Test
    @DisplayName("Intersect of two planes is a line")
    void intersectPlanePlane() {
        ok("E = Plane((0,0,0), (1,0,0), (0,1,0))");   // z = 0
        ok("F = Plane((0,0,0), (1,0,0), (0,0,1))");   // y = 0
        ok("g = Intersect(E, F)");
        assertInstanceOf(Value.Line3DV.class, v("g"));
    }

    @Test
    @DisplayName("Intersect picks both points of a line and a circle")
    void intersectLineCircleBoth() {
        ok("c = Circle((0,0), 1)");
        ok("g = Line((0,0), (1,0))");
        ok("P = Intersect(g, c, 1)");
        ok("Q = Intersect(g, c, 2)");
        assertEquals(1.0, point("P").x(), 1e-9);
        assertEquals(-1.0, point("Q").x(), 1e-9);
    }
}
