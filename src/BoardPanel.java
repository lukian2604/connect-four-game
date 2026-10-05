import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/** Swing panel that draws the board in a hand-drawn style, animates falling discs and reports column clicks. */
public class BoardPanel extends JPanel {

    private Board board;

    // Single source of geometry for both drawing and columnAtX, so clicks always match what is drawn.
    private static final int LEFT_MARGIN = 58;
    private static final int RIGHT_MARGIN = 18;
    private static final int TOP_MARGIN = 42;
    private static final int BOTTOM_MARGIN = 34;
    private static final int BOARD_BORDER = 8;

    // Static layers, regenerated only when the panel or board size changes.
    private BufferedImage boardCache;
    private BufferedImage hoverCache;
    private final Map<Long, BufferedImage> discCache = new HashMap<>();
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    private int cachedRows = -1;
    private int cachedColumns = -1;
    private Font bannerFont = null;

    private int hoverColumn = -1;

    private static final double GRAVITY = 0.012;
    private static final double BOUNCE_DAMPING = 0.35;

    private int animatedColumn = -1;
    private int animatedTargetRow = -1;
    private double animatedRow = 0;
    private double animatedVelocity = 0;
    private boolean bouncing = false;
    private int animatedPlayer = 0;
    private Timer animationTimer;

    private Queue<QueuedMove> animationQueue = new LinkedList<>();

    private int[][] winningCells = null;
    private String endMessage = null;
    private int endTick = 0;
    private Timer endTimer;
    // The game-over event arrives while the last disc is still falling: it is parked here
    // and shown only once every queued animation has finished.
    private boolean endPending = false;
    private String pendingMessage = null;
    private int[][] pendingWinningCells = null;
    private static final int END_TICK_DURATION = 90;

    private ColumnClickListener listener;

    public BoardPanel() {
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (board != null && animatedColumn == -1) {
                    int column = columnAtX(e.getX());

                    if (listener != null && board.isValidColumn(column)) {
                        listener.onColumnClicked(column);
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverColumn = -1;
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (board != null) {
                    int newColumn = columnAtX(e.getX());
                    if (newColumn != hoverColumn) {
                        hoverColumn = newColumn;
                        repaint();
                    }
                }
            }
        });
    }

    private int cellWidth() {
        return Math.max(1, (getWidth() - LEFT_MARGIN - RIGHT_MARGIN) / board.getColumns());
    }

    private int cellHeight() {
        return Math.max(1, (getHeight() - TOP_MARGIN - BOTTOM_MARGIN) / board.getRows());
    }

    private int discDiameter() {
        return Math.max(8, Math.min(cellWidth(), cellHeight()) - 18);
    }

    private double centerX(int column) {
        return LEFT_MARGIN + column * cellWidth() + cellWidth() / 2.0;
    }

    private double centerY(double row) {
        return TOP_MARGIN + row * cellHeight() + cellHeight() / 2.0;
    }

    private int columnAtX(int x) {
        // floorDiv instead of "/": left of the board this yields -1 (invalid), not 0.
        return Math.floorDiv(x - LEFT_MARGIN, cellWidth());
    }

    private static long discSeed(int player, int row, int column) {
        return player * 10007L + row * 31L + column;
    }

    public void setClickListener(ColumnClickListener listener) {
        this.listener = listener;
    }

    public void setBoard(Board board) {
        this.board = board;
        repaint();
    }

    public void showGameOver(String message, int[][] winningCells) {
        endPending = true;
        pendingMessage = message;
        pendingWinningCells = winningCells;
        startEndIfAnimationsFinished();
    }

    private void startEndIfAnimationsFinished() {
        if (endPending && animatedColumn == -1 && animationQueue.isEmpty()) {
            endPending = false;
            this.endMessage = pendingMessage;
            this.winningCells = pendingWinningCells;
            endTick = 0;

            if (winningCells != null) {
                Sounds.playWin();
            }

            if (endTimer != null && endTimer.isRunning()) {
                endTimer.stop();
            }
            endTimer = new Timer(40, e -> {
                endTick++;
                if (endTick >= END_TICK_DURATION) {
                    endTimer.stop();
                }
                repaint();
            });
            endTimer.start();
        }
    }

    /**
     * Animates a disc dropping into the column. {@code board} must be a snapshot
     * taken right after the move; calls during an animation are queued and played in order.
     */
    public void animateDrop(Board board, int column, int playerId) {
        if (animatedColumn != -1) {
            animationQueue.add(new QueuedMove(board, column, playerId));
        } else {
            startAnimation(board, column, playerId);
        }
    }

    private void startAnimation(Board board, int column, int playerId) {
        this.board = board;

        int targetRow = board.getTopOccupiedRow(column);

        if (targetRow < 0) {
            repaint();
            startNextQueuedAnimation();
        } else {
            animatedColumn = column;
            animatedTargetRow = targetRow;
            animatedRow = 0;
            animatedVelocity = 0;
            bouncing = false;
            animatedPlayer = playerId;

            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }

            // Simple physics: gravity accelerates the disc, which bounces once on landing and then stops.
            animationTimer = new Timer(10, e -> {
                animatedVelocity += GRAVITY;
                animatedRow += animatedVelocity;

                if (animatedRow >= animatedTargetRow) {
                    animatedRow = animatedTargetRow;

                    if (!bouncing) {
                        Sounds.playMove();
                    }

                    if (!bouncing && animatedVelocity > 0.08) {
                        animatedVelocity = -animatedVelocity * BOUNCE_DAMPING;
                        bouncing = true;
                    } else {
                        animatedColumn = -1;
                        animationTimer.stop();
                        startNextQueuedAnimation();
                    }
                }

                repaint();
            });
            animationTimer.start();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (board != null) {
            updateCache();

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int rows = board.getRows();
            int columns = board.getColumns();
            int cellWidth = cellWidth();
            int cellHeight = cellHeight();
            int diameter = discDiameter();

            if (hoverColumn >= 0 && hoverColumn < columns && endMessage == null) {
                Graphics2D gHover = (Graphics2D) g2d.create();
                gHover.clipRect(LEFT_MARGIN + hoverColumn * cellWidth, TOP_MARGIN - BOARD_BORDER,
                        cellWidth, rows * cellHeight + BOARD_BORDER * 2);
                gHover.drawImage(hoverCache, 0, 0, null);
                gHover.dispose();

                g2d.setColor(Sketch.INK);
                Sketch.handArrow(g2d, centerX(hoverColumn), 8, TOP_MARGIN - BOARD_BORDER - 6, 7,
                        500 + hoverColumn, true);
            }

            g2d.drawImage(boardCache, 0, 0, null);

            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    int value = board.getCell(row, column);
                    // The cell of the falling disc is skipped here and drawn separately at its animated position.
                    if (value != 0 && (column != animatedColumn || row != animatedTargetRow)) {
                        drawDisc(g2d, centerX(column), centerY(row), diameter, value, discSeed(value, row, column));
                    }
                }
            }

            if (animatedColumn != -1) {
                drawDisc(g2d, centerX(animatedColumn), centerY(animatedRow), diameter, animatedPlayer,
                        discSeed(animatedPlayer, animatedTargetRow, animatedColumn));
            }

            if (winningCells != null) {
                drawWinningLine(g2d, diameter);
            }

            if (endMessage != null) {
                drawBanner(g2d);
            }
            g2d.dispose();
        }
    }

    /** Redraws the board and hover images if the size changed; disc images are cached by seed. */
    private void updateCache() {
        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        if (width != cachedWidth || height != cachedHeight
                || board.getRows() != cachedRows || board.getColumns() != cachedColumns) {
            cachedWidth = width;
            cachedHeight = height;
            cachedRows = board.getRows();
            cachedColumns = board.getColumns();
            discCache.clear();

            int cellWidth = cellWidth();
            int cellHeight = cellHeight();
            double holeRadius = discDiameter() / 2.0 + 4;
            double x0 = LEFT_MARGIN - BOARD_BORDER;
            double y0 = TOP_MARGIN - BOARD_BORDER;
            double boardWidth = cachedColumns * cellWidth + BOARD_BORDER * 2;
            double boardHeight = cachedRows * cellHeight + BOARD_BORDER * 2;
            Rectangle2D body = new Rectangle2D.Double(x0, y0, boardWidth, boardHeight);

            boardCache = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gc = boardCache.createGraphics();
            gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Sketch.hatchFill(gc, body, Sketch.FAINT_PENCIL, -0.9, 777, 7, 1f);

            gc.setComposite(AlphaComposite.Clear);
            for (int row = 0; row < cachedRows; row++) {
                for (int column = 0; column < cachedColumns; column++) {
                    gc.fill(new Ellipse2D.Double(centerX(column) - holeRadius, centerY(row) - holeRadius, holeRadius * 2, holeRadius * 2));
                }
            }
            gc.setComposite(AlphaComposite.SrcOver);

            gc.setColor(Sketch.INK);
            Sketch.handRect(gc, x0, y0, boardWidth, boardHeight, 31, 1.25f);

            double bottom = y0 + boardHeight;
            double base = bottom + BOTTOM_MARGIN - BOARD_BORDER - 12;
            Sketch.wobblyLine(gc, x0 + 16, bottom, x0 + 4, base, 41);
            Sketch.wobblyLine(gc, x0 - 6, base, x0 + 22, base, 42);
            Sketch.wobblyLine(gc, x0 + boardWidth - 16, bottom, x0 + boardWidth - 4, base, 43);
            Sketch.wobblyLine(gc, x0 + boardWidth - 22, base, x0 + boardWidth + 6, base, 44);

            for (int row = 0; row < cachedRows; row++) {
                for (int column = 0; column < cachedColumns; column++) {
                    Sketch.handCircle(gc, centerX(column), centerY(row), holeRadius, 1000 + row * 31 + column, 1.0, 0.85f);
                }
            }
            gc.dispose();

            hoverCache = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gh = hoverCache.createGraphics();
            Sketch.hatchFill(gh, body, new Color(31, 58, 147, 40), 1.35, 555, 5, 1.3f);
            gh.dispose();
        }
    }

    private void drawDisc(Graphics2D g2d, double cx, double cy, int diameter, int player, long seed) {
        BufferedImage image = discCache.get(seed);
        if (image == null) {
            int side = diameter + 14;
            image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gi = image.createGraphics();
            Sketch.handDisc(gi, side / 2.0, side / 2.0, diameter / 2.0, player, seed);
            gi.dispose();
            discCache.put(seed, image);
        }
        g2d.drawImage(image, (int) Math.round(cx - image.getWidth() / 2.0), (int) Math.round(cy - image.getHeight() / 2.0), null);
    }

    private void drawWinningLine(Graphics2D g2d, int diameter) {
        double radius = diameter / 2.0 + 7;
        g2d.setColor(Sketch.INK);
        for (int i = 0; i < winningCells.length; i++) {
            // Each circle starts 4 ticks after the previous one, so they appear to be drawn one by one.
            double progress = (endTick - i * 4) / 10.0;
            if (progress > 0) {
                int[] cell = winningCells[i];
                Sketch.handCircle(g2d, centerX(cell[1]), centerY(cell[0]), radius, 9000 + cell[0] * 31 + cell[1],
                        Math.min(1, progress), 1.6f);
            }
        }

        double lineProgress = (endTick - 26) / 12.0;
        if (lineProgress > 0) {
            int[] first = winningCells[0];
            int[] last = winningCells[winningCells.length - 1];
            double x1 = centerX(first[1]);
            double y1 = centerY(first[0]);
            double x2 = centerX(last[1]);
            double y2 = centerY(last[0]);
            double length = Math.max(1, Math.hypot(x2 - x1, y2 - y1));
            double extension = radius * 0.8;
            double ux = (x2 - x1) / length;
            double uy = (y2 - y1) / length;
            Sketch.wobblyLine(g2d, x1 - ux * extension, y1 - uy * extension,
                    x2 + ux * extension, y2 + uy * extension, 9100, Math.min(1, lineProgress), 1.7f);
        }
    }

    private void drawBanner(Graphics2D g2d) {
        double progress = Math.min(1.0, endTick / 12.0);
        double scale = 1.0 - Math.pow(1.0 - progress, 3);

        if (bannerFont == null) {
            float size = 36f;
            Font candidate = Sketch.handFont(Font.BOLD, size);
            while (getFontMetrics(candidate).stringWidth(endMessage) + 90 > getWidth() && size > 16) {
                size -= 2;
                candidate = Sketch.handFont(Font.BOLD, size);
            }
            bannerFont = candidate;
        }

        if (scale > 0.01) {
            FontMetrics fm = g2d.getFontMetrics(bannerFont);
            int textWidth = fm.stringWidth(endMessage);
            double bannerWidth = textWidth + 64;
            double bannerHeight = fm.getAscent() + 44;

            Graphics2D gb = (Graphics2D) g2d.create();
            gb.translate(getWidth() / 2.0, (TOP_MARGIN + getHeight() - BOTTOM_MARGIN) / 2.0);
            gb.rotate(Math.toRadians(-2.5));
            gb.scale(scale, scale);
            double x = -bannerWidth / 2;
            double y = -bannerHeight / 2;

            gb.setColor(new Color(60, 50, 30, 35));
            gb.fill(new Rectangle2D.Double(x + 4, y + 5, bannerWidth, bannerHeight));
            gb.setColor(Sketch.PAPER);
            gb.fill(new Rectangle2D.Double(x, y, bannerWidth, bannerHeight));
            gb.setColor(Sketch.NOTEBOOK_LINE);
            for (double row = y + Sketch.LINE_SPACING; row < y + bannerHeight - 4; row += Sketch.LINE_SPACING) {
                gb.draw(new java.awt.geom.Line2D.Double(x, row, x + bannerWidth, row));
            }
            gb.setColor(Sketch.INK);
            Sketch.handRect(gb, x, y, bannerWidth, bannerHeight, 8080, 1.2f);

            double baseline = y + 14 + fm.getAscent() - fm.getDescent() / 2.0;
            if (progress > 0.6) {
                gb.setFont(bannerFont);
                gb.drawString(endMessage, (float) (-textWidth / 2.0), (float) baseline);
            }

            double underlineProgress = (endTick - 12) / 14.0;
            if (underlineProgress > 0) {
                Sketch.handUnderline(gb, -textWidth / 2.0 - 6, baseline + fm.getDescent() / 2.0 + 4,
                        textWidth + 12, 8181, 2, Math.min(1, underlineProgress));
            }

            if (endTick > 28) {
                drawSparkle(gb, x + bannerWidth + 2, y - 4, 8282);
                drawSparkle(gb, x - 4, y + bannerHeight + 2, 8383);
            }
            gb.dispose();
        }
    }

    private void drawSparkle(Graphics2D g2d, double cx, double cy, long seed) {
        double radius = 8;
        for (int i = 0; i < 3; i++) {
            double angle = Math.PI / 3 * i + 0.3;
            double dx = Math.cos(angle) * radius;
            double dy = Math.sin(angle) * radius;
            Sketch.wobblyLine(g2d, cx - dx, cy - dy, cx + dx, cy + dy, seed + i);
        }
    }

    private void startNextQueuedAnimation() {
        QueuedMove next = animationQueue.poll();
        if (next != null) {
            startAnimation(next.board, next.column, next.playerId);
        } else {
            startEndIfAnimationsFinished();
        }
    }

    private static class QueuedMove {
        private Board board;
        private int column;
        private int playerId;

        private QueuedMove(Board board, int column, int playerId) {
            this.board = board;
            this.column = column;
            this.playerId = playerId;
        }
    }

    public interface ColumnClickListener {
        void onColumnClicked(int column);
    }
}
