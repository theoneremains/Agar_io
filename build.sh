#!/usr/bin/env bash
set -euo pipefail

echo "=== Agar.io Build Script ==="
echo

# ── Paths ──────────────────────────────────────────────────────────────────
SRC_DIR="src"
OUT_DIR="out/production/Agar_io"
JAR_FILE="AgarIO.jar"

# ── Clean previous build output ────────────────────────────────────────────
if [ -d "$OUT_DIR" ]; then
    echo "Cleaning previous build..."
    rm -rf "$OUT_DIR"
fi
mkdir -p "$OUT_DIR"

# ── Compile Java sources ───────────────────────────────────────────────────
echo "Compiling Java sources..."
javac -d "$OUT_DIR" "$SRC_DIR"/*.java
echo "Compilation successful."
echo

# ── Copy assets ───────────────────────────────────────────────────────────
echo "Copying assets..."
cp "$SRC_DIR"/*.png "$OUT_DIR"/
cp "$SRC_DIR"/*.wav "$OUT_DIR"/
echo "Assets copied."
echo

# ── Write MANIFEST.MF ─────────────────────────────────────────────────────
mkdir -p "$OUT_DIR/META-INF"
printf 'Main-Class: MainClass\n\n' > "$OUT_DIR/META-INF/MANIFEST.MF"

# ── Package into a runnable JAR ───────────────────────────────────────────
echo "Creating runnable JAR: $JAR_FILE..."
jar cfm "$JAR_FILE" "$OUT_DIR/META-INF/MANIFEST.MF" -C "$OUT_DIR" .
echo

echo "========================================================="
echo " Build complete!"
echo
echo " To run the game:"
echo "   java -jar AgarIO.jar"
echo
echo " To create a native installer:"
echo "   ./package.sh           (Linux/macOS)"
echo "   package.bat            (Windows)"
echo "========================================================="
