@echo off
setlocal

echo === Agar.io Packager (Windows Installer / EXE) ===
echo.

REM ── Prerequisites check ───────────────────────────────────────────────────
where jpackage >nul 2>&1
if errorlevel 1 (
    echo ERROR: jpackage not found. Requires JDK 14 or newer.
    echo        Download from: https://adoptium.net/
    exit /b 1
)

REM ── Build the JAR first ───────────────────────────────────────────────────
if not exist "AgarIO.jar" (
    echo AgarIO.jar not found. Running build.bat first...
    echo.
    call build.bat
    if errorlevel 1 exit /b 1
    echo.
)

REM ── Output directory ─────────────────────────────────────────────────────
set DIST_DIR=dist

if exist "%DIST_DIR%" (
    echo Cleaning previous dist...
    rmdir /s /q "%DIST_DIR%"
)
mkdir "%DIST_DIR%"

REM ── Run jpackage ──────────────────────────────────────────────────────────
echo Creating Windows installer...
echo (This bundles a JRE — no Java installation required on target machine)
echo.

jpackage ^
  --type exe ^
  --name "AgarIO" ^
  --app-version "1.0" ^
  --description "Agar.io clone — AIU Hackathon project" ^
  --vendor "AIU Hackathon Team" ^
  --input . ^
  --main-jar AgarIO.jar ^
  --main-class MainClass ^
  --dest "%DIST_DIR%" ^
  --win-menu ^
  --win-shortcut ^
  --win-dir-chooser ^
  --win-per-user-install ^
  --java-options "-Xmx256m"

if errorlevel 1 (
    echo.
    echo NOTE: If the exe type failed, trying msi...
    jpackage ^
      --type msi ^
      --name "AgarIO" ^
      --app-version "1.0" ^
      --description "Agar.io clone — AIU Hackathon project" ^
      --vendor "AIU Hackathon Team" ^
      --input . ^
      --main-jar AgarIO.jar ^
      --main-class MainClass ^
      --dest "%DIST_DIR%" ^
      --win-menu ^
      --win-shortcut ^
      --java-options "-Xmx256m"
)

if errorlevel 1 (
    echo.
    echo ERROR: Packaging failed.
    echo        On Windows, WiX Toolset 3.x is required for exe/msi.
    echo        Download from: https://wixtoolset.org/
    exit /b 1
)

echo.
echo =========================================================
echo  Packaging complete!
echo  Installer located in: %DIST_DIR%\
echo.
echo  Distribute the installer — users do NOT need Java.
echo =========================================================

endlocal
