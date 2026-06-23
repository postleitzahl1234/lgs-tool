package de.lgstool.geo.model;

import de.lgstool.geo.expr.Value;

/** The displayable kind of a {@link GeoObject}, derived from its current {@link Value}. */
public enum GeoType {
    NUMBER,
    BOOLEAN,
    COMPLEX,
    POINT,
    VECTOR,
    LINE,
    SEGMENT,
    CIRCLE,
    FUNCTION,
    PLANE,
    SPHERE,
    POLYGON,
    CONIC,
    LOCUS,
    MATRIX,
    LIST,
    TEXT,
    IMPLICIT;

    public static GeoType of(Value v) {
        return switch (v) {
            case Value.NumberV ignored -> NUMBER;
            case Value.BooleanV ignored -> BOOLEAN;
            case Value.ComplexV ignored -> COMPLEX;
            case Value.PointV ignored -> POINT;
            case Value.VectorV ignored -> VECTOR;
            case Value.LineV ignored -> LINE;
            case Value.Line3DV ignored -> LINE;
            case Value.SegmentV ignored -> SEGMENT;
            case Value.CircleV ignored -> CIRCLE;
            case Value.FunctionV ignored -> FUNCTION;
            case Value.PlaneV ignored -> PLANE;
            case Value.SphereV ignored -> SPHERE;
            case Value.PolygonV ignored -> POLYGON;
            case Value.ConicV ignored -> CONIC;
            case Value.LocusV ignored -> LOCUS;
            case Value.MatrixV ignored -> MATRIX;
            case Value.ListV ignored -> LIST;
            case Value.TextV ignored -> TEXT;
            case Value.ImplicitV ignored -> IMPLICIT;
        };
    }
}
