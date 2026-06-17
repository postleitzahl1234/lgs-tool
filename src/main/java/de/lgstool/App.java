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
        // Full screen bounds (incl. the taskbar area) so true fullscreen has no
        // size mismatch; maximized is the fallback when fullscreen is toggled off.
        Rectangle2D bounds = Screen.getPrimary().getBounds();
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
        stage.setMaximized(true);
        stage.show();

        // Applying fullscreen after show() is more reliable on Windows; this hides
        // the taskbar and covers the whole screen.
        stage.setFullScreen(Settings.startFullscreen);
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--selftest")) {
            SelfTest.run();
            return;
        }
        launch(args);
    }
}
