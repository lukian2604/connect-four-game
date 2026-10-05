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
public final class Matita {

    public static final Color CARTA = new Color(251, 248, 239);
    public static final Color POST_IT = new Color(255, 246, 196);
    public static final Color RIGA_QUADERNO = new Color(150, 185, 225, 120);
    public static final Color MARGINE_ROSSO = new Color(220, 80, 80, 150);
    public static final Color PENNA = new Color(31, 58, 147);
    public static final Color GRAFITE = new Color(70, 70, 70);
    public static final Color MATITA_TENUE = new Color(80, 80, 80, 45);
    public static final Color ROSSO_PASTELLO = new Color(229, 72, 77, 215);
    public static final Color GIALLO_EVIDENZIATORE = new Color(245, 190, 0, 225);
    public static final Color VERDE_EVIDENZIATORE = new Color(100, 195, 100, 130);
    public static final Color GIALLO_SELEZIONE = new Color(255, 214, 60, 140);

    public static final int INTERLINEA = 26;
    public static final int X_MARGINE_ROSSO = 40;

    private static final double TREMOLIO = 1.1;
    private static final double PASSO_PUNTI = 14;
    private static final float SPESSORE_PRIMO = 1.6f;
    private static final float SPESSORE_SECONDO = 1.1f;
    private static final double PASSO_TRATTEGGIO = 3.4;

    private static Font fontBase = null;

    private Matita() {
    }

    /** Returns the first installed handwriting-like font, falling back to sans-serif. */
    public static synchronized Font fontAMano(int stile, float dimensione) {
        if (fontBase == null) {
            String[] preferiti = {"Noteworthy", "Chalkboard SE", "Bradley Hand", "Marker Felt", "Segoe Print", "Comic Sans MS"};
            String[] installati = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            String scelto = Font.SANS_SERIF;
            boolean trovato = false;
            for (int i = 0; i < preferiti.length && !trovato; i++) {
                for (String nome : installati) {
                    if (!trovato && nome.equalsIgnoreCase(preferiti[i])) {
                        scelto = nome;
                        trovato = true;
                    }
                }
            }
            fontBase = new Font(scelto, Font.PLAIN, 12);
        }
        return fontBase.deriveFont(stile, dimensione);
    }

    /** Works on a copy so callers don't get their stroke, color or clip changed. */
    private static Graphics2D prepara(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /**
     * Every shape is drawn from a fixed seed, so it looks identical on every repaint instead of flickering.
     * Close seeds give similar java.util.Random sequences, so the seed is scrambled first.
     */
    private static Random casuale(long seme) {
        return new Random(seme * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L);
    }

    private static BasicStroke tratto(float spessore) {
        return new BasicStroke(spessore, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    /** Builds one wobbly pen stroke; {@code progresso} (0..1) draws only part of it, for animations. */
    private static Path2D percorsoTremolante(double x1, double y1, double x2, double y2, Random r, double progresso) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lunghezza = Math.max(0.5, Math.hypot(dx, dy));
        double ux = dx / lunghezza;
        double uy = dy / lunghezza;
        double nx = -uy;
        double ny = ux;

        double sporgenzaInizio = -1.5 + r.nextDouble() * 4.0;
        double sporgenzaFine = -1.5 + r.nextDouble() * 4.0;
        double pancia = (r.nextDouble() - 0.5) * Math.min(lunghezza * 0.02, 3.0);
        double inizioX = x1 - ux * sporgenzaInizio;
        double inizioY = y1 - uy * sporgenzaInizio;
        double totale = lunghezza + sporgenzaInizio + sporgenzaFine;

        int segmenti = Math.max(2, (int) (lunghezza / PASSO_PUNTI));
        double[] px = new double[segmenti + 1];
        double[] py = new double[segmenti + 1];
        for (int i = 0; i <= segmenti; i++) {
            double t = (double) i / segmenti;
            double spostamento = pancia * Math.sin(Math.PI * t) + (r.nextDouble() - 0.5) * 2 * TREMOLIO;
            px[i] = inizioX + ux * totale * t + nx * spostamento;
            py[i] = inizioY + uy * totale * t + ny * spostamento;
        }

        Path2D percorso = new Path2D.Double();
        percorso.moveTo(px[0], py[0]);
        double limite = Math.max(0, Math.min(1, progresso)) * segmenti;
        int ultimoIntero = (int) Math.floor(limite);
        for (int i = 1; i <= ultimoIntero; i++) {
            double medioX = (px[i - 1] + px[i]) / 2;
            double medioY = (py[i - 1] + py[i]) / 2;
            // Curve through the midpoints keeps the wobble smooth, without corners.
            percorso.quadTo(px[i - 1], py[i - 1], medioX, medioY);
        }
        if (ultimoIntero < segmenti) {
            double frazione = limite - ultimoIntero;
            percorso.lineTo(px[ultimoIntero] + (px[ultimoIntero + 1] - px[ultimoIntero]) * frazione,
                    py[ultimoIntero] + (py[ultimoIntero + 1] - py[ultimoIntero]) * frazione);
        } else {
            percorso.lineTo(px[segmenti], py[segmenti]);
        }
        return percorso;
    }

    public static void lineaTremolante(Graphics2D g, double x1, double y1, double x2, double y2, long seme) {
        lineaTremolante(g, x1, y1, x2, y2, seme, 1.0, 1f);
    }

    /** Draws a wobbly line twice with a small offset, like retracing it by hand. */
    public static void lineaTremolante(Graphics2D g, double x1, double y1, double x2, double y2, long seme,
                                       double progresso, float scalaSpessore) {
        Graphics2D g2 = prepara(g);
        Random r = casuale(seme);
        for (int passaggio = 0; passaggio < 2; passaggio++) {
            double ox = (r.nextDouble() - 0.5) * 1.6;
            double oy = (r.nextDouble() - 0.5) * 1.6;
            g2.setStroke(tratto((passaggio == 0 ? SPESSORE_PRIMO : SPESSORE_SECONDO) * scalaSpessore));
            g2.draw(percorsoTremolante(x1 + ox, y1 + oy, x2 + ox, y2 + oy, r, progresso));
        }
        g2.dispose();
    }

    public static void rettangoloAMano(Graphics2D g, double x, double y, double larghezza, double altezza, long seme) {
        rettangoloAMano(g, x, y, larghezza, altezza, seme, 1f);
    }

    public static void rettangoloAMano(Graphics2D g, double x, double y, double larghezza, double altezza, long seme, float scalaSpessore) {
        long base = seme * 7;
        lineaTremolante(g, x, y, x + larghezza, y, base + 1, 1.0, scalaSpessore);
        lineaTremolante(g, x + larghezza, y, x + larghezza, y + altezza, base + 2, 1.0, scalaSpessore);
        lineaTremolante(g, x + larghezza, y + altezza, x, y + altezza, base + 3, 1.0, scalaSpessore);
        lineaTremolante(g, x, y + altezza, x, y, base + 4, 1.0, scalaSpessore);
    }

    public static void cerchioAMano(Graphics2D g, double cx, double cy, double raggio, long seme) {
        cerchioAMano(g, cx, cy, raggio, seme, 1.0, 1f);
    }

    /** A slightly irregular circle drawn in two passes whose ends do not meet perfectly. */
    public static void cerchioAMano(Graphics2D g, double cx, double cy, double raggio, long seme,
                                    double progresso, float scalaSpessore) {
        Graphics2D g2 = prepara(g);
        Random r = casuale(seme);
        int punti = Math.max(20, (int) (raggio * 1.3));
        for (int passaggio = 0; passaggio < 2; passaggio++) {
            double inizio = r.nextDouble() * Math.PI * 2;
            double giro = Math.PI * 2 + (-0.15 + r.nextDouble() * 0.45);
            double ampiezza1 = 0.015 + r.nextDouble() * 0.025;
            double fase1 = r.nextDouble() * Math.PI * 2;
            double ampiezza2 = 0.005 + r.nextDouble() * 0.015;
            double fase2 = r.nextDouble() * Math.PI * 2;
            double deriva = (r.nextDouble() - 0.5) * 0.07;
            double ox = (r.nextDouble() - 0.5) * 1.6;
            double oy = (r.nextDouble() - 0.5) * 1.6;

            int ultimo = (int) Math.round(punti * Math.max(0, Math.min(1, progresso)));
            if (ultimo >= 1) {
                Path2D percorso = new Path2D.Double();
                for (int i = 0; i <= ultimo; i++) {
                    double t = (double) i / punti;
                    double angolo = giro * t;
                    double rr = raggio * (1 + ampiezza1 * Math.sin(2 * angolo + fase1)
                            + ampiezza2 * Math.sin(3 * angolo + fase2) + deriva * (t - 0.5));
                    double x = cx + ox + rr * Math.cos(inizio + angolo);
                    double y = cy + oy + rr * Math.sin(inizio + angolo);
                    if (i == 0) {
                        percorso.moveTo(x, y);
                    } else {
                        percorso.lineTo(x, y);
                    }
                }
                g2.setStroke(tratto((passaggio == 0 ? SPESSORE_PRIMO : SPESSORE_SECONDO) * scalaSpessore));
                g2.draw(percorso);
            }
        }
        g2.dispose();
    }

    public static void riempimentoTratteggiato(Graphics2D g, Shape forma, Color colore, double angolo, long seme) {
        riempimentoTratteggiato(g, forma, colore, angolo, seme, PASSO_TRATTEGGIO, 2.3f);
    }

    /** Crayon-like fill: dense slanted zig-zag strokes clipped to the shape with soft edges. */
    public static void riempimentoTratteggiato(Graphics2D g, Shape forma, Color colore, double angolo, long seme,
                                               double passo, float spessoreTratto) {
        Rectangle2D limiti = forma.getBounds2D();
        int bordo = 4;
        int origineX = (int) Math.floor(limiti.getX()) - bordo;
        int origineY = (int) Math.floor(limiti.getY()) - bordo;
        int larghezza = (int) Math.ceil(limiti.getWidth()) + bordo * 2 + 1;
        int altezza = (int) Math.ceil(limiti.getHeight()) + bordo * 2 + 1;

        // Drawn on a separate image and then cut with DstOut: Java2D's clip() gives jagged edges.
        BufferedImage foglietto = new BufferedImage(larghezza, altezza, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gi = foglietto.createGraphics();
        gi.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gi.translate(-origineX, -origineY);

        Random r = casuale(seme);
        double cx = limiti.getCenterX();
        double cy = limiti.getCenterY();
        double semiDiagonale = Math.hypot(limiti.getWidth(), limiti.getHeight()) / 2 + 3;

        gi.setColor(colore);
        for (int strato = 0; strato < 2; strato++) {
            double passoStrato = (strato == 0) ? passo : passo * 2.3;
            double deviazione = (strato == 0) ? 0 : 0.22 + r.nextDouble() * 0.12;
            double cosStrato = Math.cos(angolo + deviazione);
            double senStrato = Math.sin(angolo + deviazione);
            Path2D zigzag = new Path2D.Double();
            boolean versoDestra = true;
            boolean primo = true;
            for (double v = -semiDiagonale; v <= semiDiagonale; v += passoStrato) {
                double u = (versoDestra ? 1 : -1) * (semiDiagonale + r.nextDouble() * 3);
                double vv = v + (r.nextDouble() - 0.5) * passoStrato * 0.5;
                double x = cx + u * cosStrato - vv * senStrato;
                double y = cy + u * senStrato + vv * cosStrato;
                if (primo) {
                    double x0 = cx - u * cosStrato - vv * senStrato;
                    double y0 = cy - u * senStrato + vv * cosStrato;
                    zigzag.moveTo(x0, y0);
                    primo = false;
                }
                zigzag.lineTo(x, y);
                versoDestra = !versoDestra;
            }
            gi.setStroke(tratto(strato == 0 ? spessoreTratto : spessoreTratto * 0.7f));
            gi.draw(zigzag);
        }

        double scala = 1 + 3.0 / Math.max(8, Math.min(limiti.getWidth(), limiti.getHeight()));
        AffineTransform ingrandisci = new AffineTransform();
        ingrandisci.translate(cx, cy);
        ingrandisci.scale(scala, scala);
        ingrandisci.translate(-cx, -cy);
        java.awt.geom.Area fuori = new java.awt.geom.Area(new Rectangle2D.Double(origineX, origineY, larghezza, altezza));
        fuori.subtract(new java.awt.geom.Area(ingrandisci.createTransformedShape(forma)));
        gi.setComposite(AlphaComposite.DstOut);
        gi.setColor(Color.BLACK);
        gi.fill(fuori);
        gi.dispose();

        g.drawImage(foglietto, origineX, origineY, null);
    }

    public static void sottolineaturaAMano(Graphics2D g, double x, double y, double larghezza, long seme,
                                           int quanteLinee, double progresso) {
        Random r = casuale(seme);
        for (int i = 0; i < quanteLinee; i++) {
            double pendenza = (r.nextDouble() - 0.5) * 4;
            double rientro = i * (6 + r.nextDouble() * 6);
            double progressoLinea = Math.max(0, Math.min(1, progresso * quanteLinee - i));
            if (progressoLinea > 0) {
                lineaTremolante(g, x + rientro, y + i * 5, x + larghezza - rientro / 2, y + i * 5 + pendenza,
                        seme * 13 + i, progressoLinea, 1.1f);
            }
        }
    }

    public static void frecciaAMano(Graphics2D g, double cx, double yAlto, double yBasso, double apertura, long seme, boolean versoIlBasso) {
        double punta = versoIlBasso ? yBasso : yAlto;
        double coda = versoIlBasso ? yAlto : yBasso;
        double ritorno = versoIlBasso ? -apertura : apertura;
        lineaTremolante(g, cx, coda, cx, punta, seme * 3 + 1);
        lineaTremolante(g, cx, punta, cx - apertura, punta + ritorno, seme * 3 + 2);
        lineaTremolante(g, cx, punta, cx + apertura, punta + ritorno, seme * 3 + 3);
    }

    public static void puntaAMano(Graphics2D g, double cx, double cy, double apertura, long seme, boolean versoIlBasso) {
        double verso = versoIlBasso ? 1 : -1;
        lineaTremolante(g, cx - apertura, cy - verso * apertura / 2, cx, cy + verso * apertura / 2, seme * 5 + 1);
        lineaTremolante(g, cx, cy + verso * apertura / 2, cx + apertura, cy - verso * apertura / 2, seme * 5 + 2);
    }

    public static void disegnaFoglio(Graphics2D g, int larghezza, int altezza, boolean conMargine) {
        Graphics2D g2 = prepara(g);
        g2.setColor(CARTA);
        g2.fillRect(0, 0, larghezza, altezza);

        Random grana = casuale(2026);
        int puntini = larghezza * altezza / 1600;
        for (int i = 0; i < puntini; i++) {
            g2.setColor(new Color(120, 110, 90, 18 + grana.nextInt(22)));
            g2.fillRect(grana.nextInt(Math.max(1, larghezza)), grana.nextInt(Math.max(1, altezza)), 1, 1);
        }

        g2.setStroke(new BasicStroke(1f));
        g2.setColor(RIGA_QUADERNO);
        for (int y = INTERLINEA; y < altezza; y += INTERLINEA) {
            g2.drawLine(0, y, larghezza, y);
        }

        if (conMargine) {
            g2.setColor(MARGINE_ROSSO);
            g2.setStroke(new BasicStroke(1.3f));
            g2.drawLine(X_MARGINE_ROSSO, 0, X_MARGINE_ROSSO, altezza);
        }
        g2.dispose();
    }

    /** A complete scribbled disc (fill plus outline), used by the board, logo, move list and turn label. */
    public static void gettoneAMano(Graphics2D g, double cx, double cy, double raggio, int giocatore, long seme) {
        Color colore = (giocatore == 1) ? ROSSO_PASTELLO : GIALLO_EVIDENZIATORE;
        double angolo = (giocatore == 1) ? -Math.PI / 4 : Math.PI / 4;
        double passo = Math.max(2.2, Math.min(PASSO_TRATTEGGIO, raggio / 4));
        float spessore = (float) Math.max(1.3, Math.min(2.3, raggio / 9));
        Shape cerchio = new java.awt.geom.Ellipse2D.Double(cx - raggio, cy - raggio, raggio * 2, raggio * 2);
        riempimentoTratteggiato(g, cerchio, colore, angolo, seme, passo, spessore);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(PENNA);
        cerchioAMano(g2, cx, cy, raggio, seme + 17, 1.0, raggio < 12 ? 0.8f : 1f);
        g2.dispose();
    }

    /** Background panel with the notebook page, cached: redrawing the paper grain on every repaint is too slow. */
    public static class PannelloFoglio extends JPanel {
        private final boolean conMargine;
        private BufferedImage cache;

        public PannelloFoglio(boolean conMargine) {
            this.conMargine = conMargine;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (cache == null || cache.getWidth() != getWidth() || cache.getHeight() != getHeight()) {
                cache = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_RGB);
                Graphics2D gc = cache.createGraphics();
                disegnaFoglio(gc, getWidth(), getHeight(), conMargine);
                gc.dispose();
            }
            g.drawImage(cache, 0, 0, null);
        }
    }

    /** Hand-drawn button: wobbly frame, highlighter on hover, small shift when pressed. */
    public static class PulsanteAMano extends JButton {
        private final long seme;
        private final Color coloreHover;

        public PulsanteAMano(String testo, long seme, float dimensioneFont, Color coloreHover) {
            super(testo);
            this.seme = seme;
            this.coloreHover = coloreHover;
            setFont(fontAMano(Font.BOLD, dimensioneFont));
            setForeground(PENNA);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics metriche = getFontMetrics(getFont());
            return new Dimension(metriche.stringWidth(getText()) + 44, metriche.getHeight() + 16);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepara(g);
            ButtonModel modello = getModel();
            if (modello.isArmed() && modello.isPressed()) {
                g2.translate(2, 2);
            }
            int w = getWidth() - 8;
            int h = getHeight() - 8;

            if (modello.isRollover()) {
                riempimentoTratteggiato(g2, new Rectangle2D.Double(6, 6, w - 4, h - 4), coloreHover, -0.5, seme + 99, 4.5, 3f);
            }

            g2.setColor(PENNA);
            rettangoloAMano(g2, 4, 4, w, h, seme);

            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            FontMetrics metriche = g2.getFontMetrics();
            int x = (getWidth() - metriche.stringWidth(getText())) / 2;
            int y = (getHeight() - metriche.getHeight()) / 2 + metriche.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    /** Label with one or two hand-drawn underlines. */
    public static class TitoloAMano extends JLabel {
        private final int quanteLinee;
        private final long seme;

        public TitoloAMano(String testo, float dimensione, int quanteLinee, long seme) {
            super(testo, SwingConstants.CENTER);
            this.quanteLinee = quanteLinee;
            this.seme = seme;
            setFont(fontAMano(Font.BOLD, dimensione));
            setForeground(PENNA);
            setBorder(BorderFactory.createEmptyBorder(0, 6, 4 + quanteLinee * 5, 6));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepara(g);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paintComponent(g2);
            FontMetrics metriche = g2.getFontMetrics(getFont());
            int larghezzaTesto = metriche.stringWidth(getText());
            int x = (getWidth() - larghezzaTesto) / 2;
            int y = getHeight() - getInsets().bottom + 1;
            g2.setColor(PENNA);
            sottolineaturaAMano(g2, x - 4, y, larghezzaTesto + 8, seme, quanteLinee, 1.0);
            g2.dispose();
        }
    }

    /** Hand-drawn rectangular border (tooltips, popups). */
    public static class BordoAMano extends AbstractBorder {
        private final long seme;

        public BordoAMano(long seme) {
            this.seme = seme;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int larghezza, int altezza) {
            Graphics2D g2 = prepara(g);
            g2.setColor(PENNA);
            rettangoloAMano(g2, x + 3, y + 3, larghezza - 7, altezza - 7, seme);
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
    public static class BordoSottolineato extends AbstractBorder {
        private final long seme;

        public BordoSottolineato(long seme) {
            this.seme = seme;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int larghezza, int altezza) {
            Graphics2D g2 = prepara(g);
            g2.setColor(c.hasFocus() ? PENNA : GRAFITE);
            lineaTremolante(g2, x + 3, y + altezza - 4, x + larghezza - 4, y + altezza - 4, seme);
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
    public static class PulsanteFreccia extends JButton {
        private final boolean versoIlBasso;
        private final long seme;

        public PulsanteFreccia(boolean versoIlBasso, long seme) {
            this.versoIlBasso = versoIlBasso;
            this.seme = seme;
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
            Graphics2D g2 = prepara(g);
            ButtonModel modello = getModel();
            g2.setColor(modello.isRollover() || modello.isPressed() ? PENNA : GRAFITE);
            double spostamento = (modello.isArmed() && modello.isPressed()) ? 1 : 0;
            puntaAMano(g2, getWidth() / 2.0 + spostamento, getHeight() / 2.0 + spostamento, 4, seme, versoIlBasso);
            g2.dispose();
        }
    }

    /** Thin pencil-mark scroll bar: no track and no arrow buttons. */
    public static class BarraMatitaUI extends BasicScrollBarUI {
        @Override
        protected JButton createDecreaseButton(int orientamento) {
            return pulsanteInvisibile();
        }

        @Override
        protected JButton createIncreaseButton(int orientamento) {
            return pulsanteInvisibile();
        }

        private JButton pulsanteInvisibile() {
            JButton pulsante = new JButton();
            pulsante.setPreferredSize(new Dimension(0, 0));
            pulsante.setMinimumSize(new Dimension(0, 0));
            pulsante.setMaximumSize(new Dimension(0, 0));
            return pulsante;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle limiti) {
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle limiti) {
            if (!limiti.isEmpty()) {
                Graphics2D g2 = prepara(g);
                g2.setColor(new Color(110, 110, 110, 170));
                double cx = limiti.x + limiti.width / 2.0;
                lineaTremolante(g2, cx, limiti.y + 4, cx, limiti.y + limiti.height - 4, 4242, 1.0, 2f);
                g2.dispose();
            }
        }
    }
}
