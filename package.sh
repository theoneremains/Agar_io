#!/usr/bin/env bash
set -euo pipefail

echo "=== Agar.io Packager (Linux / macOS) ==="
echo

# ── Prerequisites check ───────────────────────────────────────────────────
if ! command -v jpackage &>/dev/null; then
    echo "ERROR: jpackage not found. Requires JDK 14 or newer."
    echo "       Install via: sudo apt install openjdk-21-jdk   (Ubuntu/Debian)"
    echo "                or: brew install openjdk               (macOS)"
    exit 1
fi

# ── Build the JAR first ───────────────────────────────────────────────────
if [ ! -f "AgarIO.jar" ]; then
    echo "AgarIO.jar not found. Running build.sh first..."
    echo
    bash build.sh
    echo
fi

# ── Output directory ─────────────────────────────────────────────────────
DIST_DIR="dist"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

# ── Detect OS and choose package type ────────────────────────────────────
OS="$(uname -s)"
case "$OS" in
    Linux*)
        # Prefer deb on Debian/Ubuntu, rpm on Fedora/RHEL
        if command -v dpkg &>/dev/null; then
            PKG_TYPE="deb"
        elif command -v rpm &>/dev/null; then
            PKG_TYPE="rpm"
        else
            PKG_TYPE="app-image"
        fi
        ;;
    Darwin*)
        PKG_TYPE="dmg"
        ;;
    *)
        PKG_TYPE="app-image"
        ;;
esac

echo "Detected OS: $OS  →  package type: $PKG_TYPE"
echo "(This bundles a JRE — no Java installation required on target machine)"
echo

# ── Run jpackage ──────────────────────────────────────────────────────────
jpackage \
  --type "$PKG_TYPE" \
  --name "AgarIO" \
  --app-version "1.0" \
  --description "Agar.io clone — AIU Hackathon project" \
  --vendor "AIU Hackathon Team" \
  --input . \
  --main-jar AgarIO.jar \
  --main-class MainClass \
  --dest "$DIST_DIR" \
  --java-options "-Xmx256m"

echo
echo "========================================================="
echo " Packaging complete!"
echo " Output located in: $DIST_DIR/"
ls "$DIST_DIR/"
echo
echo " Distribute the package — users do NOT need Java."
echo "========================================================="
