
import javax.swing.*;
import java.awt.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Random;

/**
 * GamePanel : Game runs here
 * It creates the user cell in the middle of the panel and random cells in a random place
 * Puts the randomcells in an ArrayList
 * When player cell eats a random cell its size is increased as well as the score
 * @author Kamil Yunus Özkaya
 */
public class GamePanel extends JPanel implements KeyListener {
    public HUD hud = new HUD();

    private Random random = new Random();

    public Color[] colors = {Color.BLACK,Color.BLUE,Color.CYAN,Color.DARK_GRAY,Color.GRAY,Color.GREEN,Color.LIGHT_GRAY,Color.MAGENTA,Color.ORANGE,Color.YELLOW,Color.PINK};

    public static int highscore = 0;

    public ArrayList<Cell> celllist = new ArrayList<>();

    public boolean right, left, up, down;

    public Background background;

    public Cell playerCell,randomCell,coloredCell;

    public int cellrad = 18;                            //Radius of the player cell is 18 initially

    public int rndcellrad = 13;                         //All random cells have radius of 13

    private Sound music = new Sound("coolMusic.wav",1); //This is just for trolling the end of the game :)

    public int mus = 0;

    MainClass mainClass;

    //Creates the panel and runs the threads
    public GamePanel(MainClass mainClass) {

        this.mainClass = mainClass;

        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);

        setFocusable(true);

        addKeyListener(this);

        setVisible(true);

        background = new Background(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);

        playerCell = new Cell(MainClass.SCREEN_WIDTH/2, MainClass.SCREEN_HEIGHT/2, cellrad);

        randomCell = new Cell(random.nextInt(MainClass.SCREEN_WIDTH-rndcellrad),random.nextInt(MainClass.SCREEN_HEIGHT-rndcellrad-25), rndcellrad);

        randomCell.cellColor = Color.BLUE;
        celllist.add(randomCell);

        cellThread();

        runGameThread();
    }

    //Randomly creates cells in random places
    public void cellThread(){
        Thread cellthread = new Thread(){
            @Override
            public void run(){
                while(true){
                    if(celllist.size()<30) {
                        coloredCell = new Cell(random.nextInt(MainClass.SCREEN_WIDTH - rndcellrad), random.nextInt(MainClass.SCREEN_HEIGHT - rndcellrad - 25), rndcellrad);
                        coloredCell.cellColor = colors[random.nextInt(colors.length)];
                        celllist.add(coloredCell);
                    }
                    repaint();
                    try {
                        Thread.sleep(random.nextInt(12)*1000);//From 0 to 12 seconds it creates random cells
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        cellthread.start();
    }
    //Game runs in this thread
    public void runGameThread() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    playerCell.updateCellPos(right, left, up, down);//Position of the cell is updated
                    repaint();//Draw everything again when thread is executed
                    hud.getElapsedTime();//Each time thread executed, we get the elapsed time
                    for(int i = 0; i<celllist.size();i++) {
                        if (playerCell.isCollision(playerCell, celllist.get(i))) {//Removes the eaten cell and creates another one in a random place, increases the score
                            hud.score++;
                            celllist.remove(i);
                            if(celllist.size()<20) {
                                coloredCell = new Cell(random.nextInt(MainClass.SCREEN_WIDTH - rndcellrad), random.nextInt(MainClass.SCREEN_HEIGHT - rndcellrad - 25), rndcellrad);
                                coloredCell.cellColor = colors[random.nextInt(colors.length)];
                                celllist.add(coloredCell);
                            }
                            //Increases the size of the player cell based on volume eaten
                            if(playerCell.cellRad == (int)Math.pow(Math.pow(playerCell.cellRad, 3) + Math.pow(celllist.get(i).cellRad, 3), 1.0 / 3)){
                                playerCell.cellRad+=1;
                            }
                            else
                                playerCell.cellRad = (int)Math.pow(Math.pow(playerCell.cellRad, 3) + Math.pow(celllist.get(i).cellRad, 3), 1.0 / 3);
                        }
                    }
                    //Surprise here ^^
                    if(playerCell.cellRad>400 && mus==0) {
                        hud.resetTime();
                        mus++;
                        music.playSound();
                    }
                    try {
                        Thread.sleep(10);//Value inside it changes the speed of the game, it runs in 10 ms
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        Graphics2D g2d = (Graphics2D) g;

        background.drawBackground(g2d);

        for(int i=0;i<celllist.size();i++) {
            celllist.get(i).drawCell(g2d, rndcellrad);
        }

        playerCell.drawCell(g2d, playerCell.cellRad);

        g.setColor(Color.BLUE);

        g.drawString("Score " + hud.score, 10, 20);

        g.drawString("Elapsed Time " + hud.elapsedTime / 1000, 490, 20);

        g.drawString("Kamil", playerCell.getX()+playerCell.cellRad-cellrad,playerCell.getY()+playerCell.cellRad);//Name inside the cell

        if(hud.elapsedTime/1000>5 && hud.elapsedTime/1000<8 && mus!=0){
            g.setColor(Color.WHITE);
            g.setFont(new Font("", Font.BOLD,200));
            g.drawString("TOO EASY???",0, (MainClass.SCREEN_HEIGHT) / 2);
        }
        else if(hud.elapsedTime/1000>=8 && hud.elapsedTime/1000<13 && mus!=0){
            g.setColor(Color.WHITE);
            g.setFont(new Font("", Font.BOLD,130));
            g.drawString("YOU KNOW WHAT :)",0, (MainClass.SCREEN_HEIGHT) / 2);
        }
        else if(hud.elapsedTime/1000>=14 && hud.elapsedTime/1000<17 && mus!=0)
        {
            g.setColor(Color.WHITE);
            g.setFont(new Font("", Font.BOLD,80));
            g.drawString("FIRE IT LOUD",0, (MainClass.SCREEN_HEIGHT) / 2);
            g.setFont(new Font("", Font.BOLD,80));
            g.drawString("ANOTHER ROUND OF SHOTS",0, (MainClass.SCREEN_HEIGHT + 160) / 2);
        }
        else if(hud.elapsedTime/1000>17 && mus!=0)
        {
            g.setColor(Color.WHITE);
            g.setFont(new Font("", Font.BOLD,150));
            g.drawString("TURN DOWN FOR",0, (MainClass.SCREEN_HEIGHT) / 2);
            g.drawString("WHAT",0, (MainClass.SCREEN_HEIGHT + 300) / 2);
            g.setFont(new Font("", Font.BOLD,50));
            g.drawString("HA HA HA!",random.nextInt(MainClass.SCREEN_WIDTH), random.nextInt(MainClass.SCREEN_WIDTH));
        }
    }

    //Below part is for using the keyboard as controller
    //W A S D and Arrow keys
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT:
                right = true;
                break;
            case KeyEvent.VK_D:
                right = true;
                break;
            case KeyEvent.VK_LEFT:
                left = true;
                break;
            case KeyEvent.VK_A:
                left = true;
                break;
            case KeyEvent.VK_UP:
                up = true;
                break;
            case KeyEvent.VK_W:
                up = true;
                break;
            case KeyEvent.VK_DOWN:
                down = true;
                break;
            case KeyEvent.VK_S:
                down = true;
                break;
            case KeyEvent.VK_ESCAPE:
                int confirmed = JOptionPane.showConfirmDialog(null, "Are you sure you want to return back to Menu?",
                        "Exit Program Message Box", JOptionPane.YES_NO_OPTION);
                if (confirmed == JOptionPane.YES_OPTION) {
                    mainClass.mainPanel = new MainPanel(mainClass);
                    mainClass.getContentPane().removeAll();
                    mainClass.getContentPane().add(mainClass.mainPanel);
                    mainClass.mainPanel.requestFocusInWindow();
                    mainClass.revalidate();
                    break;
                }
                else {
                    break;
                }

        }
    }
    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT:
                right = false;
                break;
            case KeyEvent.VK_D:
                right = false;
                break;
            case KeyEvent.VK_LEFT:
                left = false;
                break;
            case KeyEvent.VK_A:
                left = false;
                break;
            case KeyEvent.VK_UP:
                up = false;
                break;
            case KeyEvent.VK_W:
                up = false;
                break;
            case KeyEvent.VK_DOWN:
                down = false;
                break;
            case KeyEvent.VK_S:
                down = false;
                break;
        }
    }
}