
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
 * camera tracking, and game state (pause, game over, easter egg).
 * @author Kamil Yunus Özkaya
 */
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

    // ── Entities ─────────────────────────────────────────────────────────

    private Cell playerCell;
    private final CopyOnWriteArrayList<Cell> foodCells = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<NPC> npcList = new CopyOnWriteArrayList<>();
    private final Background background;

    // ── Input State ──────────────────────────────────────────────────────

    private boolean right, left, up, down;

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
    private final Sound easterEggMusic = new Sound("coolMusic.wav", 1);

    // ── Game Flags ───────────────────────────────────────────────────────

    /** When true the game loop is paused (used by the developer log) */
    public volatile boolean paused = false;

    /** When true the game has ended */
    public volatile boolean gameOver = false;

    /** When true, the easter egg is playing and player actions are frozen */
    private volatile boolean easterEggActive = false;

    /** Whether the easter egg sequence has been triggered at all */
    private boolean easterEggTriggered = false;

    /** Saved elapsed time (ms) at the moment the game is won / player dies */
    private long finalElapsedTime = -1;

    /** When true, skip automatic speed recalculation (dev override active) */
    public boolean devSpeedOverride = false;

    /** Reference to the developer log dialog (null when closed) */
    private DevLogDialog devLogDialog = null;

    /** Number of NPC players in this game session */
    private final int npcCount;

    /** Flag to signal game threads to stop */
    private volatile boolean running = true;

    // ── Subsystems ───────────────────────────────────────────────────────

    private final CollisionHandler collisionHandler;
    private final GameRenderer renderer;

    // ── Constructor ──────────────────────────────────────────────────────

    public GamePanel(MainClass mainClass, int npcCount) {
        this.mainClass = mainClass;
        this.npcCount = npcCount;
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
    public boolean isPaused() { return paused; }
    public boolean isEasterEggActive() { return easterEggActive; }
    public boolean wasEasterEggTriggered() { return easterEggTriggered; }

    public long getDisplayElapsedTime() {
        return finalElapsedTime >= 0 ? finalElapsedTime : hud.elapsedTime;
    }

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
        NPC.Difficulty[] difficulties = NPC.Difficulty.values();
        for (int i = 0; i < npcCount; i++) {
            int cx = GameConstants.SPAWN_BORDER + random.nextInt(
                Math.max(1, MainClass.WORLD_WIDTH - 2 * GameConstants.SPAWN_BORDER));
            int cy = GameConstants.SPAWN_BORDER + random.nextInt(
                Math.max(1, MainClass.WORLD_HEIGHT - 2 * GameConstants.SPAWN_BORDER));
            NPC.Difficulty diff = difficulties[i % difficulties.length];
            NPC npc = new NPC(cx, cy, GameConstants.INITIAL_RADIUS, usedNames, diff);
            npcList.add(npc);
        }
    }

    // ── Food Cell Generation ─────────────────────────────────────────────

    /**
     * Returns a random food cell radius based on the three fixed categories:
     * Small (90%, radius 1), Medium (7%, radius 2–5), Large (3%, radius 5–10).
     */
    private double randomFoodRadius() {
        double roll = random.nextDouble();
        if (roll < GameConstants.SMALL_CHANCE) {
            return GameConstants.SMALL_RAD;
        } else if (roll < GameConstants.SMALL_CHANCE + GameConstants.MEDIUM_CHANCE) {
            return GameConstants.MEDIUM_RAD_MIN + random.nextDouble() * (GameConstants.MEDIUM_RAD_MAX - GameConstants.MEDIUM_RAD_MIN);
        } else {
            return GameConstants.LARGE_RAD_MIN + random.nextDouble() * (GameConstants.LARGE_RAD_MAX - GameConstants.LARGE_RAD_MIN);
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
                if (!paused && !gameOver) {
                    // Fixed speed
                    if (!devSpeedOverride) {
                        playerCell.speedX = GameConstants.DEFAULT_SPEED;
                        playerCell.speedY = GameConstants.DEFAULT_SPEED;
                    }

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

                    // Check easter egg condition
                    checkEasterEgg();
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

    // ── Easter Egg ───────────────────────────────────────────────────────

    private void checkEasterEgg() {
        if (gameOver || easterEggTriggered) return;

        boolean allNpcsDead = true;
        for (NPC npc : npcList) {
            if (npc.alive) { allNpcsDead = false; break; }
        }

        if (allNpcsDead) {
            easterEggTriggered = true;
            hud.updateElapsedTime();
            finalElapsedTime = hud.elapsedTime;
            updatePlayerScore();
            easterEggActive = true;
            hud.resetTime();
            easterEggMusic.playSound();

            Thread endThread = new Thread(() -> {
                try { Thread.sleep(20000); } catch (InterruptedException e) { return; }
                triggerGameOver();
            });
            endThread.setDaemon(true);
            endThread.start();
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
        gameOver = true;
        easterEggMusic.closeSound();
        ToneGenerator.stopLine(gameAmbientLine);
        gameAmbientLine = null;
        SwingUtilities.invokeLater(this::showGameOverScreen);
    }

    private void showGameOverScreen() {
        StyledButton restartButton = new StyledButton("RESTART", GameConstants.BTN_GREEN);
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
        ToneGenerator.stopLine(gameAmbientLine);
        gameAmbientLine = null;
        if (devLogDialog != null) { devLogDialog.dispose(); devLogDialog = null; }
        paused = false;

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

    // ── Legacy Compatibility ─────────────────────────────────────────────
    // These provide backward compatibility for code that references the old field names

    /** @deprecated Use {@link GameConstants#CELL_COLORS} instead */
    public static Color[] colors = GameConstants.CELL_COLORS;

    /** @deprecated Use {@link #getFoodCells()} instead */
    public CopyOnWriteArrayList<Cell> getCelllist() { return foodCells; }
    // DevLogDialog accesses celllist directly — provide compatibility
    public CopyOnWriteArrayList<Cell> celllist = foodCells;
}
