import java.util.Scanner;

/** Human player for the console version: reads the column from standard input. */
public class GiocatoreUmano implements Giocatore {

    private int id;
    private Scanner scanner;

    public GiocatoreUmano(int id) {
        this.id = id;
        this.scanner = new Scanner(System.in);
    }

    public int sceglieMossa(Griglia griglia) {
        int colonna = -1;
        boolean valida = false;

        while (!valida) {
            System.out.print("Giocatore " + id + ", scegli una colonna (0-" + (griglia.getColonne() - 1) + "): ");
            colonna = scanner.nextInt();
            valida = griglia.colonnaValida(colonna);

            if (!valida) {
                System.out.println("Colonna non valida, riprova.");
            }
        }

        return colonna;
    }

    public int getId() {
        return id;
    }
}
