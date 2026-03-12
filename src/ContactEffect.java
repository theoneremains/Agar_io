
import java.awt.*;
import java.awt.geom.QuadCurve2D;

/**
 * ContactEffect : Renders smooth curvy ripple lines at the point of contact
 * between two cells. Used for both division contact buildup and
 * bounce/rejection when a cell touches one it cannot eat.
 * @author Kamil Yunus Ozkaya
 */
public class ContactEffect {
    private final double cx, cy;
    private final double radius;
    private final Color color;
    private final boolean isDivisionContact;

    private int tick = 0;
    private final int maxTicks;
    public boolean finished = false;

    /**
     * Creates a contact effect at the point where two cells touch.
     * @param cx contact point X (world coords)
     * @param cy contact point Y (world coords)
     * @param radius approximate size of the effect
     * @param color color of the effect arcs
     * @param isDivisionContact true for division buildup (longer, pulsing), false for bounce (short)
     */
    public ContactEffect(double cx, double cy, double radius, Color color, boolean isDivisionContact) {
        this.cx = cx;
        this.cy = cy;
        this.radius = Math.max(3, radius);
        this.color = color;
        this.isDivisionContact = isDivisionContact;
        this.maxTicks = isDivisionContact ? 40 : 20;
    }

    /** Advances the animation by one tick */
    public void update() {
        if (finished) return;
        tick++;
        if (tick >= maxTicks) finished = true;
    }

    /**
     * Draws curvy arc ripples at the contact point.
     * @param g2d the Graphics2D context (in world-space coordinates)
     */
    public void draw(Graphics2D g2d) {
        double t = (double) tick / maxTicks;
        float alpha = Math.max(0f, 1f - (float) (t * t));
        if (alpha <= 0.01f) return;

        Composite orig = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        Stroke origStroke = g2d.getStroke();
        float strokeW = isDivisionContact ? 2.5f : 1.5f;
        g2d.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int numArcs = isDivisionContact ? 4 : 3;
        double spread = radius * (1.0 + t * 1.5);

        for (int i = 0; i < numArcs; i++) {
            double angle = (2 * Math.PI * i / numArcs) + t * Math.PI * 0.5;
            double x1 = cx + Math.cos(angle - 0.5) * spread;
            double y1 = cy + Math.sin(angle - 0.5) * spread;
            double x2 = cx + Math.cos(angle + 0.5) * spread;
            double y2 = cy + Math.sin(angle + 0.5) * spread;
            double ctrlX = cx + Math.cos(angle) * spread * (isDivisionContact ? 1.8 : 1.4);
            double ctrlY = cy + Math.sin(angle) * spread * (isDivisionContact ? 1.8 : 1.4);

            if (isDivisionContact) {
                // Pulsing color for division
                float pulse = 0.5f + 0.5f * (float) Math.sin(tick * 0.5);
                g2d.setColor(new Color(
                    Math.min(255, color.getRed() + (int) (60 * pulse)),
                    Math.min(255, color.getGreen() + (int) (60 * pulse)),
                    Math.min(255, color.getBlue() + (int) (30 * pulse))));
            } else {
                g2d.setColor(color);
            }

            QuadCurve2D.Double curve = new QuadCurve2D.Double(x1, y1, ctrlX, ctrlY, x2, y2);
            g2d.draw(curve);
        }

        g2d.setStroke(origStroke);
        g2d.setComposite(orig);
    }
}
