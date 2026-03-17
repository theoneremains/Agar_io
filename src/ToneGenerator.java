import javax.sound.sampled.*;

/**
 * ToneGenerator : Programmatic audio tone generation utility.
 * Centralizes the common pattern of creating short sound effects
 * (eat, click, bounce, division) without requiring .wav files.
 * All tones are generated as 16-bit mono PCM audio at 44100 Hz.
 * @author Kamil Yunus Ozkaya
 */
public final class ToneGenerator {

    private ToneGenerator() {} // Prevent instantiation

    private static final int SAMPLE_RATE = GameConstants.SAMPLE_RATE;

    /**
     * Functional interface for defining a waveform.
     * Given a sample index (0-based) and total sample count, returns a value in [-1, 1].
     */
    @FunctionalInterface
    public interface Waveform {
        double sample(int index, int totalSamples);
    }

    /**
     * Plays a short tone on a daemon thread using the given waveform function.
     * @param durationMs duration in milliseconds
     * @param waveform function that produces sample values in [-1, 1]
     */
    public static void playTone(int durationMs, Waveform waveform) {
        Thread t = new Thread(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
                line.open(fmt);
                line.start();
                int samples = SAMPLE_RATE * durationMs / 1000;
                byte[] buf = new byte[samples * 2];
                for (int i = 0; i < samples; i++) {
                    double value = waveform.sample(i, samples);
                    short s = (short) (Math.max(-1.0, Math.min(1.0, value)) * Short.MAX_VALUE);
                    buf[i * 2]     = (byte) (s & 0xff);
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
     * Starts a continuous ambient audio stream on a daemon thread.
     * @param waveform function that produces sample values in [-1, 1]
     * @return the SourceDataLine (call stop()/close() to end), or null on failure
     */
    public static SourceDataLine playAmbient(Waveform waveform) {
        try {
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            final SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
            line.open(fmt, 8820);
            line.start();
            Thread t = new Thread(() -> {
                byte[] buf = new byte[4410]; // 50ms buffer
                long sample = 0;
                while (line.isOpen()) {
                    for (int i = 0; i < buf.length / 2; i++) {
                        double value = waveform.sample((int) (sample & Integer.MAX_VALUE), -1);
                        short s = (short) (Math.max(-1.0, Math.min(1.0, value)) * Short.MAX_VALUE);
                        buf[i * 2]     = (byte) (s & 0xff);
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
     * Helper: sine wave at a given frequency.
     */
    public static double sine(double freq, int sampleIndex) {
        return Math.sin(2 * Math.PI * freq * sampleIndex / SAMPLE_RATE);
    }

    /**
     * Helper: linear interpolation between two values.
     */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Safely stops and closes an audio line.
     */
    public static void stopLine(SourceDataLine line) {
        if (line != null) {
            try { line.stop(); line.close(); } catch (Exception ignored) {}
        }
    }
}
