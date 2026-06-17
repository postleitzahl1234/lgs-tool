# Builds the portable Windows app-image: target\dist\LGSTool\LGSTool.exe
# The result runs on any Windows PC with nothing installed (bundled Java + JavaFX).
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
Set-Location $root

if (-not $env:JAVA_HOME) {
    if (Test-Path 'C:\Program Files\Java\jdk-25') {
        $env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
    } else {
        throw "JAVA_HOME is not set and JDK 25 was not found. Install a JDK >= 21 and set JAVA_HOME."
    }
}

Write-Host "==> Building runtime image (mvnw + jlink)..." -ForegroundColor Cyan
& "$root\mvnw.cmd" -DskipTests clean package javafx:jlink
if ($LASTEXITCODE -ne 0) { throw "Maven build failed." }

$jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
if (-not (Test-Path $jpackage)) { throw "jpackage not found in JAVA_HOME ($jpackage)." }

Write-Host "==> Packaging portable app-image (jpackage)..." -ForegroundColor Cyan
Remove-Item "$root\target\dist" -Recurse -Force -ErrorAction SilentlyContinue
& $jpackage --type app-image --name LGSTool `
    --runtime-image "$root\target\lgstool-runtime" `
    --module "de.lgstool/de.lgstool.App" `
    --dest "$root\target\dist" `
    --app-version 1.0.0 --vendor "LGS-Tool" `
    --java-options "--enable-native-access=javafx.graphics"
if ($LASTEXITCODE -ne 0) { throw "jpackage failed." }

Write-Host "==> Done. Portable app: target\dist\LGSTool\LGSTool.exe" -ForegroundColor Green
Write-Host "    Zip the target\dist\LGSTool folder and copy it to any Windows PC." -ForegroundColor Green
