package de.lgstool.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import de.lgstool.geo.ui.Workspace;
import javafx.scene.Parent;
import javafx.stage.Stage;

/**
 * Hosts the single unified {@link Workspace} and the global dialogs. Changing the
 * language or theme rebuilds the workspace around the same engine, so the user's
 * objects survive the rebuild.
 */
public final class AppController {

    private final Stage primaryStage;
    private Workspace workspace;

    public AppController(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.workspace = new Workspace(this::openSettings, this::openGlossary);
    }

    public Parent root() {
        return workspace.getRoot();
    }

    public void openSettings() {
        SettingsDialog.show(primaryStage, this);
    }

    public void openGlossary() {
        GlossaryDialog.show(primaryStage);
    }

    public void changeLanguage(String code) {
        I18n.setLanguage(code);
        rebuildUi();
    }

    public void changeTheme(boolean dark) {
        Theme.setDark(dark);
        rebuildUi();
    }

    private void rebuildUi() {
        workspace = new Workspace(workspace.engine(), this::openSettings, this::openGlossary);
        primaryStage.setTitle(I18n.t("app.title"));
        if (primaryStage.getScene() != null) {
            primaryStage.getScene().setRoot(workspace.getRoot());
        }
    }
}
