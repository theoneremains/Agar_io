import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * OptionsPanel class : Options Panel of the game
 * SOUND button toggles all game sounds on/off
 * COLOR button cycles through available player cell colors
 * FULLSCREEN button toggles between fullscreen and windowed mode
 * WORLD SIZE fields allow changing the world dimensions
 * CELL DENSITY field controls how many food cells spawn based on world area
 * @author Kamil Yunus Özkaya
 */
public class OptionsPanel extends JPanel {
    public JButton soundButton = new JButton(Sound.soundEnabled ? "SOUND: ON" : "SOUND: OFF");

    public JButton backButton = new JButton("BACK");

    public JButton colorButton = new JButton("COLOR");

    public JButton fullscreenButton = new JButton(MainClass.fullscreen ? "FULLSCREEN: ON" : "FULLSCREEN: OFF");

    public BufferedImage backgroundImg;

    private Sound click_sound = new Sound("click.wav", 1);

    // Small panel showing the currently selected player cell color
    private JPanel colorPreview = new JPanel();

    // World dimension input fields
    private JTextField tfWorldWidth;
    private JTextField tfWorldHeight;
    private JButton applyWorldButton = new JButton("APPLY WORLD");

    // Cell density input field
    private JTextField tfCellDensity;
    private JButton applyDensityButton = new JButton("APPLY DENSITY");


    public OptionsPanel(MainClass mainClass)
    {
        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
        setPreferredSize(new Dimension(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT));

        setFocusable(true);

        setLayout(null);

        int centerX = MainClass.SCREEN_WIDTH / 2;
        int centerY = MainClass.SCREEN_HEIGHT / 2;

        colorButton.setBounds(
                centerX - MainClass.BUTTON_WIDTH / 2,
                centerY - 4 * MainClass.BUTTON_HEIGHT,
                MainClass.BUTTON_WIDTH, MainClass.BUTTON_HEIGHT);

        soundButton.setBounds(
                centerX - MainClass.BUTTON_WIDTH / 2,
                centerY - 2 * MainClass.BUTTON_HEIGHT - MainClass.BUTTON_HEIGHT / 2,
                MainClass.BUTTON_WIDTH, MainClass.BUTTON_HEIGHT);

        fullscreenButton.setBounds(
                centerX - MainClass.BUTTON_WIDTH / 2,
                centerY - MainClass.BUTTON_HEIGHT,
                MainClass.BUTTON_WIDTH, MainClass.BUTTON_HEIGHT);

        // World size section
        JLabel worldLabel = new JLabel("World Size:");
        worldLabel.setForeground(Color.WHITE);
        worldLabel.setFont(new Font("Arial", Font.BOLD, 14));
        worldLabel.setBounds(centerX - MainClass.BUTTON_WIDTH / 2, centerY + 10, 100, 25);

        tfWorldWidth = new JTextField(String.valueOf(MainClass.WORLD_WIDTH));
        tfWorldWidth.setBounds(centerX - MainClass.BUTTON_WIDTH / 2 + 100, centerY + 10, 60, 25);

        JLabel xLabel = new JLabel(" x ");
        xLabel.setForeground(Color.WHITE);
        xLabel.setFont(new Font("Arial", Font.BOLD, 14));
        xLabel.setBounds(centerX - MainClass.BUTTON_WIDTH / 2 + 162, centerY + 10, 20, 25);

        tfWorldHeight = new JTextField(String.valueOf(MainClass.WORLD_HEIGHT));
        tfWorldHeight.setBounds(centerX - MainClass.BUTTON_WIDTH / 2 + 182, centerY + 10, 60, 25);

        applyWorldButton.setBounds(centerX - MainClass.BUTTON_WIDTH / 2 + 250, centerY + 10, 120, 25);
        applyWorldButton.setFont(new Font("Arial", Font.PLAIN, 11));

        // Cell density section
        JLabel densityLabel = new JLabel("Cell Density:");
        densityLabel.setForeground(Color.WHITE);
        densityLabel.setFont(new Font("Arial", Font.BOLD, 14));
        densityLabel.setBounds(centerX - MainClass.BUTTON_WIDTH / 2, centerY + 45, 110, 25);

        tfCellDensity = new JTextField(String.format("%.2f", GamePanel.cellDensity));
        tfCellDensity.setBounds(centerX - MainClass.BUTTON_WIDTH / 2 + 110, centerY + 45, 60, 25);

        JLabel densityHint = new JLabel("cells/M px");
        densityHint.setForeground(new Color(200, 200, 200));
        densityHint.setFont(new Font("Arial", Font.PLAIN, 11));
        densityHint.setBounds(centerX - MainClass.BUTTON_WIDTH / 2 + 175, centerY + 45, 80, 25);

        applyDensityButton.setBounds(centerX - MainClass.BUTTON_WIDTH / 2 + 250, centerY + 45, 120, 25);
        applyDensityButton.setFont(new Font("Arial", Font.PLAIN, 11));

        backButton.setBounds(
                centerX - MainClass.BUTTON_WIDTH / 2,
                centerY + 3 * MainClass.BUTTON_HEIGHT,
                MainClass.BUTTON_WIDTH, MainClass.BUTTON_HEIGHT);

        // Color preview square placed to the right of the COLOR button
        colorPreview.setBackground(GamePanel.playerColor);
        colorPreview.setBounds(
                centerX - MainClass.BUTTON_WIDTH / 2 + MainClass.BUTTON_WIDTH + 10,
                centerY - 4 * MainClass.BUTTON_HEIGHT,
                MainClass.BUTTON_HEIGHT, MainClass.BUTTON_HEIGHT);

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

        try
        {
            backgroundImg = ImageIO.read(this.getClass().getResource("agario.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        backButton.addActionListener(arg0 -> {
            click_sound.playSound();
            mainClass.mainPanel = new MainPanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.mainPanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.mainPanel.requestFocusInWindow();
        });

        // SOUND button: toggle all game sounds on/off
        soundButton.addActionListener(arg0 -> {
            click_sound.playSound();
            Sound.soundEnabled = !Sound.soundEnabled;
            soundButton.setText(Sound.soundEnabled ? "SOUND: ON" : "SOUND: OFF");
        });

        // COLOR button: cycle through the available cell color palette
        colorButton.addActionListener(arg0 -> {
            click_sound.playSound();
            GamePanel.playerColorIndex = (GamePanel.playerColorIndex + 1) % GamePanel.colors.length;
            GamePanel.playerColor = GamePanel.colors[GamePanel.playerColorIndex];
            colorPreview.setBackground(GamePanel.playerColor);
        });

        // FULLSCREEN button: toggle between fullscreen and windowed mode
        fullscreenButton.addActionListener(arg0 -> {
            click_sound.playSound();
            mainClass.toggleFullscreen();
            fullscreenButton.setText(MainClass.fullscreen ? "FULLSCREEN: ON" : "FULLSCREEN: OFF");
            // Recreate options panel to adjust layout to new screen size
            mainClass.optionsPanel = new OptionsPanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.optionsPanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.optionsPanel.requestFocusInWindow();
        });

        // APPLY WORLD SIZE button: update world dimensions
        applyWorldButton.addActionListener(arg0 -> {
            click_sound.playSound();
            try {
                int w = Integer.parseInt(tfWorldWidth.getText().trim());
                int h = Integer.parseInt(tfWorldHeight.getText().trim());
                if (w >= 800 && h >= 600) {
                    MainClass.WORLD_WIDTH = w;
                    MainClass.WORLD_HEIGHT = h;
                    JOptionPane.showMessageDialog(mainClass,
                        "World size set to " + w + " x " + h + ".\nChanges apply to the next game.",
                        "World Size Updated", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(mainClass,
                        "Minimum world size is 800 x 600.",
                        "Invalid Size", JOptionPane.WARNING_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(mainClass,
                    "Please enter valid numbers for width and height.",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            }
        });

        // APPLY DENSITY button: update cell density
        applyDensityButton.addActionListener(arg0 -> {
            click_sound.playSound();
            try {
                double d = Double.parseDouble(tfCellDensity.getText().trim());
                if (d > 0) {
                    GamePanel.cellDensity = d;
                    double worldArea = (double) MainClass.WORLD_WIDTH * MainClass.WORLD_HEIGHT / 1_000_000.0;
                    int maxCells = Math.max(5, (int) Math.round(d * worldArea));
                    JOptionPane.showMessageDialog(mainClass,
                        "Cell density set to " + String.format("%.2f", d) + " cells/M px.\n" +
                        "Max food cells for current world: " + maxCells,
                        "Cell Density Updated", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(mainClass,
                        "Density must be a positive number.",
                        "Invalid Density", JOptionPane.WARNING_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(mainClass,
                    "Please enter a valid number for density.",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            }
        });

    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (backgroundImg != null) {
            g.drawImage(backgroundImg, 0, 0, MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT, null);
        }
    }

    @Override
    public void paint(Graphics g)
    {
        super.paint(g); // paints background, border, and child components (buttons)
        g.setColor(Color.BLUE);
        g.drawString("High Score " + GamePanel.highscore, 10, 20);
        g.setColor(Color.BLACK);
        g.setFont(new Font("", Font.BOLD, 55));
        g.drawString("Agar.io", (MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2, (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2 - 5 * MainClass.BUTTON_HEIGHT);
    }
}
