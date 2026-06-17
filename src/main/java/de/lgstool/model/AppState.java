package de.lgstool.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The whole application state: all created systems (in creation order) plus the
 * name of the system currently shown in the detail window. Deep-copied by the
 * undo system.
 */
public final class AppState {

    private final Map<String, Lgs> systems = new LinkedHashMap<>();
    private String openName;

    public boolean contains(String name) {
        return systems.containsKey(name);
    }

    public Lgs get(String name) {
        return systems.get(name);
    }

    public void put(Lgs lgs) {
        systems.put(lgs.name(), lgs);
    }

    public void remove(String name) {
        systems.remove(name);
        if (name.equals(openName)) {
            openName = null;
        }
    }

    public void rename(String oldName, String newName) {
        Lgs lgs = systems.remove(oldName);
        if (lgs == null) {
            return;
        }
        lgs.setName(newName);
        // Preserve order by rebuilding the map.
        Map<String, Lgs> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, Lgs> e : systems.entrySet()) {
            rebuilt.put(e.getKey(), e.getValue());
        }
        systems.clear();
        systems.putAll(rebuilt);
        systems.put(newName, lgs);
        if (oldName.equals(openName)) {
            openName = newName;
        }
    }

    public List<String> names() {
        return new ArrayList<>(systems.keySet());
    }

    public int size() {
        return systems.size();
    }

    public String openName() {
        return openName;
    }

    public void setOpenName(String openName) {
        this.openName = openName;
    }

    public AppState copy() {
        AppState c = new AppState();
        for (Lgs lgs : systems.values()) {
            c.systems.put(lgs.name(), lgs.copy());
        }
        c.openName = openName;
        return c;
    }
}
