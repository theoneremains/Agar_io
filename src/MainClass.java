import javax.swing.*;
import java.awt.*;

/**
 * MainClass : Top-level JFrame window and entry point for the game.
 * Holds global screen/world dimension state and references to all panels.
 * Supports fullscreen mode (default) with configurable world dimensions.
 * @author Kamil Yunus Özkaya
 */
public class MainClass extends JFrame {

    public static int SCREEN_WIDTH  = GameConstants.DEFAULT_SCREEN_WIDTH;
    public static int SCREEN_HEIGHT = GameConstants.DEFAULT_SCREEN_HEIGHT;

    public static int WORLD_WIDTH  = GameConstants.DEFAULT_WORLD_WIDTH;
    public static int WORLD_HEIGHT = GameConstants.DEFAULT_WORLD_HEIGHT;

    /** @deprecated Use {@link GameConstants#BUTTON_WIDTH} */
    public static int BUTTON_WIDTH  = GameConstants.BUTTON_WIDTH;
    /** @deprecated Use {@link GameConstants#BUTTON_HEIGHT} */
    public static int BUTTON_HEIGHT = GameConstants.BUTTON_HEIGHT;

    /** Whether the game is running in fullscreen mode */
    public static boolean fullscreen = true;

    public MainPanel mainPanel;
    public OptionsPanel optionsPanel;
    public GamePanel gamePanel;
    public WorldSettingsPanel worldSettingsPanel;

    public MainClass() {
        this.setTitle("Agar.io - Java Swing Clone");

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

    /** Applies windowed mode at 1280×720 */
    public void applyWindowed() {
        SCREEN_WIDTH  = GameConstants.DEFAULT_SCREEN_WIDTH;
        SCREEN_HEIGHT = GameConstants.DEFAULT_SCREEN_HEIGHT;
        this.dispose();
        this.setUndecorated(false);
        this.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        this.setExtendedState(JFrame.NORMAL);
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        this.setVisible(true);
    }

    /** Toggles between fullscreen and windowed mode */
    public void toggleFullscreen() {
        fullscreen = !fullscreen;
        if (fullscreen) {
            applyFullscreen();
        } else {
            applyWindowed();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainClass::new);
    }
}
