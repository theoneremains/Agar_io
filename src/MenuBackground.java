import java.awt.*;
import javax.swing.*;

/**
 * MenuBackground : Reusable animated gradient background with floating circles.
 * Used by MainPanel, OptionsPanel, and WorldSettingsPanel for consistent menu aesthetics.
 * Manages its own animation timer and phase progression.
 * @author Kamil Yunus Ozkaya
 */
public class MenuBackground {

    private float phase = 0f;
    private Timer animTimer;
    private final int circleCount;

    /**
     * Creates a menu background with the specified number of floating circles.
     * @param circleCount number of decorative floating circles
     * @param repaintTarget the component to repaint each animation frame
     */
    public MenuBackground(int circleCount, JComponent repaintTarget) {
        this.circleCount = circleCount;
        this.animTimer = new Timer(30, e -> {
            phase = (phase + 0.003f) % 1f;
            repaintTarget.repaint();
        });
    }

    /** Starts the background animation timer */
    public void start() {
        animTimer.start();
    }

    /** Stops the background animation timer */
    public void stop() {
        if (animTimer != null) animTimer.stop();
    }

    /** Returns the current animation phase (0–1) */
    public float getPhase() {
        return phase;
    }

    /**
     * Draws the animated gradient background and floating circles.
     * Call this from paintComponent().
     * @param g2d the Graphics2D context
     * @param width the panel width
     * @param height the panel height
     */
    public void draw(Graphics2D g2d, int width, int height) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Animated gradient background
        Color c1 = Color.getHSBColor(phase, 0.35f, 0.18f);
        Color c2 = Color.getHSBColor((phase + 0.25f) % 1f, 0.30f, 0.12f);
        g2d.setPaint(new GradientPaint(0, 0, c1, width, height, c2));
        g2d.fillRect(0, 0, width, height);

        // Subtle floating circles for depth
        Composite orig = g2d.getComposite();
        for (int i = 0; i < circleCount; i++) {
            float hue = (phase + i * (1.0f / Math.max(1, circleCount))) % 1f;
            Color circleColor = Color.getHSBColor(hue, 0.3f, 0.22f);
            float circleAlpha = 0.05f + 0.03f * (float) Math.sin(phase * Math.PI * 2 + i);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, circleAlpha)));
            g2d.setColor(circleColor);
            int cx = (int) (width * 0.15 + (width * 0.7) * Math.sin(phase * Math.PI + i * 0.9));
            int cy = (int) (height * 0.15 + (height * 0.7) * Math.cos(phase * Math.PI * 0.7 + i * 1.1));
            int sz = 80 + i * 45;
            g2d.fillOval(cx - sz, cy - sz, sz * 2, sz * 2);
        }
        g2d.setComposite(orig);
    }

    /**
     * Draws the high score text in the top-left corner.
     * @param g2d the Graphics2D context
     * @param highscore the current high score value
     */
    public static void drawHighScore(Graphics2D g2d, int highscore) {
        g2d.setColor(new Color(180, 200, 255, 180));
        g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 14));
        g2d.drawString("High Score: " + highscore, 15, 25);
    }

    /**
     * Draws a centered title with optional glow effect.
     * @param g2d the Graphics2D context
     * @param title the title text
     * @param fontSize the font size
     * @param y the vertical position
     * @param width the panel width for centering
     * @param glow whether to add glow effect
     */
    public static void drawTitle(Graphics2D g2d, String title, int fontSize, int y, int width, boolean glow) {
        g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, fontSize));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = (width - fm.stringWidth(title)) / 2;

        if (glow) {
            g2d.setColor(new Color(100, 180, 255, 50));
            g2d.drawString(title, tx - 2, y + 2);
            g2d.drawString(title, tx + 2, y - 2);
        }
        g2d.setColor(new Color(220, 240, 255));
        g2d.drawString(title, tx, y);
    }
}
