import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Main game window: board, turn indicator and move history. Bridges {@link Game} events to the Swing UI. */
public class GameWindow extends JFrame implements Game.GameListener, BoardPanel.ColumnClickListener {

    private static final int AI_DEPTH = 6;
    private static final int CELL_SIZE = 90;
    private static final int NOTES_WIDTH = 250;

    private BoardPanel boardPanel;
    private JLabel statusLabel;
    private DefaultListModel<String> moveHistoryModel;
    private JList<String> moveHistoryList;

    private Board board;
    private Player player1;
    private Player player2;
    private String player1Name;
    private String player2Name;
    private int moveNumber = 0;
    private Game game;
    private Thread gameThread;

    public GameWindow(int rows, int columns, int mode, String apiKey, String player1Name, String player2Name) {
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        setTitle("Connect Four");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new Sketch.PaperPanel(true));
        setLayout(new BorderLayout());

        board = new Board(rows, columns);

        player1 = new HumanPlayer(1);
        if (mode == 0) {
            player2 = new HumanPlayer(2);
        } else if (mode == 1) {
            player2 = new ComputerPlayer(2, AI_DEPTH);
        } else {
            player2 = new GeminiPlayer(2, new GeminiClient(apiKey));
        }

        boardPanel = new BoardPanel();
        boardPanel.setClickListener(this);
        boardPanel.setBoard(board);
        boardPanel.setPreferredSize(new Dimension(columns * CELL_SIZE + 76, rows * CELL_SIZE + 76));
        add(boardPanel, BorderLayout.CENTER);

        Sketch.HandButton menuButton = new Sketch.HandButton("Back to menu", 11, 16f, Sketch.SELECTION_YELLOW);
        menuButton.addActionListener(e -> backToMenu());

        JPanel topBar = new JPanel(new BorderLayout(14, 0));
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 52, 2, 16));

        statusLabel = new JLabel(player1Name + "'s turn", SwingConstants.CENTER);
        statusLabel.setFont(Sketch.handFont(Font.BOLD, 22f));
        statusLabel.setForeground(Sketch.INK);
        statusLabel.setIcon(PLAYER_ICONS[1]);
        statusLabel.setIconTextGap(10);

        topBar.add(menuButton, BorderLayout.WEST);
        topBar.add(statusLabel, BorderLayout.CENTER);
        add(topBar, BorderLayout.NORTH);

        add(createMoveHistoryPanel(), BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);

        game = new Game(board, player1, player2, this);

        // The game runs on its own thread: human moves block in chooseMove() waiting
        // for a click, which must not freeze the GUI.
        gameThread = new Thread(game::run);
        gameThread.setDaemon(true);
        gameThread.start();
    }

    private JPanel createMoveHistoryPanel() {
        moveHistoryModel = new DefaultListModel<>();
        moveHistoryList = new JList<>(moveHistoryModel);
        moveHistoryList.setOpaque(false);
        moveHistoryList.setCellRenderer(new MoveRenderer());
        moveHistoryList.setFixedCellHeight(Sketch.LINE_SPACING);
        moveHistoryList.setFocusable(false);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewport(new NotebookViewport());
        scrollPane.setViewportView(moveHistoryList);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        scrollBar.setUI(new Sketch.PencilScrollBarUI());
        scrollBar.setOpaque(false);
        scrollBar.setPreferredSize(new Dimension(10, 0));
        scrollBar.setUnitIncrement(Sketch.LINE_SPACING);

        Sketch.HandTitle title = new Sketch.HandTitle("Moves", 22f, 1, 61);

        JPanel panel = new NotesPanel();
        panel.setLayout(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 20, 16));
        panel.setPreferredSize(new Dimension(NOTES_WIDTH, 0));
        panel.add(title, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void backToMenu() {
        // Otherwise the game thread would wait forever for a click on a window that no longer exists.
        game.stop();
        gameThread.interrupt(); // Wakes it up if it is waiting for a click or an AI response.
        dispose();

        StartScreen startScreen = new StartScreen();
        startScreen.setVisible(true);
    }

    /** Forwards the click to both players: only the one currently waiting in chooseMove() uses it. */
    @Override
    public void onColumnClicked(int column) {
        if (player1 instanceof HumanPlayer) {
            ((HumanPlayer) player1).receiveClick(column);
        }
        if (player2 instanceof HumanPlayer) {
            ((HumanPlayer) player2).receiveClick(column);
        }
    }

    private String nameOf(int playerId) {
        return (playerId == player1.getId()) ? player1Name : player2Name;
    }

    @Override
    public void onMove(Board board, int column, int playerId) {
        // Listener callbacks arrive on the game thread: hop onto the EDT.
        SwingUtilities.invokeLater(() -> {
            boardPanel.animateDrop(board, column, playerId);

            moveNumber++;
            moveHistoryModel.addElement(moveNumber + ". " + nameOf(playerId) + " -> column " + (column + 1));
            moveHistoryList.ensureIndexIsVisible(moveHistoryModel.size() - 1);

            int nextPlayer = (playerId == player1.getId()) ? player2.getId() : player1.getId();
            statusLabel.setText(nameOf(nextPlayer) + "'s turn");
            statusLabel.setIcon(PLAYER_ICONS[nextPlayer]);
        });
    }

    @Override
    public void onGameOver(int winnerId) {
        SwingUtilities.invokeLater(() -> {

            // No setBoard(board) here: the live board would show the last disc landed while it is still falling.
            String message;
            int[][] winningLine = null;
            if (winnerId == 0) {
                message = "Draw!";
            } else {
                message = nameOf(winnerId) + " wins!";
                winningLine = board.getWinningLine(winnerId);
            }

            statusLabel.setText(message);
            statusLabel.setIcon(winnerId == 0 ? null : PLAYER_ICONS[winnerId]);
            boardPanel.showGameOver(message, winningLine);
        });
    }

    private static final Icon[] PLAYER_ICONS = {null, new DiscIcon(1), new DiscIcon(2)};

    private static class DiscIcon implements Icon {
        private static final int SIZE = 26;
        private final BufferedImage image;

        private DiscIcon(int player) {
            image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            Sketch.handDisc(g2, SIZE / 2.0, SIZE / 2.0, 9.5, player, 300 + player);
            g2.dispose();
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.drawImage(image, x, y, null);
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }

    private static class NotesPanel extends JPanel {
        private NotesPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = 6;
            int y = 8;
            int w = getWidth() - 16;
            int h = getHeight() - 18;
            g2.setColor(new Color(60, 50, 30, 30));
            g2.fillRect(x + 3, y + 4, w, h);
            g2.setColor(Sketch.PAPER);
            g2.fillRect(x, y, w, h);
            g2.setColor(Sketch.INK);
            Sketch.handRect(g2, x, y, w, h, 62);
            g2.dispose();
        }
    }

    /** Viewport that draws notebook lines in list coordinates, so lines and entries scroll together. */
    private static class NotebookViewport extends JViewport {
        private NotebookViewport() {
            setOpaque(false);
            setScrollMode(JViewport.SIMPLE_SCROLL_MODE); // Blit scrolling would smear the ruled lines.
        }

        @Override
        public void setViewPosition(Point position) {
            // Snap to whole lines so the top entry is never cut in half.
            int y = ((position.y + Sketch.LINE_SPACING - 1) / Sketch.LINE_SPACING) * Sketch.LINE_SPACING;
            super.setViewPosition(new Point(position.x, y));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Sketch.NOTEBOOK_LINE);
            int offset = getViewPosition().y % Sketch.LINE_SPACING;
            for (int y = Sketch.LINE_SPACING - 1 - offset; y < getHeight(); y += Sketch.LINE_SPACING) {
                g.drawLine(0, y, getWidth(), y);
            }
        }
    }

    private static class MoveRenderer extends JComponent implements ListCellRenderer<String> {
        private final Font normalFont = Sketch.handFont(Font.PLAIN, 16f);
        private final Font smallFont = Sketch.handFont(Font.PLAIN, 12.5f);
        private String text = "";
        private int number = 1;

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            text = value;
            number = index + 1;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int player = (number % 2 == 1) ? 1 : 2;
            Sketch.handDisc(g2, 10, getHeight() / 2.0, 6, player, number);

            int textSpace = getWidth() - 26;
            Font font = (getFontMetrics(normalFont).stringWidth(text) <= textSpace) ? normalFont : smallFont;
            g2.setFont(font);
            g2.setColor(Sketch.INK);
            g2.drawString(text, 22, getHeight() - 7);
            g2.dispose();
        }
    }
}
