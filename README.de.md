# LGS-Tool

*[English](README.md) · Deutsch*

Ein JavaFX-Werkzeug zum Lösen **linearer Gleichungssysteme** (LGS) mit
**exakter Bruch-Arithmetik** und **1D/2D/3D-Visualisierung**. Bedienung über eine
eigene Befehlssprache (`befehl(argumente);`).

- **Exakt:** alle Rechnungen mit `BigInteger`-Brüchen; Dezimalzahlen werden exakt
  gespeichert (`1.2 = 6/5`).
- **Lösung:** eindeutig, **unendlich viele** (parametrisierte allgemeine Lösung,
  z. B. `x = 2 − 3t, y = t (frei)`) oder keine Lösung.
- **Visualisierung:** Zahlengerade (1 Variable), Geraden + Schnittpunkt
  (2 Variablen), Ebenen im Raum (3 Variablen). Ab 4 Variablen ein Hinweis.
- **Portabel:** als eigenständige Windows-App ohne jede Installation lauffähig.

Die vollständige Spezifikation steht in [`befehle.md`](befehle.md).

## Schnellstart (fertige App)

Die fertige Windows-App liegt unter **Releases** als ZIP. Entpacken und
`LGSTool.exe` doppelklicken (nach einem eigenen Build: `target\dist\LGSTool\LGSTool.exe`). Es wird **kein** installiertes
Java oder JavaFX benötigt — Laufzeit ist eingebettet. Den Ordner
`target\dist\LGSTool\` kann man zippen und auf jeden Windows-PC kopieren.

## Erste Schritte (im Programm)

Im Hauptbildschirm unten ist die Befehlszeile `[HAUPT]`:

```
create(test, 2*2);     # erzeugt ein LGS mit 2 Variablen, 2 Zeilen + Button
open(test);            # öffnet das Detailfenster
```

Im Detailfenster `[LGS]`:

```
add(1 1 = 3);          # x + y = 3
add(1 -1 = 1);         # x − y = 1
solve();               # → x = 2, y = 1   (mit Schnittpunkt im Koordinatensystem)
solve(3);              # mit 3 vorgerechneten Schritten
det();  rank();  cond();  inverse();  verify();
```

Werte lassen sich auch **direkt in der Matrix anklicken** und bearbeiten. Große
Systeme verschiebt man mit der Maus.

## Entwicklung

Voraussetzung: **JDK 25** (das `pom.xml` kompiliert mit `release 25`).
Maven wird über den **Wrapper** `mvnw` automatisch geladen — keine Maven-Installation nötig.

```powershell
.\mvnw.cmd test            # Unit-Tests (Mathematik, Modell, Befehle, Persistenz)
.\mvnw.cmd javafx:run      # App im Entwicklungsmodus starten
```

Setze ggf. `JAVA_HOME` auf dein JDK 25 (z. B. `C:\Program Files\Java\jdk-25`).

Interner UI-Rauchtest (baut die Oberfläche, rendert 1D/2D/3D, prüft alle Pfade):

```powershell
.\mvnw.cmd -DskipTests package javafx:jlink
.\target\lgstool-runtime\bin\java.exe -m de.lgstool/de.lgstool.App --selftest
# erwartet: "SELFTEST OK"
```

## Portable App bauen

```powershell
.\build-portable.ps1
# Ergebnis: target\dist\LGSTool\LGSTool.exe (~90 MB, alles eingebettet)
```

Eine portable App ist **pro Betriebssystem** zu bauen (`jpackage` erzeugt für das
System, auf dem es läuft). Dieses Skript erzeugt die **Windows-x64**-Variante.

## Architektur

```
de.lgstool
├── math/     Fraction, FractionMatrix, Gauss (RREF/Det/Rang/Inverse/parametrische Lösung),
│             ConditionNumber, Solution, Roman
├── model/    Lgs, AppState, UndoManager (mehrstufiges Undo, ~50 Schritte)
├── command/  CommandParser, CommandEngine (alle 28 Befehle), UiBridge, CommandContext
├── io/       LgsStorage (.lgs-Textformat speichern/laden)
├── geo/      Geometrie- & CAS-Modul (Ausdrucksparser, Ableitungen, Kegelschnitte, LaTeX-Export)
├── viz/      Visualization (1D/2D-Canvas, 3D-SubScene, Zoom/Pan/Highlight/Animation)
├── ui/       AppController, MainScreen, LgsWindow, MatrixView, CommandBar, LogPane,
│             Glossary-/Settings-/NewLgs-Dialoge
├── config/   Theme (Farben), Settings
└── App, SelfTest
```

Die gesamte Logik (`math`, `model`, `command`, `io`) ist **UI-frei** und durch
Unit-Tests abgedeckt; JavaFX-Effekte laufen über die `UiBridge`-Schnittstelle.

## Farben (aus der Präsentation)

| Rolle | Hex |
|------|-----|
| Hintergrund | `#E8E6DF` |
| Akzent Petrol | `#1F5E63` |
| Akzent Salbeigrün | `#8FAF9A` |
| Text | `#1A1A1A` |

## Lizenz & Entstehung

MIT-Lizenz, siehe [LICENSE](LICENSE).

Entwickelt mit KI-Unterstützung (Claude); Konzept, Entwurf der Befehlssprache und Tests stammen von mir.
