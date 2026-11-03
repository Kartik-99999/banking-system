import com.bank.ui.LoginFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Application entry point: opens the login window. */
public class Main {
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Default look and feel is fine.
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
