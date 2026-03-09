# CLAUDE.md — Agar_io Codebase Guide

This file provides context for AI assistants working on this repository.

## Project Overview

An incomplete Java Swing Agar.io clone developed as an AIU Hackathon group project.

**Authors:** Kamil Yunus Özkaya, Mert Efe Sevim, Ahmet Kurt

**Game concept:** The player controls a circular cell. Eating smaller cells increases the player's size and score. The goal is to achieve the highest score.

**Current status:** Incomplete — single player only, fixed-size world, camera does not follow the player.

---

## Tech Stack

| Area | Technology |
|------|-----------|
| Language | Java (standard library only) |
| GUI | `javax.swing`, `java.awt` |
| Audio | `sun.audio` (deprecated legacy API — do not replace without testing) |
| Build | IntelliJ IDEA (no Maven/Gradle) |
| Tests | None |

---

## Project Structure

```
Agar_io/
├── src/                    # All Java source files + game assets
│   ├── MainClass.java      # Entry point, JFrame container, global constants
│   ├── MainPanel.java      # Main menu UI
│   ├── GamePanel.java      # Core game loop, collision, rendering, input
│   ├── Cell.java           # Cell entity model
│   ├── Background.java     # Background rendering
│   ├── HUD.java            # Score + elapsed time tracking
│   ├── OptionsPanel.java   # Placeholder (not functional)
│   ├── Sound.java          # Audio playback wrapper
│   ├── agario.png          # Menu/options background image
│   ├── background.png      # In-game background image
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
- Holds global screen/button constants (reference these instead of hardcoding):
  ```java
  public static int SCREEN_WIDTH  = 1280;
  public static int SCREEN_HEIGHT = 720;
  public static int BUTTON_WIDTH  = 200;
  public static int BUTTON_HEIGHT = 50;
  ```
- Entry point: `main()` calls `SwingUtilities.invokeLater(MainClass::new)`
- Holds references to all three panels: `mainPanel`, `optionsPanel`, `gamePanel`

### `GamePanel.java`
- Extends `JPanel`, implements `KeyListener`
- **Two game threads** (raw `Thread`, not `ExecutorService`):
  - `cellThread()` — spawns enemy cells up to max 30, random 0–12 second intervals
  - `runGameThread()` — main loop at 10ms tick (~100 FPS)
- **Rendering:** overrides `paint()` (not `paintComponent`) — preserve this
- **Score** stored in `hud.score`; `highscore` field exists but is currently unused
- **Easter egg:** when `playerCell.cellRad > 400`, plays `coolMusic.wav` and resets timer

### `Cell.java`
- Stores: `x`, `y`, `cellRad`, `cellColor`, `speedX = 5`, `speedY = 5`
- `drawCell(Graphics2D, int radius)` — renders a filled oval
- `updateCellPos(boolean right, left, up, down)` — moves with boundary checks against `MainClass.SCREEN_WIDTH/HEIGHT`
- `isCollision(Cell player, Cell enemy)` — circle-circle collision:
  - Only triggers if `playerRad > enemyRad + 4` (minimum 4-unit size advantage)
  - Uses center-to-center distance formula

### `HUD.java`
- Extends `JPanel` (though only used as a data class)
- `score` — integer incremented on each eaten cell
- `elapsedTime` — milliseconds since `startTime`; reset by `resetTime()` on easter egg trigger

### `Background.java`
- Loads and renders `background.png` with configurable opacity

### `MainPanel.java`
- Main menu with START, OPTIONS, EXIT buttons
- Button clicks play `click.wav`
- START creates `new GamePanel(mainClass)` and swaps panels

### `Sound.java`
- Wraps `sun.audio` for `.wav` playback
- Constructor: `new Sound(filename, 1)`
- Play: `sound.playSound()`

### `OptionsPanel.java`
- Placeholder — no working options are implemented

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
t.start();
```

### Cell Growth Formula (volume-based)
```java
newRad = (int) Math.pow(Math.pow(r1, 3) + Math.pow(r2, 3), 1.0 / 3);
```
A fallback `+= 1` applies when the cube-root formula would produce no change.

### Enemy Cell Colors
Use the existing palette in `GamePanel.colors[]`:
```java
Color[] colors = {BLACK, BLUE, CYAN, DARK_GRAY, GRAY, GREEN, LIGHT_GRAY, MAGENTA, ORANGE, YELLOW, PINK};
```

---

## Key Conventions

1. **JavaDoc on every class** — include `@author` tag, brief purpose, and notable behavior.
2. **Use `MainClass` constants** for screen/button dimensions — never hardcode `1280`, `720`, `200`, or `50`.
3. **Player initial state:** radius `cellrad = 18`, spawns at `(SCREEN_WIDTH/2, SCREEN_HEIGHT/2)`.
4. **Enemy cell radius:** always `rndcellrad = 13`.
5. **Assets live in `src/`** alongside Java files — do not create a separate `resources/` directory without updating the build setup.
6. **No external libraries** — the project has no dependency manager; do not add third-party JARs without establishing a build system first.

---

## Known Incomplete Areas

These are acknowledged gaps — do not remove these notes:

- **Camera:** Does not follow the player cell; the world is fixed at screen size.
- **Multiplayer:** Single player only.
- **World size:** Bounded by screen dimensions (1280x720) — no scrolling world.
- **Options panel:** `OptionsPanel.java` exists but has no functional settings.
- **`highscore`:** Declared as `public static int highscore = 0` in `GamePanel` but never updated or displayed.
- **Thread safety:** `celllist` (`ArrayList<Cell>`) is accessed from both `cellThread` and `runGameThread` without synchronization.

---

## What to Avoid

- Do not switch to Maven or Gradle without team agreement.
- Do not replace `sun.audio` with a modern API (`javax.sound.sampled`) without thorough testing across platforms.
- Do not change `paint()` to `paintComponent()` without verifying repaint behavior.
- Do not add external dependencies — no dependency management system is configured.
- Do not break the two-thread game loop pattern without profiling impact on frame rate.
