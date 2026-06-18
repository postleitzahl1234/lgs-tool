package de.lgstool.geo.ui;

import de.lgstool.config.Theme;
import de.lgstool.geo.model.GeoObject;
import javafx.scene.paint.Color;

/** Shared object colouring so the plane and the algebra list agree on each object's colour. */
public final class GeoPalette {

    private static final Color[] COLORS = {
            Color.web(Theme.ACCENT_PETROL),
            Color.web("#C46A4E"),
            Color.web("#6A5ACD"),
            Color.web(Theme.ACCENT_SAGE),
            Color.web("#B59B2E"),
            Color.web("#3E7CB1")
    };

    private GeoPalette() {
    }

    public static Color color(GeoObject o, int index) {
        if (o.colorHex() != null) {
            try {
                return Color.web(o.colorHex());
            } catch (IllegalArgumentException ignored) {
                // fall through to palette
            }
        }
        return COLORS[Math.floorMod(index, COLORS.length)];
    }
}
