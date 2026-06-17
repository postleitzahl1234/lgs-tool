package de.lgstool.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import de.lgstool.math.Fraction;
import de.lgstool.viz.Visualization;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Manual editor for the coordinate-system axis ranges (per axis min/max). */
public final class AxisDialog {

    private AxisDialog() {
    }

    public static void show(Window owner, Visualization viz, int width) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(I18n.t("axis.title"));
        stage.setAlwaysOnTop(true);

        double[] b = viz.bounds2D();
        TextField xMin = field(b[0]);
        TextField xMax = field(b[1]);
        TextField yMin = field(b[2]);
        TextField yMax = field(b[3]);
        TextField zMin = field(b[4]);
        TextField zMax = field(b[5]);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label(I18n.t("axis.min")), 1, 0);
        grid.add(new Label(I18n.t("axis.max")), 2, 0);
        grid.addRow(1, new Label("x"), xMin, xMax);
        if (width >= 2) {
            grid.addRow(2, new Label("y"), yMin, yMax);
        }
        if (width >= 3) {
            grid.addRow(3, new Label("z"), zMin, zMax);
        }

        Label title = new Label(I18n.t("axis.title"));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + Theme.ACCENT_PETROL + ";");
        Label hint = new Label(I18n.t("axis.hint"));
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: " + Theme.TEXT + ";");

        Button auto = new Button(I18n.t("common.auto"));
        auto.setOnAction(e -> {
            viz.setAxisRange(-1, 0, 0);
            stage.close();
        });
        Button apply = new Button(I18n.t("common.apply"));
        apply.setDefaultButton(true);
        apply.setStyle("-fx-background-color: " + Theme.ACCENT_PETROL + "; -fx-text-fill: white; -fx-font-weight: bold;");
        apply.setOnAction(e -> {
            applyAxis(viz, 0, xMin, xMax);
            if (width >= 2) {
                applyAxis(viz, 1, yMin, yMax);
            }
            if (width >= 3) {
                applyAxis(viz, 2, zMin, zMax);
            }
            stage.close();
        });
        Button cancel = new Button(I18n.t("common.cancel"));
        cancel.setOnAction(e -> stage.close());

        HBox buttons = new HBox(10, auto, apply, cancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(14, title, hint, grid, buttons);
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color: " + Theme.BACKGROUND + ";");
        stage.setScene(new Scene(root, 380, 320));
        stage.show();
    }

    private static void applyAxis(Visualization viz, int axisIndex, TextField minField, TextField maxField) {
        Double min = parse(minField.getText());
        Double max = parse(maxField.getText());
        if (min != null && max != null && max > min) {
            viz.setAxisRange(axisIndex, min, max);
        }
    }

    private static TextField field(double value) {
        TextField tf = new TextField(String.valueOf(Math.round(value * 1000.0) / 1000.0));
        tf.setPrefColumnCount(6);
        return tf;
    }

    private static Double parse(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Fraction.parse(t).doubleValue();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
