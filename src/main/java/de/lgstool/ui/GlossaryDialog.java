package de.lgstool.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

    /** One command (or syntax form) per row, expanded from feature groups and sorted A→Z. */
    private static Row[] rows() {
        record Group(String key, String... commands) {
        }
        Group[] groups = {
                new Group("gg.point", "A = (x, y)", "A = (x, y, z)"),
                new Group("gg.func", "f(x) = …", "g(x, y) = …"),
                new Group("gg.implicit", "x^2 + y^2 = 1"),
                new Group("gg.line", "Line(A, B)", "Segment(A, B)"),
                new Group("gg.circle", "Circle(M, r)", "Midpoint(A, B)"),
                new Group("gg.shapes", "Vector(A, B)", "Polygon(A, B, …)", "Angle(A, B, C)"),
                new Group("gg.slider", "Slider(min, max [, step])"),
                new Group("gg.matrixcreate", "create(M, width*height)", "rand(M, lo, hi)"),
                new Group("gg.matrix", "M = {{1,2},{3,4}}", "Solve(M)", "RREF(M)", "Det(M)",
                        "Inverse(M)", "Transpose(M)", "Rank(M)", "Identity(n)"),
                new Group("gg.calc", "Derivative(f)", "Integral(f)", "Integral(f, a, b)",
                        "Integral(f, g, a, b)", "Tangent(x0, f)"),
                new Group("gg.analysis", "Root(f)", "Extremum(f)", "InflectionPoint(f)", "Normal(x0, f)"),
                new Group("gg.cas", "Solve(x^2 - 4 = 0)", "Limit(f, x0)", "TaylorPolynomial(f, x0, n)",
                        "Substitute(expr, x, v)", "Expand(expr)", "Factor(expr)", "Simplify(expr)"),
                new Group("gg.measure", "Length(seg)", "Area(poly)", "Perimeter(poly)", "Slope(line)"),
                new Group("gg.relate", "Distance(A, B)", "Angle(g, h)"),
                new Group("gg.intersect", "Intersect(a, b [, n])"),
                new Group("gg.transform", "Reflect(obj, line)", "Rotate(obj, angle)",
                        "Translate(obj, vec)", "Dilate(obj, k)"),
                new Group("gg.construct", "PerpendicularLine(P, g)", "ParallelLine(P, g)",
                        "PerpendicularBisector(A, B)", "AngleBisector(A, B, C)", "PointOn(path, t)"),
                new Group("gg.conic", "Ellipse(F1, F2, a)", "Hyperbola(F1, F2, a)", "Parabola(F, line)"),
                new Group("gg.conican", "Center(conic)", "Eccentricity(conic)", "Circle(A, B, C)",
                        "Centroid(poly)", "ClosestPoint(P, obj)", "Curvature(x0, f)", "Roots(f)"),
                new Group("gg.locus", "Locus(P, s)"),
                new Group("gg.space", "Plane(A, B, C)", "Sphere(M, r)"),
                new Group("gg.list", "{1, 2, 3}", "Sequence(a, b [, step])", "Sum(list)",
                        "Mean(list)", "Element(list, i)", "Sort(list)"),
                new Group("gg.higher", "Sequence(k^2, k, a, b)", "Map(expr, k, list)", "KeepIf(cond, k, list)",
                        "CountIf(cond, k, list)", "Zip(expr, k, l1, m, l2)", "Take(list, m, n)",
                        "Union(l1, l2)", "Intersection(l1, l2)"),
                new Group("gg.stats", "Median(list)", "Q1(list)", "Q3(list)", "Variance(list)",
                        "SD(list)", "Mode(list)", "FitLine(points)", "FitPoly(points, n)"),
                new Group("gg.logic", "a < b", "a == b", "a & b", "If(cond, then, else)"),
                new Group("gg.complex", "3 + 4i", "Real(z)", "Imaginary(z)", "Conjugate(z)",
                        "Modulus(z)", "Arg(z)"),
                new Group("gg.text", "\"text\"", "Text(\"…\", P)"),
                new Group("gg.interact", "b = true"),
                new Group("gg.meta", "Delete(obj)", "clear", "undo", "redo", "list"),
                new Group("gg.io", "save(name)", "load(name)")
        };
        List<Row> rows = new ArrayList<>();
        for (Group g : groups) {
            for (String command : g.commands()) {
                rows.add(new Row(command, g.key(), "GEO"));
            }
        }
        rows.sort(Comparator.comparing(r -> r.command().toLowerCase(Locale.ROOT)));
        return rows.toArray(new Row[0]);
    }
}
