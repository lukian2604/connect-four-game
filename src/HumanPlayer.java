/**
 * Human player for the Swing version: the game thread waits here until
 * {@link BoardPanel} reports a click through {@link #receiveClick(int)}.
 */
public class HumanPlayer implements Player {

    private int id;
    private Integer clickedColumn;

    public HumanPlayer(int id) {
        this.id = id;
    }

    /** Blocks until a column is clicked; returns -1 if the game thread is interrupted. */
    public synchronized int chooseMove(Board board) {
        clickedColumn = null;

        boolean interrupted = false;

        while (clickedColumn == null && !interrupted) {
            try {
                wait();
            } catch (InterruptedException e) {
                interrupted = true;
                // Don't wait again: with the interrupt flag set, wait() would throw immediately.
                Thread.currentThread().interrupt();
            }
        }

        return interrupted ? -1 : clickedColumn;
    }

    public synchronized void receiveClick(int column) {
        clickedColumn = column;
        notify();
    }

    public int getId() {
        return id;
    }
}
