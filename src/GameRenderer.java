import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.Random;

/**
 * GameRenderer : Handles all drawing/rendering for the game.
 * Extracted from GamePanel to separate rendering concerns from game logic.
 * Draws the world, cells, NPCs, player, effects, HUD, scoreboard, and overlays.
 * @author Kamil Yunus Ozkaya
 */
public class GameRenderer {

    private final GamePanel game;
    private final Random random = new Random();

    public GameRenderer(GamePanel game) {
        this.game = game;
    }

    /**
     * Main render method — called from GamePanel.paintComponent().
     * Handles the full rendering pipeline: world space → effects → screen space HUD.
     */
    public void render(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        AffineTransform savedTransform = g2d.getTransform();

        int camX = (int) game.getCameraX();
        int camY = (int) game.getCameraY();
        double zoom = game.getCameraZoom();

        // Apply camera zoom and translation
        g2d.scale(zoom, zoom);
        g2d.translate(-camX, -camY);

        int visibleW = (int) Math.ceil(MainClass.SCREEN_WIDTH / zoom);
        int visibleH = (int) Math.ceil(MainClass.SCREEN_HEIGHT / zoom);

        // World-space drawing
        game.getWorldBackground().drawBackground(g2d, camX, camY, visibleW, visibleH);
        drawFoodCells(g2d);
        drawNPCs(g2d);
        drawPlayer(g2d);
        drawEffects(g2d);

        // Restore to screen space for HUD
        g2d.setTransform(savedTransform);

        drawHUD(g2d);
        drawScoreboard(g2d);
        drawEasterEggOverlay(g2d);
        drawPausedOverlay(g2d);
        if (game.isGameOver()) {
            drawGameOverOverlay(g2d);
        }
    }

    // ── World-space Drawing ──────────────────────────────────────────────

    private void drawFoodCells(Graphics2D g2d) {
        for (Cell c : game.getFoodCells()) {
            float alpha = c.spawnAlpha;
            int drawRad = Math.max(1, (int) Math.round(c.cellRad * alpha));
            if (drawRad < 1) continue;
            Composite orig = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            c.drawCell(g2d, drawRad);
            g2d.setComposite(orig);
        }
    }

    private void drawNPCs(Graphics2D g2d) {
        for (NPC npc : game.getNPCList()) {
            if (!npc.alive) continue;
            int npcDrawRad = (int) Math.round(npc.cell.cellRad);
            npc.cell.drawCell(g2d, npcDrawRad);
            drawCellName(g2d, npc.cell, npc.name, npcDrawRad);
        }
    }

    private void drawPlayer(Graphics2D g2d) {
        if (game.isGameOver() && !game.wasEasterEggTriggered()) return;

        Cell player = game.getPlayerCell();
        int playerDrawRad = (int) Math.round(player.cellRad);
        player.drawCell(g2d, playerDrawRad);
        drawCellName(g2d, player, GamePanel.playerName, playerDrawRad);
    }

    private void drawCellName(Graphics2D g2d, Cell cell, String name, int drawRad) {
        int fontSize = Math.max(8, Math.min(drawRad / 2, 24));
        Font nameFont = new Font(GameConstants.FONT_FAMILY_MONO, Font.BOLD, fontSize);
        g2d.setFont(nameFont);
        FontMetrics fm = g2d.getFontMetrics();
        int nameW = fm.stringWidth(name);
        int nameH = fm.getAscent();
        int nameX = (int) Math.round(cell.getCenterX()) - nameW / 2;
        int nameY = (int) Math.round(cell.getCenterY()) + nameH / 2 - 2;
        g2d.setColor(Color.WHITE);
        g2d.drawString(name, nameX, nameY);
    }

    private void drawEffects(Graphics2D g2d) {
        for (DivisionEffect effect : game.getDivisionEffects()) {
            effect.draw(g2d);
        }
        for (EatEffect effect : game.getEatEffects()) {
            effect.draw(g2d);
        }
        for (ContactEffect effect : game.getContactEffects()) {
            effect.draw(g2d);
        }
    }

    // ── Screen-space HUD Drawing ─────────────────────────────────────────

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 12));
        g2d.drawString("Score " + game.getHUD().score, 10, 20);

        long displayElapsed = game.getDisplayElapsedTime();
        g2d.drawString("Elapsed Time " + displayElapsed / 1000, 490, 20);
    }

    private void drawScoreboard(Graphics2D g2d) {
        List<Object[]> board = game.getScoreboard();
        int sbWidth = 200;
        int lineHeight = 20;
        int sbX = MainClass.SCREEN_WIDTH - sbWidth - 10;
        int sbY = 10;

        // Background
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fillRoundRect(sbX - 5, sbY - 5, sbWidth + 10,
            lineHeight * (board.size() + 1) + 15, 10, 10);

        // Title
        g2d.setFont(new Font(GameConstants.FONT_FAMILY_MONO, Font.BOLD, 14));
        g2d.setColor(Color.YELLOW);
        g2d.drawString("SCOREBOARD", sbX + 40, sbY + 14);

        g2d.setFont(new Font(GameConstants.FONT_FAMILY_MONO, Font.PLAIN, 12));
        int y = sbY + 14 + lineHeight;
        int rank = 1;
        for (Object[] entry : board) {
            String name = (String) entry[0];
            int score = (int) entry[1];
            boolean alive = (boolean) entry[2];
            boolean isPlayer = (boolean) entry[3];

            if (isPlayer) {
                g2d.setColor(new Color(255, 255, 100));
                g2d.setFont(new Font(GameConstants.FONT_FAMILY_MONO, Font.BOLD, 12));
            } else {
                g2d.setColor(alive ? Color.WHITE : new Color(150, 150, 150));
                g2d.setFont(new Font(GameConstants.FONT_FAMILY_MONO, Font.PLAIN, 12));
            }
            String status = alive ? "" : " [dead]";
            String line = rank + ". " + name + " - " + score + status;
            g2d.drawString(line, sbX, y);
            y += lineHeight;
            rank++;
        }
    }

    private void drawEasterEggOverlay(Graphics2D g2d) {
        if (!game.wasEasterEggTriggered() || game.isGameOver()) return;

        long secs = game.getHUD().elapsedTime / 1000;
        g2d.setColor(Color.WHITE);

        if (secs > 5 && secs < 8) {
            g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 200));
            g2d.drawString("TOO EASY???", 0, MainClass.SCREEN_HEIGHT / 2);
        } else if (secs >= 8 && secs < 13) {
            g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 130));
            g2d.drawString("YOU KNOW WHAT :)", 0, MainClass.SCREEN_HEIGHT / 2);
        } else if (secs >= 14 && secs < 17) {
            g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 80));
            g2d.drawString("FIRE IT LOUD", 0, MainClass.SCREEN_HEIGHT / 2);
            g2d.drawString("ANOTHER ROUND OF SHOTS", 0, (MainClass.SCREEN_HEIGHT + 160) / 2);
        } else if (secs > 17) {
            g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 150));
            g2d.drawString("TURN DOWN FOR", 0, MainClass.SCREEN_HEIGHT / 2);
            g2d.drawString("WHAT", 0, (MainClass.SCREEN_HEIGHT + 300) / 2);
            g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 50));
            g2d.drawString("HA HA HA!",
                random.nextInt(MainClass.SCREEN_WIDTH), random.nextInt(MainClass.SCREEN_HEIGHT));
        }
    }

    private void drawPausedOverlay(Graphics2D g2d) {
        if (!game.isPaused() || game.isGameOver()) return;

        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font(GameConstants.FONT_FAMILY_MONO, Font.BOLD, 28));
        String pauseMsg = "[ PAUSED \u2014 Dev Log Open ]";
        FontMetrics pfm = g2d.getFontMetrics();
        int px = (MainClass.SCREEN_WIDTH - pfm.stringWidth(pauseMsg)) / 2;
        g2d.drawString(pauseMsg, px, MainClass.SCREEN_HEIGHT / 2);
    }

    private void drawGameOverOverlay(Graphics2D g2d) {
        // Dark overlay
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);

        // Game Over title
        g2d.setColor(Color.RED);
        g2d.setFont(new Font(GameConstants.FONT_FAMILY_MONO, Font.BOLD, 64));
        String title = "GAME OVER";
        FontMetrics tfm = g2d.getFontMetrics();
        int tx = (MainClass.SCREEN_WIDTH - tfm.stringWidth(title)) / 2;
        g2d.drawString(title, tx, 120);

        // Final standings
        List<Object[]> board = game.getScoreboard();
        g2d.setFont(new Font(GameConstants.FONT_FAMILY_MONO, Font.BOLD, 20));
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
                g2d.setFont(new Font(GameConstants.FONT_FAMILY_MONO, Font.BOLD, 18));
            } else {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font(GameConstants.FONT_FAMILY_MONO, Font.PLAIN, 18));
            }
            String line = "#" + rank + "  " + name + "  \u2014  Score: " + score;
            FontMetrics lfm = g2d.getFontMetrics();
            g2d.drawString(line, (MainClass.SCREEN_WIDTH - lfm.stringWidth(line)) / 2, y);
            y += 30;
            rank++;
        }

        // Time played
        long displayTime = game.getDisplayElapsedTime();
        g2d.setColor(new Color(180, 220, 255));
        g2d.setFont(new Font(GameConstants.FONT_FAMILY_MONO, Font.PLAIN, 16));
        String timeStr = "Time played: " + displayTime / 1000 + " seconds";
        FontMetrics fm2 = g2d.getFontMetrics();
        g2d.drawString(timeStr, (MainClass.SCREEN_WIDTH - fm2.stringWidth(timeStr)) / 2, y + 20);
    }
}
