import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Background class : Class for your background
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
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
    public void drawBackground(Graphics2D g)
    {
        float opacity = 1f;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g.drawImage(backgroundImg, 0, 0, null);

    }
}
