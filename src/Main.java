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

        applyPaperTheme();
        ToolTipManager.sharedInstance().setInitialDelay(300);

        // Swing components must be created on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            StartScreen startScreen = new StartScreen();
            startScreen.setVisible(true);
        });
    }

    /** Applies the hand-drawn "paper" look to the standard Swing components (dialogs, tooltips, lists). */
    private static void applyPaperTheme() {
        ColorUIResource paper = new ColorUIResource(Sketch.PAPER);
        ColorUIResource ink = new ColorUIResource(Sketch.INK);
        FontUIResource textFont = new FontUIResource(Sketch.handFont(Font.PLAIN, 16f));
        FontUIResource buttonFont = new FontUIResource(Sketch.handFont(Font.BOLD, 15f));

        UIManager.put("Panel.background", paper);
        UIManager.put("OptionPane.background", paper);
        UIManager.put("OptionPane.messageForeground", ink);
        UIManager.put("OptionPane.messageFont", textFont);
        UIManager.put("OptionPane.buttonFont", buttonFont);
        UIManager.put("Label.foreground", ink);
        UIManager.put("Button.font", buttonFont);

        UIManager.put("ToolTip.background", new ColorUIResource(Sketch.STICKY_NOTE));
        UIManager.put("ToolTip.foreground", ink);
        UIManager.put("ToolTip.font", new FontUIResource(Sketch.handFont(Font.PLAIN, 15f)));
        UIManager.put("ToolTip.border", new BorderUIResource(new Sketch.HandBorder(90)));

        UIManager.put("PopupMenu.background", paper);
        UIManager.put("PopupMenu.border", new BorderUIResource(BorderFactory.createEmptyBorder()));
        UIManager.put("List.background", paper);
        UIManager.put("List.foreground", ink);
    }
}
