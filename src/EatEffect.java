
import java.awt.*;

/**
 * EatEffect : Renders a smooth particle burst animation when a cell is eaten.
 * Small colored particles radiate outward from the eat location with fade-out.
 * @author Kamil Yunus Ozkaya
 */
public class EatEffect {
    private final double cx, cy;
    private final Color color;
    private final int numParticles;
    private final double[] angles;
    private final double[] speeds;
    private final double[] sizes;

    private int tick = 0;
    private static final int ANIM_TICKS = 25; // 250ms at 10ms/tick
    public boolean finished = false;

    /**
     * Creates an eat effect at the given world position.
     * @param cx center X where the cell was eaten
     * @param cy center Y where the cell was eaten
     * @param radius radius of the eaten cell (determines effect size)
     * @param color color of the eaten cell
     */
    public EatEffect(double cx, double cy, double radius, Color color) {
        this.cx = cx;
        this.cy = cy;
        this.color = color;
        this.numParticles = Math.max(6, Math.min(16, (int) (radius * 2)));
        this.angles = new double[numParticles];
        this.speeds = new double[numParticles];
        this.sizes = new double[numParticles];
        java.util.Random rng = new java.util.Random();
        for (int i = 0; i < numParticles; i++) {
            angles[i] = rng.nextDouble() * 2 * Math.PI;
            speeds[i] = 1.5 + rng.nextDouble() * 3.0;
            sizes[i] = Math.max(1.5, radius * 0.2 + rng.nextDouble() * radius * 0.3);
        }
    }

    /** Advances the animation by one tick */
    public void update() {
        if (finished) return;
        tick++;
        if (tick >= ANIM_TICKS) finished = true;
    }

    /**
     * Draws the eat particle burst.
     * @param g2d the Graphics2D context (in world-space coordinates)
     */
    public void draw(Graphics2D g2d) {
        double t = (double) tick / ANIM_TICKS;
        float alpha = Math.max(0f, 1f - (float) (t * t));
        if (alpha <= 0.01f) return;

        Composite orig = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        for (int i = 0; i < numParticles; i++) {
            double dist = speeds[i] * tick;
            double px = cx + Math.cos(angles[i]) * dist;
            double py = cy + Math.sin(angles[i]) * dist;
            double sz = sizes[i] * (1.0 - t * 0.5);
            if (sz < 0.5) continue;
            g2d.setColor(color);
            g2d.fillOval((int) (px - sz), (int) (py - sz), (int) (sz * 2), (int) (sz * 2));
        }

        g2d.setComposite(orig);
    }
}
