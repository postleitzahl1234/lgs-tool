package de.lgstool.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/** A small form for creating a new system without typing the {@code create} command. */
public final class NewLgsDialog {

    private NewLgsDialog() {
    }

    public static void show(Stage owner, AppController controller) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setAlwaysOnTop(true);
        stage.setTitle(I18n.t("new.title"));

        TextField name = new TextField(controller.nextDefaultName());
        Spinner<Integer> width = new Spinner<>(1, 100, 3);
        Spinner<Integer> height = new Spinner<>(1, 100, 3);
        width.setEditable(true);
        height.setEditable(true);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label(I18n.t("new.name")), name);
        form.addRow(1, new Label(I18n.t("new.width")), width);
        form.addRow(2, new Label(I18n.t("new.height")), height);

        Label title = new Label(I18n.t("new.title"));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + Theme.ACCENT_PETROL + ";");

        Button ok = new Button(I18n.t("common.create"));
        ok.setStyle("-fx-background-color: " + Theme.ACCENT_PETROL + "; -fx-text-fill: white; -fx-font-weight: bold;");
        ok.setDefaultButton(true);
        Button cancel = new Button(I18n.t("common.cancel"));
        cancel.setOnAction(e -> stage.close());
        ok.setOnAction(e -> {
            String n = name.getText().trim();
            if (!n.isEmpty()) {
                controller.runMain("create(" + n + ", " + width.getValue() + "*" + height.getValue() + ");");
                controller.open(n);
            }
            stage.close();
        });

        HBox buttons = new HBox(10, ok, cancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(16, title, form, buttons);
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color: " + Theme.BACKGROUND + ";");

        stage.setScene(new Scene(root, 380, 280));
        stage.show();
    }
}
