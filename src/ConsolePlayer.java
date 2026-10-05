import java.util.Scanner;

/** Human player for the console version: reads the column from standard input. */
public class ConsolePlayer implements Player {

    private int id;
    private Scanner scanner;

    public ConsolePlayer(int id) {
        this.id = id;
        this.scanner = new Scanner(System.in);
    }

    public int chooseMove(Board board) {
        int column = -1;
        boolean valid = false;

        while (!valid) {
            System.out.print("Player " + id + ", choose a column (0-" + (board.getColumns() - 1) + "): ");
            column = scanner.nextInt();
            valid = board.isValidColumn(column);

            if (!valid) {
                System.out.println("Invalid column, try again.");
            }
        }

        return column;
    }

    public int getId() {
        return id;
    }
}
