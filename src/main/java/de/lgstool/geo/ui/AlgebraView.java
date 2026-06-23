package de.lgstool.geo.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import de.lgstool.geo.command.GeoEngine;
import de.lgstool.geo.command.GeoFormat;
import de.lgstool.geo.expr.Value;
import de.lgstool.geo.model.GeoObject;
import de.lgstool.geo.model.Palette;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * The algebra list: one row per construction object showing its colour dot, name and
 * value, with a visibility toggle and delete. Double-click the name to rename, the value
 * to redefine. Matrices render as an inline grid whose rows are tinted shades of the
 * element colour (the matrix is still one element). Free sliders get a live control.
 */
public final class AlgebraView extends VBox {

    private final GeoEngine engine;
    private final Runnable onVisibilityChanged;
    private final Map<String, Label> valueLabels = new HashMap<>();
    private boolean rebuilding;

    public AlgebraView(GeoEngine engine, Runnable onVisibilityChanged) {
        this.engine = engine;
        this.onVisibilityChanged = onVisibilityChanged;
        setSpacing(4);
        setPadding(new Insets(6));
        rebuild();
    }

    public void rebuild() {
        if (rebuilding) {
            return;
        }
        rebuilding = true;
        try {
            getChildren().clear();
            valueLabels.clear();
            List<GeoObject> objs = engine.construction().objects();
            if (objs.isEmpty()) {
                Label empty = new Label(I18n.t("graph.noObjects"));
                empty.setStyle("-fx-text-fill: " + Theme.TEXT + "; -fx-padding: 8;");
                getChildren().add(empty);
                return;
            }
            for (GeoObject o : objs) {
                getChildren().add(row(o));
            }
        } finally {
            rebuilding = false;
        }
    }

    public void updateValues() {
        for (Map.Entry<String, Label> e : valueLabels.entrySet()) {
            GeoObject o = engine.construction().get(e.getKey());
            if (o != null) {
                e.getValue().setText(display(o));
            }
        }
    }

    /** The value text for an object, or a localized "undefined" marker when it failed to evaluate. */
    private static String display(GeoObject o) {
        return o.isDefined() ? GeoFormat.value(o.value()) : I18n.t("geo.undefined");
    }

    private VBox row(GeoObject o) {
        String colorHex = o.colorHex() == null ? Theme.ACCENT_PETROL : o.colorHex();

        CheckBox visible = new CheckBox();
        visible.setSelected(o.isVisible());
        visible.setOnAction(e -> {
            o.setVisible(visible.isSelected());
            onVisibilityChanged.run();
        });

        Circle dot = new Circle(6, Color.web(colorHex));

        Label name = new Label(o.name());
        name.setStyle("-fx-text-fill: " + Theme.TEXT + "; -fx-font-family: 'Consolas', monospace; -fx-font-weight: bold;");
        Tooltip.install(name, new Tooltip(I18n.t("workspace.renameHint")));

        Label eq = new Label("=");
        eq.setStyle("-fx-text-fill: " + Theme.TEXT + ";");

        Label value = new Label(display(o));
        value.setStyle("-fx-text-fill: " + Theme.TEXT + "; -fx-font-family: 'Consolas', monospace;");
        value.setWrapText(true);
        value.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(value, Priority.ALWAYS);
        Tooltip.install(value, new Tooltip(I18n.t("graph.editHint")));
        valueLabels.put(o.name(), value);

        Button delete = new Button("×");
        delete.setStyle("-fx-text-fill: #B23A48; -fx-font-weight: bold;");
        delete.setOnAction(e -> engine.execute("Delete(" + o.name() + ")"));

        HBox header = new HBox(6, visible, dot, name, eq, value, delete);
        header.setAlignment(Pos.CENTER_LEFT);
        name.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                startRename(o, name, header);
            }
        });
        value.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                startRedefine(o, value, header);
            }
        });

        VBox cell = new VBox(2, header);
        if (o.value() instanceof Value.MatrixV m) {
            cell.getChildren().add(matrixGrid(m, colorHex));
        }
        if (o.isSlider() && o.value() instanceof Value.NumberV n) {
            cell.getChildren().add(sliderFor(o, n.v()));
        }
        if (o.kind() == GeoObject.Kind.FREE && o.value() instanceof Value.BooleanV b) {
            cell.getChildren().add(checkboxFor(o, b.value()));
        }
        return cell;
    }

    /** An inline matrix grid; each row gets its own tint (a shade of the element colour). */
    private VBox matrixGrid(Value.MatrixV m, String baseHex) {
        VBox grid = new VBox(2);
        grid.setPadding(new Insets(3, 0, 4, 24));
        int rows = m.rowCount();
        for (int i = 0; i < rows; i++) {
            double deltaL = rows <= 1 ? 0 : (i - (rows - 1) / 2.0) * 0.10;
            Color tint = Color.web(Palette.shade(baseHex, deltaL), 0.22);
            HBox rowBox = new HBox(8);
            rowBox.setPadding(new Insets(2, 8, 2, 8));
            rowBox.setBackground(new Background(new BackgroundFill(tint, new CornerRadii(4), Insets.EMPTY)));
            for (int j = 0; j < m.colCount(); j++) {
                Label cell = new Label(m.rows()[i][j].toString());
                cell.setMinWidth(28);
                cell.setStyle("-fx-text-fill: " + Theme.TEXT + "; -fx-font-family: 'Consolas', monospace;");
                rowBox.getChildren().add(cell);
            }
            grid.getChildren().add(rowBox);
        }
        return grid;
    }

    private void startRename(GeoObject o, Label name, HBox header) {
        TextField editor = new TextField(o.name());
        editor.setPrefColumnCount(6);
        editor.setStyle("-fx-font-family: 'Consolas', monospace;");
        int idx = header.getChildren().indexOf(name);
        if (idx < 0) {
            return;
        }
        header.getChildren().set(idx, editor);
        editor.requestFocus();
        editor.selectAll();
        editor.setOnAction(e -> {
            String to = editor.getText().trim();
            if (!to.isEmpty() && !to.equals(o.name())) {
                engine.rename(o.name(), to);   // -> structureChanged -> rebuild()
            } else {
                rebuild();
            }
        });
        editor.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                rebuild();
            }
        });
        editor.focusedProperty().addListener((obs, was, is) -> {
            if (!is) {
                rebuild();
            }
        });
    }

    /** Replaces an object's value label with an inline editor for its definition. */
    private void startRedefine(GeoObject o, Label value, HBox header) {
        TextField editor = new TextField(engine.editableLine(o));
        editor.setStyle("-fx-font-family: 'Consolas', monospace;");
        HBox.setHgrow(editor, Priority.ALWAYS);
        int idx = header.getChildren().indexOf(value);
        if (idx < 0) {
            return;
        }
        header.getChildren().set(idx, editor);
        editor.requestFocus();
        editor.selectAll();
        editor.setOnAction(e -> {
            String line = editor.getText().trim();
            if (!line.isEmpty()) {
                engine.execute(line);   // redefine -> structureChanged -> rebuild()
            } else {
                rebuild();
            }
        });
        editor.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                rebuild();
            }
        });
        editor.focusedProperty().addListener((obs, was, is) -> {
            if (!is) {
                rebuild();
            }
        });
    }

    private Node sliderFor(GeoObject o, double value) {
        Slider slider = new Slider(o.sliderMin(), o.sliderMax(), value);
        slider.setBlockIncrement(o.sliderStep());
        HBox.setHgrow(slider, Priority.ALWAYS);
        slider.setOnMousePressed(e -> engine.beginInteraction());
        slider.valueProperty().addListener(
                (obs, ov, nv) -> engine.setFreeValueLive(o.name(), new Value.NumberV(nv.doubleValue())));

        ToggleButton play = new ToggleButton("▶");
        play.setStyle("-fx-font-size: 10px; -fx-padding: 1 5 1 5;");
        AnimationTimer timer = animator(o, slider);
        play.setOnAction(e -> {
            if (play.isSelected()) {
                play.setText("⏸");
                timer.start();
            } else {
                play.setText("▶");
                timer.stop();
            }
        });

        HBox box = new HBox(6, slider, play);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 0, 2, 24));
        return box;
    }

    /** A slider animator that bounces the value between min and max while playing. */
    private AnimationTimer animator(GeoObject o, Slider slider) {
        return new AnimationTimer() {
            private long last;
            private int direction = 1;

            @Override
            public void handle(long now) {
                if (now - last < 30_000_000L) {   // ~33 fps
                    return;
                }
                last = now;
                double next = slider.getValue() + direction * o.sliderStep();
                if (next >= o.sliderMax()) {
                    next = o.sliderMax();
                    direction = -1;
                } else if (next <= o.sliderMin()) {
                    next = o.sliderMin();
                    direction = 1;
                }
                slider.setValue(next);   // listener pushes the live value into the engine
            }
        };
    }

    private CheckBox checkboxFor(GeoObject o, boolean value) {
        CheckBox box = new CheckBox(o.name());
        box.setSelected(value);
        box.setPadding(new Insets(0, 0, 2, 24));
        box.setStyle("-fx-text-fill: " + Theme.TEXT + ";");
        box.setOnAction(e -> {
            engine.beginInteraction();
            engine.setFreeValue(o.name(), new Value.BooleanV(box.isSelected()));
        });
        return box;
    }
}
