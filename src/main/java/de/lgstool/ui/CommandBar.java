package de.lgstool.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Settings;
import de.lgstool.config.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Popup;

/**
 * A labelled command input line. Enter (or the button) submits; multiple commands can
 * be entered on one line separated by {@code ;}. When command suggestions are enabled
 * (see {@link Settings#suggestionsEnabled}) a popup shows matching commands as you
 * type: ↑/↓ moves the selection, Tab autocompletes (you still fill in arguments), Esc
 * hides it. When the popup is hidden, ↑/↓ recalls input history.
 */
public final class CommandBar extends HBox {

    /** Receives a submitted command line. */
    public interface Handler {
        void run(String line);
    }

    private static final Pattern TRAILING_IDENT = Pattern.compile("([A-Za-z][A-Za-z0-9_]*)$");
    private static final int MAX_VISIBLE = 8;
    private static final double ROW_HEIGHT = 26;

    private final TextField input = new TextField();
    private final List<String> history = new ArrayList<>();
    private int historyIndex;

    private final List<Suggestion> pool;
    private final Popup popup = new Popup();
    private final ListView<Suggestion> list = new ListView<>();

    public CommandBar(String prompt, Handler handler) {
        this(prompt, handler, List.of());
    }

    public CommandBar(String prompt, Handler handler, List<Suggestion> suggestions) {
        this.pool = List.copyOf(suggestions);
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

        configureSuggestions();

        go.setOnAction(e -> submit(handler));
        input.setOnAction(e -> submit(handler));
        input.textProperty().addListener((o, a, b) -> updateSuggestions());
        input.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);

        getChildren().addAll(label, input, go);
    }

    public void focusInput() {
        input.requestFocus();
    }

    // ---- submission ------------------------------------------------------

    private void submit(Handler handler) {
        String text = input.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        history.add(text);
        historyIndex = history.size();
        hideSuggestions();
        for (String part : text.split(";")) {
            String segment = part.trim();
            if (!segment.isEmpty()) {
                handler.run(segment);
            }
        }
        input.clear();
    }

    // ---- key handling ----------------------------------------------------

    private void onKeyPressed(KeyEvent e) {
        boolean showing = popup.isShowing();
        switch (e.getCode()) {
            case DOWN -> {
                if (showing) {
                    move(1);
                } else {
                    recall(1);
                }
                e.consume();
            }
            case UP -> {
                if (showing) {
                    move(-1);
                } else {
                    recall(-1);
                }
                e.consume();
            }
            case TAB -> {
                if (showing) {
                    acceptSelected();
                    e.consume();
                }
            }
            case ESCAPE -> {
                if (showing) {
                    hideSuggestions();
                    e.consume();
                }
            }
            default -> {
                // typing continues; the text listener refreshes the popup
            }
        }
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

    // ---- suggestions -----------------------------------------------------

    private void configureSuggestions() {
        list.setFocusTraversable(false);
        list.setStyle("-fx-font-family: 'Consolas', monospace;");
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Suggestion s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : s.signature());
            }
        });
        list.setOnMouseClicked(e -> acceptSelected());
        popup.setAutoHide(true);
        popup.setHideOnEscape(false);
        popup.getContent().add(list);
    }

    private void updateSuggestions() {
        if (!Settings.suggestionsEnabled || pool.isEmpty()) {
            hideSuggestions();
            return;
        }
        String prefix = currentPrefix();
        if (prefix.isEmpty()) {
            hideSuggestions();
            return;
        }
        String lower = prefix.toLowerCase();
        List<Suggestion> matches = new ArrayList<>();
        for (Suggestion s : pool) {
            if (s.name().toLowerCase().startsWith(lower)) {
                matches.add(s);
                if (matches.size() >= MAX_VISIBLE) {
                    break;
                }
            }
        }
        if (matches.isEmpty()) {
            hideSuggestions();
            return;
        }
        showSuggestions(matches);
    }

    private void showSuggestions(List<Suggestion> matches) {
        list.getItems().setAll(matches);
        list.getSelectionModel().select(0);
        double height = matches.size() * ROW_HEIGHT + 4;
        list.setPrefSize(Math.max(input.getWidth(), 240), height);
        Point2D top = input.localToScreen(0, 0);
        if (top == null) {
            return;
        }
        double x = top.getX();
        double y = top.getY() - height - 2;   // above the input bar (which sits at the window bottom)
        if (popup.isShowing()) {
            popup.setX(x);
            popup.setY(y);
        } else {
            popup.show(input, x, y);
        }
    }

    private void hideSuggestions() {
        if (popup.isShowing()) {
            popup.hide();
        }
    }

    private void move(int delta) {
        int n = list.getItems().size();
        if (n == 0) {
            return;
        }
        int i = Math.floorMod(list.getSelectionModel().getSelectedIndex() + delta, n);
        list.getSelectionModel().select(i);
        list.scrollTo(i);
    }

    private void acceptSelected() {
        Suggestion s = list.getSelectionModel().getSelectedItem();
        if (s == null) {
            return;
        }
        int caret = input.getCaretPosition();
        String text = input.getText();
        String before = text.substring(0, caret);
        Matcher m = TRAILING_IDENT.matcher(before);
        int start = m.find() ? m.start(1) : caret;
        String newBefore = before.substring(0, start) + s.insert();
        input.setText(newBefore + text.substring(caret));
        input.positionCaret(newBefore.length());
        hideSuggestions();
    }

    private String currentPrefix() {
        int caret = input.getCaretPosition();
        String before = input.getText().substring(0, Math.min(caret, input.getText().length()));
        Matcher m = TRAILING_IDENT.matcher(before);
        return m.find() ? m.group(1) : "";
    }
}
