package de.lgstool.geo.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Settings;
import de.lgstool.config.Theme;
import de.lgstool.geo.command.GeoEngine;
import de.lgstool.geo.command.GeoFormat;
import de.lgstool.geo.expr.Value;
import de.lgstool.geo.model.GeoObject;
import de.lgstool.geo.model.Palette;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
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
    private final Set<String> expanded = new HashSet<>();   // matrices/LGS whose table is open
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
        if (!o.isDefined()) {
            return I18n.t("geo.undefined");
        }
        // Functions show their formula (the body) rather than the opaque word "Funktion",
        // so it is visible and obvious that double-clicking edits it.
        if (o.value() instanceof Value.FunctionV) {
            String src = o.source();
            int eq = src.indexOf('=');
            return eq >= 0 ? src.substring(eq + 1).trim() : src;
        }
        if (o.value() instanceof Value.MatrixV m) {
            return matrixSummary(o, m);
        }
        return GeoFormat.value(o.value());
    }

    /** A one-line summary for a (collapsed) matrix/LGS row. */
    private static String matrixSummary(GeoObject o, Value.MatrixV m) {
        if (o.isLinearSystem()) {
            return "LGS " + m.rowCount() + "×" + (m.colCount() - 1) + " (+b)";
        }
        return "Matrix " + m.rowCount() + "×" + m.colCount();
    }

    private void toggleExpanded(String name) {
        if (!expanded.remove(name)) {
            expanded.add(name);
        }
        rebuild();
    }

    /**
     * A square, pannable window holding the editable matrix table; drag to move around
     * (like the coordinate system) and scroll for large systems. Sized to the content but
     * capped square so a 100×100 system stays a fixed square with scrollbars.
     */
    private ScrollPane matrixWindow(GeoObject o, Value.MatrixV m, String colorHex) {
        ScrollPane sp = new ScrollPane(matrixGrid(o, m, colorHex));
        sp.setPannable(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        double contentW = m.colCount() * 48 + 22;
        double contentH = m.rowCount() * 32 + 16;
        double side = Math.min(Math.max(contentW, contentH), 300);   // square, capped at 300px
        sp.setPrefViewportWidth(side);
        sp.setPrefViewportHeight(side);
        sp.setMaxWidth(side + 6);
        sp.setStyle("-fx-background: derive(" + Theme.BACKGROUND + ", 12%);"
                + " -fx-background-color: derive(" + Theme.BACKGROUND + ", 12%);"
                + " -fx-border-color: derive(" + Theme.ACCENT_SAGE + ", -5%); -fx-border-width: 1;"
                + " -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setMargin(sp, new Insets(2, 0, 6, 18));
        return sp;
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
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                startRedefine(o, value, header);
            }
        });

        VBox cell = new VBox(2, header);
        if (o.value() instanceof Value.MatrixV m) {
            // A matrix/LGS collapses behind an arrow; expanded it opens a square, pannable
            // table (move around like the coordinate system) where every cell is editable.
            boolean open = expanded.contains(o.name());
            Button arrow = new Button(open ? "▾" : "▸");
            arrow.setStyle("-fx-background-color: transparent; -fx-text-fill: " + Theme.ACCENT_PETROL
                    + "; -fx-font-weight: bold; -fx-padding: 0 4 0 0;");
            arrow.setOnAction(e -> toggleExpanded(o.name()));
            header.getChildren().add(0, arrow);
            if (open) {
                cell.getChildren().add(matrixWindow(o, m, colorHex));
            }
        }
        if (o.isSlider() && o.value() instanceof Value.NumberV n) {
            cell.getChildren().add(sliderFor(o, n.v()));
        }
        if (o.kind() == GeoObject.Kind.FREE && o.value() instanceof Value.BooleanV b) {
            cell.getChildren().add(checkboxFor(o, b.value()));
        }
        return cell;
    }

    /**
     * An inline matrix grid; each row gets its own tint. Cells are editable: typing a new
     * entry and committing (Enter or focus loss) redefines the whole matrix object, so the
     * change participates in the dependency graph like any other edit.
     */
    private VBox matrixGrid(GeoObject o, Value.MatrixV m, String baseHex) {
        VBox grid = new VBox(2);
        grid.setPadding(new Insets(3, 2, 4, 6));
        int rows = m.rowCount();
        for (int i = 0; i < rows; i++) {
            double deltaL = rows <= 1 ? 0 : (i - (rows - 1) / 2.0) * 0.10;
            Color tint = Color.web(Palette.shade(baseHex, deltaL), 0.22);
            HBox rowBox = new HBox(6);
            rowBox.setPadding(new Insets(2, 8, 2, 8));
            rowBox.setBackground(new Background(new BackgroundFill(tint, new CornerRadii(4), Insets.EMPTY)));
            for (int j = 0; j < m.colCount(); j++) {
                rowBox.getChildren().add(matrixCell(o, i, j, GeoFormat.matrixEntryText(o.source(), m, i, j)));
            }
            grid.getChildren().add(rowBox);
        }
        return grid;
    }

    private static final String CELL_IDLE = "-fx-text-fill: " + Theme.TEXT
            + "; -fx-font-family: 'Consolas', monospace; -fx-background-color: rgba(255,255,255,0.5);"
            + " -fx-border-color: derive(" + Theme.ACCENT_SAGE + ", -10%); -fx-border-width: 1;"
            + " -fx-border-radius: 3; -fx-background-radius: 3; -fx-padding: 1 3 1 3;";
    private static final String CELL_FOCUS = CELL_IDLE
            + "-fx-border-color: " + Theme.ACCENT_PETROL + "; -fx-border-width: 1.5;";

    private TextField matrixCell(GeoObject o, int row, int col, String text) {
        TextField field = new TextField(text);
        field.setPrefColumnCount(3);
        field.setMinWidth(42);
        field.setStyle(CELL_IDLE);
        Tooltip.install(field, new Tooltip(I18n.t("graph.editHint")));
        field.setOnAction(e -> commitMatrixCell(o, row, col, field.getText()));
        field.focusedProperty().addListener((obs, was, now) -> {
            field.setStyle(now ? CELL_FOCUS : CELL_IDLE);
            if (now) {
                field.selectAll();
            } else {
                commitMatrixCell(o, row, col, field.getText());
            }
        });
        return field;
    }

    /** Rebuilds the matrix literal with one entry changed and redefines the object. */
    private void commitMatrixCell(GeoObject o, int row, int col, String entry) {
        GeoObject current = engine.construction().get(o.name());
        if (current == null || !(current.value() instanceof Value.MatrixV m)) {
            return;
        }
        String currentEntry = GeoFormat.matrixEntryText(current.source(), m, row, col).trim();
        String nextEntry = entry == null || entry.isBlank() ? "0" : entry.trim();
        if (currentEntry.equals(nextEntry)) {
            return;
        }
        engine.editMatrixCell(o.name(), row, col, entry);
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
                long intervalNanos = Math.max(1_000_000L, (long) (Settings.sliderSecondsPerStep * 1_000_000_000L));
                if (now - last < intervalNanos) {
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
