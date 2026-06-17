package de.lgstool.ui;

import de.lgstool.config.Theme;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Read-only, monospaced output area for command results and the solution log. */
public final class LogPane extends VBox {

    private final TextArea area = new TextArea();

    public LogPane(String header) {
        setSpacing(6);
        setPadding(new Insets(10));
        setStyle("-fx-background-color: " + Theme.BACKGROUND + ";");

        Label title = new Label(header);
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.ACCENT_PETROL + ";");

        area.setEditable(false);
        area.setWrapText(false);
        area.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px;");
        VBox.setVgrow(area, Priority.ALWAYS);

        getChildren().addAll(title, area);
    }

    public void append(List<String> lines) {
        for (String line : lines) {
            area.appendText(line + "\n");
        }
        area.appendText("\n");
        area.setScrollTop(Double.MAX_VALUE);
    }

    public void appendLine(String line) {
        area.appendText(line + "\n");
    }

    public void clear() {
        area.clear();
    }

    public String getText() {
        return area.getText();
    }
}
