/** Connect Four board: cells hold 0 (empty) or the id of the player who owns them. Row 0 is the top. */
public class Griglia {

    private int[][] celle;
    private int righe;
    private int colonne;

    public Griglia(int righe, int colonne) {
        this.righe = righe;
        this.colonne = colonne;
        this.celle = new int[righe][colonne];
    }

    public boolean colonnaValida(int colonna) {
        boolean valida;

        if (colonna < 0 || colonna >= colonne) {
            valida = false;
        } else {
            valida = celle[0][colonna] == 0;
        }

        return valida;
    }

    /** Drops a disc into the column; returns false if the column is full or out of range. */
    public boolean inserisciGettone(int colonna, int giocatore) {
        boolean inserito = false;

        if (colonnaValida(colonna)) {
            int riga = righe - 1;
            while (riga >= 0 && !inserito) {
                if (celle[riga][colonna] == 0) {
                    celle[riga][colonna] = giocatore;
                    inserito = true;
                }
                riga--;
            }
        }

        return inserito;
    }

    public boolean grigliaPiena() {
        boolean piena = true;
        int colonna = 0;

        while (colonna < colonne && piena) {
            if (celle[0][colonna] == 0) {
                piena = false;
            }
            colonna++;
        }

        return piena;
    }

    public boolean verificaVittoria(int giocatore) {
        return getLineaVincente(giocatore) != null;
    }

    /** Returns the {row, column} pairs of a winning line for the player, or null if there is none. */
    public int[][] getLineaVincente(int giocatore) {
        int[][] lineaVincente = cercaOrizzontale(giocatore);

        if (lineaVincente == null) {
            lineaVincente = cercaVerticale(giocatore);
        }
        if (lineaVincente == null) {
            lineaVincente = cercaDiagonaleDiscendente(giocatore);
        }
        if (lineaVincente == null) {
            lineaVincente = cercaDiagonaleAscendente(giocatore);
        }

        return lineaVincente;
    }

    private int[][] cercaOrizzontale(int giocatore) {
        int[][] lineaVincente = null;

        for (int riga = 0; riga < righe && lineaVincente == null; riga++) {
            for (int colonna = 0; colonna <= colonne - 4 && lineaVincente == null; colonna++) {
                if (celle[riga][colonna] == giocatore
                        && celle[riga][colonna + 1] == giocatore
                        && celle[riga][colonna + 2] == giocatore
                        && celle[riga][colonna + 3] == giocatore) {
                    lineaVincente = new int[][]{{riga, colonna}, {riga, colonna + 1}, {riga, colonna + 2}, {riga, colonna + 3}};
                }
            }
        }

        return lineaVincente;
    }

    private int[][] cercaVerticale(int giocatore) {
        int[][] lineaVincente = null;

        for (int colonna = 0; colonna < colonne && lineaVincente == null; colonna++) {
            for (int riga = 0; riga <= righe - 4 && lineaVincente == null; riga++) {
                if (celle[riga][colonna] == giocatore
                        && celle[riga + 1][colonna] == giocatore
                        && celle[riga + 2][colonna] == giocatore
                        && celle[riga + 3][colonna] == giocatore) {
                    lineaVincente = new int[][]{{riga, colonna}, {riga + 1, colonna}, {riga + 2, colonna}, {riga + 3, colonna}};
                }
            }
        }

        return lineaVincente;
    }

    private int[][] cercaDiagonaleDiscendente(int giocatore) {
        int[][] lineaVincente = null;

        for (int riga = 0; riga <= righe - 4 && lineaVincente == null; riga++) {
            for (int colonna = 0; colonna <= colonne - 4 && lineaVincente == null; colonna++) {
                if (celle[riga][colonna] == giocatore
                        && celle[riga + 1][colonna + 1] == giocatore
                        && celle[riga + 2][colonna + 2] == giocatore
                        && celle[riga + 3][colonna + 3] == giocatore) {
                    lineaVincente = new int[][]{{riga, colonna}, {riga + 1, colonna + 1}, {riga + 2, colonna + 2}, {riga + 3, colonna + 3}};
                }
            }
        }

        return lineaVincente;
    }

    private int[][] cercaDiagonaleAscendente(int giocatore) {
        int[][] lineaVincente = null;

        for (int riga = 3; riga < righe && lineaVincente == null; riga++) {
            for (int colonna = 0; colonna <= colonne - 4 && lineaVincente == null; colonna++) {
                if (celle[riga][colonna] == giocatore
                        && celle[riga - 1][colonna + 1] == giocatore
                        && celle[riga - 2][colonna + 2] == giocatore
                        && celle[riga - 3][colonna + 3] == giocatore) {
                    lineaVincente = new int[][]{{riga, colonna}, {riga - 1, colonna + 1}, {riga - 2, colonna + 2}, {riga - 3, colonna + 3}};
                }
            }
        }

        return lineaVincente;
    }

    public void stampaGriglia() {
        for (int riga = 0; riga < righe; riga++) {
            for (int colonna = 0; colonna < colonne; colonna++) {
                System.out.print("[" + celle[riga][colonna] + "]");
            }
            System.out.println();
        }
    }

    /** Deep copy, used by the AI to simulate moves and by the GUI to get a stable snapshot. */
    public Griglia clonaGriglia() {
        Griglia copia = new Griglia(righe, colonne);
        for (int riga = 0; riga < righe; riga++) {
            for (int colonna = 0; colonna < colonne; colonna++) {
                copia.celle[riga][colonna] = this.celle[riga][colonna];
            }
        }
        return copia;
    }

    public int getRighe() {
        return righe;
    }

    public int getColonne() {
        return colonne;
    }

    public int getCella(int riga, int colonna) {
        return celle[riga][colonna];
    }

    /** Returns the row of the topmost disc in the column, or -1 if the column is empty. */
    public int getRigaSuperioreOccupata(int colonna) {
        int rigaTrovata = -1;
        int riga = 0;

        while (riga < righe && rigaTrovata == -1) {
            if (celle[riga][colonna] != 0) {
                rigaTrovata = riga;
            }
            riga++;
        }

        return rigaTrovata;
    }
}
