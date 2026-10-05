/** Runs a single game: alternates turns and notifies a listener after every move. */
public class Game {

    private Board board;
    private Player player1;
    private Player player2;
    private int currentTurn;
    private GameListener listener;
    private volatile boolean stopped = false; // Written by the GUI thread, read by the game thread.

    public Game(Board board, Player player1, Player player2, GameListener listener) {
        this.board = board;
        this.player1 = player1;
        this.player2 = player2;
        this.listener = listener;
        this.currentTurn = player1.getId();
    }

    /** Game loop; blocks the calling thread until someone wins, the board is full, or {@link #stop()} is called. */
    public void run() {
        boolean gameOver = false;

        while (!gameOver && !stopped) {

            Player currentPlayer = (currentTurn == player1.getId()) ? player1 : player2;

            int column = currentPlayer.chooseMove(board);

            // The game may have been abandoned while the player was thinking: don't play that move.
            if (!stopped) {
                board.dropDisc(column, currentPlayer.getId());

                if (listener != null) {
                    // Pass a snapshot: the listener animates later, and must not see moves made in the meantime.
                    listener.onMove(board.copy(), column, currentPlayer.getId());
                }

                if (board.hasWon(currentPlayer.getId())) {
                    gameOver = true;
                    if (listener != null) {
                        listener.onGameOver(currentPlayer.getId());
                    }
                } else if (board.isFull()) {
                    gameOver = true;
                    if (listener != null) {
                        listener.onGameOver(0);
                    }
                } else {
                    currentTurn = (currentTurn == player1.getId()) ? player2.getId() : player1.getId();
                }
            }
        }
    }

    public void stop() {
        stopped = true;
    }

    /** Callbacks invoked on the game thread; winnerId is 0 for a draw. */
    public interface GameListener {
        void onMove(Board board, int column, int playerId);

        void onGameOver(int winnerId);
    }
}
