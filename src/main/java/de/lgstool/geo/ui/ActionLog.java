package de.lgstool.geo.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * The right-hand panel: a scrolling list of actions and results. Each entry shows the
 * originating element's colour dot, a one-line description and (for objects) its LaTeX;
 * clicking an entry copies its LaTeX to the clipboard.
 */
public final class ActionLog extends VBox {

    private final VBox entries = new VBox(2);

    private static final double PANEL_WIDTH = 320;

    public ActionLog() {
        setSpacing(6);
        setPadding(new Insets(10));
        // Fixed width so long results wrap onto multiple rows instead of widening the panel.
        setPrefWidth(PANEL_WIDTH);
        setMinWidth(PANEL_WIDTH);
        setMaxWidth(PANEL_WIDTH);
        setStyle("-fx-background-color: derive(" + Theme.BACKGROUND + ", 20%); -fx-border-color: derive("
                + Theme.ACCENT_SAGE + ", 10%); -fx-border-width: 0 0 0 1;");

        Label title = new Label(I18n.t("workspace.log"));
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.ACCENT_PETROL + ";");

        entries.setPadding(new Insets(2));
        ScrollPane scroll = new ScrollPane(entries);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.vvalueProperty().bind(entries.heightProperty());   // autoscroll to newest
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(title, scroll);
    }

    /** A neutral action / message line (errors, "solved", …) — copies its own text. */
    public void addAction(String text) {
        addRow(null, text, text);
    }

    /** An object entry: coloured dot + "name = value"; clicking copies the LaTeX. */
    public void addObject(String colorHex, String text, String latex) {
        addRow(colorHex, text, latex);
    }

    public void clear() {
        entries.getChildren().clear();
    }

    private void addRow(String colorHex, String text, String copyPayload) {
        Circle dot = new Circle(5);
        dot.setFill(colorHex == null ? Color.web(Theme.TEXT, 0.35) : Color.web(colorHex));

        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-text-fill: " + Theme.TEXT + "; -fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
        HBox.setHgrow(label, Priority.ALWAYS);

        HBox row = new HBox(7, dot, label);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(3, 4, 3, 4));
        Tooltip.install(row, new Tooltip(I18n.t("workspace.copyHint")));
        row.setOnMouseClicked(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(copyPayload);
            Clipboard.getSystemClipboard().setContent(content);
        });
        entries.getChildren().add(row);
    }
}
