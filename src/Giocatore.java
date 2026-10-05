/** A Connect Four player: anything that can pick a column to play. */
public interface Giocatore {

    /** Returns the column to play on the given grid, or -1 if the game was abandoned. */
    int sceglieMossa(Griglia griglia);

    int getId();
}
