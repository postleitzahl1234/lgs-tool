package de.lgstool.geo.ui;

import de.lgstool.config.Settings;
import de.lgstool.config.Theme;
import de.lgstool.geo.command.GeoEngine;
import de.lgstool.geo.expr.Evaluator;
import de.lgstool.geo.expr.Expr;
import de.lgstool.geo.expr.NameResolver;
import de.lgstool.geo.expr.Value;
import de.lgstool.geo.model.Construction;
import de.lgstool.geo.model.GeoObject;
import de.lgstool.viz.Plane2DMath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * The 2D drawing surface: a pannable / zoomable Cartesian plane that renders the 2D
 * objects of a {@link Construction} (points, lines, segments, circles, vectors and
 * function graphs) and lets free points be dragged with the mouse.
 *
 * <p>World↔screen mapping, grid and axis drawing are intentionally self-contained so
 * the component can be reused by other views.
 */
public final class CoordinatePlane2D extends Pane {

    private static final double HIT_RADIUS = 10;
    private static final int SAMPLES = 400;

    private final GeoEngine engine;
    private final Canvas canvas = new Canvas();

    private double vx0 = -10;
    private double vx1 = 10;
    private double vy0 = -6;
    private double vy1 = 6;

    private boolean panning;
    private String dragName;
    private double pressX;
    private double pressY;
    private double sVx0;
    private double sVx1;
    private double sVy0;
    private double sVy1;

    public CoordinatePlane2D(GeoEngine engine) {
        this.engine = engine;
        getChildren().add(canvas);
        setStyle("-fx-background-color: " + Theme.BACKGROUND + ";");
        widthProperty().addListener((o, a, b) -> resize());
        heightProperty().addListener((o, a, b) -> resize());
        installMouseHandlers();
    }

    public void fit() {
        vx0 = -10;
        vx1 = 10;
        vy0 = -6;
        vy1 = 6;
        redraw();
    }

    public void zoom(double factor) {
        double cx = (vx0 + vx1) / 2;
        double cy = (vy0 + vy1) / 2;
        double hx = (vx1 - vx0) / 2 / factor;
        double hy = (vy1 - vy0) / 2 / factor;
        vx0 = cx - hx;
        vx1 = cx + hx;
        vy0 = cy - hy;
        vy1 = cy + hy;
        redraw();
    }

    public void redraw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web(Theme.BACKGROUND));
        g.fillRect(0, 0, w, h);
        if (w <= 0 || h <= 0) {
            return;
        }
        // Rounded caps/joins make function graphs, polygons and vectors render smoothly.
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        correctAspect(w, h);
        drawGrid(g, w, h);
        drawObjects(g, w, h);
    }

    // ---- layout ----------------------------------------------------------

    private void resize() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        redraw();
    }

    private void correctAspect(double w, double h) {
        double cy = (vy0 + vy1) / 2;
        double halfY = (vx1 - vx0) / 2 * h / w;
        vy0 = cy - halfY;
        vy1 = cy + halfY;
    }

    private double sx(double x, double w) {
        return Plane2DMath.sx(x, vx0, vx1, w);
    }

    private double sy(double y, double h) {
        return Plane2DMath.sy(y, vy0, vy1, h);
    }

    private double wx(double px, double w) {
        return Plane2DMath.wx(px, vx0, vx1, w);
    }

    private double wy(double py, double h) {
        return Plane2DMath.wy(py, vy0, vy1, h);
    }

    private double unitPx(double w) {
        return w / (vx1 - vx0);
    }

    // ---- grid ------------------------------------------------------------

    private void drawGrid(GraphicsContext g, double w, double h) {
        double step = niceStep((vx1 - vx0) / 10);
        g.setLineWidth(1);
        g.setStroke(Color.web(Theme.TEXT, 0.08));
        for (double x = Math.ceil(vx0 / step) * step; x <= vx1; x += step) {
            g.strokeLine(sx(x, w), 0, sx(x, w), h);
        }
        for (double y = Math.ceil(vy0 / step) * step; y <= vy1; y += step) {
            g.strokeLine(0, sy(y, h), w, sy(y, h));
        }
        g.setStroke(Color.web(Theme.TEXT, 0.55));
        g.setLineWidth(1.5);
        g.strokeLine(0, sy(0, h), w, sy(0, h));
        g.strokeLine(sx(0, w), 0, sx(0, w), h);

        // Axis tick numbers — clamped so they stay visible when the origin is off-screen,
        // each drawn with a background halo so it stays readable over the grid and objects.
        g.setFont(Font.font("Consolas", 11));
        Color tickColor = Color.web(Theme.TEXT, 0.85);
        double labelBaseline = Math.max(12, Math.min(h - 4, sy(0, h) + 12));
        double labelX = Math.max(4, Math.min(w - 26, sx(0, w) + 4));
        for (double x = Math.ceil(vx0 / step) * step; x <= vx1; x += step) {
            if (Math.abs(x) > 1e-9) {
                haloLabel(g, trim(x), sx(x, w) + 2, labelBaseline, tickColor);
            }
        }
        for (double y = Math.ceil(vy0 / step) * step; y <= vy1; y += step) {
            if (Math.abs(y) > 1e-9) {
                haloLabel(g, trim(y), labelX, sy(y, h) - 2, tickColor);
            }
        }
        haloLabel(g, "0", Math.max(4, sx(0, w) + 4), Math.max(12, Math.min(h - 4, sy(0, h) + 12)), tickColor);

        // Axis names at the ends.
        g.setFont(Font.font("System", FontWeight.BOLD, 14));
        Color axisColor = Color.web(Theme.ACCENT_PETROL);
        haloLabel(g, "x", w - 14, Math.max(16, Math.min(h - 6, sy(0, h) - 6)), axisColor);
        haloLabel(g, "y", Math.max(6, Math.min(w - 16, sx(0, w) + 6)), 16, axisColor);
    }

    /** Draws text with a soft background outline so it stays legible over any backdrop. */
    private void haloLabel(GraphicsContext g, String text, double x, double y, Color fill) {
        g.setLineWidth(3);
        g.setStroke(Color.web(Theme.BACKGROUND, 0.85));
        g.strokeText(text, x, y);
        g.setFill(fill);
        g.fillText(text, x, y);
    }

    // ---- objects ---------------------------------------------------------

    private void drawObjects(GraphicsContext g, double w, double h) {
        Construction c = engine.construction();
        List<GeoObject> objs = c.objects();
        for (int i = 0; i < objs.size(); i++) {
            GeoObject o = objs.get(i);
            if (!o.isVisible() || !o.isDefined()) {
                continue;
            }
            Color color = colorFor(o, i);
            switch (o.value()) {
                case Value.LineV line -> drawLine(g, w, h, line, color);
                case Value.SegmentV s -> drawSegment(g, w, h, s, color);
                case Value.CircleV circle -> drawCircle(g, w, h, circle, color);
                case Value.VectorV v -> drawVector(g, w, h, v, color);
                case Value.PolygonV poly -> drawPolygon(g, w, h, poly, color);
                case Value.ConicV con -> drawConic(g, w, h, con, color);
                case Value.LocusV loc -> drawLocus(g, w, h, loc, color);
                case Value.FunctionV ignored -> drawFunction(g, w, h, o, color);
                case Value.PointV p -> drawPoint(g, w, h, p, o.name(), color);
                case Value.ListV list -> drawList(g, w, h, list, color);
                case Value.TextV t -> drawText(g, w, h, t, color);
                case Value.ImplicitV im -> drawImplicit(g, w, h, im, color);
                default -> {
                    // 3D objects (plane/sphere/3D point/line) and matrices are not drawn here
                }
            }
        }
    }

    private void drawPoint(GraphicsContext g, double w, double h, Value.PointV p, String name, Color color) {
        if (p.is3D()) {
            return;
        }
        double px = sx(p.x(), w);
        double py = sy(p.y(), h);
        g.setFill(color);
        g.fillOval(px - 4.5, py - 4.5, 9, 9);
        g.setStroke(Color.web(Theme.BACKGROUND));
        g.setLineWidth(1.2);
        g.strokeOval(px - 4.5, py - 4.5, 9, 9);
        g.setFont(Font.font("System", FontWeight.BOLD, 12));
        haloLabel(g, name, px + 7, py - 7, Color.web(Theme.TEXT));
    }

    private void drawLine(GraphicsContext g, double w, double h, Value.LineV line, Color color) {
        g.setStroke(color);
        g.setLineWidth(2);
        if (Math.abs(line.b()) > 1e-12) {
            double yl = (line.c() - line.a() * vx0) / line.b();
            double yr = (line.c() - line.a() * vx1) / line.b();
            g.strokeLine(sx(vx0, w), sy(yl, h), sx(vx1, w), sy(yr, h));
        } else if (Math.abs(line.a()) > 1e-12) {
            double x = line.c() / line.a();
            g.strokeLine(sx(x, w), 0, sx(x, w), h);
        }
    }

    private void drawSegment(GraphicsContext g, double w, double h, Value.SegmentV s, Color color) {
        if (s.p().is3D() || s.q().is3D()) {
            return;
        }
        g.setStroke(color);
        g.setLineWidth(2.5);
        g.strokeLine(sx(s.p().x(), w), sy(s.p().y(), h), sx(s.q().x(), w), sy(s.q().y(), h));
    }

    private void drawCircle(GraphicsContext g, double w, double h, Value.CircleV circle, Color color) {
        double rpx = circle.r() * unitPx(w);
        double cx = sx(circle.center().x(), w);
        double cy = sy(circle.center().y(), h);
        g.setStroke(color);
        g.setLineWidth(2);
        g.strokeOval(cx - rpx, cy - rpx, 2 * rpx, 2 * rpx);
    }

    private void drawVector(GraphicsContext g, double w, double h, Value.VectorV v, Color color) {
        if (v.is3D()) {
            return;
        }
        double bx = v.base() != null ? v.base().x() : 0;
        double by = v.base() != null ? v.base().y() : 0;
        double x0 = sx(bx, w);
        double y0 = sy(by, h);
        double x1 = sx(bx + v.x(), w);
        double y1 = sy(by + v.y(), h);
        g.setStroke(color);
        g.setFill(color);
        g.setLineWidth(2);
        g.strokeLine(x0, y0, x1, y1);
        double angle = Math.atan2(y1 - y0, x1 - x0);
        double ah = 9;
        g.fillPolygon(
                new double[]{x1, x1 - ah * Math.cos(angle - 0.4), x1 - ah * Math.cos(angle + 0.4)},
                new double[]{y1, y1 - ah * Math.sin(angle - 0.4), y1 - ah * Math.sin(angle + 0.4)}, 3);
    }

    private void drawPolygon(GraphicsContext g, double w, double h, Value.PolygonV poly, Color color) {
        List<Value.PointV> vs = poly.vertices();
        double[] xs = new double[vs.size()];
        double[] ys = new double[vs.size()];
        int n = 0;
        for (Value.PointV p : vs) {
            if (!p.is3D()) {
                xs[n] = sx(p.x(), w);
                ys[n] = sy(p.y(), h);
                n++;
            }
        }
        if (n < 3) {
            return;
        }
        double[] fx = Arrays.copyOf(xs, n);
        double[] fy = Arrays.copyOf(ys, n);
        g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.18));
        g.fillPolygon(fx, fy, n);
        g.setStroke(color);
        g.setLineWidth(2);
        g.strokePolygon(fx, fy, n);
    }

    private void drawLocus(GraphicsContext g, double w, double h, Value.LocusV loc, Color color) {
        List<Value.PointV> pts = loc.points();
        if (pts.size() < 2) {
            return;
        }
        g.setStroke(color);
        g.setLineWidth(2);
        g.beginPath();
        g.moveTo(sx(pts.get(0).x(), w), sy(pts.get(0).y(), h));
        for (int i = 1; i < pts.size(); i++) {
            g.lineTo(sx(pts.get(i).x(), w), sy(pts.get(i).y(), h));
        }
        g.stroke();
    }

    /** Draws a general conic's zero contour via marching squares. */
    private void drawConic(GraphicsContext g, double w, double h, Value.ConicV con, Color color) {
        g.setStroke(color);
        g.setLineWidth(2);
        int n = 130;
        double dx = (vx1 - vx0) / n;
        double dy = (vy1 - vy0) / n;
        // Sample the conic once per grid corner; each interior corner is shared by up to
        // four cells, so caching avoids ~4× redundant evaluations and seam artefacts.
        double[][] f = new double[n + 1][n + 1];
        for (int i = 0; i <= n; i++) {
            double x = vx0 + i * dx;
            for (int j = 0; j <= n; j++) {
                f[i][j] = conicValue(con, x, vy0 + j * dy);
            }
        }
        for (int i = 0; i < n; i++) {
            double x = vx0 + i * dx;
            for (int j = 0; j < n; j++) {
                marchCell(g, w, h, x, vy0 + j * dy, dx, dy, f[i][j], f[i + 1][j], f[i + 1][j + 1], f[i][j + 1]);
            }
        }
    }

    private void marchCell(GraphicsContext g, double w, double h, double x, double y, double dx, double dy,
            double f00, double f10, double f11, double f01) {
        List<double[]> pts = new ArrayList<>(4);
        crossing(pts, x, y, f00, x + dx, y, f10);
        crossing(pts, x + dx, y, f10, x + dx, y + dy, f11);
        crossing(pts, x + dx, y + dy, f11, x, y + dy, f01);
        crossing(pts, x, y + dy, f01, x, y, f00);
        if (pts.size() >= 2) {
            g.strokeLine(sx(pts.get(0)[0], w), sy(pts.get(0)[1], h), sx(pts.get(1)[0], w), sy(pts.get(1)[1], h));
        }
        if (pts.size() == 4) {
            g.strokeLine(sx(pts.get(2)[0], w), sy(pts.get(2)[1], h), sx(pts.get(3)[0], w), sy(pts.get(3)[1], h));
        }
    }

    private static void crossing(List<double[]> pts, double xa, double ya, double fa,
            double xb, double yb, double fb) {
        if ((fa <= 0 && fb > 0) || (fa > 0 && fb <= 0)) {
            double t = fa / (fa - fb);
            pts.add(new double[]{xa + t * (xb - xa), ya + t * (yb - ya)});
        }
    }

    private static double conicValue(Value.ConicV c, double x, double y) {
        return c.a() * x * x + c.b() * x * y + c.c() * y * y + c.d() * x + c.e() * y + c.f();
    }

    /**
     * Draws the zero contour of an implicit equation {@code expr(x,y) = 0} via marching
     * squares. The expression is evaluated live against the construction (so it tracks
     * any referenced objects), with {@code x}/{@code y} bound to each grid corner.
     */
    private void drawImplicit(GraphicsContext g, double w, double h, Value.ImplicitV im, Color color) {
        double[] xy = new double[2];
        NameResolver scope = name -> switch (name) {
            case "x" -> new Value.NumberV(xy[0]);
            case "y" -> new Value.NumberV(xy[1]);
            default -> engine.construction().valueOf(name);
        };
        Evaluator ev = new Evaluator(scope, engine.construction().builtins());
        g.setStroke(color);
        g.setLineWidth(2);
        int n = 140;
        double dx = (vx1 - vx0) / n;
        double dy = (vy1 - vy0) / n;
        double[][] f = new double[n + 1][n + 1];
        for (int i = 0; i <= n; i++) {
            double x = vx0 + i * dx;
            for (int j = 0; j <= n; j++) {
                xy[0] = x;
                xy[1] = vy0 + j * dy;
                f[i][j] = implicitValue(ev, im.expr());
            }
        }
        for (int i = 0; i < n; i++) {
            double x = vx0 + i * dx;
            for (int j = 0; j < n; j++) {
                marchCell(g, w, h, x, vy0 + j * dy, dx, dy, f[i][j], f[i + 1][j], f[i + 1][j + 1], f[i][j + 1]);
            }
        }
    }

    private static double implicitValue(Evaluator ev, Expr expr) {
        try {
            return ev.eval(expr) instanceof Value.NumberV n ? n.v() : Double.NaN;
        } catch (RuntimeException e) {
            return Double.NaN;   // undefined here (e.g. division by zero) → no contour crossing
        }
    }

    private void drawFunction(GraphicsContext g, double w, double h, GeoObject f, Color color) {
        // Read the arity from the value, not the object's declaration: a derived function
        // (e.g. g = Derivative(f)) holds a 1-arg FunctionV but has no f(x)= parameter list.
        if (!(f.value() instanceof Value.FunctionV fn) || fn.params().size() != 1) {
            return;   // multivariate functions are drawn in 3D
        }
        Construction c = engine.construction();
        g.setStroke(color);
        g.setLineWidth(2);
        double yJump = (vy1 - vy0) * 2;
        boolean penDown = false;
        double prevY = 0;
        g.beginPath();
        for (int i = 0; i <= SAMPLES; i++) {
            double x = vx0 + (vx1 - vx0) * i / SAMPLES;
            double y;
            try {
                Value v = c.evaluate(new Expr.Call(f.name(), List.of(new Expr.Num(x))));
                if (!(v instanceof Value.NumberV n) || !Double.isFinite(n.v())) {
                    penDown = false;
                    continue;
                }
                y = n.v();
            } catch (RuntimeException e) {
                penDown = false;
                continue;
            }
            if (penDown && Math.abs(y - prevY) > yJump) {
                penDown = false;
            }
            double px = sx(x, w);
            double py = sy(y, h);
            if (penDown) {
                g.lineTo(px, py);
            } else {
                g.moveTo(px, py);
                penDown = true;
            }
            prevY = y;
        }
        g.stroke();
    }

    private void drawList(GraphicsContext g, double w, double h, Value.ListV list, Color color) {
        for (Value item : list.items()) {
            switch (item) {
                case Value.PointV p -> {
                    if (!p.is3D()) {
                        g.setFill(color);
                        g.fillOval(sx(p.x(), w) - 3.5, sy(p.y(), h) - 3.5, 7, 7);
                    }
                }
                case Value.SegmentV s -> drawSegment(g, w, h, s, color);
                default -> {
                    // non-plottable list items (numbers, nested lists) live in the list/log only
                }
            }
        }
    }

    private void drawText(GraphicsContext g, double w, double h, Value.TextV t, Color color) {
        if (t.anchor() == null || t.anchor().is3D()) {
            return;   // unanchored text shows in the algebra list only
        }
        g.setFont(Font.font("System", FontWeight.BOLD, 14));
        haloLabel(g, t.text(), sx(t.anchor().x(), w) + 4, sy(t.anchor().y(), h) - 4, color);
    }

    private Color colorFor(GeoObject o, int index) {
        return GeoPalette.color(o, index);
    }

    // ---- interaction -----------------------------------------------------

    private void installMouseHandlers() {
        canvas.setOnMousePressed(e -> {
            pressX = e.getX();
            pressY = e.getY();
            dragName = pickPoint(e.getX(), e.getY());
            if (dragName != null) {
                engine.beginInteraction();
                panning = false;
            } else {
                panning = true;
                sVx0 = vx0;
                sVx1 = vx1;
                sVy0 = vy0;
                sVy1 = vy1;
            }
        });
        canvas.setOnMouseDragged(e -> {
            double w = canvas.getWidth();
            double h = canvas.getHeight();
            if (dragName != null) {
                double worldX = wx(e.getX(), w);
                double worldY = wy(e.getY(), h);
                if (Settings.snapToGrid || e.isShiftDown()) {
                    double step = niceStep((vx1 - vx0) / 10);
                    worldX = Math.round(worldX / step) * step;
                    worldY = Math.round(worldY / step) * step;
                }
                engine.setFreeValueLive(dragName, Value.PointV.of2D(worldX, worldY));
            } else if (panning) {
                double dx = (e.getX() - pressX) / w * (sVx1 - sVx0);
                double dy = (e.getY() - pressY) / h * (sVy1 - sVy0);
                vx0 = sVx0 - dx;
                vx1 = sVx1 - dx;
                vy0 = sVy0 + dy;
                vy1 = sVy1 + dy;
                redraw();
            }
        });
        canvas.setOnMouseReleased(e -> {
            dragName = null;
            panning = false;
        });
        canvas.setOnScroll(e -> zoom(e.getDeltaY() > 0 ? 1.1 : 1 / 1.1));
    }

    private String pickPoint(double px, double py) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        for (GeoObject o : engine.construction().objects()) {
            if (o.isDraggable() && o.isVisible() && o.value() instanceof Value.PointV p && !p.is3D()) {
                if (Math.hypot(sx(p.x(), w) - px, sy(p.y(), h) - py) <= HIT_RADIUS) {
                    return o.name();
                }
            }
        }
        return null;
    }

    // ---- helpers ---------------------------------------------------------

    private static double niceStep(double raw) {
        return Plane2DMath.niceStep(raw);
    }

    private static String trim(double v) {
        return Plane2DMath.trim(v);
    }
}
