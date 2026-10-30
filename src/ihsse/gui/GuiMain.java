package ihsse.gui;

import ihsse.db.Database;

import javax.swing.SwingUtilities;

/**
 * Entry point aplikasi GUI I-HSSE.
 */
public final class GuiMain {

    private GuiMain() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            try {
                Database.testConnection();
            } catch (Exception | LinkageError ex) {
                Theme.showError(null,
                        "Database belum bisa dihubungi.\n\n"
                        + "1. Pastikan MySQL XAMPP sudah Start.\n"
                        + "2. Jalankan setup_database.bat satu kali.\n"
                        + "3. Periksa db.properties (user/password/port).\n\n"
                        + "Detail: " + ex.getMessage());
            }
            new LoginFrame().setVisible(true);
        });
    }
}
