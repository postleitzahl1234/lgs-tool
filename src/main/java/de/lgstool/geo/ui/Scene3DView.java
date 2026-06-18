package de.lgstool.geo.ui;

import de.lgstool.config.Theme;
import de.lgstool.geo.command.GeoEngine;
import de.lgstool.geo.expr.Expr;
import de.lgstool.geo.expr.Value;
import de.lgstool.geo.model.Construction;
import de.lgstool.geo.model.GeoObject;
import java.util.List;
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

    private static final double AXIS = 8;
    private static final int SURFACE_GRID = 26;
    private static final double SURFACE_RANGE = 6;

    private final GeoEngine engine;
    private final Group world = new Group();
    private final SubScene subScene;
    private final Rotate rotateX = new Rotate(24, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-32, Rotate.Y_AXIS);

    private double scale = 36;
    private double dragX;
    private double dragY;

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

    public void redraw() {
        world.getChildren().clear();
        addAxes();
        Construction c = engine.construction();
        List<GeoObject> objs = c.objects();
        for (int i = 0; i < objs.size(); i++) {
            GeoObject o = objs.get(i);
            if (!o.isVisible()) {
                continue;
            }
            Color color = GeoPalette.color(o, i);
            switch (o.value()) {
                case Value.PointV p -> addPoint(p, o.name(), color);
                case Value.SphereV s -> addSphere(s, color);
                case Value.PlaneV pl -> addPlane(pl, color);
                case Value.FunctionV ignored -> addSurface(o, color);
                default -> {
                    // lines/segments/circles are shown in the 2D view
                }
            }
        }
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
        if (f.params().size() != 2) {
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
        addAxis(new Point3D(1, 0, 0), Color.web("#B23A48"), "x", AXIS, 0, 0);
        addAxis(new Point3D(0, 1, 0), Color.web("#2E7D32"), "y", 0, AXIS, 0);
        addAxis(new Point3D(0, 0, 1), Color.web("#1565C0"), "z", 0, 0, AXIS);
    }

    private void addAxis(Point3D dir, Color color, String name, double lx, double ly, double lz) {
        double length = AXIS * scale;
        Box axis = new Box(
                dir.getX() != 0 ? length : 1.5,
                dir.getY() != 0 ? length : 1.5,
                dir.getZ() != 0 ? length : 1.5);
        axis.setMaterial(new PhongMaterial(color));
        world.getChildren().add(axis);

        Text label = new Text(name);
        label.setFill(color);
        label.setFont(Font.font(22));
        label.setTranslateX(lx * scale);
        label.setTranslateY(-ly * scale);
        label.setTranslateZ(lz * scale);
        world.getChildren().add(label);
    }

    // ---- interaction -----------------------------------------------------

    private void installMouseHandlers() {
        subScene.setOnMousePressed(e -> {
            dragX = e.getX();
            dragY = e.getY();
        });
        subScene.setOnMouseDragged(e -> {
            rotateY.setAngle(rotateY.getAngle() + (e.getX() - dragX) * 0.4);
            rotateX.setAngle(rotateX.getAngle() - (e.getY() - dragY) * 0.4);
            dragX = e.getX();
            dragY = e.getY();
        });
        subScene.setOnScroll(e -> {
            scale = clamp(scale * (e.getDeltaY() > 0 ? 1.1 : 1 / 1.1), 6, 300);
            redraw();
        });
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
