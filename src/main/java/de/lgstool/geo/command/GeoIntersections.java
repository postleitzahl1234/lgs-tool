package de.lgstool.geo.command;

import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.Value;
import java.util.List;

/**
 * 2D intersection maths for {@code Intersect(a, b)}: line∩line, line∩circle and
 * circle∩circle. Returns the (first) intersection point or fails when there is none.
 */
final class GeoIntersections {

    private static final double EPS = 1e-9;

    private GeoIntersections() {
    }

    static Value intersect(List<Value> args) {
        GeoBuiltins.require(args, 2, "Intersect");
        Value a = args.get(0);
        Value b = args.get(1);
        if (a instanceof Value.LineV la && b instanceof Value.LineV lb) {
            return lineLine(la, lb);
        }
        if (a instanceof Value.LineV la && b instanceof Value.CircleV cb) {
            return lineCircle(la, cb);
        }
        if (a instanceof Value.CircleV ca && b instanceof Value.LineV lb) {
            return lineCircle(lb, ca);
        }
        if (a instanceof Value.CircleV ca && b instanceof Value.CircleV cb) {
            return lineCircle(radicalLine(ca, cb), ca);
        }
        throw new EvalException("Intersect unterstützt Gerade/Kreis-Schnitte (2D).");
    }

    private static Value lineLine(Value.LineV p, Value.LineV q) {
        double det = p.a() * q.b() - q.a() * p.b();
        if (Math.abs(det) < EPS) {
            throw new EvalException("Die Geraden sind parallel — kein Schnittpunkt.");
        }
        double x = (p.c() * q.b() - q.c() * p.b()) / det;
        double y = (p.a() * q.c() - q.a() * p.c()) / det;
        return Value.PointV.of2D(x, y);
    }

    private static Value lineCircle(Value.LineV line, Value.CircleV circle) {
        double a = line.a();
        double b = line.b();
        double c = line.c();
        double cx = circle.center().x();
        double cy = circle.center().y();
        double r = circle.r();

        // Foot of the perpendicular from the centre onto the line, then offset by the
        // half-chord along the line direction.
        double norm2 = a * a + b * b;
        if (norm2 < EPS) {
            throw new EvalException("Ungültige Gerade.");
        }
        double dist = (a * cx + b * cy - c) / Math.sqrt(norm2);
        if (Math.abs(dist) > r + EPS) {
            throw new EvalException("Gerade und Kreis schneiden sich nicht.");
        }
        double footX = cx - a * (a * cx + b * cy - c) / norm2;
        double footY = cy - b * (a * cx + b * cy - c) / norm2;
        double half = Math.sqrt(Math.max(0, r * r - dist * dist));
        double dirLen = Math.sqrt(norm2);
        double dx = -b / dirLen;
        double dy = a / dirLen;
        return Value.PointV.of2D(footX + dx * half, footY + dy * half);
    }

    private static Value.LineV radicalLine(Value.CircleV c1, Value.CircleV c2) {
        double x1 = c1.center().x();
        double y1 = c1.center().y();
        double x2 = c2.center().x();
        double y2 = c2.center().y();
        // Subtracting the two circle equations cancels the quadratic terms.
        double a = 2 * (x2 - x1);
        double b = 2 * (y2 - y1);
        double c = (x2 * x2 - x1 * x1) + (y2 * y2 - y1 * y1) + (c1.r() * c1.r() - c2.r() * c2.r());
        if (Math.abs(a) < EPS && Math.abs(b) < EPS) {
            throw new EvalException("Die Kreise schneiden sich nicht.");
        }
        return new Value.LineV(a, b, c);
    }
}
