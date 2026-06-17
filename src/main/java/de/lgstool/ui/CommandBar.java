package de.lgstool.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** A labelled command input line. Enter or the button submits the typed command. */
public final class CommandBar extends HBox {

    /** Receives a submitted command line. */
    public interface Handler {
        void run(String line);
    }

    private final TextField input = new TextField();
    private final List<String> history = new ArrayList<>();
    private int historyIndex;

    public CommandBar(String prompt, Handler handler) {
        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(10));
        setStyle("-fx-background-color: derive(" + Theme.ACCENT_SAGE + ", 60%);");

        Label label = new Label(prompt);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.ACCENT_PETROL + ";");

        input.setPromptText(I18n.t("cmd.placeholder"));
        input.setStyle("-fx-font-family: 'Consolas', monospace;");
        HBox.setHgrow(input, Priority.ALWAYS);

        Button go = new Button(I18n.t("cmd.run"));
        go.setStyle("-fx-background-color: " + Theme.ACCENT_PETROL + "; -fx-text-fill: white; -fx-font-weight: bold;");

        Runnable exec = () -> {
            String text = input.getText().trim();
            if (!text.isEmpty()) {
                history.add(text);
                historyIndex = history.size();
                handler.run(text);
                input.clear();
            }
        };
        go.setOnAction(e -> exec.run());
        input.setOnAction(e -> exec.run());
        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.UP) {
                recall(-1);
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN) {
                recall(1);
                e.consume();
            }
        });

        getChildren().addAll(label, input, go);
    }

    private void recall(int delta) {
        if (history.isEmpty()) {
            return;
        }
        historyIndex = Math.max(0, Math.min(history.size(), historyIndex + delta));
        if (historyIndex >= history.size()) {
            input.clear();
        } else {
            input.setText(history.get(historyIndex));
            input.positionCaret(input.getText().length());
        }
    }

    public void focusInput() {
        input.requestFocus();
    }
}
