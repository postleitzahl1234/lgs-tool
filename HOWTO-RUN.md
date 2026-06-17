# How to run LGS-Tool · So startest du das LGS-Tool

## English

### Option 1 — Just run it (no install)
Double-click:

```
target\dist\LGSTool\LGSTool.exe
```

It contains its own Java + JavaFX, so it runs on any Windows PC with nothing
installed. To move it to another PC, zip the whole `target\dist\LGSTool\` folder
and copy it over, then double-click `LGSTool.exe`.

### Option 2 — Run from source (development)
Requires a JDK ≥ 21 (built with JDK 25). Maven is fetched automatically by the
wrapper — no Maven install needed.

```powershell
.\mvnw javafx:run
```

If `JAVA_HOME` is not set, point it at your JDK first:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
```

### Option 3 — Rebuild the portable app
```powershell
.\build-portable.ps1
```
Result: `target\dist\LGSTool\LGSTool.exe`.

### Run the tests
```powershell
.\mvnw test
```

### First steps inside the program
Click **“+ Neues LGS”** (or type in the `[HAUPT]` line):

```
create(test, 2*2);
open(test);
```
Then in the system window’s `[LGS]` line:

```
add(1 1 = 3);
add(1 -1 = 1);
solve();
```

Keys: **F11** toggles fullscreen, **ESC** leaves fullscreen.

---

## Deutsch

### Variante 1 — Einfach starten (ohne Installation)
Doppelklick auf:

```
target\dist\LGSTool\LGSTool.exe
```

Java und JavaFX sind eingebettet – läuft auf jedem Windows-PC ohne Installation.
Zum Mitnehmen den ganzen Ordner `target\dist\LGSTool\` zippen, kopieren und dort
`LGSTool.exe` doppelklicken.

### Variante 2 — Aus dem Quellcode starten (Entwicklung)
Benötigt ein JDK ≥ 21 (entwickelt mit JDK 25). Maven wird über den Wrapper
automatisch geladen.

```powershell
.\mvnw javafx:run
```

Falls `JAVA_HOME` nicht gesetzt ist:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
```

### Variante 3 — Portable App neu bauen
```powershell
.\build-portable.ps1
```

### Tests ausführen
```powershell
.\mvnw test
```

### Erste Schritte im Programm
„**+ Neues LGS**“ klicken (oder in der `[HAUPT]`-Zeile tippen):

```
create(test, 2*2);
open(test);
```
Im LGS-Fenster in der `[LGS]`-Zeile:

```
add(1 1 = 3);
add(1 -1 = 1);
solve();
```

Tasten: **F11** schaltet Vollbild um, **ESC** verlässt das Vollbild.
