import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/** Sound effects synthesized at startup (no audio files) and played on a background thread. */
public class Sounds {

    private static final float SAMPLE_RATE = 44100;
    // 16-bit mono: at 8 bits the waveform sounded harsh.
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    // Small buffer (~45 ms) to keep latency low.
    private static final int BUFFER_SIZE = 2048 * 2;

    // Single daemon thread: sounds play in order and never block application exit.
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Sounds");
        t.setDaemon(true);
        return t;
    });

    // Kept open for the whole session: opening a line per sound added ~100 ms of latency on macOS.
    // Only touched by the "Sounds" thread, so no synchronization is needed.
    private static SourceDataLine line;
    private static boolean audioUnavailable = false;

    private static final byte[] MOVE_SOUND = toBytes(generateTock());
    private static final byte[] WIN_SOUND = toBytes(generateArpeggio());

    static {
        // Open the line right away so that even the first sound is on time.
        executor.execute(Sounds::openLine);
    }

    public static void playMove() {
        executor.execute(() -> play(MOVE_SOUND));
    }

    public static void playWin() {
        executor.execute(() -> play(WIN_SOUND));
    }

    /** A short wooden "tock": a falling-pitch sine plus a burst of filtered noise. */
    private static double[] generateTock() {
        int sampleCount = (int) (0.14 * SAMPLE_RATE);
        double[] samples = new double[sampleCount];
        Random noise = new Random(7); // Fixed seed: the sound is identical on every run.
        double phase = 0;
        double filteredNoise = 0;

        for (int i = 0; i < sampleCount; i++) {
            double t = i / SAMPLE_RATE;
            double frequency = 140 + 90 * Math.exp(-t / 0.02);
            phase += 2 * Math.PI * frequency / SAMPLE_RATE;

            double attack = Math.min(1.0, t / 0.002);
            double body = Math.sin(phase) * Math.exp(-t / 0.035);
            double wood = 0.25 * Math.sin(phase * 2.7) * Math.exp(-t / 0.012);

            filteredNoise += 0.25 * ((noise.nextDouble() * 2 - 1) - filteredNoise);
            double click = 0.5 * filteredNoise * Math.exp(-t / 0.004);

            samples[i] = attack * (body + wood + click);
        }

        return normalize(samples, 0.45);
    }

    /** Rising C-major arpeggio played on a win. */
    private static double[] generateArpeggio() {
        double[] notes = {523.25, 659.25, 783.99, 1046.50};
        double spacing = 0.11;
        double finalTail = 1.1;
        int sampleCount = (int) ((spacing * (notes.length - 1) + finalTail) * SAMPLE_RATE);
        double[] samples = new double[sampleCount];

        for (int n = 0; n < notes.length; n++) {
            int start = (int) (n * spacing * SAMPLE_RATE);
            double decay = (n == notes.length - 1) ? 0.45 : 0.22;

            for (int i = start; i < sampleCount; i++) {
                double t = (i - start) / SAMPLE_RATE;
                double angle = 2 * Math.PI * notes[n] * t;
                double attack = Math.min(1.0, t / 0.006);
                double tone = Math.sin(angle) + 0.3 * Math.sin(2 * angle) + 0.08 * Math.sin(3 * angle);
                samples[i] += attack * tone * Math.exp(-t / decay);
            }
        }

        int fadeOut = (int) (0.05 * SAMPLE_RATE);
        for (int i = 0; i < fadeOut; i++) {
            samples[sampleCount - 1 - i] *= i / (double) fadeOut;
        }

        return normalize(samples, 0.3);
    }

    private static double[] normalize(double[] samples, double maxPeak) {
        double peak = 0;
        for (double s : samples) {
            peak = Math.max(peak, Math.abs(s));
        }
        for (int i = 0; i < samples.length; i++) {
            samples[i] = samples[i] / peak * maxPeak;
        }
        return samples;
    }

    /** Converts samples in [-1, 1] to 16-bit little-endian PCM. */
    private static byte[] toBytes(double[] samples) {
        byte[] buffer = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            short sample = (short) (samples[i] * Short.MAX_VALUE);
            buffer[2 * i] = (byte) sample;
            buffer[2 * i + 1] = (byte) (sample >> 8);
        }
        return buffer;
    }

    private static void openLine() {
        try {
            line = AudioSystem.getSourceDataLine(FORMAT);
            line.open(FORMAT, BUFFER_SIZE);
            line.start();
        } catch (Exception e) {
            audioUnavailable = true;
        }
    }

    private static void play(byte[] sound) {
        if (line == null && !audioUnavailable) {
            openLine();
        }
        if (!audioUnavailable) {
            line.write(sound, 0, sound.length);
        }
    }
}
