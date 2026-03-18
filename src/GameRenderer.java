import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.awt.BasicStroke;

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
            if (npc.upgradeCount > 0) {
                drawNPCUpgradeStar(g2d, npc, npcDrawRad);
            }
        }
    }

    /**
     * Draws a small gold star badge with upgrade count on upgraded NPCs.
     * Positioned at the top-right of the cell.
     */
    private void drawNPCUpgradeStar(Graphics2D g2d, NPC npc, int drawRad) {
        int badgeX = (int) Math.round(npc.cell.getCenterX()) + drawRad - 6;
        int badgeY = (int) Math.round(npc.cell.getCenterY()) - drawRad + 6;
        g2d.setColor(new Color(255, 215, 0, 220));
        g2d.fillOval(badgeX - 6, badgeY - 6, 12, 12);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 8));
        FontMetrics fm = g2d.getFontMetrics();
        String label = "\u2605" + npc.upgradeCount; // ★N
        g2d.drawString(label, badgeX - fm.stringWidth(label) / 2, badgeY + fm.getAscent() / 2 - 1);
    }

    private void drawPlayer(Graphics2D g2d) {
        if (game.isGameOver()) return;

        Cell player = game.getPlayerCell();
        int playerDrawRad = (int) Math.round(player.cellRad);

        // Magnet aura — dashed circle pulsing at the pull radius
        if (game.magnetLevel > 0) {
            drawMagnetAura(g2d, player);
        }

        player.drawCell(g2d, playerDrawRad);
        drawCellName(g2d, player, GamePanel.playerName, playerDrawRad);
    }

    /**
     * Draws a pulsing dashed circle at the player's magnet radius (world space).
     */
    private void drawMagnetAura(Graphics2D g2d, Cell player) {
        double pulseAlpha = 0.18 + 0.10 * Math.sin(System.currentTimeMillis() / 350.0);
        int alpha = Math.max(10, Math.min(255, (int) (pulseAlpha * 255)));
        g2d.setColor(new Color(80, 200, 255, alpha));
        Stroke saved = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
            0, new float[]{10, 8}, (float) (System.currentTimeMillis() / 80.0 % 18)));
        int mr = (int) game.magnetRadius;
        int cx = (int) Math.round(player.getCenterX());
        int cy = (int) Math.round(player.getCenterY());
        g2d.drawOval(cx - mr, cy - mr, mr * 2, mr * 2);
        g2d.setStroke(saved);
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

        // Active upgrade indicators (bottom-left)
        int indicatorX = 10;
        if (game.getUpgradeManager().hasDodge()) {
            drawDodgeIndicator(g2d, indicatorX);
            indicatorX += 50;  // circle diameter (36) + gap
        }
        if (game.magnetLevel > 0) {
            drawSimpleIndicator(g2d, indicatorX, "\u25CE Magnet x" + game.magnetLevel,
                new Color(80, 200, 255));
            indicatorX += 130;
        }
        if (game.regenLevel > 0) {
            drawSimpleIndicator(g2d, indicatorX, "\u2665 Regen x" + game.regenLevel,
                new Color(100, 220, 130));
        }

        // Upgrade history panel (top-center area, below score)
        drawPlayerUpgradeHistory(g2d);
    }

    /**
     * Draws a compact panel listing all upgrades the player has taken so far,
     * shown below the score line on the left side of the screen.
     */
    private void drawPlayerUpgradeHistory(Graphics2D g2d) {
        java.util.Map<UpgradeType, Integer> counts = game.getUpgradeManager().getAppliedCounts();
        if (counts.isEmpty()) return;

        int x = 10;
        int y = 38;
        int badgeH = 16;
        int badgePad = 4;
        int badgeGap = 4;

        g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 11));

        for (java.util.Map.Entry<UpgradeType, Integer> entry : counts.entrySet()) {
            UpgradeType type = entry.getKey();
            int count = entry.getValue();

            String label = upgradeShortName(type) + " Lv." + count;
            FontMetrics fm = g2d.getFontMetrics();
            int badgeW = fm.stringWidth(label) + badgePad * 2;

            // Badge background
            g2d.setColor(new Color(20, 30, 50, 190));
            g2d.fillRoundRect(x, y, badgeW, badgeH, 6, 6);
            // Badge border
            g2d.setColor(upgradeColor(type));
            g2d.drawRoundRect(x, y, badgeW, badgeH, 6, 6);
            // Badge text
            g2d.setColor(Color.WHITE);
            g2d.drawString(label, x + badgePad, y + fm.getAscent() + 1);

            x += badgeW + badgeGap;
            // Wrap to next line if near screen edge
            if (x > MainClass.SCREEN_WIDTH / 2) {
                x = 10;
                y += badgeH + badgeGap;
            }
        }
    }

    /** Returns a short display name for each upgrade type suitable for HUD badges. */
    private String upgradeShortName(UpgradeType type) {
        switch (type) {
            case SPEED_BOOST:    return "\u26A1 Speed";
            case SIZE_BOOST:     return "\u25CF Size";
            case REGENERATION:   return "\u2665 Regen";
            case SPLIT_SHIELD:   return "\u25A3 Shield";
            case DENSITY_BOOST:  return "\u2726 Density";
            case DIVERGENCY_BOOST: return "\u2665 Big Feast";
            case MAGNET:         return "\u25CE Magnet";
            case DODGE:          return "\u21E8 Dodge";
            default:             return type.displayName;
        }
    }

    /** Returns a theme color for each upgrade type for badge borders. */
    private Color upgradeColor(UpgradeType type) {
        switch (type) {
            case SPEED_BOOST:    return new Color(255, 220, 50);
            case SIZE_BOOST:     return new Color(100, 200, 100);
            case REGENERATION:   return new Color(100, 220, 130);
            case SPLIT_SHIELD:   return new Color(150, 150, 255);
            case DENSITY_BOOST:  return new Color(255, 180, 80);
            case DIVERGENCY_BOOST: return new Color(255, 120, 80);
            case MAGNET:         return new Color(80, 200, 255);
            case DODGE:          return new Color(220, 80, 220);
            default:             return Color.WHITE;
        }
    }

    /**
     * Draws the DODGE status indicator as a clock-like circular arc.
     * A filled arc sweeps clockwise from the top as the cooldown expires.
     * When ready the circle glows green; during cooldown it fills with blue.
     * Returns the pixel width consumed so callers can advance indicatorX.
     */
    private void drawDodgeIndicator(Graphics2D g2d, int x) {
        final int r  = 18;  // circle radius
        final int cx = x + r;
        final int cy = MainClass.SCREEN_HEIGHT - 45;

        int     cooldown = game.getDodgeCooldownTicks();
        boolean ready    = cooldown <= 0;

        // Dark background circle
        g2d.setColor(new Color(30, 30, 30, 210));
        g2d.fillOval(cx - r, cy - r, r * 2, r * 2);

        // Filled arc — sweeps clockwise from 12-o'clock as cooldown recovers
        double fraction = ready ? 1.0
                                : 1.0 - (double) cooldown / GameConstants.DODGE_COOLDOWN_TICKS;
        int arcAngle = (int) Math.round(360 * fraction);
        g2d.setColor(ready ? new Color(80, 230, 80) : new Color(80, 140, 200));
        g2d.fillArc(cx - r, cy - r, r * 2, r * 2, 90, -arcAngle);

        // Circle border
        g2d.setColor(ready ? new Color(80, 230, 80, 200) : new Color(130, 130, 160, 180));
        g2d.drawOval(cx - r, cy - r, r * 2, r * 2);

        // Icon glyph centred inside the circle
        g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 13));
        g2d.setColor(Color.WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        String icon = "\u21E8"; // ⇨
        g2d.drawString(icon, cx - fm.stringWidth(icon) / 2, cy + fm.getAscent() / 2 - 1);

        // "DODGE" label below the circle
        g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 10));
        g2d.setColor(ready ? new Color(80, 230, 80) : new Color(160, 160, 160));
        FontMetrics lfm = g2d.getFontMetrics();
        g2d.drawString("DODGE", cx - lfm.stringWidth("DODGE") / 2, cy + r + 13);

        // "SPACE" hint shown only when ready
        if (ready) {
            g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 9));
            g2d.setColor(new Color(80, 230, 80, 200));
            FontMetrics sfm = g2d.getFontMetrics();
            g2d.drawString("SPACE", cx - sfm.stringWidth("SPACE") / 2, cy + r + 24);
        }
    }

    /** Draws a simple colored label in the bottom-left HUD area at the given x offset. */
    private void drawSimpleIndicator(Graphics2D g2d, int x, String label, Color color) {
        int y = MainClass.SCREEN_HEIGHT - 40;
        g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 13));
        g2d.setColor(color);
        g2d.drawString(label, x, y);
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
        java.util.List<UpgradeOffer> choices = game.getUpgradeManager().getCurrentChoices();
        int n = choices.size();
        int totalW = n * GameConstants.UPGRADE_CARD_WIDTH + (n - 1) * GameConstants.UPGRADE_CARD_GAP;
        int startX = (MainClass.SCREEN_WIDTH - totalW) / 2;
        int cardY  = MainClass.SCREEN_HEIGHT / 2 - 80;

        for (int i = 0; i < n; i++) {
            UpgradeOffer offer = choices.get(i);
            int cardX = startX + i * (GameConstants.UPGRADE_CARD_WIDTH + GameConstants.UPGRADE_CARD_GAP);

            String rarityTag = offer.getRarityTag();

            // Card background
            g2d.setColor(new Color(30, 45, 70, 230));
            g2d.fillRoundRect(cardX, cardY, GameConstants.UPGRADE_CARD_WIDTH,
                GameConstants.UPGRADE_CARD_HEIGHT, 16, 16);

            // Card border — coloured by rarity
            g2d.setColor(rarityBorderColor(rarityTag));
            g2d.drawRoundRect(cardX, cardY, GameConstants.UPGRADE_CARD_WIDTH,
                GameConstants.UPGRADE_CARD_HEIGHT, 16, 16);

            int textY = cardY + 20;

            // Rarity tag (if present) at top of card
            if (!rarityTag.isEmpty()) {
                g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 11));
                g2d.setColor(rarityTextColor(rarityTag));
                FontMetrics rfm = g2d.getFontMetrics();
                g2d.drawString(rarityTag,
                    cardX + (GameConstants.UPGRADE_CARD_WIDTH - rfm.stringWidth(rarityTag)) / 2,
                    textY);
                textY += 16;
            }

            // Description text (word-wrapped)
            g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 13));
            g2d.setColor(new Color(200, 215, 240));
            drawWrappedText(g2d, offer.description,
                cardX + 10, textY,
                GameConstants.UPGRADE_CARD_WIDTH - 20, 17);
        }
    }

    /** Returns the card border color based on the rarity tag string. */
    private Color rarityBorderColor(String tag) {
        if (tag.contains("LEGENDARY")) return new Color(255, 160, 30, 220);
        if (tag.contains("EPIC"))      return new Color(180, 80, 220, 210);
        if (tag.contains("RARE"))      return new Color(80, 160, 230, 200);
        return new Color(90, 130, 210, 180);
    }

    /** Returns the rarity label text color. */
    private Color rarityTextColor(String tag) {
        if (tag.contains("LEGENDARY")) return new Color(255, 200, 60);
        if (tag.contains("EPIC"))      return new Color(210, 100, 255);
        if (tag.contains("RARE"))      return new Color(100, 180, 255);
        return Color.WHITE;
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
        boolean isVictory = game.isVictory();

        // Dark overlay (gold tint for victory, dark for loss)
        g2d.setColor(isVictory ? new Color(40, 30, 0, 200) : new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);

        // Title: VICTORY or GAME OVER
        String title = isVictory ? "VICTORY!" : "GAME OVER";
        g2d.setColor(isVictory ? new Color(255, 220, 0) : Color.RED);
        g2d.setFont(new Font(GameConstants.FONT_FAMILY_MONO, Font.BOLD, 64));
        FontMetrics tfm = g2d.getFontMetrics();
        int tx = (MainClass.SCREEN_WIDTH - tfm.stringWidth(title)) / 2;
        g2d.drawString(title, tx, 120);

        // Victory sub-message
        if (isVictory) {
            g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 20));
            g2d.setColor(new Color(255, 240, 180));
            String sub = "You eliminated all opponents!";
            FontMetrics sfm2 = g2d.getFontMetrics();
            g2d.drawString(sub, (MainClass.SCREEN_WIDTH - sfm2.stringWidth(sub)) / 2, 155);
        }

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
