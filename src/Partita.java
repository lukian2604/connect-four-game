/** Runs a single game: alternates turns and notifies a listener after every move. */
public class Partita {

    private Griglia griglia;
    private Giocatore giocatore1;
    private Giocatore giocatore2;
    private int turnoCorrente;
    private PartitaListener listener;
    private volatile boolean fermata = false; // Written by the GUI thread, read by the game thread.

    public Partita(Griglia griglia, Giocatore giocatore1, Giocatore giocatore2, PartitaListener listener) {
        this.griglia = griglia;
        this.giocatore1 = giocatore1;
        this.giocatore2 = giocatore2;
        this.listener = listener;
        this.turnoCorrente = giocatore1.getId();
    }

    /** Game loop; blocks the calling thread until someone wins, the grid is full, or {@link #ferma()} is called. */
    public void avvia() {
        boolean partitaFinita = false;

        while (!partitaFinita && !fermata) {

            Giocatore giocatoreAttuale = (turnoCorrente == giocatore1.getId()) ? giocatore1 : giocatore2;

            int colonna = giocatoreAttuale.sceglieMossa(griglia);

            // The game may have been abandoned while the player was thinking: don't play that move.
            if (!fermata) {
                griglia.inserisciGettone(colonna, giocatoreAttuale.getId());

                if (listener != null) {
                    // Pass a snapshot: the listener animates later, and must not see moves made in the meantime.
                    listener.onMossa(griglia.clonaGriglia(), colonna, giocatoreAttuale.getId());
                }

                if (griglia.verificaVittoria(giocatoreAttuale.getId())) {
                    partitaFinita = true;
                    if (listener != null) {
                        listener.onFinePartita(giocatoreAttuale.getId());
                    }
                } else if (griglia.grigliaPiena()) {
                    partitaFinita = true;
                    if (listener != null) {
                        listener.onFinePartita(0);
                    }
                } else {
                    turnoCorrente = (turnoCorrente == giocatore1.getId()) ? giocatore2.getId() : giocatore1.getId();
                }
            }
        }
    }

    public void ferma() {
        fermata = true;
    }

    /** Callbacks invoked on the game thread; idVincitore is 0 for a draw. */
    public interface PartitaListener {
        void onMossa(Griglia griglia, int colonna, int idGiocatore);

        void onFinePartita(int idVincitore);
    }
}
