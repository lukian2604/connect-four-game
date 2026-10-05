/** Connect Four board: cells hold 0 (empty) or the id of the player who owns them. Row 0 is the top. */
public class Board {

    private int[][] cells;
    private int rows;
    private int columns;

    public Board(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.cells = new int[rows][columns];
    }

    public boolean isValidColumn(int column) {
        boolean valid;

        if (column < 0 || column >= columns) {
            valid = false;
        } else {
            valid = cells[0][column] == 0;
        }

        return valid;
    }

    /** Drops a disc into the column; returns false if the column is full or out of range. */
    public boolean dropDisc(int column, int player) {
        boolean dropped = false;

        if (isValidColumn(column)) {
            int row = rows - 1;
            while (row >= 0 && !dropped) {
                if (cells[row][column] == 0) {
                    cells[row][column] = player;
                    dropped = true;
                }
                row--;
            }
        }

        return dropped;
    }

    public boolean isFull() {
        boolean full = true;
        int column = 0;

        while (column < columns && full) {
            if (cells[0][column] == 0) {
                full = false;
            }
            column++;
        }

        return full;
    }

    public boolean hasWon(int player) {
        return getWinningLine(player) != null;
    }

    /** Returns the {row, column} pairs of a winning line for the player, or null if there is none. */
    public int[][] getWinningLine(int player) {
        int[][] winningLine = findHorizontal(player);

        if (winningLine == null) {
            winningLine = findVertical(player);
        }
        if (winningLine == null) {
            winningLine = findDescendingDiagonal(player);
        }
        if (winningLine == null) {
            winningLine = findAscendingDiagonal(player);
        }

        return winningLine;
    }

    private int[][] findHorizontal(int player) {
        int[][] winningLine = null;

        for (int row = 0; row < rows && winningLine == null; row++) {
            for (int column = 0; column <= columns - 4 && winningLine == null; column++) {
                if (cells[row][column] == player
                        && cells[row][column + 1] == player
                        && cells[row][column + 2] == player
                        && cells[row][column + 3] == player) {
                    winningLine = new int[][]{{row, column}, {row, column + 1}, {row, column + 2}, {row, column + 3}};
                }
            }
        }

        return winningLine;
    }

    private int[][] findVertical(int player) {
        int[][] winningLine = null;

        for (int column = 0; column < columns && winningLine == null; column++) {
            for (int row = 0; row <= rows - 4 && winningLine == null; row++) {
                if (cells[row][column] == player
                        && cells[row + 1][column] == player
                        && cells[row + 2][column] == player
                        && cells[row + 3][column] == player) {
                    winningLine = new int[][]{{row, column}, {row + 1, column}, {row + 2, column}, {row + 3, column}};
                }
            }
        }

        return winningLine;
    }

    private int[][] findDescendingDiagonal(int player) {
        int[][] winningLine = null;

        for (int row = 0; row <= rows - 4 && winningLine == null; row++) {
            for (int column = 0; column <= columns - 4 && winningLine == null; column++) {
                if (cells[row][column] == player
                        && cells[row + 1][column + 1] == player
                        && cells[row + 2][column + 2] == player
                        && cells[row + 3][column + 3] == player) {
                    winningLine = new int[][]{{row, column}, {row + 1, column + 1}, {row + 2, column + 2}, {row + 3, column + 3}};
                }
            }
        }

        return winningLine;
    }

    private int[][] findAscendingDiagonal(int player) {
        int[][] winningLine = null;

        for (int row = 3; row < rows && winningLine == null; row++) {
            for (int column = 0; column <= columns - 4 && winningLine == null; column++) {
                if (cells[row][column] == player
                        && cells[row - 1][column + 1] == player
                        && cells[row - 2][column + 2] == player
                        && cells[row - 3][column + 3] == player) {
                    winningLine = new int[][]{{row, column}, {row - 1, column + 1}, {row - 2, column + 2}, {row - 3, column + 3}};
                }
            }
        }

        return winningLine;
    }

    public void print() {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                System.out.print("[" + cells[row][column] + "]");
            }
            System.out.println();
        }
    }

    /** Deep copy, used by the AI to simulate moves and by the GUI to get a stable snapshot. */
    public Board copy() {
        Board copy = new Board(rows, columns);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                copy.cells[row][column] = this.cells[row][column];
            }
        }
        return copy;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public int getCell(int row, int column) {
        return cells[row][column];
    }

    /** Returns the row of the topmost disc in the column, or -1 if the column is empty. */
    public int getTopOccupiedRow(int column) {
        int foundRow = -1;
        int row = 0;

        while (row < rows && foundRow == -1) {
            if (cells[row][column] != 0) {
                foundRow = row;
            }
            row++;
        }

        return foundRow;
    }
}
