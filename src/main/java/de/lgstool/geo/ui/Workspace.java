package de.lgstool.geo.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import de.lgstool.geo.command.GeoEngine;
import de.lgstool.geo.command.GeoFormat;
import de.lgstool.geo.command.GeoResult;
import de.lgstool.geo.command.GeoView;
import de.lgstool.geo.command.Latex;
import de.lgstool.geo.io.GeoStorage;
import de.lgstool.geo.model.GeoObject;
import de.lgstool.ui.CommandBar;
import de.lgstool.ui.Suggestions;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javax.imageio.ImageIO;

/**
 * The single unified workspace (GeoGebra-style): the algebra/object list on the left
 * (functions, matrices, lists, … all as one list with colour dots), the coordinate
 * system in the middle, and the LaTeX/action log on the right. One input bar at the
 * bottom drives everything; matrices and geometry share the same engine and language.
 */
public final class Workspace implements GeoView {

    /** Bottom margin (≈ toggle-strip height) so the floating terminal sits just above the strip. */
    private static final double TERMINAL_OFFSET = 34;

    private final GeoEngine engine;
    private final CoordinatePlane2D plane;
    private final Scene3DView scene3d;
    private final AlgebraView algebra;
    private final ActionLog log = new ActionLog();
    private final BorderPane root = new BorderPane();
    private final StackPane appRoot = new StackPane();   // base content + the floating terminal layer
    private final CommandBar commandBar = new CommandBar("▶", this::run, Suggestions.geo());

    private boolean threeD;

    public Workspace(Runnable onSettings, Runnable onGlossary) {
        this(new GeoEngine(), onSettings, onGlossary);
    }

    /** Reuses an existing engine (so its objects survive a theme/language rebuild). */
    public Workspace(GeoEngine engine, Runnable onSettings, Runnable onGlossary) {
        this.engine = engine;
        this.plane = new CoordinatePlane2D(engine);
        this.scene3d = new Scene3DView(engine);
        this.algebra = new AlgebraView(engine, this::redrawActive);
        engine.setView(this);
        scene3d.setVisible(false);

        StackPane viewStack = new StackPane(plane, scene3d);
        root.setStyle("-fx-background-color: " + Theme.BACKGROUND + ";");
        root.setTop(topBar(onSettings, onGlossary));
        root.setLeft(leftPanel());
        root.setCenter(viewStack);
        root.setRight(log);
        root.setBottom(terminalToggleStrip());

        // The terminal floats as a layer above the content, so toggling it never reflows
        // the toolbar/panels/coordinate system (the toggle strip below it is always present).
        commandBar.setVisible(false);
        commandBar.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(commandBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(commandBar, new Insets(0, 0, TERMINAL_OFFSET, 0));
        appRoot.getChildren().addAll(root, commandBar);
    }

    public Parent getRoot() {
        return appRoot;
    }

    public GeoEngine engine() {
        return engine;
    }

    /** Runs a single command directly (used by the self-test; bypasses the ";" terminator rule). */
    public void submit(String line) {
        runOne(line);
    }

    public void showView(boolean threeDView) {
        this.threeD = threeDView;
        plane.setVisible(!threeDView);
        scene3d.setVisible(threeDView);
        redrawActive();
    }

    // ---- command handling ------------------------------------------------

    /**
     * Handles a raw command line from the terminal: every command must end with {@code ;}.
     * Multiple {@code ;}-separated commands run in order. A missing terminator is reported
     * as an error instead of being executed.
     */
    private void run(String raw) {
        String line = raw == null ? "" : raw.trim();
        if (line.isEmpty()) {
            return;
        }
        if (!line.endsWith(";")) {
            log.addAction(I18n.t("workspace.needSemicolon"));
            return;
        }
        for (String part : line.split(";")) {
            String segment = part.trim();
            if (!segment.isEmpty()) {
                runOne(segment);
            }
        }
    }

    private void runOne(String line) {
        Map<String, String> before = formattedValues();
        GeoResult result = engine.execute(line);
        boolean loggedObject = false;
        for (GeoObject o : engine.construction().objects()) {
            String now = GeoFormat.value(o.value());
            if (!now.equals(before.get(o.name()))) {
                log.addObject(o.colorHex(), o.name() + " = " + now, Latex.of(o.value()));
                loggedObject = true;
            }
        }
        if (!loggedObject || !result.ok()) {
            result.messages().forEach(log::addAction);
        }
    }

    private Map<String, String> formattedValues() {
        Map<String, String> map = new HashMap<>();
        for (GeoObject o : engine.construction().objects()) {
            map.put(o.name(), GeoFormat.value(o.value()));
        }
        return map;
    }

    // ---- layout ----------------------------------------------------------

    private HBox topBar(Runnable onSettings, Runnable onGlossary) {
        Label title = new Label(I18n.t("app.title"));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + Theme.ACCENT_PETROL + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(8, title, spacer, viewToggle(),
                control("+", () -> zoomActive(1.2)),
                control("−", () -> zoomActive(1 / 1.2)),
                control(I18n.t("lgs.center"), this::fitActive),
                control(I18n.t("graph.exportPng"), this::exportPng),
                control(I18n.t("graph.spreadsheet"), this::openSpreadsheet),
                control(I18n.t("graph.clear"), () -> runOne("clear")),
                control(I18n.t("lgs.undo"), () -> runOne("undo")),
                control(I18n.t("main.settings"), onSettings),
                control(I18n.t("main.glossary"), onGlossary));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10));
        bar.setStyle("-fx-background-color: derive(" + Theme.ACCENT_SAGE + ", 50%);");
        return bar;
    }

    /**
     * The always-present bottom strip with an arrow that reveals/hides the floating
     * terminal. The strip's height never changes, so toggling the terminal does not shift
     * any other element — the terminal itself is a separate layer in {@link #appRoot}.
     */
    private HBox terminalToggleStrip() {
        Button toggle = new Button("▸ " + I18n.t("workspace.terminal"));
        toggle.setStyle("-fx-background-color: transparent; -fx-text-fill: " + Theme.ACCENT_PETROL
                + "; -fx-font-weight: bold;");
        toggle.setOnAction(e -> {
            boolean show = !commandBar.isVisible();
            commandBar.setVisible(show);
            toggle.setText((show ? "▾ " : "▸ ") + I18n.t("workspace.terminal"));
            if (show) {
                commandBar.focusInput();
            }
        });

        HBox strip = new HBox(toggle);
        strip.setAlignment(Pos.CENTER_LEFT);
        strip.setPadding(new Insets(2, 8, 2, 8));
        strip.setStyle("-fx-background-color: derive(" + Theme.ACCENT_SAGE + ", 60%);");
        return strip;
    }

    private HBox viewToggle() {
        ToggleGroup group = new ToggleGroup();
        ToggleButton two = new ToggleButton(I18n.t("graph.view2d"));
        ToggleButton three = new ToggleButton(I18n.t("graph.view3d"));
        two.setToggleGroup(group);
        three.setToggleGroup(group);
        two.setSelected(true);
        two.setOnAction(e -> showView(false));
        three.setOnAction(e -> showView(true));
        group.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                a.setSelected(true);
            }
        });
        HBox box = new HBox(two, three);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private VBox leftPanel() {
        Label header = new Label(I18n.t("graph.objects"));
        header.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT + ";");

        ScrollPane scroll = new ScrollPane(algebra);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + Theme.BACKGROUND + "; -fx-background-color: " + Theme.BACKGROUND + ";");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox left = new VBox(8, header, scroll);
        left.setPadding(new Insets(12));
        left.setPrefWidth(340);
        left.setStyle("-fx-background-color: derive(" + Theme.BACKGROUND + ", 30%); -fx-border-color: derive("
                + Theme.ACCENT_SAGE + ", 10%); -fx-border-width: 0 1 0 0;");
        return left;
    }

    private Button control(String label, Runnable action) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color: " + Theme.ACCENT_PETROL + "; -fx-text-fill: white; -fx-font-weight: bold;");
        b.setOnAction(e -> action.run());
        return b;
    }

    /** Zoom/fit act on whichever view (2D plane or 3D scene) is currently shown. */
    private void zoomActive(double factor) {
        if (threeD) {
            scene3d.zoom(factor);
        } else {
            plane.zoom(factor);
        }
    }

    private void fitActive() {
        if (threeD) {
            scene3d.fit();
        } else {
            plane.fit();
        }
    }

    private void openSpreadsheet() {
        SpreadsheetDialog.show(root.getScene() == null ? null : root.getScene().getWindow(), engine);
    }

    private void exportPng() {
        Node node = threeD ? scene3d : plane;
        Path file = GeoStorage.directory().resolve(I18n.t("graph.sheetPrefix") + ".png");
        try {
            WritableImage image = node.snapshot(null, null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file.toFile());
            log.addAction(I18n.t("graph.pngSaved", file.toAbsolutePath().toString()));
        } catch (Exception e) {
            log.addAction(I18n.t("graph.pngFailed", e.getMessage()));
        }
    }

    private void redrawActive() {
        if (threeD) {
            scene3d.redraw();
        } else {
            plane.redraw();
        }
    }

    @Override
    public void structureChanged() {
        algebra.rebuild();
        redrawActive();
    }

    @Override
    public void valuesChanged() {
        algebra.updateValues();
        redrawActive();
    }
}
