package de.lgstool.geo.command;

/**
 * Callback the {@link GeoEngine} uses to tell the UI the construction changed, so it
 * can re-render the plane and the algebra list. {@link #NONE} is a no-op used in
 * tests and headless runs.
 */
public interface GeoView {

    void constructionChanged();

    GeoView NONE = () -> {
    };
}
