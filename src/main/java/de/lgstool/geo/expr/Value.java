package de.lgstool.geo.expr;

import java.util.List;

/**
 * A runtime value produced by the {@link Evaluator}. Geometry uses {@code double}
 * arithmetic (unlike the exact-fraction LGS engine). Points and vectors carry an
 * {@code is3D} flag so the 2D and 3D views can decide what to render.
 */
public sealed interface Value {

    record NumberV(double v) implements Value {
    }

    record PointV(double x, double y, double z, boolean is3D) implements Value {
        public static PointV of2D(double x, double y) {
            return new PointV(x, y, 0, false);
        }

        public static PointV of3D(double x, double y, double z) {
            return new PointV(x, y, z, true);
        }
    }

    record VectorV(double x, double y, double z, boolean is3D) implements Value {
    }

    /** Infinite 2D line {@code a·x + b·y = c}. */
    record LineV(double a, double b, double c) implements Value {
    }

    record SegmentV(PointV p, PointV q) implements Value {
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
}
