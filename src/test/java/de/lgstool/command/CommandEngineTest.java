package de.lgstool.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.lgstool.io.LgsStorage;
import de.lgstool.math.Fraction;
import de.lgstool.model.Lgs;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandEngineTest {

    private CommandEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CommandEngine();
    }

    private CommandResult main(String line) {
        return engine.execute(line, CommandContext.main());
    }

    private CommandResult lgs(String name, String line) {
        return engine.execute(line, CommandContext.lgs(name));
    }

    private String text(CommandResult r) {
        return String.join("\n", r.messages());
    }

    @Test
    void createOnlyInMainScreen() {
        assertTrue(main("create(test, 2*2);").isOk());
        assertTrue(engine.state().contains("test"));
        assertFalse(engine.execute("create(x, 2*2)", CommandContext.lgs("test")).isOk());
    }

    @Test
    void duplicateNameRejected() {
        main("create(t, 2*2);");
        assertFalse(main("create(t, 3*3);").isOk());
    }

    @Test
    void addFillsEmptyRowsThenGrows() {
        main("create(t, 2*2);");
        assertTrue(lgs("t", "add(1 1 = 3);").isOk());
        assertTrue(lgs("t", "add(1 -1 = 1);").isOk());
        assertEquals(2, engine.state().get("t").height());
        assertTrue(lgs("t", "add(2 0 = 4);").isOk());
        assertEquals(3, engine.state().get("t").height());
    }

    @Test
    void addRejectsWrongCoefficientCount() {
        main("create(t, 2*2);");
        assertFalse(lgs("t", "add(1 1 1 = 3);").isOk());
    }

    @Test
    void addWithRowOverwrites() {
        main("create(t, 2*2);");
        lgs("t", "add(1 1 = 3);");
        assertTrue(lgs("t", "add(9 9 = 9, 2);").isOk());
        assertEquals(Fraction.of(9), engine.state().get("t").row(2)[0]);
    }

    @Test
    void solveUnique() {
        main("create(t, 2*2);");
        lgs("t", "add(1 1 = 3);");
        lgs("t", "add(1 -1 = 1);");
        String s = text(lgs("t", "solve();"));
        assertTrue(s.contains("x = 2"), s);
        assertTrue(s.contains("y = 1"), s);
    }

    @Test
    void solveNoSolution() {
        main("create(t, 2*2);");
        lgs("t", "add(1 1 = 1);");
        lgs("t", "add(1 1 = 2);");
        assertTrue(text(lgs("t", "solve();")).contains("Keine Lösung"));
    }

    @Test
    void solveInfiniteWithSteps() {
        main("create(t, 2*1);");
        lgs("t", "add(1 2 = 3);");
        String s = text(lgs("t", "solve(5);"));
        assertTrue(s.contains("Unendlich"), s);
        assertTrue(s.contains("frei"), s);
        assertTrue(s.contains("Rechenschritte"), s);
    }

    @Test
    void undoRevertsCreate() {
        main("create(t, 2*2);");
        assertTrue(engine.state().contains("t"));
        main("undo();");
        assertFalse(engine.state().contains("t"));
    }

    @Test
    void undoRevertsOverwrite() {
        main("create(t, 2*2);");
        lgs("t", "add(1 1 = 3);");
        lgs("t", "add(9 9 = 9, 2);");
        main("undo();");
        assertTrue(engine.state().get("t").row(2)[0].isZero());
    }

    @Test
    void deleteRowAndSystem() {
        main("create(t, 2*2);");
        lgs("t", "add(1 1 = 3);");
        lgs("t", "add(1 -1 = 1);");
        assertTrue(lgs("t", "del(1);").isOk());
        assertEquals(1, engine.state().get("t").height());
        assertTrue(main("del(t);").isOk());
        assertFalse(engine.state().contains("t"));
    }

    @Test
    void rowOperations() {
        main("create(t, 2*2);");
        lgs("t", "add(1 0 = 1);");
        lgs("t", "add(0 1 = 2);");
        assertTrue(lgs("t", "swap(1, 2);").isOk());
        assertTrue(lgs("t", "scale(1, 2);").isOk());
        assertTrue(lgs("t", "combine(2, -1, 1);").isOk());
        assertFalse(lgs("t", "scale(1, 0);").isOk());
    }

    @Test
    void determinantRankInverse() {
        main("create(t, 2*2);");
        lgs("t", "add(1 2 = 0);");
        lgs("t", "add(3 4 = 0);");
        assertTrue(text(lgs("t", "det();")).contains("det = -2"));
        assertTrue(text(lgs("t", "rank();")).contains("Eindeutig"));
        assertTrue(text(lgs("t", "inverse();")).contains("-2"));
    }

    @Test
    void conditionNumberRuns() {
        main("create(t, 2*2);");
        lgs("t", "add(1 0 = 0);");
        lgs("t", "add(0 1 = 0);");
        assertTrue(text(lgs("t", "cond();")).contains("cond"));
        assertTrue(lgs("t", "cond(2);").isOk());
    }

    @Test
    void renameUpdatesState() {
        main("create(t, 2*2);");
        assertTrue(lgs("t", "rename(neu);").isOk());
        assertTrue(engine.state().contains("neu"));
        assertFalse(engine.state().contains("t"));
    }

    @Test
    void setvarRenamesColumn() {
        main("create(t, 2*2);");
        lgs("t", "setvar(1, a);");
        assertEquals("a", engine.state().get("t").variableNames()[0]);
    }

    @Test
    void fractionDecimalToggle() {
        main("create(t, 2*2);");
        assertTrue(lgs("t", "dec();").isOk());
        assertFalse(engine.state().get("t").isFractionDisplay());
        assertTrue(lgs("t", "frac();").isOk());
        assertTrue(engine.state().get("t").isFractionDisplay());
    }

    @Test
    void resizeWarnsOnDiscard() {
        main("create(t, 2*2);");
        lgs("t", "add(1 2 = 3);");
        assertTrue(text(lgs("t", "resize(1*2);")).contains("Warnung"));
        assertEquals(1, engine.state().get("t").width());
    }

    @Test
    void verifyChecksSolution() {
        main("create(t, 2*2);");
        lgs("t", "add(1 1 = 3);");
        lgs("t", "add(1 -1 = 1);");
        String s = text(lgs("t", "verify();"));
        assertTrue(s.contains("✓"), s);
        assertTrue(s.contains("erfüllt"), s);
    }

    @Test
    void substAddsConstraint() {
        main("create(t, 2*2);");
        lgs("t", "add(1 1 = 3);");
        assertTrue(lgs("t", "subst(y, 1);").isOk());
        assertTrue(lgs("t", "solve();").isOk());
    }

    @Test
    void saveAndLoadRoundtrip() throws Exception {
        main("create(rt, 2*2);");
        lgs("rt", "add(1 2 = 3);");
        lgs("rt", "add(4 5 = 6);");
        lgs("rt", "setvar(1, a);");
        try {
            assertTrue(lgs("rt", "save(rt_test_file);").isOk());
            CommandEngine other = new CommandEngine();
            assertTrue(other.execute("load(rt_test_file)", CommandContext.main()).isOk());
            Lgs loaded = other.state().get("rt");
            assertEquals(2, loaded.width());
            assertEquals(2, loaded.height());
            assertEquals("a", loaded.variableNames()[0]);
            assertEquals(Fraction.of(3), loaded.row(1)[2]);
        } finally {
            Files.deleteIfExists(LgsStorage.fileFor("rt_test_file"));
        }
    }

    @Test
    void uiCallbacksFire() {
        RecordingUi ui = new RecordingUi();
        CommandEngine e = new CommandEngine(ui);
        e.execute("create(t, 2*2)", CommandContext.main());
        assertTrue(ui.systemsChanged > 0);
        e.execute("open(t)", CommandContext.main());
        assertEquals("t", ui.lastOpened);
        e.execute("animate(1, 0, 10)", CommandContext.lgs("t"));
        assertEquals("t", ui.lastAnimateName);
    }

    @Test
    void unknownAndMalformedCommands() {
        assertFalse(main("bogus()").isOk());
        assertFalse(main("create").isOk());
        assertFalse(engine.execute("add(1 1 = 3)", CommandContext.main()).isOk());
    }

    @Test
    void resizeGrowKeepsVariableNames() {
        main("create(t, 2*2);");
        lgs("t", "setvar(1, a);");
        assertTrue(lgs("t", "resize(3*3);").isOk());
        Lgs g = engine.state().get("t");
        assertEquals(3, g.width());
        assertEquals(3, g.height());
        assertEquals("a", g.variableNames()[0]);
    }

    @Test
    void saveLoadPreservesFractionsAndDecimals() throws Exception {
        main("create(rf, 2*1);");
        lgs("rf", "add(1/2 -3 = 1.2);");
        try {
            assertTrue(lgs("rf", "save(rf_test_file);").isOk());
            CommandEngine other = new CommandEngine();
            assertTrue(other.execute("load(rf_test_file)", CommandContext.main()).isOk());
            Lgs g = other.state().get("rf");
            assertEquals(Fraction.of(1, 2), g.row(1)[0]);
            assertEquals(Fraction.of(-3), g.row(1)[1]);
            assertEquals(Fraction.of(6, 5), g.row(1)[2]);
        } finally {
            Files.deleteIfExists(LgsStorage.fileFor("rf_test_file"));
        }
    }

    @Test
    void randFillsMatrix() {
        main("create(t, 2*2);");
        assertTrue(lgs("t", "rand(7, 7);").isOk());
        Lgs g = engine.state().get("t");
        for (int r = 1; r <= 2; r++) {
            for (int col = 0; col <= 2; col++) {
                assertEquals(Fraction.of(7), g.row(r)[col]);
            }
        }
        assertFalse(lgs("t", "rand(5, 1);").isOk());
        assertTrue(lgs("t", "rand(0, 0, 1);").isOk());
        assertEquals(Fraction.ZERO, engine.state().get("t").row(1)[0]);
    }

    @Test
    void axisCommandRoutesToUi() {
        RecordingUi ui = new RecordingUi();
        CommandEngine e = new CommandEngine(ui);
        e.execute("create(t, 2*2)", CommandContext.main());
        e.execute("open(t)", CommandContext.main());
        assertTrue(e.execute("axis(x, -5, 5)", CommandContext.lgs("t")).isOk());
        assertEquals(0, ui.lastAxisIndex);
        assertEquals("t", ui.lastAxisName);
        assertTrue(e.execute("axis()", CommandContext.lgs("t")).isOk());
        assertEquals(-1, ui.lastAxisIndex);
        assertFalse(e.execute("axis(z, -5, 5)", CommandContext.lgs("t")).isOk());
    }

    @Test
    void redoReappliesChange() {
        main("create(a, 2*2);");
        lgs("a", "rand(1, 1);");
        assertEquals(Fraction.of(1), engine.state().get("a").row(1)[0]);
        engine.execute("undo();", CommandContext.lgs("a"));
        assertEquals(Fraction.ZERO, engine.state().get("a").row(1)[0]);
        assertTrue(engine.execute("redo();", CommandContext.lgs("a")).isOk());
        assertEquals(Fraction.of(1), engine.state().get("a").row(1)[0]);
    }

    @Test
    void newMathCommandsRun() {
        main("create(m, 2*2);");
        lgs("m", "add(2 1 = 3);");
        lgs("m", "add(1 1 = 2);");
        assertTrue(lgs("m", "rref();").isOk());
        assertTrue(lgs("m", "ref();").isOk());
        assertTrue(lgs("m", "transpose();").isOk());
        assertTrue(lgs("m", "trace();").isOk());
        assertTrue(lgs("m", "cramer();").isOk());
        assertTrue(lgs("m", "nullspace();").isOk());
    }

    @Test
    void latexCopiesToClipboard() {
        RecordingUi ui = new RecordingUi();
        CommandEngine e = new CommandEngine(ui);
        e.execute("create(m, 2*2)", CommandContext.main());
        e.execute("open(m)", CommandContext.main());
        e.execute("rand(1, 1)", CommandContext.lgs("m"));
        assertTrue(e.execute("latex();", CommandContext.lgs("m")).isOk());
        assertNotNull(ui.lastClipboard);
        assertTrue(ui.lastClipboard.contains("array"), ui.lastClipboard);
    }

    @Test
    void examplesLoadKnownSystems() {
        assertTrue(main("examples(unique2);").isOk());
        assertTrue(engine.state().contains("unique2"));
        assertFalse(main("examples(does-not-exist);").isOk());
    }

    private static final class RecordingUi implements UiBridge {
        int systemsChanged;
        String lastOpened;
        String lastAnimateName;
        String lastAxisName;
        int lastAxisIndex = -99;
        String lastClipboard;
        boolean copyLogCalled;
        String savedLogPath;
        String savedPlotPath;

        @Override
        public void systemsChanged() {
            systemsChanged++;
        }

        @Override
        public void openWindow(String name) {
            lastOpened = name;
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
            lastAnimateName = name;
        }

        @Override
        public void setAxisRange(String name, int axisIndex, double min, double max) {
            lastAxisName = name;
            lastAxisIndex = axisIndex;
        }

        @Override
        public void copyToClipboard(String text) {
            lastClipboard = text;
        }

        @Override
        public void copyLog() {
            copyLogCalled = true;
        }

        @Override
        public void saveLog(String path) {
            savedLogPath = path;
        }

        @Override
        public void savePlot(String path) {
            savedPlotPath = path;
        }

        @Override
        public void toggleLabels(String name) {
        }
    }
}
