package de.lgstool.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class I18nTest {

    @AfterEach
    void resetToGerman() {
        I18n.setLanguage("de");
    }

    @Test
    void switchesBetweenLanguages() {
        I18n.setLanguage("de");
        assertEquals("Einstellungen", I18n.t("main.settings"));
        I18n.setLanguage("en");
        assertEquals("Settings", I18n.t("main.settings"));
    }

    @Test
    void unknownKeyFallsBackToKey() {
        assertEquals("does.not.exist", I18n.t("does.not.exist"));
    }

    @Test
    void formatsArguments() {
        I18n.setLanguage("de");
        String created = I18n.t("m.created", "test", 2, 3);
        assertTrue(created.contains("'test'"), created);
        assertTrue(created.contains("erstellt"), created);

        I18n.setLanguage("en");
        String createdEn = I18n.t("m.created", "test", 2, 3);
        assertTrue(createdEn.contains("created"), createdEn);
    }

    @Test
    void registersBothLanguages() {
        assertEquals(2, I18n.languages().size());
    }
}
