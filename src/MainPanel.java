
import javax.swing.*;
import java.awt.*;

/**
 * MainPanel class : Main Menu of the game with modern UI.
 * Has Start, Options and Exit styled buttons with hover effects.
 * Features an animated gradient background and ambient menu sound.
 * @author Kamil Yunus Ozkaya
 */
@SuppressWarnings({"serial", "this-escape"})
public class MainPanel extends JPanel {

    private final StyledButton startButton    = new StyledButton("START", GameConstants.BTN_GREEN);
    private final StyledButton evolvingButton = new StyledButton("EVOLVING MODE", GameConstants.BTN_PURPLE);
    private final StyledButton optionsButton  = new StyledButton("OPTIONS", GameConstants.BTN_BLUE);
    private final StyledButton exitButton     = new StyledButton("EXIT", GameConstants.BTN_RED);

    /** Ambient menu sound line — stopped when leaving the menu */
    private javax.sound.sampled.SourceDataLine ambientLine;

    /** Shared animated menu background */
    private final MenuBackground menuBg;

    public MainPanel(final MainClass mainClass) {
        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
        setPreferredSize(new Dimension(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT));
        setLayout(null);

        int btnW = GameConstants.BUTTON_WIDTH + 40;
        int btnH = GameConstants.BUTTON_HEIGHT + 6;
        int gap  = 14;
        int totalH = btnH * 4 + gap * 3;
        int startY = MainClass.SCREEN_HEIGHT / 2 - totalH / 2;

        startButton.setBounds((MainClass.SCREEN_WIDTH - btnW) / 2, startY, btnW, btnH);
        evolvingButton.setBounds((MainClass.SCREEN_WIDTH - btnW) / 2, startY + (btnH + gap), btnW, btnH);
        optionsButton.setBounds((MainClass.SCREEN_WIDTH - btnW) / 2, startY + (btnH + gap) * 2, btnW, btnH);
        exitButton.setBounds((MainClass.SCREEN_WIDTH - btnW) / 2, startY + (btnH + gap) * 3, btnW, btnH);

        add(startButton);
        add(evolvingButton);
        add(optionsButton);
        add(exitButton);

        // Start ambient menu sound
        ambientLine = Sound.playMenuAmbient();

        // Start animated background
        menuBg = new MenuBackground(8, this);
        menuBg.start();

        startButton.addActionListener(e -> {
            Sound.playClickSound();
            stopAmbient();
            mainClass.worldSettingsPanel = new WorldSettingsPanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.worldSettingsPanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.worldSettingsPanel.requestFocusInWindow();
        });

        evolvingButton.addActionListener(e -> {
            Sound.playClickSound();
            stopAmbient();
            mainClass.evolvingModePanel = new EvolvingModePanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.evolvingModePanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.evolvingModePanel.requestFocusInWindow();
        });

        optionsButton.addActionListener(e -> {
            Sound.playClickSound();
            stopAmbient();
            mainClass.optionsPanel = new OptionsPanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.optionsPanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.optionsPanel.requestFocusInWindow();
        });

        exitButton.addActionListener(e -> {
            Sound.playClickSound();
            boolean confirmed = StyledDialog.showConfirmDialog(mainClass,
                    "Are you sure you want to exit the program?", "Exit Program");
            if (confirmed) {
                stopAmbient();
                System.exit(0);
            }
        });
    }

    /** Stops the ambient menu sound and background animation */
    private void stopAmbient() {
        ToneGenerator.stopLine(ambientLine);
        ambientLine = null;
        menuBg.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        menuBg.draw((Graphics2D) g, MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = MainClass.SCREEN_WIDTH;
        int h = MainClass.SCREEN_HEIGHT;

        MenuBackground.drawHighScore(g2d, GamePanel.highscore);

        // Title with glow
        int titleY = h / 2 - GameConstants.BUTTON_HEIGHT * 3 - 30;
        MenuBackground.drawTitle(g2d, "Agar.io", 64, titleY, w, true);

        // Subtitle
        g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 16));
        g2d.setColor(new Color(150, 170, 200, 160));
        String sub = "A Java Swing Clone";
        FontMetrics sfm = g2d.getFontMetrics();
        g2d.drawString(sub, (w - sfm.stringWidth(sub)) / 2, titleY + 35);
    }
}
