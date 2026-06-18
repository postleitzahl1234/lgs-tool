package de.lgstool.geo.command;

/**
 * Callbacks the {@link GeoEngine} uses to tell the UI the construction changed.
 * {@link #structureChanged()} fires when objects are added/removed/redefined (rebuild
 * the algebra list); {@link #valuesChanged()} fires on a drag or slider move (only the
 * plane needs to repaint). {@link #NONE} is a no-op for tests and headless runs.
 */
public interface GeoView {

    void structureChanged();

    void valuesChanged();

    GeoView NONE = new GeoView() {
        @Override
        public void structureChanged() {
        }

        @Override
        public void valuesChanged() {
        }
    };
}
