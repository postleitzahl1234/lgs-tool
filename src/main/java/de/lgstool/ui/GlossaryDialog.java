package de.lgstool.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Reference window listing every command with syntax, meaning and context. */
public final class GlossaryDialog {

    /** One row of the glossary. */
    public record Row(String command, String meaningKey, String context) {
    }

    private GlossaryDialog() {
    }

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setAlwaysOnTop(true);
        stage.setTitle(I18n.t("glossary.title"));

        TableView<Row> table = new TableView<>(FXCollections.observableArrayList(rows()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Row, String> c1 = new TableColumn<>(I18n.t("glossary.colCmd"));
        c1.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().command()));
        c1.setPrefWidth(240);
        TableColumn<Row, String> c2 = new TableColumn<>(I18n.t("glossary.colMeaning"));
        c2.setCellValueFactory(d -> new ReadOnlyStringWrapper(I18n.t(d.getValue().meaningKey())));
        c2.setPrefWidth(520);
        TableColumn<Row, String> c3 = new TableColumn<>(I18n.t("glossary.colContext"));
        c3.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().context()));
        c3.setPrefWidth(130);
        table.getColumns().add(c1);
        table.getColumns().add(c2);
        table.getColumns().add(c3);

        Label header = new Label(I18n.t("glossary.header"));
        header.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.ACCENT_PETROL + ";");

        Label hkHeader = new Label(I18n.t("hk.title"));
        hkHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.ACCENT_PETROL + ";");
        TableView<Hotkey> hkTable = new TableView<>(FXCollections.observableArrayList(hotkeys()));
        hkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        hkTable.setPrefHeight(220);
        TableColumn<Hotkey, String> k1 = new TableColumn<>(I18n.t("hk.colKey"));
        k1.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().key()));
        k1.setPrefWidth(180);
        TableColumn<Hotkey, String> k2 = new TableColumn<>(I18n.t("hk.colAction"));
        k2.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().action()));
        hkTable.getColumns().add(k1);
        hkTable.getColumns().add(k2);

        VBox root = new VBox(10, header, table, hkHeader, hkTable);
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: " + Theme.BACKGROUND + ";");

        stage.setScene(new Scene(root, 940, 760));
        stage.show();
        stage.toFront();
    }

    /** One keyboard / mouse shortcut row. */
    public record Hotkey(String key, String action) {
    }

    private static Hotkey[] hotkeys() {
        return new Hotkey[]{
                new Hotkey(I18n.t("hk.keyEnter"), I18n.t("hk.enter")),
                new Hotkey("Tab", I18n.t("hk.tab")),
                new Hotkey("↑ / ↓", I18n.t("hk.updown")),
                new Hotkey(";", I18n.t("hk.semicolon")),
                new Hotkey("F11", I18n.t("hk.f11")),
                new Hotkey("Esc", I18n.t("hk.esc")),
                new Hotkey(I18n.t("hk.keyWheel"), I18n.t("hk.wheel")),
                new Hotkey(I18n.t("hk.keyDrag"), I18n.t("hk.drag")),
                new Hotkey(I18n.t("hk.keyClick"), I18n.t("hk.click"))
        };
    }

    private static Row[] rows() {
        // Only the active unified-workspace (GEO) commands, sorted alphabetically by syntax.
        Row[] rows = {
                new Row("A=(1,2); B=(1,2,3)", "gg.point", "GEO"),
                new Row("Circle(M,r); Midpoint(A,B)", "gg.circle", "GEO"),
                new Row("Delete(obj); clear; undo; redo; list", "gg.meta", "GEO"),
                new Row("Derivative(f); Integral(f,a,b); Tangent(x0,f)", "gg.calc", "GEO"),
                new Row("Distance/Angle(point/line/plane …)", "gg.relate", "GEO"),
                new Row("Ellipse(F1,F2,a); Hyperbola(…); Parabola(F,line)", "gg.conic", "GEO"),
                new Row("Center; Eccentricity; Circle(A,B,C); Centroid; ClosestPoint; Curvature; Roots", "gg.conican", "GEO"),
                new Row("f(x)=…; g(x,y)=…", "gg.func", "GEO"),
                new Row("Length; Area; Perimeter; Slope", "gg.measure", "GEO"),
                new Row("Line(A,B); Segment(A,B)", "gg.line", "GEO"),
                new Row("Locus(P, s)", "gg.locus", "GEO"),
                new Row("M = {{1,2},{3,4}}; Solve; RREF; Det; Inverse; Transpose", "gg.matrix", "GEO"),
                new Row("Median; Q1; Q3; Variance; SD; Mode; FitLine; FitPoly", "gg.stats", "GEO"),
                new Row("PerpendicularLine; ParallelLine; …Bisector; PointOn", "gg.construct", "GEO"),
                new Row("Plane(A,B,C); Sphere(M,r)", "gg.space", "GEO"),
                new Row("Reflect; Rotate; Translate; Dilate(obj, …)", "gg.transform", "GEO"),
                new Row("Root(f); Extremum(f); InflectionPoint(f); Normal(x0,f)", "gg.analysis", "GEO"),
                new Row("Solve; Limit; TaylorPolynomial; Substitute; Expand; Factor; Simplify", "gg.cas", "GEO"),
                new Row("Slider(min, max [, step])", "gg.slider", "GEO"),
                new Row("Vector(A,B); Polygon(A,…); Angle(A,B,C)", "gg.shapes", "GEO"),
                new Row("save(name); load(name)", "gg.io", "GEO"),
                new Row("x^2 + y^2 = 1; 1 = x^2+y^2; f(x^2,y^2)=1", "gg.implicit", "GEO"),
                new Row("{1,2,3}; Sequence; Sum; Mean; Element; Sort", "gg.list", "GEO"),
                new Row("Map; KeepIf; CountIf; Zip; Take; Union; Intersection", "gg.higher", "GEO"),
                new Row("a < b; a == b; a & b; If(cond, then, else)", "gg.logic", "GEO"),
                new Row("3 + 4i; Real; Imaginary; Conjugate; Modulus; Arg", "gg.complex", "GEO"),
                new Row("b = true; (checkbox); Slider + ▶ (animate)", "gg.interact", "GEO"),
                new Row("\"text\"; Text(\"…\", P)", "gg.text", "GEO"),
                new Row("Intersect(a,b); Distance(A,B)", "gg.intersect", "GEO")
        };
        java.util.Arrays.sort(rows,
                java.util.Comparator.comparing(r -> r.command().toLowerCase(java.util.Locale.ROOT)));
        return rows;
    }
}
