package de.lgstool.command;

import de.lgstool.math.Fraction;

/**
 * Callbacks from the command engine into the user interface. The engine stays
 * UI-agnostic and fully testable; a no-op implementation ({@link #NONE}) is used
 * in headless tests.
 */
public interface UiBridge {

    /** The set of systems changed (created, deleted, renamed or loaded). */
    void systemsChanged();

    /** Open the detail window for the named system. */
    void openWindow(String name);

    /** Close the detail window for the named system. */
    void closeWindow(String name);

    /** A system's matrix or display state changed; refresh its views. */
    void lgsChanged(String name);

    /** Highlight a row in the visualization (1-based); {@code hex} may be null for auto color. */
    void highlightRow(String name, int rowOneBased, String hex);

    /** Re-center the visualization on the whole system. */
    void fitView(String name);

    /** Start an animation of a right-hand side from {@code from} to {@code to}. */
    void animate(String name, int rowOneBased, Fraction from, Fraction to, double durationSeconds);

    /** Set a display axis range (axisIndex 0=x,1=y,2=z; -1 resets to automatic). */
    void setAxisRange(String name, int axisIndex, double min, double max);

    /** Copy arbitrary text to the system clipboard. */
    void copyToClipboard(String text);

    /** Copy the active log contents to the system clipboard. */
    void copyLog();

    /** Save the active log to the given file path. */
    void saveLog(String path);

    /** Save a snapshot of the coordinate system to the given image file path. */
    void savePlot(String path);

    UiBridge NONE = new UiBridge() {
        @Override
        public void systemsChanged() {
        }

        @Override
        public void openWindow(String name) {
        }

        @Override
        public void closeWindow(String name) {
        }

        @Override
        public void lgsChanged(String name) {
        }

        @Override
        public void highlightRow(String name, int rowOneBased, String hex) {
        }

        @Override
        public void fitView(String name) {
        }

        @Override
        public void animate(String name, int rowOneBased, Fraction from, Fraction to, double durationSeconds) {
        }

        @Override
        public void setAxisRange(String name, int axisIndex, double min, double max) {
        }

        @Override
        public void copyToClipboard(String text) {
        }

        @Override
        public void copyLog() {
        }

        @Override
        public void saveLog(String path) {
        }

        @Override
        public void savePlot(String path) {
        }
    };
}
