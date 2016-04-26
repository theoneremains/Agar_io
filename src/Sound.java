import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * Sound class : Takes the sound file as a string given with the address location on your computer and plays it
 * @author Kamil Yunus Özkaya
 */
public class Sound {

    AudioStream audioStream;

    public int soundSeconds = 0;

    public String soundFile;

    public Sound(String soundFile,int soundSeconds)
    {
        this.soundFile = soundFile;

        this.soundSeconds = soundSeconds;
    }

    /**
     * playSound : Starts the sound file
     */
    public void playSound()
    {
        InputStream inputStream = getClass().getResourceAsStream(soundFile);
        try
        {
            audioStream = new AudioStream(inputStream);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        AudioPlayer.player.start(audioStream);
    }

    /**
     * closeSound : If needed, closes the music
     * Not used in the game
     */
    public void closeSound()
    {
        AudioPlayer.player.stop(audioStream);
    }

    /**
     * runSoundThread : Lets the music repeated in the background via a thread, in given seconds
     * If you want to put a music you can call the sound file using this method
     * Not used in the game
     */
    public void runSoundThread()
    {
        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                while(true)
                {
                    if(soundSeconds != 0)
                        playSound();
                    try
                    {
                        Thread.sleep(soundSeconds * 1000);
                        AudioPlayer.player.stop(audioStream);
                    }
                    catch(InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }
}
