/**
 * Human player for the Swing version: the game thread waits here until
 * {@link GrigliaPanel} reports a click through {@link #riceviClick(int)}.
 */
public class GiocatoreUmanoGUI implements Giocatore {

    private int id;
    private Integer colonnaCliccata;

    public GiocatoreUmanoGUI(int id) {
        this.id = id;
    }

    /** Blocks until a column is clicked; returns -1 if the game thread is interrupted. */
    public synchronized int sceglieMossa(Griglia griglia) {
        colonnaCliccata = null;

        boolean interrotto = false;

        while (colonnaCliccata == null && !interrotto) {
            try {
                wait();
            } catch (InterruptedException e) {
                interrotto = true;
                // Don't wait again: with the interrupt flag set, wait() would throw immediately.
                Thread.currentThread().interrupt();
            }
        }

        return interrotto ? -1 : colonnaCliccata;
    }

    public synchronized void riceviClick(int colonna) {
        colonnaCliccata = colonna;
        notify();
    }

    public int getId() {
        return id;
    }
}
