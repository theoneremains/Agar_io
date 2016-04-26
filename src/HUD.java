import javax.swing.*;


/**
 * HUD Class : For this game class only calculates the time and holds the value of score
 * Takes the current time of the system when the class is called
 * @author Kamil Yunus Özkaya
 */
public class HUD extends JPanel {
    public long startTime = System.currentTimeMillis();

    public long endTime;

    public long elapsedTime;

    public int score = 0;

    public void getElapsedTime() {
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
    }
    public void resetTime(){
        startTime = System.currentTimeMillis();
    }
}