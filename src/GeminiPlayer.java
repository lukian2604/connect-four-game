/** Player driven by a remote LLM (Gemini): the board is sent as text and the reply parsed as a column. */
public class GeminiPlayer implements Player {

    private int id;
    private int opponentId;
    private GeminiClient client;
    private boolean errorAlreadyShown = false;

    public GeminiPlayer(int id, GeminiClient client) {
        this.id = id;
        this.opponentId = (id == 1) ? 2 : 1;
        this.client = client;
    }

    /** Asks the model for a move; falls back to the first free column on errors or invalid answers. */
    public int chooseMove(Board board) {
        String prompt = buildPrompt(board);
        int chosenColumn = -1;

        try {
            String reply = client.ask(prompt);
            int column = extractColumn(reply);

            if (board.isValidColumn(column)) {
                chosenColumn = column;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("AI request failed: " + e.getMessage());

            // Show the error dialog only once per game, not on every move.
            if (!errorAlreadyShown) {
                errorAlreadyShown = true;
                String message = "AI mode is not working:\n" + e.getMessage()
                        + "\n\nFor this game a fallback move will be used (first free column).";
                javax.swing.SwingUtilities.invokeLater(() -> javax.swing.JOptionPane.showMessageDialog(null,
                        message, "AI mode error", javax.swing.JOptionPane.WARNING_MESSAGE));
            }
        }

        if (chosenColumn == -1) {
            chosenColumn = firstValidColumn(board);
        }

        return chosenColumn;
    }

    private String buildPrompt(Board board) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are playing Connect Four. You are player ").append(id)
                .append(", your opponent is player ").append(opponentId).append(".\n");
        sb.append("The board has ").append(board.getRows()).append(" rows and ")
                .append(board.getColumns()).append(" columns, columns are indexed from 0.\n");
        sb.append("0 = empty cell, 1 = disc of player 1, 2 = disc of player 2.\n");
        sb.append("Current board (from the top row to the bottom row):\n");

        for (int row = 0; row < board.getRows(); row++) {
            for (int column = 0; column < board.getColumns(); column++) {
                sb.append(board.getCell(row, column));
            }
            sb.append("\n");
        }

        sb.append("Reply ONLY with the number of the column you want to play (an integer from 0 to ")
                .append(board.getColumns() - 1).append("), with no other text.");

        return sb.toString();
    }

    /** Returns the first integer found in the reply, or -1 if there is none. */
    private int extractColumn(String reply) {
        int column = -1;

        if (reply != null) {
            StringBuilder number = new StringBuilder();
            boolean finished = false;
            int i = 0;

            while (i < reply.length() && !finished) {
                char c = reply.charAt(i);
                if (Character.isDigit(c)) {
                    number.append(c);
                } else if (number.length() > 0) {
                    finished = true;
                }
                i++;
            }

            if (number.length() > 0) {
                column = Integer.parseInt(number.toString());
            }
        }

        return column;
    }

    private int firstValidColumn(Board board) {
        int foundColumn = -1;
        int column = 0;

        while (column < board.getColumns() && foundColumn == -1) {
            if (board.isValidColumn(column)) {
                foundColumn = column;
            }
            column++;
        }

        return foundColumn;
    }

    public int getId() {
        return id;
    }
}
