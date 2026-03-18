# Agar_io
AIU Hackathon group project — Agar.io clone made with Java
Group Members : Kamil Yunus Özkaya, Mert Efe Sevim, Ahmet Kurt

## How to Play
You are a circular cell. To eat another cell, your area must be at least **twice** theirs. If you're bigger but lack 2x area, overlapping the smaller cell will **shave** (erode) it — chipping away its mass based on how much you overlap and how fast you're moving. The shaved debris falls off as small food cells at the contact point. Keep pressure on to whittle them down!

As you grow, you'll earn **upgrades** at score milestones — choose from 3 random options each time:
- **Speed Boost** — move faster
- **Size Surge** — instant size increase
- **Regeneration** — slowly recover lost size over time
- **Split Shield** — reduce shave damage taken
- **Magnet** — nearby food drifts toward you
- **Bountiful World** — more food spawns globally
- **Big Feast** — larger food cells appear
- **Dodge [EPIC]** — press **SPACE** to dash (one-time unlock)

**NPCs also get upgrades** — when they cross the same score thresholds, they automatically receive a random upgrade from Speed Boost, Size Surge, Regeneration, or Split Shield. Upgraded NPCs display a gold ★ badge.

Compete against NPC players who also move, eat, grow, and upgrade. Eliminate all NPCs to win! Aim for the highest score!

### Infinite Evolving Cells Mode

An endless challenge unlocked from the main menu's **EVOLVING MODE** button. Instead of a fixed NPC count and a single victory, you progress through infinite stages:

- **Infinite stages** — there is no final win; survive as long as possible and reach the highest stage
- **Stage scaling** — each stage adds more NPCs (starting at 5, +3 per stage, up to 30) and shifts the difficulty mix toward HARD
- **Carry-over** — your cell size, speed, upgrades, and score all carry into the next stage
- **NPC seeding** — enemies start each stage with upgrades proportional to your own upgrade count and the current stage number, keeping the challenge fair
- **Per-player saves** — progress (max stage reached, highest score) is saved automatically to `saves/evolving/<name>.ecfg` by player name; load a previous run from the entry screen
- **Developer log** — the Ctrl+I dev log shows current stage and NPC upgrade diversity for debugging

Controls: **WASD** or **Arrow Keys** to move, **SPACE** to dodge (if unlocked), **ESC** to return to the main menu, **Ctrl+I** (or **Cmd+I** on macOS) to open the developer log.

## Features
- **Infinite Evolving Cells mode** — endless staged challenge from the main menu; NPC count and difficulty auto-scale each stage; player size, speed, upgrades, and score carry over; NPCs seeded with upgrades proportional to player progress; per-player progress saved in `saves/evolving/`
- **Modern UI** — styled buttons with rounded corners, gradient fills, hover glow, and press animations; dark themed animated gradient backgrounds with floating translucent circles; modern styled dialogs replacing system message boxes
- **Ambient sounds** — soothing synthesized pad music in the main menu; evolving pentatonic chord progression during gameplay; all programmatically generated
- **Roguelite upgrades** — 8 upgrade types awarded at score thresholds; choose 1 of 3 random options each time; NPCs also receive upgrades automatically
- **Eat animation** — colored particle burst at the point of consumption
- **Shave (erosion) mechanic** — cells slightly larger than others erode them on contact; damage scales with overlap depth and speed; debris spawns as food at the contact point; replaces the old 2-second division system
- **Bounce animation** — curvy ripple effect with bounce sound when touching an uneatable cell
- **Button hover effects** — smooth color transitions between resting, hover, and pressed states
- **10x radius scoring** — score equals 10 times the cell's current radius; on death/division, score freezes
- **Fixed speed** — all cells move at a constant speed of 3, making gameplay more controllable; dev log still allows manual speed override
- **Fullscreen mode** (default) — game launches in fullscreen; toggle between fullscreen and windowed in Options
- **Configurable world dimensions** — change world size in Options (default 3840 × 2160); minimum 800 × 600
- **Cell density** — controls how many food cells spawn based on world area; configurable in Options and Dev Log
- **Dynamic camera zoom** — camera starts zoomed in so small cells are clearly visible; smoothly zooms out as the player grows larger
- **Cell shaving** — cells bigger but lacking 2x area advantage continuously erode the target's mass on contact; faster cells deal more shave damage; debris spawns as food
- Camera follows the player cell with smooth lerp
- **Toroidal world wrapping** — move off any edge and emerge from the opposite side seamlessly
- **No-overlap spawning** — new cells guaranteed not to appear on top of existing cells
- **Area-based growth** — eating a cell conserves total area (`r3 = sqrt(r1² + r2²)`)
- **Anti-aliased rendering** — all cells drawn with smooth, round edges
- **World Settings panel** — configure player name, NPC count, world dimensions, and cell density before each game; replaces the old separate input dialogs
- **Save/Load system** — save up to 3 configuration presets to files; load, rename, or overwrite saves; restore defaults with one click
- **Player name** — enter your name in World Settings; displayed centered inside your cell
- **NPC difficulty system** — NPCs have EASY, MEDIUM, or HARD difficulty; affects error rate, vision range, steering jitter, mood stability, and food-seeking aggression; distributed equally in round-robin
- **NPC opponents with navigation AI** — AI-controlled cells that intelligently flee from bigger cells and chase smaller ones; choose NPC count (minimum 3) in World Settings
- **Live scoreboard** — top-right corner shows all players ranked by score with difficulty tags [E]/[M]/[H]
- **Game over screen** — final standings, time played, and styled RESTART button
- **Sound effects** — eat bloop, division rise tone, bounce wobble, and UI click sounds; all programmatic
- **Animated gradient background** — slowly shifting pastel colors with 12 drifting semi-transparent blobs
- **Developer log (Ctrl+I / Cmd+I)** — pauses game; edit player name, radius, score, speed, position; manual speed override checkbox for debugging
- Highscore tracked and displayed on the main menu
- Options menu (modern styled buttons and animated background):
  - **SOUND** — toggle sounds on/off (button color changes dynamically)
  - **COLOR** — cycle through available player cell colors
  - **FULLSCREEN** — toggle fullscreen/windowed mode

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
- No online multiplayer (NPCs provide local AI competition)

If you see any errors or have an idea to improve the code, please make comments or open an issue.
Enjoy!!!
