
import javax.swing.*;
import java.awt.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GamePanel : Game runs here
 * It creates the user cell in the middle of the world and random cells in random places
 * Puts the randomcells in a thread-safe CopyOnWriteArrayList
 * When player cell eats a random cell its size is increased as well as the score
 * Camera follows the player cell with smooth lerp, supporting toroidal world wrapping
 * Enemy cells have random sizes and appear with a smooth grow-in animation
 * @author Kamil Yunus Özkaya
 */
public class GamePanel extends JPanel implements KeyListener {
    public HUD hud = new HUD();

    private Random random = new Random();

    public static Color[] colors = {Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE, Color.YELLOW, Color.PINK};

    public static int highscore = 0;

    public static Color playerColor = Color.BLACK;
    public static int playerColorIndex = 0;

    public CopyOnWriteArrayList<Cell> celllist = new CopyOnWriteArrayList<>();

    public boolean right, left, up, down;

    public Background background;

    public Cell playerCell, randomCell, coloredCell;

    public int cellrad = 18;                            // Radius of the player cell initially

    // Enemy cell size bounds (random per spawn, capped below player size)
    private static final int MIN_ENEMY_RAD    = 8;
    private static final int MAX_ENEMY_RAD    = 35;
    private static final int SPAWN_BORDER     = 40;    // spawn boundary buffer

    private Sound music = new Sound("coolMusic.wav", 1); // Easter egg music

    public int mus = 0;

    // Camera position in world coordinates (double for smooth lerp)
    private double cameraX = 0;
    private double cameraY = 0;

    MainClass mainClass;

    // Creates the panel and runs the threads
    public GamePanel(MainClass mainClass) {

        this.mainClass = mainClass;

        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);

        setFocusable(true);

        addKeyListener(this);

        setVisible(true);

        background = new Background(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);

        // Player spawns at the center of the world
        playerCell = new Cell(MainClass.WORLD_WIDTH / 2, MainClass.WORLD_HEIGHT / 2, cellrad);
        playerCell.cellColor = playerColor;
        playerCell.spawnAlpha = 1f; // player appears immediately

        // First enemy cell at a random world position
        randomCell = new Cell(
                SPAWN_BORDER + random.nextInt(MainClass.WORLD_WIDTH  - 2 * SPAWN_BORDER),
                SPAWN_BORDER + random.nextInt(MainClass.WORLD_HEIGHT - 2 * SPAWN_BORDER),
                randomEnemyRadius());
        randomCell.cellColor = Color.BLUE;
        celllist.add(randomCell);

        cellThread();

        runGameThread();
    }

    /** Returns a random enemy radius between MIN_ENEMY_RAD and MAX_ENEMY_RAD,
     *  capped to stay at least 5 units smaller than the player's current radius. */
    private int randomEnemyRadius() {
        int cap = Math.min(MAX_ENEMY_RAD, Math.max(MIN_ENEMY_RAD + 1, playerCell.cellRad - 5));
        return MIN_ENEMY_RAD + random.nextInt(cap - MIN_ENEMY_RAD + 1);
    }

    // Randomly creates cells in random world positions
    public void cellThread() {
        Thread cellthread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (celllist.size() < 30) {
                        coloredCell = new Cell(
                                SPAWN_BORDER + random.nextInt(MainClass.WORLD_WIDTH  - 2 * SPAWN_BORDER),
                                SPAWN_BORDER + random.nextInt(MainClass.WORLD_HEIGHT - 2 * SPAWN_BORDER),
                                randomEnemyRadius());
                        coloredCell.cellColor = colors[random.nextInt(colors.length)];
                        // spawnAlpha starts at 0 — grow-in animation handled in runGameThread
                        celllist.add(coloredCell);
                    }
                    repaint();
                    try {
                        Thread.sleep(random.nextInt(12) * 1000); // 0–12 seconds between spawns
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
                    playerCell.updateCellPos(right, left, up, down); // position updated (toroidal)

                    // Advance spawn animation for all enemy cells
                    for (Cell c : celllist) {
                        if (c.spawnAlpha < 1f) c.spawnAlpha = Math.min(1f, c.spawnAlpha + 0.05f);
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

                    repaint(); // Draw everything again when thread is executed
                    hud.getElapsedTime(); // Each time thread executed, we get the elapsed time

                    for (int i = 0; i < celllist.size(); i++) {
                        if (playerCell.isCollision(playerCell, celllist.get(i))) { // Removes eaten cell, increases score
                            // Save eaten cell radius BEFORE removing it from the list
                            int eatenRad = celllist.get(i).cellRad;
                            hud.score++;
                            if (hud.score > highscore) highscore = hud.score;
                            celllist.remove(i);
                            Sound.playEatSound(); // Soothing bloop feedback on eat
                            if (celllist.size() < 20) {
                                coloredCell = new Cell(
                                        SPAWN_BORDER + random.nextInt(MainClass.WORLD_WIDTH  - 2 * SPAWN_BORDER),
                                        SPAWN_BORDER + random.nextInt(MainClass.WORLD_HEIGHT - 2 * SPAWN_BORDER),
                                        randomEnemyRadius());
                                coloredCell.cellColor = colors[random.nextInt(colors.length)];
                                celllist.add(coloredCell);
                            }
                            // Increases the size of the player cell based on volume eaten
                            int newRad = (int) Math.pow(Math.pow(playerCell.cellRad, 3) + Math.pow(eatenRad, 3), 1.0 / 3);
                            if (newRad <= playerCell.cellRad) newRad = playerCell.cellRad + 1;
                            playerCell.cellRad = newRad;
                        }
                    }

                    // Surprise here ^^
                    if (playerCell.cellRad > 400 && mus == 0) {
                        hud.resetTime();
                        mus++;
                        music.playSound();
                    }
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        int camX = (int) cameraX;
        int camY = (int) cameraY;

        // Apply camera transform: shift coordinate system so world is drawn relative to camera
        g2d.translate(-camX, -camY);

        // Draw world-space elements (background, cells, player name)
        background.drawBackground(g2d, camX, camY);

        // Draw enemy cells with smooth grow-in animation
        for (Cell c : celllist) {
            float alpha = c.spawnAlpha;
            int drawRad = (int)(c.cellRad * alpha);
            if (drawRad < 1) continue;
            Composite orig = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            c.drawCell(g2d, drawRad);
            g2d.setComposite(orig);
        }

        playerCell.drawCell(g2d, playerCell.cellRad);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("", Font.PLAIN, 12));
        g2d.drawString("Kamil", playerCell.getX() + playerCell.cellRad - cellrad, playerCell.getY() + playerCell.cellRad); // Name inside the cell

        // Restore camera transform for screen-space HUD
        g2d.translate(camX, camY);

        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("", Font.PLAIN, 12));
        g2d.drawString("Score " + hud.score, 10, 20);
        g2d.drawString("Elapsed Time " + hud.elapsedTime / 1000, 490, 20);

        if (hud.elapsedTime / 1000 > 5 && hud.elapsedTime / 1000 < 8 && mus != 0) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("", Font.BOLD, 200));
            g2d.drawString("TOO EASY???", 0, (MainClass.SCREEN_HEIGHT) / 2);
        } else if (hud.elapsedTime / 1000 >= 8 && hud.elapsedTime / 1000 < 13 && mus != 0) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("", Font.BOLD, 130));
            g2d.drawString("YOU KNOW WHAT :)", 0, (MainClass.SCREEN_HEIGHT) / 2);
        } else if (hud.elapsedTime / 1000 >= 14 && hud.elapsedTime / 1000 < 17 && mus != 0) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("", Font.BOLD, 80));
            g2d.drawString("FIRE IT LOUD", 0, (MainClass.SCREEN_HEIGHT) / 2);
            g2d.drawString("ANOTHER ROUND OF SHOTS", 0, (MainClass.SCREEN_HEIGHT + 160) / 2);
        } else if (hud.elapsedTime / 1000 > 17 && mus != 0) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("", Font.BOLD, 150));
            g2d.drawString("TURN DOWN FOR", 0, (MainClass.SCREEN_HEIGHT) / 2);
            g2d.drawString("WHAT", 0, (MainClass.SCREEN_HEIGHT + 300) / 2);
            g2d.setFont(new Font("", Font.BOLD, 50));
            g2d.drawString("HA HA HA!", random.nextInt(MainClass.SCREEN_WIDTH), random.nextInt(MainClass.SCREEN_HEIGHT));
        }
    }

    // Below part is for using the keyboard as controller
    // W A S D and Arrow keys
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
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
            case KeyEvent.VK_ESCAPE:
                int confirmed = JOptionPane.showConfirmDialog(null, "Are you sure you want to return back to Menu?",
                        "Exit Program Message Box", JOptionPane.YES_NO_OPTION);
                if (confirmed == JOptionPane.YES_OPTION) {
                    mainClass.mainPanel = new MainPanel(mainClass);
                    mainClass.getContentPane().removeAll();
                    mainClass.getContentPane().add(mainClass.mainPanel);
                    mainClass.mainPanel.requestFocusInWindow();
                    mainClass.revalidate();
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
