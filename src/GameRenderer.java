import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

/**
 * GameRenderer : Handles all drawing/rendering for the game.
 * Extracted from GamePanel to separate rendering concerns from game logic.
 * Draws the world, cells, NPCs, player, effects, HUD, scoreboard, and overlays
 * (upgrade selection, pause, game over).
 * @author Kamil Yunus Ozkaya
 */
public class GameRenderer {

    private final GamePanel game;

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
        drawUpgradeOverlay(g2d);
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
        if (game.isGameOver()) return;

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

        // Dodge indicator (only shown once the player has the Dodge upgrade)
        if (game.getUpgradeManager().hasDodge()) {
            drawDodgeIndicator(g2d);
        }
    }

    /**
     * Draws a small DODGE status indicator in the bottom-left corner.
     * Shows "DODGE [READY]" in green or "DODGE [cooldown]" in gray with a
     * depleting cooldown bar.
     */
    private void drawDodgeIndicator(Graphics2D g2d) {
        int x = 10;
        int y = MainClass.SCREEN_HEIGHT - 40;
        int barW = 100;
        int barH = 8;

        int cooldown = game.getDodgeCooldownTicks();
        boolean ready = cooldown <= 0;

        // Label
        g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 13));
        g2d.setColor(ready ? new Color(80, 230, 80) : new Color(160, 160, 160));
        g2d.drawString("DODGE", x, y);

        // Cooldown bar background
        g2d.setColor(new Color(60, 60, 60, 180));
        g2d.fillRoundRect(x, y + 5, barW, barH, 4, 4);

        // Cooldown bar fill
        int filled = ready ? barW
                           : (int) (barW * (1.0 - (double) cooldown / GameConstants.DODGE_COOLDOWN_TICKS));
        g2d.setColor(ready ? new Color(80, 230, 80) : new Color(80, 140, 200));
        if (filled > 0) g2d.fillRoundRect(x, y + 5, filled, barH, 4, 4);

        // Ready label
        if (ready) {
            g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 11));
            g2d.setColor(new Color(80, 230, 80));
            g2d.drawString("SPACE", x + barW + 6, y + 13);
        }
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

    // ── Upgrade Selection Overlay ────────────────────────────────────────

    /**
     * Draws the upgrade selection overlay when the player has earned a level-up.
     * The actual clickable buttons are added as Swing components by GamePanel;
     * this method draws the visual background, title, and per-card descriptions.
     */
    private void drawUpgradeOverlay(Graphics2D g2d) {
        if (!game.upgradeSelecting) return;

        // Dark overlay
        g2d.setColor(new Color(0, 0, 0, 185));
        g2d.fillRect(0, 0, MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);

        // Title
        g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 36));
        g2d.setColor(new Color(255, 215, 0));
        String title = "LEVEL UP!  Choose Your Upgrade";
        FontMetrics tfm = g2d.getFontMetrics();
        g2d.drawString(title, (MainClass.SCREEN_WIDTH - tfm.stringWidth(title)) / 2,
            MainClass.SCREEN_HEIGHT / 2 - 115);

        // Subtitle hint
        g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 14));
        g2d.setColor(new Color(200, 200, 200));
        String hint = "Click a card to apply the upgrade and resume the game.";
        FontMetrics hfm = g2d.getFontMetrics();
        g2d.drawString(hint, (MainClass.SCREEN_WIDTH - hfm.stringWidth(hint)) / 2,
            MainClass.SCREEN_HEIGHT / 2 - 85);

        // Cards
        java.util.List<UpgradeType> choices = game.getUpgradeManager().getCurrentChoices();
        int n = choices.size();
        int totalW = n * GameConstants.UPGRADE_CARD_WIDTH + (n - 1) * GameConstants.UPGRADE_CARD_GAP;
        int startX = (MainClass.SCREEN_WIDTH - totalW) / 2;
        int cardY  = MainClass.SCREEN_HEIGHT / 2 - 80;

        for (int i = 0; i < n; i++) {
            UpgradeType type = choices.get(i);
            int cardX = startX + i * (GameConstants.UPGRADE_CARD_WIDTH + GameConstants.UPGRADE_CARD_GAP);

            // Card background
            g2d.setColor(new Color(30, 45, 70, 230));
            g2d.fillRoundRect(cardX, cardY, GameConstants.UPGRADE_CARD_WIDTH,
                GameConstants.UPGRADE_CARD_HEIGHT, 16, 16);
            // Card border
            g2d.setColor(new Color(90, 130, 210, 180));
            g2d.drawRoundRect(cardX, cardY, GameConstants.UPGRADE_CARD_WIDTH,
                GameConstants.UPGRADE_CARD_HEIGHT, 16, 16);

            // Description text (word-wrapped)
            g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 13));
            g2d.setColor(new Color(200, 215, 240));
            drawWrappedText(g2d, type.description,
                cardX + 10, cardY + 22,
                GameConstants.UPGRADE_CARD_WIDTH - 20, 18);
        }
    }

    /** Draws word-wrapped text within a given pixel width. */
    private void drawWrappedText(Graphics2D g2d, String text, int x, int y, int maxWidth, int lineHeight) {
        FontMetrics fm = g2d.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int curY = y;
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(test) > maxWidth && line.length() > 0) {
                g2d.drawString(line.toString(), x, curY);
                line = new StringBuilder(word);
                curY += lineHeight;
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) g2d.drawString(line.toString(), x, curY);
    }

    // ── Other Overlays ───────────────────────────────────────────────────

    private void drawPausedOverlay(Graphics2D g2d) {
        // Only show pause overlay for dev-log pause, not for upgrade selection
        if (!game.isPaused() || game.isGameOver() || game.upgradeSelecting) return;

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
