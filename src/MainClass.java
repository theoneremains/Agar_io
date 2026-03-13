import javax.swing.*;
import java.awt.*;

/**
 * MainClass class : Holds the width and height of the screen and the game world, creates the frame for the game
 * Has access to the panels, and all panels are connected to this frame
 * Supports fullscreen mode (default) with configurable world dimensions
 * Run the game through here
 * @author Kamil Yunus Özkaya
 */
public class MainClass extends JFrame
{
    public static int SCREEN_WIDTH  = 1280;
    public static int SCREEN_HEIGHT = 720;

    public static int WORLD_WIDTH  = 3840;
    public static int WORLD_HEIGHT = 2160;

    public static int BUTTON_WIDTH  = 200;
    public static int BUTTON_HEIGHT = 50;

    /** Whether the game is running in fullscreen mode */
    public static boolean fullscreen = true;

    public MainPanel mainPanel;
    public OptionsPanel optionsPanel;
    public GamePanel gamePanel;
    public WorldSettingsPanel worldSettingsPanel;

    public MainClass()
    {
        this.setTitle("Java Project");

        // Default to fullscreen: use screen dimensions
        if (fullscreen) {
            applyFullscreen();
        } else {
            this.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
            this.setLocationRelativeTo(null);
            this.setResizable(false);
        }

        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        mainPanel = new MainPanel(this);

        this.getContentPane().add(mainPanel);

        this.setVisible(true);
    }

    /**
     * Applies fullscreen mode: sets screen dimensions to match the display,
     * removes window decorations, and maximizes the frame.
     */
    public void applyFullscreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        SCREEN_WIDTH  = screenSize.width;
        SCREEN_HEIGHT = screenSize.height;
        this.dispose();
        this.setUndecorated(true);
        this.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setResizable(false);
        this.setVisible(true);
    }

    /**
     * Applies windowed mode with the given screen dimensions.
     */
    public void applyWindowed() {
        SCREEN_WIDTH  = 1280;
        SCREEN_HEIGHT = 720;
        this.dispose();
        this.setUndecorated(false);
        this.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        this.setExtendedState(JFrame.NORMAL);
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        this.setVisible(true);
    }

    /**
     * Toggles between fullscreen and windowed mode.
     */
    public void toggleFullscreen() {
        fullscreen = !fullscreen;
        if (fullscreen) {
            applyFullscreen();
        } else {
            applyWindowed();
        }
    }


    public static void main(String [] args)
    {
        SwingUtilities.invokeLater(MainClass::new);
    }
}
