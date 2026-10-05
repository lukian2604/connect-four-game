import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

/** Start screen: board size, game mode, player names and the optional Gemini API key. */
public class StartScreen extends JFrame {

    private JSpinner rowsSpinner;
    private JSpinner columnsSpinner;
    private JComboBox<String> modeCombo;
    private JTextField player1NameField;
    private JTextField player2NameField;
    private JPanel namesCard;
    private JTextField apiKeyField;
    private JPanel apiKeyCard;

    public StartScreen() {
        setTitle("Connect Four");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        Sketch.PaperPanel panel = new Sketch.PaperPanel(true);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(22, 62, 28, 40));

        panel.add(createHeader());
        panel.add(Box.createVerticalStrut(18));

        panel.add(createCard(createRow("Rows", rowsSpinner = createSpinner(6, 21)), 1));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createCard(createRow("Columns", columnsSpinner = createSpinner(7, 22)), 2));
        panel.add(Box.createVerticalStrut(10));

        String[] modeOptions = {"Human vs Human", "Human vs Computer", "Human vs AI"};
        modeCombo = new JComboBox<>(modeOptions);
        styleComboBox(modeCombo);
        panel.add(createCard(createRow("Mode", modeCombo), 3));
        panel.add(Box.createVerticalStrut(10));

        player1NameField = new JTextField(10);
        styleHandField(player1NameField, 24);
        player2NameField = new JTextField(10);
        styleHandField(player2NameField, 25);
        namesCard = createCard(createNamesRow(), 4);
        panel.add(namesCard);
        panel.add(Box.createVerticalStrut(10));

        apiKeyField = new JTextField(15);
        styleHandField(apiKeyField, 26);
        apiKeyCard = createCard(createApiKeyRow("AI API key", apiKeyField), 5);
        panel.add(apiKeyCard);
        panel.add(Box.createVerticalStrut(20));

        panel.add(createStartButton());

        add(panel);

        modeCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateFieldVisibility();
            }
        });
        updateFieldVisibility();

        pack();
        setLocationRelativeTo(null);
    }

    /** Shows the name fields only for human vs human and the API key only for the AI mode. */
    private void updateFieldVisibility() {
        int mode = modeCombo.getSelectedIndex();
        namesCard.setVisible(mode == 0);
        apiKeyCard.setVisible(mode == 2);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        Sketch.HandTitle title = new Sketch.HandTitle("CONNECT 4", 50f, 2, 7);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        DiscsPanel decorativeDiscs = new DiscsPanel();
        decorativeDiscs.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(8));
        header.add(decorativeDiscs);

        return header;
    }

    private JSpinner createSpinner(int initialValue, long seed) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(initialValue, 4, 8, 1));

        // Basic UI so the native Aqua arrows can be replaced by hand-drawn ones.
        spinner.setUI(new javax.swing.plaf.basic.BasicSpinnerUI() {
            @Override
            protected Component createNextButton() {
                Component button = new Sketch.ArrowButton(false, seed * 2);
                installNextButtonListeners(button);
                return button;
            }

            @Override
            protected Component createPreviousButton() {
                Component button = new Sketch.ArrowButton(true, seed * 2 + 1);
                installPreviousButtonListeners(button);
                return button;
            }
        });
        spinner.setOpaque(false);
        spinner.setBorder(new Sketch.UnderlineBorder(seed));

        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.setOpaque(false);
        JFormattedTextField field = editor.getTextField();
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        field.setFont(Sketch.handFont(Font.BOLD, 19f));
        field.setForeground(Sketch.INK);
        field.setCaretColor(Sketch.INK);
        field.setHorizontalAlignment(JTextField.LEFT);
        return spinner;
    }

    private void styleHandField(JTextField field, long seed) {
        field.setOpaque(false);
        field.setBorder(new Sketch.UnderlineBorder(seed));
        field.setFont(Sketch.handFont(Font.PLAIN, 18f));
        field.setForeground(Sketch.INK);
        field.setCaretColor(Sketch.INK);
        field.setSelectionColor(Sketch.SELECTION_YELLOW);
        field.setSelectedTextColor(Sketch.INK);
    }

    private void styleComboBox(JComboBox<String> combo) {
        // On macOS the native Aqua combo ignores setBackground/setBorder: switch to the basic UI to draw it ourselves.
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                return new Sketch.ArrowButton(true, 33);
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            }

            @Override
            protected javax.swing.plaf.basic.ComboPopup createPopup() {
                javax.swing.plaf.basic.BasicComboPopup menu = new javax.swing.plaf.basic.BasicComboPopup(comboBox);
                menu.setBorder(new Sketch.HandBorder(34));
                menu.setBackground(Sketch.PAPER);
                return menu;
            }
        });

        // JComboBox ignores the foreground color of the displayed value, so a custom renderer draws it.
        combo.setRenderer(new HandItem());
        combo.setOpaque(false);
        combo.setBackground(Sketch.PAPER);
        combo.setForeground(Sketch.INK);
        combo.setFont(Sketch.handFont(Font.PLAIN, 18f));
        combo.setBorder(new Sketch.UnderlineBorder(35));
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private JPanel createCard(JComponent content, long seed) {
        HandCard card = new HandCard(seed);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createNamesRow() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.add(createRow("Player 1 name", player1NameField));
        container.add(Box.createVerticalStrut(8));
        container.add(createRow("Player 2 name", player2NameField));
        return container;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Sketch.INK);
        label.setFont(Sketch.handFont(Font.PLAIN, 19f));
        return label;
    }

    private JPanel createRow(String labelText, JComponent component) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.add(createLabel(labelText), BorderLayout.WEST);
        row.add(component, BorderLayout.CENTER);
        return row;
    }

    private JPanel createApiKeyRow(String labelText, JComponent component) {
        JPanel labelWithHelp = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        labelWithHelp.setOpaque(false);

        String helpText = "<html>No key is written in the code: here you can paste<br>"
                + "your free key from aistudio.google.com.<br>"
                + "If you leave the field empty, the game tries to<br>"
                + "read it from the GEMINI_API_KEY environment variable, if you set it.</html>";

        JLabel label = createLabel(labelText);
        label.setToolTipText(helpText);

        labelWithHelp.add(label);
        labelWithHelp.add(createHelpDot(helpText));

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.add(labelWithHelp, BorderLayout.WEST);
        row.add(component, BorderLayout.CENTER);
        return row;
    }

    private JComponent createHelpDot(String helpText) {
        HelpDot dot = new HelpDot();
        dot.setToolTipText(helpText);
        return dot;
    }

    private JButton createStartButton() {
        Sketch.HandButton button = new Sketch.HandButton("START", 5, 24f, Sketch.GREEN_HIGHLIGHTER);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(1000, button.getPreferredSize().height));
        button.addActionListener(e -> startGame());
        return button;
    }

    /** Reads the settings and opens the game window; an empty API key field falls back to GEMINI_API_KEY. */
    private void startGame() {
        int rows = (Integer) rowsSpinner.getValue();
        int columns = (Integer) columnsSpinner.getValue();
        int mode = modeCombo.getSelectedIndex();

        String apiKey = apiKeyField.getText().trim();
        if (apiKey.isEmpty()) {
            apiKey = System.getenv("GEMINI_API_KEY");
        }

        String name1 = "Player 1";
        String name2 = "Player 2";
        if (mode == 0) {
            if (!player1NameField.getText().trim().isEmpty()) {
                name1 = player1NameField.getText().trim();
            }
            if (!player2NameField.getText().trim().isEmpty()) {
                name2 = player2NameField.getText().trim();
            }
        } else if (mode == 1) {
            name2 = "Computer";
        } else {
            name2 = "AI";
        }

        dispose();

        GameWindow gameWindow = new GameWindow(rows, columns, mode, apiKey, name1, name2);
        gameWindow.setVisible(true);
    }

    private static class HandCard extends JPanel {
        private final long seed;

        private HandCard(long seed) {
            super(new BorderLayout());
            this.seed = seed;
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
            Sketch.handRect(g2, 3, 3, getWidth() - 7, getHeight() - 7, seed);
            g2.dispose();
        }
    }

    private static class HandItem extends JLabel implements ListCellRenderer<String> {
        private boolean highlighted = false;
        private int index = -1;

        private HandItem() {
            setOpaque(false);
            setFont(Sketch.handFont(Font.PLAIN, 18f));
            setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            setText(value);
            this.index = index;
            highlighted = isSelected && index >= 0;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (highlighted) {
                Sketch.hatchFill(g2, new java.awt.geom.Rectangle2D.Double(4, 3, getWidth() - 8, getHeight() - 6),
                        Sketch.SELECTION_YELLOW, -0.35, 700 + index, 3.5, 3.2f);
            }
            g2.setFont(getFont());
            g2.setColor(Sketch.INK);
            FontMetrics metrics = g2.getFontMetrics();
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(getText(), getInsets().left, y);
            g2.dispose();
        }
    }

    private static class DiscsPanel extends JPanel {
        private static final int DIAMETER = 30;
        private static final int GAP = 14;

        private DiscsPanel() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(4 * DIAMETER + 3 * GAP + 16, DIAMETER + 10);
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
            int totalWidth = 4 * DIAMETER + 3 * GAP;
            double x = (getWidth() - totalWidth) / 2.0 + DIAMETER / 2.0;
            for (int i = 0; i < 4; i++) {
                int player = (i % 2 == 0) ? 1 : 2;
                Sketch.handDisc(g2d, x, getHeight() / 2.0, DIAMETER / 2.0, player, 401 + i);
                x += DIAMETER + GAP;
            }
        }
    }

    private static class HelpDot extends JComponent {
        private final Font markFont = Sketch.handFont(Font.BOLD, 15f);

        private HelpDot() {
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
            g2d.setColor(Sketch.INK);
            Sketch.handCircle(g2d, getWidth() / 2.0, getHeight() / 2.0, 9.5, 71, 1.0, 0.85f);

            g2d.setFont(markFont);
            FontMetrics metrics = g2d.getFontMetrics();
            String text = "?";
            int x = (getWidth() - metrics.stringWidth(text)) / 2;
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2d.drawString(text, x, y);
            g2d.dispose();
        }
    }
}
