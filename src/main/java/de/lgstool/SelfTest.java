package de.lgstool;

import de.lgstool.ui.AppController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Headless-ish smoke test ({@code App --selftest}): boots the JavaFX toolkit,
 * builds the full UI and exercises 1D/2D/3D rendering, window handling, solving,
 * editing, undo and animation. Prints {@code SELFTEST OK} and exits 0 on success.
 */
public final class SelfTest {

    private SelfTest() {
    }

    public static void run() {
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running
        }
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                AppController c = new AppController(stage);
                Scene scene = new Scene(c.root(), 1280, 800);
                stage.setScene(scene);

                // 2 variables: lines + intersection.
                c.runMain("create(a, 2*2);");
                c.open("a");
                c.runLgs("a", "add(1 1 = 3);");
                c.runLgs("a", "add(1 -1 = 1);");
                c.runLgs("a", "solve();");
                c.runLgs("a", "det();");
                c.runLgs("a", "rank();");
                c.runLgs("a", "cond();");
                c.runLgs("a", "inverse();");
                c.runLgs("a", "verify();");
                c.runLgs("a", "highlight(1, #1F5E63);");
                c.runLgs("a", "fit();");
                c.editCell("a", 1, 0, "2");
                layout(scene);
                c.runLgs("a", "animate(1, 0, 6, 1);");
                layout(scene);
                c.runLgs("a", "axis(x, -10, 10);");
                c.runLgs("a", "axis(y, -7, 7);");
                c.runLgs("a", "axis();");
                c.runLgs("a", "rand(-5, 5);");
                layout(scene);

                // new analysis + I/O commands.
                c.runLgs("a", "rref();");
                c.runLgs("a", "ref();");
                c.runLgs("a", "transpose();");
                c.runLgs("a", "trace();");
                c.runLgs("a", "nullspace();");
                c.runLgs("a", "cramer();");
                c.runLgs("a", "latex();");
                c.runLgs("a", "csv(selftest_csv);");
                c.runLgs("a", "saveplot(selftest_plot);");
                c.runLgs("a", "savelog(selftest_log);");
                c.runLgs("a", "copylog();");
                c.runLgs("a", "labels();");
                c.runLgs("a", "labels();");
                layout(scene);

                // 1 variable: number line.
                c.runMain("create(b, 1*1);");
                c.open("b");
                c.runLgs("b", "add(2 = 4);");
                c.runLgs("b", "solve();");
                layout(scene);

                // 3 variables: planes in 3D.
                c.runMain("create(d, 3*3);");
                c.open("d");
                c.runLgs("d", "add(1 0 0 = 1);");
                c.runLgs("d", "add(0 1 0 = 2);");
                c.runLgs("d", "add(0 0 1 = 3);");
                c.runLgs("d", "solve();");
                layout(scene);

                // structural commands.
                c.runLgs("d", "rename(dd);");
                c.runMain("undo();");
                c.runMain("redo();");
                c.runMain("list();");
                c.runMain("examples();");
                c.runMain("examples(unique2);");
                c.runLgs("a", "close();");
                layout(scene);

                // large system (glass-pane virtualization, up to 100x100).
                c.runMain("create(big, 100*100);");
                c.open("big");
                c.editCell("big", 1, 0, "5");
                c.editCell("big", 100, 100, "7");
                layout(scene);
                c.runLgs("big", "close();");
                layout(scene);

                // language switch (rebuilds the UI).
                c.changeLanguage("en");
                layout(scene);
                c.changeLanguage("de");
                layout(scene);

                // theme switch (rebuilds the UI).
                c.changeTheme(true);
                layout(scene);
                c.changeTheme(false);
                layout(scene);
            } catch (Throwable t) {
                errors.add(t.toString());
                for (StackTraceElement el : t.getStackTrace()) {
                    errors.add("  at " + el);
                }
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                errors.add("TIMEOUT waiting for FX thread");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (errors.isEmpty()) {
            System.out.println("SELFTEST OK");
        } else {
            System.out.println("SELFTEST FAILED");
            errors.forEach(System.out::println);
        }
        Platform.exit();
        System.exit(errors.isEmpty() ? 0 : 1);
    }

    private static void layout(Scene scene) {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
    }
}
