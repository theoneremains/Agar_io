# CLAUDE.md — Agar_io Codebase Guide

This file provides context for AI assistants working on this repository.

> **AI Assistant Instruction:** Always analyze and update this `CLAUDE.md` file before and after making significant changes to the codebase. Keep the project structure, current status, and source file descriptions accurate and up to date so future assistants have reliable context.

## Project Overview

A Java Swing Agar.io clone developed as an AIU Hackathon group project.

**Authors:** Kamil Yunus Özkaya, Mert Efe Sevim, Ahmet Kurt

**Game concept:** The player controls a circular cell. Eating smaller cells increases the player's size and score. The goal is to achieve the highest score.

**Current status:** Functional game with intelligent NPC opponents (navigation AI with difficulty levels EASY/MEDIUM/HARD — flee bigger, chase smaller, roam when idle), smooth camera tracking, highscore, sound toggle, player color selection, fullscreen mode (default), configurable world dimensions, configurable cell density (default 200 cells/M px), toroidal world wrapping, three-tier food cell categories (Small/Medium/Large), smooth cell spawn animation, eat particle effects, shave-based erosion mechanics (overlap depth × speed = area loss, spawns food debris), bounce contact effects, ambient menu music (A major pad) and evolving game ambient (pentatonic chord progression), modernized UI with styled buttons (hover/press animations) and styled dialogs (replacing JOptionPane), animated gradient menu backgrounds, fixed speed (3), no-overlap spawn, area-based growth (`r3 = sqrt(r1² + r2²)`), 10x-radius scoring, double-precision cell radii, live scoreboard with difficulty tags, game over screen with stats, in-game developer log, World Settings panel (pre-game configuration for player name, NPC count, world size, cell density), persistent save/load system (up to 3 save files with rename and default restore), roguelite upgrade system with 8 upgrade types (Speed Boost, Size Surge, Regeneration, Split Shield, Bountiful World, Big Feast, Magnet, Dodge) awarded at score thresholds — NPCs also receive upgrades automatically from an eligible subset — and **Infinite Evolving Cells mode**: infinite stages with auto-scaling NPC count/difficulty per stage, player upgrades/size/score persist across stage transitions, NPCs seeded with upgrades based on stage and player progress, per-player save files in `saves/evolving/`, and NPC upgrade diversity shown in the developer log.

---

## Tech Stack

| Area | Technology |
|------|-----------|
| Language | Java (standard library only) |
| GUI | `javax.swing`, `java.awt` |
| Audio | `javax.sound.sampled` (Clip + `SourceDataLine` for programmatic tones) |
| Build | IntelliJ IDEA / `build.sh` / `build.bat` (no Maven/Gradle) |
| Packaging | `jpackage` (JDK 14+) via `package.sh` / `package.bat` |
| Tests | None |

---

## Project Structure

```
Agar_io/
├── src/                        # All Java source files + game assets
│   ├── MainClass.java          # Entry point, JFrame container, screen/world dimensions
│   ├── GameConstants.java      # Centralized constants, magic numbers, and utility methods
│   ├── MainPanel.java          # Main menu UI (START → WorldSettingsPanel)
│   ├── GamePanel.java          # Core game loop, input, camera, thread management
│   ├── CollisionHandler.java   # All collision detection and resolution (extracted from GamePanel)
│   ├── GameRenderer.java       # All rendering/drawing logic (extracted from GamePanel)
│   ├── Cell.java               # Cell entity model with static collision check
│   ├── Background.java         # In-game animated gradient + drifting blob world background
│   ├── MenuBackground.java     # Reusable animated gradient background for menu panels
│   ├── HUD.java                # Score + elapsed time tracking (data class, not a JPanel)
│   ├── OptionsPanel.java       # SOUND toggle + COLOR picker + FULLSCREEN settings
│   ├── Sound.java              # Audio playback — delegates tone synthesis to ToneGenerator
│   ├── ToneGenerator.java      # Programmatic audio tone synthesis (waveform lambdas)
│   ├── NPC.java                # NPC entity — AI-controlled cells with difficulty levels
│   ├── DevLogDialog.java       # Developer log window (Ctrl+I) — pauses game, editable world stats
│   ├── DivisionEffect.java     # Division split animation with bezier curves
│   ├── EatEffect.java          # Particle burst animation when a cell is eaten
│   ├── ContactEffect.java      # Curvy ripple animation at cell contact points
│   ├── StyledButton.java       # Modern animated button with hover/press effects
│   ├── StyledDialog.java       # Modern dark-themed dialog replacements for JOptionPane
│   ├── WorldSettingsPanel.java  # Pre-game settings: name, NPC count, world size, density, save/load
│   ├── GameSettings.java       # Settings persistence: save/load/rename with max 3 save files
│   ├── EvolvingModePanel.java  # Infinite Evolving Cells mode entry screen (name field, load save, start)
│   ├── EvolvingProgressSave.java # Per-player evolving progress: max stage, highest score (.ecfg files)
│   ├── agario.png              # Legacy background image (unused — backgrounds are procedural)
│   ├── background.png          # Legacy background image (unused — backgrounds are procedural)
│   ├── coolMusic.wav           # Easter egg audio
│   └── click.wav               # Button click sound
├── saves/                      # User save files (.cfg), max 3 — created at runtime
├── saves/evolving/             # Per-player evolving mode progress files (.ecfg) — created at runtime
├── out/production/Agar_io/     # Compiled .class files (IntelliJ output)
├── dist/                       # Native installer output (generated by package scripts, git-ignored)
├── .idea/                      # IntelliJ project configuration
├── Agar_io.iml                 # IntelliJ module file
├── build.sh                    # Linux/macOS: compile + create AgarIO.jar
├── build.bat                   # Windows: compile + create AgarIO.jar
├── package.sh                  # Linux/macOS: jpackage native installer
├── package.bat                 # Windows: jpackage .exe installer
├── .gitignore                  # Excludes AgarIO.jar, dist/
└── README.md                   # Brief project description
```

---

## Building and Running

### Build scripts (preferred for distribution)

| Script | Platform | Output |
|--------|----------|--------|
| `build.sh` | Linux / macOS | `AgarIO.jar` (runnable fat JAR) |
| `build.bat` | Windows | `AgarIO.jar` (runnable fat JAR) |
| `package.sh` | Linux / macOS | `dist/` — `.deb`, `.rpm`, or app-image |
| `package.bat` | Windows | `dist/AgarIO-1.0.exe` (requires WiX Toolset 3.x) |

```bash
# Build runnable JAR
bash build.sh        # Linux/macOS
build.bat            # Windows

# Run the JAR
java -jar AgarIO.jar

# Create native installer (bundles JRE — no Java needed on target machine)
bash package.sh      # Linux/macOS
package.bat          # Windows
```

`package.*` uses **`jpackage`** (built into JDK 14+). No Maven/Gradle is needed.

### IntelliJ IDEA

Open `Agar_io.iml`, then run `MainClass`. IntelliJ copies assets automatically.

### Raw command line

```bash
javac -d out/production/Agar_io src/*.java
cp src/*.png src/*.wav out/production/Agar_io/
java -cp out/production/Agar_io MainClass
```

Note: Assets (`.png`, `.wav`) live in `src/` alongside Java files. The build scripts copy them automatically; raw command-line builds require a manual copy.

---

## Source Files and Responsibilities

### `GameConstants.java` *(new — extracted)*
- Centralized repository for all magic numbers, constants, and utility methods
- **Screen/world defaults:** `DEFAULT_SCREEN_WIDTH` (1280), `DEFAULT_SCREEN_HEIGHT` (720), `DEFAULT_WORLD_WIDTH` (3840), `DEFAULT_WORLD_HEIGHT` (2160)
- **Cell properties:** `INITIAL_CELL_RADIUS` (2), `DEFAULT_SPEED` (3), `COLLISION_SIZE_ADVANTAGE` (0.5), `MIN_DIVIDE_RADIUS` (6)
- **Food categories:** `FOOD_SMALL_CHANCE` (0.90), `FOOD_MEDIUM_CHANCE` (0.07), size ranges for each tier
- **NPC settings:** `MIN_NPC_COUNT` (3), `NPC_NAMES[]` (20 names), `DEFAULT_CELL_DENSITY` (200.0)
- **Camera:** `CAMERA_LERP` (0.15), `CAMERA_ZOOM_LERP` (0.05), `MIN_ZOOM`/`MAX_ZOOM`
- **Timing:** `GAME_TICK_MS` (10), `CELL_SPAWN_INTERVAL_MS` (500), `CELL_SPAWN_BATCH_SIZE` (10), `SPAWN_ALPHA_INCREMENT` (0.05f)
- **Effects limits:** `MAX_EAT_EFFECTS` (50), `MAX_CONTACT_EFFECTS` (30), `MAX_DIVISION_EFFECTS` (20)
- **UI theme:** `BTN_GREEN`, `BTN_BLUE`, `BTN_RED`, `BTN_ON`, `BTN_OFF`, `FONT_FAMILY`, `FONT_FAMILY_MONO`
- **Color palette:** `CELL_COLORS[]` — shared across all cell coloring
- **Save system:** `MAX_SAVE_FILES` (3), `SAVES_DIR`, `SAVE_EXT`
- **Evolving mode:** `EVOLVING_SAVES_DIR` (`saves/evolving`), `EVOLVING_SAVE_EXT` (`.ecfg`), `EVOLVING_BASE_NPC_COUNT` (5), `EVOLVING_NPC_INCREMENT` (3), `EVOLVING_MAX_NPC_COUNT` (30), `BTN_PURPLE` (purple button color for evolving mode)
- **Utility methods:** `growRadius(r1, r2)` (area-based growth), `scoreFromRadius(radius)` (10x scoring), `distSq(x1,y1,x2,y2)` (squared distance)
- **Usage rule:** Always reference `GameConstants.*` instead of inline magic numbers

### `MainClass.java`
- Extends `JFrame` — the top-level window
- Holds mutable screen/world state (initialized from `GameConstants` defaults):
  ```java
  public static int SCREEN_WIDTH  = GameConstants.DEFAULT_SCREEN_WIDTH;
  public static int SCREEN_HEIGHT = GameConstants.DEFAULT_SCREEN_HEIGHT;
  public static int WORLD_WIDTH   = GameConstants.DEFAULT_WORLD_WIDTH;
  public static int WORLD_HEIGHT  = GameConstants.DEFAULT_WORLD_HEIGHT;
  public static boolean fullscreen = true;
  ```
- `BUTTON_WIDTH`/`BUTTON_HEIGHT` are deprecated — use `GameConstants.BUTTON_WIDTH/HEIGHT`
- **Fullscreen mode:** defaults to `true`; `applyFullscreen()` sets `SCREEN_WIDTH`/`SCREEN_HEIGHT` to display dimensions and removes window decorations; `applyWindowed()` restores 1280×720 windowed mode; `toggleFullscreen()` switches between modes
- Entry point: `main()` calls `SwingUtilities.invokeLater(MainClass::new)`
- Holds references to all panels: `mainPanel`, `optionsPanel`, `gamePanel`, `worldSettingsPanel`

### `GamePanel.java` *(refactored — collision and rendering extracted)*
- Extends `JPanel`, implements `KeyListener`
- **Constructor:** `GamePanel(MainClass mainClass, int npcCount)` — standard mode; accepts the number of NPC players (minimum 3); sets `evolvingMode=false`
- **Evolving constructor:** `GamePanel(MainClass mainClass, EvolvingProgressSave evolvingProgress)` — evolving mode; sets `evolvingMode=true`, `currentStage=1`, calls `spawnNPCsForEvolvingStage(1)`
- **Evolving mode fields:** `evolvingMode` (boolean), `currentStage` (int), `stageTransitioning` (volatile boolean), `evolvingProgress` (EvolvingProgressSave)
- **`spawnNPCsForEvolvingStage(int stage)`** — calculates NPC count (`min(5+(stage-1)*3, 30)`) and difficulty fractions (EASY decreases, HARD increases each stage); seeds NPCs with upgrades based on stage and player's current upgrade count; calls `spawnEvolvingNPCsOfDifficulty()` for each tier
- **`triggerStageComplete()`** — saves progress, sets `stageTransitioning=true`, adds "NEXT STAGE ▶" button overlay
- **`nextStage()`** — increments `currentStage`, saves progress, clears NPCs and food, respawns via `spawnNPCsForEvolvingStage()`, clears `stageTransitioning`
- **Evolving accessors:** `isEvolvingMode()`, `getCurrentStage()`, `isStageTransitioning()`
- **Delegates to:** `CollisionHandler` (all collision logic), `GameRenderer` (all drawing)
- **Thread management:** `volatile boolean running = true` — threads check this flag and exit cleanly when `returnToMenu()` is called; eliminates thread leaks on restart
- **Two daemon game threads:**
  - `startCellSpawnThread()` — initial batch spawn fills world to max capacity, then tops up in batches of `CELL_SPAWN_BATCH_SIZE` every `CELL_SPAWN_INTERVAL_MS`; stops when `gameOver` or `!running`
  - `startGameThread()` — main loop at `GAME_TICK_MS` (~100 FPS); updates player, NPC AI, advances `spawnAlpha`, camera lerp, delegates to `CollisionHandler`; skips logic when `paused` or `gameOver`
- **Public accessors:** `getPlayerCell()`, `getFoodCells()`, `getNPCList()`, `getHUD()`, `getWorldBackground()`, `getCameraX/Y()`, `getCameraZoom()`, `getScoreboard()`, `getDivisionEffects()`, `getEatEffects()`, `getContactEffects()`, `getDisplayElapsedTime()`, `isGameOver()`, `isPaused()`, `wasEasterEggTriggered()`
- **`returnToMenu()`** — stops `running`, closes ambient audio, disposes dev log, switches to MainPanel; ensures clean resource cleanup
- **Cell density:** `static double cellDensity` — default from `GameConstants.DEFAULT_CELL_DENSITY`
- **Food cell categories:** three tiers (Small 90%, Medium 7%, Large 3%) via `randomFoodRadius()` using `GameConstants` values
- **NPC system:** `npcList` is a `CopyOnWriteArrayList<NPC>`
- **Camera:** smooth lerp with dynamic zoom based on player radius
- **Player color/name:** `static Color playerColor`, `static String playerName`
- **Easter egg:** score freeze, elapsed time preservation, coolMusic playback
- **Developer log:** `toggleDevLog()` via Ctrl+I / Meta+I

### `CollisionHandler.java` *(refactored — shave-based erosion)*
- Handles all collision detection and resolution for the game
- **Constructor:** `CollisionHandler(GamePanel game)` — holds reference to game state
- **`update()`** — called once per game tick; runs all collision checks in order:
  1. `checkPlayerEatsFood()` — player eats food cells (area ≥ 2x)
  2. `checkPlayerEatsNPCs()` — player eats smaller NPCs
  3. `checkNPCsEatFood()` — NPCs eat food cells
  4. `checkNPCsEatNPCs()` — NPC↔NPC eating
  5. `checkNPCsEatPlayer()` — NPCs eat the player
  6. `updateShaving()` — continuous erosion for overlapping cells in the canDivide zone
  7. `updateBounceEffects()` — visual/audio feedback for uneatable contacts
- **Shave (erosion) mechanic:** replaces the old 200-tick sustained-contact division; when a slightly-larger cell overlaps a smaller one (`canDivide` zone: 1.01x–2x area), the smaller cell continuously loses area proportional to `overlapDepth × attackerSpeed × SHAVE_RATE`; lost area accumulates in `shaveAccumulators` HashMap and spawns as food cells at the contact point once `SHAVE_MIN_FOOD_AREA` is reached; Split Shield reduces damage via `splitShieldFactor` multiplier
- **`shaveTarget()`** — unified shaving method for all entity types (player, NPC, food); calculates overlap depth, speed factor, shield reduction, erodes target radius, accumulates debris, spawns food and visual effects
- **Bounce cooldown:** `BOUNCE_COOLDOWN_MS = 200` prevents sound/effect spam when cells touch repeatedly
- **Effect spawning:** creates `EatEffect`, `ContactEffect` instances; respects `GameConstants.MAX_*_EFFECTS` limits

### `GameRenderer.java` *(new — extracted from GamePanel)*
- Handles all drawing/rendering for the game
- **Constructor:** `GameRenderer(GamePanel game)` — holds reference to game state
- **`render(Graphics2D g2d)`** — full rendering pipeline called from `GamePanel.paintComponent()`:
  1. Applies camera zoom and translation (world space)
  2. Draws world background, food cells, NPCs, player, effects
  3. Restores screen space via `AffineTransform`
  4. Draws HUD, scoreboard, overlays (pause, easter egg, game over)
- **World-space drawing:** `drawFoodCells()`, `drawNPCs()`, `drawPlayer()`, `drawEffects()`
- **Screen-space drawing:** `drawHUD()`, `drawScoreboard()`, `drawEasterEggOverlay()`, `drawPausedOverlay()`, `drawGameOverOverlay()`, `drawStageCompleteOverlay()`
- **`drawStageCompleteOverlay()`** — dark purple overlay shown during `stageTransitioning`; displays "STAGE N CLEARED!", gold `★ ★ ★` row, next stage preview (NPC count, difficulty, score, carry-over reminder)
- **`drawHUD()`** — draws centered stage badge `"STAGE  N"` in purple when `game.isEvolvingMode()`
- **`drawGameOverOverlay()`** — purple tint in evolving mode; shows "You reached Stage N!" sub-title and "Final Score" label instead of "Final Standings"
- **Cell name rendering:** `drawCellName()` — font-size fitted, centered inside cells

### `Cell.java` *(refactored)*
- Stores: `x` (`double`), `y` (`double`), `cellRad` (`double`), `cellColor`, `speedX` (`double`), `speedY` (`double`)
- `spawnAlpha` — `float` field (0 = newly created/invisible, 1 = fully visible); incremented by `GameConstants.SPAWN_ALPHA_INCREMENT` per tick
- **Constructor:** `Cell(int centerX, int centerY, double radius)` — sets `x = centerX - radius`, `y = centerY - radius`
- **Convenience methods:** `getCenterX()`, `getCenterY()` — return center coordinates
- `drawCell(Graphics2D, int radius)` — enables anti-aliasing, renders filled oval at world coordinates
- `updateCellPos(boolean right, left, up, down)` — moves then wraps toroidally using `MainClass.WORLD_WIDTH/HEIGHT`
- **Static collision:** `checkEatCollision(Cell eater, Cell prey)` — circle-circle collision with `COLLISION_SIZE_ADVANTAGE` (0.5) minimum size requirement; replaces old instance `isCollision()` method
- Dead fields removed: `screenWidth`, `screenHeight`, `radiusDifference`

### `NPC.java`
- Represents an AI-controlled cell with a name, score, difficulty level, and intelligent navigation
- **Difficulty enum:** `NPC.Difficulty` with three levels (EASY, MEDIUM, HARD), each defining:
  - `errorChance` — chance per tick to ignore navigation (EASY 15%, MEDIUM 6%, HARD 2%)
  - `steerJitter` — angular jitter in radians (EASY 0.70, MEDIUM 0.35, HARD 0.15)
  - `moodChangeChance` — chance per tick to become distracted (EASY 1%, MEDIUM 0.4%, HARD 0.1%)
  - `foodWeight` — food chase weight multiplier (EASY 0.8, MEDIUM 1.5, HARD 2.5)
  - `visionRange` — how far the NPC can see (EASY 300, MEDIUM 450, HARD 600)
- **Fields:** `cell` (Cell entity), `name` (String), `score` (int), `alive` (boolean), `difficulty` (Difficulty), `upgradeManager` (UpgradeManager — per-NPC), `speedBonus` (double), `regenLevel` (int), `splitShieldFactor` (double), `upgradeCount` (int — display badge)
- **Navigation AI:** `update(playerCell, npcList, foodList)` calls `navigate()` which scans nearby entities within `difficulty.visionRange`:
  - **Flee** from cells bigger than this NPC (player, other NPCs) — stronger force when closer (weight 3.0); fleeing always works even when distracted
  - **Chase** cells smaller than this NPC — player (weight 2.5), other NPCs (weight 2.0), food cells (weight `difficulty.foodWeight`); chasing is disabled when NPC is in "distracted" mood
  - Steering vector has angular jitter (`difficulty.steerJitter` radians) added each tick for less robotic movement
  - **Error chance:** `difficulty.errorChance` per tick the NPC ignores navigation and moves randomly
  - **Mood swings:** `difficulty.moodChangeChance` per tick the NPC becomes "distracted" for 60–180 ticks, during which it will not chase prey but will still flee threats
  - **Roaming:** when nothing is in vision range, the NPC picks a random world target and steers towards it (with jitter), picking new targets upon arrival or timeout
- **Speed:** fixed at `GameConstants.DEFAULT_SPEED` (3) via `updateSpeed()` — no size scaling
- **Growth:** `grow(eatenRad)` uses `GameConstants.growRadius()` for area-based growth, `GameConstants.scoreFromRadius()` for scoring
- **Name pool:** `GameConstants.NPC_NAMES[]` (20 names); duplicates avoided via `usedNames` set
- **Colors:** uses `GameConstants.CELL_COLORS[]` for random cell coloring
- **Constructor:** `NPC(int cx, int cy, double radius, Set<String> usedNames, Difficulty difficulty)` — picks unique name, random color, stores difficulty

### `StyledDialog.java`
- Static utility class replacing all `JOptionPane` calls with modern dark-themed dialogs
- **`showInputDialog(parent, message, defaultValue)`** — modal input dialog with styled text field; returns entered text or null
- **`showConfirmDialog(parent, message, title)`** — modal Yes/No dialog; returns boolean
- **`showMessageDialog(parent, message, title, isWarning)`** — modal info/warning dialog with styled OK button
- All dialogs use undecorated windows with rounded panels, dark backgrounds, and StyledButton instances
- Keyboard shortcuts: Enter to confirm, Escape to cancel

### `WorldSettingsPanel.java`
- Pre-game configuration screen opened from MainPanel's START button
- **Editable fields:** Player Name, NPC Count (minimum 3), World Width, World Height, Cell Density
- **START GAME button:** validates inputs, applies settings via `GameSettings.applyToGame()`, creates `GamePanel`, switches to game
- **BACK button:** returns to MainPanel (no game started — fixes the old cancel-button-doesn't-cancel bug)
- **SAVE button:** saves current field values to a named `.cfg` file in `saves/` directory; enforces max 3 files; offers overwrite when limit reached
- **LOAD button:** shows a picker dialog of existing saves; loads selected file and populates fields
- **RENAME button:** renames an existing save file
- **DEFAULT button:** restores all fields to default values (with confirmation)
- **Animated background:** same dark gradient style as MainPanel/OptionsPanel
- Uses `GameSettings` for persistence and validation

### `GameSettings.java` *(updated)*
- Manages game configuration with save/load support via simple key=value text files
- **Save directory:** `GameConstants.SAVES_DIR` (`saves/`); extension `GameConstants.SAVE_EXT` (`.cfg`)
- **Maximum save files:** `GameConstants.MAX_SAVE_FILES` (3)
- **Settings stored:** playerName, npcCount, worldWidth, worldHeight, cellDensity, soundEnabled, fullscreen, playerColorIndex
- **Default values:** sourced from `GameConstants` (e.g., `MIN_NPC_COUNT`, `DEFAULT_WORLD_WIDTH`, `DEFAULT_CELL_DENSITY`)
- `save(fileName)` / `load(fileName)` — write/read settings to/from `saves/<name>.cfg`
- `listSaves()` — returns sorted list of save file names (without extension)
- `deleteSave(fileName)` / `renameSave(oldName, newName)` — file management
- `applyToGame()` — writes settings to static game fields; uses `GameConstants.CELL_COLORS` for color lookup
- `readFromGame()` — reads current values from static game fields
- `restoreDefaults()` — resets all settings to compile-time defaults

### `EvolvingModePanel.java` *(new)*
- Mode entry screen opened from MainPanel's EVOLVING MODE button
- **Fields:** player name text field, progress info label, animated `MenuBackground`
- **`refreshProgressInfo(String name)`** — checks for an existing `.ecfg` save and shows max stage and highest score, or "No existing save" if none
- **`showLoadSaveDialog()`** — styled `JDialog` listing all saved player names with their progress summary; clicking a name populates the name field and refreshes the progress label
- **START button:** validates name, reads current `GameSettings`, loads or creates `EvolvingProgressSave`, then creates `new GamePanel(mainClass, progress)` (the evolving constructor) and switches to the game
- **BACK button:** returns to `MainPanel` without starting a game
- **`paint()`** overlays title `"Infinite Evolving Cells"` and subtitle `"∞ INFINITE STAGES ∞"` in purple/gold using `MenuBackground.drawTitle()`
- Uses `MenuBackground` for animated gradient background (same style as `MainPanel`)

### `EvolvingProgressSave.java` *(new)*
- Manages per-player evolving mode progress persisted to `.ecfg` files in `saves/evolving/`
- **Fields:** `playerName` (String), `maxStageReached` (int), `highestScore` (int)
- **Save directory / extension:** `GameConstants.EVOLVING_SAVES_DIR` (`saves/evolving`) / `GameConstants.EVOLVING_SAVE_EXT` (`.ecfg`)
- **Filename sanitization:** `playerName.replaceAll("[^a-zA-Z0-9_\\-]", "_")` ensures valid file names
- `save()` — writes key=value text file; creates directory if absent
- `static load(String name)` — reads and returns an `EvolvingProgressSave`, or null if file absent
- `updateAndSave(int stage, int score)` — updates `maxStageReached` and `highestScore` if improved, then calls `save()`
- `static saveExists(String name)` — checks if a save file exists for the given player name
- `static loadForPlayer(String name)` — returns existing save or creates a fresh one (stage 1, score 0)
- `static listSavedPlayerNames()` — returns list of saved player names by reading `playerName=` line from each `.ecfg` file
- `getSummary()` — returns a one-line display string, e.g. `"Best: Stage 5 | Score: 470"`

### `HUD.java` *(refactored)*
- Pure data class (no longer extends `JPanel`)
- `score` — integer reflecting 10x the player cell's current radius (`ceil(cellRad * 10)`)
- `elapsedTime` — milliseconds since `startTime`; reset by `resetTime()` on easter egg trigger
- `updateElapsedTime()` — renamed from `getElapsedTime()` for clarity (has side effect of updating `elapsedTime`)

### `Background.java`
- Procedural animated background — no image file required
- Initialises 12 "blob" entities: random world positions, slow random velocities, large radii (150–500), individual hue and alpha
- `drawBackground(Graphics2D g, int cameraX, int cameraY)` — call while transform is `translate(-cameraX, -cameraY)`:
  1. Advances `phase` (0.002/call) for overall color cycling
  2. Fills viewport with a `GradientPaint` derived from `Color.getHSBColor(phase, ...)`
  3. Moves each blob with toroidal wrap, shifts its hue, draws it as a `RadialGradientPaint` oval if visible

### `MenuBackground.java` *(new — extracted)*
- Reusable animated gradient background with floating translucent circles for menu panels
- **Constructor:** `MenuBackground()` — initializes circle positions, velocities, sizes, colors
- **`start()`/`stop()`** — manages Swing `Timer` at 30ms for animation
- **`draw(Graphics2D, int width, int height)`** — renders animated gradient + drifting circles
- **Static helpers:** `drawHighScore(Graphics2D, width)`, `drawTitle(Graphics2D, title, width)` — shared HUD elements for menu panels
- Used by `MainPanel`, `OptionsPanel`, `WorldSettingsPanel` — eliminates ~90 lines of duplicated background rendering

### `ToneGenerator.java` *(new — extracted)*
- Low-level programmatic audio tone synthesis shared by all sound effects
- **`Waveform` functional interface:** `double sample(int index)` — defines a waveform as a lambda
- **`playTone(durationMs, waveform)`** — renders waveform to byte buffer and plays via `SourceDataLine` on a daemon thread
- **`playAmbient(waveform)`** — plays a continuous looping waveform; returns `SourceDataLine` for caller to stop
- **`stopLine(SourceDataLine)`** — safely stops and closes an audio line
- **`sine(freq, index)`** — utility for generating sine wave samples at a given frequency
- **`lerp(a, b, t)`** — linear interpolation utility
- Eliminates ~200 lines of duplicated audio byte-buffer boilerplate from Sound.java

### `MainPanel.java` *(updated)*
- Main menu with START, OPTIONS, EXIT `StyledButton` instances (modern animated buttons)
- Button clicks play programmatic `Sound.playClickSound()` (no .wav needed)
- **Uses `MenuBackground`** for animated gradient rendering (replaces inline implementation)
- **Ambient menu sound:** `Sound.playMenuAmbient()` plays a soothing pad sound; stopped via `ToneGenerator.stopLine()` when navigating away
- START navigates to `WorldSettingsPanel` where the player configures name, NPC count, world size, and cell density before starting
- `paintComponent` draws via `MenuBackground.draw()`; `paint` overlays title with glow effect and highscore text

### `DevLogDialog.java`
- Non-modal `JDialog` opened/closed by `GamePanel.toggleDevLog()` via Ctrl+I / Meta+I
- Sets `gamePanel.paused = true` on open; `false` on close
- **Editable fields:** Player Name, Player Radius, Score, Speed X, Speed Y, Position X, Position Y
- **Manual Speed Override checkbox:** when checked, sets `gamePanel.devSpeedOverride = true`, preventing automatic speed recalculation each tick
- **Enemy cell count** displayed as a read-only label
- **Cell Density (cells/M px):** editable field for `GamePanel.cellDensity`; changes take effect immediately on apply
- **Max Food Cells:** read-only label computed from density × world area via `gamePanel.getMaxCells()`
- **Refresh button** re-reads current game state into the fields
- **Apply & Resume button** (and X window close) validates input, writes values to `gamePanel.playerCell` / `GamePanel.playerName` / `gamePanel.hud.score` / `GamePanel.cellDensity`, then unpauses the game
- Ctrl+I / Meta+I keyboard shortcut (via `getRootPane().getInputMap`) also closes the dialog
- **Evolving mode section** — shown when `gamePanel.isEvolvingMode()`: displays current stage (`"Stage N (Evolving Mode active)"` or `"N/A"`), and NPC upgrade diversity table (HTML-formatted map of each `UpgradeType` to count of NPCs holding it, plus count of NPCs with zero upgrades)

### `Sound.java` *(refactored — delegates to ToneGenerator)*
- Wraps `javax.sound.sampled.Clip` for `.wav` playback
- `static boolean soundEnabled = true` — respects this flag; set to `false` to mute all sounds
- Constructor: `new Sound(filename, soundSeconds)`
- Play: `sound.playSound()` — no-ops when `soundEnabled == false`
- Stop: `sound.closeSound()`
- All programmatic tone methods now use `ToneGenerator.playTone()` with lambda waveforms (~5 lines each):
  - `playEatSound()` — 150 ms descending sine sweep (600 → 200 Hz)
  - `playMenuAmbient()` — A major chord pad with slow breathing modulation; returns `SourceDataLine`
  - `playGameAmbient()` — evolving pentatonic chord progression; returns `SourceDataLine`
  - `playDivisionSound()` — rising dual-tone (200 ms)
  - `playBounceSound()` — short bouncy wobble tone (100 ms)
  - `playClickSound()` — crisp descending click tone (60 ms)
- Reduced from ~346 lines to ~160 lines via ToneGenerator extraction

### `OptionsPanel.java` *(updated)*
- All buttons use `StyledButton` with `GameConstants.BTN_ON`/`BTN_OFF` colors for toggle states
- **SOUND button:** toggles `Sound.soundEnabled`; dynamically changes button color
- **COLOR button:** cycles `GamePanel.playerColorIndex` through `GameConstants.CELL_COLORS[]`
- **FULLSCREEN button:** toggles `MainClass.fullscreen`; dynamically changes button color
- **BACK button:** returns to main menu
- **Uses `MenuBackground`** for animated gradient (replaces inline implementation)
- **Note:** World Size and Cell Density settings have moved to `WorldSettingsPanel`

### `StyledButton.java`
- Extends `JButton` — custom-painted modern button with rounded corners, gradient fill, drop shadow
- **Hover animation:** smooth color transition to brighter shade via Swing `Timer` (12ms ticks)
- **Press feedback:** darker color on mouse press
- **Glow border:** semi-transparent border appears on hover
- **Text rendering:** centered text with drop shadow for depth
- `setBaseColor(Color)` — changes the button's color scheme dynamically
- Used throughout MainPanel, OptionsPanel, and GamePanel (restart button)

### `EatEffect.java`
- Particle burst animation when a cell is eaten
- Spawns 6–16 colored particles radiating outward from the eat point
- Particles have randomized angles, speeds, and sizes based on the eaten cell's radius
- Fades out over 25 ticks (250ms) with quadratic alpha decay
- `update()` advances animation; `draw(Graphics2D)` renders particles; `finished` flag when done

### `ContactEffect.java`
- Curvy ripple arc animation at cell contact points
- **Division contact mode** (isDivisionContact=true): 4 pulsing arcs, 40 ticks, used during division buildup
- **Bounce mode** (isDivisionContact=false): 3 arcs, 20 ticks, used when touching an uneatable cell
- Arcs are quadratic bezier curves radiating outward with expanding spread
- Division mode has pulsing color brightness; bounce mode uses solid color

---

## Architecture Patterns

### Panel/Screen Switching
```java
mainClass.getContentPane().removeAll();
mainClass.getContentPane().add(newPanel);
mainClass.revalidate();
mainClass.repaint();      // REQUIRED — prevents stale panel artifacts in fullscreen mode
newPanel.requestFocusInWindow();
```

### Game Loop (both threads follow this pattern)
```java
Thread t = new Thread() {
    @Override
    public void run() {
        while (running) {  // volatile boolean — set false by returnToMenu()
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
int camX = (int) cameraX;
int camY = (int) cameraY;
g2d.translate(-camX, -camY);   // enter world space
background.drawBackground(g2d, camX, camY);
// draw cells and player at their world x, y
g2d.translate(camX, camY);     // restore screen space
// draw HUD elements at screen coordinates
```

Camera uses smooth lerp each tick (15% per frame) with instant snap on world-boundary wrap:
```java
double targetX = clamp(playerCell.x + playerCell.cellRad - SCREEN_WIDTH  / 2.0, 0, WORLD_WIDTH  - SCREEN_WIDTH);
double targetY = clamp(playerCell.y + playerCell.cellRad - SCREEN_HEIGHT / 2.0, 0, WORLD_HEIGHT - SCREEN_HEIGHT);
// Snap instantly if player wrapped (delta > half world size)
if (Math.abs(targetX - cameraX) > WORLD_WIDTH  / 2.0) cameraX = targetX;
if (Math.abs(targetY - cameraY) > WORLD_HEIGHT / 2.0) cameraY = targetY;
cameraX += (targetX - cameraX) * 0.15;
cameraY += (targetY - cameraY) * 0.15;
```

### Cell Growth Formula (area-based)
```java
double eatenRad = celllist.get(i).cellRad; // save BEFORE removing from list
celllist.remove(i);
// Area-based: r3 = sqrt(r1^2 + r2^2) — total area is conserved
double newRad = Math.sqrt(playerCell.cellRad * playerCell.cellRad + eatenRad * eatenRad);
playerCell.cellRad = newRad;
// Score = 10x radius: hud.score = (int) Math.ceil(playerCell.cellRad * 10);
```

### Shave (Erosion) Mechanic
```java
// When a larger cell overlaps a smaller one in the canDivide zone (1.01x–2x area):
double overlapDepth = (attacker.cellRad + target.cellRad) - distance;
double speedFactor = Math.max(0.5, attackerSpeed / DEFAULT_SPEED);
double shavedArea = overlapDepth * speedFactor * SHAVE_RATE * splitShieldFactor;
// Target loses that area:
double newArea = targetArea - shavedArea;
target.cellRad = Math.sqrt(newArea);
// Accumulated shaved area spawns as food at the contact point:
if (accumulated >= SHAVE_MIN_FOOD_AREA) { spawnFoodAtContact(); }
```

### Fixed Speed
```java
// Applied every tick in runGameThread (unless devSpeedOverride is true)
// Speed is constant — no scaling with size
playerCell.speedX = DEFAULT_SPEED;  // DEFAULT_SPEED = 3
playerCell.speedY = DEFAULT_SPEED;
// Dev log speed override still allows manual speed changes for debugging
```

### No-overlap Spawn
```java
// GamePanel.generateNonOverlappingCell() — called for every new food cell
for (int attempt = 0; attempt < 50; attempt++) {
    // ... pick random cx, cy, r (from randomFoodRadius()) ...
    // reject if within (playerCell.cellRad + r + 20) of player center
    // reject if within (c.cellRad + r + 10) of any existing food cell
    // reject if within (npc.cell.cellRad + r + 10) of any live NPC
    // accept if no overlap found
}
// Fallback after 50 attempts: place anyway (rare)
```

### Thread Safety
`foodCells` (renamed from `celllist`) is a `CopyOnWriteArrayList<Cell>` — safe for reads from the paint thread and concurrent writes from `startCellSpawnThread` and `startGameThread`. `npcList` is a `CopyOnWriteArrayList<NPC>` — same thread-safety guarantees for NPC entities. `eatEffects`, `contactEffects`, and `divisionEffects` are also `CopyOnWriteArrayList` instances.

### Enemy Cell Color Palette
Use the centralized palette in `GameConstants.CELL_COLORS[]`:
```java
// GameConstants.java
public static final Color[] CELL_COLORS = {BLACK, BLUE, CYAN, DARK_GRAY, GRAY, GREEN, LIGHT_GRAY, MAGENTA, ORANGE, YELLOW, PINK};
```
`GamePanel.colors` is retained for backward compatibility but references `GameConstants.CELL_COLORS`.

---

## Key Conventions

1. **JavaDoc on every class** — include `@author` tag, brief purpose, and notable behavior.
2. **Use `GameConstants` for all magic numbers** — screen, world, button dimensions, speeds, timing, colors, etc. `MainClass` holds mutable state initialized from `GameConstants` defaults. Never hardcode numeric values.
3. **Player initial state:** radius `cellrad = 2` (double), spawns at world center `(WORLD_WIDTH/2, WORLD_HEIGHT/2)`, color = `GamePanel.playerColor`. NPCs also start at radius 2.
4. **Food cell radius:** determined by fixed three-tier categories — Small (90%, radius 1), Medium (7%, radius 2–5), Large (3%, radius 5–10). Use `randomFoodRadius()` for all spawns. These values cannot be changed in-game.
5. **Spawn positions use world coordinates** — random in `[SPAWN_BORDER=40, WORLD_WIDTH - SPAWN_BORDER]` × `[SPAWN_BORDER, WORLD_HEIGHT - SPAWN_BORDER]`.
6. **All new food cell spawns go through `generateNonOverlappingCell()`** — never place cells directly without the overlap check (checks player, food cells, and NPCs).
7. **Area-based growth** — `newRad = sqrt(r1² + r2²)`; all cell radii are `double`; score = `(int) Math.ceil(currentRadius)` (radius-based, not cumulative).
8. **Anti-aliasing must be enabled** at the top of `paintComponent` and inside `Cell.drawCell` via `RenderingHints.VALUE_ANTIALIAS_ON`.
9. **Assets live in `src/`** alongside Java files — do not create a separate `resources/` directory without updating the build setup.
10. **No external libraries** — the project has no dependency manager; do not add third-party JARs without establishing a build system first.
11. **All game threads are daemon threads** — they terminate automatically when the JVM exits.
12. **`paused` and `gameOver` flags must be `volatile`** — they are written by the Swing EDT (key events, dialog close, game end) and read by the game thread.
13. **Score equals 10x radius** — `hud.score = (int) Math.ceil(playerCell.cellRad * 10)`. NPCs also set `score = ceil(cell.cellRad * 10)`. On death/division, score is frozen at the radius at that moment.
14. **NPC count minimum is 3** — enforced in `MainPanel` via `Math.max(3, ...)` on user input.
15. **NPC names must be unique** — use a `Set<String>` of used names when spawning NPCs to avoid duplicates.
16. **NPC `update()` requires navigation context** — always call `npc.update(playerCell, npcList, celllist)` with player, NPC list, and food list so navigation AI can detect threats and prey.
17. **Cell density default is 200 cells/M px** — use `getMaxCells()` (density × world area) instead of hardcoded limits; ~200 cells per 1000×1000 area; configurable via WorldSettingsPanel and Dev Log.
18. **Fullscreen is the default** — `MainClass.fullscreen = true`; screen dimensions are set to display size on startup; toggling recalculates `SCREEN_WIDTH`/`SCREEN_HEIGHT`.
19. **World dimensions are configurable** — minimum 800×600; changes apply to the next game session.
20. **Panel switching must call `repaint()`** — always call `mainClass.repaint()` after `mainClass.revalidate()` when swapping panels, otherwise stale rendering artifacts persist in fullscreen mode.
21. **All panels must set `setPreferredSize()`** — in addition to `setSize()`, panels must set preferred size to `(SCREEN_WIDTH, SCREEN_HEIGHT)` for proper layout in the content pane.
22. **Food cells are immutable** — food cells cannot move, cannot eat other cells, and their category distribution (Small 90% / Medium 7% / Large 3%) cannot be changed in-game. Density is configurable.
24. **All cell radii are `double`** — `cellRad`, `x`, `y`, `speedX`, `speedY` are all `double` in `Cell.java`; cast to `int` only for rendering (`Math.round()` for `drawCell`/`fillOval`).
23. **NPC AI has difficulty-based imperfection** — `errorChance`, `steerJitter`, and `moodChangeChance` are per-difficulty instance fields in `NPC.Difficulty` enum; EASY is most imprecise, HARD is most precise; do not remove these without team agreement.
25. **Use `StyledButton` for all buttons** — never use plain `JButton` in UI panels; `StyledButton` provides consistent modern look with hover/press animations.
26. **Fixed speed for all cells** — `DEFAULT_SPEED = 3` for both player and NPCs; dev log speed override is preserved for debugging but dynamic speed scaling has been removed.
27. **Visual effects use `CopyOnWriteArrayList`** — `eatEffects`, `contactEffects`, and `divisionEffects` are thread-safe lists updated in the game thread and drawn in `paintComponent`.
28. **Ambient sounds return `SourceDataLine`** — callers must call `stop()`/`close()` on the returned line when leaving the panel or ending the game to avoid audio resource leaks.
29. **Menu panels use animated backgrounds** — `MainPanel` and `OptionsPanel` use Swing `Timer` at 30ms for gradient animation; timer must be stopped when navigating away.
30. **Use `StyledDialog` instead of `JOptionPane`** — all user-facing dialogs (input, confirm, message) must use `StyledDialog` static methods for consistent dark-themed UI.
31. **NPC difficulty is assigned round-robin** — when spawning NPCs, cycle EASY → MEDIUM → HARD equally; difficulty affects error rate, vision, jitter, mood, and food aggression.
32. **Shave mechanic replaces old division** — cells in the canDivide zone (1.01x–2x area) continuously erode the smaller cell's area based on overlap depth and attacker speed; lost area accumulates and spawns as food debris; `splitShieldFactor` (default 1.0) multiplies the shave damage (lower = less damage via Split Shield upgrades).
33. **Game ambient sound is an evolving chord progression** — `playGameAmbient()` cycles through pentatonic chords (C, D, E patterns) with crossfading; distinct from the menu's static A major pad.
34. **Pre-game settings go through WorldSettingsPanel** — START button on MainPanel navigates to WorldSettingsPanel; player configures name, NPC count, world size, and cell density before starting; BACK button cancels without starting a game.
35. **Settings persistence via GameSettings** — save/load settings to `.cfg` files in `saves/` directory; max 3 save files; supports rename, overwrite, and restore-to-defaults; `applyToGame()` writes values to game statics, `readFromGame()` reads them.
36. **Maximum 3 save files** — enforced in `WorldSettingsPanel` save button; when limit reached, user must overwrite an existing save.
37. **Easter egg freezes player score** — `easterEggActive` flag prevents player from eating food or NPCs during the easter egg; `finalElapsedTime` preserves the actual game time before the easter egg timer reset.
38. **NPCs receive upgrades automatically** — each NPC has its own `UpgradeManager` instance; when an NPC crosses a score threshold, a random upgrade from the NPC-eligible pool (Speed Boost, Size Surge, Regeneration, Split Shield) is applied instantly with no UI; `upgradeCount` drives the gold star badge drawn by `GameRenderer`.
39. **Upgrade buttons must not steal focus** — `StyledButton` instances in the upgrade selection overlay must have `setFocusable(false)` to prevent them from stealing keyboard focus from `GamePanel`; after dismissal, `requestFocusInWindow()` must be called to restore key input.
40. **Magnet is player-only** — food cells within `magnetRadius` are pulled toward the player each tick; NPCs do not get Magnet (their navigation AI already steers toward food).
41. **Regeneration applies to both player and NPCs** — each tick, `cellRad += REGEN_RATE_PER_LEVEL * regenLevel` for any entity with `regenLevel > 0`; applies in `GamePanel.applyRegen()`.
42. **Split Shield reduces shave damage** — `splitShieldFactor` starts at 1.0 (full damage) and decreases by `SPLIT_SHIELD_PER_LEVEL` per upgrade toward `SPLIT_SHIELD_MIN` (0.25); the shave mechanic multiplies area loss by this factor.
43. **Evolving mode uses a separate constructor** — `GamePanel(MainClass, EvolvingProgressSave)` sets `evolvingMode=true`; the standard constructor sets `evolvingMode=false`. Never call `spawnNPCsForEvolvingStage()` from standard mode.
44. **Evolving NPC seeding formula** — `minUpgrades = min(max(0, stage-1), playerUpgrades)`, `maxUpgrades = playerUpgrades`; each NPC receives a random number of upgrades in `[minUpgrades, maxUpgrades]` via `applyRandomNPCUpgrade()`.
45. **Stage transition uses `stageTransitioning` flag** — set by `triggerStageComplete()`; the game loop skips all logic while this is true (same as `gameOver`); cleared by `nextStage()` once the player clicks "NEXT STAGE ▶".
46. **Evolving progress is auto-saved** — `updateAndSave()` is called on stage clear, game over, and `returnToMenu()`; never let the player lose stage/score progress due to missing a save call.
47. **Evolving save files are per-player-name** — stored in `saves/evolving/<sanitized-name>.ecfg`; filename sanitized by replacing non-alphanumeric characters with `_`; player name is stored inside the file for display.
48. **`UpgradeManager.getTotalAppliedLevels()`** — sums all values in `appliedCounts`; used by `spawnNPCsForEvolvingStage()` to determine how many upgrades the player currently has for seeding NPCs.
49. **`UpgradeManager.applyRandomNPCUpgrade(NPC)`** — picks from `pickNPCChoices()` and applies one random NPC-eligible upgrade; used for both automatic per-tick NPC upgrades and evolving stage seeding.

---

## What to Avoid

- Do not switch to Maven or Gradle without team agreement.
- Do not add external dependencies — no dependency management system is configured.
- Do not call `g2d.translate(...)` for world-space drawing without restoring with the inverse translate before drawing HUD elements. `GameRenderer` uses `AffineTransform` save/restore for this.
- Do not use `paint()` without calling `super.paint(g)` first — use `paintComponent()` for game panel rendering.
- Do not access `foodCells` without going through the `CopyOnWriteArrayList` API — never replace it with a plain `ArrayList`.
- Do not read an eaten cell's data from `foodCells` after calling `foodCells.remove(i)`.
- Do not break the two-thread game loop pattern without profiling impact on frame rate.
- Do not put collision logic in `GamePanel` — it belongs in `CollisionHandler`.
- Do not put rendering logic in `GamePanel` — it belongs in `GameRenderer`.
- Do not duplicate tone generation boilerplate — use `ToneGenerator.playTone()` with a `Waveform` lambda.
- Do not duplicate menu background animation — use `MenuBackground` in all menu panels.
- Do not use inline magic numbers — add constants to `GameConstants` and reference them.
