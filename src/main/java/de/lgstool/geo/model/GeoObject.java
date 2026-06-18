package de.lgstool.geo.model;

import de.lgstool.geo.expr.Expr;
import de.lgstool.geo.expr.Value;
import java.util.List;

/**
 * One named object in a {@link Construction}: its definition (source text + parsed
 * {@link Expr}), the names it depends on, and its cached {@link Value}.
 *
 * <p>{@link Kind#FREE} objects hold their value directly (a draggable point or a
 * slider number); {@link Kind#DEPENDENT} objects are re-evaluated from their
 * definition when an input changes; {@link Kind#FUNCTION} objects are
 * {@code f(x)=…} definitions whose value is a {@link Value.FunctionV}.
 */
public final class GeoObject {

    public enum Kind {
        FREE,
        DEPENDENT,
        FUNCTION
    }

    private final String name;
    private final Kind kind;
    private final Expr definition;
    private final List<String> dependencies;
    private final List<String> params;

    private String source;
    private Value value;
    private boolean visible = true;
    private String colorHex;

    private boolean slider;
    private double sliderMin;
    private double sliderMax;
    private double sliderStep;

    public GeoObject(String name, Kind kind, Expr definition, List<String> dependencies,
            List<String> params, String source, Value value) {
        this.name = name;
        this.kind = kind;
        this.definition = definition;
        this.dependencies = List.copyOf(dependencies);
        this.params = params == null ? List.of() : List.copyOf(params);
        this.source = source;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public Kind kind() {
        return kind;
    }

    public Expr definition() {
        return definition;
    }

    public List<String> dependencies() {
        return dependencies;
    }

    public List<String> params() {
        return params;
    }

    public String source() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Value value() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public GeoType type() {
        return GeoType.of(value);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String colorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public boolean isDraggable() {
        return kind == Kind.FREE && value instanceof Value.PointV;
    }

    public boolean isSlider() {
        return slider;
    }

    public double sliderMin() {
        return sliderMin;
    }

    public double sliderMax() {
        return sliderMax;
    }

    public double sliderStep() {
        return sliderStep;
    }

    public void configureSlider(double min, double max, double step) {
        this.slider = true;
        this.sliderMin = min;
        this.sliderMax = max;
        this.sliderStep = step;
    }

    /** Deep copy for the undo system (values and AST are immutable, so shared). */
    public GeoObject copy() {
        GeoObject c = new GeoObject(name, kind, definition, dependencies, params, source, value);
        c.visible = visible;
        c.colorHex = colorHex;
        c.slider = slider;
        c.sliderMin = sliderMin;
        c.sliderMax = sliderMax;
        c.sliderStep = sliderStep;
        return c;
    }
}
