import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * OptionsPanel class : Options Panel of the game
 * SOUND button toggles all game sounds on/off
 * COLOR button cycles through available player cell colors
 * @author Kamil Yunus Özkaya
 */
public class OptionsPanel extends JPanel {
    public JButton soundButton = new JButton(Sound.soundEnabled ? "SOUND: ON" : "SOUND: OFF");

    public JButton backButton = new JButton("BACK");

    public JButton colorButton = new JButton("COLOR");

    public BufferedImage backgroundImg;

    private Sound click_sound = new Sound("click.wav", 1);

    // Small panel showing the currently selected player cell color
    private JPanel colorPreview = new JPanel();

    public OptionsPanel(MainClass mainClass)
    {
        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);

        setFocusable(true);

        setLayout(null);

        colorButton.setBounds(
                (MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2,
                (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2 - 2 * MainClass.BUTTON_HEIGHT,
                MainClass.BUTTON_WIDTH, MainClass.BUTTON_HEIGHT);

        soundButton.setBounds(
                (MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2,
                (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2,
                MainClass.BUTTON_WIDTH, MainClass.BUTTON_HEIGHT);

        backButton.setBounds(
                (MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2,
                (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2 + 2 * MainClass.BUTTON_HEIGHT,
                MainClass.BUTTON_WIDTH, MainClass.BUTTON_HEIGHT);

        // Color preview square placed to the right of the COLOR button
        colorPreview.setBackground(GamePanel.playerColor);
        colorPreview.setBounds(
                (MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2 + MainClass.BUTTON_WIDTH + 10,
                (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2 - 2 * MainClass.BUTTON_HEIGHT,
                MainClass.BUTTON_HEIGHT, MainClass.BUTTON_HEIGHT);

        add(backButton);
        add(soundButton);
        add(colorButton);
        add(colorPreview);

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
            mainClass.mainPanel.requestFocusInWindow();
            mainClass.revalidate();
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
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g.drawImage(backgroundImg, 0, 0, null);
    }

    @Override
    public void paint(Graphics g)
    {
        super.paint(g); // paints background, border, and child components (buttons)
        g.setColor(Color.BLUE);
        g.drawString("High Score " + GamePanel.highscore, 10, 20);
        g.setColor(Color.BLACK);
        g.setFont(new Font("", Font.BOLD, 55));
        g.drawString("Agar.io", (MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2, (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2 - 4 * MainClass.BUTTON_HEIGHT);
    }
}
