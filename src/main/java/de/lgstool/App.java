package de.lgstool;

import de.lgstool.config.I18n;
import de.lgstool.config.Settings;
import de.lgstool.ui.AppController;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Application entry point. Builds the main screen via {@link AppController} and
 * shows it filling the primary screen.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        AppController controller = new AppController(stage);
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(controller.root(), bounds.getWidth(), bounds.getHeight());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.setFullScreen(false);
            } else if (e.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });
        stage.setTitle(I18n.t("app.title"));
        stage.setScene(scene);
        stage.setFullScreenExitHint("ESC / F11");

        // Fill the whole primary screen, positioned at its top-left corner.
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMaximized(true);
        stage.show();

        // Applying fullscreen after show() is more reliable on Windows.
        if (Settings.startFullscreen) {
            stage.setFullScreen(true);
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--selftest")) {
            SelfTest.run();
            return;
        }
        launch(args);
    }
}
