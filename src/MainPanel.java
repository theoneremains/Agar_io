
import javax.swing.*;
import java.awt.*;

/**
 * MainPanel class : Main Menu of the game with modern UI
 * Has Start, Options and Exit styled buttons with hover effects
 * Features an animated gradient background and ambient menu sound
 * @author Kamil Yunus Ozkaya
 */
public class MainPanel extends JPanel
{
    public StyledButton startButton = new StyledButton("START", new Color(40, 140, 70));
    public StyledButton optionsButton = new StyledButton("OPTIONS", new Color(50, 80, 140));
    public StyledButton exitButton = new StyledButton("EXIT", new Color(140, 50, 50));

    /** Ambient menu sound line — stopped when leaving the menu */
    private javax.sound.sampled.SourceDataLine ambientLine;

    /** Phase for animated background gradient */
    private float bgPhase = 0f;

    /** Timer for background animation */
    private Timer bgTimer;

    public MainPanel(final MainClass mainClass)
    {
        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
        setPreferredSize(new Dimension(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT));

        setLayout(null);

        int btnW = MainClass.BUTTON_WIDTH + 40;
        int btnH = MainClass.BUTTON_HEIGHT + 6;

        startButton.setBounds((MainClass.SCREEN_WIDTH - btnW) / 2,
            (MainClass.SCREEN_HEIGHT) / 2 - btnH - 20, btnW, btnH);
        optionsButton.setBounds((MainClass.SCREEN_WIDTH - btnW) / 2,
            (MainClass.SCREEN_HEIGHT) / 2 + 5, btnW, btnH);
        exitButton.setBounds((MainClass.SCREEN_WIDTH - btnW) / 2,
            (MainClass.SCREEN_HEIGHT) / 2 + btnH + 30, btnW, btnH);

        add(startButton);
        add(optionsButton);
        add(exitButton);

        // Start ambient menu sound
        ambientLine = Sound.playMenuAmbient();

        // Start background animation timer
        bgTimer = new Timer(30, e -> {
            bgPhase = (bgPhase + 0.003f) % 1f;
            repaint();
        });
        bgTimer.start();

        startButton.addActionListener(arg0 -> {
            Sound.playClickSound();
            stopAmbient();
            mainClass.worldSettingsPanel = new WorldSettingsPanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.worldSettingsPanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.worldSettingsPanel.requestFocusInWindow();
        });

        optionsButton.addActionListener(arg0 -> {
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
        if (ambientLine != null) {
            try { ambientLine.stop(); ambientLine.close(); } catch (Exception ignored) {}
            ambientLine = null;
        }
        if (bgTimer != null) bgTimer.stop();
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

        // Subtle floating circles for depth
        Composite orig = g2d.getComposite();
        for (int i = 0; i < 8; i++) {
            float hue = (bgPhase + i * 0.12f) % 1f;
            Color circleColor = Color.getHSBColor(hue, 0.3f, 0.25f);
            float circleAlpha = 0.06f + 0.03f * (float) Math.sin(bgPhase * Math.PI * 2 + i);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, circleAlpha));
            g2d.setColor(circleColor);
            int cx = (int) (w * 0.15 + (w * 0.7) * Math.sin(bgPhase * Math.PI + i * 0.8));
            int cy = (int) (h * 0.15 + (h * 0.7) * Math.cos(bgPhase * Math.PI * 0.7 + i * 1.1));
            int sz = 100 + i * 50;
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

        int w = MainClass.SCREEN_WIDTH;
        int h = MainClass.SCREEN_HEIGHT;

        // High score display
        g2d.setColor(new Color(180, 200, 255, 180));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2d.drawString("High Score: " + GamePanel.highscore, 15, 25);

        // Title with glow effect
        String title = "Agar.io";
        g2d.setFont(new Font("SansSerif", Font.BOLD, 64));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = (w - fm.stringWidth(title)) / 2;
        int ty = h / 2 - MainClass.BUTTON_HEIGHT * 3 - 30;

        // Glow
        g2d.setColor(new Color(100, 180, 255, 50));
        g2d.drawString(title, tx - 2, ty + 2);
        g2d.drawString(title, tx + 2, ty - 2);
        // Main text
        g2d.setColor(new Color(220, 240, 255));
        g2d.drawString(title, tx, ty);

        // Subtitle
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g2d.setColor(new Color(150, 170, 200, 160));
        String sub = "A Java Swing Clone";
        FontMetrics sfm = g2d.getFontMetrics();
        g2d.drawString(sub, (w - sfm.stringWidth(sub)) / 2, ty + 35);
    }
}
