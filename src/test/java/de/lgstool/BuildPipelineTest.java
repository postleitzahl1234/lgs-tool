package de.lgstool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.lgstool.config.Theme;
import org.junit.jupiter.api.Test;

/**
 * Phase 0 smoke test: proves compilation, the test runner, and basic wiring work
 * before any real features are added.
 */
class BuildPipelineTest {

    @Test
    void testRunnerExecutes() {
        assertEquals(4, 2 + 2);
    }

    @Test
    void themeExposesSpecColors() {
        assertEquals("#E8E6DF", Theme.BACKGROUND);
        assertEquals("#1F5E63", Theme.ACCENT_PETROL);
        assertEquals("#8FAF9A", Theme.ACCENT_SAGE);
        assertEquals("#1A1A1A", Theme.TEXT);
    }
}
