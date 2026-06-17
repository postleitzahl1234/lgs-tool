package de.lgstool.model;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bounded multi-level undo/redo. Before every mutating command a deep snapshot of
 * the {@link AppState} is pushed; {@link #undo(AppState)} restores the most recent
 * one and {@link #redo(AppState)} re-applies it. Recording a new change clears the
 * redo history.
 */
public final class UndoManager {

    public static final int DEFAULT_LIMIT = 50;

    private final Deque<AppState> history = new ArrayDeque<>();
    private final Deque<AppState> redo = new ArrayDeque<>();
    private final int limit;

    public UndoManager() {
        this(DEFAULT_LIMIT);
    }

    public UndoManager(int limit) {
        this.limit = Math.max(1, limit);
    }

    /** Records a snapshot of the state prior to a mutation; clears the redo stack. */
    public void record(AppState state) {
        history.push(state.copy());
        redo.clear();
        while (history.size() > limit) {
            history.removeLast();
        }
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    public int depth() {
        return history.size();
    }

    /**
     * Restores the most recent snapshot.
     *
     * @return the previous state, or {@code null} if there is nothing to undo
     */
    public AppState undo(AppState current) {
        if (history.isEmpty()) {
            return null;
        }
        redo.push(current.copy());
        while (redo.size() > limit) {
            redo.removeLast();
        }
        return history.pop();
    }

    /**
     * Re-applies the most recently undone snapshot.
     *
     * @return the redone state, or {@code null} if there is nothing to redo
     */
    public AppState redo(AppState current) {
        if (redo.isEmpty()) {
            return null;
        }
        history.push(current.copy());
        while (history.size() > limit) {
            history.removeLast();
        }
        return redo.pop();
    }

    public void clear() {
        history.clear();
        redo.clear();
    }
}
