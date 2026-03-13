
import javax.swing.*;
import java.awt.*;

/**
 * OptionsPanel class : Options Panel of the game with modern styled UI
 * SOUND button toggles all game sounds on/off
 * COLOR button cycles through available player cell colors
 * FULLSCREEN button toggles between fullscreen and windowed mode
 * World size and cell density settings have moved to WorldSettingsPanel
 * @author Kamil Yunus Ozkaya
 */
public class OptionsPanel extends JPanel {
    public StyledButton soundButton = new StyledButton(
        Sound.soundEnabled ? "SOUND: ON" : "SOUND: OFF",
        Sound.soundEnabled ? new Color(40, 130, 70) : new Color(120, 50, 50));

    public StyledButton backButton = new StyledButton("BACK", new Color(100, 60, 60));

    public StyledButton colorButton = new StyledButton("COLOR", new Color(60, 60, 130));

    public StyledButton fullscreenButton = new StyledButton(
        MainClass.fullscreen ? "FULLSCREEN: ON" : "FULLSCREEN: OFF",
        MainClass.fullscreen ? new Color(40, 130, 70) : new Color(120, 50, 50));

    // Small panel showing the currently selected player cell color
    private JPanel colorPreview = new JPanel();


    /** Phase for animated background gradient */
    private float bgPhase = 0f;
    private Timer bgTimer;


    public OptionsPanel(MainClass mainClass)
    {
        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
        setPreferredSize(new Dimension(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT));

        setFocusable(true);

        setLayout(null);

        int centerX = MainClass.SCREEN_WIDTH / 2;
        int centerY = MainClass.SCREEN_HEIGHT / 2;
        int btnW = MainClass.BUTTON_WIDTH + 40;
        int btnH = MainClass.BUTTON_HEIGHT + 6;

        colorButton.setBounds(
                centerX - btnW / 2,
                centerY - 4 * MainClass.BUTTON_HEIGHT - 10,
                btnW, btnH);

        soundButton.setBounds(
                centerX - btnW / 2,
                centerY - 2 * MainClass.BUTTON_HEIGHT - MainClass.BUTTON_HEIGHT / 2 - 5,
                btnW, btnH);

        fullscreenButton.setBounds(
                centerX - btnW / 2,
                centerY - MainClass.BUTTON_HEIGHT + 5,
                btnW, btnH);

        backButton.setBounds(
                centerX - btnW / 2,
                centerY + MainClass.BUTTON_HEIGHT + 15,
                btnW, btnH);

        // Color preview square placed to the right of the COLOR button
        colorPreview.setBackground(GamePanel.playerColor);
        colorPreview.setBounds(
                centerX + btnW / 2 + 10,
                centerY - 4 * MainClass.BUTTON_HEIGHT - 10,
                btnH, btnH);
        colorPreview.setBorder(BorderFactory.createLineBorder(new Color(100, 120, 160), 2));

        add(backButton);
        add(soundButton);
        add(colorButton);
        add(fullscreenButton);
        add(colorPreview);

        // Start background animation
        bgTimer = new Timer(30, e -> {
            bgPhase = (bgPhase + 0.003f) % 1f;
            repaint();
        });
        bgTimer.start();

        backButton.addActionListener(arg0 -> {
            Sound.playClickSound();
            bgTimer.stop();
            mainClass.mainPanel = new MainPanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.mainPanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.mainPanel.requestFocusInWindow();
        });

        // SOUND button: toggle all game sounds on/off
        soundButton.addActionListener(arg0 -> {
            Sound.playClickSound();
            Sound.soundEnabled = !Sound.soundEnabled;
            soundButton.setText(Sound.soundEnabled ? "SOUND: ON" : "SOUND: OFF");
            soundButton.setBaseColor(Sound.soundEnabled ? new Color(40, 130, 70) : new Color(120, 50, 50));
        });

        // COLOR button: cycle through the available cell color palette
        colorButton.addActionListener(arg0 -> {
            Sound.playClickSound();
            GamePanel.playerColorIndex = (GamePanel.playerColorIndex + 1) % GamePanel.colors.length;
            GamePanel.playerColor = GamePanel.colors[GamePanel.playerColorIndex];
            colorPreview.setBackground(GamePanel.playerColor);
        });

        // FULLSCREEN button: toggle between fullscreen and windowed mode
        fullscreenButton.addActionListener(arg0 -> {
            Sound.playClickSound();
            mainClass.toggleFullscreen();
            fullscreenButton.setText(MainClass.fullscreen ? "FULLSCREEN: ON" : "FULLSCREEN: OFF");
            fullscreenButton.setBaseColor(MainClass.fullscreen ? new Color(40, 130, 70) : new Color(120, 50, 50));
            // Recreate options panel to adjust layout to new screen size
            bgTimer.stop();
            mainClass.optionsPanel = new OptionsPanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.optionsPanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.optionsPanel.requestFocusInWindow();
        });


    }


    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = MainClass.SCREEN_WIDTH;
        int h = MainClass.SCREEN_HEIGHT;

        // Animated gradient background
        Color c1 = Color.getHSBColor(bgPhase, 0.35f, 0.18f);
        Color c2 = Color.getHSBColor((bgPhase + 0.25f) % 1f, 0.30f, 0.12f);
        g2d.setPaint(new GradientPaint(0, 0, c1, w, h, c2));
        g2d.fillRect(0, 0, w, h);

        // Subtle floating circles
        Composite orig = g2d.getComposite();
        for (int i = 0; i < 6; i++) {
            float hue = (bgPhase + i * 0.15f) % 1f;
            Color circleColor = Color.getHSBColor(hue, 0.3f, 0.22f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.05f));
            g2d.setColor(circleColor);
            int cx = (int) (w * 0.2 + (w * 0.6) * Math.sin(bgPhase * Math.PI + i * 1.0));
            int cy = (int) (h * 0.2 + (h * 0.6) * Math.cos(bgPhase * Math.PI * 0.6 + i * 1.3));
            int sz = 80 + i * 40;
            g2d.fillOval(cx - sz, cy - sz, sz * 2, sz * 2);
        }
        g2d.setComposite(orig);
    }

    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // High score
        g2d.setColor(new Color(180, 200, 255, 180));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2d.drawString("High Score: " + GamePanel.highscore, 15, 25);

        // Title
        String title = "Options";
        g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = (MainClass.SCREEN_WIDTH - fm.stringWidth(title)) / 2;
        g2d.setColor(new Color(220, 240, 255));
        g2d.drawString(title, tx, MainClass.SCREEN_HEIGHT / 2 - 6 * MainClass.BUTTON_HEIGHT);
    }
}
