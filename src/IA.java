/** Minimax engine with alpha-beta pruning and a heuristic board evaluation. */
public class IA {

    private int profonditaMax;
    private int idIA;
    private int idAvversario;

    private static final int VINCITA = 1000000;
    private static final int SCONFITTA = -1000000;

    public IA(int profonditaMax, int idIA) {
        this.profonditaMax = profonditaMax;
        this.idIA = idIA;
        this.idAvversario = (idIA == 1) ? 2 : 1;
    }

    /** Returns the best column for the AI, searching up to {@code profonditaMax} plies. */
    public int sceglieMossa(Griglia griglia) {
        int migliorColonna = -1;
        int migliorValore = Integer.MIN_VALUE;

        for (int colonna = 0; colonna < griglia.getColonne(); colonna++) {
            if (griglia.colonnaValida(colonna)) {

                Griglia copia = griglia.clonaGriglia();
                copia.inserisciGettone(colonna, idIA);

                int valore = minimax(copia, profonditaMax - 1, false, Integer.MIN_VALUE, Integer.MAX_VALUE);

                if (valore > migliorValore) {
                    migliorValore = valore;
                    migliorColonna = colonna;
                }
            }
        }

        return migliorColonna;
    }

    /**
     * Recursive minimax with alpha-beta pruning.
     *
     * @param turnoMax true when it is the AI's turn (maximizing player)
     * @return the value of the position from the AI's point of view
     */
    private int minimax(Griglia griglia, int profondita, boolean turnoMax, int alpha, int beta) {
        int risultato;

        if (profondita == 0 || griglia.verificaVittoria(idIA) || griglia.verificaVittoria(idAvversario) || griglia.grigliaPiena()) {
            risultato = valutaGriglia(griglia);

            // Prefer faster wins and slower losses; otherwise a lost AI stops defending and plays any column.
            if (risultato == VINCITA) {
                risultato += profondita;
            } else if (risultato == SCONFITTA) {
                risultato -= profondita;
            }

        } else if (turnoMax) {

            int migliore = Integer.MIN_VALUE;
            int colonna = 0;

            // "beta > alpha" in the loop condition replaces the usual break on a cutoff.
            while (colonna < griglia.getColonne() && beta > alpha) {
                if (griglia.colonnaValida(colonna)) {

                    Griglia copia = griglia.clonaGriglia();
                    copia.inserisciGettone(colonna, idIA);

                    int valore = minimax(copia, profondita - 1, false, alpha, beta);
                    migliore = Math.max(migliore, valore);
                    alpha = Math.max(alpha, migliore);
                }
                colonna++;
            }

            risultato = migliore;

        } else {

            int migliore = Integer.MAX_VALUE;
            int colonna = 0;

            while (colonna < griglia.getColonne() && beta > alpha) {
                if (griglia.colonnaValida(colonna)) {

                    Griglia copia = griglia.clonaGriglia();
                    copia.inserisciGettone(colonna, idAvversario);

                    int valore = minimax(copia, profondita - 1, true, alpha, beta);
                    migliore = Math.min(migliore, valore);
                    beta = Math.min(beta, migliore);
                }
                colonna++;
            }

            risultato = migliore;
        }

        return risultato;
    }

    /**
     * Static evaluation: +/-VINCITA for a decided game, otherwise the sum of
     * every horizontal, vertical and diagonal window of four cells.
     */
    private int valutaGriglia(Griglia griglia) {
        int punteggio;

        if (griglia.verificaVittoria(idIA)) {
            punteggio = VINCITA;

        } else if (griglia.verificaVittoria(idAvversario)) {
            punteggio = SCONFITTA;

        } else {
            punteggio = 0;

            int righe = griglia.getRighe();
            int colonne = griglia.getColonne();

            for (int riga = 0; riga < righe; riga++) {
                for (int colonna = 0; colonna <= colonne - 4; colonna++) {
                    int[] finestra = {
                            griglia.getCella(riga, colonna),
                            griglia.getCella(riga, colonna + 1),
                            griglia.getCella(riga, colonna + 2),
                            griglia.getCella(riga, colonna + 3)
                    };
                    punteggio += valutaFinestra(finestra);
                }
            }

            for (int colonna = 0; colonna < colonne; colonna++) {
                for (int riga = 0; riga <= righe - 4; riga++) {
                    int[] finestra = {
                            griglia.getCella(riga, colonna),
                            griglia.getCella(riga + 1, colonna),
                            griglia.getCella(riga + 2, colonna),
                            griglia.getCella(riga + 3, colonna)
                    };
                    punteggio += valutaFinestra(finestra);
                }
            }

            for (int riga = 0; riga <= righe - 4; riga++) {
                for (int colonna = 0; colonna <= colonne - 4; colonna++) {
                    int[] finestra = {
                            griglia.getCella(riga, colonna),
                            griglia.getCella(riga + 1, colonna + 1),
                            griglia.getCella(riga + 2, colonna + 2),
                            griglia.getCella(riga + 3, colonna + 3)
                    };
                    punteggio += valutaFinestra(finestra);
                }
            }

            for (int riga = 3; riga < righe; riga++) {
                for (int colonna = 0; colonna <= colonne - 4; colonna++) {
                    int[] finestra = {
                            griglia.getCella(riga, colonna),
                            griglia.getCella(riga - 1, colonna + 1),
                            griglia.getCella(riga - 2, colonna + 2),
                            griglia.getCella(riga - 3, colonna + 3)
                    };
                    punteggio += valutaFinestra(finestra);
                }
            }
        }

        return punteggio;
    }

    /** Scores a window of four cells; mixed windows can never become a line, so they count 0. */
    private int valutaFinestra(int[] finestra) {

        int mie = 0;
        int avversarie = 0;
        int vuote = 0;

        for (int valore : finestra) {
            if (valore == idIA) {
                mie++;
            } else if (valore == idAvversario) {
                avversarie++;
            } else {
                vuote++;
            }
        }

        int punteggio;

        if (mie > 0 && avversarie > 0) {
            punteggio = 0;
        } else if (mie == 3 && vuote == 1) {
            punteggio = 50;
        } else if (mie == 2 && vuote == 2) {
            punteggio = 10;
        } else if (mie == 1 && vuote == 3) {
            punteggio = 1;
        } else if (avversarie == 3 && vuote == 1) {
            punteggio = -50;
        } else if (avversarie == 2 && vuote == 2) {
            punteggio = -10;
        } else if (avversarie == 1 && vuote == 3) {
            punteggio = -1;
        } else {
            punteggio = 0;
        }

        return punteggio;
    }
}
