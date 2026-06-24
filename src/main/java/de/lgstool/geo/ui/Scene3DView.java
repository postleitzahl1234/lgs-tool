package de.lgstool.geo.ui;

import de.lgstool.config.Theme;
import de.lgstool.geo.command.GeoEngine;
import de.lgstool.geo.expr.Expr;
import de.lgstool.geo.expr.Value;
import de.lgstool.geo.command.GeoFormat;
import de.lgstool.geo.model.Construction;
import de.lgstool.geo.model.GeoObject;
import de.lgstool.geo.model.Palette;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;

/**
 * The 3D view: a rotatable/zoomable {@link SubScene} rendering the spatial objects of
 * a {@link Construction} — points (2D points sit on the z=0 plane), spheres, planes and
 * surfaces {@code z=f(x,y)}. Drag to orbit, scroll to zoom.
 */
public final class Scene3DView extends Pane {

    private static final double AXIS_RANGE = 12;   // world units each axis extends each way (looks "infinite")
    private static final int TICK_STEP = 2;        // number every other integer to reduce clutter
    private static final int SURFACE_GRID = 26;
    private static final double SURFACE_RANGE = 6;

    private final GeoEngine engine;
    private final Group world = new Group();
    private final SubScene subScene;
    private final Rotate rotateX = new Rotate(24, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-32, Rotate.Y_AXIS);

    private static final int MAX_PLANES_FOR_CROSSINGS = 12;   // cap C(n,3) work
    private static final double CROSSING_HOVER_RADIUS = 22;   // screen px

    private double scale = 36;
    private double dragX;
    private double dragY;

    // Triple-plane intersection points: marker nodes + their world coordinates (parallel lists).
    private final List<Sphere> crossingNodes = new ArrayList<>();
    private final List<double[]> crossingPoints = new ArrayList<>();
    private Text hoverLabel;

    public Scene3DView(GeoEngine engine) {
        this.engine = engine;
        world.getTransforms().addAll(rotateX, rotateY);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-900);
        camera.setNearClip(0.1);
        camera.setFarClip(5000);
        camera.setFieldOfView(40);

        subScene = new SubScene(world, 1, 1, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web(Theme.BACKGROUND));
        subScene.setCamera(camera);
        getChildren().add(subScene);

        widthProperty().addListener((o, a, b) -> resize());
        heightProperty().addListener((o, a, b) -> resize());
        installMouseHandlers();
    }

    /** Zooms the scene by scaling object size (factor &gt; 1 zooms in). */
    public void zoom(double factor) {
        scale = clamp(scale * factor, 6, 300);
        redraw();
    }

    /** Resets zoom and orientation to the default view. */
    public void fit() {
        scale = 36;
        rotateX.setAngle(24);
        rotateY.setAngle(-32);
        redraw();
    }

    public void redraw() {
        world.getChildren().clear();
        addAxes();
        Construction c = engine.construction();
        List<GeoObject> objs = c.objects();
        for (int i = 0; i < objs.size(); i++) {
            GeoObject o = objs.get(i);
            if (!o.isVisible() || !o.isDefined()) {
                continue;
            }
            Color color = GeoPalette.color(o, i);
            switch (o.value()) {
                case Value.PointV p -> addPoint(p, o.name(), color);
                case Value.SphereV s -> addSphere(s, color);
                case Value.PlaneV pl -> addPlane(pl, color);
                case Value.FunctionV ignored -> addSurface(o, color);
                case Value.MatrixV mtx -> addSystem(mtx, o);
                default -> {
                    // lines/segments/circles are shown in the 2D view
                }
            }
        }
        hoverLabel = null;   // cleared with the world above
        addCrossings();
    }

    // ---- layout ----------------------------------------------------------

    private void resize() {
        subScene.setWidth(getWidth());
        subScene.setHeight(getHeight());
    }

    // ---- objects ---------------------------------------------------------

    private void addPoint(Value.PointV p, String name, Color color) {
        Sphere sphere = new Sphere(5);
        sphere.setMaterial(new PhongMaterial(color));
        sphere.setTranslateX(p.x() * scale);
        sphere.setTranslateY(-p.y() * scale);
        sphere.setTranslateZ(p.z() * scale);
        world.getChildren().add(sphere);

        Text label = new Text(name);
        label.setFill(color);
        label.setFont(Font.font(18));
        label.setTranslateX(p.x() * scale + 6);
        label.setTranslateY(-p.y() * scale - 6);
        label.setTranslateZ(p.z() * scale);
        world.getChildren().add(label);
    }

    private void addSphere(Value.SphereV s, Color color) {
        Sphere sphere = new Sphere(s.r() * scale);
        PhongMaterial mat = new PhongMaterial(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.45));
        sphere.setMaterial(mat);
        sphere.setTranslateX(s.center().x() * scale);
        sphere.setTranslateY(-s.center().y() * scale);
        sphere.setTranslateZ(s.center().z() * scale);
        world.getChildren().add(sphere);
    }

    /** Draws a 4-column augmented matrix {@code [a b c | d]} as one plane per row (a 3-variable system). */
    private void addSystem(Value.MatrixV m, GeoObject o) {
        if (m.colCount() != 4) {
            return;   // only 3-variable augmented systems map cleanly to planes
        }
        int rows = m.rowCount();
        for (int i = 0; i < rows; i++) {
            double a = m.rows()[i][0].doubleValue();
            double b = m.rows()[i][1].doubleValue();
            double c = m.rows()[i][2].doubleValue();
            double d = m.rows()[i][3].doubleValue();
            double deltaL = rows <= 1 ? 0 : (i - (rows - 1) / 2.0) * 0.10;
            Color rowColor = o.colorHex() != null ? Color.web(Palette.shade(o.colorHex(), deltaL))
                    : GeoPalette.color(o, i);
            addPlane(new Value.PlaneV(a, b, c, d), rowColor);
        }
    }

    private void addPlane(Value.PlaneV plane, Color color) {
        Point3D n = new Point3D(plane.a(), plane.b(), plane.c());
        double len = n.magnitude();
        if (len < 1e-12) {
            return;
        }
        Point3D unit = n.normalize();
        Point3D foot = unit.multiply(plane.d() / len);
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
        world.getChildren().add(view);
    }

    private void addSurface(GeoObject f, Color color) {
        if (!(f.value() instanceof Value.FunctionV fn) || fn.params().size() != 2) {
            return;
        }
        Construction c = engine.construction();
        int n = SURFACE_GRID;
        double lo = -SURFACE_RANGE;
        double span = 2 * SURFACE_RANGE;
        float[] pts = new float[(n + 1) * (n + 1) * 3];
        boolean any = false;
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                double x = lo + span * i / n;
                double y = lo + span * j / n;
                double h = surfaceHeight(c, f.name(), x, y);
                int idx = (i * (n + 1) + j) * 3;
                pts[idx] = (float) (x * scale);
                pts[idx + 1] = (float) (-h * scale);
                pts[idx + 2] = (float) (y * scale);
                any |= Double.isFinite(h);
            }
        }
        if (!any) {
            return;
        }
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(pts);
        mesh.getTexCoords().addAll(0f, 0f);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int p00 = i * (n + 1) + j;
                int p01 = p00 + 1;
                int p10 = (i + 1) * (n + 1) + j;
                int p11 = p10 + 1;
                mesh.getFaces().addAll(p00, 0, p10, 0, p11, 0);
                mesh.getFaces().addAll(p00, 0, p11, 0, p01, 0);
            }
        }
        MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.NONE);
        view.setMaterial(new PhongMaterial(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.7)));
        world.getChildren().add(view);
    }

    private static double surfaceHeight(Construction c, String name, double x, double y) {
        try {
            Value v = c.evaluate(new Expr.Call(name, List.of(new Expr.Num(x), new Expr.Num(y))));
            return v instanceof Value.NumberV num ? num.v() : Double.NaN;
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private void fillPoint(float[] arr, int offset, Point3D p) {
        arr[offset] = (float) (p.getX() * scale);
        arr[offset + 1] = (float) (-p.getY() * scale);
        arr[offset + 2] = (float) (p.getZ() * scale);
    }

    // ---- axes ------------------------------------------------------------

    private void addAxes() {
        addAxis(new Point3D(1, 0, 0), Color.web("#B23A48"), "x");
        addAxis(new Point3D(0, 1, 0), Color.web("#2E7D32"), "y");
        addAxis(new Point3D(0, 0, 1), Color.web("#1565C0"), "z");
        addTickNumbers();
    }

    /** A long thin box spanning both directions through the origin, plus a name at the +end. */
    private void addAxis(Point3D dir, Color color, String name) {
        double length = 2 * AXIS_RANGE * scale;
        Box axis = new Box(
                dir.getX() != 0 ? length : 1.2,
                dir.getY() != 0 ? length : 1.2,
                dir.getZ() != 0 ? length : 1.2);
        axis.setMaterial(new PhongMaterial(color));
        world.getChildren().add(axis);

        Text label = new Text(name);
        label.setFill(color);
        label.setFont(Font.font(22));
        label.setTranslateX(dir.getX() * (AXIS_RANGE * scale + 8));
        label.setTranslateY(-dir.getY() * (AXIS_RANGE * scale + 8));
        label.setTranslateZ(dir.getZ() * (AXIS_RANGE * scale + 8));
        world.getChildren().add(label);
    }

    /** Integer tick numbers along all three axes (0,±2,±4,…) so the grid is readable. */
    private void addTickNumbers() {
        Color color = Color.web(Theme.TEXT);
        int range = (int) AXIS_RANGE;
        for (int t = -range; t <= range; t += TICK_STEP) {
            if (t == 0) {
                continue;
            }
            addNumber(Integer.toString(t), t * scale, 0, 0, color);
            addNumber(Integer.toString(t), 0, -t * scale, 0, color);
            addNumber(Integer.toString(t), 0, 0, t * scale, color);
        }
        addNumber("0", 0, 0, 0, color);
    }

    private void addNumber(String text, double tx, double ty, double tz, Color color) {
        Text label = new Text(text);
        label.setFill(color);
        label.setFont(Font.font(11));
        label.setTranslateX(tx + 3);
        label.setTranslateY(ty + 3);
        label.setTranslateZ(tz);
        world.getChildren().add(label);
    }

    // ---- plane intersection points + hover coordinates -------------------

    /** Marks each triple-plane intersection point with a small dot (coords shown on hover). */
    private void addCrossings() {
        crossingNodes.clear();
        crossingPoints.clear();
        List<double[]> planes = collectPlanes();
        if (planes.size() < 3 || planes.size() > MAX_PLANES_FOR_CROSSINGS) {
            return;   // need 3 planes for a point; cap the O(n³) enumeration
        }
        for (int i = 0; i < planes.size(); i++) {
            for (int j = i + 1; j < planes.size(); j++) {
                for (int k = j + 1; k < planes.size(); k++) {
                    double[] p = intersect3(planes.get(i), planes.get(j), planes.get(k));
                    if (p != null) {
                        addCrossingMarker(p);
                    }
                }
            }
        }
    }

    private void addCrossingMarker(double[] p) {
        Sphere dot = new Sphere(3.2);
        dot.setMaterial(new PhongMaterial(Color.web(Theme.ACCENT_PETROL)));
        dot.setTranslateX(p[0] * scale);
        dot.setTranslateY(-p[1] * scale);
        dot.setTranslateZ(p[2] * scale);
        world.getChildren().add(dot);
        crossingNodes.add(dot);
        crossingPoints.add(p);
    }

    private List<double[]> collectPlanes() {
        List<double[]> planes = new ArrayList<>();
        for (GeoObject o : engine.construction().objects()) {
            if (!o.isVisible() || !o.isDefined()) {
                continue;
            }
            if (o.value() instanceof Value.PlaneV pl) {
                planes.add(new double[]{pl.a(), pl.b(), pl.c(), pl.d()});
            } else if (o.value() instanceof Value.MatrixV m && m.colCount() == 4) {
                for (int i = 0; i < m.rowCount(); i++) {
                    planes.add(new double[]{m.rows()[i][0].doubleValue(), m.rows()[i][1].doubleValue(),
                            m.rows()[i][2].doubleValue(), m.rows()[i][3].doubleValue()});
                }
            }
        }
        return planes;
    }

    /** Unique point of three planes {@code a·x+b·y+c·z=d}; null if (near-)singular (parallel/line). */
    private static double[] intersect3(double[] p1, double[] p2, double[] p3) {
        double det = det3(p1[0], p1[1], p1[2], p2[0], p2[1], p2[2], p3[0], p3[1], p3[2]);
        if (Math.abs(det) < 1e-9) {
            return null;
        }
        double x = det3(p1[3], p1[1], p1[2], p2[3], p2[1], p2[2], p3[3], p3[1], p3[2]) / det;
        double y = det3(p1[0], p1[3], p1[2], p2[0], p2[3], p2[2], p3[0], p3[3], p3[2]) / det;
        double z = det3(p1[0], p1[1], p1[3], p2[0], p2[1], p2[3], p3[0], p3[1], p3[3]) / det;
        return new double[]{x, y, z};
    }

    private static double det3(double a, double b, double c, double d, double e, double f,
            double g, double h, double i) {
        return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
    }

    /** Shows the coordinates of the nearest crossing point when the cursor is close (screen space). */
    private void updateCrossingLabel(double screenX, double screenY) {
        if (hoverLabel != null) {
            world.getChildren().remove(hoverLabel);
            hoverLabel = null;
        }
        double best = CROSSING_HOVER_RADIUS;
        int bestIdx = -1;
        for (int idx = 0; idx < crossingNodes.size(); idx++) {
            Point2D s = crossingNodes.get(idx).localToScreen(0, 0, 0);
            if (s == null) {
                continue;
            }
            double d = Math.hypot(s.getX() - screenX, s.getY() - screenY);
            if (d <= best) {
                best = d;
                bestIdx = idx;
            }
        }
        if (bestIdx >= 0) {
            double[] p = crossingPoints.get(bestIdx);
            Text label = new Text("(" + GeoFormat.num(p[0]) + ", " + GeoFormat.num(p[1])
                    + ", " + GeoFormat.num(p[2]) + ")");
            label.setFill(Color.web(Theme.ACCENT_PETROL));
            label.setFont(Font.font(15));
            label.setTranslateX(p[0] * scale + 6);
            label.setTranslateY(-p[1] * scale - 6);
            label.setTranslateZ(p[2] * scale);
            world.getChildren().add(label);
            hoverLabel = label;
        }
    }

    // ---- interaction -----------------------------------------------------

    private void installMouseHandlers() {
        subScene.setOnMousePressed(e -> {
            dragX = e.getX();
            dragY = e.getY();
        });
        subScene.setOnMouseMoved(e -> updateCrossingLabel(e.getScreenX(), e.getScreenY()));
        subScene.setOnMouseDragged(e -> {
            rotateY.setAngle(rotateY.getAngle() + (e.getX() - dragX) * 0.4);
            rotateX.setAngle(rotateX.getAngle() - (e.getY() - dragY) * 0.4);
            dragX = e.getX();
            dragY = e.getY();
        });
        // Scroll-to-zoom on the pane itself (events bubble up from the SubScene, so this
        // fires reliably whether or not the SubScene is the direct target).
        setOnScroll(e -> {
            zoom(e.getDeltaY() > 0 ? 1.1 : 1 / 1.1);
            e.consume();
        });
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
