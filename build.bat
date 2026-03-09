@echo off
setlocal

echo === Agar.io Build Script ===
echo.

REM ── Paths ──────────────────────────────────────────────────────────────────
set SRC_DIR=src
set OUT_DIR=out\production\Agar_io
set JAR_FILE=AgarIO.jar

REM ── Clean previous build output ────────────────────────────────────────────
if exist "%OUT_DIR%" (
    echo Cleaning previous build...
    rmdir /s /q "%OUT_DIR%"
)
mkdir "%OUT_DIR%"

REM ── Compile Java sources ───────────────────────────────────────────────────
echo Compiling Java sources...
javac -d "%OUT_DIR%" "%SRC_DIR%\*.java"
if errorlevel 1 (
    echo.
    echo ERROR: Compilation failed. Make sure the JDK is installed and in your PATH.
    exit /b 1
)
echo Compilation successful.
echo.

REM ── Copy assets ───────────────────────────────────────────────────────────
echo Copying assets...
copy /y "%SRC_DIR%\*.png" "%OUT_DIR%\" >nul
copy /y "%SRC_DIR%\*.wav" "%OUT_DIR%\" >nul
echo Assets copied.
echo.

REM ── Write MANIFEST.MF ─────────────────────────────────────────────────────
if not exist "%OUT_DIR%\META-INF" mkdir "%OUT_DIR%\META-INF"
(
    echo Main-Class: MainClass
    echo.
) > "%OUT_DIR%\META-INF\MANIFEST.MF"

REM ── Package into a runnable JAR ───────────────────────────────────────────
echo Creating runnable JAR: %JAR_FILE%...
jar cfm "%JAR_FILE%" "%OUT_DIR%\META-INF\MANIFEST.MF" -C "%OUT_DIR%" .
if errorlevel 1 (
    echo.
    echo ERROR: JAR creation failed.
    exit /b 1
)
echo.
echo =========================================================
echo  Build complete!
echo.
echo  To run the game:
echo    Double-click AgarIO.jar   (if Java is associated)
echo    -- OR --
echo    java -jar AgarIO.jar
echo.
echo  To create a standalone Windows installer (.exe):
echo    Run package.bat
echo =========================================================

endlocal
