/**
 * HUD : Heads-up display data — tracks score and elapsed time.
 * Used by GamePanel and GameRenderer to display game statistics.
 * @author Kamil Yunus Özkaya
 */
public class HUD {

    public long startTime = System.currentTimeMillis();
    public long elapsedTime;
    public int score = 0;

    /** Updates the elapsed time from the system clock */
    public void updateElapsedTime() {
        elapsedTime = System.currentTimeMillis() - startTime;
    }

    /** Resets the timer to the current time (used by easter egg) */
    public void resetTime() {
        startTime = System.currentTimeMillis();
    }
}
