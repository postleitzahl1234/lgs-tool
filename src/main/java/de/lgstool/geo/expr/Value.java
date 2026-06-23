package de.lgstool.geo.expr;

import de.lgstool.math.Fraction;
import java.util.List;

/**
 * A runtime value produced by the {@link Evaluator}. Geometry uses {@code double}
 * arithmetic (unlike the exact-fraction LGS engine). Points and vectors carry an
 * {@code is3D} flag so the 2D and 3D views can decide what to render.
 */
public sealed interface Value {

    record NumberV(double v) implements Value {
    }

    /** A boolean truth value (from a comparison/logical expression or a checkbox object). */
    record BooleanV(boolean value) implements Value {
    }

    /** A complex number {@code re + im·i}. */
    record ComplexV(double re, double im) implements Value {
    }

    record PointV(double x, double y, double z, boolean is3D) implements Value {
        public static PointV of2D(double x, double y) {
            return new PointV(x, y, 0, false);
        }

        public static PointV of3D(double x, double y, double z) {
            return new PointV(x, y, z, true);
        }
    }

    /** A vector; {@code base} is the optional tail point (null = drawn from the origin). */
    record VectorV(double x, double y, double z, boolean is3D, PointV base) implements Value {
        public VectorV(double x, double y, double z, boolean is3D) {
            this(x, y, z, is3D, null);
        }
    }

    /** Infinite 2D line {@code a·x + b·y = c}. */
    record LineV(double a, double b, double c) implements Value {
    }

    record SegmentV(PointV p, PointV q) implements Value {
    }

    /** A polygon given by its ordered vertices. */
    record PolygonV(List<PointV> vertices) implements Value {
    }

    /** A general 2D conic {@code A·x² + B·xy + C·y² + D·x + E·y + F = 0}. */
    record ConicV(double a, double b, double c, double d, double e, double f) implements Value {
    }

    /** A locus: the sampled path of a dependent point as a parameter varies. */
    record LocusV(List<PointV> points) implements Value {
    }

    /** 2D circle with the given centre and radius. */
    record CircleV(PointV center, double r) implements Value {
    }

    /** A user-defined function {@code f(params...) = body}. */
    record FunctionV(List<String> params, Expr body) implements Value {
    }

    /** 3D plane {@code a·x + b·y + c·z = d}. */
    record PlaneV(double a, double b, double c, double d) implements Value {
    }

    record SphereV(PointV center, double r) implements Value {
    }

    /** 3D line through {@code point} with direction {@code dir}. */
    record Line3DV(PointV point, VectorV dir) implements Value {
    }

    /** An exact rational matrix (rows × cols), shared with the LGS engine's {@link Fraction} maths. */
    record MatrixV(Fraction[][] rows) implements Value {
        public int rowCount() {
            return rows.length;
        }

        public int colCount() {
            return rows.length == 0 ? 0 : rows[0].length;
        }
    }

    /** An ordered list of values, e.g. {@code {1, 2, 3}} or a list of points. */
    record ListV(List<Value> items) implements Value {
    }

    /** A text/label; {@code anchor} is the world position to draw it at (null = list-only). */
    record TextV(String text, PointV anchor) implements Value {
    }

    /**
     * An implicit curve {@code expr = 0} in the plotting variables {@code x} and {@code y}
     * (e.g. {@code x^2 + y^2 - 1}). {@code label} keeps the original {@code LHS = RHS}
     * input for display and replay. Rendered by evaluating {@code expr} over a grid.
     */
    record ImplicitV(Expr expr, String label) implements Value {
    }
}
