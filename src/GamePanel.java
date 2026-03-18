
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GamePanel : Core game panel that orchestrates the game loop, input handling,
 * and coordinates between CollisionHandler and GameRenderer.
 * Creates the player cell and NPC cells, manages food cell spawning,
 * camera tracking, and game state (pause, game over, upgrade selection).
 * @author Kamil Yunus Özkaya
 */
@SuppressWarnings({"serial", "this-escape"})
public class GamePanel extends JPanel implements KeyListener {

    // ── Game State ───────────────────────────────────────────────────────

    private final HUD hud = new HUD();
    private final Random random = new Random();
    private final MainClass mainClass;

    /** Player display name, set from the settings before game starts */
    public static String playerName = "Player";
    public static int highscore = 0;
    public static Color playerColor = Color.BLACK;
    public static int playerColorIndex = 0;

    /**
     * Cell density: number of food cells per million world-area pixels.
     * Configurable via WorldSettingsPanel and Dev Log.
     */
    public static double cellDensity = GameConstants.DEFAULT_CELL_DENSITY;

    // ── NPC difficulty distribution (set from GameSettings.applyToGame()) ─
    /** Number of EASY NPCs to spawn; uses round-robin when all three are 0. */
    public static int npcEasyCount   = GameSettings.DEFAULT_NPC_EASY;
    /** Number of MEDIUM NPCs to spawn */
    public static int npcMediumCount = GameSettings.DEFAULT_NPC_MEDIUM;
    /** Number of HARD NPCs to spawn */
    public static int npcHardCount   = GameSettings.DEFAULT_NPC_HARD;

    /** Multiplier for the shave/erosion rate (1.0 = default) */
    public static double shaveRateMultiplier = GameSettings.DEFAULT_SHAVE_RATE_MULTIPLIER;

    // ── Entities ─────────────────────────────────────────────────────────

    private Cell playerCell;
    private final CopyOnWriteArrayList<Cell> foodCells = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<NPC> npcList = new CopyOnWriteArrayList<>();
    private final Background background;

    // ── Input State ──────────────────────────────────────────────────────

    private boolean right, left, up, down;
    /** Set to true for one tick when the player presses SPACE for dodge */
    private volatile boolean dodgeRequested = false;

    // ── Camera ───────────────────────────────────────────────────────────

    private double cameraX = 0;
    private double cameraY = 0;
    private double cameraZoom = GameConstants.INITIAL_ZOOM;

    // ── Visual Effects ───────────────────────────────────────────────────

    private final CopyOnWriteArrayList<DivisionEffect> divisionEffects = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<EatEffect> eatEffects = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ContactEffect> contactEffects = new CopyOnWriteArrayList<>();

    // ── Audio ────────────────────────────────────────────────────────────

    private javax.sound.sampled.SourceDataLine gameAmbientLine;

    // ── Game Flags ───────────────────────────────────────────────────────

    /** When true the game loop is paused (used by the developer log) */
    public volatile boolean paused = false;

    /** When true the game has ended */
    public volatile boolean gameOver = false;

    /** When true the player eliminated all NPCs (displayed on game-over screen) */
    public volatile boolean victory = false;

    // ── Evolving Mode State ───────────────────────────────────────────────

    /** True when the game is running in Infinite Evolving Cells mode */
    public boolean evolvingMode = false;

    /** Current stage number (1-based); 0 for standard game mode */
    public int currentStage = 0;

    /**
     * True while the player is viewing the stage-complete screen between stages.
     * The game loop skips all logic (but still repaints) during this state.
     */
    public volatile boolean stageTransitioning = false;

    /** Evolving mode progress save; null in standard mode */
    private EvolvingProgressSave evolvingProgress = null;

    /**
     * When true the game loop is frozen while the player picks an upgrade.
     * Distinct from {@code paused} so the dev-log overlay is not shown
     * during upgrade selection.
     */
    public volatile boolean upgradeSelecting = false;

    /** Saved elapsed time (ms) when the player dies */
    private long finalElapsedTime = -1;

    /** When true, skip automatic speed recalculation (dev override active) */
    public boolean devSpeedOverride = false;

    /** Reference to the developer log dialog (null when closed) */
    private DevLogDialog devLogDialog = null;

    /** Number of NPC players in this game session */
    private final int npcCount;

    /** Flag to signal game threads to stop */
    private volatile boolean running = true;

    // ── Roguelite Upgrade State ──────────────────────────────────────────

    private final UpgradeManager upgradeManager = new UpgradeManager();

    /** Bonus speed added to the player by Speed Boost upgrades */
    public double playerSpeedBonus = 0.0;

    /**
     * Current small-food probability (modified by Big Feast upgrades).
     * Starts at the global default and drifts downward with each Big Feast level.
     */
    public double foodSmallChance = GameConstants.SMALL_CHANCE;

    /**
     * Current medium-food probability (modified by Big Feast upgrades).
     * Starts at the global default and drifts upward with each Big Feast level.
     */
    public double foodMediumChance = GameConstants.MEDIUM_CHANCE;

    /** Countdown ticks remaining before Dodge can be used again (0 = ready) */
    private int dodgeCooldownTicks = 0;

    /** Number of Magnet upgrade levels (determines pull radius; 0 = base magnet only) */
    public int magnetLevel = 0;

    /**
     * World-space radius in which food cells are attracted to the player.
     * Initialised to BASE_MAGNET_RADIUS so a small effect is always active.
     */
    public double magnetRadius = GameConstants.BASE_MAGNET_RADIUS;

    /** Number of Regeneration upgrade levels */
    public int regenLevel = 0;

    /**
     * Shave damage multiplier for the player (1.0 = full damage, lower = more resilient).
     * Decreases with Split Shield upgrades toward {@link GameConstants#SPLIT_SHIELD_MIN}.
     */
    public double splitShieldFactor = GameConstants.SPLIT_SHIELD_BASE;

    // ── Subsystems ───────────────────────────────────────────────────────

    private final CollisionHandler collisionHandler;
    private final GameRenderer renderer;

    // ── Constructor ──────────────────────────────────────────────────────

    /**
     * Standard game mode constructor.
     * @param mainClass the top-level JFrame
     * @param npcCount  number of NPC opponents to spawn
     */
    public GamePanel(MainClass mainClass, int npcCount) {
        this.mainClass = mainClass;
        this.npcCount = npcCount;
        this.evolvingMode = false;
        this.currentStage = 0;
        this.evolvingProgress = null;
        this.collisionHandler = new CollisionHandler(this);
        this.renderer = new GameRenderer(this);

        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
        setPreferredSize(new Dimension(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        setVisible(true);

        background = new Background();

        // Player spawns at the center of the world
        playerCell = new Cell(MainClass.WORLD_WIDTH / 2, MainClass.WORLD_HEIGHT / 2, GameConstants.INITIAL_RADIUS);
        playerCell.cellColor = playerColor;
        playerCell.spawnAlpha = 1f;
        playerCell.speedX = GameConstants.DEFAULT_SPEED;
        playerCell.speedY = GameConstants.DEFAULT_SPEED;

        // Initialize camera centered on player
        double initVisW = MainClass.SCREEN_WIDTH / cameraZoom;
        double initVisH = MainClass.SCREEN_HEIGHT / cameraZoom;
        cameraX = playerCell.x + playerCell.cellRad - initVisW / 2.0;
        cameraY = playerCell.y + playerCell.cellRad - initVisH / 2.0;
        cameraX = Math.max(0, Math.min(cameraX, MainClass.WORLD_WIDTH - initVisW));
        cameraY = Math.max(0, Math.min(cameraY, MainClass.WORLD_HEIGHT - initVisH));

        // Spawn initial food cell and NPCs
        Cell firstFood = generateNonOverlappingCell();
        firstFood.cellColor = Color.BLUE;
        foodCells.add(firstFood);
        spawnNPCs();

        // Start game threads
        startCellSpawnThread();
        startGameThread();

        // Start ambient game sound
        gameAmbientLine = Sound.playGameAmbient();
    }

    /**
     * Evolving mode constructor. Stage 1 starts automatically.
     * The player's progress (max stage reached, highest score) is auto-saved.
     * @param mainClass      the top-level JFrame
     * @param evolvingProgress the loaded-or-new progress save for this player
     */
    public GamePanel(MainClass mainClass, EvolvingProgressSave evolvingProgress) {
        this.mainClass = mainClass;
        this.npcCount  = GameConstants.EVOLVING_BASE_NPC_COUNT; // stage 1 count
        this.evolvingMode     = true;
        this.currentStage     = 1;
        this.evolvingProgress = evolvingProgress;
        this.collisionHandler = new CollisionHandler(this);
        this.renderer         = new GameRenderer(this);

        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
        setPreferredSize(new Dimension(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        setVisible(true);

        background = new Background();

        playerCell = new Cell(MainClass.WORLD_WIDTH / 2, MainClass.WORLD_HEIGHT / 2, GameConstants.INITIAL_RADIUS);
        playerCell.cellColor = playerColor;
        playerCell.spawnAlpha = 1f;
        playerCell.speedX = GameConstants.DEFAULT_SPEED;
        playerCell.speedY = GameConstants.DEFAULT_SPEED;

        double initVisW = MainClass.SCREEN_WIDTH / cameraZoom;
        double initVisH = MainClass.SCREEN_HEIGHT / cameraZoom;
        cameraX = playerCell.x + playerCell.cellRad - initVisW / 2.0;
        cameraY = playerCell.y + playerCell.cellRad - initVisH / 2.0;
        cameraX = Math.max(0, Math.min(cameraX, MainClass.WORLD_WIDTH - initVisW));
        cameraY = Math.max(0, Math.min(cameraY, MainClass.WORLD_HEIGHT - initVisH));

        Cell firstFood = generateNonOverlappingCell();
        firstFood.cellColor = Color.BLUE;
        foodCells.add(firstFood);

        // Spawn stage 1 NPCs
        spawnNPCsForEvolvingStage(1);

        // Record stage 1 as started
        if (evolvingProgress != null) {
            evolvingProgress.updateAndSave(1, hud.score);
        }

        startCellSpawnThread();
        startGameThread();

        gameAmbientLine = Sound.playGameAmbient();
    }

    // ── Public Accessors (for CollisionHandler and GameRenderer) ─────────

    public Cell getPlayerCell() { return playerCell; }
    public CopyOnWriteArrayList<Cell> getFoodCells() { return foodCells; }
    public CopyOnWriteArrayList<NPC> getNPCList() { return npcList; }
    public CopyOnWriteArrayList<DivisionEffect> getDivisionEffects() { return divisionEffects; }
    public CopyOnWriteArrayList<EatEffect> getEatEffects() { return eatEffects; }
    public CopyOnWriteArrayList<ContactEffect> getContactEffects() { return contactEffects; }
    public Background getWorldBackground() { return background; }
    public HUD getHUD() { return hud; }
    public double getCameraX() { return cameraX; }
    public double getCameraY() { return cameraY; }
    public double getCameraZoom() { return cameraZoom; }
    public boolean isGameOver() { return gameOver; }
    public boolean isVictory() { return victory; }
    public boolean isPaused() { return paused; }
    public UpgradeManager getUpgradeManager() { return upgradeManager; }
    public int getDodgeCooldownTicks() { return dodgeCooldownTicks; }

    public long getDisplayElapsedTime() {
        return finalElapsedTime >= 0 ? finalElapsedTime : hud.elapsedTime;
    }

    public boolean isEvolvingMode()    { return evolvingMode; }
    public int getCurrentStage()       { return currentStage; }
    public boolean isStageTransitioning() { return stageTransitioning; }

    /** Returns the maximum number of food cells based on current world size and density */
    public int getMaxCells() {
        double worldArea = (double) MainClass.WORLD_WIDTH * MainClass.WORLD_HEIGHT / 1_000_000.0;
        return Math.max(5, (int) Math.round(cellDensity * worldArea));
    }

    /** Updates the player score to match current radius */
    public void updatePlayerScore() {
        hud.score = GameConstants.scoreFromRadius(playerCell.cellRad);
        if (hud.score > highscore) highscore = hud.score;
    }

    // ── NPC Spawning ─────────────────────────────────────────────────────

    private void spawnNPCs() {
        Set<String> usedNames = new HashSet<>();
        usedNames.add(playerName);

        // Determine per-difficulty counts: use static distribution if total > 0,
        // otherwise fall back to round-robin across npcCount.
        int totalDistrib = npcEasyCount + npcMediumCount + npcHardCount;
        int spawnEasy, spawnMedium, spawnHard;
        if (totalDistrib > 0) {
            spawnEasy   = npcEasyCount;
            spawnMedium = npcMediumCount;
            spawnHard   = npcHardCount;
        } else {
            // Round-robin: evenly distribute across difficulties
            NPC.Difficulty[] difficulties = NPC.Difficulty.values();
            spawnEasy = spawnMedium = spawnHard = 0;
            for (int i = 0; i < npcCount; i++) {
                switch (difficulties[i % difficulties.length]) {
                    case EASY:   spawnEasy++;   break;
                    case MEDIUM: spawnMedium++; break;
                    case HARD:   spawnHard++;   break;
                }
            }
        }

        spawnNPCsOfDifficulty(spawnEasy,   NPC.Difficulty.EASY,   usedNames);
        spawnNPCsOfDifficulty(spawnMedium, NPC.Difficulty.MEDIUM, usedNames);
        spawnNPCsOfDifficulty(spawnHard,   NPC.Difficulty.HARD,   usedNames);
    }

    private void spawnNPCsOfDifficulty(int count, NPC.Difficulty diff, Set<String> usedNames) {
        for (int i = 0; i < count; i++) {
            int cx = GameConstants.SPAWN_BORDER + random.nextInt(
                Math.max(1, MainClass.WORLD_WIDTH - 2 * GameConstants.SPAWN_BORDER));
            int cy = GameConstants.SPAWN_BORDER + random.nextInt(
                Math.max(1, MainClass.WORLD_HEIGHT - 2 * GameConstants.SPAWN_BORDER));
            npcList.add(new NPC(cx, cy, GameConstants.INITIAL_RADIUS, usedNames, diff));
        }
    }

    // ── Evolving Mode NPC Spawning ────────────────────────────────────────

    /**
     * Spawns NPCs for the given evolving mode stage.
     * NPC count and difficulty distribution scale with stage number.
     * Each NPC receives a random number of initial upgrades based on the player's
     * current upgrade total and the stage number.
     *
     * <p>Difficulty distribution (linear interpolation across stages):
     * <ul>
     *   <li>Stage 1:  60% EASY, 30% MEDIUM, 10% HARD</li>
     *   <li>Stage 11+: 10% EASY, 20% MEDIUM, 70% HARD</li>
     * </ul>
     *
     * <p>NPC initial upgrades: each NPC receives between
     * {@code max(0, stage-1)} and {@code playerUpgrades} upgrades (clamped).
     *
     * @param stage the stage number (1-based)
     */
    private void spawnNPCsForEvolvingStage(int stage) {
        npcList.clear();

        Set<String> usedNames = new HashSet<>();
        usedNames.add(playerName);

        // NPC count grows by EVOLVING_NPC_INCREMENT per stage, capped
        int count = Math.min(
            GameConstants.EVOLVING_BASE_NPC_COUNT + (stage - 1) * GameConstants.EVOLVING_NPC_INCREMENT,
            GameConstants.EVOLVING_MAX_NPC_COUNT
        );

        // Difficulty distribution shifts harder with each stage (caps at stage 11)
        double easyFrac   = Math.max(0.10, 0.60 - (stage - 1) * 0.05);
        double hardFrac   = Math.min(0.70, 0.10 + (stage - 1) * 0.05);
        double mediumFrac = Math.max(0.10, 1.0 - easyFrac - hardFrac);

        int easyCount   = (int) Math.round(count * easyFrac);
        int hardCount   = (int) Math.round(count * hardFrac);
        int mediumCount = count - easyCount - hardCount;

        // Determine upgrade seeding range from player's total upgrade levels
        int playerUpgrades = upgradeManager.getTotalAppliedLevels();
        int minUpgrades = Math.min(Math.max(0, stage - 1), playerUpgrades);
        int maxUpgrades = playerUpgrades;

        spawnEvolvingNPCsOfDifficulty(easyCount,   NPC.Difficulty.EASY,   usedNames, minUpgrades, maxUpgrades);
        spawnEvolvingNPCsOfDifficulty(mediumCount,  NPC.Difficulty.MEDIUM, usedNames, minUpgrades, maxUpgrades);
        spawnEvolvingNPCsOfDifficulty(hardCount,    NPC.Difficulty.HARD,   usedNames, minUpgrades, maxUpgrades);
    }

    /**
     * Spawns {@code count} NPCs of the given difficulty and seeds them with a
     * random number of initial upgrades in the range [minUpgrades, maxUpgrades].
     */
    private void spawnEvolvingNPCsOfDifficulty(int count, NPC.Difficulty diff,
                                                Set<String> usedNames,
                                                int minUpgrades, int maxUpgrades) {
        for (int i = 0; i < count; i++) {
            int cx = GameConstants.SPAWN_BORDER + random.nextInt(
                Math.max(1, MainClass.WORLD_WIDTH - 2 * GameConstants.SPAWN_BORDER));
            int cy = GameConstants.SPAWN_BORDER + random.nextInt(
                Math.max(1, MainClass.WORLD_HEIGHT - 2 * GameConstants.SPAWN_BORDER));
            NPC npc = new NPC(cx, cy, GameConstants.INITIAL_RADIUS, usedNames, diff);

            // Seed initial upgrades
            if (maxUpgrades > 0) {
                int range = maxUpgrades - minUpgrades;
                int upgradeCount = minUpgrades + (range > 0 ? random.nextInt(range + 1) : 0);
                for (int u = 0; u < upgradeCount; u++) {
                    npc.upgradeManager.applyRandomNPCUpgrade(npc);
                }
            }
            npcList.add(npc);
        }
    }

    // ── Food Cell Generation ─────────────────────────────────────────────

    /**
     * Returns a random food cell radius based on the current probability distribution.
     * The distribution shifts toward larger cells as the player takes Big Feast upgrades.
     */
    private double randomFoodRadius() {
        double roll = random.nextDouble();
        if (roll < foodSmallChance) {
            return GameConstants.SMALL_RAD;
        } else if (roll < foodSmallChance + foodMediumChance) {
            return GameConstants.MEDIUM_RAD_MIN
                + random.nextDouble() * (GameConstants.MEDIUM_RAD_MAX - GameConstants.MEDIUM_RAD_MIN);
        } else {
            return GameConstants.LARGE_RAD_MIN
                + random.nextDouble() * (GameConstants.LARGE_RAD_MAX - GameConstants.LARGE_RAD_MIN);
        }
    }

    /**
     * Generates a new food cell at a position that does not overlap
     * the player, existing food cells, or NPCs.
     */
    private Cell generateNonOverlappingCell() {
        final int MAX_ATTEMPTS = 50;
        int worldW = Math.max(1, MainClass.WORLD_WIDTH - 2 * GameConstants.SPAWN_BORDER);
        int worldH = Math.max(1, MainClass.WORLD_HEIGHT - 2 * GameConstants.SPAWN_BORDER);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int cx = GameConstants.SPAWN_BORDER + random.nextInt(worldW);
            int cy = GameConstants.SPAWN_BORDER + random.nextInt(worldH);
            double r = randomFoodRadius();

            // Check clearance against the player cell
            if (GameConstants.distSq(cx, cy, playerCell.getCenterX(), playerCell.getCenterY())
                    < (playerCell.cellRad + r + 20) * (playerCell.cellRad + r + 20)) continue;

            // Check clearance against existing food cells
            boolean overlaps = false;
            for (Cell c : foodCells) {
                if (GameConstants.distSq(cx, cy, c.getCenterX(), c.getCenterY())
                        < (c.cellRad + r + 10) * (c.cellRad + r + 10)) {
                    overlaps = true;
                    break;
                }
            }
            if (overlaps) continue;

            // Check clearance against NPCs
            for (NPC npc : npcList) {
                if (!npc.alive) continue;
                if (GameConstants.distSq(cx, cy, npc.cell.getCenterX(), npc.cell.getCenterY())
                        < (npc.cell.cellRad + r + 10) * (npc.cell.cellRad + r + 10)) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) return new Cell(cx, cy, r);
        }
        // Fallback: place without overlap check (rare)
        return new Cell(
            GameConstants.SPAWN_BORDER + random.nextInt(worldW),
            GameConstants.SPAWN_BORDER + random.nextInt(worldH),
            randomFoodRadius());
    }

    // ── Game Threads ─────────────────────────────────────────────────────

    /** Spawns initial food cells then continuously tops up */
    private void startCellSpawnThread() {
        Thread cellThread = new Thread(() -> {
            // Initial batch spawn
            int maxCells = getMaxCells();
            while (foodCells.size() < maxCells && !gameOver && running) {
                Cell c = generateNonOverlappingCell();
                c.cellColor = GameConstants.CELL_COLORS[random.nextInt(GameConstants.CELL_COLORS.length)];
                c.spawnAlpha = 1f;
                foodCells.add(c);
            }
            // Continuous top-up
            while (running) {
                if (!gameOver) {
                    int deficit = getMaxCells() - foodCells.size();
                    int batch = Math.min(deficit, GameConstants.CELL_SPAWN_BATCH);
                    for (int i = 0; i < batch && running; i++) {
                        Cell c = generateNonOverlappingCell();
                        c.cellColor = GameConstants.CELL_COLORS[random.nextInt(GameConstants.CELL_COLORS.length)];
                        foodCells.add(c);
                    }
                }
                repaint();
                try {
                    Thread.sleep(GameConstants.CELL_SPAWN_TICK_MS);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        cellThread.setDaemon(true);
        cellThread.start();
    }

    /** Main game loop thread */
    private void startGameThread() {
        Thread thread = new Thread(() -> {
            while (running) {
                if (!paused && !gameOver && !upgradeSelecting && !stageTransitioning) {
                    // Fixed speed + any speed upgrades
                    if (!devSpeedOverride) {
                        double speed = GameConstants.DEFAULT_SPEED + playerSpeedBonus;
                        playerCell.speedX = speed;
                        playerCell.speedY = speed;
                    }

                    // Dodge dash
                    if (dodgeRequested) {
                        dodgeRequested = false;
                        if (upgradeManager.hasDodge() && dodgeCooldownTicks <= 0) {
                            performDodge();
                        }
                    }

                    // Countdown dodge cooldown
                    if (dodgeCooldownTicks > 0) dodgeCooldownTicks--;

                    playerCell.updateCellPos(right, left, up, down);

                    // Advance spawn animation
                    for (Cell c : foodCells) {
                        if (c.spawnAlpha < 1f) {
                            c.spawnAlpha = Math.min(1f, c.spawnAlpha + GameConstants.SPAWN_ALPHA_STEP);
                        }
                    }

                    // Update NPCs
                    for (NPC npc : npcList) {
                        if (npc.alive) npc.update(playerCell, npcList, foodCells);
                    }

                    // Dynamic camera zoom
                    double targetZoom = Math.max(GameConstants.MIN_ZOOM,
                        GameConstants.INITIAL_ZOOM * Math.sqrt(GameConstants.INITIAL_RADIUS / playerCell.cellRad));
                    cameraZoom += (targetZoom - cameraZoom) * GameConstants.ZOOM_LERP;

                    // Camera tracking
                    updateCamera();

                    hud.updateElapsedTime();

                    // Collision handling (eating, division, bounce)
                    collisionHandler.update();

                    // Update visual effects
                    updateEffects();

                    // Roguelite passive effects
                    applyMagnet();
                    applyRegen();
                    checkNPCUpgrades();

                    // Check score-based upgrade threshold
                    upgradeManager.checkScore(hud.score);
                    if (upgradeManager.isUpgradeReady() && !upgradeSelecting) {
                        upgradeSelecting = true;
                        SwingUtilities.invokeLater(this::showUpgradeSelection);
                    }

                    // Check victory: all NPCs dead
                    checkAllNPCsDead();
                }

                repaint();
                try {
                    Thread.sleep(GameConstants.GAME_TICK_MS);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ── Victory Check ────────────────────────────────────────────────────

    /**
     * Triggers a victory if all NPCs are dead and the game is still running.
     * Called every tick after collision handling.
     */
    private void checkAllNPCsDead() {
        if (gameOver || stageTransitioning || npcList.isEmpty()) return;
        for (NPC npc : npcList) {
            if (npc.alive) return; // at least one alive
        }
        // All NPCs dead
        if (evolvingMode) {
            triggerStageComplete();
        } else {
            triggerVictory();
        }
    }

    private void triggerVictory() {
        if (gameOver) return;
        // Cancel any pending upgrade selection so it doesn't appear over the victory screen
        if (upgradeSelecting || upgradeManager.isUpgradeReady()) {
            upgradeSelecting = false;
            upgradeManager.cancelPendingUpgrade();
        }
        hud.updateElapsedTime();
        finalElapsedTime = hud.elapsedTime;
        gameOver = true;
        victory  = true;
        ToneGenerator.stopLine(gameAmbientLine);
        gameAmbientLine = null;
        SwingUtilities.invokeLater(this::showGameOverScreen);
    }

    // ── Evolving Mode Stage Transition ───────────────────────────────────

    /**
     * Called when all NPCs are eliminated in evolving mode.
     * Saves progress, pauses the game loop, and shows the stage-complete screen.
     */
    private void triggerStageComplete() {
        if (stageTransitioning || gameOver) return;
        // Cancel pending upgrade selection (avoid overlap with stage screen)
        if (upgradeSelecting || upgradeManager.isUpgradeReady()) {
            upgradeSelecting = false;
            upgradeManager.cancelPendingUpgrade();
        }
        hud.updateElapsedTime();
        stageTransitioning = true;

        // Save progress for the stage that was just cleared
        if (evolvingProgress != null) {
            evolvingProgress.updateAndSave(currentStage, hud.score);
        }

        SwingUtilities.invokeLater(this::showStageCompleteScreen);
    }

    /**
     * Advances to the next stage: increments stage counter, spawns new NPCs,
     * clears and refills food, then resumes the game loop.
     * Must be called on the EDT (via button action listener).
     */
    private void nextStage() {
        // Remove the NEXT STAGE button
        Component[] components = getComponents();
        for (Component c : components) {
            if (c instanceof JButton && "next_stage_btn".equals(c.getName())) {
                remove(c);
            }
        }

        currentStage++;

        // Record the new stage as started
        if (evolvingProgress != null) {
            evolvingProgress.updateAndSave(currentStage, hud.score);
        }

        // Clear existing NPCs and spawn for new stage
        npcList.clear();
        spawnNPCsForEvolvingStage(currentStage);

        // Clear food — spawn thread will refill
        foodCells.clear();

        stageTransitioning = false;
        requestFocusInWindow();
        revalidate();
        repaint();
    }

    /** Adds the "NEXT STAGE" button overlay on the EDT after a stage is cleared. */
    private void showStageCompleteScreen() {
        setLayout(null);

        int bw = GameConstants.BUTTON_WIDTH + 60;
        int bh = GameConstants.BUTTON_HEIGHT + 14;
        StyledButton nextBtn = new StyledButton("NEXT STAGE \u25B6", GameConstants.BTN_PURPLE);
        nextBtn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 20));
        nextBtn.setBounds((MainClass.SCREEN_WIDTH - bw) / 2,
            MainClass.SCREEN_HEIGHT - 100, bw, bh);
        nextBtn.setName("next_stage_btn");
        nextBtn.setFocusable(false);
        nextBtn.addActionListener(e -> nextStage());
        add(nextBtn);
        revalidate();
        repaint();
    }

    // ── Roguelite Passive Effects ────────────────────────────────────────

    /**
     * Attracts food cells within {@code magnetRadius} toward the player each tick.
     * A small base effect is always active even before the Magnet upgrade is taken.
     */
    private void applyMagnet() {
        if (magnetRadius <= 0) return;
        double pCX = playerCell.getCenterX();
        double pCY = playerCell.getCenterY();
        double radSq = magnetRadius * magnetRadius;
        for (Cell food : foodCells) {
            double dx = pCX - food.getCenterX();
            double dy = pCY - food.getCenterY();
            double distSq = dx * dx + dy * dy;
            if (distSq < radSq && distSq > 0.01) {
                double dist = Math.sqrt(distSq);
                food.x += (dx / dist) * GameConstants.MAGNET_PULL_SPEED;
                food.y += (dy / dist) * GameConstants.MAGNET_PULL_SPEED;
            }
        }
    }

    /**
     * Applies regeneration to the player and any NPCs that have the Regeneration upgrade.
     * Each level adds a small fixed radius per tick.
     */
    private void applyRegen() {
        if (regenLevel > 0) {
            playerCell.cellRad += GameConstants.REGEN_RATE_PER_LEVEL * regenLevel;
            updatePlayerScore();
        }
        for (NPC npc : npcList) {
            if (npc.alive && npc.regenLevel > 0) {
                npc.cell.cellRad += GameConstants.REGEN_RATE_PER_LEVEL * npc.regenLevel;
            }
        }
    }

    /**
     * Checks every living NPC's score against upgrade thresholds and auto-applies
     * a random NPC-eligible upgrade when a threshold is crossed.
     */
    private void checkNPCUpgrades() {
        for (NPC npc : npcList) {
            if (npc.alive) {
                npc.upgradeManager.checkAndAutoApplyForNPC(npc.score, npc);
            }
        }
    }

    // ── Kill-based Upgrade ───────────────────────────────────────────────

    /**
     * Called by CollisionHandler when the player eats an NPC.
     * Immediately offers an upgrade selection if none is already pending.
     */
    public void offerKillUpgrade() {
        if (gameOver || upgradeSelecting || upgradeManager.isUpgradeReady()) return;
        upgradeManager.triggerKillUpgrade();
        if (upgradeManager.isUpgradeReady()) {
            upgradeSelecting = true;
            SwingUtilities.invokeLater(this::showUpgradeSelection);
        }
    }

    // ── Dodge Mechanic ───────────────────────────────────────────────────

    /**
     * Instantly moves the player cell in the current movement direction by
     * {@code DODGE_DISTANCE} pixels, then wraps toroidally.
     * If no direction keys are held the player dashes to the right.
     */
    private void performDodge() {
        double dx = 0, dy = 0;
        if (right) dx += 1;
        if (left)  dx -= 1;
        if (up)    dy -= 1;
        if (down)  dy += 1;
        if (dx == 0 && dy == 0) dx = 1; // default: dash right

        double len = Math.sqrt(dx * dx + dy * dy);
        dx /= len;
        dy /= len;

        double cx = playerCell.getCenterX() + dx * GameConstants.DODGE_DISTANCE;
        double cy = playerCell.getCenterY() + dy * GameConstants.DODGE_DISTANCE;

        // Toroidal wrap
        cx = ((cx % MainClass.WORLD_WIDTH)  + MainClass.WORLD_WIDTH)  % MainClass.WORLD_WIDTH;
        cy = ((cy % MainClass.WORLD_HEIGHT) + MainClass.WORLD_HEIGHT) % MainClass.WORLD_HEIGHT;

        playerCell.x = cx - playerCell.cellRad;
        playerCell.y = cy - playerCell.cellRad;

        dodgeCooldownTicks = GameConstants.DODGE_COOLDOWN_TICKS;
        Sound.playDodgeSound();
    }

    // ── Upgrade Selection UI ─────────────────────────────────────────────

    /**
     * Adds three upgrade choice buttons to the panel so the player can select
     * one.  Must be called on the EDT.  {@code upgradeSelecting} is already
     * {@code true} at this point (set by the game thread).
     */
    private void showUpgradeSelection() {
        setLayout(null);

        List<UpgradeOffer> choices = upgradeManager.getCurrentChoices();
        int n = choices.size();

        int totalW = n * GameConstants.UPGRADE_CARD_WIDTH + (n - 1) * GameConstants.UPGRADE_CARD_GAP;
        int startX = (MainClass.SCREEN_WIDTH - totalW) / 2;
        int cardY   = MainClass.SCREEN_HEIGHT / 2 - 80;
        int btnY    = cardY + GameConstants.UPGRADE_CARD_HEIGHT
                           - GameConstants.UPGRADE_BTN_HEIGHT
                           - GameConstants.UPGRADE_BTN_MARGIN;
        int btnW    = GameConstants.UPGRADE_CARD_WIDTH - 2 * GameConstants.UPGRADE_BTN_MARGIN;

        for (int i = 0; i < n; i++) {
            UpgradeOffer offer = choices.get(i);
            int btnX = startX + i * (GameConstants.UPGRADE_CARD_WIDTH + GameConstants.UPGRADE_CARD_GAP)
                       + GameConstants.UPGRADE_BTN_MARGIN;

            StyledButton btn = new StyledButton(offer.displayName, GameConstants.BTN_BLUE);
            btn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 13));
            btn.setBounds(btnX, btnY, btnW, GameConstants.UPGRADE_BTN_HEIGHT);
            btn.setName("upgrade_btn");
            btn.setFocusable(false);   // prevent stealing keyboard focus from GamePanel

            final UpgradeOffer chosen = offer;
            btn.addActionListener(e -> dismissUpgradeSelection(chosen));

            add(btn);
        }

        revalidate();
        repaint();
    }

    /**
     * Applies the chosen upgrade offer, removes the choice buttons, and resumes
     * the game loop.  Must be called on the EDT.
     */
    private void dismissUpgradeSelection(UpgradeOffer chosen) {
        upgradeManager.applyUpgrade(chosen, this);

        // Remove all upgrade buttons
        Component[] components = getComponents();
        for (Component c : components) {
            if (c instanceof JButton && "upgrade_btn".equals(c.getName())) {
                remove(c);
            }
        }

        upgradeSelecting = false;
        requestFocusInWindow();   // restore keyboard focus lost when buttons were removed
        revalidate();
        repaint();
    }

    // ── Camera ───────────────────────────────────────────────────────────

    private void updateCamera() {
        double visW = MainClass.SCREEN_WIDTH / cameraZoom;
        double visH = MainClass.SCREEN_HEIGHT / cameraZoom;
        double targetX = playerCell.x + playerCell.cellRad - visW / 2.0;
        double targetY = playerCell.y + playerCell.cellRad - visH / 2.0;

        if (visW >= MainClass.WORLD_WIDTH) {
            targetX = (MainClass.WORLD_WIDTH - visW) / 2.0;
        } else {
            targetX = Math.max(0, Math.min(targetX, MainClass.WORLD_WIDTH - visW));
        }
        if (visH >= MainClass.WORLD_HEIGHT) {
            targetY = (MainClass.WORLD_HEIGHT - visH) / 2.0;
        } else {
            targetY = Math.max(0, Math.min(targetY, MainClass.WORLD_HEIGHT - visH));
        }

        // Snap camera instantly when player wraps across world boundary
        if (Math.abs(targetX - cameraX) > MainClass.WORLD_WIDTH / 2.0) cameraX = targetX;
        if (Math.abs(targetY - cameraY) > MainClass.WORLD_HEIGHT / 2.0) cameraY = targetY;

        cameraX += (targetX - cameraX) * GameConstants.CAMERA_LERP;
        cameraY += (targetY - cameraY) * GameConstants.CAMERA_LERP;
    }

    // ── Visual Effects Update ────────────────────────────────────────────

    private void updateEffects() {
        for (int i = divisionEffects.size() - 1; i >= 0; i--) {
            divisionEffects.get(i).update();
            if (divisionEffects.get(i).finished) divisionEffects.remove(i);
        }
        for (int i = eatEffects.size() - 1; i >= 0; i--) {
            eatEffects.get(i).update();
            if (eatEffects.get(i).finished) eatEffects.remove(i);
        }
        for (int i = contactEffects.size() - 1; i >= 0; i--) {
            contactEffects.get(i).update();
            if (contactEffects.get(i).finished) contactEffects.remove(i);
        }
    }

    // ── Game Over ────────────────────────────────────────────────────────

    /** Called by CollisionHandler when an NPC eats the player */
    public void onPlayerEaten(NPC eater) {
        hud.score = GameConstants.scoreFromRadius(playerCell.cellRad);
        hud.updateElapsedTime();
        finalElapsedTime = hud.elapsedTime;
        eater.grow(playerCell.cellRad);
        eatEffects.add(new EatEffect(
            playerCell.getCenterX(), playerCell.getCenterY(),
            playerCell.cellRad, playerCell.cellColor));
        triggerGameOver();
    }

    private void triggerGameOver() {
        if (gameOver) return;
        // Cancel any pending upgrade selection so it doesn't appear over the game-over screen
        if (upgradeSelecting || upgradeManager.isUpgradeReady()) {
            upgradeSelecting = false;
            upgradeManager.cancelPendingUpgrade();
        }
        // Cancel stage transition if dying during a weird edge case
        stageTransitioning = false;
        gameOver = true;

        // Save evolving progress on death
        if (evolvingMode && evolvingProgress != null) {
            evolvingProgress.updateAndSave(currentStage, hud.score);
        }

        ToneGenerator.stopLine(gameAmbientLine);
        gameAmbientLine = null;
        SwingUtilities.invokeLater(this::showGameOverScreen);
    }

    private void showGameOverScreen() {
        StyledButton restartButton = new StyledButton("MENU", GameConstants.BTN_GREEN);
        restartButton.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 20));
        int bw = GameConstants.BUTTON_WIDTH + 60;
        int bh = GameConstants.BUTTON_HEIGHT + 14;
        restartButton.setBounds((MainClass.SCREEN_WIDTH - bw) / 2,
            MainClass.SCREEN_HEIGHT - 100, bw, bh);
        restartButton.addActionListener(e -> returnToMenu());
        setLayout(null);
        add(restartButton);
        revalidate();
        repaint();
    }

    /** Stops game threads and returns to the main menu */
    private void returnToMenu() {
        running = false;
        stageTransitioning = false;
        ToneGenerator.stopLine(gameAmbientLine);
        gameAmbientLine = null;
        if (devLogDialog != null) { devLogDialog.dispose(); devLogDialog = null; }
        paused = false;

        // Final save when returning to menu from evolving mode
        if (evolvingMode && evolvingProgress != null) {
            evolvingProgress.updateAndSave(currentStage, hud.score);
        }

        mainClass.mainPanel = new MainPanel(mainClass);
        mainClass.getContentPane().removeAll();
        mainClass.getContentPane().add(mainClass.mainPanel);
        mainClass.revalidate();
        mainClass.repaint();
        mainClass.mainPanel.requestFocusInWindow();
    }

    // ── Scoreboard ───────────────────────────────────────────────────────

    /**
     * Returns a sorted scoreboard: [name, score, alive, isPlayer].
     */
    public List<Object[]> getScoreboard() {
        List<Object[]> board = new ArrayList<>();
        int playerScore = gameOver ? hud.score : GameConstants.scoreFromRadius(playerCell.cellRad);
        hud.score = playerScore;
        if (playerScore > highscore) highscore = playerScore;
        board.add(new Object[]{playerName, playerScore, true, true});

        for (NPC npc : npcList) {
            if (npc.alive) npc.score = GameConstants.scoreFromRadius(npc.cell.cellRad);
            String diffTag = npc.difficulty == NPC.Difficulty.EASY ? "[E]"
                           : npc.difficulty == NPC.Difficulty.MEDIUM ? "[M]" : "[H]";
            board.add(new Object[]{npc.name + " " + diffTag, npc.score, npc.alive, false});
        }
        board.sort((a, b) -> Integer.compare((int) b[1], (int) a[1]));
        return board;
    }

    // ── Rendering ────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.render((Graphics2D) g);
    }

    // ── Developer Log ────────────────────────────────────────────────────

    private void toggleDevLog() {
        if (devLogDialog != null && devLogDialog.isVisible()) {
            devLogDialog.dispose();
            devLogDialog = null;
            paused = false;
        } else {
            paused = true;
            devLogDialog = new DevLogDialog(mainClass, this);
            devLogDialog.setVisible(true);
        }
    }

    // ── Input Handling ───────────────────────────────────────────────────

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) return;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: right = true; break;
            case KeyEvent.VK_LEFT:  case KeyEvent.VK_A: left  = true; break;
            case KeyEvent.VK_UP:    case KeyEvent.VK_W: up    = true; break;
            case KeyEvent.VK_DOWN:  case KeyEvent.VK_S: down  = true; break;
            case KeyEvent.VK_SPACE:
                if (upgradeManager.hasDodge()) dodgeRequested = true;
                break;
            case KeyEvent.VK_I:
                if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0 ||
                    (e.getModifiersEx() & KeyEvent.META_DOWN_MASK) != 0) {
                    toggleDevLog();
                }
                break;
            case KeyEvent.VK_ESCAPE:
                boolean confirmed = StyledDialog.showConfirmDialog(mainClass,
                    "Are you sure you want to return back to Menu?", "Return to Menu");
                if (confirmed) {
                    returnToMenu();
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: right = false; break;
            case KeyEvent.VK_LEFT:  case KeyEvent.VK_A: left  = false; break;
            case KeyEvent.VK_UP:    case KeyEvent.VK_W: up    = false; break;
            case KeyEvent.VK_DOWN:  case KeyEvent.VK_S: down  = false; break;
        }
    }

}
