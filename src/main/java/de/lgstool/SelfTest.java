package de.lgstool;

import de.lgstool.geo.ui.Workspace;
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
 * Headless-ish smoke test ({@code App --selftest}): boots the JavaFX toolkit, builds the
 * unified workspace and exercises geometry, functions, matrices, lists, text, LaTeX,
 * rename, the LGS command compatibility layer and the 2D/3D views. Prints
 * {@code SELFTEST OK} and exits 0 on success.
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
                AppController controller = new AppController(stage);
                Scene scene = new Scene(controller.root(), 1280, 800);
                stage.setScene(scene);

                Workspace ws = new Workspace(() -> {
                }, () -> {
                });
                Scene wsScene = new Scene(ws.getRoot(), 1280, 800);

                // geometry + functions + slider.
                ws.submit("A = (1, 2)");
                ws.submit("s = Slider(0, 5)");
                ws.submit("c = Circle(A, s)");
                ws.submit("B = (4, 3)");
                ws.submit("M = Midpoint(A, B)");
                ws.submit("g = Line(A, B)");
                ws.submit("f(x) = sin(x)");
                ws.submit("df = Derivative(f)");
                layout(wsScene);

                // matrices (exact) + operations + LGS compatibility commands.
                ws.submit("Mx = {{1, 2}, {3, 4}}");
                ws.submit("d = Det(Mx)");
                ws.submit("Inv = Inverse(Mx)");
                ws.submit("Rr = RREF(Mx)");
                ws.submit("Tn = Transpose(Mx)");
                ws.submit("Aug = {{1, 1, 3}, {1, -1, 1}}");
                ws.submit("sol = Solve(Aug)");
                ws.submit("frac = {{1, 1/2}, {1/3, 1/4}}");
                ws.submit("Mx = {{1, 2, 3}, {4, 5, 6}}");   // non-square redefine: d=Det(Mx) -> undefined, must not poison
                ws.submit("create(Grid, 3*2)");             // editable zero matrix
                ws.submit("rand(1, 9)");                    // fill the last matrix with randoms
                layout(wsScene);

                // implicit equations (zero-contour curves).
                ws.submit("x^2 + y^2 = 4");
                ws.submit("1 = x^2 + y^2");
                ws.submit("y = x^2");
                layout(wsScene);

                // lists, sequences, text, statistics + regression.
                ws.submit("L = {1, 2, 3, 4}");
                ws.submit("sm = Sum(L)");
                ws.submit("seq = Sequence(1, 5)");
                ws.submit("md = Median(L)");
                ws.submit("vr = Variance(L)");
                ws.submit("data = {(0,1), (1,3), (2,5)}");
                ws.submit("fit = FitLine(data)");
                ws.submit("poly = FitPoly(data, 2)");
                ws.submit("pts = {(0,0), (1,1), (2,4)}");
                ws.submit("t1 = \"Hallo\"");
                ws.submit("t2 = Text(\"Punkt A\", A)");
                layout(wsScene);

                // booleans + higher-order lists + CAS-lite + complex + spreadsheet cells.
                ws.submit("flag = true");                   // boolean object -> checkbox
                ws.submit("cmp = 3 > 2");
                ws.submit("pick = If(flag, 10, 20)");
                ws.submit("sq = Sequence(k^2, k, 1, 5)");
                ws.submit("ev = KeepIf(n - 2*floor(n/2) == 0, n, sq)");
                ws.submit("sol = Solve(x^2 - 4 = 0)");
                ws.submit("lim = Limit(f, 0)");
                ws.submit("anti = Integral(f)");
                ws.submit("ex = Expand((x + 1)^2)");
                ws.submit("zc = 3 + 4i");                   // complex number
                ws.submit("mod = Modulus(zc)");
                ws.submit("A1 = 5");                        // spreadsheet cells
                ws.submit("B1 = A1 * 2");
                layout(wsScene);

                // rename + redefine + multi-command + undo.
                ws.submit("A = (2, 1)");          // redefine
                ws.engine().rename("B", "Bee");
                ws.submit("undo");
                ws.submit("clear");
                layout(wsScene);

                // 3D view.
                ws.showView(true);
                ws.submit("P = (1, 1, 1)");
                ws.submit("E = Plane((0,0,0), (1,0,0), (0,1,0))");
                ws.submit("K = Sphere((0,0,0), 2)");
                ws.submit("surf(x, y) = x^2 - y^2");
                ws.submit("Sys3 = {{1, 0, 0, 1}, {0, 1, 0, 2}, {0, 0, 1, 3}}");   // 3-var system -> planes
                layout(wsScene);
                ws.showView(false);
                layout(wsScene);

                // language + theme rebuild (around the shared engine).
                controller.changeLanguage("en");
                layout(scene);
                controller.changeLanguage("de");
                controller.changeTheme(true);
                layout(scene);
                controller.changeTheme(false);
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
