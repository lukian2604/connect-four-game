/** Computer opponent backed by the local minimax engine ({@link IA}). */
public class GiocatoreComputer implements Giocatore {

    private int id;
    private IA ia;

    public GiocatoreComputer(int id, int profonditaMax) {
        this.id = id;
        this.ia = new IA(profonditaMax, id);
    }

    public int sceglieMossa(Griglia griglia) {
        return ia.sceglieMossa(griglia);
    }

    public int getId() {
        return id;
    }
}
