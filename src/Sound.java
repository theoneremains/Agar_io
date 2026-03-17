import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

/**
 * Sound class : Audio playback for .wav files and programmatic tone generation.
 * Uses ToneGenerator for all synthesized sound effects (eat, click, bounce, division).
 * Menu and game ambient sounds return a SourceDataLine that callers must stop/close.
 * @author Kamil Yunus Özkaya
 */
public class Sound {

    /** Global sound toggle — when false, all sound methods no-op */
    public static boolean soundEnabled = true;

    private Clip clip;
    public int soundSeconds = 0;
    public String soundFile;

    public Sound(String soundFile, int soundSeconds) {
        this.soundFile = soundFile;
        this.soundSeconds = soundSeconds;
    }

    /** Plays the .wav file. Respects soundEnabled toggle. */
    public void playSound() {
        if (!soundEnabled) return;
        try {
            URL url = getClass().getResource(soundFile);
            if (url == null) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }

    /** Stops the currently playing .wav clip */
    public void closeSound() {
        if (clip != null && clip.isRunning()) clip.stop();
    }

    /**
     * Loops the .wav sound on a daemon thread.
     * Re-plays every soundSeconds until the thread is stopped.
     */
    public void runSoundThread() {
        Thread thread = new Thread(() -> {
            while (true) {
                if (soundSeconds != 0) playSound();
                try {
                    Thread.sleep(soundSeconds * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                closeSound();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ── Programmatic Sound Effects (via ToneGenerator) ───────────────────

    /** Short descending "bloop" tone (600→200 Hz, 150ms) for eating a cell */
    public static void playEatSound() {
        if (!soundEnabled) return;
        ToneGenerator.playTone(150, (i, total) -> {
            float progress = (float) i / total;
            float freq = 600f - 400f * progress;
            float amp = 0.7f * (1f - progress);
            return amp * ToneGenerator.sine(freq, i);
        });
    }

    /** Rising dual-tone (200ms) for cell division events */
    public static void playDivisionSound() {
        if (!soundEnabled) return;
        ToneGenerator.playTone(200, (i, total) -> {
            float progress = (float) i / total;
            float freq1 = 300f + 400f * progress;
            float freq2 = 350f + 350f * progress;
            float amp = 0.5f * (1f - progress * 0.7f);
            return amp * 0.5 * (ToneGenerator.sine(freq1, i) + ToneGenerator.sine(freq2, i));
        });
    }

    /** Short bouncy wobble tone (100ms) for touching an uneatable cell */
    public static void playBounceSound() {
        if (!soundEnabled) return;
        ToneGenerator.playTone(100, (i, total) -> {
            float progress = (float) i / total;
            float freq = 400f + 200f * (float) Math.sin(Math.PI * progress * 3);
            float amp = 0.4f * (1f - progress);
            return amp * ToneGenerator.sine(freq, i);
        });
    }

    /** Quick rising whoosh (80ms) played when the player performs a Dodge dash */
    public static void playDodgeSound() {
        if (!soundEnabled) return;
        ToneGenerator.playTone(80, (i, total) -> {
            float progress = (float) i / total;
            float freq = 150f + 700f * progress;
            float amp = 0.55f * (1f - progress * 0.4f);
            return amp * ToneGenerator.sine(freq, i);
        });
    }

    /** Crisp descending click tone (60ms) for UI button interactions */
    public static void playClickSound() {
        if (!soundEnabled) return;
        ToneGenerator.playTone(60, (i, total) -> {
            float progress = (float) i / total;
            float freq = 800f - 200f * progress;
            float amp = 0.5f * (1f - progress) * (1f - progress);
            return amp * ToneGenerator.sine(freq, i);
        });
    }

    // ── Ambient Sound Streams ────────────────────────────────────────────

    /**
     * Soothing A major chord pad with slow breathing modulation for the main menu.
     * @return the SourceDataLine (call stop()/close() to end), or null if sound is off
     */
    public static SourceDataLine playMenuAmbient() {
        if (!soundEnabled) return null;
        return ToneGenerator.playAmbient((i, ignored) -> {
            double time = (double) i / GameConstants.SAMPLE_RATE;
            // A major chord: A3, C#4, E4
            double wave = 0.15 * ToneGenerator.sine(220, i);
            wave += 0.12 * ToneGenerator.sine(277.18, i);
            wave += 0.10 * ToneGenerator.sine(329.63, i);
            // Slow amplitude modulation for breathing effect
            double mod = 0.5 + 0.5 * Math.sin(2 * Math.PI * 0.15 * time);
            return wave * mod * 0.3;
        });
    }

    /**
     * Evolving pentatonic chord progression for in-game atmosphere.
     * Cycles through C-G-C4, D-A-D4, E-B-E4 with crossfading.
     * @return the SourceDataLine (call stop()/close() to end), or null if sound is off
     */
    public static SourceDataLine playGameAmbient() {
        if (!soundEnabled) return null;

        final double[][] chords = {
            {130.81, 196.00, 261.63},  // C3, G3, C4
            {146.83, 220.00, 293.66},  // D3, A3, D4
            {164.81, 246.94, 329.63},  // E3, B3, E4
            {130.81, 196.00, 329.63},  // C3, G3, E4
        };
        final double chordDuration = 8.0;

        return ToneGenerator.playAmbient((i, ignored) -> {
            double time = (double) i / GameConstants.SAMPLE_RATE;
            double chordProgress = (time % (chordDuration * chords.length)) / chordDuration;
            int chordIdx = (int) chordProgress % chords.length;
            int nextChordIdx = (chordIdx + 1) % chords.length;
            double blend = chordProgress - chordIdx;
            double crossfade = blend > 0.8 ? (blend - 0.8) / 0.2 : 0.0;

            double[] curr = chords[chordIdx];
            double[] next = chords[nextChordIdx];

            double wave = 0;
            double[] amps = {0.10, 0.08, 0.06};
            for (int n = 0; n < curr.length; n++) {
                double a = amps[n] * (1.0 - crossfade);
                wave += a * ToneGenerator.sine(curr[n], i);
                wave += a * 0.15 * ToneGenerator.sine(curr[n] * 2, i);
            }
            for (int n = 0; n < next.length; n++) {
                double a = amps[n] * crossfade;
                wave += a * ToneGenerator.sine(next[n], i);
                wave += a * 0.15 * ToneGenerator.sine(next[n] * 2, i);
            }
            double mod  = 0.6 + 0.4 * Math.sin(2 * Math.PI * 0.12 * time);
            double mod2 = 0.8 + 0.2 * Math.sin(2 * Math.PI * 0.03 * time);
            return wave * mod * mod2 * 0.2;
        });
    }
}
