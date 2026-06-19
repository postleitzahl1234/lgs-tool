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
        return new Row[]{
                new Row("create(name, b*h);", "g.create", "HAUPT"),
                new Row("add(c1 … cn = b [, zeile]);", "g.add", "LGS"),
                new Row("solve(n);", "g.solve", "LGS"),
                new Row("del(arg);", "g.del", "HAUPT/LGS"),
                new Row("swap(i, j);", "g.swap", "LGS"),
                new Row("scale(i, faktor);", "g.scale", "LGS"),
                new Row("combine(i, faktor, j);", "g.combine", "LGS"),
                new Row("det();", "g.det", "LGS"),
                new Row("rank();", "g.rank", "LGS"),
                new Row("cond([norm]);", "g.cond", "LGS"),
                new Row("show();", "g.show", "LGS"),
                new Row("clear();", "g.clear", "LGS"),
                new Row("undo();", "g.undo", "HAUPT/LGS"),
                new Row("rename(neu);", "g.rename", "LGS"),
                new Row("highlight(i [, hex]);", "g.highlight", "LGS"),
                new Row("resize(b*h);", "g.resize", "LGS"),
                new Row("setvar(spalte, name);", "g.setvar", "LGS"),
                new Row("save(name);", "g.save", "LGS"),
                new Row("load(name);", "g.load", "HAUPT"),
                new Row("open(name);", "g.open", "HAUPT"),
                new Row("close();", "g.close", "LGS"),
                new Row("list();", "g.list", "HAUPT"),
                new Row("fit();", "g.fit", "LGS"),
                new Row("verify();", "g.verify", "LGS"),
                new Row("inverse();", "g.inverse", "LGS"),
                new Row("subst(var, wert);", "g.subst", "LGS"),
                new Row("frac(); / dec();", "g.frac", "LGS"),
                new Row("animate(zeile, von, bis [, dauer]);", "g.animate", "LGS"),
                new Row("axis(achse, min, max);", "g.axis", "LGS"),
                new Row("rand(min, max [, zeile]);", "g.rand", "LGS"),
                new Row("redo();", "g.redo", "HAUPT/LGS"),
                new Row("rref(); / ref();", "g.rref", "LGS"),
                new Row("transpose();", "g.transpose", "LGS"),
                new Row("trace();", "g.trace", "LGS"),
                new Row("nullspace();", "g.nullspace", "LGS"),
                new Row("cramer();", "g.cramer", "LGS"),
                new Row("latex();", "g.latex", "LGS"),
                new Row("csv(name); / loadcsv(name);", "g.csv", "LGS/HAUPT"),
                new Row("run(datei);", "g.run", "HAUPT/LGS"),
                new Row("examples([name]);", "g.examples", "HAUPT"),
                new Row("savelog(name); / copylog();", "g.savelog", "LGS"),
                new Row("saveplot(name);", "g.saveplot", "LGS"),
                new Row("labels();", "g.labels", "LGS"),

                new Row("A=(1,2); B=(1,2,3)", "gg.point", "GEO"),
                new Row("f(x)=…; g(x,y)=…", "gg.func", "GEO"),
                new Row("Slider(min, max [, step])", "gg.slider", "GEO"),
                new Row("Line(A,B); Segment(A,B)", "gg.line", "GEO"),
                new Row("Circle(M,r); Midpoint(A,B)", "gg.circle", "GEO"),
                new Row("Intersect(a,b); Distance(A,B)", "gg.intersect", "GEO"),
                new Row("Vector(A,B); Polygon(A,…); Angle(A,B,C)", "gg.shapes", "GEO"),
                new Row("Derivative(f); Integral(f,a,b); Tangent(x0,f)", "gg.calc", "GEO"),
                new Row("Ellipse(F1,F2,a); Hyperbola(…); Parabola(F,line)", "gg.conic", "GEO"),
                new Row("Locus(P, s)", "gg.locus", "GEO"),
                new Row("Plane(A,B,C); Sphere(M,r)", "gg.space", "GEO"),
                new Row("Delete(obj); clear; undo; redo; list", "gg.meta", "GEO"),
                new Row("save(name); load(name)", "gg.io", "GEO")
        };
    }
}
