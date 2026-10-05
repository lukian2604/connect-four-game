import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Hand-drawn "pencil on notebook paper" toolkit: shared colors, fonts, drawing
 * primitives and a few Swing components, so every window shares the same style.
 */
public final class Sketch {

    public static final Color PAPER = new Color(251, 248, 239);
    public static final Color STICKY_NOTE = new Color(255, 246, 196);
    public static final Color NOTEBOOK_LINE = new Color(150, 185, 225, 120);
    public static final Color RED_MARGIN = new Color(220, 80, 80, 150);
    public static final Color INK = new Color(31, 58, 147);
    public static final Color GRAPHITE = new Color(70, 70, 70);
    public static final Color FAINT_PENCIL = new Color(80, 80, 80, 45);
    public static final Color RED_CRAYON = new Color(229, 72, 77, 215);
    public static final Color YELLOW_HIGHLIGHTER = new Color(245, 190, 0, 225);
    public static final Color GREEN_HIGHLIGHTER = new Color(100, 195, 100, 130);
    public static final Color SELECTION_YELLOW = new Color(255, 214, 60, 140);

    public static final int LINE_SPACING = 26;
    public static final int RED_MARGIN_X = 40;

    private static final double WOBBLE = 1.1;
    private static final double POINT_STEP = 14;
    private static final float FIRST_STROKE_WIDTH = 1.6f;
    private static final float SECOND_STROKE_WIDTH = 1.1f;
    private static final double HATCH_STEP = 3.4;

    private static Font baseFont = null;

    private Sketch() {
    }

    /** Returns the first installed handwriting-like font, falling back to sans-serif. */
    public static synchronized Font handFont(int style, float size) {
        if (baseFont == null) {
            String[] preferred = {"Noteworthy", "Chalkboard SE", "Bradley Hand", "Marker Felt", "Segoe Print", "Comic Sans MS"};
            String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            String chosen = Font.SANS_SERIF;
            boolean found = false;
            for (int i = 0; i < preferred.length && !found; i++) {
                for (String name : installed) {
                    if (!found && name.equalsIgnoreCase(preferred[i])) {
                        chosen = name;
                        found = true;
                    }
                }
            }
            baseFont = new Font(chosen, Font.PLAIN, 12);
        }
        return baseFont.deriveFont(style, size);
    }

    /** Works on a copy so callers don't get their stroke, color or clip changed. */
    private static Graphics2D prepare(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /**
     * Every shape is drawn from a fixed seed, so it looks identical on every repaint instead of flickering.
     * Close seeds give similar java.util.Random sequences, so the seed is scrambled first.
     */
    private static Random seededRandom(long seed) {
        return new Random(seed * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L);
    }

    private static BasicStroke stroke(float width) {
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    /** Builds one wobbly pen stroke; {@code progress} (0..1) draws only part of it, for animations. */
    private static Path2D wobblyPath(double x1, double y1, double x2, double y2, Random r, double progress) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.max(0.5, Math.hypot(dx, dy));
        double ux = dx / length;
        double uy = dy / length;
        double nx = -uy;
        double ny = ux;

        double startOvershoot = -1.5 + r.nextDouble() * 4.0;
        double endOvershoot = -1.5 + r.nextDouble() * 4.0;
        double bulge = (r.nextDouble() - 0.5) * Math.min(length * 0.02, 3.0);
        double startX = x1 - ux * startOvershoot;
        double startY = y1 - uy * startOvershoot;
        double total = length + startOvershoot + endOvershoot;

        int segments = Math.max(2, (int) (length / POINT_STEP));
        double[] px = new double[segments + 1];
        double[] py = new double[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double offset = bulge * Math.sin(Math.PI * t) + (r.nextDouble() - 0.5) * 2 * WOBBLE;
            px[i] = startX + ux * total * t + nx * offset;
            py[i] = startY + uy * total * t + ny * offset;
        }

        Path2D path = new Path2D.Double();
        path.moveTo(px[0], py[0]);
        double limit = Math.max(0, Math.min(1, progress)) * segments;
        int lastWhole = (int) Math.floor(limit);
        for (int i = 1; i <= lastWhole; i++) {
            double midX = (px[i - 1] + px[i]) / 2;
            double midY = (py[i - 1] + py[i]) / 2;
            // Curve through the midpoints keeps the wobble smooth, without corners.
            path.quadTo(px[i - 1], py[i - 1], midX, midY);
        }
        if (lastWhole < segments) {
            double fraction = limit - lastWhole;
            path.lineTo(px[lastWhole] + (px[lastWhole + 1] - px[lastWhole]) * fraction,
                    py[lastWhole] + (py[lastWhole + 1] - py[lastWhole]) * fraction);
        } else {
            path.lineTo(px[segments], py[segments]);
        }
        return path;
    }

    public static void wobblyLine(Graphics2D g, double x1, double y1, double x2, double y2, long seed) {
        wobblyLine(g, x1, y1, x2, y2, seed, 1.0, 1f);
    }

    /** Draws a wobbly line twice with a small offset, like retracing it by hand. */
    public static void wobblyLine(Graphics2D g, double x1, double y1, double x2, double y2, long seed,
                                  double progress, float widthScale) {
        Graphics2D g2 = prepare(g);
        Random r = seededRandom(seed);
        for (int pass = 0; pass < 2; pass++) {
            double ox = (r.nextDouble() - 0.5) * 1.6;
            double oy = (r.nextDouble() - 0.5) * 1.6;
            g2.setStroke(stroke((pass == 0 ? FIRST_STROKE_WIDTH : SECOND_STROKE_WIDTH) * widthScale));
            g2.draw(wobblyPath(x1 + ox, y1 + oy, x2 + ox, y2 + oy, r, progress));
        }
        g2.dispose();
    }

    public static void handRect(Graphics2D g, double x, double y, double width, double height, long seed) {
        handRect(g, x, y, width, height, seed, 1f);
    }

    public static void handRect(Graphics2D g, double x, double y, double width, double height, long seed, float widthScale) {
        long base = seed * 7;
        wobblyLine(g, x, y, x + width, y, base + 1, 1.0, widthScale);
        wobblyLine(g, x + width, y, x + width, y + height, base + 2, 1.0, widthScale);
        wobblyLine(g, x + width, y + height, x, y + height, base + 3, 1.0, widthScale);
        wobblyLine(g, x, y + height, x, y, base + 4, 1.0, widthScale);
    }

    public static void handCircle(Graphics2D g, double cx, double cy, double radius, long seed) {
        handCircle(g, cx, cy, radius, seed, 1.0, 1f);
    }

    /** A slightly irregular circle drawn in two passes whose ends do not meet perfectly. */
    public static void handCircle(Graphics2D g, double cx, double cy, double radius, long seed,
                                  double progress, float widthScale) {
        Graphics2D g2 = prepare(g);
        Random r = seededRandom(seed);
        int points = Math.max(20, (int) (radius * 1.3));
        for (int pass = 0; pass < 2; pass++) {
            double start = r.nextDouble() * Math.PI * 2;
            double sweep = Math.PI * 2 + (-0.15 + r.nextDouble() * 0.45);
            double amplitude1 = 0.015 + r.nextDouble() * 0.025;
            double phase1 = r.nextDouble() * Math.PI * 2;
            double amplitude2 = 0.005 + r.nextDouble() * 0.015;
            double phase2 = r.nextDouble() * Math.PI * 2;
            double drift = (r.nextDouble() - 0.5) * 0.07;
            double ox = (r.nextDouble() - 0.5) * 1.6;
            double oy = (r.nextDouble() - 0.5) * 1.6;

            int last = (int) Math.round(points * Math.max(0, Math.min(1, progress)));
            if (last >= 1) {
                Path2D path = new Path2D.Double();
                for (int i = 0; i <= last; i++) {
                    double t = (double) i / points;
                    double angle = sweep * t;
                    double rr = radius * (1 + amplitude1 * Math.sin(2 * angle + phase1)
                            + amplitude2 * Math.sin(3 * angle + phase2) + drift * (t - 0.5));
                    double x = cx + ox + rr * Math.cos(start + angle);
                    double y = cy + oy + rr * Math.sin(start + angle);
                    if (i == 0) {
                        path.moveTo(x, y);
                    } else {
                        path.lineTo(x, y);
                    }
                }
                g2.setStroke(stroke((pass == 0 ? FIRST_STROKE_WIDTH : SECOND_STROKE_WIDTH) * widthScale));
                g2.draw(path);
            }
        }
        g2.dispose();
    }

    public static void hatchFill(Graphics2D g, Shape shape, Color color, double angle, long seed) {
        hatchFill(g, shape, color, angle, seed, HATCH_STEP, 2.3f);
    }

    /** Crayon-like fill: dense slanted zig-zag strokes clipped to the shape with soft edges. */
    public static void hatchFill(Graphics2D g, Shape shape, Color color, double angle, long seed,
                                 double step, float strokeWidth) {
        Rectangle2D bounds = shape.getBounds2D();
        int margin = 4;
        int originX = (int) Math.floor(bounds.getX()) - margin;
        int originY = (int) Math.floor(bounds.getY()) - margin;
        int width = (int) Math.ceil(bounds.getWidth()) + margin * 2 + 1;
        int height = (int) Math.ceil(bounds.getHeight()) + margin * 2 + 1;

        // Drawn on a separate image and then cut with DstOut: Java2D's clip() gives jagged edges.
        BufferedImage sheet = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gi = sheet.createGraphics();
        gi.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gi.translate(-originX, -originY);

        Random r = seededRandom(seed);
        double cx = bounds.getCenterX();
        double cy = bounds.getCenterY();
        double halfDiagonal = Math.hypot(bounds.getWidth(), bounds.getHeight()) / 2 + 3;

        gi.setColor(color);
        for (int layer = 0; layer < 2; layer++) {
            double layerStep = (layer == 0) ? step : step * 2.3;
            double deviation = (layer == 0) ? 0 : 0.22 + r.nextDouble() * 0.12;
            double layerCos = Math.cos(angle + deviation);
            double layerSin = Math.sin(angle + deviation);
            Path2D zigzag = new Path2D.Double();
            boolean rightward = true;
            boolean first = true;
            for (double v = -halfDiagonal; v <= halfDiagonal; v += layerStep) {
                double u = (rightward ? 1 : -1) * (halfDiagonal + r.nextDouble() * 3);
                double vv = v + (r.nextDouble() - 0.5) * layerStep * 0.5;
                double x = cx + u * layerCos - vv * layerSin;
                double y = cy + u * layerSin + vv * layerCos;
                if (first) {
                    double x0 = cx - u * layerCos - vv * layerSin;
                    double y0 = cy - u * layerSin + vv * layerCos;
                    zigzag.moveTo(x0, y0);
                    first = false;
                }
                zigzag.lineTo(x, y);
                rightward = !rightward;
            }
            gi.setStroke(stroke(layer == 0 ? strokeWidth : strokeWidth * 0.7f));
            gi.draw(zigzag);
        }

        double scale = 1 + 3.0 / Math.max(8, Math.min(bounds.getWidth(), bounds.getHeight()));
        AffineTransform enlarge = new AffineTransform();
        enlarge.translate(cx, cy);
        enlarge.scale(scale, scale);
        enlarge.translate(-cx, -cy);
        java.awt.geom.Area outside = new java.awt.geom.Area(new Rectangle2D.Double(originX, originY, width, height));
        outside.subtract(new java.awt.geom.Area(enlarge.createTransformedShape(shape)));
        gi.setComposite(AlphaComposite.DstOut);
        gi.setColor(Color.BLACK);
        gi.fill(outside);
        gi.dispose();

        g.drawImage(sheet, originX, originY, null);
    }

    public static void handUnderline(Graphics2D g, double x, double y, double width, long seed,
                                     int lineCount, double progress) {
        Random r = seededRandom(seed);
        for (int i = 0; i < lineCount; i++) {
            double slope = (r.nextDouble() - 0.5) * 4;
            double indent = i * (6 + r.nextDouble() * 6);
            double lineProgress = Math.max(0, Math.min(1, progress * lineCount - i));
            if (lineProgress > 0) {
                wobblyLine(g, x + indent, y + i * 5, x + width - indent / 2, y + i * 5 + slope,
                        seed * 13 + i, lineProgress, 1.1f);
            }
        }
    }

    public static void handArrow(Graphics2D g, double cx, double yTop, double yBottom, double spread, long seed, boolean pointingDown) {
        double tip = pointingDown ? yBottom : yTop;
        double tail = pointingDown ? yTop : yBottom;
        double back = pointingDown ? -spread : spread;
        wobblyLine(g, cx, tail, cx, tip, seed * 3 + 1);
        wobblyLine(g, cx, tip, cx - spread, tip + back, seed * 3 + 2);
        wobblyLine(g, cx, tip, cx + spread, tip + back, seed * 3 + 3);
    }

    public static void handArrowhead(Graphics2D g, double cx, double cy, double spread, long seed, boolean pointingDown) {
        double direction = pointingDown ? 1 : -1;
        wobblyLine(g, cx - spread, cy - direction * spread / 2, cx, cy + direction * spread / 2, seed * 5 + 1);
        wobblyLine(g, cx, cy + direction * spread / 2, cx + spread, cy - direction * spread / 2, seed * 5 + 2);
    }

    public static void drawPaper(Graphics2D g, int width, int height, boolean withMargin) {
        Graphics2D g2 = prepare(g);
        g2.setColor(PAPER);
        g2.fillRect(0, 0, width, height);

        Random grain = seededRandom(2026);
        int specks = width * height / 1600;
        for (int i = 0; i < specks; i++) {
            g2.setColor(new Color(120, 110, 90, 18 + grain.nextInt(22)));
            g2.fillRect(grain.nextInt(Math.max(1, width)), grain.nextInt(Math.max(1, height)), 1, 1);
        }

        g2.setStroke(new BasicStroke(1f));
        g2.setColor(NOTEBOOK_LINE);
        for (int y = LINE_SPACING; y < height; y += LINE_SPACING) {
            g2.drawLine(0, y, width, y);
        }

        if (withMargin) {
            g2.setColor(RED_MARGIN);
            g2.setStroke(new BasicStroke(1.3f));
            g2.drawLine(RED_MARGIN_X, 0, RED_MARGIN_X, height);
        }
        g2.dispose();
    }

    /** A complete scribbled disc (fill plus outline), used by the board, logo, move list and turn label. */
    public static void handDisc(Graphics2D g, double cx, double cy, double radius, int player, long seed) {
        Color color = (player == 1) ? RED_CRAYON : YELLOW_HIGHLIGHTER;
        double angle = (player == 1) ? -Math.PI / 4 : Math.PI / 4;
        double step = Math.max(2.2, Math.min(HATCH_STEP, radius / 4));
        float width = (float) Math.max(1.3, Math.min(2.3, radius / 9));
        Shape circle = new java.awt.geom.Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2);
        hatchFill(g, circle, color, angle, seed, step, width);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(INK);
        handCircle(g2, cx, cy, radius, seed + 17, 1.0, radius < 12 ? 0.8f : 1f);
        g2.dispose();
    }

    /** Background panel with the notebook page, cached: redrawing the paper grain on every repaint is too slow. */
    public static class PaperPanel extends JPanel {
        private final boolean withMargin;
        private BufferedImage cache;

        public PaperPanel(boolean withMargin) {
            this.withMargin = withMargin;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (cache == null || cache.getWidth() != getWidth() || cache.getHeight() != getHeight()) {
                cache = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_RGB);
                Graphics2D gc = cache.createGraphics();
                drawPaper(gc, getWidth(), getHeight(), withMargin);
                gc.dispose();
            }
            g.drawImage(cache, 0, 0, null);
        }
    }

    /** Hand-drawn button: wobbly frame, highlighter on hover, small shift when pressed. */
    public static class HandButton extends JButton {
        private final long seed;
        private final Color hoverColor;

        public HandButton(String text, long seed, float fontSize, Color hoverColor) {
            super(text);
            this.seed = seed;
            this.hoverColor = hoverColor;
            setFont(handFont(Font.BOLD, fontSize));
            setForeground(INK);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics metrics = getFontMetrics(getFont());
            return new Dimension(metrics.stringWidth(getText()) + 44, metrics.getHeight() + 16);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            ButtonModel model = getModel();
            if (model.isArmed() && model.isPressed()) {
                g2.translate(2, 2);
            }
            int w = getWidth() - 8;
            int h = getHeight() - 8;

            if (model.isRollover()) {
                hatchFill(g2, new Rectangle2D.Double(6, 6, w - 4, h - 4), hoverColor, -0.5, seed + 99, 4.5, 3f);
            }

            g2.setColor(INK);
            handRect(g2, 4, 4, w, h, seed);

            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            FontMetrics metrics = g2.getFontMetrics();
            int x = (getWidth() - metrics.stringWidth(getText())) / 2;
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    /** Label with one or two hand-drawn underlines. */
    public static class HandTitle extends JLabel {
        private final int lineCount;
        private final long seed;

        public HandTitle(String text, float size, int lineCount, long seed) {
            super(text, SwingConstants.CENTER);
            this.lineCount = lineCount;
            this.seed = seed;
            setFont(handFont(Font.BOLD, size));
            setForeground(INK);
            setBorder(BorderFactory.createEmptyBorder(0, 6, 4 + lineCount * 5, 6));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paintComponent(g2);
            FontMetrics metrics = g2.getFontMetrics(getFont());
            int textWidth = metrics.stringWidth(getText());
            int x = (getWidth() - textWidth) / 2;
            int y = getHeight() - getInsets().bottom + 1;
            g2.setColor(INK);
            handUnderline(g2, x - 4, y, textWidth + 8, seed, lineCount, 1.0);
            g2.dispose();
        }
    }

    /** Hand-drawn rectangular border (tooltips, popups). */
    public static class HandBorder extends AbstractBorder {
        private final long seed;

        public HandBorder(long seed) {
            this.seed = seed;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = prepare(g);
            g2.setColor(INK);
            handRect(g2, x + 3, y + 3, width - 7, height - 7, seed);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(7, 10, 7, 10);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(7, 10, 7, 10);
            return insets;
        }
    }

    /** "Fill-in line" under text fields, spinners and combo boxes, like on a paper form. */
    public static class UnderlineBorder extends AbstractBorder {
        private final long seed;

        public UnderlineBorder(long seed) {
            this.seed = seed;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = prepare(g);
            g2.setColor(c.hasFocus() ? INK : GRAPHITE);
            wobblyLine(g2, x + 3, y + height - 4, x + width - 4, y + height - 4, seed);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2, 6, 7, 6);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(2, 6, 7, 6);
            return insets;
        }
    }

    /** Small hand-drawn arrow button replacing the native spinner and combo arrows. */
    public static class ArrowButton extends JButton {
        private final boolean pointingDown;
        private final long seed;

        public ArrowButton(boolean pointingDown, long seed) {
            this.pointingDown = pointingDown;
            this.seed = seed;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(22, 14);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            ButtonModel model = getModel();
            g2.setColor(model.isRollover() || model.isPressed() ? INK : GRAPHITE);
            double shift = (model.isArmed() && model.isPressed()) ? 1 : 0;
            handArrowhead(g2, getWidth() / 2.0 + shift, getHeight() / 2.0 + shift, 4, seed, pointingDown);
            g2.dispose();
        }
    }

    /** Thin pencil-mark scroll bar: no track and no arrow buttons. */
    public static class PencilScrollBarUI extends BasicScrollBarUI {
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return invisibleButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return invisibleButton();
        }

        private JButton invisibleButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle bounds) {
            if (!bounds.isEmpty()) {
                Graphics2D g2 = prepare(g);
                g2.setColor(new Color(110, 110, 110, 170));
                double cx = bounds.x + bounds.width / 2.0;
                wobblyLine(g2, cx, bounds.y + 4, cx, bounds.y + bounds.height - 4, 4242, 1.0, 2f);
                g2.dispose();
            }
        }
    }
}
