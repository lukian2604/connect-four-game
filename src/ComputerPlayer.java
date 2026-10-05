/** Computer opponent backed by the local minimax engine ({@link MinimaxAI}). */
public class ComputerPlayer implements Player {

    private int id;
    private MinimaxAI ai;

    public ComputerPlayer(int id, int maxDepth) {
        this.id = id;
        this.ai = new MinimaxAI(maxDepth, id);
    }

    public int chooseMove(Board board) {
        return ai.chooseMove(board);
    }

    public int getId() {
        return id;
    }
}
