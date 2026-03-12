import java.awt.*;
import java.util.Random;

/**
 * Background class : Renders a procedural animated gradient background with drifting
 * semi-transparent cell-like blobs. Colors shift slowly over time for a soothing feel.
 * Replaces the former static tiled image background.
 * @author Kamil Yunus Özkaya
 */
public class Background
{
    private static final int NUM_BLOBS = 12;

    private final float[] blobX;
    private final float[] blobY;
    private final float[] blobVX;
    private final float[] blobVY;
    private final float[] blobSize;
    private final float[] blobHue;
    private final float[] blobAlpha;

    private float phase = 0f;

    private final int worldW = MainClass.WORLD_WIDTH;
    private final int worldH = MainClass.WORLD_HEIGHT;

    public Background(int screenWidth, int screenHeight)
    {
        blobX     = new float[NUM_BLOBS];
        blobY     = new float[NUM_BLOBS];
        blobVX    = new float[NUM_BLOBS];
        blobVY    = new float[NUM_BLOBS];
        blobSize  = new float[NUM_BLOBS];
        blobHue   = new float[NUM_BLOBS];
        blobAlpha = new float[NUM_BLOBS];

        Random rng = new Random(42);
        for (int i = 0; i < NUM_BLOBS; i++) {
            blobX[i]     = rng.nextFloat() * worldW;
            blobY[i]     = rng.nextFloat() * worldH;
            blobVX[i]    = (rng.nextFloat() - 0.5f) * 0.8f;
            blobVY[i]    = (rng.nextFloat() - 0.5f) * 0.8f;
            blobSize[i]  = 150f + rng.nextFloat() * 350f;
            blobHue[i]   = rng.nextFloat();
            blobAlpha[i] = 0.06f + rng.nextFloat() * 0.08f;
        }
    }

    /**
     * drawBackground : Draws a slowly shifting pastel gradient and 12 drifting translucent
     * cell-like blobs across the visible viewport. Call this while the Graphics2D transform
     * is shifted by -cameraX, -cameraY (and optionally scaled for zoom) so rendering is in world space.
     *
     * @param g            the Graphics2D context (already translated by -cameraX, -cameraY)
     * @param cameraX      current horizontal camera offset in world coordinates
     * @param cameraY      current vertical camera offset in world coordinates
     * @param visibleWidth  visible world width (SCREEN_WIDTH / zoom)
     * @param visibleHeight visible world height (SCREEN_HEIGHT / zoom)
     */
    public void drawBackground(Graphics2D g, int cameraX, int cameraY, int visibleWidth, int visibleHeight)
    {
        // Advance overall color phase (full cycle ~500 seconds at 100 FPS)
        phase = (phase + 0.002f) % 1f;

        // Base gradient spanning the visible viewport
        Color c1 = Color.getHSBColor(phase,                0.25f, 0.97f);
        Color c2 = Color.getHSBColor((phase + 0.3f) % 1f,  0.20f, 0.90f);
        g.setPaint(new GradientPaint(
                cameraX,                          cameraY,                            c1,
                cameraX + visibleWidth, cameraY + visibleHeight, c2));
        g.fillRect(cameraX, cameraY, visibleWidth, visibleHeight);

        // Drifting blob cells
        Composite origComposite = g.getComposite();
        for (int i = 0; i < NUM_BLOBS; i++) {
            // Advance blob positions with toroidal wrap
            blobX[i] = (blobX[i] + blobVX[i] + worldW) % worldW;
            blobY[i] = (blobY[i] + blobVY[i] + worldH) % worldH;
            blobHue[i] = (blobHue[i] + 0.0003f) % 1f;

            float s = blobSize[i];

            // Skip blobs entirely outside the current viewport (with margin)
            if (blobX[i] + s < cameraX || blobX[i] - s > cameraX + visibleWidth)  continue;
            if (blobY[i] + s < cameraY || blobY[i] - s > cameraY + visibleHeight) continue;

            // Radial gradient: blob center color fading to transparent at edges
            Color blobCenter = Color.getHSBColor(blobHue[i], 0.4f, 0.95f);
            float[] fractions = { 0f, 1f };
            Color[] blobColors = {
                new Color(blobCenter.getRed(), blobCenter.getGreen(),
                          blobCenter.getBlue(), (int)(blobAlpha[i] * 255)),
                new Color(0, 0, 0, 0)
            };
            RadialGradientPaint rgp = new RadialGradientPaint(blobX[i], blobY[i], s, fractions, blobColors);
            g.setPaint(rgp);
            g.setComposite(AlphaComposite.SrcOver);
            g.fillOval((int)(blobX[i] - s), (int)(blobY[i] - s), (int)(s * 2), (int)(s * 2));
        }

        g.setComposite(origComposite);
        g.setPaint(null);
    }
}
