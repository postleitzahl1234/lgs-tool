package de.lgstool.viz;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import de.lgstool.math.Fraction;
import de.lgstool.math.Gauss;
import de.lgstool.math.Roman;
import de.lgstool.math.Solution;
import de.lgstool.model.Lgs;
import java.util.Locale;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

/**
 * The coordinate system. Number line (1 var), 2D lines (2 vars), 3D planes (3 vars).
 *
 * <p>The 2D/1D view is defined by explicit world bounds {@code [vx0,vx1] × [vy0,vy1]}
 * which can be panned, zoomed, auto-fitted, or set per axis from the terminal /
 * a dialog ({@link #setAxisRange}).
 */
public final class Visualization extends StackPane {

    private static final Color[] PALETTE = {
            Color.web(Theme.ACCENT_PETROL),
            Color.web(Theme.ACCENT_SAGE),
            Color.web("#C46A4E"),
            Color.web("#6A5ACD"),
            Color.web("#B59B2E"),
            Color.web("#3E7CB1")
    };

    private static final double DEFAULT_X = 8;
    private static final double DEFAULT_Y = 5;

    private final Canvas canvas = new Canvas();
    private final Pane threeDHolder = new Pane();
    private final Label info = new Label();

    private Lgs current;
    private Lgs animationOverride;

    // 2D / 1D view bounds (world coordinates).
    private double vx0 = -DEFAULT_X;
    private double vx1 = DEFAULT_X;
    private double vy0 = -DEFAULT_Y;
    private double vy1 = DEFAULT_Y;
    private boolean customRanges;
    private boolean showLabels = true;

    // 3D extents.
    private double zMin = -DEFAULT_X;
    private double zMax = DEFAULT_X;
    private double scale3d = 36;
    private final Rotate rotateX = new Rotate(24, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-32, Rotate.Y_AXIS);

    private double dragX;
    private double dragY;
    private double sVx0;
    private double sVx1;
    private double sVy0;
    private double sVy1;

    private int highlightRow = -1;
    private Color highlightColor;
    private AnimationTimer animTimer;

    public Visualization() {
        setStyle("-fx-background-color: " + Theme.BACKGROUND + ";");
        threeDHolder.setVisible(false);
        info.setStyle("-fx-text-fill: " + Theme.TEXT + "; -fx-font-size: 14px; -fx-padding: 12px;");
        getChildren().addAll(canvas, threeDHolder, info);
        StackPane.setAlignment(info, Pos.TOP_LEFT);

        widthProperty().addListener((o, a, b) -> resizeAndRedraw());
        heightProperty().addListener((o, a, b) -> resizeAndRedraw());

        canvas.setOnMousePressed(e -> {
            dragX = e.getX();
            dragY = e.getY();
            sVx0 = vx0;
            sVx1 = vx1;
            sVy0 = vy0;
            sVy1 = vy1;
        });
        canvas.setOnMouseDragged(e -> {
            double w = canvas.getWidth();
            double h = canvas.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            double dxWorld = (e.getX() - dragX) / w * (sVx1 - sVx0);
            double dyWorld = (e.getY() - dragY) / h * (sVy1 - sVy0);
            vx0 = sVx0 - dxWorld;
            vx1 = sVx1 - dxWorld;
            vy0 = sVy0 + dyWorld;
            vy1 = sVy1 + dyWorld;
            redraw();
        });
        canvas.setOnScroll(e -> zoom(e.getDeltaY() > 0 ? 1.1 : 1 / 1.1));

        threeDHolder.setOnMousePressed(e -> {
            dragX = e.getX();
            dragY = e.getY();
        });
        threeDHolder.setOnMouseDragged(e -> {
            rotateY.setAngle(rotateY.getAngle() + (e.getX() - dragX) * 0.4);
            rotateX.setAngle(rotateX.getAngle() - (e.getY() - dragY) * 0.4);
            dragX = e.getX();
            dragY = e.getY();
        });
        threeDHolder.setOnScroll(e -> zoom(e.getDeltaY() > 0 ? 1.1 : 1 / 1.1));
    }

    public void setLgs(Lgs lgs) {
        this.current = lgs;
        this.highlightRow = -1;
        this.highlightColor = null;
        redraw();
    }

    public void zoom(double factor) {
        scaleBounds(1 / factor);
        scale3d = clamp(scale3d * factor, 6, 400);
        redraw();
    }

    private void scaleBounds(double k) {
        double cx = (vx0 + vx1) / 2;
        double cy = (vy0 + vy1) / 2;
        double hx = (vx1 - vx0) / 2 * k;
        double hy = (vy1 - vy0) / 2 * k;
        vx0 = cx - hx;
        vx1 = cx + hx;
        vy0 = cy - hy;
        vy1 = cy + hy;
    }

    public void fit() {
        customRanges = false;
        vx0 = -DEFAULT_X;
        vx1 = DEFAULT_X;
        vy0 = -DEFAULT_Y;
        vy1 = DEFAULT_Y;
        scale3d = 36;
        rotateX.setAngle(24);
        rotateY.setAngle(-32);
        Lgs lgs = renderLgs();
        if (lgs != null && lgs.width() == 2 && lgs.height() > 0) {
            Solution s = safeSolve(lgs);
            if (s != null && s.kind() == Solution.Kind.UNIQUE) {
                double x = s.particular()[0].doubleValue();
                double y = s.particular()[1].doubleValue();
                vx0 = x - DEFAULT_X;
                vx1 = x + DEFAULT_X;
                vy0 = y - DEFAULT_Y;
                vy1 = y + DEFAULT_Y;
            }
        }
        redraw();
    }

    /**
     * Sets the display range of an axis. {@code axisIndex} 0=x, 1=y, 2=z;
     * {@code -1} resets all axes to automatic.
     */
    public void setAxisRange(int axisIndex, double min, double max) {
        if (axisIndex < 0) {
            fit();
            return;
        }
        if (max <= min) {
            return;
        }
        customRanges = true;
        switch (axisIndex) {
            case 0 -> {
                vx0 = min;
                vx1 = max;
            }
            case 1 -> {
                vy0 = min;
                vy1 = max;
            }
            case 2 -> {
                zMin = min;
                zMax = max;
            }
            default -> {
            }
        }
        redraw();
    }

    public double[] bounds2D() {
        return new double[]{vx0, vx1, vy0, vy1, zMin, zMax};
    }

    public boolean hasCustomRanges() {
        return customRanges;
    }

    public void setShowLabels(boolean show) {
        this.showLabels = show;
        redraw();
    }

    public void toggleLabels() {
        setShowLabels(!showLabels);
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public void highlight(int rowOneBased, String hex) {
        this.highlightRow = rowOneBased;
        this.highlightColor = hex == null ? Color.web("#D7263D") : Color.web(hex.startsWith("#") ? hex : "#" + hex);
        redraw();
    }

    public void startAnimation(int rowOneBased, Fraction from, Fraction to, double durationSeconds) {
        stopAnimation();
        if (current == null || rowOneBased < 1 || rowOneBased > current.height()) {
            return;
        }
        animationOverride = current.copy();
        int rhsCol = animationOverride.width();
        double fromD = from.doubleValue();
        double toD = to.doubleValue();
        double durNanos = Math.max(0.1, durationSeconds) * 1e9;
        long start = System.nanoTime();
        animTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double cycle = ((now - start) % (2 * durNanos)) / durNanos;
                double t = cycle <= 1 ? cycle : 2 - cycle;
                double value = fromD + t * (toD - fromD);
                animationOverride.setCell(rowOneBased, rhsCol, Fraction.parse(String.format(Locale.US, "%.6f", value)));
                redraw();
            }
        };
        animTimer.start();
    }

    public void stopAnimation() {
        if (animTimer != null) {
            animTimer.stop();
            animTimer = null;
        }
        animationOverride = null;
        redraw();
    }

    private Lgs renderLgs() {
        return animationOverride != null ? animationOverride : current;
    }

    private void resizeAndRedraw() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        threeDHolder.setPrefSize(getWidth(), getHeight());
        redraw();
    }

    private void redraw() {
        Lgs lgs = renderLgs();
        info.setText("");
        threeDHolder.setVisible(false);
        canvas.setVisible(true);
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.setFill(Color.web(Theme.BACKGROUND));
        g.fillRect(0, 0, w, h);

        if (lgs == null) {
            info.setText(I18n.t("viz.noLgs"));
            return;
        }
        if (!customRanges) {
            correctAspect(w, h);
        }
        String cls = classificationText(lgs);
        String base = lgs.name() + "  (" + lgs.width() + " " + I18n.t("viz.vars") + ")"
                + (cls.isEmpty() ? "" : " — " + cls);
        if (lgs.width() == 1) {
            info.setText(base);
            draw1D(g, w, h, lgs);
        } else if (lgs.width() == 2) {
            info.setText(base);
            draw2D(g, w, h, lgs);
        } else if (lgs.width() == 3) {
            info.setText(base);
            canvas.setVisible(false);
            draw3D(lgs);
        } else {
            info.setText(I18n.t("viz.moreThan3", lgs.name()) + (cls.isEmpty() ? "" : " (" + cls + ")"));
        }
    }

    private void correctAspect(double w, double h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        double cy = (vy0 + vy1) / 2;
        double halfX = (vx1 - vx0) / 2;
        double halfY = halfX * h / w;
        vy0 = cy - halfY;
        vy1 = cy + halfY;
    }

    private double sx(double x, double w) {
        return (x - vx0) / (vx1 - vx0) * w;
    }

    private double sy(double y, double h) {
        return h - (y - vy0) / (vy1 - vy0) * h;
    }

    private String classificationText(Lgs lgs) {
        if (lgs.height() == 0) {
            return I18n.t("viz.empty");
        }
        Solution s = safeSolve(lgs);
        if (s == null) {
            return "";
        }
        return switch (s.kind()) {
            case UNIQUE -> I18n.t("viz.unique");
            case INFINITE -> I18n.t("viz.infinite", s.freeParameterCount());
            case NONE -> I18n.t("viz.none");
        };
    }

    // ---- 2D --------------------------------------------------------------

    private void draw2D(GraphicsContext g, double w, double h, Lgs lgs) {
        drawGrid(g, w, h);
        for (int r = 1; r <= lgs.height(); r++) {
            Fraction[] row = lgs.row(r);
            double a = row[0].doubleValue();
            double b = row[1].doubleValue();
            double c = row[2].doubleValue();
            g.setStroke(colorFor(r));
            g.setLineWidth(r == highlightRow ? 3.5 : 2);
            if (Math.abs(b) > 1e-12) {
                double yL = (c - a * vx0) / b;
                double yR = (c - a * vx1) / b;
                g.strokeLine(sx(vx0, w), sy(yL, h), sx(vx1, w), sy(yR, h));
            } else if (Math.abs(a) > 1e-12) {
                double x = c / a;
                g.strokeLine(sx(x, w), 0, sx(x, w), h);
            }
        }
        Solution s = safeSolve(lgs);
        if (s != null && s.kind() == Solution.Kind.UNIQUE) {
            double x = s.particular()[0].doubleValue();
            double y = s.particular()[1].doubleValue();
            double px = sx(x, w);
            double py = sy(y, h);
            g.setFill(Color.web("#D7263D"));
            g.fillOval(px - 6, py - 6, 12, 12);
            g.setFill(Color.web(Theme.TEXT));
            g.fillText(String.format(Locale.US, "(%.3g, %.3g)", x, y), px + 10, py - 10);
        }
        String[] names = lgs.variableNames();
        drawAxisNames(g, w, h, names[0], names.length > 1 ? names[1] : null);
        drawLegend(g, w, h, lgs);
    }

    private void drawLegend(GraphicsContext g, double w, double h, Lgs lgs) {
        g.setFont(Font.font("Consolas", 12));
        int n = Math.min(lgs.height(), 8);
        if (n == 0) {
            return;
        }
        double pad = 10;
        double sw = 12;
        double lh = 18;
        double boxW = 320;
        double boxH = n * lh + 8;
        double x0 = pad;
        double y0 = h - boxH - pad;
        g.setFill(Color.web(Theme.BACKGROUND, 0.85));
        g.fillRect(x0, y0, boxW, boxH);
        g.setStroke(Color.web(Theme.TEXT, 0.2));
        g.setLineWidth(1);
        g.strokeRect(x0, y0, boxW, boxH);
        g.setTextBaseline(VPos.CENTER);
        g.setTextAlign(TextAlignment.LEFT);
        for (int r = 1; r <= n; r++) {
            double y = y0 + 4 + (r - 1) * lh + lh / 2;
            g.setFill(colorFor(r));
            g.fillRect(x0 + 6, y - sw / 2, sw, sw);
            g.setFill(Color.web(Theme.TEXT));
            g.fillText(equationText(lgs, r), x0 + 6 + sw + 6, y, boxW - sw - 20);
        }
    }

    private String equationText(Lgs lgs, int r) {
        Fraction[] row = lgs.row(r);
        boolean frac = lgs.isFractionDisplay();
        String[] names = lgs.variableNames();
        StringBuilder sb = new StringBuilder(Roman.of(r)).append(": ");
        boolean wrote = false;
        for (int c = 0; c < lgs.width(); c++) {
            Fraction coeff = row[c];
            if (coeff.isZero()) {
                continue;
            }
            String mag = frac ? coeff.abs().toString() : coeff.abs().toDecimalString(3);
            String term = (mag.equals("1") ? "" : mag) + names[c];
            if (!wrote) {
                sb.append(coeff.signum() < 0 ? "−" : "").append(term);
                wrote = true;
            } else {
                sb.append(coeff.signum() < 0 ? " − " : " + ").append(term);
            }
        }
        if (!wrote) {
            sb.append('0');
        }
        sb.append(" = ").append(frac ? row[lgs.width()].toString() : row[lgs.width()].toDecimalString(3));
        return sb.toString();
    }

    private void drawGrid(GraphicsContext g, double w, double h) {
        double stepX = niceStep((vx1 - vx0) / 10);
        double stepY = niceStep((vy1 - vy0) / 10);
        g.setStroke(Color.web(Theme.TEXT, 0.08));
        g.setLineWidth(1);
        for (double x = Math.ceil(vx0 / stepX) * stepX; x <= vx1; x += stepX) {
            g.strokeLine(sx(x, w), 0, sx(x, w), h);
        }
        for (double y = Math.ceil(vy0 / stepY) * stepY; y <= vy1; y += stepY) {
            g.strokeLine(0, sy(y, h), w, sy(y, h));
        }
        g.setStroke(Color.web(Theme.TEXT, 0.55));
        g.setLineWidth(1.5);
        g.strokeLine(0, sy(0, h), w, sy(0, h));
        g.strokeLine(sx(0, w), 0, sx(0, w), h);
        if (showLabels) {
            g.setFill(Color.web(Theme.TEXT, 0.6));
            for (double x = Math.ceil(vx0 / stepX) * stepX; x <= vx1; x += stepX) {
                if (Math.abs(x) > 1e-9) {
                    g.fillText(trim(x), sx(x, w) + 2, sy(0, h) + 14);
                }
            }
            for (double y = Math.ceil(vy0 / stepY) * stepY; y <= vy1; y += stepY) {
                if (Math.abs(y) > 1e-9) {
                    g.fillText(trim(y), sx(0, w) + 4, sy(y, h) - 2);
                }
            }
        }
    }

    private void drawAxisNames(GraphicsContext g, double w, double h, String xName, String yName) {
        if (!showLabels) {
            return;
        }
        g.setFill(Color.web(Theme.ACCENT_PETROL));
        g.setFont(Font.font("System", FontWeight.BOLD, 15));
        double axisY = clamp(sy(0, h), 14, h - 4);
        g.setTextAlign(TextAlignment.RIGHT);
        g.setTextBaseline(VPos.BOTTOM);
        g.fillText(xName, w - 6, axisY - 4);
        double axisX = clamp(sx(0, w), 4, w - 24);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);
        if (yName != null) {
            g.fillText(yName, axisX + 6, 4);
        }
        g.setFont(Font.font("Consolas", 13));
    }

    private static double niceStep(double raw) {
        if (raw <= 0) {
            return 1;
        }
        double pow = Math.pow(10, Math.floor(Math.log10(raw)));
        double frac = raw / pow;
        double nice = frac < 1.5 ? 1 : frac < 3 ? 2 : frac < 7 ? 5 : 10;
        return nice * pow;
    }

    private static String trim(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format(Locale.US, "%.2g", v);
    }

    // ---- 1D --------------------------------------------------------------

    private void draw1D(GraphicsContext g, double w, double h, Lgs lgs) {
        double cy = h / 2;
        g.setStroke(Color.web(Theme.TEXT, 0.55));
        g.setLineWidth(1.5);
        g.strokeLine(0, cy, w, cy);
        double stepX = niceStep((vx1 - vx0) / 10);
        g.setStroke(Color.web(Theme.TEXT, 0.25));
        g.setFill(Color.web(Theme.TEXT, 0.6));
        for (double x = Math.ceil(vx0 / stepX) * stepX; x <= vx1; x += stepX) {
            double px = sx(x, w);
            g.strokeLine(px, cy - 6, px, cy + 6);
            if (showLabels) {
                g.fillText(trim(x), px - 6, cy + 22);
            }
        }
        for (int r = 1; r <= lgs.height(); r++) {
            Fraction[] row = lgs.row(r);
            double a = row[0].doubleValue();
            double c = row[1].doubleValue();
            if (Math.abs(a) > 1e-12) {
                double px = sx(c / a, w);
                g.setStroke(colorFor(r));
                g.setLineWidth(r == highlightRow ? 3.5 : 2);
                g.strokeLine(px, cy - 24, px, cy + 24);
            }
        }
        Solution s = safeSolve(lgs);
        if (s != null && s.kind() == Solution.Kind.UNIQUE) {
            double x = s.particular()[0].doubleValue();
            double px = sx(x, w);
            g.setFill(Color.web("#D7263D"));
            g.fillOval(px - 6, cy - 6, 12, 12);
            g.setFill(Color.web(Theme.TEXT));
            g.fillText(lgs.variableNames()[0] + " = " + String.format(Locale.US, "%.4g", x), px + 10, cy - 12);
        }
        if (showLabels) {
            g.setFill(Color.web(Theme.ACCENT_PETROL));
            g.setFont(Font.font("System", FontWeight.BOLD, 15));
            g.setTextAlign(TextAlignment.RIGHT);
            g.setTextBaseline(VPos.BOTTOM);
            g.fillText(lgs.variableNames()[0], w - 6, cy - 6);
            g.setTextAlign(TextAlignment.LEFT);
            g.setTextBaseline(VPos.BASELINE);
        }
        drawLegend(g, w, h, lgs);
    }

    // ---- 3D --------------------------------------------------------------

    private void draw3D(Lgs lgs) {
        threeDHolder.setVisible(true);
        if (customRanges) {
            double span = Math.max(vx1 - vx0, Math.max(vy1 - vy0, zMax - zMin));
            scale3d = clamp(Math.min(getWidth(), getHeight()) / Math.max(1e-9, span), 6, 400);
        }
        Group world = new Group();
        world.getTransforms().addAll(rotateX, rotateY);
        addAxis(world, new Point3D(1, 0, 0), Color.web("#B23A48"));
        addAxis(world, new Point3D(0, 1, 0), Color.web("#2E7D32"));
        addAxis(world, new Point3D(0, 0, 1), Color.web("#1565C0"));
        if (showLabels) {
            String[] names = lgs.variableNames();
            addAxisLabel(world, 8, 0, 0, names.length > 0 ? names[0] : "x", Color.web("#B23A48"));
            addAxisLabel(world, 0, 8, 0, names.length > 1 ? names[1] : "y", Color.web("#2E7D32"));
            addAxisLabel(world, 0, 0, 8, names.length > 2 ? names[2] : "z", Color.web("#1565C0"));
        }

        for (int r = 1; r <= lgs.height(); r++) {
            Fraction[] row = lgs.row(r);
            MeshView plane = buildPlane(row[0].doubleValue(), row[1].doubleValue(),
                    row[2].doubleValue(), row[3].doubleValue(), colorFor(r));
            if (plane != null) {
                world.getChildren().add(plane);
            }
        }
        Solution s = safeSolve(lgs);
        if (s != null && s.kind() == Solution.Kind.UNIQUE) {
            Sphere sphere = new Sphere(6);
            sphere.setMaterial(new PhongMaterial(Color.web("#D7263D")));
            sphere.setTranslateX(s.particular()[0].doubleValue() * scale3d);
            sphere.setTranslateY(-s.particular()[1].doubleValue() * scale3d);
            sphere.setTranslateZ(s.particular()[2].doubleValue() * scale3d);
            world.getChildren().add(sphere);
        }

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-900);
        camera.setNearClip(0.1);
        camera.setFarClip(5000);
        camera.setFieldOfView(40);

        SubScene subScene = new SubScene(world, getWidth(), getHeight(), true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web(Theme.BACKGROUND));
        subScene.setCamera(camera);
        threeDHolder.getChildren().setAll(subScene);
    }

    private void addAxisLabel(Group world, double dx, double dy, double dz, String text, Color color) {
        Text t = new Text(text);
        t.setFill(color);
        t.setFont(Font.font(22));
        t.setTranslateX(dx * scale3d);
        t.setTranslateY(-dy * scale3d);
        t.setTranslateZ(dz * scale3d);
        world.getChildren().add(t);
    }

    private void addAxis(Group world, Point3D dir, Color color) {
        double length = 8 * scale3d;
        Box axis = new Box(
                dir.getX() != 0 ? length : 1.5,
                dir.getY() != 0 ? length : 1.5,
                dir.getZ() != 0 ? length : 1.5);
        axis.setMaterial(new PhongMaterial(color));
        world.getChildren().add(axis);
    }

    private MeshView buildPlane(double a, double b, double c, double d, Color color) {
        Point3D n = new Point3D(a, b, c);
        double len = n.magnitude();
        if (len < 1e-12) {
            return null;
        }
        Point3D unit = n.normalize();
        Point3D foot = unit.multiply(d / len);
        Point3D ref = Math.abs(unit.getX()) < 0.9 ? new Point3D(1, 0, 0) : new Point3D(0, 1, 0);
        Point3D u = unit.crossProduct(ref).normalize();
        Point3D v = unit.crossProduct(u).normalize();
        double s = 6;
        Point3D p1 = foot.add(u.multiply(s)).add(v.multiply(s));
        Point3D p2 = foot.add(u.multiply(-s)).add(v.multiply(s));
        Point3D p3 = foot.add(u.multiply(-s)).add(v.multiply(-s));
        Point3D p4 = foot.add(u.multiply(s)).add(v.multiply(-s));

        float[] points = new float[12];
        fillPoint(points, 0, p1);
        fillPoint(points, 3, p2);
        fillPoint(points, 6, p3);
        fillPoint(points, 9, p4);

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(points);
        mesh.getTexCoords().addAll(0f, 0f);
        mesh.getFaces().addAll(0, 0, 1, 0, 2, 0, 0, 0, 2, 0, 3, 0);
        MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.NONE);
        view.setMaterial(new PhongMaterial(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.5)));
        return view;
    }

    private void fillPoint(float[] arr, int offset, Point3D p) {
        arr[offset] = (float) (p.getX() * scale3d);
        arr[offset + 1] = (float) (-p.getY() * scale3d);
        arr[offset + 2] = (float) (p.getZ() * scale3d);
    }

    // ---- helpers ---------------------------------------------------------

    private Color colorFor(int rowOneBased) {
        if (rowOneBased == highlightRow && highlightColor != null) {
            return highlightColor;
        }
        return PALETTE[(rowOneBased - 1) % PALETTE.length];
    }

    private static Solution safeSolve(Lgs lgs) {
        try {
            if (lgs.height() == 0) {
                return null;
            }
            return Gauss.solve(lgs.coefficientMatrix(), lgs.rightHandSide());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
