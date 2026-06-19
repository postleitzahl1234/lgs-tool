package de.lgstool.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Settings;
import de.lgstool.config.Theme;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/** Global settings: language, fullscreen and the default display mode for new systems. */
public final class SettingsDialog {

    private SettingsDialog() {
    }

    public static void show(Stage primaryStage, AppController controller) {
        Stage stage = new Stage();
        stage.initOwner(primaryStage);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setAlwaysOnTop(true);
        stage.setTitle(I18n.t("settings.title"));

        Label title = new Label(I18n.t("settings.title"));
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + Theme.ACCENT_PETROL + ";");

        Label languageLabel = new Label(I18n.t("settings.language"));
        ComboBox<I18n.Language> language = new ComboBox<>(FXCollections.observableArrayList(I18n.languages()));
        language.setConverter(new StringConverter<>() {
            @Override
            public String toString(I18n.Language l) {
                return l == null ? "" : l.displayName();
            }

            @Override
            public I18n.Language fromString(String s) {
                return null;
            }
        });
        for (I18n.Language l : I18n.languages()) {
            if (l.code().equals(I18n.currentCode())) {
                language.getSelectionModel().select(l);
            }
        }
        language.setOnAction(e -> {
            I18n.Language sel = language.getValue();
            if (sel != null && !sel.code().equals(I18n.currentCode())) {
                stage.close();
                controller.changeLanguage(sel.code());
            }
        });

        CheckBox fullscreen = new CheckBox(I18n.t("settings.fullscreen"));
        fullscreen.setSelected(primaryStage.isFullScreen());
        fullscreen.selectedProperty().addListener((o, a, b) -> {
            primaryStage.setFullScreen(b);
            Settings.startFullscreen = b;
        });

        CheckBox darkMode = new CheckBox(I18n.t("settings.darkMode"));
        darkMode.setSelected(Theme.isDark());
        darkMode.setOnAction(e -> {
            stage.close();
            controller.changeTheme(darkMode.isSelected());
        });

        CheckBox suggestions = new CheckBox(I18n.t("settings.suggestions"));
        suggestions.setSelected(Settings.suggestionsEnabled);
        suggestions.selectedProperty().addListener((o, a, b) -> Settings.suggestionsEnabled = b);

        Spinner<Integer> precision = new Spinner<>(1, 15, Settings.decimalPrecision);
        precision.setMaxWidth(90);
        precision.valueProperty().addListener((o, a, b) -> Settings.decimalPrecision = b);
        HBox precisionRow = new HBox(8, new Label(I18n.t("settings.precision")), precision);

        Label displayLabel = new Label(I18n.t("settings.defaultDisplay"));
        ToggleGroup group = new ToggleGroup();
        RadioButton frac = new RadioButton(I18n.t("settings.fractions"));
        RadioButton dec = new RadioButton(I18n.t("settings.decimals"));
        frac.setToggleGroup(group);
        dec.setToggleGroup(group);
        frac.setSelected(Settings.fractionDefault);
        dec.setSelected(!Settings.fractionDefault);
        frac.setOnAction(e -> Settings.fractionDefault = true);
        dec.setOnAction(e -> Settings.fractionDefault = false);

        Label about = new Label(I18n.t("settings.about"));
        about.setStyle("-fx-text-fill: " + Theme.TEXT + ";");

        Button close = new Button(I18n.t("common.close"));
        close.setStyle("-fx-background-color: " + Theme.ACCENT_PETROL + "; -fx-text-fill: white;");
        close.setOnAction(e -> stage.close());

        VBox root = new VBox(14, title, languageLabel, language, fullscreen, darkMode, suggestions,
                displayLabel, frac, dec, precisionRow, about, close);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: " + Theme.BACKGROUND + ";");

        stage.setScene(new Scene(root, 460, 500));
        stage.show();
    }
}
