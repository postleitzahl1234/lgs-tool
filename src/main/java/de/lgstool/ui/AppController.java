package de.lgstool.ui;

import de.lgstool.command.CommandContext;
import de.lgstool.command.CommandEngine;
import de.lgstool.command.CommandResult;
import de.lgstool.command.UiBridge;
import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import de.lgstool.math.Fraction;
import de.lgstool.model.Lgs;
import java.io.File;
import java.util.List;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

/**
 * Central wiring between the {@link CommandEngine} and the JavaFX UI. Implements
 * {@link UiBridge} so engine effects (open/close windows, refresh, highlight,
 * animate) drive the interface, while the UI sends command lines back to the engine.
 */
public final class AppController implements UiBridge {

    private final CommandEngine engine = new CommandEngine();
    private final Stage primaryStage;
    private MainScreen mainScreen;
    private LgsWindow detailWindow;

    /** System shown in the coordinate system; persists after its window closes. */
    private String vizName;

    public AppController(Stage primaryStage) {
        this.primaryStage = primaryStage;
        engine.setUi(this);
        this.mainScreen = new MainScreen(this);
        refreshButtons();
        refreshVisualization();
    }

    public Parent root() {
        return mainScreen.getRoot();
    }

    // ---- UI -> engine ----------------------------------------------------

    public void runMain(String line) {
        CommandResult result = engine.execute(line, CommandContext.main());
        mainScreen.log(result.messages());
    }

    public void runLgs(String name, String line) {
        CommandResult result = engine.execute(line, CommandContext.lgs(name));
        if (detailWindow != null && name.equals(detailWindow.lgsName())) {
            detailWindow.log(result.messages());
        } else {
            mainScreen.log(result.messages());
        }
    }

    public void open(String name) {
        runMain("open(" + name + ");");
    }

    public void editCell(String name, int rowOneBased, int colZeroBased, String text) {
        Fraction value;
        try {
            value = Fraction.parse(text);
        } catch (RuntimeException e) {
            if (detailWindow != null) {
                detailWindow.log(List.of("Fehler: ungültige Zahl '" + text + "'."));
            }
            lgsChanged(name);
            return;
        }
        engine.editCell(name, rowOneBased, colZeroBased, value);
    }

    public void newLgsDialog() {
        NewLgsDialog.show(primaryStage, this);
    }

    /** Next free default system name for the current language, e.g. lgs1 / matrix1. */
    public String nextDefaultName() {
        String prefix = I18n.t("new.defaultPrefix");
        int n = 1;
        while (engine.state().contains(prefix + n)) {
            n++;
        }
        return prefix + n;
    }

    public void openSettings() {
        SettingsDialog.show(primaryStage, this);
    }

    public void openGlossary() {
        GlossaryDialog.show(primaryStage);
    }

    public void openAxes() {
        String open = engine.state().openName();
        Lgs lgs = open == null ? null : engine.state().get(open);
        int width = lgs == null ? 2 : lgs.width();
        AxisDialog.show(primaryStage, mainScreen.visualization(), width);
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
        String open = engine.state().openName();
        detailWindow = null;
        mainScreen = new MainScreen(this);
        primaryStage.setTitle(I18n.t("app.title"));
        if (primaryStage.getScene() != null) {
            primaryStage.getScene().setRoot(mainScreen.getRoot());
        }
        refreshButtons();
        refreshVisualization();
        if (open != null && engine.state().get(open) != null) {
            ensureDetail(open);
        }
    }

    // ---- engine -> UI (UiBridge) ----------------------------------------

    @Override
    public void systemsChanged() {
        refreshButtons();
        reconcileDetail();
        refreshVisualization();
    }

    @Override
    public void openWindow(String name) {
        ensureDetail(name);
        refreshButtons();
        refreshVisualization();
    }

    @Override
    public void closeWindow(String name) {
        if (detailWindow != null && name != null && name.equals(detailWindow.lgsName())) {
            showMain();
            detailWindow = null;
        }
        refreshButtons();
        refreshVisualization();
    }

    @Override
    public void lgsChanged(String name) {
        if (detailWindow != null && name.equals(detailWindow.lgsName())) {
            detailWindow.refresh(engine.state().get(name));
        }
        if (name.equals(vizName)) {
            refreshVisualization();
        }
    }

    @Override
    public void highlightRow(String name, int rowOneBased, String hex) {
        if (name.equals(vizName)) {
            mainScreen.visualization().highlight(rowOneBased, hex);
        }
    }

    @Override
    public void fitView(String name) {
        if (name.equals(vizName)) {
            mainScreen.visualization().fit();
        }
    }

    @Override
    public void animate(String name, int rowOneBased, Fraction from, Fraction to, double durationSeconds) {
        if (name.equals(vizName)) {
            mainScreen.visualization().startAnimation(rowOneBased, from, to, durationSeconds);
        }
    }

    @Override
    public void setAxisRange(String name, int axisIndex, double min, double max) {
        if (name.equals(vizName)) {
            mainScreen.visualization().setAxisRange(axisIndex, min, max);
        }
    }

    @Override
    public void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    @Override
    public void copyLog() {
        copyToClipboard(activeLogText());
    }

    @Override
    public void saveLog(String path) {
        try {
            File file = new File(path);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            java.nio.file.Files.writeString(file.toPath(), activeLogText());
        } catch (Exception e) {
            // best-effort; the engine already reported the intended path
        }
    }

    @Override
    public void savePlot(String path) {
        try {
            WritableImage image = mainScreen.visualization().snapshot(null, null);
            File file = new File(path);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (Exception e) {
            // best-effort
        }
    }

    @Override
    public void toggleLabels(String name) {
        if (name.equals(vizName)) {
            mainScreen.visualization().toggleLabels();
        }
    }

    private String activeLogText() {
        if (detailWindow != null) {
            return detailWindow.logText();
        }
        return mainScreen.statusLogText();
    }

    // ---- helpers ---------------------------------------------------------

    private void refreshButtons() {
        mainScreen.refreshButtons(engine.state().names(), engine.state().openName());
    }

    private void refreshVisualization() {
        String open = engine.state().openName();
        if (open != null) {
            vizName = open;
        }
        // Keep showing the last selected system after its window closes; drop it
        // only once the system itself no longer exists.
        if (vizName != null && engine.state().get(vizName) == null) {
            vizName = null;
        }
        mainScreen.visualization().setLgs(vizName == null ? null : engine.state().get(vizName));
    }

    private void ensureDetail(String name) {
        if (engine.state().get(name) == null) {
            return;
        }
        if (detailWindow == null) {
            detailWindow = new LgsWindow(this);
        }
        detailWindow.bind(engine.state().get(name));
        showDetail();
    }

    private void reconcileDetail() {
        String open = engine.state().openName();
        if (open == null || engine.state().get(open) == null) {
            if (detailWindow != null) {
                showMain();
                detailWindow = null;
            }
        } else {
            ensureDetail(open);
        }
    }

    /** Swaps the single window's content to the detail view of the open system. */
    private void showDetail() {
        if (detailWindow != null && primaryStage.getScene() != null) {
            primaryStage.getScene().setRoot(detailWindow.getRoot());
        }
    }

    /** Swaps the single window's content back to the main overview screen. */
    private void showMain() {
        if (primaryStage.getScene() != null) {
            primaryStage.getScene().setRoot(mainScreen.getRoot());
        }
    }
}
