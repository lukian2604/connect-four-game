/** A Connect Four player: anything that can pick a column to play. */
public interface Player {

    /** Returns the column to play on the given board, or -1 if the game was abandoned. */
    int chooseMove(Board board);

    int getId();
}
