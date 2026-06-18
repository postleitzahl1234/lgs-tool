package de.lgstool.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import de.lgstool.viz.Visualization;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** The main screen: system buttons + [HAUPT] command line on the left, the coordinate system on the right. */
public final class MainScreen {

    private final AppController controller;
    private final BorderPane root = new BorderPane();
    private final VBox buttonBox = new VBox(8);
    private final LogPane statusLog = new LogPane(I18n.t("main.messages"));
    private final Visualization viz = new Visualization();

    public MainScreen(AppController controller) {
        this.controller = controller;
        root.setStyle("-fx-background-color: " + Theme.BACKGROUND + ";");
        root.setLeft(buildLeftPanel());
        root.setCenter(buildVisualization());
    }

    private VBox buildLeftPanel() {
        Label title = new Label(I18n.t("app.title"));
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + Theme.ACCENT_PETROL + ";");

        Label sysLabel = new Label(I18n.t("main.systems"));
        sysLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT + ";");

        Button newLgs = new Button(I18n.t("main.newLgs"));
        newLgs.setMaxWidth(Double.MAX_VALUE);
        newLgs.setStyle("-fx-background-color: " + Theme.ACCENT_PETROL + "; -fx-text-fill: white; -fx-font-weight: bold;");
        newLgs.setOnAction(e -> controller.newLgsDialog());

        Button newGraph = new Button(I18n.t("main.newGraph"));
        newGraph.setMaxWidth(Double.MAX_VALUE);
        newGraph.setStyle("-fx-background-color: " + Theme.ACCENT_SAGE + "; -fx-text-fill: " + Theme.TEXT + "; -fx-font-weight: bold;");
        newGraph.setOnAction(e -> controller.openGraph());

        buttonBox.setPadding(new Insets(4));
        ScrollPane scroll = new ScrollPane(buttonBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + Theme.BACKGROUND + "; -fx-background-color: " + Theme.BACKGROUND + ";");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button settings = new Button(I18n.t("main.settings"));
        settings.setOnAction(e -> controller.openSettings());
        Button glossary = new Button(I18n.t("main.glossary"));
        glossary.setOnAction(e -> controller.openGlossary());
        HBox options = new HBox(8, settings, glossary);

        CommandBar mainBar = new CommandBar("[HAUPT]", controller::runMain);

        statusLog.setPrefHeight(200);
        statusLog.setMinHeight(140);

        VBox left = new VBox(12, title, sysLabel, newLgs, newGraph, scroll, new Separator(), options, mainBar, statusLog);
        left.setPadding(new Insets(16));
        left.setPrefWidth(360);
        left.setStyle("-fx-background-color: derive(" + Theme.BACKGROUND + ", 30%); -fx-border-color: derive("
                + Theme.ACCENT_SAGE + ", 10%); -fx-border-width: 0 1 0 0;");
        return left;
    }

    private StackPane buildVisualization() {
        Button zoomIn = controlButton("+");
        zoomIn.setOnAction(e -> viz.zoom(1.2));
        Button zoomOut = controlButton("−");
        zoomOut.setOnAction(e -> viz.zoom(1 / 1.2));
        Button fit = controlButton("Fit");
        fit.setOnAction(e -> viz.fit());
        Button stop = controlButton("⏹");
        stop.setOnAction(e -> viz.stopAnimation());
        Button axes = controlButton("⚙");
        axes.setOnAction(e -> controller.openAxes());
        Button labels = controlButton("#");
        labels.setOnAction(e -> viz.toggleLabels());

        VBox controls = new VBox(6, zoomIn, zoomOut, fit, stop, axes, labels);
        controls.setAlignment(Pos.TOP_RIGHT);
        controls.setPadding(new Insets(14));
        controls.setMaxWidth(Double.NEGATIVE_INFINITY);
        controls.setPickOnBounds(false);

        StackPane stack = new StackPane(viz, controls);
        StackPane.setAlignment(controls, Pos.TOP_RIGHT);
        return stack;
    }

    private Button controlButton(String label) {
        Button b = new Button(label);
        b.setMinWidth(44);
        b.setStyle("-fx-background-color: " + Theme.ACCENT_PETROL + "; -fx-text-fill: white; -fx-font-weight: bold;");
        return b;
    }

    public Parent getRoot() {
        return root;
    }

    public Visualization visualization() {
        return viz;
    }

    public void log(List<String> lines) {
        statusLog.append(lines);
    }

    public String statusLogText() {
        return statusLog.getText();
    }

    public void refreshButtons(List<String> names, String openName) {
        buttonBox.getChildren().clear();
        if (names.isEmpty()) {
            Label none = new Label(I18n.t("main.noSystems"));
            none.setStyle("-fx-text-fill: " + Theme.TEXT + ";");
            buttonBox.getChildren().add(none);
            return;
        }
        for (String name : names) {
            Button button = new Button(name);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            boolean open = name.equals(openName);
            button.setStyle("-fx-background-color: " + (open ? Theme.ACCENT_PETROL : "derive(" + Theme.ACCENT_SAGE + ", 40%)")
                    + "; -fx-text-fill: " + (open ? "white" : Theme.TEXT) + "; -fx-font-weight: bold;");
            button.setOnAction(e -> controller.open(name));
            HBox.setHgrow(button, Priority.ALWAYS);

            Button delete = new Button("×");
            delete.setStyle("-fx-text-fill: #B23A48; -fx-font-weight: bold;");
            delete.setOnAction(e -> confirmDelete(name));

            HBox row = new HBox(4, button, delete);
            buttonBox.getChildren().add(row);
        }
    }

    private void confirmDelete(String name) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, I18n.t("confirm.deleteText", name),
                ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(I18n.t("confirm.deleteTitle"));
        alert.setHeaderText(null);
        alert.initOwner(root.getScene() == null ? null : root.getScene().getWindow());
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                controller.runMain("del(" + name + ");");
            }
        });
    }
}
