import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/** Sound effects synthesized at startup (no audio files) and played on a background thread. */
public class Suoni {

    private static final float CAMPIONAMENTO = 44100;
    // 16-bit mono: at 8 bits the waveform sounded harsh.
    private static final AudioFormat FORMATO = new AudioFormat(CAMPIONAMENTO, 16, 1, true, false);
    // Small buffer (~45 ms) to keep latency low.
    private static final int DIMENSIONE_BUFFER = 2048 * 2;

    // Single daemon thread: sounds play in order and never block application exit.
    private static final ExecutorService esecutore = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Suoni");
        t.setDaemon(true);
        return t;
    });

    // Kept open for the whole session: opening a line per sound added ~100 ms of latency on macOS.
    // Only touched by the "Suoni" thread, so no synchronization is needed.
    private static SourceDataLine linea;
    private static boolean audioNonDisponibile = false;

    private static final byte[] SUONO_MOSSA = inByte(generaToc());
    private static final byte[] SUONO_VITTORIA = inByte(generaArpeggio());

    static {
        // Open the line right away so that even the first sound is on time.
        esecutore.execute(Suoni::apriLinea);
    }

    public static void riproduciMossa() {
        esecutore.execute(() -> riproduci(SUONO_MOSSA));
    }

    public static void riproduciVittoria() {
        esecutore.execute(() -> riproduci(SUONO_VITTORIA));
    }

    /** A short wooden "tock": a falling-pitch sine plus a burst of filtered noise. */
    private static double[] generaToc() {
        int numCampioni = (int) (0.14 * CAMPIONAMENTO);
        double[] campioni = new double[numCampioni];
        Random rumore = new Random(7); // Fixed seed: the sound is identical on every run.
        double fase = 0;
        double rumoreFiltrato = 0;

        for (int i = 0; i < numCampioni; i++) {
            double t = i / CAMPIONAMENTO;
            double frequenza = 140 + 90 * Math.exp(-t / 0.02);
            fase += 2 * Math.PI * frequenza / CAMPIONAMENTO;

            double attacco = Math.min(1.0, t / 0.002);
            double corpo = Math.sin(fase) * Math.exp(-t / 0.035);
            double legno = 0.25 * Math.sin(fase * 2.7) * Math.exp(-t / 0.012);

            rumoreFiltrato += 0.25 * ((rumore.nextDouble() * 2 - 1) - rumoreFiltrato);
            double click = 0.5 * rumoreFiltrato * Math.exp(-t / 0.004);

            campioni[i] = attacco * (corpo + legno + click);
        }

        return normalizza(campioni, 0.45);
    }

    /** Rising C-major arpeggio played on a win. */
    private static double[] generaArpeggio() {
        double[] note = {523.25, 659.25, 783.99, 1046.50};
        double distanza = 0.11;
        double codaFinale = 1.1;
        int numCampioni = (int) ((distanza * (note.length - 1) + codaFinale) * CAMPIONAMENTO);
        double[] campioni = new double[numCampioni];

        for (int n = 0; n < note.length; n++) {
            int inizio = (int) (n * distanza * CAMPIONAMENTO);
            double smorzamento = (n == note.length - 1) ? 0.45 : 0.22;

            for (int i = inizio; i < numCampioni; i++) {
                double t = (i - inizio) / CAMPIONAMENTO;
                double angolo = 2 * Math.PI * note[n] * t;
                double attacco = Math.min(1.0, t / 0.006);
                double tono = Math.sin(angolo) + 0.3 * Math.sin(2 * angolo) + 0.08 * Math.sin(3 * angolo);
                campioni[i] += attacco * tono * Math.exp(-t / smorzamento);
            }
        }

        int dissolvenza = (int) (0.05 * CAMPIONAMENTO);
        for (int i = 0; i < dissolvenza; i++) {
            campioni[numCampioni - 1 - i] *= i / (double) dissolvenza;
        }

        return normalizza(campioni, 0.3);
    }

    private static double[] normalizza(double[] campioni, double piccoMassimo) {
        double picco = 0;
        for (double c : campioni) {
            picco = Math.max(picco, Math.abs(c));
        }
        for (int i = 0; i < campioni.length; i++) {
            campioni[i] = campioni[i] / picco * piccoMassimo;
        }
        return campioni;
    }

    /** Converts samples in [-1, 1] to 16-bit little-endian PCM. */
    private static byte[] inByte(double[] campioni) {
        byte[] buffer = new byte[campioni.length * 2];
        for (int i = 0; i < campioni.length; i++) {
            short campione = (short) (campioni[i] * Short.MAX_VALUE);
            buffer[2 * i] = (byte) campione;
            buffer[2 * i + 1] = (byte) (campione >> 8);
        }
        return buffer;
    }

    private static void apriLinea() {
        try {
            linea = AudioSystem.getSourceDataLine(FORMATO);
            linea.open(FORMATO, DIMENSIONE_BUFFER);
            linea.start();
        } catch (Exception e) {
            audioNonDisponibile = true;
        }
    }

    private static void riproduci(byte[] suono) {
        if (linea == null && !audioNonDisponibile) {
            apriLinea();
        }
        if (!audioNonDisponibile) {
            linea.write(suono, 0, suono.length);
        }
    }
}
