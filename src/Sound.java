import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

/**
 * Sound class : Takes the sound file as a string and plays it using javax.sound.sampled
 * Also supports programmatic tone generation via playEatSound() — no .wav file required
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
     * playEatSound : Generates and plays a short descending "bloop" tone (600→200 Hz, 150ms)
     * entirely programmatically — no .wav file needed. Runs on a daemon thread.
     * Respects the soundEnabled toggle.
     */
    public static void playEatSound() {
        if (!soundEnabled) return;
        Thread t = new Thread(() -> {
            try {
                AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
                line.open(fmt);
                line.start();
                int samples = 44100 * 150 / 1000; // 150 ms
                byte[] buf = new byte[samples * 2];
                for (int i = 0; i < samples; i++) {
                    float freq = 600f - (400f * i / samples); // sweep 600 Hz → 200 Hz
                    float amp  = 0.7f * (1f - (float) i / samples); // linear decay
                    short s = (short)(amp * Short.MAX_VALUE
                            * Math.sin(2 * Math.PI * freq * i / 44100.0));
                    buf[i * 2]     = (byte)(s & 0xff);
                    buf[i * 2 + 1] = (byte)((s >> 8) & 0xff);
                }
                line.write(buf, 0, buf.length);
                line.drain();
                line.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
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
