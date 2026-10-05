import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;

/** Entry point of the graphical version. */
public class Main {
    public static void main(String[] args) {
        // On some networks the IPv6 route hangs before falling back to IPv4,
        // making API calls look stuck: force IPv4.
        System.setProperty("java.net.preferIPv4Stack", "true");

        impostaTemaCarta();
        ToolTipManager.sharedInstance().setInitialDelay(300);

        // Swing components must be created on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            SchermataIniziale schermata = new SchermataIniziale();
            schermata.setVisible(true);
        });
    }

    /** Applies the hand-drawn "paper" look to the standard Swing components (dialogs, tooltips, lists). */
    private static void impostaTemaCarta() {
        ColorUIResource carta = new ColorUIResource(Matita.CARTA);
        ColorUIResource penna = new ColorUIResource(Matita.PENNA);
        FontUIResource fontTesto = new FontUIResource(Matita.fontAMano(Font.PLAIN, 16f));
        FontUIResource fontPulsanti = new FontUIResource(Matita.fontAMano(Font.BOLD, 15f));

        UIManager.put("Panel.background", carta);
        UIManager.put("OptionPane.background", carta);
        UIManager.put("OptionPane.messageForeground", penna);
        UIManager.put("OptionPane.messageFont", fontTesto);
        UIManager.put("OptionPane.buttonFont", fontPulsanti);
        UIManager.put("Label.foreground", penna);
        UIManager.put("Button.font", fontPulsanti);

        UIManager.put("ToolTip.background", new ColorUIResource(Matita.POST_IT));
        UIManager.put("ToolTip.foreground", penna);
        UIManager.put("ToolTip.font", new FontUIResource(Matita.fontAMano(Font.PLAIN, 15f)));
        UIManager.put("ToolTip.border", new BorderUIResource(new Matita.BordoAMano(90)));

        UIManager.put("PopupMenu.background", carta);
        UIManager.put("PopupMenu.border", new BorderUIResource(BorderFactory.createEmptyBorder()));
        UIManager.put("List.background", carta);
        UIManager.put("List.foreground", penna);
    }
}
