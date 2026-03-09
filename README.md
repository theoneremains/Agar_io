# Agar_io
AIU Hackathon group project — Agar.io clone made with Java
Group Members : Kamil Yunus Özkaya, Mert Efe Sevim, Ahmet Kurt

## How to Play
You are a circular cell that can eat other cells smaller than you, and you get bigger. Score increases as player eats other cells.
Aim is to get the highest score!

Controls: **WASD** or **Arrow Keys** to move, **ESC** to return to the main menu, **Ctrl+I** (or **Cmd+I** on macOS) to open the developer log.

## Features
- Camera follows the player cell across a large scrolling world (3840 × 2160) with smooth lerp
- **Toroidal world wrapping** — move off any edge and emerge from the opposite side seamlessly
- **Enemy cells with random sizes** — radius varies from 8 to 35 pixels (capped below the player's current size), making every encounter feel different
- **No-overlap spawning** — new enemy cells are guaranteed not to appear on top of the player or existing cells (up to 50 retry attempts per spawn)
- **Volume-based growth** — eating a cell always grows the player by at least the eaten cell's radius, never just 1 pixel
- **Anti-aliased rendering** — all cells are drawn with `RenderingHints.VALUE_ANTIALIAS_ON` for smooth, round edges
- **Player name** — enter your name before each game; it is displayed centered and font-size fitted inside your cell as it grows
- **Dynamic player speed** — starts at 7 px/tick at radius 18; speed scales down as the player grows but never reaches zero (`max(1, 7 × 18 / radius)`)
- **Smooth cell spawn animation** — new enemy cells grow in over ~200 ms instead of appearing instantly
- **Eat sound effect** — a short descending "bloop" tone plays each time a cell is consumed (generated programmatically, no extra audio file needed); respects the SOUND toggle
- **Animated gradient background** — slowly shifting pastel colors with 12 drifting semi-transparent cell-like blobs replace the former static image
- **Developer log (Ctrl+I / Cmd+I)** — pauses the game and opens an editable overlay showing: player name, radius, score, speed (X/Y), world position, and live enemy count. All fields are editable; click "Apply & Resume" or close the window to resume play. Tick "Manual Speed Override" to pin speed independently of the dynamic calculation.
- Highscore is tracked and displayed on the main menu between games
- Options menu:
  - **SOUND** — toggle all game sounds on/off
  - **COLOR** — cycle through available player cell colors

## Installation & Running

### Option 1 — Native Installer (recommended, no Java required)

Build a self-contained installer that bundles its own JRE. Users just install and double-click.

**Prerequisites:** JDK 14+ on the *developer* machine. On Windows, also install [WiX Toolset 3.x](https://wixtoolset.org/) for `.exe` output.

```bat
:: Windows — produces dist\AgarIO-1.0.exe
package.bat
```
```bash
# Linux  → dist/agario_1.0_amd64.deb  (or .rpm / app-image)
# macOS  → dist/AgarIO-1.0.dmg
bash package.sh
```

Distribute the file in `dist/` — recipients do **not** need Java installed.

---

### Option 2 — Runnable JAR (requires Java 8+ on target machine)

```bat
:: Windows
build.bat
java -jar AgarIO.jar
```
```bash
# Linux / macOS
bash build.sh
java -jar AgarIO.jar
```

---

### Option 3 — IntelliJ IDEA (for development)

Open `Agar_io.iml` in IntelliJ IDEA and run `MainClass`.

---

### Option 4 — Raw command line

```bash
javac -d out/production/Agar_io src/*.java
cp src/*.png src/*.wav out/production/Agar_io/
java -cp out/production/Agar_io MainClass
```

## Known Limitations
- Single player only (no multiplayer)

If you see any errors or have an idea to improve the code, please make comments or open an issue.
Enjoy!!!
