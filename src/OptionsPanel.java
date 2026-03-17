
import javax.swing.*;
import java.awt.*;

/**
 * OptionsPanel class : Options Panel of the game with modern styled UI.
 * SOUND button toggles all game sounds on/off.
 * COLOR button cycles through available player cell colors.
 * FULLSCREEN button toggles between fullscreen and windowed mode.
 * @author Kamil Yunus Ozkaya
 */
@SuppressWarnings({"serial", "this-escape"})
public class OptionsPanel extends JPanel {

    private final StyledButton soundButton = new StyledButton(
        Sound.soundEnabled ? "SOUND: ON" : "SOUND: OFF",
        Sound.soundEnabled ? GameConstants.BTN_ON : GameConstants.BTN_OFF);

    private final StyledButton colorButton = new StyledButton("COLOR", new Color(60, 60, 130));

    private final StyledButton fullscreenButton = new StyledButton(
        MainClass.fullscreen ? "FULLSCREEN: ON" : "FULLSCREEN: OFF",
        MainClass.fullscreen ? GameConstants.BTN_ON : GameConstants.BTN_OFF);

    private final StyledButton backButton = new StyledButton("BACK", new Color(100, 60, 60));

    private final JPanel colorPreview = new JPanel();
    private final MenuBackground menuBg;

    public OptionsPanel(MainClass mainClass) {
        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
        setPreferredSize(new Dimension(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT));
        setFocusable(true);
        setLayout(null);

        int centerX = MainClass.SCREEN_WIDTH / 2;
        int centerY = MainClass.SCREEN_HEIGHT / 2;
        int btnW = GameConstants.BUTTON_WIDTH + 40;
        int btnH = GameConstants.BUTTON_HEIGHT + 6;

        colorButton.setBounds(centerX - btnW / 2, centerY - 4 * GameConstants.BUTTON_HEIGHT - 10, btnW, btnH);
        soundButton.setBounds(centerX - btnW / 2, centerY - 2 * GameConstants.BUTTON_HEIGHT - GameConstants.BUTTON_HEIGHT / 2 - 5, btnW, btnH);
        fullscreenButton.setBounds(centerX - btnW / 2, centerY - GameConstants.BUTTON_HEIGHT + 5, btnW, btnH);
        backButton.setBounds(centerX - btnW / 2, centerY + GameConstants.BUTTON_HEIGHT + 15, btnW, btnH);

        // Color preview square
        colorPreview.setBackground(GamePanel.playerColor);
        colorPreview.setBounds(centerX + btnW / 2 + 10, centerY - 4 * GameConstants.BUTTON_HEIGHT - 10, btnH, btnH);
        colorPreview.setBorder(BorderFactory.createLineBorder(new Color(100, 120, 160), 2));

        add(backButton);
        add(soundButton);
        add(colorButton);
        add(fullscreenButton);
        add(colorPreview);

        // Start animated background
        menuBg = new MenuBackground(6, this);
        menuBg.start();

        backButton.addActionListener(e -> {
            Sound.playClickSound();
            menuBg.stop();
            mainClass.mainPanel = new MainPanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.mainPanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.mainPanel.requestFocusInWindow();
        });

        soundButton.addActionListener(e -> {
            Sound.playClickSound();
            Sound.soundEnabled = !Sound.soundEnabled;
            soundButton.setText(Sound.soundEnabled ? "SOUND: ON" : "SOUND: OFF");
            soundButton.setBaseColor(Sound.soundEnabled ? GameConstants.BTN_ON : GameConstants.BTN_OFF);
        });

        colorButton.addActionListener(e -> {
            Sound.playClickSound();
            GamePanel.playerColorIndex = (GamePanel.playerColorIndex + 1) % GameConstants.CELL_COLORS.length;
            GamePanel.playerColor = GameConstants.CELL_COLORS[GamePanel.playerColorIndex];
            colorPreview.setBackground(GamePanel.playerColor);
        });

        fullscreenButton.addActionListener(e -> {
            Sound.playClickSound();
            mainClass.toggleFullscreen();
            fullscreenButton.setText(MainClass.fullscreen ? "FULLSCREEN: ON" : "FULLSCREEN: OFF");
            fullscreenButton.setBaseColor(MainClass.fullscreen ? GameConstants.BTN_ON : GameConstants.BTN_OFF);
            // Recreate options panel to adjust layout to new screen size
            menuBg.stop();
            mainClass.optionsPanel = new OptionsPanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.optionsPanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.optionsPanel.requestFocusInWindow();
        });
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

        MenuBackground.drawHighScore(g2d, GamePanel.highscore);
        MenuBackground.drawTitle(g2d, "Options", 48,
            MainClass.SCREEN_HEIGHT / 2 - 6 * GameConstants.BUTTON_HEIGHT, MainClass.SCREEN_WIDTH, false);
    }
}
