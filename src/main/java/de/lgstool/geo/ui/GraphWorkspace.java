package de.lgstool.geo.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import de.lgstool.geo.command.GeoEngine;
import de.lgstool.geo.command.GeoResult;
import de.lgstool.geo.command.GeoView;
import de.lgstool.ui.CommandBar;
import de.lgstool.ui.LogPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The graphing workspace: an algebra list and message log on the left, a switchable
 * 2D/3D view in the centre, and a GeoGebra-style input bar at the bottom. Implements
 * {@link GeoView} so engine changes refresh the list and the active view.
 */
public final class GraphWorkspace implements GeoView {

    private final GeoEngine engine = new GeoEngine();
    private final CoordinatePlane2D plane = new CoordinatePlane2D(engine);
    private final Scene3DView scene3d = new Scene3DView(engine);
    private final AlgebraView algebra = new AlgebraView(engine, this::redrawActive);
    private final LogPane log = new LogPane(I18n.t("graph.log"));
    private final BorderPane root = new BorderPane();

    private boolean threeD;
    private final String name;

    public GraphWorkspace(Runnable onBack) {
        this(null, onBack);
    }

    public GraphWorkspace(String name, Runnable onBack) {
        this.name = name;
        engine.setView(this);
        scene3d.setVisible(false);

        StackPane viewStack = new StackPane(plane, scene3d);
        root.setStyle("-fx-background-color: " + Theme.BACKGROUND + ";");
        root.setTop(topBar(onBack));
        root.setLeft(leftPanel());
        root.setCenter(viewStack);
        root.setBottom(new CommandBar("[GEO]", this::run));
    }

    public Parent getRoot() {
        return root;
    }

    /** Runs an input line as if typed into the command bar (used by the self-test). */
    public void submit(String line) {
        run(line);
    }

    /** Switches between the 2D and 3D view. */
    public void showView(boolean threeDView) {
        this.threeD = threeDView;
        plane.setVisible(!threeDView);
        scene3d.setVisible(threeDView);
        redrawActive();
    }

    private void run(String line) {
        GeoResult result = engine.execute(line);
        log.append(result.messages());
    }

    private HBox topBar(Runnable onBack) {
        Button back = new Button(I18n.t("lgs.back"));
        back.setStyle("-fx-background-color: " + Theme.ACCENT_PETROL + "; -fx-text-fill: white; -fx-font-weight: bold;");
        back.setOnAction(e -> onBack.run());

        Label title = new Label(name == null ? I18n.t("graph.title") : I18n.t("graph.title") + ": " + name);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + Theme.ACCENT_PETROL + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, back, title, spacer, viewToggle(),
                control("+", () -> plane.zoom(1.2)),
                control("−", () -> plane.zoom(1 / 1.2)),
                control(I18n.t("lgs.center"), plane::fit),
                control(I18n.t("graph.clear"), () -> engine.execute("clear")));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10));
        bar.setStyle("-fx-background-color: derive(" + Theme.ACCENT_SAGE + ", 50%);");
        return bar;
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
        // Prevent deselecting the active button (keep one always selected).
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

        log.setPrefHeight(150);
        log.setMinHeight(110);

        VBox left = new VBox(8, header, scroll, log);
        left.setPadding(new Insets(12));
        left.setPrefWidth(330);
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
