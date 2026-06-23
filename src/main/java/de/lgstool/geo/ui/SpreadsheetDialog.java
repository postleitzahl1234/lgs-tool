package de.lgstool.geo.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import de.lgstool.geo.command.GeoEngine;
import de.lgstool.geo.command.GeoFormat;
import de.lgstool.geo.model.GeoObject;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * A spreadsheet view over the same {@link GeoEngine}: each cell is an object named by its
 * address ({@code A1}, {@code B2}, …). Typing a value or a formula referencing other cells
 * ({@code =A1*2}) runs an assignment through the engine, so cells participate in the same
 * dependency graph as everything else and update live. A focused cell shows its formula;
 * an unfocused cell shows the computed value.
 */
public final class SpreadsheetDialog {

    private static final int COLS = 10;   // A … J
    private static final int ROWS = 20;   // 1 … 20

    private final GeoEngine engine;
    private final Map<String, TextField> cells = new LinkedHashMap<>();

    private SpreadsheetDialog(GeoEngine engine) {
        this.engine = engine;
    }

    public static void show(Window owner, GeoEngine engine) {
        SpreadsheetDialog dialog = new SpreadsheetDialog(engine);
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setTitle(I18n.t("graph.spreadsheet"));

        ScrollPane scroll = new ScrollPane(dialog.grid());
        scroll.setFitToWidth(false);
        scroll.setStyle("-fx-background: " + Theme.BACKGROUND + ";");

        Label hint = new Label(I18n.t("sheet.hint"));
        hint.setStyle("-fx-text-fill: " + Theme.TEXT + "; -fx-padding: 6;");

        VBox root = new VBox(hint, scroll);
        root.setStyle("-fx-background-color: " + Theme.BACKGROUND + ";");
        stage.setScene(new Scene(root, 720, 560));
        stage.show();
        dialog.refresh();
    }

    private GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setStyle("-fx-padding: 8;");

        for (int c = 0; c < COLS; c++) {
            grid.add(header(columnLetter(c)), c + 1, 0);
        }
        for (int r = 1; r <= ROWS; r++) {
            grid.add(header(Integer.toString(r)), 0, r);
            for (int c = 0; c < COLS; c++) {
                String name = columnLetter(c) + r;
                grid.add(cell(name), c + 1, r);
            }
        }
        return grid;
    }

    private Label header(String text) {
        Label label = new Label(text);
        label.setAlignment(Pos.CENTER);
        label.setPrefSize(64, 24);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT
                + "; -fx-background-color: derive(" + Theme.ACCENT_SAGE + ", 40%);");
        return label;
    }

    private TextField cell(String name) {
        TextField field = new TextField();
        field.setPrefWidth(64);
        field.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
        cells.put(name, field);

        // On focus: show the editable formula. On commit/blur: run it and show the value.
        field.focusedProperty().addListener((obs, was, now) -> {
            if (now) {
                field.setText(formulaOf(name));
            } else {
                commit(name, field);
            }
        });
        field.setOnAction(e -> {
            commit(name, field);
            field.getParent().requestFocus();
        });
        return field;
    }

    private void commit(String name, TextField field) {
        String text = field.getText().trim();
        if (text.startsWith("=")) {
            text = text.substring(1).trim();
        }
        if (text.isEmpty()) {
            if (engine.construction().contains(name)) {
                engine.execute("Delete(" + name + ")");
            }
        } else {
            engine.execute(name + " = " + text);
        }
        refresh();
    }

    /** The formula to edit for a cell: its source definition without the {@code name = } prefix. */
    private String formulaOf(String name) {
        GeoObject o = engine.construction().get(name);
        if (o == null) {
            return "";
        }
        String line = engine.editableLine(o);
        int eq = line.indexOf('=');
        return eq >= 0 ? line.substring(eq + 1).trim() : line;
    }

    /** Refreshes every cell to display the current computed value. */
    private void refresh() {
        for (Map.Entry<String, TextField> e : cells.entrySet()) {
            if (e.getValue().isFocused()) {
                continue;
            }
            GeoObject o = engine.construction().get(e.getKey());
            if (o == null) {
                e.getValue().setText("");
            } else {
                e.getValue().setText(o.isDefined() ? GeoFormat.value(o.value()) : I18n.t("geo.undefined"));
            }
        }
    }

    private static String columnLetter(int index) {
        return String.valueOf((char) ('A' + index));
    }
}
