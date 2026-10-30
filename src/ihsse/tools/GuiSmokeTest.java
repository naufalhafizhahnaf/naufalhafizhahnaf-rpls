package ihsse.tools;

import ihsse.dao.AdminDao;
import ihsse.gui.InduksiPanel;
import ihsse.gui.InformasiPanel;
import ihsse.gui.InsidenPanel;
import ihsse.gui.InspeksiPanel;
import ihsse.gui.LoginFrame;
import ihsse.gui.MainFrame;
import ihsse.gui.PengunjungPanel;
import ihsse.gui.Theme;
import ihsse.model.Admin;
import ihsse.service.InduksiService;
import ihsse.service.InformasiService;
import ihsse.service.InsidenService;
import ihsse.service.InspeksiService;
import ihsse.service.PengunjungService;

import javax.swing.SwingUtilities;

/**
 * Menguji inisialisasi seluruh panel GUI dan pembacaan data dari database.
 */
public final class GuiSmokeTest {

    private GuiSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Theme.install();
        Admin admin = new AdminDao().login("admin", "admin123");
        if (admin == null) {
            throw new IllegalStateException("Login admin gagal.");
        }

        SwingUtilities.invokeAndWait(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.dispose();

            MainFrame mainFrame = new MainFrame(admin);
            mainFrame.dispose();

            InformasiPanel informasi = new InformasiPanel(new InformasiService(), () -> {
            });
            informasi.refreshData();

            PengunjungPanel pengunjung = new PengunjungPanel(
                    new PengunjungService(), admin, () -> {
            });
            pengunjung.refreshData();

            InduksiPanel induksi = new InduksiPanel(
                    new PengunjungService(), new InduksiService(), admin, () -> {
            });
            induksi.refreshData();

            InspeksiPanel inspeksi = new InspeksiPanel(
                    new InformasiService(), new InspeksiService(), admin, () -> {
            });
            inspeksi.refreshData();

            InsidenPanel insiden = new InsidenPanel(
                    new InsidenService(), admin, () -> {
            });
            insiden.refreshData();
        });

        System.out.println("GUI SMOKE TEST LULUS.");
    }
}
