package de.lgstool.geo.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import de.lgstool.geo.command.GeoEngine;
import de.lgstool.geo.command.GeoFormat;
import de.lgstool.geo.expr.Value;
import de.lgstool.geo.model.GeoObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 * The algebra list: one row per construction object showing its colour, name and
 * value, with a visibility toggle and a delete button. Free slider numbers get a live
 * {@link Slider}. {@link #rebuild()} re-creates rows after structural changes;
 * {@link #updateValues()} only refreshes value labels during a drag (so slider controls
 * are not torn down mid-interaction).
 */
public final class AlgebraView extends VBox {

    private final GeoEngine engine;
    private final Runnable onVisibilityChanged;
    private final Map<String, Label> valueLabels = new HashMap<>();

    public AlgebraView(GeoEngine engine, Runnable onVisibilityChanged) {
        this.engine = engine;
        this.onVisibilityChanged = onVisibilityChanged;
        setSpacing(4);
        setPadding(new Insets(6));
        rebuild();
    }

    public void rebuild() {
        getChildren().clear();
        valueLabels.clear();
        List<GeoObject> objs = engine.construction().objects();
        if (objs.isEmpty()) {
            Label empty = new Label(I18n.t("graph.noObjects"));
            empty.setStyle("-fx-text-fill: " + Theme.TEXT + "; -fx-padding: 8;");
            getChildren().add(empty);
            return;
        }
        for (int i = 0; i < objs.size(); i++) {
            getChildren().add(row(objs.get(i), i));
        }
    }

    public void updateValues() {
        for (Map.Entry<String, Label> e : valueLabels.entrySet()) {
            GeoObject o = engine.construction().get(e.getKey());
            if (o != null) {
                e.getValue().setText(o.name() + " = " + GeoFormat.value(o.value()));
            }
        }
    }

    private VBox row(GeoObject o, int index) {
        CheckBox visible = new CheckBox();
        visible.setSelected(o.isVisible());
        visible.setOnAction(e -> {
            o.setVisible(visible.isSelected());
            onVisibilityChanged.run();
        });

        Rectangle swatch = new Rectangle(12, 12, GeoPalette.color(o, index));
        swatch.setArcWidth(3);
        swatch.setArcHeight(3);

        Label text = new Label(o.name() + " = " + GeoFormat.value(o.value()));
        text.setStyle("-fx-text-fill: " + Theme.TEXT + "; -fx-font-family: 'Consolas', monospace;");
        Tooltip.install(text, new Tooltip(o.source()));
        valueLabels.put(o.name(), text);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button delete = new Button("×");
        delete.setStyle("-fx-text-fill: #B23A48; -fx-font-weight: bold;");
        delete.setOnAction(e -> engine.execute("Delete(" + o.name() + ")"));

        HBox header = new HBox(6, visible, swatch, text, spacer, delete);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox cell = new VBox(2, header);
        if (o.isSlider() && o.value() instanceof Value.NumberV n) {
            cell.getChildren().add(sliderFor(o, n.v()));
        }
        return cell;
    }

    private Slider sliderFor(GeoObject o, double value) {
        Slider slider = new Slider(o.sliderMin(), o.sliderMax(), value);
        slider.setBlockIncrement(o.sliderStep());
        slider.setOnMousePressed(e -> engine.beginInteraction());
        slider.valueProperty().addListener(
                (obs, ov, nv) -> engine.setFreeValueLive(o.name(), new Value.NumberV(nv.doubleValue())));
        return slider;
    }
}
