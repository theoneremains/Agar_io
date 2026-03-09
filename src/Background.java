import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Background class : Renders the background image tiled across the visible viewport
 * @author Kamil Yunus Özkaya
 */
public class Background
{
    public BufferedImage backgroundImg;
    public int screenWidth;
    public int screenHeight;

    public Background(int screenWidth, int screenHeight)
    {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        try
        {
            backgroundImg = ImageIO.read(this.getClass().getResource("background.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * drawBackground : Tiles the background image to fill the visible viewport.
     * Call this while the Graphics2D transform is shifted by -cameraX, -cameraY
     * so tiles are drawn in world space and align with the camera position.
     * @param g       the Graphics2D context (already translated by -cameraX, -cameraY)
     * @param cameraX current horizontal camera offset in world coordinates
     * @param cameraY current vertical camera offset in world coordinates
     */
    public void drawBackground(Graphics2D g, int cameraX, int cameraY)
    {
        if (backgroundImg == null) return;
        int imgW = backgroundImg.getWidth();
        int imgH = backgroundImg.getHeight();
        // Find the first tile origin that is at or before the left/top camera edge
        int startX = (cameraX / imgW) * imgW;
        int startY = (cameraY / imgH) * imgH;
        // Draw tiles until they cover the full visible viewport
        for (int tx = startX; tx < cameraX + screenWidth + imgW; tx += imgW)
        {
            for (int ty = startY; ty < cameraY + screenHeight + imgH; ty += imgH)
            {
                g.drawImage(backgroundImg, tx, ty, null);
            }
        }
    }
}
