package de.lgstool.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UndoManagerTest {

    @Test
    void historyIsBounded() {
        UndoManager undo = new UndoManager(50);
        AppState state = new AppState();
        for (int i = 0; i < 60; i++) {
            state.put(Lgs.create("a" + i, 1, 1));
            undo.record(state);
        }
        assertEquals(50, undo.depth());
    }

    @Test
    void undoRestoresSnapshot() {
        UndoManager undo = new UndoManager();
        AppState state = new AppState();
        state.put(Lgs.create("a", 1, 1));
        undo.record(state);
        state.put(Lgs.create("b", 1, 1));
        assertTrue(undo.canUndo());

        AppState restored = undo.undo(state);
        assertNotNull(restored);
        assertTrue(restored.contains("a"));
        assertFalse(restored.contains("b"));
    }

    @Test
    void undoOnEmptyHistoryReturnsNull() {
        UndoManager undo = new UndoManager();
        assertNull(undo.undo(new AppState()));
    }

    @Test
    void snapshotsAreDeepCopies() {
        UndoManager undo = new UndoManager();
        AppState state = new AppState();
        Lgs lgs = Lgs.create("a", 2, 2);
        state.put(lgs);
        undo.record(state);
        // mutate after recording; snapshot must be unaffected
        lgs.addEquation(new de.lgstool.math.Fraction[]{
                de.lgstool.math.Fraction.of(5), de.lgstool.math.Fraction.of(5)}, de.lgstool.math.Fraction.of(9), 1);
        AppState restored = undo.undo(state);
        assertTrue(restored.get("a").row(1)[0].isZero());
    }
}
