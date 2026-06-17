package de.lgstool.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import de.lgstool.model.Lgs;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * The LGS detail view: editable matrix, solution log and an [LGS] command line.
 *
 * <p>This is a panel, not a top-level window. The {@link AppController} swaps it
 * into the single application window's scene, so opening a system does not spawn
 * a second OS window. "← Overview" returns to the main screen.
 */
public final class LgsWindow {

    private final AppController controller;
    private final MatrixView matrixView;
    private final LogPane logPane = new LogPane(I18n.t("lgs.log"));
    private final Label titleLabel = new Label();
    private final BorderPane root = new BorderPane();
    private Lgs lgs;

    public LgsWindow(AppController controller) {
        this.controller = controller;
        this.matrixView = new MatrixView((row, col, text) -> controller.editCell(lgs.name(), row, col, text));

        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + Theme.ACCENT_PETROL + ";");

        Button back = action(I18n.t("lgs.back"), "close();");
        back.setStyle("-fx-background-color: " + Theme.ACCENT_PETROL + "; -fx-text-fill: white; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, back, titleLabel, spacer,
                action(I18n.t("lgs.solve"), "solve();"),
                action(I18n.t("lgs.check"), "verify();"),
                action(I18n.t("lgs.undo"), "undo();"),
                action(I18n.t("lgs.fractions"), "frac();"),
                action(I18n.t("lgs.decimals"), "dec();"),
                action(I18n.t("lgs.center"), "fit();"));
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(10));
        top.setStyle("-fx-background-color: derive(" + Theme.ACCENT_SAGE + ", 50%);");

        logPane.setPrefWidth(340);
        logPane.setMinWidth(280);

        CommandBar bar = new CommandBar("[LGS]", line -> controller.runLgs(lgs.name(), line));

        // The glass pane (matrix viewport) occupies 70% of the center area and is
        // centered, so it reads as a movable "pane" over a larger matrix.
        StackPane matrixWrap = new StackPane(matrixView);
        matrixWrap.setStyle("-fx-background-color: derive(" + Theme.BACKGROUND + ", -4%);");
        matrixView.maxWidthProperty().bind(matrixWrap.widthProperty().multiply(0.7));
        matrixView.maxHeightProperty().bind(matrixWrap.heightProperty().multiply(0.7));
        matrixView.prefWidthProperty().bind(matrixWrap.widthProperty().multiply(0.7));
        matrixView.prefHeightProperty().bind(matrixWrap.heightProperty().multiply(0.7));

        root.setStyle("-fx-background-color: " + Theme.BACKGROUND + ";");
        root.setTop(top);
        root.setCenter(matrixWrap);
        root.setRight(logPane);
        root.setBottom(bar);
    }

    private Button action(String label, String command) {
        Button b = new Button(label);
        b.setOnAction(e -> controller.runLgs(lgs.name(), command));
        return b;
    }

    public Parent getRoot() {
        return root;
    }

    public String logText() {
        return logPane.getText();
    }

    public void bind(Lgs lgs) {
        this.lgs = lgs;
        titleLabel.setText("LGS: " + lgs.name());
        matrixView.resetPan();
        matrixView.render(lgs);
    }

    public void refresh(Lgs lgs) {
        this.lgs = lgs;
        titleLabel.setText("LGS: " + lgs.name());
        matrixView.render(lgs);
    }

    public String lgsName() {
        return lgs == null ? null : lgs.name();
    }

    public void log(List<String> lines) {
        logPane.append(lines);
    }
}
