
import javax.swing.*;
import java.awt.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GamePanel : Game runs here
 * It creates the user cell in the middle of the world and random cells in random places
 * Puts the randomcells in a thread-safe CopyOnWriteArrayList
 * When player cell eats a random cell its size is increased as well as the score (by eaten cell's radius)
 * Camera follows the player cell with smooth lerp, supporting toroidal world wrapping
 * Enemy cells have random sizes and appear with a smooth grow-in animation
 * Player name is displayed centered and fitted inside the player cell
 * Dynamic speed: faster when small, slower when large, minimum speed is 3
 * NPC cells move randomly, eat smaller cells, and compete on the scoreboard
 * Easter egg triggers when all NPCs are eliminated; game ends with stats display
 * Developer log (Ctrl+I / Cmd+I) pauses game and shows editable world state
 * @author Kamil Yunus Özkaya
 */
public class GamePanel extends JPanel implements KeyListener {
    public HUD hud = new HUD();

    private Random random = new Random();

    public static Color[] colors = {Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE, Color.YELLOW, Color.PINK};

    public static int highscore = 0;

    public static Color playerColor = Color.BLACK;
    public static int playerColorIndex = 0;

    /** Player display name, set from the main menu before game starts */
    public static String playerName = "Player";

    public CopyOnWriteArrayList<Cell> celllist = new CopyOnWriteArrayList<>();

    /** List of NPC entities that move, eat, and compete */
    public CopyOnWriteArrayList<NPC> npcList = new CopyOnWriteArrayList<>();

    public boolean right, left, up, down;

    public Background background;

    public Cell playerCell, randomCell, coloredCell;

    public double cellrad = 2;                           // Radius of the player cell initially

    // Food cell categories (Small / Medium / Large)
    // These values are fixed and cannot be changed in-game.
    private static final double SMALL_RAD        = 1;     // Small: radius 1
    private static final double MEDIUM_RAD_MIN   = 2;     // Medium: radius 2–5
    private static final double MEDIUM_RAD_MAX   = 5;
    private static final double LARGE_RAD_MIN    = 5;     // Large: radius 5–10
    private static final double LARGE_RAD_MAX    = 10;
    private static final double SMALL_CHANCE     = 0.90;  // 90% small
    private static final double MEDIUM_CHANCE    = 0.07;  // 7% medium
    // Large = remaining 3%
    private static final int SPAWN_BORDER        = 40;    // spawn boundary buffer

    // Dynamic speed constants
    private static final double BASE_SPEED       = 7;     // speed at initial radius
    private static final double INITIAL_RAD      = 2;     // player starting radius
    private static final double MIN_SPEED        = 3;     // speed never drops below this

    private Sound music = new Sound("coolMusic.wav", 1); // Easter egg music

    public int mus = 0;

    // Camera position in world coordinates (double for smooth lerp)
    private double cameraX = 0;
    private double cameraY = 0;

    /** When true the game loop is paused (used by the developer log) */
    public volatile boolean paused = false;

    /** When true the game has ended (all NPCs eliminated or player eaten) */
    public volatile boolean gameOver = false;

    /** When true, skip automatic speed recalculation (dev override active) */
    public boolean devSpeedOverride = false;

    /** Reference to the developer log dialog (null when closed) */
    private DevLogDialog devLogDialog = null;

    /** Number of NPC players in this game session */
    private int npcCount;

    /**
     * Cell density: number of food cells per million world-area pixels.
     * Default: 200 cells per 1000x1000 area = 200 cells per million px.
     * Configurable via Options and Dev Log.
     */
    public static double cellDensity = 200.0;

    /** Returns the maximum number of food cells based on current world size and density */
    public int getMaxCells() {
        double worldArea = (double) MainClass.WORLD_WIDTH * MainClass.WORLD_HEIGHT / 1_000_000.0;
        return Math.max(5, (int) Math.round(cellDensity * worldArea));
    }

    MainClass mainClass;

    // Creates the panel and runs the threads
    public GamePanel(MainClass mainClass, int npcCount) {

        this.mainClass = mainClass;
        this.npcCount = npcCount;

        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
        setPreferredSize(new Dimension(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT));

        setFocusable(true);

        addKeyListener(this);

        setVisible(true);

        background = new Background(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);

        // Player spawns at the center of the world
        playerCell = new Cell(MainClass.WORLD_WIDTH / 2, MainClass.WORLD_HEIGHT / 2, cellrad);
        playerCell.cellColor = playerColor;
        playerCell.spawnAlpha = 1f; // player appears immediately
        playerCell.speedX = BASE_SPEED;
        playerCell.speedY = BASE_SPEED;

        // First enemy cell at a non-overlapping world position
        randomCell = generateNonOverlappingCell();
        randomCell.cellColor = Color.BLUE;
        celllist.add(randomCell);

        // Spawn NPC cells at non-overlapping positions
        spawnNPCs();

        cellThread();

        runGameThread();
    }

    /** Spawns the configured number of NPC cells at non-overlapping world positions */
    private void spawnNPCs() {
        Set<String> usedNames = new HashSet<>();
        usedNames.add(playerName);
        for (int i = 0; i < npcCount; i++) {
            int cx = SPAWN_BORDER + random.nextInt(MainClass.WORLD_WIDTH  - 2 * SPAWN_BORDER);
            int cy = SPAWN_BORDER + random.nextInt(MainClass.WORLD_HEIGHT - 2 * SPAWN_BORDER);
            NPC npc = new NPC(cx, cy, INITIAL_RAD, usedNames);
            npcList.add(npc);
        }
    }

    /**
     * Returns a random food cell radius based on the three fixed categories:
     * Small (90%, radius 1), Medium (7%, radius 2–5), Large (3%, radius 5–10).
     */
    private double randomFoodRadius() {
        double roll = random.nextDouble();
        if (roll < SMALL_CHANCE) {
            return SMALL_RAD;
        } else if (roll < SMALL_CHANCE + MEDIUM_CHANCE) {
            return MEDIUM_RAD_MIN + random.nextDouble() * (MEDIUM_RAD_MAX - MEDIUM_RAD_MIN);
        } else {
            return LARGE_RAD_MIN + random.nextDouble() * (LARGE_RAD_MAX - LARGE_RAD_MIN);
        }
    }

    /**
     * Generates a new food cell at a position that does not overlap the player, any
     * existing food cell, or any NPC. Attempts up to 50 random positions before falling back.
     * Food cells cannot move, cannot eat other cells. Their radius is determined by
     * the fixed category distribution (Small 90%, Medium 7%, Large 3%).
     */
    private Cell generateNonOverlappingCell() {
        final int MAX_ATTEMPTS = 50;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int cx = SPAWN_BORDER + random.nextInt(MainClass.WORLD_WIDTH  - 2 * SPAWN_BORDER);
            int cy = SPAWN_BORDER + random.nextInt(MainClass.WORLD_HEIGHT - 2 * SPAWN_BORDER);
            double r = randomFoodRadius();

            // Check clearance against the player cell
            double dx = cx - (playerCell.x + playerCell.cellRad);
            double dy = cy - (playerCell.y + playerCell.cellRad);
            if (Math.sqrt(dx * dx + dy * dy) < playerCell.cellRad + r + 20) continue;

            // Check clearance against all existing food cells
            boolean overlaps = false;
            for (Cell c : celllist) {
                double ex = cx - (c.x + c.cellRad);
                double ey = cy - (c.y + c.cellRad);
                if (Math.sqrt(ex * ex + ey * ey) < c.cellRad + r + 10) {
                    overlaps = true;
                    break;
                }
            }
            if (overlaps) continue;

            // Check clearance against all NPC cells
            for (NPC npc : npcList) {
                if (!npc.alive) continue;
                double nx = cx - (npc.cell.x + npc.cell.cellRad);
                double ny = cy - (npc.cell.y + npc.cell.cellRad);
                if (Math.sqrt(nx * nx + ny * ny) < npc.cell.cellRad + r + 10) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) return new Cell(cx, cy, r);
        }
        // Fallback (rare): return a cell ignoring overlaps
        return new Cell(
            SPAWN_BORDER + random.nextInt(MainClass.WORLD_WIDTH  - 2 * SPAWN_BORDER),
            SPAWN_BORDER + random.nextInt(MainClass.WORLD_HEIGHT - 2 * SPAWN_BORDER),
            randomFoodRadius());
    }

    /**
     * Spawns the initial batch of food cells to fill up the world,
     * then continuously tops up at a steady rate.
     */
    public void cellThread() {
        Thread cellthread = new Thread() {
            @Override
            public void run() {
                // Initial batch spawn: fill up to max cells quickly
                int maxCells = getMaxCells();
                while (celllist.size() < maxCells && !gameOver) {
                    Cell c = generateNonOverlappingCell();
                    c.cellColor = colors[random.nextInt(colors.length)];
                    c.spawnAlpha = 1f; // initial cells appear immediately
                    celllist.add(c);
                }
                // Continuous top-up: spawn a small batch every tick to replace eaten cells
                while (true) {
                    if (!gameOver) {
                        int deficit = getMaxCells() - celllist.size();
                        int batch = Math.min(deficit, 10); // spawn up to 10 per cycle
                        for (int i = 0; i < batch; i++) {
                            Cell c = generateNonOverlappingCell();
                            c.cellColor = colors[random.nextInt(colors.length)];
                            // spawnAlpha starts at 0 — grow-in animation handled in runGameThread
                            celllist.add(c);
                        }
                    }
                    repaint();
                    try {
                        Thread.sleep(500); // check every 500ms
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        cellthread.setDaemon(true);
        cellthread.start();
    }

    // Game runs in this thread
    public void runGameThread() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (!paused && !gameOver) {
                        // Dynamic speed: faster at small size, slower as player grows, min 3
                        // Speed scales with area ratio: speed = BASE_SPEED * (initialArea / currentArea)
                        // = BASE_SPEED * (INITIAL_RAD / cellRad)^2
                        if (!devSpeedOverride) {
                            double ratio = INITIAL_RAD / playerCell.cellRad;
                            double dynSpeed = Math.max(MIN_SPEED, BASE_SPEED * ratio * ratio);
                            playerCell.speedX = dynSpeed;
                            playerCell.speedY = dynSpeed;
                        }

                        playerCell.updateCellPos(right, left, up, down); // position updated (toroidal)

                        // Advance spawn animation for all enemy cells
                        for (Cell c : celllist) {
                            if (c.spawnAlpha < 1f) c.spawnAlpha = Math.min(1f, c.spawnAlpha + 0.05f);
                        }

                        // Update NPC positions and AI (with navigation helper)
                        for (NPC npc : npcList) {
                            if (npc.alive) npc.update(playerCell, npcList, celllist);
                        }

                        // Smooth camera lerp — follows player center
                        double targetX = playerCell.x + playerCell.cellRad - MainClass.SCREEN_WIDTH  / 2.0;
                        double targetY = playerCell.y + playerCell.cellRad - MainClass.SCREEN_HEIGHT / 2.0;
                        targetX = Math.max(0, Math.min(targetX, MainClass.WORLD_WIDTH  - MainClass.SCREEN_WIDTH));
                        targetY = Math.max(0, Math.min(targetY, MainClass.WORLD_HEIGHT - MainClass.SCREEN_HEIGHT));

                        // Snap camera instantly when player wraps across a world boundary
                        if (Math.abs(targetX - cameraX) > MainClass.WORLD_WIDTH  / 2.0) cameraX = targetX;
                        if (Math.abs(targetY - cameraY) > MainClass.WORLD_HEIGHT / 2.0) cameraY = targetY;

                        cameraX += (targetX - cameraX) * 0.15;
                        cameraY += (targetY - cameraY) * 0.15;

                        hud.getElapsedTime(); // Each time thread executed, we get the elapsed time

                        // Player eats food cells
                        for (int i = 0; i < celllist.size(); i++) {
                            if (playerCell.isCollision(playerCell, celllist.get(i))) { // Removes eaten cell, increases score
                                // Save eaten cell radius BEFORE removing it from the list
                                double eatenRad = celllist.get(i).cellRad;
                                hud.score += (int) Math.ceil(eatenRad);
                                if (hud.score > highscore) highscore = hud.score;
                                celllist.remove(i);
                                Sound.playEatSound(); // Soothing bloop feedback on eat
                                // Area-based growth: r3 = sqrt(r1^2 + r2^2)
                                double newRad = Math.sqrt(playerCell.cellRad * playerCell.cellRad + eatenRad * eatenRad);
                                playerCell.cellRad = newRad;
                            }
                        }

                        // Player eats NPC cells (player bigger than NPC)
                        for (NPC npc : npcList) {
                            if (npc.alive && playerCell.isCollision(playerCell, npc.cell)) {
                                double eatenRad = npc.cell.cellRad;
                                hud.score += (int) Math.ceil(eatenRad);
                                if (hud.score > highscore) highscore = hud.score;
                                npc.alive = false;
                                Sound.playEatSound();
                                double newRad = Math.sqrt(playerCell.cellRad * playerCell.cellRad + eatenRad * eatenRad);
                                playerCell.cellRad = newRad;
                            }
                        }

                        // NPCs eat food cells
                        for (NPC npc : npcList) {
                            if (!npc.alive) continue;
                            for (int i = 0; i < celllist.size(); i++) {
                                if (i >= celllist.size()) break;
                                Cell food = celllist.get(i);
                                if (npc.cell.isCollision(npc.cell, food)) {
                                    double eatenRad = food.cellRad;
                                    celllist.remove(i);
                                    npc.grow(eatenRad);
                                    i--;
                                }
                            }
                        }

                        // NPCs eat other NPCs (bigger eats smaller)
                        for (NPC predator : npcList) {
                            if (!predator.alive) continue;
                            for (NPC prey : npcList) {
                                if (!prey.alive || predator == prey) continue;
                                if (predator.cell.isCollision(predator.cell, prey.cell)) {
                                    double eatenRad = prey.cell.cellRad;
                                    prey.alive = false;
                                    predator.grow(eatenRad);
                                }
                            }
                        }

                        // NPC eats the player (NPC bigger than player)
                        for (NPC npc : npcList) {
                            if (!npc.alive) continue;
                            if (npc.cell.isCollision(npc.cell, playerCell)) {
                                // Player is eaten — game over
                                npc.grow((double) playerCell.cellRad);
                                triggerGameOver();
                                break;
                            }
                        }

                        // Easter egg: when all NPCs are dead, play the sound and end the game
                        if (!gameOver && mus == 0) {
                            boolean allNpcsDead = true;
                            for (NPC npc : npcList) {
                                if (npc.alive) { allNpcsDead = false; break; }
                            }
                            if (allNpcsDead) {
                                mus++;
                                hud.resetTime();
                                music.playSound();
                                // Wait for music to finish (~20 seconds), then end the game
                                Thread endThread = new Thread(() -> {
                                    try {
                                        Thread.sleep(20000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    triggerGameOver();
                                });
                                endThread.setDaemon(true);
                                endThread.start();
                            }
                        }
                    }

                    repaint(); // Draw everything again when thread is executed
                    try {
                        Thread.sleep(10); // 10 ms tick (~100 FPS)
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    /** Triggers the game over state and displays the end screen */
    private void triggerGameOver() {
        if (gameOver) return;
        gameOver = true;
        music.closeSound();
        SwingUtilities.invokeLater(this::showGameOverScreen);
    }

    /** Shows the game over overlay with a restart button */
    private void showGameOverScreen() {
        JButton restartButton = new JButton("RESTART");
        restartButton.setFont(new Font("Arial", Font.BOLD, 20));
        restartButton.setBackground(new Color(40, 120, 60));
        restartButton.setForeground(Color.WHITE);
        restartButton.setFocusPainted(false);
        int bw = MainClass.BUTTON_WIDTH + 40;
        int bh = MainClass.BUTTON_HEIGHT + 10;
        restartButton.setBounds((MainClass.SCREEN_WIDTH - bw) / 2,
            MainClass.SCREEN_HEIGHT - 100, bw, bh);
        restartButton.addActionListener(e -> {
            mainClass.mainPanel = new MainPanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.mainPanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.mainPanel.requestFocusInWindow();
        });
        setLayout(null);
        add(restartButton);
        revalidate();
        repaint();
    }

    /**
     * Builds a sorted scoreboard of all players (human + NPCs), sorted by score descending.
     * Each entry is: [name, score, alive status, isPlayer flag].
     */
    private java.util.List<Object[]> getScoreboard() {
        java.util.List<Object[]> board = new ArrayList<>();
        board.add(new Object[]{playerName, hud.score, true, true});
        for (NPC npc : npcList) {
            board.add(new Object[]{npc.name, npc.score, npc.alive, false});
        }
        board.sort((a, b) -> Integer.compare((int) b[1], (int) a[1]));
        return board;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smooth cell rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int camX = (int) cameraX;
        int camY = (int) cameraY;

        // Apply camera transform: shift coordinate system so world is drawn relative to camera
        g2d.translate(-camX, -camY);

        // Draw world-space elements (background, cells, player name)
        background.drawBackground(g2d, camX, camY);

        // Draw enemy cells with smooth grow-in animation
        for (Cell c : celllist) {
            float alpha = c.spawnAlpha;
            int drawRad = Math.max(1, (int) Math.round(c.cellRad * alpha));
            if (drawRad < 1) continue;
            Composite orig = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            c.drawCell(g2d, drawRad);
            g2d.setComposite(orig);
        }

        // Draw NPC cells with names
        for (NPC npc : npcList) {
            if (!npc.alive) continue;
            int npcDrawRad = (int) Math.round(npc.cell.cellRad);
            npc.cell.drawCell(g2d, npcDrawRad);
            // Draw NPC name centered inside its cell
            int fontSize = Math.max(8, Math.min(npcDrawRad / 2, 24));
            Font nameFont = new Font("Arial", Font.BOLD, fontSize);
            g2d.setFont(nameFont);
            FontMetrics fm = g2d.getFontMetrics();
            int nameW = fm.stringWidth(npc.name);
            int nameH = fm.getAscent();
            int nameX = (int) Math.round(npc.cell.x + npc.cell.cellRad) - nameW / 2;
            int nameY = (int) Math.round(npc.cell.y + npc.cell.cellRad) + nameH / 2 - 2;
            g2d.setColor(Color.WHITE);
            g2d.drawString(npc.name, nameX, nameY);
        }

        // Draw player cell and name (always visible unless game over from being eaten)
        if (!gameOver || mus > 0) {
            int playerDrawRad = (int) Math.round(playerCell.cellRad);
            playerCell.drawCell(g2d, playerDrawRad);

            // Draw player name centered and font-size fitted inside the player cell
            int fontSize = Math.max(8, Math.min(playerDrawRad / 2, 24));
            Font nameFont = new Font("Arial", Font.BOLD, fontSize);
            g2d.setFont(nameFont);
            FontMetrics fm = g2d.getFontMetrics();
            int nameW = fm.stringWidth(playerName);
            int nameH = fm.getAscent();
            int nameX = (int) Math.round(playerCell.x + playerCell.cellRad) - nameW / 2;
            int nameY = (int) Math.round(playerCell.y + playerCell.cellRad) + nameH / 2 - 2;
            g2d.setColor(Color.WHITE);
            g2d.drawString(playerName, nameX, nameY);
        }

        // Restore camera transform for screen-space HUD
        g2d.translate(camX, camY);

        // Draw HUD: score and elapsed time
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("", Font.PLAIN, 12));
        g2d.drawString("Score " + hud.score, 10, 20);
        g2d.drawString("Elapsed Time " + hud.elapsedTime / 1000, 490, 20);

        // Draw scoreboard in top-right corner
        drawScoreboard(g2d);

        // Easter egg text overlays (while music is playing before game over)
        if (mus != 0 && !gameOver) {
            long secs = hud.elapsedTime / 1000;
            if (secs > 5 && secs < 8) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("", Font.BOLD, 200));
                g2d.drawString("TOO EASY???", 0, (MainClass.SCREEN_HEIGHT) / 2);
            } else if (secs >= 8 && secs < 13) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("", Font.BOLD, 130));
                g2d.drawString("YOU KNOW WHAT :)", 0, (MainClass.SCREEN_HEIGHT) / 2);
            } else if (secs >= 14 && secs < 17) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("", Font.BOLD, 80));
                g2d.drawString("FIRE IT LOUD", 0, (MainClass.SCREEN_HEIGHT) / 2);
                g2d.drawString("ANOTHER ROUND OF SHOTS", 0, (MainClass.SCREEN_HEIGHT + 160) / 2);
            } else if (secs > 17) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("", Font.BOLD, 150));
                g2d.drawString("TURN DOWN FOR", 0, (MainClass.SCREEN_HEIGHT) / 2);
                g2d.drawString("WHAT", 0, (MainClass.SCREEN_HEIGHT + 300) / 2);
                g2d.setFont(new Font("", Font.BOLD, 50));
                g2d.drawString("HA HA HA!", random.nextInt(MainClass.SCREEN_WIDTH), random.nextInt(MainClass.SCREEN_HEIGHT));
            }
        }

        // Draw paused overlay when dev log is open
        if (paused && !gameOver) {
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRect(0, 0, MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 28));
            String pauseMsg = "[ PAUSED \u2014 Dev Log Open ]";
            FontMetrics pfm = g2d.getFontMetrics();
            int px = (MainClass.SCREEN_WIDTH - pfm.stringWidth(pauseMsg)) / 2;
            g2d.drawString(pauseMsg, px, MainClass.SCREEN_HEIGHT / 2);
        }

        // Draw game over overlay
        if (gameOver) {
            drawGameOverOverlay(g2d);
        }
    }

    /** Draws the scoreboard in the top-right corner of the screen */
    private void drawScoreboard(Graphics2D g2d) {
        java.util.List<Object[]> board = getScoreboard();
        int sbWidth = 200;
        int lineHeight = 20;
        int sbX = MainClass.SCREEN_WIDTH - sbWidth - 10;
        int sbY = 10;

        // Background
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fillRoundRect(sbX - 5, sbY - 5, sbWidth + 10,
            lineHeight * (board.size() + 1) + 15, 10, 10);

        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.YELLOW);
        g2d.drawString("SCOREBOARD", sbX + 40, sbY + 14);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        int y = sbY + 14 + lineHeight;
        int rank = 1;
        for (Object[] entry : board) {
            String name = (String) entry[0];
            int score = (int) entry[1];
            boolean alive = (boolean) entry[2];
            boolean isPlayer = (boolean) entry[3];

            if (isPlayer) {
                // Highlight player's score
                g2d.setColor(new Color(255, 255, 100));
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
            } else {
                g2d.setColor(alive ? Color.WHITE : new Color(150, 150, 150));
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            }
            String status = alive ? "" : " [dead]";
            String line = rank + ". " + name + " - " + score + status;
            g2d.drawString(line, sbX, y);
            y += lineHeight;
            rank++;
        }
    }

    /** Draws the game over overlay with final stats */
    private void drawGameOverOverlay(Graphics2D g2d) {
        // Dark overlay
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);

        // Game Over title
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 64));
        String title = "GAME OVER";
        FontMetrics tfm = g2d.getFontMetrics();
        int tx = (MainClass.SCREEN_WIDTH - tfm.stringWidth(title)) / 2;
        g2d.drawString(title, tx, 120);

        // Final stats
        java.util.List<Object[]> board = getScoreboard();
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(Color.WHITE);
        String subTitle = "Final Standings";
        FontMetrics sfm = g2d.getFontMetrics();
        g2d.drawString(subTitle, (MainClass.SCREEN_WIDTH - sfm.stringWidth(subTitle)) / 2, 170);

        int y = 210;
        int rank = 1;
        for (Object[] entry : board) {
            String name = (String) entry[0];
            int score = (int) entry[1];
            boolean isPlayer = (boolean) entry[3];

            if (isPlayer) {
                g2d.setColor(new Color(255, 255, 100));
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
            } else {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            }
            String line = "#" + rank + "  " + name + "  \u2014  Score: " + score;
            FontMetrics lfm = g2d.getFontMetrics();
            g2d.drawString(line, (MainClass.SCREEN_WIDTH - lfm.stringWidth(line)) / 2, y);
            y += 30;
            rank++;
        }

        // Player's final time
        g2d.setColor(new Color(180, 220, 255));
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        String timeStr = "Time played: " + hud.elapsedTime / 1000 + " seconds";
        FontMetrics fm2 = g2d.getFontMetrics();
        g2d.drawString(timeStr, (MainClass.SCREEN_WIDTH - fm2.stringWidth(timeStr)) / 2, y + 20);
    }

    /**
     * Toggles the developer log dialog.
     * If it is already open, closes it (unpausing the game).
     * If it is closed, opens it (pausing the game).
     */
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

    // Below part is for using the keyboard as controller
    // W A S D and Arrow keys
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) return;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT:
                right = true;
                break;
            case KeyEvent.VK_D:
                right = true;
                break;
            case KeyEvent.VK_LEFT:
                left = true;
                break;
            case KeyEvent.VK_A:
                left = true;
                break;
            case KeyEvent.VK_UP:
                up = true;
                break;
            case KeyEvent.VK_W:
                up = true;
                break;
            case KeyEvent.VK_DOWN:
                down = true;
                break;
            case KeyEvent.VK_S:
                down = true;
                break;
            case KeyEvent.VK_I:
                // Ctrl+I (Windows/Linux) or Cmd+I (macOS)
                if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0 ||
                    (e.getModifiersEx() & KeyEvent.META_DOWN_MASK) != 0) {
                    toggleDevLog();
                }
                break;
            case KeyEvent.VK_ESCAPE:
                int confirmed = JOptionPane.showConfirmDialog(null, "Are you sure you want to return back to Menu?",
                        "Exit Program Message Box", JOptionPane.YES_NO_OPTION);
                if (confirmed == JOptionPane.YES_OPTION) {
                    if (devLogDialog != null) { devLogDialog.dispose(); devLogDialog = null; }
                    paused = false;
                    mainClass.mainPanel = new MainPanel(mainClass);
                    mainClass.getContentPane().removeAll();
                    mainClass.getContentPane().add(mainClass.mainPanel);
                    mainClass.revalidate();
                    mainClass.repaint();
                    mainClass.mainPanel.requestFocusInWindow();
                    break;
                } else {
                    break;
                }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT:
                right = false;
                break;
            case KeyEvent.VK_D:
                right = false;
                break;
            case KeyEvent.VK_LEFT:
                left = false;
                break;
            case KeyEvent.VK_A:
                left = false;
                break;
            case KeyEvent.VK_UP:
                up = false;
                break;
            case KeyEvent.VK_W:
                up = false;
                break;
            case KeyEvent.VK_DOWN:
                down = false;
                break;
            case KeyEvent.VK_S:
                down = false;
                break;
        }
    }
}
