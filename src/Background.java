import java.awt.*;
import java.util.Random;

/**
 * Background class : Renders a procedural animated gradient background with drifting
 * semi-transparent cell-like blobs. Colors shift slowly over time for a soothing feel.
 * Reads world dimensions dynamically from MainClass to support configurable world sizes.
 * @author Kamil Yunus Özkaya
 */
public class Background {

    private static final int NUM_BLOBS = 12;

    private final float[] blobX;
    private final float[] blobY;
    private final float[] blobVX;
    private final float[] blobVY;
    private final float[] blobSize;
    private final float[] blobHue;
    private final float[] blobAlpha;

    private float phase = 0f;

    public Background() {
        blobX     = new float[NUM_BLOBS];
        blobY     = new float[NUM_BLOBS];
        blobVX    = new float[NUM_BLOBS];
        blobVY    = new float[NUM_BLOBS];
        blobSize  = new float[NUM_BLOBS];
        blobHue   = new float[NUM_BLOBS];
        blobAlpha = new float[NUM_BLOBS];

        Random rng = new Random(42);
        int worldW = MainClass.WORLD_WIDTH;
        int worldH = MainClass.WORLD_HEIGHT;
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
     * Draws the animated gradient and drifting blobs across the visible viewport.
     * Call this while the Graphics2D transform is shifted by -cameraX, -cameraY.
     * Uses current MainClass.WORLD_WIDTH/HEIGHT for toroidal wrapping so it
     * adapts to world size changes between game sessions.
     *
     * <p>Blobs whose screen-space radius would exceed {@code MAX_SCREEN_BLOB_RAD}
     * are skipped.  At high zoom (e.g. 5×) a blob of world radius 300 would
     * become a 1500-px RadialGradientPaint oval — extremely expensive.  The
     * base gradient already covers the background at that scale.
     *
     * @param g            the Graphics2D context
     * @param cameraX      current horizontal camera offset
     * @param cameraY      current vertical camera offset
     * @param visibleWidth  visible world width
     * @param visibleHeight visible world height
     * @param zoom          current camera zoom factor
     */
    public void drawBackground(Graphics2D g, int cameraX, int cameraY,
                                int visibleWidth, int visibleHeight, double zoom) {
        int worldW = MainClass.WORLD_WIDTH;
        int worldH = MainClass.WORLD_HEIGHT;

        // Maximum screen-space radius before we skip drawing a blob.
        // A RadialGradientPaint oval larger than this is too expensive and
        // invisible at the zoom level anyway (the gradient center is off-screen).
        final float MAX_SCREEN_BLOB_RAD = 400f;

        phase = (phase + 0.002f) % 1f;

        // Base gradient
        Color c1 = Color.getHSBColor(phase,                0.25f, 0.97f);
        Color c2 = Color.getHSBColor((phase + 0.3f) % 1f,  0.20f, 0.90f);
        g.setPaint(new GradientPaint(
            cameraX, cameraY, c1,
            cameraX + visibleWidth, cameraY + visibleHeight, c2));
        g.fillRect(cameraX, cameraY, visibleWidth, visibleHeight);

        // Drifting blobs
        Composite origComposite = g.getComposite();
        for (int i = 0; i < NUM_BLOBS; i++) {
            blobX[i] = (blobX[i] + blobVX[i] + worldW) % worldW;
            blobY[i] = (blobY[i] + blobVY[i] + worldH) % worldH;
            blobHue[i] = (blobHue[i] + 0.0003f) % 1f;

            float s = blobSize[i];

            // Skip blobs that would be too large in screen space — their
            // RadialGradientPaint covers thousands of pixels and kills performance.
            if (s * zoom > MAX_SCREEN_BLOB_RAD) continue;

            // Skip blobs outside viewport
            if (blobX[i] + s < cameraX || blobX[i] - s > cameraX + visibleWidth) continue;
            if (blobY[i] + s < cameraY || blobY[i] - s > cameraY + visibleHeight) continue;

            Color blobCenter = Color.getHSBColor(blobHue[i], 0.4f, 0.95f);
            float[] fractions = {0f, 1f};
            Color[] blobColors = {
                new Color(blobCenter.getRed(), blobCenter.getGreen(),
                          blobCenter.getBlue(), (int) (blobAlpha[i] * 255)),
                new Color(0, 0, 0, 0)
            };
            RadialGradientPaint rgp = new RadialGradientPaint(blobX[i], blobY[i], s, fractions, blobColors);
            g.setPaint(rgp);
            g.setComposite(AlphaComposite.SrcOver);
            g.fillOval((int) (blobX[i] - s), (int) (blobY[i] - s), (int) (s * 2), (int) (s * 2));
        }

        g.setComposite(origComposite);
        g.setPaint(null);
    }
}
