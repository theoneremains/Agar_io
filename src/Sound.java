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

    /**
     * playMenuAmbient : Generates a gentle, continuous ambient pad sound for the main menu.
     * Uses layered sine waves with slow modulation for a soothing atmosphere.
     * Runs on a daemon thread and loops until stopped via the returned SourceDataLine.
     * Respects the soundEnabled toggle.
     * @return the SourceDataLine (call stop()/close() to end), or null if sound is off
     */
    public static javax.sound.sampled.SourceDataLine playMenuAmbient() {
        if (!soundEnabled) return null;
        try {
            AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
            final javax.sound.sampled.SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
            line.open(fmt, 8820);
            line.start();
            Thread t = new Thread(() -> {
                int sampleRate = 44100;
                long sample = 0;
                byte[] buf = new byte[4410]; // 50ms buffer
                while (line.isOpen()) {
                    for (int i = 0; i < buf.length / 2; i++) {
                        double time = (double) sample / sampleRate;
                        // Three soft sine tones forming a major chord
                        double wave = 0.15 * Math.sin(2 * Math.PI * 220 * time);  // A3
                        wave += 0.12 * Math.sin(2 * Math.PI * 277.18 * time);     // C#4
                        wave += 0.10 * Math.sin(2 * Math.PI * 329.63 * time);     // E4
                        // Slow amplitude modulation for breathing effect
                        double mod = 0.5 + 0.5 * Math.sin(2 * Math.PI * 0.15 * time);
                        wave *= mod * 0.3;
                        short s = (short) (wave * Short.MAX_VALUE);
                        buf[i * 2] = (byte) (s & 0xff);
                        buf[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
                        sample++;
                    }
                    try {
                        line.write(buf, 0, buf.length);
                    } catch (Exception e) {
                        break;
                    }
                }
            });
            t.setDaemon(true);
            t.start();
            return line;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * playGameAmbient : Generates a soothing, evolving ambient soundscape for in-game atmosphere.
     * Features slowly cycling pentatonic chords with gentle modulation and harmonic layering.
     * Distinct from the menu ambient — warmer, more spacious, with subtle chord progression.
     * Runs on a daemon thread.
     * @return the SourceDataLine (call stop()/close() to end), or null if sound is off
     */
    public static javax.sound.sampled.SourceDataLine playGameAmbient() {
        if (!soundEnabled) return null;
        try {
            AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
            final javax.sound.sampled.SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
            line.open(fmt, 8820);
            line.start();
            Thread t = new Thread(() -> {
                int sampleRate = 44100;
                long sample = 0;
                byte[] buf = new byte[4410];
                // Pentatonic chord progression (soothing and non-dissonant)
                double[][] chords = {
                    {130.81, 196.00, 261.63},  // C3, G3, C4
                    {146.83, 220.00, 293.66},  // D3, A3, D4
                    {164.81, 246.94, 329.63},  // E3, B3, E4
                    {130.81, 196.00, 329.63},  // C3, G3, E4
                };
                double chordDuration = 8.0; // seconds per chord
                while (line.isOpen()) {
                    for (int i = 0; i < buf.length / 2; i++) {
                        double time = (double) sample / sampleRate;
                        double chordProgress = (time % (chordDuration * chords.length)) / chordDuration;
                        int chordIdx = (int) chordProgress % chords.length;
                        int nextChordIdx = (chordIdx + 1) % chords.length;
                        double blend = chordProgress - chordIdx;
                        double crossfade = blend > 0.8 ? (blend - 0.8) / 0.2 : 0.0;

                        double[] curr = chords[chordIdx];
                        double[] next = chords[nextChordIdx];

                        double wave = 0;
                        for (int n = 0; n < curr.length; n++) {
                            double amp = (n == 0 ? 0.10 : n == 1 ? 0.08 : 0.06) * (1.0 - crossfade);
                            wave += amp * Math.sin(2 * Math.PI * curr[n] * time);
                            wave += amp * 0.15 * Math.sin(2 * Math.PI * curr[n] * 2 * time);
                        }
                        for (int n = 0; n < next.length; n++) {
                            double amp = (n == 0 ? 0.10 : n == 1 ? 0.08 : 0.06) * crossfade;
                            wave += amp * Math.sin(2 * Math.PI * next[n] * time);
                            wave += amp * 0.15 * Math.sin(2 * Math.PI * next[n] * 2 * time);
                        }
                        double mod = 0.6 + 0.4 * Math.sin(2 * Math.PI * 0.12 * time);
                        double mod2 = 0.8 + 0.2 * Math.sin(2 * Math.PI * 0.03 * time);
                        wave *= mod * mod2 * 0.2;
                        short s = (short) (wave * Short.MAX_VALUE);
                        buf[i * 2] = (byte) (s & 0xff);
                        buf[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
                        sample++;
                    }
                    try {
                        line.write(buf, 0, buf.length);
                    } catch (Exception e) {
                        break;
                    }
                }
            });
            t.setDaemon(true);
            t.start();
            return line;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * playDivisionSound : Generates a short splitting/tearing tone for cell division.
     * Rising dual-tone with stereo-like wobble. Runs on a daemon thread.
     * Respects the soundEnabled toggle.
     */
    public static void playDivisionSound() {
        if (!soundEnabled) return;
        Thread t = new Thread(() -> {
            try {
                AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
                javax.sound.sampled.SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
                line.open(fmt);
                line.start();
                int samples = 44100 * 200 / 1000; // 200ms
                byte[] buf = new byte[samples * 2];
                for (int i = 0; i < samples; i++) {
                    float progress = (float) i / samples;
                    float freq1 = 300f + 400f * progress; // rising 300->700
                    float freq2 = 350f + 350f * progress; // rising 350->700
                    float amp = 0.5f * (1f - progress * 0.7f);
                    double wave = amp * 0.5 * Math.sin(2 * Math.PI * freq1 * i / 44100.0);
                    wave += amp * 0.5 * Math.sin(2 * Math.PI * freq2 * i / 44100.0);
                    short s = (short) (wave * Short.MAX_VALUE);
                    buf[i * 2] = (byte) (s & 0xff);
                    buf[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
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
     * playBounceSound : Generates a short bouncy tone for when a cell contacts
     * another cell that it cannot eat. Respects the soundEnabled toggle.
     */
    public static void playBounceSound() {
        if (!soundEnabled) return;
        Thread t = new Thread(() -> {
            try {
                AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
                javax.sound.sampled.SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
                line.open(fmt);
                line.start();
                int samples = 44100 * 100 / 1000; // 100ms
                byte[] buf = new byte[samples * 2];
                for (int i = 0; i < samples; i++) {
                    float progress = (float) i / samples;
                    float freq = 400f + 200f * (float) Math.sin(Math.PI * progress * 3);
                    float amp = 0.4f * (1f - progress);
                    short s = (short) (amp * Short.MAX_VALUE
                            * Math.sin(2 * Math.PI * freq * i / 44100.0));
                    buf[i * 2] = (byte) (s & 0xff);
                    buf[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
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
     * playClickSound : Generates a short, crisp click tone for UI button interactions.
     * Respects the soundEnabled toggle.
     */
    public static void playClickSound() {
        if (!soundEnabled) return;
        Thread t = new Thread(() -> {
            try {
                AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
                javax.sound.sampled.SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
                line.open(fmt);
                line.start();
                int samples = 44100 * 60 / 1000; // 60ms
                byte[] buf = new byte[samples * 2];
                for (int i = 0; i < samples; i++) {
                    float progress = (float) i / samples;
                    float freq = 800f - 200f * progress;
                    float amp = 0.5f * (1f - progress) * (1f - progress);
                    short s = (short) (amp * Short.MAX_VALUE
                            * Math.sin(2 * Math.PI * freq * i / 44100.0));
                    buf[i * 2] = (byte) (s & 0xff);
                    buf[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
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
}
