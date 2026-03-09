import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

/**
 * Sound class : Takes the sound file as a string and plays it using javax.sound.sampled
 * @author Kamil Yunus Özkaya
 */
public class Sound {

    public static boolean soundEnabled = true;

    private Clip clip;

    public int soundSeconds = 0;

    public String soundFile;

    public Sound(String soundFile, int soundSeconds)
    {
        this.soundFile = soundFile;

        this.soundSeconds = soundSeconds;
    }

    /**
     * playSound : Starts the sound file. Respects the soundEnabled toggle.
     */
    public void playSound()
    {
        if (!soundEnabled) return;
        try
        {
            URL url = getClass().getResource(soundFile);
            if (url == null) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();
        }
        catch (UnsupportedAudioFileException | LineUnavailableException | IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * closeSound : Stops the currently playing sound clip
     */
    public void closeSound()
    {
        if (clip != null && clip.isRunning()) clip.stop();
    }

    /**
     * runSoundThread : Lets the music repeat in the background via a daemon thread
     * If you want to put looping music, call this method instead of playSound()
     */
    public void runSoundThread()
    {
        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                while (true)
                {
                    if (soundSeconds != 0) playSound();
                    try
                    {
                        Thread.sleep(soundSeconds * 1000L);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    closeSound();
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }
}
