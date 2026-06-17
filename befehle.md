# LGS-Tool — Befehls- & Oberflächenspezifikation (final)

Java + JavaFX · exakte Bruch-Arithmetik · 1D/2D/3D-Visualisierung.
Farben aus der Präsentation: Hintergrund `#E8E6DF`, Akzente `#1F5E63` (Petrol) & `#8FAF9A` (Salbeigrün), Text `#1A1A1A`.

## Befehle

Mehrere Befehlszeilen. **Nicht jeder Befehl gilt in jeder Befehlszeile** (Spalte „Kontext").
**[HAUPT]** = Hauptbildschirm · **[LGS]** = LGS-Detailfenster.

| # | Befehl / Syntax | Bedeutung | Beispiel | Kontext |
|---|-----------------|-----------|----------|---------|
| 1 | `create(name, breite*höhe);` | Erzeugt ein neues LGS + Button im Hauptbildschirm. Breite = Variablen (Ergebnisspalte extra), Höhe = Zeilen. Legt `höhe` leere Zeilen an. | `create(test, 3*3);` | [HAUPT] |
| 2 | `add(c1 … cn = b [, zeile]);` | Ohne `zeile`: füllt die nächste leere Zeile; sind alle voll, wächst das LGS um eine Zeile. Mit `zeile`: überschreibt diese Gleichung. Fehlermeldung bei falscher Koeffizientenzahl. Negativ/Dezimal/Bruch erlaubt. | `add(1 -2 1.2 = 5);` · `add(1 -2 1.2 = 5, 2);` | [LGS] |
| 3 | `solve(n);` | Löst das LGS, Ausgabe pro Parameter im Log. `n` = vorgerechnete Schritte (optional, `n` > Schrittzahl → komplett). **Unendlich viele Lösungen:** freie Parameter werden gewählt und die parametrisierte allgemeine Lösung ausgegeben (`x = 2 − 3t`, `y = t (frei)` …). **Keine Lösung:** entsprechende Meldung. | `solve();` · `solve(2);` | [LGS] |
| 4 | `del(arg);` | [HAUPT] `del(name)` löscht LGS + Button. [LGS] `del(zahl)` löscht die Zeile; Zeilen werden neu durchnummeriert. | `del(test);` · `del(2);` | [HAUPT]/[LGS] |
| 5 | `swap(i, j);` | Vertauscht Zeile I und J. | `swap(1, 3);` | [LGS] |
| 6 | `scale(i, faktor);` | Multipliziert Zeile I mit Faktor (Bruch `1/3` erlaubt). | `scale(2, 1/3);` | [LGS] |
| 7 | `combine(i, faktor, j);` | Zeile I := Zeile I + Faktor · Zeile J. | `combine(2, -2, 1);` | [LGS] |
| 8 | `det();` | Determinante (nur quadratisch, sonst Fehlermeldung). = 0 → nicht eindeutig lösbar. | `det();` | [LGS] |
| 9 | `rank();` | Gibt Rang(A) und Rang(A\|b) aus + Klassifikation (eindeutig/unendlich/keine). | `rank();` | [LGS] |
| 10 | `cond([norm]);` | Konditionszahl (Stabilitätsmaß). Default ∞-Norm, optional `cond(2)` für 2-Norm. Numerisch berechnet. | `cond();` · `cond(2);` | [LGS] |
| 11 | `show();` | Schreibt die aktuelle Matrix als Text ins Log. | `show();` | [LGS] |
| 12 | `clear();` | Setzt alle Werte auf 0 (Struktur bleibt). | `clear();` | [LGS] |
| 13 | `undo();` | Letzte Änderung rückgängig, mehrstufig (~50 Schritte). Auch im Hauptbildschirm (z. B. nach `del`). | `undo();` | [HAUPT]/[LGS] |
| 14 | `rename(neu);` | Benennt das aktuell geöffnete LGS um. | `rename(aufgabe1);` | [LGS] |
| 15 | `highlight(i [, hexcode]);` | Hebt Zeile I im Koordinatensystem hervor. Optionaler Hex-Farbcode, sonst automatisch. | `highlight(2);` · `highlight(2, #1F5E63);` | [LGS] |
| 16 | `resize(breite*höhe);` | Ändert die Dimensionen. Beim Verkleinern werden abgeschnittene Werte verworfen (Warnung ins Log). | `resize(4*4);` | [LGS] |
| 17 | `setvar(spalte, name);` | Benennt die Variable über einer Spalte um. | `setvar(1, a);` | [LGS] |
| 18 | `save(name);` | Sichert das LGS als `./lgs/name.lgs` (eigenes Textformat). | `save(aufgabe1);` | [LGS] |
| 19 | `load(name);` | Lädt ein gespeichertes LGS (erzeugt Button). | `load(aufgabe1);` | [HAUPT] |
| 20 | `open(name);` | Öffnet das LGS-Fenster per Befehl. | `open(test);` | [HAUPT] |
| 21 | `close();` | Schließt das aktuelle LGS-Fenster. | `close();` | [LGS] |
| 22 | `list();` | Listet alle erstellten LGS (Namen) auf. | `list();` | [HAUPT] |
| 23 | `fit();` | Zentriert die Ansicht wieder auf das ganze LGS. | `fit();` | [LGS] |
| 24 | `verify();` | Setzt die aktuelle Lösung in alle Gleichungen ein und prüft sie. | `verify();` | [LGS] |
| 25 | `inverse();` | Inverse der Koeffizientenmatrix (quadratisch/invertierbar, sonst Fehlermeldung). | `inverse();` | [LGS] |
| 26 | `subst(var, wert);` | Setzt eine Variable fest (für unterbestimmte Systeme). | `subst(z, 1);` | [LGS] |
| 27 | `frac();` / `dec();` | Anzeige Bruch ↔ Dezimal. Pro LGS gemerkt, global in Einstellungen. | `frac();` · `dec();` | [LGS] |
| 28 | `animate(zeile, von, bis [, dauer]);` | Lässt die rechte Seite von `zeile` von `von` bis `bis` laufen und animiert Gerade/Ebene + Lösungspunkt. Eine RHS pro Aufruf, Ping-Pong-Schleife mit Pause/Stop. `dauer` in Sekunden (optional). | `animate(1, 0, 10);` | [LGS] |

## Allgemeine Parsing-Regeln
- Funktions-Stil mit Klammern: `name(argumente);`, Abschluss mit `;`.
- Zahlen durch **Leerzeichen** getrennt; **negative Zahlen** und **Dezimalzahlen** erlaubt.
- **Brüche** `p/q` überall als Faktor/Wert erlaubt.
- Dezimalzahlen intern **exakt als Bruch** gespeichert (1.2 = 6/5).

## Oberfläche / Layout

**Start:** Vollbild (Fullscreen).

### Hauptbildschirm
- **Links oben:** Buttons der erstellten LGS.
- **Darunter:** Befehlszeile [HAUPT] (`create`, `del`, `load`, `open`, `list`, `undo`).
- **Optionen:** „Einstellungen" und „Glossar" (Befehls-Nachschlagewerk).
- **Komplette rechte Seite:** Koordinatensystem; zeigt immer das **aktuell geöffnete/gewählte** LGS.
  - Buttons „größer"/„kleiner" (Zoom), Scroll-Funktion.
  - Visualisierung nur für **1–3 Variablen** (Punkt/Gerade/Ebene). Bei mehr: Hinweis „mehr als 3 Variablen — keine grafische Darstellung".

### LGS-Detailfenster (Klick auf einen LGS-Button)
- Großes Fenster mit dem LGS.
- **Beliebig große LGS:** passt es nicht ins Fenster, verschiebt man den Blick mit der **Maus** (Pan/Drag, „Glasscheibe").
- Werte **interaktiv anklickbar** (einzeln editierbar).
- **Zeilen** mit römischen Ziffern (I, II, III, …), **Variablennamen** über den Spalten.
- **Rechts:** Log mit den Lösungen (Variable unter Variable).
- **Unten:** Befehlszeile [LGS].

## Animations-Feature — Implementierungsidee (Befehl 28)
- Nur ein RHS-Eintrag ändert sich → Gerade/Ebene verschiebt sich parallel; Lösung wandert **linear**: `x(t) = x₀ + t·(A⁻¹·eᵢ)`. `A⁻¹` einmal berechnen, pro Frame nur Vektor-Update → flüssig.
- JavaFX `AnimationTimer`: pro Frame `t` aus verstrichener Zeit + `dauer`, `b[zeile]` setzen, neu zeichnen (Canvas 2D / MeshView+Sphere 3D). Ping-Pong = `t` hin/her.
- Singulär/parallel → kein wandernder Punkt: nur Gerade/Ebene bewegen + Hinweis.
- Bonus: live `‖Δx‖/‖Δb‖` einblenden (Brücke zu `cond()`).

## Vor dem Coden noch offen (Setup)
- **Build-Tool:** Maven, Gradle oder Plain + JavaFX-SDK?
- **Java-Version:** 21 (LTS) oder 17?
