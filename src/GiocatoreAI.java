/** Player driven by a remote LLM (Gemini): the grid is sent as text and the reply parsed as a column. */
public class GiocatoreAI implements Giocatore {

    private int id;
    private int idAvversario;
    private ClienteAI cliente;
    private boolean erroreGiaMostrato = false;

    public GiocatoreAI(int id, ClienteAI cliente) {
        this.id = id;
        this.idAvversario = (id == 1) ? 2 : 1;
        this.cliente = cliente;
    }

    /** Asks the model for a move; falls back to the first free column on errors or invalid answers. */
    public int sceglieMossa(Griglia griglia) {
        String prompt = costruisciPrompt(griglia);
        int colonnaScelta = -1;

        try {
            String risposta = cliente.chiedi(prompt);
            int colonna = estraiColonna(risposta);

            if (griglia.colonnaValida(colonna)) {
                colonnaScelta = colonna;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("Errore nella richiesta all'AI: " + e.getMessage());

            // Show the error dialog only once per game, not on every move.
            if (!erroreGiaMostrato) {
                erroreGiaMostrato = true;
                String messaggio = "La modalità AI non sta funzionando:\n" + e.getMessage()
                        + "\n\nPer questa partita verrà usata una mossa di riserva (prima colonna libera).";
                javax.swing.SwingUtilities.invokeLater(() -> javax.swing.JOptionPane.showMessageDialog(null,
                        messaggio, "Errore modalità AI", javax.swing.JOptionPane.WARNING_MESSAGE));
            }
        }

        if (colonnaScelta == -1) {
            colonnaScelta = primaColonnaValida(griglia);
        }

        return colonnaScelta;
    }

    private String costruisciPrompt(Griglia griglia) {
        StringBuilder sb = new StringBuilder();

        sb.append("Stai giocando a Forza 4. Sei il giocatore ").append(id)
                .append(", l'avversario è il giocatore ").append(idAvversario).append(".\n");
        sb.append("La griglia ha ").append(griglia.getRighe()).append(" righe e ")
                .append(griglia.getColonne()).append(" colonne, le colonne sono indicizzate da 0.\n");
        sb.append("0 = cella vuota, 1 = gettone del giocatore 1, 2 = gettone del giocatore 2.\n");
        sb.append("Griglia attuale (dalla riga più in alto a quella più in basso):\n");

        for (int riga = 0; riga < griglia.getRighe(); riga++) {
            for (int colonna = 0; colonna < griglia.getColonne(); colonna++) {
                sb.append(griglia.getCella(riga, colonna));
            }
            sb.append("\n");
        }

        sb.append("Rispondi SOLO con il numero della colonna in cui vuoi giocare (un intero da 0 a ")
                .append(griglia.getColonne() - 1).append("), senza nessun altro testo.");

        return sb.toString();
    }

    /** Returns the first integer found in the reply, or -1 if there is none. */
    private int estraiColonna(String risposta) {
        int colonna = -1;

        if (risposta != null) {
            StringBuilder numero = new StringBuilder();
            boolean terminato = false;
            int i = 0;

            while (i < risposta.length() && !terminato) {
                char c = risposta.charAt(i);
                if (Character.isDigit(c)) {
                    numero.append(c);
                } else if (numero.length() > 0) {
                    terminato = true;
                }
                i++;
            }

            if (numero.length() > 0) {
                colonna = Integer.parseInt(numero.toString());
            }
        }

        return colonna;
    }

    private int primaColonnaValida(Griglia griglia) {
        int colonnaTrovata = -1;
        int colonna = 0;

        while (colonna < griglia.getColonne() && colonnaTrovata == -1) {
            if (griglia.colonnaValida(colonna)) {
                colonnaTrovata = colonna;
            }
            colonna++;
        }

        return colonnaTrovata;
    }

    public int getId() {
        return id;
    }
}
