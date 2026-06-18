package de.lgstool.geo.command;

import de.lgstool.geo.expr.EvalException;
import de.lgstool.geo.expr.Value;

/**
 * Builds general conic coefficients {@code A·x²+B·xy+C·y²+D·x+E·y+F=0} from geometric
 * definitions: {@code Ellipse(F1,F2,a)}, {@code Hyperbola(F1,F2,a)} and
 * {@code Parabola(F, directrix)}. The plane renders the result via marching squares.
 */
final class GeoConics {

    private GeoConics() {
    }

    static Value.ConicV ellipse(Value.PointV f1, Value.PointV f2, double a) {
        return central(f1, f2, a, true);
    }

    static Value.ConicV hyperbola(Value.PointV f1, Value.PointV f2, double a) {
        return central(f1, f2, a, false);
    }

    /** Conic of points equidistant from a focus and a directrix line. */
    static Value.ConicV parabola(Value.PointV focus, Value.LineV directrix) {
        double a = directrix.a();
        double b = directrix.b();
        double c = directrix.c();
        double n2 = a * a + b * b;
        if (n2 < 1e-12) {
            throw new EvalException("Ungültige Leitgerade.");
        }
        double fx = focus.x();
        double fy = focus.y();
        return new Value.ConicV(
                n2 - a * a,
                -2 * a * b,
                n2 - b * b,
                -2 * n2 * fx + 2 * a * c,
                -2 * n2 * fy + 2 * b * c,
                n2 * (fx * fx + fy * fy) - c * c);
    }

    private static Value.ConicV central(Value.PointV f1, Value.PointV f2, double a, boolean ellipse) {
        double h = (f1.x() + f2.x()) / 2;
        double k = (f1.y() + f2.y()) / 2;
        double c = Math.hypot(f2.x() - f1.x(), f2.y() - f1.y()) / 2;
        double theta = Math.atan2(f2.y() - f1.y(), f2.x() - f1.x());
        double a2 = a * a;
        double p = 1 / a2;
        double q;
        if (ellipse) {
            double b2 = a2 - c * c;
            if (b2 <= 0) {
                throw new EvalException("Ellipse: a muss größer als der halbe Brennpunktabstand sein.");
            }
            q = 1 / b2;
        } else {
            double b2 = c * c - a2;
            if (b2 <= 0) {
                throw new EvalException("Hyperbel: a muss kleiner als der halbe Brennpunktabstand sein.");
            }
            q = -1 / b2;
        }
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        double m11 = p * cos * cos + q * sin * sin;
        double m22 = p * sin * sin + q * cos * cos;
        double m12 = (p - q) * sin * cos;
        return new Value.ConicV(
                m11,
                2 * m12,
                m22,
                -2 * m11 * h - 2 * m12 * k,
                -2 * m12 * h - 2 * m22 * k,
                m11 * h * h + 2 * m12 * h * k + m22 * k * k - 1);
    }
}
