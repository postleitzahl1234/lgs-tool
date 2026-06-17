package de.lgstool.ui;

import de.lgstool.config.I18n;
import de.lgstool.config.Theme;
import de.lgstool.math.Fraction;
import de.lgstool.math.Roman;
import de.lgstool.model.Lgs;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Virtualized matrix view ("Glasscheibe"): only the visible cells are drawn on a
 * canvas, with frozen variable-name and roman-numeral headers. The user pans the
 * viewport by dragging or scrolling, and edits a value by clicking it. This keeps
 * very large systems (up to 100×100 and beyond) fast and responsive.
 */
public final class MatrixView extends Pane {

    /** Receives an edited cell; the controller validates and applies it. */
    public interface CellEditor {
        void edit(int rowOneBased, int colZeroBased, String text);
    }

    private static final double CELL_W = 82;
    private static final double CELL_H = 30;
    private static final double HEADER_W = 64;
    private static final double HEADER_H = 34;

    private final Canvas canvas = new Canvas();
    private final TextField editor = new TextField();
    private final CellEditor cellEditor;

    private Lgs lgs;
    private double offX;
    private double offY;
    private double dragStartX;
    private double dragStartY;
    private double startOffX;
    private double startOffY;
    private boolean dragging;
    private int editRow = -1;
    private int editCol = -1;

    public MatrixView(CellEditor editor) {
        this.cellEditor = editor;
        setStyle("-fx-background-color: " + Theme.BACKGROUND + ";");
        getChildren().addAll(canvas, this.editor);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        this.editor.setVisible(false);
        this.editor.setStyle("-fx-font-family: 'Consolas', monospace;");
        this.editor.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        this.editor.focusedProperty().addListener((o, was, isNow) -> {
            if (was && !isNow) {
                commitEdit();
            }
        });
        this.editor.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ESCAPE -> {
                    cancelEdit();
                    e.consume();
                }
                case ENTER -> {
                    commitMoveTo(editRow + (e.isShiftDown() ? -1 : 1), editCol);
                    e.consume();
                }
                case TAB -> {
                    moveTab(e.isShiftDown() ? -1 : 1);
                    e.consume();
                }
                default -> {
                }
            }
        });

        widthProperty().addListener((o, a, b) -> {
            canvas.setWidth(getWidth());
            redraw();
        });
        heightProperty().addListener((o, a, b) -> {
            canvas.setHeight(getHeight());
            redraw();
        });

        canvas.setOnMousePressed(e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
            startOffX = offX;
            startOffY = offY;
            dragging = false;
        });
        canvas.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
                dragging = true;
            }
            offX = clamp(startOffX - dx, 0, maxOffX());
            offY = clamp(startOffY - dy, 0, maxOffY());
            cancelEdit();
            redraw();
        });
        canvas.setOnMouseClicked(e -> {
            if (!dragging) {
                handleClick(e.getX(), e.getY());
            }
        });
        canvas.setOnScroll(e -> {
            offX = clamp(offX - e.getDeltaX(), 0, maxOffX());
            offY = clamp(offY - e.getDeltaY(), 0, maxOffY());
            cancelEdit();
            redraw();
        });
    }

    public void resetPan() {
        offX = 0;
        offY = 0;
    }

    public void render(Lgs lgs) {
        this.lgs = lgs;
        cancelEdit();
        offX = clamp(offX, 0, maxOffX());
        offY = clamp(offY, 0, maxOffY());
        redraw();
    }

    // ---- geometry --------------------------------------------------------

    private double bodyW() {
        return Math.max(0, canvas.getWidth() - HEADER_W);
    }

    private double bodyH() {
        return Math.max(0, canvas.getHeight() - HEADER_H);
    }

    private int totalCols() {
        return lgs == null ? 0 : lgs.width() + 1;
    }

    private double maxOffX() {
        return Math.max(0, totalCols() * CELL_W - bodyW());
    }

    private double maxOffY() {
        return Math.max(0, (lgs == null ? 0 : lgs.height()) * CELL_H - bodyH());
    }

    // ---- editing ---------------------------------------------------------

    private void handleClick(double px, double py) {
        if (lgs == null || px < HEADER_W || py < HEADER_H) {
            return;
        }
        int c = (int) Math.floor((px - HEADER_W + offX) / CELL_W);
        int r = (int) Math.floor((py - HEADER_H + offY) / CELL_H);
        if (c < 0 || c >= totalCols() || r < 0 || r >= lgs.height()) {
            return;
        }
        startEdit(r, c);
    }

    private void startEdit(int r, int c) {
        editRow = r;
        editCol = c;
        double x = Math.max(HEADER_W, HEADER_W + c * CELL_W - offX);
        double y = Math.max(HEADER_H, HEADER_H + r * CELL_H - offY);
        editor.resizeRelocate(x, y, CELL_W, CELL_H);
        Fraction val = lgs.row(r + 1)[c];
        editor.setText(lgs.isFractionDisplay() ? val.toString() : val.toDecimalString(6));
        editor.setVisible(true);
        editor.requestFocus();
        editor.selectAll();
    }

    private void commitEdit() {
        if (editRow < 0) {
            return;
        }
        int r = editRow;
        int c = editCol;
        String text = editor.getText().trim();
        editRow = -1;
        editCol = -1;
        editor.setVisible(false);
        if (!text.isEmpty()) {
            cellEditor.edit(r + 1, c, text);
        }
    }

    private void cancelEdit() {
        editRow = -1;
        editCol = -1;
        editor.setVisible(false);
    }

    private void moveTab(int dir) {
        if (editRow < 0) {
            return;
        }
        int nc = editCol + dir;
        int nr = editRow;
        if (nc >= totalCols()) {
            nc = 0;
            nr = editRow + 1;
        } else if (nc < 0) {
            nc = totalCols() - 1;
            nr = editRow - 1;
        }
        commitMoveTo(nr, nc);
    }

    private void commitMoveTo(int nr, int nc) {
        if (editRow < 0) {
            return;
        }
        int r = editRow;
        int c = editCol;
        String text = editor.getText().trim();
        editRow = -1;
        editCol = -1;
        editor.setVisible(false);
        if (!text.isEmpty()) {
            cellEditor.edit(r + 1, c, text);
        }
        if (lgs != null && nr >= 0 && nr < lgs.height() && nc >= 0 && nc < totalCols()) {
            ensureVisible(nr, nc);
            redraw();
            startEdit(nr, nc);
        }
    }

    private void ensureVisible(int r, int c) {
        double left = c * CELL_W;
        double top = r * CELL_H;
        if (left < offX) {
            offX = left;
        } else if (left + CELL_W > offX + bodyW()) {
            offX = left + CELL_W - bodyW();
        }
        if (top < offY) {
            offY = top;
        } else if (top + CELL_H > offY + bodyH()) {
            offY = top + CELL_H - bodyH();
        }
        offX = clamp(offX, 0, maxOffX());
        offY = clamp(offY, 0, maxOffY());
    }

    // ---- drawing ---------------------------------------------------------

    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.setFill(Color.web(Theme.BACKGROUND));
        g.fillRect(0, 0, w, h);
        if (lgs == null || w <= 0) {
            return;
        }

        boolean frac = lgs.isFractionDisplay();
        int width = lgs.width();
        int height = lgs.height();
        int totalCols = width + 1;

        Font cellFont = Font.font("Consolas", 13);
        Font headFont = Font.font("Consolas", FontWeight.BOLD, 13);
        Color grid = Color.web(Theme.TEXT, 0.15);

        int c0 = (int) Math.floor(offX / CELL_W);
        int c1 = Math.min(totalCols - 1, (int) Math.floor((offX + bodyW()) / CELL_W));
        int r0 = (int) Math.floor(offY / CELL_H);
        int r1 = Math.min(height - 1, (int) Math.floor((offY + bodyH()) / CELL_H));

        // Body cells.
        g.setTextBaseline(VPos.CENTER);
        for (int r = r0; r <= r1 && height > 0; r++) {
            Fraction[] row = lgs.row(r + 1);
            double y = HEADER_H + r * CELL_H - offY;
            for (int c = c0; c <= c1; c++) {
                double x = HEADER_W + c * CELL_W - offX;
                g.setStroke(grid);
                g.setLineWidth(1);
                g.strokeRect(x, y, CELL_W, CELL_H);
                String s = frac ? row[c].toString() : row[c].toDecimalString(6);
                g.setFill(Color.web(Theme.TEXT));
                g.setTextAlign(TextAlignment.RIGHT);
                g.setFont(cellFont);
                g.fillText(s, x + CELL_W - 8, y + CELL_H / 2, CELL_W - 12);
            }
        }

        // Separator before the b column.
        double sepX = HEADER_W + width * CELL_W - offX;
        if (sepX >= HEADER_W && sepX <= w) {
            g.setStroke(Color.web(Theme.ACCENT_PETROL));
            g.setLineWidth(2);
            g.strokeLine(sepX, HEADER_H, sepX, h);
        }

        // Frozen top header (variable names + b).
        g.setFill(Color.web(Theme.ACCENT_SAGE, 0.35));
        g.fillRect(HEADER_W, 0, w - HEADER_W, HEADER_H);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(headFont);
        for (int c = c0; c <= c1; c++) {
            double x = HEADER_W + c * CELL_W - offX;
            String label = c < width ? lgs.variableNames()[c] : "b";
            g.setFill(Color.web(Theme.ACCENT_PETROL));
            g.fillText(label, x + CELL_W / 2, HEADER_H / 2, CELL_W - 6);
        }

        // Frozen left header (roman numerals).
        g.setFill(Color.web(Theme.ACCENT_SAGE, 0.35));
        g.fillRect(0, HEADER_H, HEADER_W, h - HEADER_H);
        g.setFill(Color.web(Theme.TEXT));
        for (int r = r0; r <= r1 && height > 0; r++) {
            double y = HEADER_H + r * CELL_H - offY;
            g.fillText(Roman.of(r + 1), HEADER_W / 2, y + CELL_H / 2, HEADER_W - 6);
        }

        // Top-left corner.
        g.setFill(Color.web(Theme.ACCENT_SAGE, 0.6));
        g.fillRect(0, 0, HEADER_W, HEADER_H);

        if (height == 0) {
            g.setFill(Color.web(Theme.TEXT));
            g.setTextAlign(TextAlignment.LEFT);
            g.setFont(cellFont);
            g.fillText(I18n.t("matrix.empty"), HEADER_W + 12, HEADER_H + 24);
        }

        // Frame around the glass pane.
        g.setStroke(Color.web(Theme.ACCENT_PETROL));
        g.setLineWidth(1.5);
        g.strokeRect(0.75, 0.75, w - 1.5, h - 1.5);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
