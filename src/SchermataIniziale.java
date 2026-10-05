import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

/** Start screen: board size, game mode, player names and the optional Gemini API key. */
public class SchermataIniziale extends JFrame {

    private JSpinner righeSpinner;
    private JSpinner colonneSpinner;
    private JComboBox<String> modalitaCombo;
    private JTextField nomeGiocatore1Field;
    private JTextField nomeGiocatore2Field;
    private JPanel cardNomi;
    private JTextField apiKeyField;
    private JPanel cardApiKey;

    public SchermataIniziale() {
        setTitle("Forza 4");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        Matita.PannelloFoglio pannello = new Matita.PannelloFoglio(true);
        pannello.setLayout(new BoxLayout(pannello, BoxLayout.Y_AXIS));
        pannello.setBorder(BorderFactory.createEmptyBorder(22, 62, 28, 40));

        pannello.add(creaIntestazione());
        pannello.add(Box.createVerticalStrut(18));

        pannello.add(creaCard(creaRiga("Righe", righeSpinner = creaSpinner(6, 21)), 1));
        pannello.add(Box.createVerticalStrut(10));
        pannello.add(creaCard(creaRiga("Colonne", colonneSpinner = creaSpinner(7, 22)), 2));
        pannello.add(Box.createVerticalStrut(10));

        String[] opzioniModalita = {"Umano vs Umano", "Umano vs Computer", "Umano vs AI"};
        modalitaCombo = new JComboBox<>(opzioniModalita);
        stilizzaComboBox(modalitaCombo);
        pannello.add(creaCard(creaRiga("Modalità", modalitaCombo), 3));
        pannello.add(Box.createVerticalStrut(10));

        nomeGiocatore1Field = new JTextField(10);
        stilizzaCampoAMano(nomeGiocatore1Field, 24);
        nomeGiocatore2Field = new JTextField(10);
        stilizzaCampoAMano(nomeGiocatore2Field, 25);
        cardNomi = creaCard(creaRigaNomi(), 4);
        pannello.add(cardNomi);
        pannello.add(Box.createVerticalStrut(10));

        apiKeyField = new JTextField(15);
        stilizzaCampoAMano(apiKeyField, 26);
        cardApiKey = creaCard(creaRigaApiKey("API key AI", apiKeyField), 5);
        pannello.add(cardApiKey);
        pannello.add(Box.createVerticalStrut(20));

        pannello.add(creaPulsanteInizia());

        add(pannello);

        modalitaCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                aggiornaVisibilitaCampi();
            }
        });
        aggiornaVisibilitaCampi();

        pack();
        setLocationRelativeTo(null);
    }

    /** Shows the name fields only for human vs human and the API key only for the AI mode. */
    private void aggiornaVisibilitaCampi() {
        int modalita = modalitaCombo.getSelectedIndex();
        cardNomi.setVisible(modalita == 0);
        cardApiKey.setVisible(modalita == 2);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel creaIntestazione() {
        JPanel intestazione = new JPanel();
        intestazione.setLayout(new BoxLayout(intestazione, BoxLayout.Y_AXIS));
        intestazione.setOpaque(false);
        intestazione.setAlignmentX(Component.CENTER_ALIGNMENT);

        Matita.TitoloAMano titolo = new Matita.TitoloAMano("FORZA 4", 50f, 2, 7);
        titolo.setAlignmentX(Component.CENTER_ALIGNMENT);

        PannelloGettoni gettoniDecorativi = new PannelloGettoni();
        gettoniDecorativi.setAlignmentX(Component.CENTER_ALIGNMENT);

        intestazione.add(titolo);
        intestazione.add(Box.createVerticalStrut(8));
        intestazione.add(gettoniDecorativi);

        return intestazione;
    }

    private JSpinner creaSpinner(int valoreIniziale, long seme) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(valoreIniziale, 4, 8, 1));

        // Basic UI so the native Aqua arrows can be replaced by hand-drawn ones.
        spinner.setUI(new javax.swing.plaf.basic.BasicSpinnerUI() {
            @Override
            protected Component createNextButton() {
                Component pulsante = new Matita.PulsanteFreccia(false, seme * 2);
                installNextButtonListeners(pulsante);
                return pulsante;
            }

            @Override
            protected Component createPreviousButton() {
                Component pulsante = new Matita.PulsanteFreccia(true, seme * 2 + 1);
                installPreviousButtonListeners(pulsante);
                return pulsante;
            }
        });
        spinner.setOpaque(false);
        spinner.setBorder(new Matita.BordoSottolineato(seme));

        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.setOpaque(false);
        JFormattedTextField campo = editor.getTextField();
        campo.setOpaque(false);
        campo.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        campo.setFont(Matita.fontAMano(Font.BOLD, 19f));
        campo.setForeground(Matita.PENNA);
        campo.setCaretColor(Matita.PENNA);
        campo.setHorizontalAlignment(JTextField.LEFT);
        return spinner;
    }

    private void stilizzaCampoAMano(JTextField campo, long seme) {
        campo.setOpaque(false);
        campo.setBorder(new Matita.BordoSottolineato(seme));
        campo.setFont(Matita.fontAMano(Font.PLAIN, 18f));
        campo.setForeground(Matita.PENNA);
        campo.setCaretColor(Matita.PENNA);
        campo.setSelectionColor(Matita.GIALLO_SELEZIONE);
        campo.setSelectedTextColor(Matita.PENNA);
    }

    private void stilizzaComboBox(JComboBox<String> combo) {
        // On macOS the native Aqua combo ignores setBackground/setBorder: switch to the basic UI to draw it ourselves.
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                return new Matita.PulsanteFreccia(true, 33);
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle limiti, boolean haFocus) {
            }

            @Override
            protected javax.swing.plaf.basic.ComboPopup createPopup() {
                javax.swing.plaf.basic.BasicComboPopup menu = new javax.swing.plaf.basic.BasicComboPopup(comboBox);
                menu.setBorder(new Matita.BordoAMano(34));
                menu.setBackground(Matita.CARTA);
                return menu;
            }
        });

        // JComboBox ignores the foreground color of the displayed value, so a custom renderer draws it.
        combo.setRenderer(new VoceAMano());
        combo.setOpaque(false);
        combo.setBackground(Matita.CARTA);
        combo.setForeground(Matita.PENNA);
        combo.setFont(Matita.fontAMano(Font.PLAIN, 18f));
        combo.setBorder(new Matita.BordoSottolineato(35));
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private JPanel creaCard(JComponent contenuto, long seme) {
        CardAMano card = new CardAMano(seme);
        card.add(contenuto, BorderLayout.CENTER);
        return card;
    }

    private JPanel creaRigaNomi() {
        JPanel contenitore = new JPanel();
        contenitore.setLayout(new BoxLayout(contenitore, BoxLayout.Y_AXIS));
        contenitore.setOpaque(false);
        contenitore.add(creaRiga("Nome Giocatore 1", nomeGiocatore1Field));
        contenitore.add(Box.createVerticalStrut(8));
        contenitore.add(creaRiga("Nome Giocatore 2", nomeGiocatore2Field));
        return contenitore;
    }

    private JLabel creaEtichetta(String testo) {
        JLabel etichetta = new JLabel(testo);
        etichetta.setForeground(Matita.PENNA);
        etichetta.setFont(Matita.fontAMano(Font.PLAIN, 19f));
        return etichetta;
    }

    private JPanel creaRiga(String testoEtichetta, JComponent componente) {
        JPanel riga = new JPanel(new BorderLayout(12, 0));
        riga.setOpaque(false);
        riga.add(creaEtichetta(testoEtichetta), BorderLayout.WEST);
        riga.add(componente, BorderLayout.CENTER);
        return riga;
    }

    private JPanel creaRigaApiKey(String testoEtichetta, JComponent componente) {
        JPanel etichettaConAiuto = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        etichettaConAiuto.setOpaque(false);

        String testoAiuto = "<html>Nessuna chiave è scritta nel codice: qui puoi incollare<br>"
                + "la tua chiave gratuita di aistudio.google.com.<br>"
                + "In alternativa, lasciando il campo vuoto, il gioco prova a<br>"
                + "leggerla dalla variabile d'ambiente GEMINI_API_KEY, se l'hai impostata tu.</html>";

        JLabel etichetta = creaEtichetta(testoEtichetta);
        etichetta.setToolTipText(testoAiuto);

        etichettaConAiuto.add(etichetta);
        etichettaConAiuto.add(creaPallinoAiuto(testoAiuto));

        JPanel riga = new JPanel(new BorderLayout(12, 0));
        riga.setOpaque(false);
        riga.add(etichettaConAiuto, BorderLayout.WEST);
        riga.add(componente, BorderLayout.CENTER);
        return riga;
    }

    private JComponent creaPallinoAiuto(String testoAiuto) {
        PallinoAiuto pallino = new PallinoAiuto();
        pallino.setToolTipText(testoAiuto);
        return pallino;
    }

    private JButton creaPulsanteInizia() {
        Matita.PulsanteAMano pulsante = new Matita.PulsanteAMano("INIZIA", 5, 24f, Matita.VERDE_EVIDENZIATORE);
        pulsante.setAlignmentX(Component.CENTER_ALIGNMENT);
        pulsante.setMaximumSize(new Dimension(1000, pulsante.getPreferredSize().height));
        pulsante.addActionListener(e -> avviaPartita());
        return pulsante;
    }

    /** Reads the settings and opens the game window; an empty API key field falls back to GEMINI_API_KEY. */
    private void avviaPartita() {
        int righe = (Integer) righeSpinner.getValue();
        int colonne = (Integer) colonneSpinner.getValue();
        int modalita = modalitaCombo.getSelectedIndex();

        String apiKey = apiKeyField.getText().trim();
        if (apiKey.isEmpty()) {
            apiKey = System.getenv("GEMINI_API_KEY");
        }

        String nome1 = "Giocatore 1";
        String nome2 = "Giocatore 2";
        if (modalita == 0) {
            if (!nomeGiocatore1Field.getText().trim().isEmpty()) {
                nome1 = nomeGiocatore1Field.getText().trim();
            }
            if (!nomeGiocatore2Field.getText().trim().isEmpty()) {
                nome2 = nomeGiocatore2Field.getText().trim();
            }
        } else if (modalita == 1) {
            nome2 = "Computer";
        } else {
            nome2 = "AI";
        }

        dispose();

        MainGUI gioco = new MainGUI(righe, colonne, modalita, apiKey, nome1, nome2);
        gioco.setVisible(true);
    }

    private static class CardAMano extends JPanel {
        private final long seme;

        private CardAMano(long seme) {
            super(new BorderLayout());
            this.seme = seme;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 18, 12, 18));
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        // Full width but exactly as tall as its content, so BoxLayout neither squashes nor stretches it.
        @Override
        public Dimension getMaximumSize() {
            return new Dimension(1000, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(31, 58, 147, 190));
            Matita.rettangoloAMano(g2, 3, 3, getWidth() - 7, getHeight() - 7, seme);
            g2.dispose();
        }
    }

    private static class VoceAMano extends JLabel implements ListCellRenderer<String> {
        private boolean evidenziata = false;
        private int indice = -1;

        private VoceAMano() {
            setOpaque(false);
            setFont(Matita.fontAMano(Font.PLAIN, 18f));
            setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> lista, String valore, int indice,
                                                      boolean selezionato, boolean haFocus) {
            setText(valore);
            this.indice = indice;
            evidenziata = selezionato && indice >= 0;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (evidenziata) {
                Matita.riempimentoTratteggiato(g2, new java.awt.geom.Rectangle2D.Double(4, 3, getWidth() - 8, getHeight() - 6),
                        Matita.GIALLO_SELEZIONE, -0.35, 700 + indice, 3.5, 3.2f);
            }
            g2.setFont(getFont());
            g2.setColor(Matita.PENNA);
            FontMetrics metriche = g2.getFontMetrics();
            int y = (getHeight() - metriche.getHeight()) / 2 + metriche.getAscent();
            g2.drawString(getText(), getInsets().left, y);
            g2.dispose();
        }
    }

    private static class PannelloGettoni extends JPanel {
        private static final int DIAMETRO = 30;
        private static final int SPAZIO = 14;

        private PannelloGettoni() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(4 * DIAMETRO + 3 * SPAZIO + 16, DIAMETRO + 10);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            int larghezzaTotale = 4 * DIAMETRO + 3 * SPAZIO;
            double x = (getWidth() - larghezzaTotale) / 2.0 + DIAMETRO / 2.0;
            for (int i = 0; i < 4; i++) {
                int giocatore = (i % 2 == 0) ? 1 : 2;
                Matita.gettoneAMano(g2d, x, getHeight() / 2.0, DIAMETRO / 2.0, giocatore, 401 + i);
                x += DIAMETRO + SPAZIO;
            }
        }
    }

    private static class PallinoAiuto extends JComponent {
        private final Font fontPunto = Matita.fontAMano(Font.BOLD, 15f);

        private PallinoAiuto() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(26, 26);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setColor(Matita.PENNA);
            Matita.cerchioAMano(g2d, getWidth() / 2.0, getHeight() / 2.0, 9.5, 71, 1.0, 0.85f);

            g2d.setFont(fontPunto);
            FontMetrics metriche = g2d.getFontMetrics();
            String testo = "?";
            int x = (getWidth() - metriche.stringWidth(testo)) / 2;
            int y = (getHeight() - metriche.getHeight()) / 2 + metriche.getAscent();
            g2d.drawString(testo, x, y);
            g2d.dispose();
        }
    }
}
