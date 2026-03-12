
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * StyledButton : A modern, animated button with rounded corners, gradient fill,
 * hover glow, press feedback, and smooth color transitions.
 * Replaces plain JButtons throughout the game for a polished look.
 * @author Kamil Yunus Ozkaya
 */
public class StyledButton extends JButton {

    private Color baseColor;
    private Color hoverColor;
    private Color pressColor;
    private Color currentBg;
    private Color textColor = Color.WHITE;

    private float hoverAlpha = 0f;
    private boolean hovering = false;
    private boolean pressing = false;

    private static final int ARC = 18;
    private static final int ANIM_MS = 12;

    private Timer animTimer;

    /**
     * Creates a styled button with the given text and base color.
     * @param text button label
     * @param base the resting background color
     */
    public StyledButton(String text, Color base) {
        super(text);
        this.baseColor = base;
        this.hoverColor = brighter(base, 40);
        this.pressColor = darker(base, 30);
        this.currentBg = base;

        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setFont(new Font("SansSerif", Font.BOLD, 16));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        animTimer = new Timer(ANIM_MS, e -> {
            float target = hovering ? 1f : 0f;
            float step = 0.12f;
            if (Math.abs(hoverAlpha - target) < step) {
                hoverAlpha = target;
                ((Timer) e.getSource()).stop();
            } else {
                hoverAlpha += (target > hoverAlpha) ? step : -step;
            }
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                if (!animTimer.isRunning()) animTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                pressing = false;
                if (!animTimer.isRunning()) animTimer.start();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressing = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressing = false;
                repaint();
            }
        });
    }

    /** Convenience constructor with default dark color */
    public StyledButton(String text) {
        this(text, new Color(50, 60, 80));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Compute interpolated background
        Color bg;
        if (pressing) {
            bg = pressColor;
        } else {
            bg = interpolate(baseColor, hoverColor, hoverAlpha);
        }

        // Soft drop shadow
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fill(new RoundRectangle2D.Float(2, 3, w - 4, h - 2, ARC, ARC));

        // Main body gradient
        GradientPaint gp = new GradientPaint(0, 0, brighter(bg, 15), 0, h, darker(bg, 15));
        g2.setPaint(gp);
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 2, ARC, ARC));

        // Subtle top highlight
        g2.setColor(new Color(255, 255, 255, (int) (30 + 25 * hoverAlpha)));
        g2.fill(new RoundRectangle2D.Float(1, 1, w - 3, h / 2f - 2, ARC, ARC));

        // Hover glow border
        if (hoverAlpha > 0.01f) {
            g2.setColor(new Color(200, 220, 255, (int) (80 * hoverAlpha)));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 4, ARC, ARC));
        }

        // Text
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(getText());
        int textH = fm.getAscent();
        int tx = (w - textW) / 2;
        int ty = (h - fm.getHeight()) / 2 + textH;

        // Text shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawString(getText(), tx + 1, ty + 1);

        // Text
        g2.setColor(textColor);
        g2.drawString(getText(), tx, ty);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = fm.stringWidth(getText()) + 40;
        int h = fm.getHeight() + 20;
        return new Dimension(Math.max(w, MainClass.BUTTON_WIDTH), Math.max(h, MainClass.BUTTON_HEIGHT));
    }

    /** Sets the base color scheme */
    public void setBaseColor(Color c) {
        this.baseColor = c;
        this.hoverColor = brighter(c, 40);
        this.pressColor = darker(c, 30);
        repaint();
    }

    public void setTextColor(Color c) {
        this.textColor = c;
        repaint();
    }

    private static Color brighter(Color c, int amount) {
        return new Color(
            Math.min(255, c.getRed() + amount),
            Math.min(255, c.getGreen() + amount),
            Math.min(255, c.getBlue() + amount));
    }

    private static Color darker(Color c, int amount) {
        return new Color(
            Math.max(0, c.getRed() - amount),
            Math.max(0, c.getGreen() - amount),
            Math.max(0, c.getBlue() - amount));
    }

    private static Color interpolate(Color a, Color b, float t) {
        return new Color(
            (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
