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
public class GrigliaPanel extends JPanel {

    private Griglia griglia;

    // Single source of geometry for both drawing and colonnaDaX, so clicks always match what is drawn.
    private static final int MARGINE_SINISTRO = 58;
    private static final int MARGINE_DESTRO = 18;
    private static final int MARGINE_ALTO = 42;
    private static final int MARGINE_BASSO = 34;
    private static final int BORDO_TABELLONE = 8;

    // Static layers, regenerated only when the panel or grid size changes.
    private BufferedImage cacheTabellone;
    private BufferedImage cacheHover;
    private final Map<Long, BufferedImage> cacheGettoni = new HashMap<>();
    private int larghezzaCache = -1;
    private int altezzaCache = -1;
    private int righeCache = -1;
    private int colonneCache = -1;
    private Font fontBanner = null;

    private int colonnaHover = -1;

    private static final double GRAVITA = 0.012;
    private static final double ATTENUAZIONE_RIMBALZO = 0.35;

    private int colonnaAnimata = -1;
    private int rigaFinaleAnimata = -1;
    private double rigaAnimata = 0;
    private double velocitaAnimata = 0;
    private boolean staRimbalzando = false;
    private int giocatoreAnimato = 0;
    private Timer timerAnimazione;

    private Queue<MossaInCoda> codaAnimazioni = new LinkedList<>();

    private int[][] celleVincenti = null;
    private String messaggioFinale = null;
    private int tickFinale = 0;
    private Timer timerFinale;
    // The game-over event arrives while the last disc is still falling: it is parked here
    // and shown only once every queued animation has finished.
    private boolean fineInAttesa = false;
    private String messaggioInAttesa = null;
    private int[][] celleVincentiInAttesa = null;
    private static final int DURATA_TICK_FINALE = 90;

    private ClickColonnaListener listener;

    public GrigliaPanel() {
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (griglia != null && colonnaAnimata == -1) {
                    int colonna = colonnaDaX(e.getX());

                    if (listener != null && griglia.colonnaValida(colonna)) {
                        listener.onColonnaCliccata(colonna);
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                colonnaHover = -1;
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (griglia != null) {
                    int nuovaColonna = colonnaDaX(e.getX());
                    if (nuovaColonna != colonnaHover) {
                        colonnaHover = nuovaColonna;
                        repaint();
                    }
                }
            }
        });
    }

    private int larghezzaCella() {
        return Math.max(1, (getWidth() - MARGINE_SINISTRO - MARGINE_DESTRO) / griglia.getColonne());
    }

    private int altezzaCella() {
        return Math.max(1, (getHeight() - MARGINE_ALTO - MARGINE_BASSO) / griglia.getRighe());
    }

    private int diametroGettone() {
        return Math.max(8, Math.min(larghezzaCella(), altezzaCella()) - 18);
    }

    private double centroX(int colonna) {
        return MARGINE_SINISTRO + colonna * larghezzaCella() + larghezzaCella() / 2.0;
    }

    private double centroY(double riga) {
        return MARGINE_ALTO + riga * altezzaCella() + altezzaCella() / 2.0;
    }

    private int colonnaDaX(int x) {
        // floorDiv instead of "/": left of the board this yields -1 (invalid), not 0.
        return Math.floorDiv(x - MARGINE_SINISTRO, larghezzaCella());
    }

    private static long semeGettone(int giocatore, int riga, int colonna) {
        return giocatore * 10007L + riga * 31L + colonna;
    }

    public void setClickListener(ClickColonnaListener listener) {
        this.listener = listener;
    }

    public void aggiorna(Griglia griglia) {
        this.griglia = griglia;
        repaint();
    }

    public void mostraFinePartita(String messaggio, int[][] celleVincenti) {
        fineInAttesa = true;
        messaggioInAttesa = messaggio;
        celleVincentiInAttesa = celleVincenti;
        avviaFineSeAnimazioniFinite();
    }

    private void avviaFineSeAnimazioniFinite() {
        if (fineInAttesa && colonnaAnimata == -1 && codaAnimazioni.isEmpty()) {
            fineInAttesa = false;
            this.messaggioFinale = messaggioInAttesa;
            this.celleVincenti = celleVincentiInAttesa;
            tickFinale = 0;

            if (celleVincenti != null) {
                Suoni.riproduciVittoria();
            }

            if (timerFinale != null && timerFinale.isRunning()) {
                timerFinale.stop();
            }
            timerFinale = new Timer(40, e -> {
                tickFinale++;
                if (tickFinale >= DURATA_TICK_FINALE) {
                    timerFinale.stop();
                }
                repaint();
            });
            timerFinale.start();
        }
    }

    /**
     * Animates a disc dropping into the column. {@code griglia} must be a snapshot
     * taken right after the move; calls during an animation are queued and played in order.
     */
    public void animaCaduta(Griglia griglia, int colonna, int idGiocatore) {
        if (colonnaAnimata != -1) {
            codaAnimazioni.add(new MossaInCoda(griglia, colonna, idGiocatore));
        } else {
            avviaAnimazione(griglia, colonna, idGiocatore);
        }
    }

    private void avviaAnimazione(Griglia griglia, int colonna, int idGiocatore) {
        this.griglia = griglia;

        int rigaFinale = griglia.getRigaSuperioreOccupata(colonna);

        if (rigaFinale < 0) {
            repaint();
            avviaProssimaAnimazioneInCoda();
        } else {
            colonnaAnimata = colonna;
            rigaFinaleAnimata = rigaFinale;
            rigaAnimata = 0;
            velocitaAnimata = 0;
            staRimbalzando = false;
            giocatoreAnimato = idGiocatore;

            if (timerAnimazione != null && timerAnimazione.isRunning()) {
                timerAnimazione.stop();
            }

            // Simple physics: gravity accelerates the disc, which bounces once on landing and then stops.
            timerAnimazione = new Timer(10, e -> {
                velocitaAnimata += GRAVITA;
                rigaAnimata += velocitaAnimata;

                if (rigaAnimata >= rigaFinaleAnimata) {
                    rigaAnimata = rigaFinaleAnimata;

                    if (!staRimbalzando) {
                        Suoni.riproduciMossa();
                    }

                    if (!staRimbalzando && velocitaAnimata > 0.08) {
                        velocitaAnimata = -velocitaAnimata * ATTENUAZIONE_RIMBALZO;
                        staRimbalzando = true;
                    } else {
                        colonnaAnimata = -1;
                        timerAnimazione.stop();
                        avviaProssimaAnimazioneInCoda();
                    }
                }

                repaint();
            });
            timerAnimazione.start();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (griglia != null) {
            aggiornaCache();

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int righe = griglia.getRighe();
            int colonne = griglia.getColonne();
            int larghezzaCella = larghezzaCella();
            int altezzaCella = altezzaCella();
            int diametro = diametroGettone();

            if (colonnaHover >= 0 && colonnaHover < colonne && messaggioFinale == null) {
                Graphics2D gHover = (Graphics2D) g2d.create();
                gHover.clipRect(MARGINE_SINISTRO + colonnaHover * larghezzaCella, MARGINE_ALTO - BORDO_TABELLONE,
                        larghezzaCella, righe * altezzaCella + BORDO_TABELLONE * 2);
                gHover.drawImage(cacheHover, 0, 0, null);
                gHover.dispose();

                g2d.setColor(Matita.PENNA);
                Matita.frecciaAMano(g2d, centroX(colonnaHover), 8, MARGINE_ALTO - BORDO_TABELLONE - 6, 7,
                        500 + colonnaHover, true);
            }

            g2d.drawImage(cacheTabellone, 0, 0, null);

            for (int riga = 0; riga < righe; riga++) {
                for (int colonna = 0; colonna < colonne; colonna++) {
                    int valore = griglia.getCella(riga, colonna);
                    // The cell of the falling disc is skipped here and drawn separately at its animated position.
                    if (valore != 0 && (colonna != colonnaAnimata || riga != rigaFinaleAnimata)) {
                        disegnaGettone(g2d, centroX(colonna), centroY(riga), diametro, valore, semeGettone(valore, riga, colonna));
                    }
                }
            }

            if (colonnaAnimata != -1) {
                disegnaGettone(g2d, centroX(colonnaAnimata), centroY(rigaAnimata), diametro, giocatoreAnimato,
                        semeGettone(giocatoreAnimato, rigaFinaleAnimata, colonnaAnimata));
            }

            if (celleVincenti != null) {
                disegnaLineaVincente(g2d, diametro);
            }

            if (messaggioFinale != null) {
                disegnaBanner(g2d);
            }
            g2d.dispose();
        }
    }

    /** Redraws the board and hover images if the size changed; disc images are cached by seed. */
    private void aggiornaCache() {
        int larghezza = Math.max(1, getWidth());
        int altezza = Math.max(1, getHeight());
        if (larghezza != larghezzaCache || altezza != altezzaCache
                || griglia.getRighe() != righeCache || griglia.getColonne() != colonneCache) {
            larghezzaCache = larghezza;
            altezzaCache = altezza;
            righeCache = griglia.getRighe();
            colonneCache = griglia.getColonne();
            cacheGettoni.clear();

            int larghezzaCella = larghezzaCella();
            int altezzaCella = altezzaCella();
            double raggioBuco = diametroGettone() / 2.0 + 4;
            double x0 = MARGINE_SINISTRO - BORDO_TABELLONE;
            double y0 = MARGINE_ALTO - BORDO_TABELLONE;
            double larghezzaTabellone = colonneCache * larghezzaCella + BORDO_TABELLONE * 2;
            double altezzaTabellone = righeCache * altezzaCella + BORDO_TABELLONE * 2;
            Rectangle2D corpo = new Rectangle2D.Double(x0, y0, larghezzaTabellone, altezzaTabellone);

            cacheTabellone = new BufferedImage(larghezza, altezza, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gc = cacheTabellone.createGraphics();
            gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Matita.riempimentoTratteggiato(gc, corpo, Matita.MATITA_TENUE, -0.9, 777, 7, 1f);

            gc.setComposite(AlphaComposite.Clear);
            for (int riga = 0; riga < righeCache; riga++) {
                for (int colonna = 0; colonna < colonneCache; colonna++) {
                    gc.fill(new Ellipse2D.Double(centroX(colonna) - raggioBuco, centroY(riga) - raggioBuco, raggioBuco * 2, raggioBuco * 2));
                }
            }
            gc.setComposite(AlphaComposite.SrcOver);

            gc.setColor(Matita.PENNA);
            Matita.rettangoloAMano(gc, x0, y0, larghezzaTabellone, altezzaTabellone, 31, 1.25f);

            double fondo = y0 + altezzaTabellone;
            double base = fondo + MARGINE_BASSO - BORDO_TABELLONE - 12;
            Matita.lineaTremolante(gc, x0 + 16, fondo, x0 + 4, base, 41);
            Matita.lineaTremolante(gc, x0 - 6, base, x0 + 22, base, 42);
            Matita.lineaTremolante(gc, x0 + larghezzaTabellone - 16, fondo, x0 + larghezzaTabellone - 4, base, 43);
            Matita.lineaTremolante(gc, x0 + larghezzaTabellone - 22, base, x0 + larghezzaTabellone + 6, base, 44);

            for (int riga = 0; riga < righeCache; riga++) {
                for (int colonna = 0; colonna < colonneCache; colonna++) {
                    Matita.cerchioAMano(gc, centroX(colonna), centroY(riga), raggioBuco, 1000 + riga * 31 + colonna, 1.0, 0.85f);
                }
            }
            gc.dispose();

            cacheHover = new BufferedImage(larghezza, altezza, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gh = cacheHover.createGraphics();
            Matita.riempimentoTratteggiato(gh, corpo, new Color(31, 58, 147, 40), 1.35, 555, 5, 1.3f);
            gh.dispose();
        }
    }

    private void disegnaGettone(Graphics2D g2d, double cx, double cy, int diametro, int giocatore, long seme) {
        BufferedImage immagine = cacheGettoni.get(seme);
        if (immagine == null) {
            int lato = diametro + 14;
            immagine = new BufferedImage(lato, lato, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gi = immagine.createGraphics();
            Matita.gettoneAMano(gi, lato / 2.0, lato / 2.0, diametro / 2.0, giocatore, seme);
            gi.dispose();
            cacheGettoni.put(seme, immagine);
        }
        g2d.drawImage(immagine, (int) Math.round(cx - immagine.getWidth() / 2.0), (int) Math.round(cy - immagine.getHeight() / 2.0), null);
    }

    private void disegnaLineaVincente(Graphics2D g2d, int diametro) {
        double raggio = diametro / 2.0 + 7;
        g2d.setColor(Matita.PENNA);
        for (int i = 0; i < celleVincenti.length; i++) {
            // Each circle starts 4 ticks after the previous one, so they appear to be drawn one by one.
            double progresso = (tickFinale - i * 4) / 10.0;
            if (progresso > 0) {
                int[] cella = celleVincenti[i];
                Matita.cerchioAMano(g2d, centroX(cella[1]), centroY(cella[0]), raggio, 9000 + cella[0] * 31 + cella[1],
                        Math.min(1, progresso), 1.6f);
            }
        }

        double progressoLinea = (tickFinale - 26) / 12.0;
        if (progressoLinea > 0) {
            int[] primo = celleVincenti[0];
            int[] ultimo = celleVincenti[celleVincenti.length - 1];
            double x1 = centroX(primo[1]);
            double y1 = centroY(primo[0]);
            double x2 = centroX(ultimo[1]);
            double y2 = centroY(ultimo[0]);
            double lunghezza = Math.max(1, Math.hypot(x2 - x1, y2 - y1));
            double prolungamento = raggio * 0.8;
            double ux = (x2 - x1) / lunghezza;
            double uy = (y2 - y1) / lunghezza;
            Matita.lineaTremolante(g2d, x1 - ux * prolungamento, y1 - uy * prolungamento,
                    x2 + ux * prolungamento, y2 + uy * prolungamento, 9100, Math.min(1, progressoLinea), 1.7f);
        }
    }

    private void disegnaBanner(Graphics2D g2d) {
        double progresso = Math.min(1.0, tickFinale / 12.0);
        double scala = 1.0 - Math.pow(1.0 - progresso, 3);

        if (fontBanner == null) {
            float dimensione = 36f;
            Font candidato = Matita.fontAMano(Font.BOLD, dimensione);
            while (getFontMetrics(candidato).stringWidth(messaggioFinale) + 90 > getWidth() && dimensione > 16) {
                dimensione -= 2;
                candidato = Matita.fontAMano(Font.BOLD, dimensione);
            }
            fontBanner = candidato;
        }

        if (scala > 0.01) {
            FontMetrics fm = g2d.getFontMetrics(fontBanner);
            int larghezzaTesto = fm.stringWidth(messaggioFinale);
            double larghezzaBanner = larghezzaTesto + 64;
            double altezzaBanner = fm.getAscent() + 44;

            Graphics2D gb = (Graphics2D) g2d.create();
            gb.translate(getWidth() / 2.0, (MARGINE_ALTO + getHeight() - MARGINE_BASSO) / 2.0);
            gb.rotate(Math.toRadians(-2.5));
            gb.scale(scala, scala);
            double x = -larghezzaBanner / 2;
            double y = -altezzaBanner / 2;

            gb.setColor(new Color(60, 50, 30, 35));
            gb.fill(new Rectangle2D.Double(x + 4, y + 5, larghezzaBanner, altezzaBanner));
            gb.setColor(Matita.CARTA);
            gb.fill(new Rectangle2D.Double(x, y, larghezzaBanner, altezzaBanner));
            gb.setColor(Matita.RIGA_QUADERNO);
            for (double riga = y + Matita.INTERLINEA; riga < y + altezzaBanner - 4; riga += Matita.INTERLINEA) {
                gb.draw(new java.awt.geom.Line2D.Double(x, riga, x + larghezzaBanner, riga));
            }
            gb.setColor(Matita.PENNA);
            Matita.rettangoloAMano(gb, x, y, larghezzaBanner, altezzaBanner, 8080, 1.2f);

            double linea = y + 14 + fm.getAscent() - fm.getDescent() / 2.0;
            if (progresso > 0.6) {
                gb.setFont(fontBanner);
                gb.drawString(messaggioFinale, (float) (-larghezzaTesto / 2.0), (float) linea);
            }

            double progressoSottolineatura = (tickFinale - 12) / 14.0;
            if (progressoSottolineatura > 0) {
                Matita.sottolineaturaAMano(gb, -larghezzaTesto / 2.0 - 6, linea + fm.getDescent() / 2.0 + 4,
                        larghezzaTesto + 12, 8181, 2, Math.min(1, progressoSottolineatura));
            }

            if (tickFinale > 28) {
                disegnaStellina(gb, x + larghezzaBanner + 2, y - 4, 8282);
                disegnaStellina(gb, x - 4, y + altezzaBanner + 2, 8383);
            }
            gb.dispose();
        }
    }

    private void disegnaStellina(Graphics2D g2d, double cx, double cy, long seme) {
        double raggio = 8;
        for (int i = 0; i < 3; i++) {
            double angolo = Math.PI / 3 * i + 0.3;
            double dx = Math.cos(angolo) * raggio;
            double dy = Math.sin(angolo) * raggio;
            Matita.lineaTremolante(g2d, cx - dx, cy - dy, cx + dx, cy + dy, seme + i);
        }
    }

    private void avviaProssimaAnimazioneInCoda() {
        MossaInCoda prossima = codaAnimazioni.poll();
        if (prossima != null) {
            avviaAnimazione(prossima.griglia, prossima.colonna, prossima.idGiocatore);
        } else {
            avviaFineSeAnimazioniFinite();
        }
    }

    private static class MossaInCoda {
        private Griglia griglia;
        private int colonna;
        private int idGiocatore;

        private MossaInCoda(Griglia griglia, int colonna, int idGiocatore) {
            this.griglia = griglia;
            this.colonna = colonna;
            this.idGiocatore = idGiocatore;
        }
    }

    public interface ClickColonnaListener {
        void onColonnaCliccata(int colonna);
    }
}
