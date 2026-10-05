import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Main game window: board, turn indicator and move history. Bridges {@link Partita} events to the Swing UI. */
public class MainGUI extends JFrame implements Partita.PartitaListener, GrigliaPanel.ClickColonnaListener {

    private static final int PROFONDITA_IA = 6;
    private static final int LATO_CELLA = 90;
    private static final int LARGHEZZA_APPUNTI = 250;

    private GrigliaPanel grigliaPanel;
    private JLabel statusLabel;
    private DefaultListModel<String> storiaMosseModel;
    private JList<String> storiaMosseList;

    private Griglia griglia;
    private Giocatore giocatore1;
    private Giocatore giocatore2;
    private String nomeGiocatore1;
    private String nomeGiocatore2;
    private int numeroMossa = 0;
    private Partita partita;
    private Thread threadPartita;

    public MainGUI(int righe, int colonne, int modalita, String apiKey, String nomeGiocatore1, String nomeGiocatore2) {
        this.nomeGiocatore1 = nomeGiocatore1;
        this.nomeGiocatore2 = nomeGiocatore2;
        setTitle("Forza 4");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new Matita.PannelloFoglio(true));
        setLayout(new BorderLayout());

        griglia = new Griglia(righe, colonne);

        giocatore1 = new GiocatoreUmanoGUI(1);
        if (modalita == 0) {
            giocatore2 = new GiocatoreUmanoGUI(2);
        } else if (modalita == 1) {
            giocatore2 = new GiocatoreComputer(2, PROFONDITA_IA);
        } else {
            giocatore2 = new GiocatoreAI(2, new ClienteAI(apiKey));
        }

        grigliaPanel = new GrigliaPanel();
        grigliaPanel.setClickListener(this);
        grigliaPanel.aggiorna(griglia);
        grigliaPanel.setPreferredSize(new Dimension(colonne * LATO_CELLA + 76, righe * LATO_CELLA + 76));
        add(grigliaPanel, BorderLayout.CENTER);

        Matita.PulsanteAMano menuButton = new Matita.PulsanteAMano("Torna al menu", 11, 16f, Matita.GIALLO_SELEZIONE);
        menuButton.addActionListener(e -> tornaAlMenu());

        JPanel barraSuperiore = new JPanel(new BorderLayout(14, 0));
        barraSuperiore.setOpaque(false);
        barraSuperiore.setBorder(BorderFactory.createEmptyBorder(8, 52, 2, 16));

        statusLabel = new JLabel("Turno di " + nomeGiocatore1, SwingConstants.CENTER);
        statusLabel.setFont(Matita.fontAMano(Font.BOLD, 22f));
        statusLabel.setForeground(Matita.PENNA);
        statusLabel.setIcon(ICONE_GIOCATORI[1]);
        statusLabel.setIconTextGap(10);

        barraSuperiore.add(menuButton, BorderLayout.WEST);
        barraSuperiore.add(statusLabel, BorderLayout.CENTER);
        add(barraSuperiore, BorderLayout.NORTH);

        add(creaPannelloStoriaMosse(), BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);

        partita = new Partita(griglia, giocatore1, giocatore2, this);

        // The game runs on its own thread: human moves block in sceglieMossa() waiting
        // for a click, which must not freeze the GUI.
        threadPartita = new Thread(partita::avvia);
        threadPartita.setDaemon(true);
        threadPartita.start();
    }

    private JPanel creaPannelloStoriaMosse() {
        storiaMosseModel = new DefaultListModel<>();
        storiaMosseList = new JList<>(storiaMosseModel);
        storiaMosseList.setOpaque(false);
        storiaMosseList.setCellRenderer(new RendererMossa());
        storiaMosseList.setFixedCellHeight(Matita.INTERLINEA);
        storiaMosseList.setFocusable(false);

        JScrollPane scorrimento = new JScrollPane();
        scorrimento.setViewport(new ViewportQuaderno());
        scorrimento.setViewportView(storiaMosseList);
        scorrimento.setOpaque(false);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar barra = scorrimento.getVerticalScrollBar();
        barra.setUI(new Matita.BarraMatitaUI());
        barra.setOpaque(false);
        barra.setPreferredSize(new Dimension(10, 0));
        barra.setUnitIncrement(Matita.INTERLINEA);

        Matita.TitoloAMano titolo = new Matita.TitoloAMano("Mosse", 22f, 1, 61);

        JPanel pannello = new PannelloAppunti();
        pannello.setLayout(new BorderLayout(0, 6));
        pannello.setBorder(BorderFactory.createEmptyBorder(16, 18, 20, 16));
        pannello.setPreferredSize(new Dimension(LARGHEZZA_APPUNTI, 0));
        pannello.add(titolo, BorderLayout.NORTH);
        pannello.add(scorrimento, BorderLayout.CENTER);
        return pannello;
    }

    private void tornaAlMenu() {
        // Otherwise the game thread would wait forever for a click on a window that no longer exists.
        partita.ferma();
        threadPartita.interrupt(); // Wakes it up if it is waiting for a click or an AI response.
        dispose();

        SchermataIniziale schermata = new SchermataIniziale();
        schermata.setVisible(true);
    }

    /** Forwards the click to both players: only the one currently waiting in sceglieMossa() uses it. */
    @Override
    public void onColonnaCliccata(int colonna) {
        if (giocatore1 instanceof GiocatoreUmanoGUI) {
            ((GiocatoreUmanoGUI) giocatore1).riceviClick(colonna);
        }
        if (giocatore2 instanceof GiocatoreUmanoGUI) {
            ((GiocatoreUmanoGUI) giocatore2).riceviClick(colonna);
        }
    }

    private String nomeDi(int idGiocatore) {
        return (idGiocatore == giocatore1.getId()) ? nomeGiocatore1 : nomeGiocatore2;
    }

    @Override
    public void onMossa(Griglia griglia, int colonna, int idGiocatore) {
        // Listener callbacks arrive on the game thread: hop onto the EDT.
        SwingUtilities.invokeLater(() -> {
            grigliaPanel.animaCaduta(griglia, colonna, idGiocatore);

            numeroMossa++;
            storiaMosseModel.addElement(numeroMossa + ". " + nomeDi(idGiocatore) + " -> colonna " + (colonna + 1));
            storiaMosseList.ensureIndexIsVisible(storiaMosseModel.size() - 1);

            int prossimoGiocatore = (idGiocatore == giocatore1.getId()) ? giocatore2.getId() : giocatore1.getId();
            statusLabel.setText("Turno di " + nomeDi(prossimoGiocatore));
            statusLabel.setIcon(ICONE_GIOCATORI[prossimoGiocatore]);
        });
    }

    @Override
    public void onFinePartita(int idVincitore) {
        SwingUtilities.invokeLater(() -> {

            // No aggiorna(griglia) here: the live grid would show the last disc landed while it is still falling.
            String messaggio;
            int[][] lineaVincente = null;
            if (idVincitore == 0) {
                messaggio = "Pareggio!";
            } else {
                messaggio = "Ha vinto " + nomeDi(idVincitore) + "!";
                lineaVincente = griglia.getLineaVincente(idVincitore);
            }

            statusLabel.setText(messaggio);
            statusLabel.setIcon(idVincitore == 0 ? null : ICONE_GIOCATORI[idVincitore]);
            grigliaPanel.mostraFinePartita(messaggio, lineaVincente);
        });
    }

    private static final Icon[] ICONE_GIOCATORI = {null, new IconaGettone(1), new IconaGettone(2)};

    private static class IconaGettone implements Icon {
        private static final int LATO = 26;
        private final BufferedImage immagine;

        private IconaGettone(int giocatore) {
            immagine = new BufferedImage(LATO, LATO, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = immagine.createGraphics();
            Matita.gettoneAMano(g2, LATO / 2.0, LATO / 2.0, 9.5, giocatore, 300 + giocatore);
            g2.dispose();
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.drawImage(immagine, x, y, null);
        }

        @Override
        public int getIconWidth() {
            return LATO;
        }

        @Override
        public int getIconHeight() {
            return LATO;
        }
    }

    private static class PannelloAppunti extends JPanel {
        private PannelloAppunti() {
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
            g2.setColor(Matita.CARTA);
            g2.fillRect(x, y, w, h);
            g2.setColor(Matita.PENNA);
            Matita.rettangoloAMano(g2, x, y, w, h, 62);
            g2.dispose();
        }
    }

    /** Viewport that draws notebook lines in list coordinates, so lines and entries scroll together. */
    private static class ViewportQuaderno extends JViewport {
        private ViewportQuaderno() {
            setOpaque(false);
            setScrollMode(JViewport.SIMPLE_SCROLL_MODE); // Blit scrolling would smear the ruled lines.
        }

        @Override
        public void setViewPosition(Point posizione) {
            // Snap to whole lines so the top entry is never cut in half.
            int y = ((posizione.y + Matita.INTERLINEA - 1) / Matita.INTERLINEA) * Matita.INTERLINEA;
            super.setViewPosition(new Point(posizione.x, y));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Matita.RIGA_QUADERNO);
            int scostamento = getViewPosition().y % Matita.INTERLINEA;
            for (int y = Matita.INTERLINEA - 1 - scostamento; y < getHeight(); y += Matita.INTERLINEA) {
                g.drawLine(0, y, getWidth(), y);
            }
        }
    }

    private static class RendererMossa extends JComponent implements ListCellRenderer<String> {
        private final Font fontNormale = Matita.fontAMano(Font.PLAIN, 16f);
        private final Font fontPiccolo = Matita.fontAMano(Font.PLAIN, 12.5f);
        private String testo = "";
        private int numero = 1;

        @Override
        public Component getListCellRendererComponent(JList<? extends String> lista, String valore, int indice,
                                                      boolean selezionato, boolean haFocus) {
            testo = valore;
            numero = indice + 1;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int giocatore = (numero % 2 == 1) ? 1 : 2;
            Matita.gettoneAMano(g2, 10, getHeight() / 2.0, 6, giocatore, numero);

            int spazioTesto = getWidth() - 26;
            Font font = (getFontMetrics(fontNormale).stringWidth(testo) <= spazioTesto) ? fontNormale : fontPiccolo;
            g2.setFont(font);
            g2.setColor(Matita.PENNA);
            g2.drawString(testo, 22, getHeight() - 7);
            g2.dispose();
        }
    }
}
