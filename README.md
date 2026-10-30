# LGS-Tool

*English · [Deutsch](README.de.md)*

A JavaFX desktop app for solving **systems of linear equations** with **exact fraction
arithmetic** and **1D/2D/3D visualization**, driven by its own small command language
(`command(arguments);`). *LGS* is German for *lineares Gleichungssystem*.

## Features

- **Exact:** every calculation uses `BigInteger` fractions; decimals are stored exactly (`1.2 = 6/5`).
- **Every solution case:** unique, **infinitely many** (parametrized general solution such as
  `x = 2 − 3t, y = t (free)`) or none.
- **Step by step:** `solve(n)` shows the first *n* Gaussian elimination steps.
- **Analysis:** determinant, rank of A and A|b, condition number, inverse, verification.
- **Visualization:** number line (1 variable), lines and intersection (2), planes in space (3).
- **Geometry & CAS module:** expression parser, derivatives, conic sections, LaTeX export.
- **Undo** (about 50 steps), save/load in a plain-text `.lgs` format.
- **Portable:** ships as a self-contained Windows app, with no Java install needed.

## Quick start

Download the Windows ZIP from **Releases**, unzip it and double-click `LGSTool.exe`.

In the main window's `[HAUPT]` command line:

```
create(test, 2*2);     # new system: 2 variables, 2 rows
open(test);            # open its detail window
```

In the detail window's `[LGS]` command line:

```
add(1 1 = 3);          # x + y = 3
add(1 -1 = 1);         # x − y = 1
solve();               # → x = 2, y = 1, intersection drawn in the plot
solve(3);              # show 3 elimination steps first
det();  rank();  cond();  inverse();  verify();
```

Matrix cells can also be edited by clicking them. The full command reference (German) is in
[`befehle.md`](befehle.md).

## Build from source

Requires **JDK 25**. Maven is downloaded by the wrapper, so no Maven install is needed.

```powershell
.\mvnw.cmd test            # unit tests (math, model, commands, persistence, geometry)
.\mvnw.cmd javafx:run      # run in development mode
.\build-portable.ps1       # build target\dist\LGSTool\LGSTool.exe (Windows x64, ~90 MB)
```

UI smoke test (builds the interface, renders 1D/2D/3D):

```powershell
.\mvnw.cmd -DskipTests package javafx:jlink
.\target\lgstool-runtime\bin\java.exe -m de.lgstool/de.lgstool.App --selftest   # expects "SELFTEST OK"
```

More detail in [HOWTO-RUN.md](HOWTO-RUN.md).

## Architecture

```
de.lgstool
├── math/     Fraction, FractionMatrix, Gauss (RREF / det / rank / inverse / parametric solution)
├── model/    Lgs, AppState, UndoManager
├── command/  CommandParser, CommandEngine, UiBridge
├── geo/      geometry & CAS module (expression parser, calculus, conics, LaTeX)
├── io/       LgsStorage (.lgs text format)
├── viz/      1D/2D canvas, 3D SubScene, zoom / pan / highlight
├── ui/       windows, matrix view, command bar, log, dialogs
└── config/   theme, settings, i18n
```

All logic in `math`, `model`, `command` and `io` is UI-free and covered by unit tests;
JavaFX effects go through the `UiBridge` interface.

## License & credits

MIT, see [LICENSE](LICENSE).

Built with AI assistance (Claude). The concept, the command-language design and testing are mine.
