package de.lgstool.config;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight in-code internationalization. Strings live in per-language maps so
 * no resource files or JPMS {@code opens} are required. Adding a language:
 * register a {@link Language} in {@link #LANGUAGES} and add its map in {@code register}.
 */
public final class I18n {

    /** A selectable UI language. */
    public record Language(String code, String displayName) {
    }

    private static final List<Language> LANGUAGES = new ArrayList<>(List.of(
            new Language("de", "Deutsch"),
            new Language("en", "English")));

    private static final Map<String, Map<String, String>> STRINGS = new LinkedHashMap<>();

    private static String currentCode = "de";

    static {
        STRINGS.put("de", german());
        STRINGS.put("en", english());
    }

    private I18n() {
    }

    public static List<Language> languages() {
        return LANGUAGES;
    }

    public static String currentCode() {
        return currentCode;
    }

    public static void setLanguage(String code) {
        if (STRINGS.containsKey(code)) {
            currentCode = code;
        }
    }

    /** Translates {@code key}; substitutes {0}, {1}, ... when arguments are given. */
    public static String t(String key, Object... args) {
        String value = STRINGS.getOrDefault(currentCode, STRINGS.get("de")).getOrDefault(key, key);
        if (args.length == 0) {
            return value;
        }
        return MessageFormat.format(value, args);
    }

    private static Map<String, String> german() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("app.title", "LGS-Tool");
        m.put("main.systems", "Systeme");
        m.put("main.newLgs", "+ Neues LGS");
        m.put("main.settings", "Einstellungen");
        m.put("main.glossary", "Glossar");
        m.put("main.messages", "Meldungen");
        m.put("main.noSystems", "Noch keine LGS.\ncreate(name, b*h);");
        m.put("cmd.run", "Ausführen ▶");
        m.put("cmd.placeholder", "Befehl eingeben, z. B. solve();");
        m.put("viz.noLgs", "Kein LGS geöffnet. Erstelle eines mit create(name, breite*höhe).");
        m.put("viz.vars", "Var.");
        m.put("viz.empty", "leer");
        m.put("viz.unique", "eindeutig lösbar");
        m.put("viz.infinite", "unendlich viele Lösungen ({0} Parameter)");
        m.put("viz.none", "keine Lösung");
        m.put("viz.moreThan3", "{0}: mehr als 3 Variablen — keine grafische Darstellung.");
        m.put("lgs.title", "LGS: {0}");
        m.put("lgs.solve", "Lösen");
        m.put("lgs.check", "Probe");
        m.put("lgs.undo", "Rückgängig");
        m.put("lgs.fractions", "Brüche");
        m.put("lgs.decimals", "Dezimal");
        m.put("lgs.center", "Zentrieren");
        m.put("lgs.axes", "Achsen…");
        m.put("lgs.close", "Schließen");
        m.put("lgs.back", "← Übersicht");
        m.put("lgs.log", "Lösungen / Log");
        m.put("matrix.empty", "Leeres LGS – mit add(...) Gleichungen eintragen.");
        m.put("settings.title", "Einstellungen");
        m.put("settings.fullscreen", "Vollbild");
        m.put("settings.defaultDisplay", "Standard-Anzeige für neue LGS:");
        m.put("settings.fractions", "Brüche");
        m.put("settings.decimals", "Dezimalzahlen");
        m.put("settings.language", "Sprache");
        m.put("settings.darkMode", "Dunkles Design");
        m.put("settings.precision", "Dezimalstellen:");
        m.put("settings.about", "LGS-Tool · exakte Bruch-Arithmetik · 1D/2D/3D-Visualisierung");
        m.put("common.close", "Schließen");
        m.put("common.cancel", "Abbrechen");
        m.put("common.create", "Erstellen");
        m.put("common.apply", "Übernehmen");
        m.put("common.auto", "Automatisch");
        m.put("confirm.deleteTitle", "Löschen bestätigen");
        m.put("confirm.deleteText", "LGS ''{0}'' wirklich löschen?");
        m.put("new.title", "Neues LGS erstellen");
        m.put("new.name", "Name:");
        m.put("new.width", "Variablen (Breite):");
        m.put("new.height", "Zeilen (Höhe):");
        m.put("new.defaultPrefix", "lgs");
        m.put("glossary.title", "Glossar — Befehlsübersicht");
        m.put("glossary.header", "Alle Befehle. [HAUPT] = Hauptbildschirm, [LGS] = LGS-Fenster.");
        m.put("glossary.colCmd", "Befehl");
        m.put("glossary.colMeaning", "Bedeutung");
        m.put("glossary.colContext", "Kontext");
        m.put("axis.title", "Achsenbereiche");
        m.put("axis.hint", "Felder leer lassen oder „Automatisch“ für selbsttätige Skalierung.");
        m.put("axis.min", "min");
        m.put("axis.max", "max");
        m.put("hk.title", "Tastenkürzel");
        m.put("hk.colKey", "Taste");
        m.put("hk.colAction", "Aktion");
        m.put("hk.keyEnter", "Enter");
        m.put("hk.keyWheel", "Mausrad");
        m.put("hk.keyDrag", "Maus ziehen");
        m.put("hk.keyClick", "Klick");
        m.put("hk.enter", "Befehl ausführen");
        m.put("hk.f11", "Vollbild umschalten");
        m.put("hk.esc", "Vollbild verlassen");
        m.put("hk.wheel", "Koordinatensystem zoomen");
        m.put("hk.drag", "Ansicht / Matrix verschieben");
        m.put("hk.click", "Wert in der Matrix bearbeiten");
        germanGlossary(m);
        germanMessages(m);
        return m;
    }

    private static void germanMessages(Map<String, String> m) {
        m.put("m.errorPrefix", "Fehler: ");
        m.put("m.unknownCmd", "Unbekannter Befehl: ''{0}''.");
        m.put("m.nameEmpty", "Name darf nicht leer sein.");
        m.put("m.exists", "LGS ''{0}'' existiert bereits.");
        m.put("m.created", "LGS ''{0}'' erstellt ({1} Variablen, {2} Zeilen).");
        m.put("m.loaded", "Geladen: ''{0}''.");
        m.put("m.notExists", "LGS ''{0}'' existiert nicht.");
        m.put("m.opened", "LGS ''{0}'' geöffnet.");
        m.put("m.noSystems", "Keine LGS vorhanden.");
        m.put("m.systemsCount", "Erstellte LGS ({0}):");
        m.put("m.deletedSystem", "LGS ''{0}'' gelöscht.");
        m.put("m.rowNotExist", "Zeile {0} existiert nicht (1..{1}).");
        m.put("m.deletedRow", "Zeile {0} gelöscht.");
        m.put("m.nothingUndo", "Nichts rückgängig zu machen.");
        m.put("m.undone", "Letzte Änderung rückgängig gemacht.");
        m.put("m.addNeedsEq", "add erwartet eine Gleichung, z. B. add(1 -2 3 = 5).");
        m.put("m.eqNeedsEquals", "Gleichung braucht ein '=' , z. B. 1 -2 3 = 5.");
        m.put("m.wrongCoeffCount", "Falsche Anzahl Koeffizienten: erwartet {0}, erhalten {1}.");
        m.put("m.eqEntered", "Gleichung eingetragen.");
        m.put("m.rowOverwritten", "Zeile {0} überschrieben.");
        m.put("m.noEquations", "Das LGS hat keine Gleichungen.");
        m.put("m.steps", "Rechenschritte ({0} von {1}):");
        m.put("m.stepN", "Schritt {0}: {1}");
        m.put("m.result", "Ergebnis:");
        m.put("m.swapDone", "Zeile {0} ↔ Zeile {1}.");
        m.put("m.factorZero", "Faktor 0 würde die Zeile löschen – nicht erlaubt.");
        m.put("m.scaleDone", "Zeile {0} × {1}.");
        m.put("m.combineDone", "Zeile {0} := Zeile {0} + ({1}) · Zeile {2}.");
        m.put("m.detNotSquare", "Determinante nur für quadratische LGS (Zeilen = Variablen) – hier {0}×{1}.");
        m.put("m.detResult", "det = {0}");
        m.put("m.detZero", "→ Determinante 0: nicht eindeutig lösbar.");
        m.put("m.rankLine", "Rang(A) = {0},  Rang(A|b) = {1},  Variablen = {2}");
        m.put("m.rankNone", "→ Keine Lösung (inkonsistent).");
        m.put("m.rankUnique", "→ Eindeutig lösbar.");
        m.put("m.rankInfinite", "→ Unendlich viele Lösungen ({0} freie Parameter).");
        m.put("m.normNeeds", "Norm muss 1, 2 oder leer (∞) sein.");
        m.put("m.condNotSquare", "Konditionszahl nur für quadratische LGS.");
        m.put("m.condResult", "cond({0}) ≈ {1}");
        m.put("m.normInf", "∞-Norm");
        m.put("m.norm1", "1-Norm");
        m.put("m.norm2", "2-Norm");
        m.put("m.matrixOf", "Matrix von ''{0}'' [{1} | b]:");
        m.put("m.emptyParen", "  (leer)");
        m.put("m.cleared", "Alle Werte auf 0 gesetzt.");
        m.put("m.valueSet", "Wert gesetzt.");
        m.put("m.newNameEmpty", "Neuer Name darf nicht leer sein.");
        m.put("m.renamed", "Umbenannt in ''{0}''.");
        m.put("m.highlightNeedsRow", "highlight erwartet eine Zeile.");
        m.put("m.badHex", "Ungültiger Farbcode: ''{0}'' (erwarte z. B. #1F5E63).");
        m.put("m.highlighted", "Zeile {0} hervorgehoben.");
        m.put("m.resized", "Größe geändert auf {0}×{1}.");
        m.put("m.resizeDiscard", "Warnung: abgeschnittene Werte wurden verworfen.");
        m.put("m.varNameEmpty", "Variablenname darf nicht leer sein.");
        m.put("m.colRenamed", "Spalte {0} heißt jetzt ''{1}''.");
        m.put("m.saved", "Gespeichert: {0}");
        m.put("m.windowClosed", "LGS-Fenster geschlossen.");
        m.put("m.centered", "Ansicht zentriert.");
        m.put("m.noSolutionCheck", "Keine Lösung vorhanden – nichts zu prüfen.");
        m.put("m.verifyInfinite", "Unendlich viele Lösungen – Probe mit freien Parametern = 0:");
        m.put("m.verifyHeader", "Probe der Lösung:");
        m.put("m.rowLabel", "Zeile {0}");
        m.put("m.allSatisfied", "Alle Gleichungen erfüllt.");
        m.put("m.notAllSatisfied", "Nicht alle Gleichungen erfüllt.");
        m.put("m.inverseNotSquare", "Inverse nur für quadratische LGS.");
        m.put("m.inverseHeader", "Inverse der Koeffizientenmatrix:");
        m.put("m.varNotFound", "Variable ''{0}'' nicht gefunden.");
        m.put("m.substAdded", "Variable ''{0}'' = {1} als Gleichung ergänzt.");
        m.put("m.displayFrac", "Anzeige: Brüche.");
        m.put("m.displayDec", "Anzeige: Dezimalzahlen.");
        m.put("m.animateArgs", "animate(zeile, von, bis [, dauer]).");
        m.put("m.durNumber", "Dauer muss eine Zahl (Sekunden) sein.");
        m.put("m.durPositive", "Dauer muss positiv sein.");
        m.put("m.animateStarted", "Animation von Zeile {0}: {1} → {2}.");
        m.put("m.axisAuto", "Achsen auf automatische Skalierung gesetzt.");
        m.put("m.axisArgs", "axis(achse, min, max) oder axis() zum Zurücksetzen.");
        m.put("m.axisMaxMin", "max muss größer als min sein.");
        m.put("m.axisSet", "Achse {0} auf [{1}, {2}] gesetzt.");
        m.put("m.axisUnknown", "Achse ''{0}'' nicht verfügbar (x, y{1} oder Variablenname).");
        m.put("m.randArgs", "rand(min, max [, zeile]).");
        m.put("m.randRange", "max muss ≥ min sein.");
        m.put("m.randFilled", "Matrix mit Zufallszahlen in [{0}, {1}] gefüllt.");
        m.put("m.redone", "Wiederhergestellt.");
        m.put("m.nothingRedo", "Nichts wiederherzustellen.");
        m.put("m.rrefHeader", "Reduzierte Zeilenstufenform (RREF):");
        m.put("m.refHeader", "Zeilenstufenform (REF):");
        m.put("m.transposeHeader", "Transponierte Koeffizientenmatrix:");
        m.put("m.traceNotSquare", "Spur nur für quadratische LGS.");
        m.put("m.traceResult", "Spur = {0}");
        m.put("m.nullspaceTrivial", "Nur die triviale Lösung (Nullvektor).");
        m.put("m.nullspaceHeader", "Basis des Nullraums ({0} Vektoren):");
        m.put("m.cramerNotRegular", "Cramersche Regel nur für reguläre quadratische LGS (det ≠ 0).");
        m.put("m.cramerHeader", "Cramersche Regel (det A = {0}):");
        m.put("m.latexCopied", "(LaTeX in die Zwischenablage kopiert)");
        m.put("m.csvSaved", "CSV gespeichert: {0}");
        m.put("m.csvLoaded", "CSV geladen: ''{0}''.");
        m.put("m.csvBadFormat", "Ungültiges CSV-Format.");
        m.put("m.fileNotFound", "Datei nicht gefunden: {0}");
        m.put("m.runFile", "Skript ausgeführt: {0}");
        m.put("m.examplesHeader", "Verfügbare Beispiele (examples(name)):");
        m.put("m.exampleLoaded", "Beispiel ''{0}'' geladen.");
        m.put("m.exampleUnknown", "Unbekanntes Beispiel: ''{0}''.");
        m.put("m.logCopied", "Log in die Zwischenablage kopiert.");
        m.put("m.logSaved", "Log gespeichert: {0}");
        m.put("m.plotSaved", "Diagramm gespeichert: {0}");
        m.put("m.labelsToggled", "Achsenbeschriftung umgeschaltet.");
        m.put("m.expectsArgs", "''{0}'' erwartet {1} Argument(e), erhalten {2}.");
        m.put("m.notInt", "{0} muss eine ganze Zahl sein: ''{1}''.");
        m.put("m.notNumber", "{0} ist keine gültige Zahl: ''{1}''.");
        m.put("m.dims", "Dimensionen als breite*höhe angeben, z. B. 3*3.");
        m.put("m.onlyIn", "Befehl ''{0}'' ist nur im {1} verfügbar.");
        m.put("m.onlyLgs", "Dieser Befehl ist nur im LGS-Fenster verfügbar.");
        m.put("m.noOpenLgs", "Kein geöffnetes LGS.");
        m.put("m.mainScreen", "Hauptbildschirm");
        m.put("m.lgsWindow", "LGS-Fenster");
        m.put("s.none", "Keine Lösung – das System ist nicht erfüllbar (inkonsistent).");
        m.put("s.unique", "Eindeutige Lösung:");
        m.put("s.infinite", "Unendlich viele Lösungen ({0} freie Parameter):");
        m.put("s.free", "(frei)");
        m.put("w.width", "Breite");
        m.put("w.height", "Höhe");
        m.put("w.row", "Zeile");
        m.put("w.rowI", "Zeile i");
        m.put("w.rowJ", "Zeile j");
        m.put("w.factor", "Faktor");
        m.put("w.steps", "Schrittzahl");
        m.put("w.norm", "Norm");
        m.put("w.column", "Spalte");
        m.put("w.rhs", "rechte Seite");
        m.put("w.coeff", "Koeffizient {0}");
        m.put("w.value", "Wert");
        m.put("w.from", "von");
        m.put("w.to", "bis");
        m.put("w.min", "min");
        m.put("w.max", "max");
    }

    private static void germanGlossary(Map<String, String> m) {
        m.put("g.create", "Neues LGS + Button. Breite = Variablen, Höhe = Zeilen.");
        m.put("g.add", "Gleichung eintragen / überschreiben.");
        m.put("g.solve", "Löst das LGS; n = vorgerechnete Schritte (optional).");
        m.put("g.del", "del(name) löscht LGS; del(zahl) löscht Zeile.");
        m.put("g.swap", "Vertauscht Zeile i und j.");
        m.put("g.scale", "Multipliziert Zeile i mit Faktor.");
        m.put("g.combine", "Zeile i := Zeile i + Faktor · Zeile j.");
        m.put("g.det", "Determinante (nur quadratisch).");
        m.put("g.rank", "Rang(A) und Rang(A|b) + Klassifikation.");
        m.put("g.cond", "Konditionszahl (∞-Norm, optional 1 oder 2).");
        m.put("g.show", "Schreibt die Matrix als Text ins Log.");
        m.put("g.clear", "Setzt alle Werte auf 0.");
        m.put("g.undo", "Letzte Änderung rückgängig (mehrstufig).");
        m.put("g.rename", "Benennt das aktuelle LGS um.");
        m.put("g.highlight", "Hebt Zeile i im Koordinatensystem hervor.");
        m.put("g.resize", "Ändert die Dimensionen.");
        m.put("g.setvar", "Benennt eine Variablen-Spalte um.");
        m.put("g.save", "Sichert das LGS als ./lgs/name.lgs.");
        m.put("g.load", "Lädt ein gespeichertes LGS.");
        m.put("g.open", "Öffnet das LGS-Fenster.");
        m.put("g.close", "Schließt das aktuelle LGS-Fenster.");
        m.put("g.list", "Listet alle erstellten LGS auf.");
        m.put("g.fit", "Zentriert die Ansicht auf das LGS.");
        m.put("g.verify", "Setzt die Lösung ein und prüft sie.");
        m.put("g.inverse", "Inverse der Koeffizientenmatrix.");
        m.put("g.subst", "Setzt eine Variable fest.");
        m.put("g.frac", "Anzeige Bruch ↔ Dezimal.");
        m.put("g.animate", "Animiert die rechte Seite einer Zeile.");
        m.put("g.axis", "Setzt den Anzeigebereich einer Achse (oder leer = automatisch).");
        m.put("g.rand", "Füllt die Matrix mit zufälligen ganzen Zahlen in [min, max].");
        m.put("g.redo", "Wiederholt die letzte rückgängig gemachte Änderung.");
        m.put("g.rref", "Zeigt die reduzierte Zeilenstufenform.");
        m.put("g.ref", "Zeigt die Zeilenstufenform.");
        m.put("g.transpose", "Zeigt die transponierte Koeffizientenmatrix.");
        m.put("g.trace", "Spur (Summe der Diagonale, quadratisch).");
        m.put("g.nullspace", "Basis des Nullraums (Kern) der Matrix.");
        m.put("g.cramer", "Löst quadratische LGS mit der Cramerschen Regel.");
        m.put("g.latex", "Gibt die Matrix als LaTeX aus (Zwischenablage).");
        m.put("g.csv", "Exportiert das LGS als CSV-Datei.");
        m.put("g.loadcsv", "Lädt ein LGS aus einer CSV-Datei.");
        m.put("g.run", "Führt Befehle aus einer Datei aus.");
        m.put("g.examples", "Listet bzw. lädt Beispiel-LGS.");
        m.put("g.savelog", "Speichert das Log in eine Datei.");
        m.put("g.copylog", "Kopiert das Log in die Zwischenablage.");
        m.put("g.saveplot", "Speichert das Koordinatensystem als PNG.");
        m.put("g.labels", "Schaltet die Achsenbeschriftung (Zahlen + Namen) um.");
    }

    private static Map<String, String> english() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("app.title", "LGS-Tool");
        m.put("main.systems", "Systems");
        m.put("main.newLgs", "+ New system");
        m.put("main.settings", "Settings");
        m.put("main.glossary", "Glossary");
        m.put("main.messages", "Messages");
        m.put("main.noSystems", "No systems yet.\ncreate(name, w*h);");
        m.put("cmd.run", "Run ▶");
        m.put("cmd.placeholder", "Enter a command, e.g. solve();");
        m.put("viz.noLgs", "No system open. Create one with create(name, width*height).");
        m.put("viz.vars", "vars");
        m.put("viz.empty", "empty");
        m.put("viz.unique", "unique solution");
        m.put("viz.infinite", "infinitely many solutions ({0} parameters)");
        m.put("viz.none", "no solution");
        m.put("viz.moreThan3", "{0}: more than 3 variables — no graphical display.");
        m.put("lgs.title", "System: {0}");
        m.put("lgs.solve", "Solve");
        m.put("lgs.check", "Verify");
        m.put("lgs.undo", "Undo");
        m.put("lgs.fractions", "Fractions");
        m.put("lgs.decimals", "Decimals");
        m.put("lgs.center", "Center");
        m.put("lgs.axes", "Axes…");
        m.put("lgs.close", "Close");
        m.put("lgs.back", "← Overview");
        m.put("lgs.log", "Solutions / Log");
        m.put("matrix.empty", "Empty system – add equations with add(...).");
        m.put("settings.title", "Settings");
        m.put("settings.fullscreen", "Fullscreen");
        m.put("settings.defaultDisplay", "Default display for new systems:");
        m.put("settings.fractions", "Fractions");
        m.put("settings.decimals", "Decimals");
        m.put("settings.language", "Language");
        m.put("settings.darkMode", "Dark theme");
        m.put("settings.precision", "Decimal digits:");
        m.put("settings.about", "LGS-Tool · exact fraction arithmetic · 1D/2D/3D visualization");
        m.put("common.close", "Close");
        m.put("common.cancel", "Cancel");
        m.put("common.create", "Create");
        m.put("common.apply", "Apply");
        m.put("common.auto", "Automatic");
        m.put("confirm.deleteTitle", "Confirm deletion");
        m.put("confirm.deleteText", "Really delete system ''{0}''?");
        m.put("new.title", "Create new system");
        m.put("new.name", "Name:");
        m.put("new.width", "Variables (width):");
        m.put("new.height", "Rows (height):");
        m.put("new.defaultPrefix", "matrix");
        m.put("glossary.title", "Glossary — Command reference");
        m.put("glossary.header", "All commands. [HAUPT] = main screen, [LGS] = system window.");
        m.put("glossary.colCmd", "Command");
        m.put("glossary.colMeaning", "Meaning");
        m.put("glossary.colContext", "Context");
        m.put("axis.title", "Axis ranges");
        m.put("axis.hint", "Leave fields empty or use \"Automatic\" for auto scaling.");
        m.put("axis.min", "min");
        m.put("axis.max", "max");
        m.put("hk.title", "Hotkeys");
        m.put("hk.colKey", "Key");
        m.put("hk.colAction", "Action");
        m.put("hk.keyEnter", "Enter");
        m.put("hk.keyWheel", "Mouse wheel");
        m.put("hk.keyDrag", "Mouse drag");
        m.put("hk.keyClick", "Click");
        m.put("hk.enter", "Run command");
        m.put("hk.f11", "Toggle fullscreen");
        m.put("hk.esc", "Exit fullscreen");
        m.put("hk.wheel", "Zoom the coordinate system");
        m.put("hk.drag", "Pan the view / matrix");
        m.put("hk.click", "Edit a value in the matrix");
        englishGlossary(m);
        englishMessages(m);
        return m;
    }

    private static void englishMessages(Map<String, String> m) {
        m.put("m.errorPrefix", "Error: ");
        m.put("m.unknownCmd", "Unknown command: ''{0}''.");
        m.put("m.nameEmpty", "Name must not be empty.");
        m.put("m.exists", "System ''{0}'' already exists.");
        m.put("m.created", "System ''{0}'' created ({1} variables, {2} rows).");
        m.put("m.loaded", "Loaded: ''{0}''.");
        m.put("m.notExists", "System ''{0}'' does not exist.");
        m.put("m.opened", "System ''{0}'' opened.");
        m.put("m.noSystems", "No systems.");
        m.put("m.systemsCount", "Created systems ({0}):");
        m.put("m.deletedSystem", "System ''{0}'' deleted.");
        m.put("m.rowNotExist", "Row {0} does not exist (1..{1}).");
        m.put("m.deletedRow", "Row {0} deleted.");
        m.put("m.nothingUndo", "Nothing to undo.");
        m.put("m.undone", "Last change undone.");
        m.put("m.addNeedsEq", "add expects an equation, e.g. add(1 -2 3 = 5).");
        m.put("m.eqNeedsEquals", "Equation needs a '=' , e.g. 1 -2 3 = 5.");
        m.put("m.wrongCoeffCount", "Wrong number of coefficients: expected {0}, got {1}.");
        m.put("m.eqEntered", "Equation entered.");
        m.put("m.rowOverwritten", "Row {0} overwritten.");
        m.put("m.noEquations", "The system has no equations.");
        m.put("m.steps", "Computation steps ({0} of {1}):");
        m.put("m.stepN", "Step {0}: {1}");
        m.put("m.result", "Result:");
        m.put("m.swapDone", "Row {0} ↔ Row {1}.");
        m.put("m.factorZero", "Factor 0 would delete the row – not allowed.");
        m.put("m.scaleDone", "Row {0} × {1}.");
        m.put("m.combineDone", "Row {0} := Row {0} + ({1}) · Row {2}.");
        m.put("m.detNotSquare", "Determinant only for square systems (rows = variables) – here {0}×{1}.");
        m.put("m.detResult", "det = {0}");
        m.put("m.detZero", "→ Determinant 0: not uniquely solvable.");
        m.put("m.rankLine", "rank(A) = {0},  rank(A|b) = {1},  variables = {2}");
        m.put("m.rankNone", "→ No solution (inconsistent).");
        m.put("m.rankUnique", "→ Uniquely solvable.");
        m.put("m.rankInfinite", "→ Infinitely many solutions ({0} free parameters).");
        m.put("m.normNeeds", "Norm must be 1, 2 or empty (∞).");
        m.put("m.condNotSquare", "Condition number only for square systems.");
        m.put("m.condResult", "cond({0}) ≈ {1}");
        m.put("m.normInf", "∞-norm");
        m.put("m.norm1", "1-norm");
        m.put("m.norm2", "2-norm");
        m.put("m.matrixOf", "Matrix of ''{0}'' [{1} | b]:");
        m.put("m.emptyParen", "  (empty)");
        m.put("m.cleared", "All values set to 0.");
        m.put("m.valueSet", "Value set.");
        m.put("m.newNameEmpty", "New name must not be empty.");
        m.put("m.renamed", "Renamed to ''{0}''.");
        m.put("m.highlightNeedsRow", "highlight expects a row.");
        m.put("m.badHex", "Invalid color code: ''{0}'' (expected e.g. #1F5E63).");
        m.put("m.highlighted", "Row {0} highlighted.");
        m.put("m.resized", "Resized to {0}×{1}.");
        m.put("m.resizeDiscard", "Warning: truncated values were discarded.");
        m.put("m.varNameEmpty", "Variable name must not be empty.");
        m.put("m.colRenamed", "Column {0} is now ''{1}''.");
        m.put("m.saved", "Saved: {0}");
        m.put("m.windowClosed", "System window closed.");
        m.put("m.centered", "View centered.");
        m.put("m.noSolutionCheck", "No solution – nothing to verify.");
        m.put("m.verifyInfinite", "Infinitely many solutions – check with free parameters = 0:");
        m.put("m.verifyHeader", "Verifying the solution:");
        m.put("m.rowLabel", "Row {0}");
        m.put("m.allSatisfied", "All equations satisfied.");
        m.put("m.notAllSatisfied", "Not all equations satisfied.");
        m.put("m.inverseNotSquare", "Inverse only for square systems.");
        m.put("m.inverseHeader", "Inverse of the coefficient matrix:");
        m.put("m.varNotFound", "Variable ''{0}'' not found.");
        m.put("m.substAdded", "Variable ''{0}'' = {1} added as an equation.");
        m.put("m.displayFrac", "Display: fractions.");
        m.put("m.displayDec", "Display: decimals.");
        m.put("m.animateArgs", "animate(row, from, to [, duration]).");
        m.put("m.durNumber", "Duration must be a number (seconds).");
        m.put("m.durPositive", "Duration must be positive.");
        m.put("m.animateStarted", "Animation of row {0}: {1} → {2}.");
        m.put("m.axisAuto", "Axes set to automatic scaling.");
        m.put("m.axisArgs", "axis(axis, min, max) or axis() to reset.");
        m.put("m.axisMaxMin", "max must be greater than min.");
        m.put("m.axisSet", "Axis {0} set to [{1}, {2}].");
        m.put("m.axisUnknown", "Axis ''{0}'' not available (x, y{1} or variable name).");
        m.put("m.randArgs", "rand(min, max [, row]).");
        m.put("m.randRange", "max must be ≥ min.");
        m.put("m.randFilled", "Matrix filled with random integers in [{0}, {1}].");
        m.put("m.redone", "Redone.");
        m.put("m.nothingRedo", "Nothing to redo.");
        m.put("m.rrefHeader", "Reduced row echelon form (RREF):");
        m.put("m.refHeader", "Row echelon form (REF):");
        m.put("m.transposeHeader", "Transposed coefficient matrix:");
        m.put("m.traceNotSquare", "Trace only for square systems.");
        m.put("m.traceResult", "trace = {0}");
        m.put("m.nullspaceTrivial", "Only the trivial solution (zero vector).");
        m.put("m.nullspaceHeader", "Null-space basis ({0} vectors):");
        m.put("m.cramerNotRegular", "Cramer's rule only for regular square systems (det ≠ 0).");
        m.put("m.cramerHeader", "Cramer's rule (det A = {0}):");
        m.put("m.latexCopied", "(LaTeX copied to clipboard)");
        m.put("m.csvSaved", "CSV saved: {0}");
        m.put("m.csvLoaded", "CSV loaded: ''{0}''.");
        m.put("m.csvBadFormat", "Invalid CSV format.");
        m.put("m.fileNotFound", "File not found: {0}");
        m.put("m.runFile", "Script executed: {0}");
        m.put("m.examplesHeader", "Available examples (examples(name)):");
        m.put("m.exampleLoaded", "Example ''{0}'' loaded.");
        m.put("m.exampleUnknown", "Unknown example: ''{0}''.");
        m.put("m.logCopied", "Log copied to clipboard.");
        m.put("m.logSaved", "Log saved: {0}");
        m.put("m.plotSaved", "Plot saved: {0}");
        m.put("m.labelsToggled", "Axis labels toggled.");
        m.put("m.expectsArgs", "''{0}'' expects {1} argument(s), got {2}.");
        m.put("m.notInt", "{0} must be an integer: ''{1}''.");
        m.put("m.notNumber", "{0} is not a valid number: ''{1}''.");
        m.put("m.dims", "Specify dimensions as width*height, e.g. 3*3.");
        m.put("m.onlyIn", "Command ''{0}'' is only available in the {1}.");
        m.put("m.onlyLgs", "This command is only available in the system window.");
        m.put("m.noOpenLgs", "No system open.");
        m.put("m.mainScreen", "main screen");
        m.put("m.lgsWindow", "system window");
        m.put("s.none", "No solution – the system is not satisfiable (inconsistent).");
        m.put("s.unique", "Unique solution:");
        m.put("s.infinite", "Infinitely many solutions ({0} free parameters):");
        m.put("s.free", "(free)");
        m.put("w.width", "width");
        m.put("w.height", "height");
        m.put("w.row", "row");
        m.put("w.rowI", "row i");
        m.put("w.rowJ", "row j");
        m.put("w.factor", "factor");
        m.put("w.steps", "step count");
        m.put("w.norm", "norm");
        m.put("w.column", "column");
        m.put("w.rhs", "right-hand side");
        m.put("w.coeff", "coefficient {0}");
        m.put("w.value", "value");
        m.put("w.from", "from");
        m.put("w.to", "to");
        m.put("w.min", "min");
        m.put("w.max", "max");
    }

    private static void englishGlossary(Map<String, String> m) {
        m.put("g.create", "New system + button. width = variables, height = rows.");
        m.put("g.add", "Add / overwrite an equation.");
        m.put("g.solve", "Solves the system; n = pre-computed steps (optional).");
        m.put("g.del", "del(name) removes a system; del(number) removes a row.");
        m.put("g.swap", "Swaps rows i and j.");
        m.put("g.scale", "Multiplies row i by a factor.");
        m.put("g.combine", "Row i := row i + factor · row j.");
        m.put("g.det", "Determinant (square only).");
        m.put("g.rank", "rank(A) and rank(A|b) + classification.");
        m.put("g.cond", "Condition number (∞-norm, optionally 1 or 2).");
        m.put("g.show", "Writes the matrix to the log as text.");
        m.put("g.clear", "Sets all values to 0.");
        m.put("g.undo", "Undo the last change (multi-level).");
        m.put("g.rename", "Renames the current system.");
        m.put("g.highlight", "Highlights row i in the coordinate system.");
        m.put("g.resize", "Changes the dimensions.");
        m.put("g.setvar", "Renames a variable column.");
        m.put("g.save", "Saves the system as ./lgs/name.lgs.");
        m.put("g.load", "Loads a saved system.");
        m.put("g.open", "Opens the system window.");
        m.put("g.close", "Closes the current system window.");
        m.put("g.list", "Lists all created systems.");
        m.put("g.fit", "Re-centers the view on the system.");
        m.put("g.verify", "Substitutes the solution and checks it.");
        m.put("g.inverse", "Inverse of the coefficient matrix.");
        m.put("g.subst", "Fixes a variable to a value.");
        m.put("g.frac", "Display fractions ↔ decimals.");
        m.put("g.animate", "Animates the right-hand side of a row.");
        m.put("g.axis", "Sets the display range of an axis (empty = automatic).");
        m.put("g.rand", "Fills the matrix with random integers in [min, max].");
        m.put("g.redo", "Re-applies the last undone change.");
        m.put("g.rref", "Shows the reduced row echelon form.");
        m.put("g.ref", "Shows the row echelon form.");
        m.put("g.transpose", "Shows the transposed coefficient matrix.");
        m.put("g.trace", "Trace (sum of the diagonal, square only).");
        m.put("g.nullspace", "Basis of the null space (kernel).");
        m.put("g.cramer", "Solves a square system with Cramer's rule.");
        m.put("g.latex", "Outputs the matrix as LaTeX (to clipboard).");
        m.put("g.csv", "Exports the system as a CSV file.");
        m.put("g.loadcsv", "Loads a system from a CSV file.");
        m.put("g.run", "Runs commands from a file.");
        m.put("g.examples", "Lists or loads example systems.");
        m.put("g.savelog", "Saves the log to a file.");
        m.put("g.copylog", "Copies the log to the clipboard.");
        m.put("g.saveplot", "Saves the coordinate system as PNG.");
        m.put("g.labels", "Toggles axis labels (numbers + names).");
    }
}
