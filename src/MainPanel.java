import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * MainPanel class : Main Menu of the game
 * Has Start, Options and Exit button
 * @author Kamil Yunus Özkaya
 */
public class MainPanel extends JPanel
{
    public BufferedImage backgroundImg;

    public JButton startButton = new JButton("START");
    public JButton optionsButton = new JButton("OPTIONS");
    public JButton exitButton = new JButton("EXIT");
    private Sound click_sound = new Sound("click.wav",1);

    public MainPanel(final MainClass mainClass)
    {
        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);

        setLayout(null);

        startButton.setBounds((MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2, (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2 - 2 * MainClass.BUTTON_HEIGHT, MainClass.BUTTON_WIDTH, MainClass.BUTTON_HEIGHT);

        optionsButton.setBounds((MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2, (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2, MainClass.BUTTON_WIDTH, MainClass.BUTTON_HEIGHT);

        exitButton.setBounds((MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2, (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2 + 2 * MainClass.BUTTON_HEIGHT, MainClass.BUTTON_WIDTH, MainClass.BUTTON_HEIGHT);

        add(startButton);

        add(optionsButton);

        add(exitButton);

        try
        {
            backgroundImg = ImageIO.read(this.getClass().getResource("agario.png"));
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        startButton.addActionListener(arg0 -> {
            click_sound.playSound();
            mainClass.gamePanel = new GamePanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.gamePanel);
            mainClass.gamePanel.requestFocusInWindow();
            mainClass.revalidate();
        });

        optionsButton.addActionListener(arg0 -> {
            click_sound.playSound();
            mainClass.optionsPanel = new OptionsPanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.optionsPanel);
            mainClass.optionsPanel.requestFocusInWindow();
            mainClass.revalidate();
        });
        exitButton.addActionListener(e -> {
            click_sound.playSound();
            int confirmed = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit the program?",
                    "Exit Program Message Box", JOptionPane.YES_NO_OPTION);
            if (confirmed == JOptionPane.YES_OPTION) {
                click_sound.playSound();
                System.exit(0);
            }
            else {
                click_sound.playSound();
            }
        });
    }

    @Override
    public void paint(Graphics g)
    {
        g.drawImage(backgroundImg, 0,0,null);
        paintComponents(g);
        g.setColor(Color.BLUE);
        g.setFont(new Font("", Font.BOLD, 10));
        g.drawString("High Score " + GamePanel.highscore,10,20);
        g.setColor(Color.BLACK);
        g.setFont(new Font("", Font.BOLD, 55));
        g.drawString("Agar.io",(MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2, (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2 - 4 * MainClass.BUTTON_HEIGHT);
    }

}
