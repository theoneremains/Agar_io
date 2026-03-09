# CLAUDE.md — Agar_io Codebase Guide

This file provides context for AI assistants working on this repository.

## Project Overview

A Java Swing Agar.io clone developed as an AIU Hackathon group project.

**Authors:** Kamil Yunus Özkaya, Mert Efe Sevim, Ahmet Kurt

**Game concept:** The player controls a circular cell. Eating smaller cells increases the player's size and score. The goal is to achieve the highest score.

**Current status:** Functional single-player game with camera tracking, highscore, sound toggle, and player color selection.

---

## Tech Stack

| Area | Technology |
|------|-----------|
| Language | Java (standard library only) |
| GUI | `javax.swing`, `java.awt` |
| Audio | `javax.sound.sampled` (Clip-based) |
| Build | IntelliJ IDEA (no Maven/Gradle) |
| Tests | None |

---

## Project Structure

```
Agar_io/
├── src/                    # All Java source files + game assets
│   ├── MainClass.java      # Entry point, JFrame container, global constants
│   ├── MainPanel.java      # Main menu UI
│   ├── GamePanel.java      # Core game loop, collision, rendering, input, camera
│   ├── Cell.java           # Cell entity model
│   ├── Background.java     # Tiled background rendering with camera offset
│   ├── HUD.java            # Score + elapsed time tracking
│   ├── OptionsPanel.java   # SOUND toggle + COLOR picker settings
│   ├── Sound.java          # Audio playback wrapper (javax.sound.sampled)
│   ├── agario.png          # Menu/options background image
│   ├── background.png      # In-game background image (tiled across world)
│   ├── coolMusic.wav       # Easter egg audio
│   └── click.wav           # Button click sound
├── out/production/Agar_io/ # Compiled .class files (IntelliJ output)
├── .idea/                  # IntelliJ project configuration
├── Agar_io.iml             # IntelliJ module file
└── README.md               # Brief project description
```

---

## Building and Running

**Preferred method:** Open in IntelliJ IDEA (`Agar_io.iml`), then run `MainClass`.

**Command-line alternative:**
```bash
# From project root — compile all source files
javac -d out/production/Agar_io src/*.java

# Run
java -cp out/production/Agar_io MainClass
```

Note: Assets (`.png`, `.wav`) in `src/` must be accessible at runtime. IntelliJ copies them automatically; for command-line builds, copy them to `out/production/Agar_io/` manually.

---

## Source Files and Responsibilities

### `MainClass.java`
- Extends `JFrame` — the top-level window
- Holds global constants — reference these, never hardcode values:
  ```java
  public static int SCREEN_WIDTH  = 1280;
  public static int SCREEN_HEIGHT = 720;
  public static int WORLD_WIDTH   = 3840;   // playable world width
  public static int WORLD_HEIGHT  = 2160;   // playable world height
  public static int BUTTON_WIDTH  = 200;
  public static int BUTTON_HEIGHT = 50;
  ```
- Entry point: `main()` calls `SwingUtilities.invokeLater(MainClass::new)`
- Holds references to all three panels: `mainPanel`, `optionsPanel`, `gamePanel`

### `GamePanel.java`
- Extends `JPanel`, implements `KeyListener`
- **Two daemon game threads:**
  - `cellThread()` — spawns enemy cells up to max 30 across the world, random 0–12 second intervals
  - `runGameThread()` — main loop at 10ms tick (~100 FPS); updates player, camera, collision, score
- **Camera:** `cameraX`/`cameraY` computed each tick; `g2d.translate(-cameraX, -cameraY)` shifts rendering to world space; restored before HUD drawing
- **Rendering:** overrides `paintComponent(Graphics g)` — call `super.paintComponent(g)` first
- **Score** stored in `hud.score`; `highscore` updated whenever `hud.score > highscore`
- **Player color:** `static Color playerColor` — set in Options, applied to playerCell on game start
- **Easter egg:** when `playerCell.cellRad > 400`, plays `coolMusic.wav` and resets timer
- `colors[]` is `static` so OptionsPanel can reference it for the color picker

### `Cell.java`
- Stores: `x`, `y`, `cellRad`, `cellColor`, `speedX = 5`, `speedY = 5`
- **Constructor:** `Cell(int centerX, int centerY, int radius)` — sets `x = centerX - radius`, `y = centerY - radius`
  - The `screenWidth`/`screenHeight` fields are legacy dead code; bounds checking uses `MainClass.WORLD_WIDTH/HEIGHT`
- `drawCell(Graphics2D, int radius)` — renders a filled oval at world coordinates
- `updateCellPos(boolean right, left, up, down)` — moves with boundary checks against world bounds (`WORLD_WIDTH`, `WORLD_HEIGHT`)
- `isCollision(Cell player, Cell enemy)` — circle-circle collision:
  - Only triggers if `playerRad > enemyRad + 4` (minimum 4-unit size advantage)
  - Center-to-center distance formula

### `HUD.java`
- Extends `JPanel` (though only used as a data class)
- `score` — integer incremented on each eaten cell
- `elapsedTime` — milliseconds since `startTime`; reset by `resetTime()` on easter egg trigger

### `Background.java`
- Loads and tiles `background.png` across the visible viewport
- `drawBackground(Graphics2D g, int cameraX, int cameraY)` — call while transform is `translate(-cameraX, -cameraY)`; draws enough tiles to fill `screenWidth × screenHeight` from the camera position

### `MainPanel.java`
- Main menu with START, OPTIONS, EXIT buttons
- Button clicks play `click.wav`
- START creates `new GamePanel(mainClass)` and swaps panels
- `paintComponent` draws background image; `paint` calls `super.paint(g)` then overlays title and highscore text

### `Sound.java`
- Wraps `javax.sound.sampled.Clip` for `.wav` playback
- `static boolean soundEnabled = true` — respects this flag; set to `false` to mute all sounds
- Constructor: `new Sound(filename, soundSeconds)`
- Play: `sound.playSound()` — no-ops when `soundEnabled == false`
- Stop: `sound.closeSound()`
- All threads set as daemon threads

### `OptionsPanel.java`
- **SOUND button:** toggles `Sound.soundEnabled`; label shows "SOUND: ON" / "SOUND: OFF"
- **COLOR button:** cycles `GamePanel.playerColorIndex` through `GamePanel.colors[]`; shows selected color in `colorPreview` panel; change applies to the next game started
- **BACK button:** returns to main menu
- `paintComponent` draws background image; `paint` calls `super.paint(g)` then overlays title and highscore

---

## Architecture Patterns

### Panel/Screen Switching
```java
mainClass.getContentPane().removeAll();
mainClass.getContentPane().add(newPanel);
mainClass.revalidate();
```

### Game Loop (both threads follow this pattern)
```java
Thread t = new Thread() {
    @Override
    public void run() {
        while (true) {
            // ... logic ...
            repaint();
            try { Thread.sleep(ms); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }
};
t.setDaemon(true);
t.start();
```

### Camera System
World objects are rendered in world space, HUD in screen space:
```java
g2d.translate(-cameraX, -cameraY);   // enter world space
background.drawBackground(g2d, cameraX, cameraY);
// draw cells and player at their world x, y
g2d.translate(cameraX, cameraY);     // restore screen space
// draw HUD elements at screen coordinates
```

Camera position computed each tick:
```java
int cx = playerCell.x + playerCell.cellRad - SCREEN_WIDTH / 2;
int cy = playerCell.y + playerCell.cellRad - SCREEN_HEIGHT / 2;
cameraX = clamp(cx, 0, WORLD_WIDTH  - SCREEN_WIDTH);
cameraY = clamp(cy, 0, WORLD_HEIGHT - SCREEN_HEIGHT);
```

### Cell Growth Formula (volume-based)
```java
int eatenRad = celllist.get(i).cellRad; // save BEFORE removing from list
celllist.remove(i);
int newRad = (int) Math.pow(Math.pow(playerCell.cellRad, 3) + Math.pow(eatenRad, 3), 1.0 / 3);
if (newRad <= playerCell.cellRad) newRad = playerCell.cellRad + 1;
playerCell.cellRad = newRad;
```

### Thread Safety
`celllist` is a `CopyOnWriteArrayList<Cell>` — safe for reads from the paint thread and concurrent writes from `cellThread` and `runGameThread`.

### Enemy Cell Color Palette
Use the existing static palette in `GamePanel.colors[]`:
```java
public static Color[] colors = {BLACK, BLUE, CYAN, DARK_GRAY, GRAY, GREEN, LIGHT_GRAY, MAGENTA, ORANGE, YELLOW, PINK};
```

---

## Key Conventions

1. **JavaDoc on every class** — include `@author` tag, brief purpose, and notable behavior.
2. **Use `MainClass` constants** for screen, world, and button dimensions — never hardcode `1280`, `720`, `3840`, `2160`, `200`, or `50`.
3. **Player initial state:** radius `cellrad = 18`, spawns at world center `(WORLD_WIDTH/2, WORLD_HEIGHT/2)`, color = `GamePanel.playerColor`.
4. **Enemy cell radius:** always `rndcellrad = 13`.
5. **Spawn positions use world coordinates** — random in `[rndcellrad, WORLD_WIDTH - rndcellrad]` × `[rndcellrad, WORLD_HEIGHT - rndcellrad - 25]`.
6. **Assets live in `src/`** alongside Java files — do not create a separate `resources/` directory without updating the build setup.
7. **No external libraries** — the project has no dependency manager; do not add third-party JARs without establishing a build system first.
8. **All game threads are daemon threads** — they terminate automatically when the JVM exits.

---

## What to Avoid

- Do not switch to Maven or Gradle without team agreement.
- Do not add external dependencies — no dependency management system is configured.
- Do not call `g2d.translate(...)` for world-space drawing without restoring with the inverse translate before drawing HUD elements.
- Do not use `paint()` without calling `super.paint(g)` first — use `paintComponent()` for game panel rendering.
- Do not access `celllist` without going through the `CopyOnWriteArrayList` API — never replace it with a plain `ArrayList`.
- Do not read an eaten cell's data from `celllist` after calling `celllist.remove(i)`.
- Do not break the two-thread game loop pattern without profiling impact on frame rate.
