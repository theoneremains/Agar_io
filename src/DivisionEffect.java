
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * DivisionEffect : Renders a smooth curvy animation when a cell divides into two halves.
 * The two halves follow quadratic bezier curves moving apart perpendicular to the contact
 * direction, with a squish/stretch effect for an organic feel.
 * @author Kamil Yunus Ozkaya
 */
public class DivisionEffect {
    private final double originX, originY;
    private final double targetAX, targetAY, targetBX, targetBY;
    private final double newRadius;
    private final Color color;
    private final double divisionAngle;
    private final double curveOffset;

    private int tick = 0;
    private static final int ANIM_TICKS = 30; // 300ms at 10ms/tick
    public boolean finished = false;

    /**
     * Creates a division animation effect.
     * @param originCX  center X of the original cell
     * @param originCY  center Y of the original cell
     * @param targetACX center X of the first half's final position
     * @param targetACY center Y of the first half's final position
     * @param targetBCX center X of the second half's final position
     * @param targetBCY center Y of the second half's final position
     * @param newRadius radius of each new half cell (originalRad / sqrt(2))
     * @param color     color of the cells
     * @param divisionAngle the perpendicular angle along which halves separate
     */
    public DivisionEffect(double originCX, double originCY,
                           double targetACX, double targetACY,
                           double targetBCX, double targetBCY,
                           double newRadius, Color color, double divisionAngle) {
        this.originX = originCX;
        this.originY = originCY;
        this.targetAX = targetACX;
        this.targetAY = targetACY;
        this.targetBX = targetBCX;
        this.targetBY = targetBCY;
        this.newRadius = newRadius;
        this.color = color;
        this.divisionAngle = divisionAngle;
        this.curveOffset = newRadius * 1.2;
    }

    /** Advances the animation by one tick */
    public void update() {
        if (finished) return;
        tick++;
        if (tick >= ANIM_TICKS) finished = true;
    }

    /**
     * Draws the division animation with curvy bezier motion and squish effect.
     * @param g2d the Graphics2D context (in world-space coordinates)
     */
    public void draw(Graphics2D g2d) {
        // Smooth ease-in-out progress
        double t = 0.5 - 0.5 * Math.cos(Math.PI * tick / (double) ANIM_TICKS);

        // Perpendicular angle for bezier control point offset
        double perpAngle = divisionAngle + Math.PI / 2;

        // Half A: quadratic bezier from origin to targetA with curved control point
        double ctrlAX = (originX + targetAX) / 2 + Math.cos(perpAngle) * curveOffset * (1 - t);
        double ctrlAY = (originY + targetAY) / 2 + Math.sin(perpAngle) * curveOffset * (1 - t);
        double posAX = (1 - t) * (1 - t) * originX + 2 * (1 - t) * t * ctrlAX + t * t * targetAX;
        double posAY = (1 - t) * (1 - t) * originY + 2 * (1 - t) * t * ctrlAY + t * t * targetAY;

        // Half B: quadratic bezier from origin to targetB with opposite curve
        double ctrlBX = (originX + targetBX) / 2 - Math.cos(perpAngle) * curveOffset * (1 - t);
        double ctrlBY = (originY + targetBY) / 2 - Math.sin(perpAngle) * curveOffset * (1 - t);
        double posBX = (1 - t) * (1 - t) * originX + 2 * (1 - t) * t * ctrlBX + t * t * targetBX;
        double posBY = (1 - t) * (1 - t) * originY + 2 * (1 - t) * t * ctrlBY + t * t * targetBY;

        // Squish effect: cells elongate perpendicular to movement at midpoint
        double squish = 1.0 + 0.35 * Math.sin(Math.PI * t);
        double drawRadX = newRadius * squish;
        double drawRadY = newRadius / squish;

        // Alpha: start slightly transparent, become fully opaque
        float alpha = Math.min(1f, 0.4f + 0.6f * (float) t);
        Composite orig = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setColor(color);

        // Draw half A with rotation for squish alignment
        AffineTransform saved = g2d.getTransform();
        g2d.translate(posAX, posAY);
        g2d.rotate(divisionAngle);
        g2d.fillOval((int) (-drawRadX), (int) (-drawRadY), (int) (drawRadX * 2), (int) (drawRadY * 2));
        g2d.setTransform(saved);

        // Draw half B with rotation
        saved = g2d.getTransform();
        g2d.translate(posBX, posBY);
        g2d.rotate(divisionAngle);
        g2d.fillOval((int) (-drawRadX), (int) (-drawRadY), (int) (drawRadX * 2), (int) (drawRadY * 2));
        g2d.setTransform(saved);

        g2d.setComposite(orig);
    }
}
