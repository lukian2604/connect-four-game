/** Minimax engine with alpha-beta pruning and a heuristic board evaluation. */
public class MinimaxAI {

    private int maxDepth;
    private int aiId;
    private int opponentId;

    private static final int WIN_SCORE = 1000000;
    private static final int LOSS_SCORE = -1000000;

    public MinimaxAI(int maxDepth, int aiId) {
        this.maxDepth = maxDepth;
        this.aiId = aiId;
        this.opponentId = (aiId == 1) ? 2 : 1;
    }

    /** Returns the best column for the AI, searching up to {@code maxDepth} plies. */
    public int chooseMove(Board board) {
        int bestColumn = -1;
        int bestValue = Integer.MIN_VALUE;

        for (int column = 0; column < board.getColumns(); column++) {
            if (board.isValidColumn(column)) {

                Board copy = board.copy();
                copy.dropDisc(column, aiId);

                int value = minimax(copy, maxDepth - 1, false, Integer.MIN_VALUE, Integer.MAX_VALUE);

                if (value > bestValue) {
                    bestValue = value;
                    bestColumn = column;
                }
            }
        }

        return bestColumn;
    }

    /**
     * Recursive minimax with alpha-beta pruning.
     *
     * @param maximizing true when it is the AI's turn (maximizing player)
     * @return the value of the position from the AI's point of view
     */
    private int minimax(Board board, int depth, boolean maximizing, int alpha, int beta) {
        int result;

        if (depth == 0 || board.hasWon(aiId) || board.hasWon(opponentId) || board.isFull()) {
            result = evaluate(board);

            // Prefer faster wins and slower losses; otherwise a lost AI stops defending and plays any column.
            if (result == WIN_SCORE) {
                result += depth;
            } else if (result == LOSS_SCORE) {
                result -= depth;
            }

        } else if (maximizing) {

            int best = Integer.MIN_VALUE;
            int column = 0;

            // "beta > alpha" in the loop condition replaces the usual break on a cutoff.
            while (column < board.getColumns() && beta > alpha) {
                if (board.isValidColumn(column)) {

                    Board copy = board.copy();
                    copy.dropDisc(column, aiId);

                    int value = minimax(copy, depth - 1, false, alpha, beta);
                    best = Math.max(best, value);
                    alpha = Math.max(alpha, best);
                }
                column++;
            }

            result = best;

        } else {

            int best = Integer.MAX_VALUE;
            int column = 0;

            while (column < board.getColumns() && beta > alpha) {
                if (board.isValidColumn(column)) {

                    Board copy = board.copy();
                    copy.dropDisc(column, opponentId);

                    int value = minimax(copy, depth - 1, true, alpha, beta);
                    best = Math.min(best, value);
                    beta = Math.min(beta, best);
                }
                column++;
            }

            result = best;
        }

        return result;
    }

    /**
     * Static evaluation: +/-WIN_SCORE for a decided game, otherwise the sum of
     * every horizontal, vertical and diagonal window of four cells.
     */
    private int evaluate(Board board) {
        int score;

        if (board.hasWon(aiId)) {
            score = WIN_SCORE;

        } else if (board.hasWon(opponentId)) {
            score = LOSS_SCORE;

        } else {
            score = 0;

            int rows = board.getRows();
            int columns = board.getColumns();

            for (int row = 0; row < rows; row++) {
                for (int column = 0; column <= columns - 4; column++) {
                    int[] window = {
                            board.getCell(row, column),
                            board.getCell(row, column + 1),
                            board.getCell(row, column + 2),
                            board.getCell(row, column + 3)
                    };
                    score += evaluateWindow(window);
                }
            }

            for (int column = 0; column < columns; column++) {
                for (int row = 0; row <= rows - 4; row++) {
                    int[] window = {
                            board.getCell(row, column),
                            board.getCell(row + 1, column),
                            board.getCell(row + 2, column),
                            board.getCell(row + 3, column)
                    };
                    score += evaluateWindow(window);
                }
            }

            for (int row = 0; row <= rows - 4; row++) {
                for (int column = 0; column <= columns - 4; column++) {
                    int[] window = {
                            board.getCell(row, column),
                            board.getCell(row + 1, column + 1),
                            board.getCell(row + 2, column + 2),
                            board.getCell(row + 3, column + 3)
                    };
                    score += evaluateWindow(window);
                }
            }

            for (int row = 3; row < rows; row++) {
                for (int column = 0; column <= columns - 4; column++) {
                    int[] window = {
                            board.getCell(row, column),
                            board.getCell(row - 1, column + 1),
                            board.getCell(row - 2, column + 2),
                            board.getCell(row - 3, column + 3)
                    };
                    score += evaluateWindow(window);
                }
            }
        }

        return score;
    }

    /** Scores a window of four cells; mixed windows can never become a line, so they count 0. */
    private int evaluateWindow(int[] window) {

        int mine = 0;
        int theirs = 0;
        int empty = 0;

        for (int value : window) {
            if (value == aiId) {
                mine++;
            } else if (value == opponentId) {
                theirs++;
            } else {
                empty++;
            }
        }

        int score;

        if (mine > 0 && theirs > 0) {
            score = 0;
        } else if (mine == 3 && empty == 1) {
            score = 50;
        } else if (mine == 2 && empty == 2) {
            score = 10;
        } else if (mine == 1 && empty == 3) {
            score = 1;
        } else if (theirs == 3 && empty == 1) {
            score = -50;
        } else if (theirs == 2 && empty == 2) {
            score = -10;
        } else if (theirs == 1 && empty == 3) {
            score = -1;
        } else {
            score = 0;
        }

        return score;
    }
}
