
import javax.swing.*;
import java.awt.*;

/**
 * OptionsPanel class : Options Panel of the game with modern styled UI
 * SOUND button toggles all game sounds on/off
 * COLOR button cycles through available player cell colors
 * FULLSCREEN button toggles between fullscreen and windowed mode
 * WORLD SIZE fields allow changing the world dimensions
 * CELL DENSITY field controls how many food cells spawn based on world area
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

    // World dimension input fields
    private JTextField tfWorldWidth;
    private JTextField tfWorldHeight;
    private StyledButton applyWorldButton = new StyledButton("APPLY", new Color(50, 90, 130));
    // Cell density input field
    private JTextField tfCellDensity;
    private StyledButton applyDensityButton = new StyledButton("APPLY", new Color(50, 90, 130));

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

        // World size section
        JLabel worldLabel = new JLabel("World Size:");
        worldLabel.setForeground(new Color(180, 200, 240));
        worldLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        worldLabel.setBounds(centerX - btnW / 2, centerY + MainClass.BUTTON_HEIGHT + 15, 100, 25);

        tfWorldWidth = new JTextField(String.valueOf(MainClass.WORLD_WIDTH));
        styleTextField(tfWorldWidth);
        tfWorldWidth.setBounds(centerX - btnW / 2 + 100, centerY + MainClass.BUTTON_HEIGHT + 15, 65, 28);

        JLabel xLabel = new JLabel(" x ");
        xLabel.setForeground(new Color(180, 200, 240));
        xLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        xLabel.setBounds(centerX - btnW / 2 + 167, centerY + MainClass.BUTTON_HEIGHT + 15, 20, 25);

        tfWorldHeight = new JTextField(String.valueOf(MainClass.WORLD_HEIGHT));
        styleTextField(tfWorldHeight);
        tfWorldHeight.setBounds(centerX - btnW / 2 + 187, centerY + MainClass.BUTTON_HEIGHT + 15, 65, 28);

        applyWorldButton.setFont(new Font("SansSerif", Font.BOLD, 11));
        applyWorldButton.setBounds(centerX - btnW / 2 + 260, centerY + MainClass.BUTTON_HEIGHT + 13, 90, 30);

        // Cell density section
        JLabel densityLabel = new JLabel("Cell Density:");
        densityLabel.setForeground(new Color(180, 200, 240));
        densityLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        densityLabel.setBounds(centerX - btnW / 2, centerY + MainClass.BUTTON_HEIGHT + 55, 110, 25);

        tfCellDensity = new JTextField(String.format("%.2f", GamePanel.cellDensity));
        styleTextField(tfCellDensity);
        tfCellDensity.setBounds(centerX - btnW / 2 + 110, centerY + MainClass.BUTTON_HEIGHT + 55, 65, 28);

        JLabel densityHint = new JLabel("cells/M px");
        densityHint.setForeground(new Color(140, 160, 190));
        densityHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        densityHint.setBounds(centerX - btnW / 2 + 180, centerY + MainClass.BUTTON_HEIGHT + 55, 80, 25);

        applyDensityButton.setFont(new Font("SansSerif", Font.BOLD, 11));
        applyDensityButton.setBounds(centerX - btnW / 2 + 260, centerY + MainClass.BUTTON_HEIGHT + 53, 90, 30);

        backButton.setBounds(
                centerX - btnW / 2,
                centerY + 3 * MainClass.BUTTON_HEIGHT + 30,
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
        add(worldLabel);
        add(tfWorldWidth);
        add(xLabel);
        add(tfWorldHeight);
        add(applyWorldButton);
        add(densityLabel);
        add(tfCellDensity);
        add(densityHint);
        add(applyDensityButton);

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

        // APPLY WORLD SIZE button: update world dimensions
        applyWorldButton.addActionListener(arg0 -> {
            Sound.playClickSound();
            try {
                int w = Integer.parseInt(tfWorldWidth.getText().trim());
                int h = Integer.parseInt(tfWorldHeight.getText().trim());
                if (w >= 800 && h >= 600) {
                    MainClass.WORLD_WIDTH = w;
                    MainClass.WORLD_HEIGHT = h;
                    StyledDialog.showMessageDialog(mainClass,
                        "World size set to " + w + " x " + h + ".<br>Changes apply to the next game.",
                        "World Size Updated", false);
                } else {
                    StyledDialog.showMessageDialog(mainClass,
                        "Minimum world size is 800 x 600.",
                        "Invalid Size", true);
                }
            } catch (NumberFormatException ex) {
                StyledDialog.showMessageDialog(mainClass,
                    "Please enter valid numbers for width and height.",
                    "Invalid Input", true);
            }
        });

        // APPLY DENSITY button: update cell density
        applyDensityButton.addActionListener(arg0 -> {
            Sound.playClickSound();
            try {
                double d = Double.parseDouble(tfCellDensity.getText().trim());
                if (d > 0) {
                    GamePanel.cellDensity = d;
                    double worldArea = (double) MainClass.WORLD_WIDTH * MainClass.WORLD_HEIGHT / 1_000_000.0;
                    int maxCells = Math.max(5, (int) Math.round(d * worldArea));
                    StyledDialog.showMessageDialog(mainClass,
                        "Cell density set to " + String.format("%.2f", d) + " cells/M px.<br>" +
                        "Max food cells for current world: " + maxCells,
                        "Cell Density Updated", false);
                } else {
                    StyledDialog.showMessageDialog(mainClass,
                        "Density must be a positive number.",
                        "Invalid Density", true);
                }
            } catch (NumberFormatException ex) {
                StyledDialog.showMessageDialog(mainClass,
                    "Please enter a valid number for density.",
                    "Invalid Input", true);
            }
        });

    }

    /** Styles a text field to match the dark modern theme */
    private void styleTextField(JTextField tf) {
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setBackground(new Color(40, 45, 60));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 80, 110)),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
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
