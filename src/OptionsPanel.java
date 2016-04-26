import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * OptionsPanel class : Options Panel of the game
 * Options class does not do anything for now
 * @author Kamil Yunus Özkaya
 */
public class OptionsPanel extends JPanel {
    public JButton soundButton = new JButton("SOUND");

    public JButton backButton = new JButton("BACK");

    public JButton colorButton = new JButton("COLOR");

    public BufferedImage backgroundImg;

    private Sound click_sound = new Sound("click.wav",1);

    public OptionsPanel(MainClass mainClass)
    {
        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);

        setFocusable(true);

        setLayout(null);

        colorButton.setBounds((MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2, (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2 - 2 * MainClass.BUTTON_HEIGHT , MainClass.BUTTON_WIDTH, MainClass.BUTTON_HEIGHT);

        soundButton.setBounds((MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2, (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2 , MainClass.BUTTON_WIDTH, MainClass.BUTTON_HEIGHT);

        backButton.setBounds((MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2, (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2 + 2 * MainClass.BUTTON_HEIGHT, MainClass.BUTTON_WIDTH, MainClass.BUTTON_HEIGHT);

        add(backButton);

        add(soundButton);

        add(colorButton);

        try
        {
            backgroundImg = ImageIO.read(this.getClass().getResource("agario.png"));
        }
        catch(IOException e)
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
        soundButton.addActionListener(arg0 -> click_sound.playSound());

        colorButton.addActionListener(arg0 -> click_sound.playSound());
    }

    @Override
    public void paint(Graphics g)
    {
        g.drawImage(backgroundImg, 0,0,null);
        paintComponents(g);
        g.setColor(Color.BLUE);
        g.drawString("High Score " + GamePanel.highscore,10,20);
        g.setColor(Color.BLACK);
        g.setFont(new Font("", Font.BOLD, 55));
        g.drawString("Agar.io",(MainClass.SCREEN_WIDTH - MainClass.BUTTON_WIDTH) / 2, (MainClass.SCREEN_HEIGHT - MainClass.BUTTON_HEIGHT) / 2 - 4 * MainClass.BUTTON_HEIGHT);
    }
}
