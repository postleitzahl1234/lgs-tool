package de.lgstool.geo.ui;

import de.lgstool.config.Theme;
import de.lgstool.geo.command.GeoEngine;
import de.lgstool.geo.expr.Expr;
import de.lgstool.geo.expr.Value;
import de.lgstool.geo.model.Construction;
import de.lgstool.geo.model.GeoObject;
import java.util.List;
import java.util.Locale;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
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
        return (x - vx0) / (vx1 - vx0) * w;
    }

    private double sy(double y, double h) {
        return h - (y - vy0) / (vy1 - vy0) * h;
    }

    private double wx(double px, double w) {
        return vx0 + px / w * (vx1 - vx0);
    }

    private double wy(double py, double h) {
        return vy0 + (h - py) / h * (vy1 - vy0);
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
        g.setFill(Color.web(Theme.TEXT, 0.6));
        g.setFont(Font.font("Consolas", 11));
        for (double x = Math.ceil(vx0 / step) * step; x <= vx1; x += step) {
            if (Math.abs(x) > 1e-9) {
                g.fillText(trim(x), sx(x, w) + 2, sy(0, h) + 12);
            }
        }
        for (double y = Math.ceil(vy0 / step) * step; y <= vy1; y += step) {
            if (Math.abs(y) > 1e-9) {
                g.fillText(trim(y), sx(0, w) + 4, sy(y, h) - 2);
            }
        }
    }

    // ---- objects ---------------------------------------------------------

    private void drawObjects(GraphicsContext g, double w, double h) {
        Construction c = engine.construction();
        List<GeoObject> objs = c.objects();
        for (int i = 0; i < objs.size(); i++) {
            GeoObject o = objs.get(i);
            if (!o.isVisible()) {
                continue;
            }
            Color color = colorFor(o, i);
            switch (o.value()) {
                case Value.LineV line -> drawLine(g, w, h, line, color);
                case Value.SegmentV s -> drawSegment(g, w, h, s, color);
                case Value.CircleV circle -> drawCircle(g, w, h, circle, color);
                case Value.VectorV v -> drawVector(g, w, h, v, color);
                case Value.FunctionV ignored -> drawFunction(g, w, h, o, color);
                case Value.PointV p -> drawPoint(g, w, h, p, o.name(), color);
                default -> {
                    // 3D objects (plane/sphere/3D point/line) are not drawn here
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
        g.setFill(Color.web(Theme.TEXT));
        g.setFont(Font.font("System", FontWeight.BOLD, 12));
        g.fillText(name, px + 7, py - 7);
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
        double x0 = sx(0, w);
        double y0 = sy(0, h);
        double x1 = sx(v.x(), w);
        double y1 = sy(v.y(), h);
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

    private void drawFunction(GraphicsContext g, double w, double h, GeoObject f, Color color) {
        if (f.params().size() != 1) {
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
                engine.setFreeValueLive(dragName, Value.PointV.of2D(wx(e.getX(), w), wy(e.getY(), h)));
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
}
