package de.lgstool.geo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.geo.expr.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocusTest {

    @Test
    @DisplayName("a point parameterized by a slider traces the unit circle")
    void tracesCircle() {
        GeoEngine engine = new GeoEngine();
        engine.execute("s = Slider(0, 6.283185307)");
        engine.execute("P = (cos(s), sin(s))");
        GeoResult r = engine.execute("loc = Locus(P, s)");
        assertTrue(r.ok(), () -> "locus failed: " + r.messages());

        Value v = engine.construction().get("loc").value();
        assertTrue(v instanceof Value.LocusV);
        Value.LocusV locus = (Value.LocusV) v;
        assertTrue(locus.points().size() > 100);
        for (Value.PointV p : locus.points()) {
            assertEquals(1, Math.hypot(p.x(), p.y()), 1e-6);
        }
    }

    @Test
    @DisplayName("Locus rejects a non-slider second argument")
    void requiresSlider() {
        GeoEngine engine = new GeoEngine();
        engine.execute("n = 3");
        engine.execute("P = (n, 0)");
        assertTrue(!engine.execute("loc = Locus(P, n)").ok());
    }
}
