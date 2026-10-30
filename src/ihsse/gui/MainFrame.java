package ihsse.gui;

import ihsse.model.Admin;
import ihsse.service.InduksiService;
import ihsse.service.InformasiService;
import ihsse.service.InsidenService;
import ihsse.service.InspeksiService;
import ihsse.service.PengunjungService;
import ihsse.service.StatistikService;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Jendela utama aplikasi GUI setelah login.
 */
public class MainFrame extends JFrame {

    private final Admin admin;
    private final InformasiService informasiService = new InformasiService();
    private final PengunjungService pengunjungService = new PengunjungService();
    private final InduksiService induksiService = new InduksiService();
    private final InspeksiService inspeksiService = new InspeksiService();
    private final InsidenService insidenService = new InsidenService();
    private final StatistikService statistikService = new StatistikService();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JLabel pageTitleLabel = new JLabel();
    private final JLabel pageSubtitleLabel = new JLabel();
    private final JPanel navPanel = new JPanel();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, Refreshable> refreshables = new LinkedHashMap<>();
    private final Map<String, String> pageTitles = new LinkedHashMap<>();
    private final Map<String, String> pageSubtitles = new LinkedHashMap<>();
    private String currentKey = "dashboard";

    public MainFrame(Admin admin) {
        this.admin = admin;
        setTitle("I-HSSE Safety Induction - " + admin.getNama());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension layar = Toolkit.getDefaultToolkit().getScreenSize();
        int lebar = Math.min(1280, Math.max(760, layar.width - 80));
        int tinggi = Math.min(800, Math.max(560, layar.height - 80));
        setSize(lebar, tinggi);
        setMinimumSize(new Dimension(Math.min(1100, lebar), Math.min(700, tinggi)));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        setContentPane(root);

        root.add(buatSidebar(), BorderLayout.WEST);
        root.add(buatKonten(), BorderLayout.CENTER);

        daftarkanHalaman();
        showPage("dashboard");
    }

    private JPanel buatSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(238, 0));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(26, 24, 24, 24));

        JLabel logo = new JLabel("I-HSSE");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("Safety Induction");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tagline.setForeground(Theme.SIDEBAR_TEXT);
        tagline.setAlignmentX(LEFT_ALIGNMENT);

        JLabel kampus = new JLabel("Universitas Gunadarma J6");
        kampus.setFont(Theme.FONT_SMALL);
        kampus.setForeground(new Color(145, 170, 190));
        kampus.setAlignmentX(LEFT_ALIGNMENT);

        brand.add(logo);
        brand.add(Box.createVerticalStrut(2));
        brand.add(tagline);
        brand.add(Box.createVerticalStrut(8));
        brand.add(kampus);
        sidebar.add(brand, BorderLayout.NORTH);

        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBorder(new EmptyBorder(6, 12, 6, 12));
        sidebar.add(navPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(16, 18, 20, 18));

        JPanel adminBox = new JPanel(new BorderLayout(10, 0));
        adminBox.setOpaque(false);

        JLabel avatar = new JLabel(initial(admin.getNama()), SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(new Color(35, 78, 116));
        avatar.setForeground(Color.WHITE);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        avatar.setPreferredSize(new Dimension(38, 38));
        avatar.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel adminInfo = new JPanel();
        adminInfo.setOpaque(false);
        adminInfo.setLayout(new BoxLayout(adminInfo, BoxLayout.Y_AXIS));
        JLabel nama = new JLabel(admin.getNama());
        nama.setFont(Theme.FONT_BOLD);
        nama.setForeground(Color.WHITE);
        JLabel jabatan = new JLabel(admin.getJabatan());
        jabatan.setFont(Theme.FONT_SMALL);
        jabatan.setForeground(new Color(160, 184, 202));
        adminInfo.add(nama);
        adminInfo.add(Box.createVerticalStrut(2));
        adminInfo.add(jabatan);

        adminBox.add(avatar, BorderLayout.WEST);
        adminBox.add(adminInfo, BorderLayout.CENTER);

        JButton logout = Theme.navButton("Keluar");
        logout.setForeground(new Color(255, 185, 185));
        logout.setBorder(new EmptyBorder(8, 8, 8, 8));
        logout.addActionListener(e -> logout());

        footer.add(adminBox, BorderLayout.CENTER);
        footer.add(logout, BorderLayout.SOUTH);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buatKonten() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BACKGROUND);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(22, 28, 8, 28));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        pageTitleLabel.setFont(Theme.TITLE);
        pageTitleLabel.setForeground(Theme.TEXT);
        pageSubtitleLabel.setFont(Theme.SUBTITLE);
        pageSubtitleLabel.setForeground(Theme.MUTED);
        titles.add(pageTitleLabel);
        titles.add(Box.createVerticalStrut(3));
        titles.add(pageSubtitleLabel);

        JLabel tanggal = Theme.muted(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy",
                        java.util.Locale.forLanguageTag("id-ID"))));

        JPanel kanan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        kanan.setOpaque(false);
        kanan.add(tanggal);

        header.add(titles, BorderLayout.WEST);
        header.add(kanan, BorderLayout.EAST);

        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(8, 28, 24, 28));

        main.add(header, BorderLayout.NORTH);
        main.add(contentPanel, BorderLayout.CENTER);
        return main;
    }

    private void daftarkanHalaman() {
        DashboardPanel dashboard = new DashboardPanel(
                statistikService, insidenService, informasiService);
        tambahHalaman("dashboard", "Dashboard", "Dashboard",
                "Ringkasan aktivitas HSSE kampus J6.", dashboard);

        InformasiPanel informasi = new InformasiPanel(informasiService, this::dataChanged);
        tambahHalaman("informasi", "Informasi HSSE", "Informasi HSSE",
                "Kelola konten Health, Security, Safety, dan Environment.", informasi);

        PengunjungPanel pengunjung = new PengunjungPanel(
                pengunjungService, admin, this::dataChanged);
        tambahHalaman("pengunjung", "Pengunjung & Pass", "Pengunjung & Visitor Pass",
                "Registrasi pengunjung dan penerbitan visitor pass.", pengunjung);

        InduksiPanel induksi = new InduksiPanel(
                pengunjungService, induksiService, admin, this::dataChanged);
        tambahHalaman("induksi", "Induksi & Sertifikat", "Induksi & Sertifikat",
                "Selesaikan safety induction dan terbitkan sertifikat.", induksi);

        InspeksiPanel inspeksi = new InspeksiPanel(
                informasiService, inspeksiService, admin, this::dataChanged);
        tambahHalaman("inspeksi", "Inspeksi Fasilitas", "Inspeksi Fasilitas",
                "Catat pemeriksaan APAR, hydrant, dan fasilitas keselamatan.", inspeksi);

        InsidenPanel insiden = new InsidenPanel(
                insidenService, admin, this::dataChanged);
        tambahHalaman("insiden", "Laporan Insiden", "Laporan Insiden",
                "Terima laporan publik dan proses tindak lanjutnya.", insiden);
    }

    private void tambahHalaman(String key, String navLabel, String pageTitle,
                               String subtitle, JPanel panel) {
        contentPanel.add(panel, key);
        pageTitles.put(key, pageTitle);
        pageSubtitles.put(key, subtitle);
        if (panel instanceof Refreshable) {
            refreshables.put(key, (Refreshable) panel);
        }

        JButton nav = Theme.navButton(navLabel);
        nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        nav.setAlignmentX(LEFT_ALIGNMENT);
        nav.addActionListener(e -> showPage(key));
        navButtons.put(key, nav);
        navPanel.add(nav);
        navPanel.add(Box.createVerticalStrut(4));
    }

    private void showPage(String key) {
        currentKey = key;
        cardLayout.show(contentPanel, key);
        pageTitleLabel.setText(pageTitles.getOrDefault(key, ""));
        pageSubtitleLabel.setText(pageSubtitles.getOrDefault(key, ""));

        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean aktif = entry.getKey().equals(key);
            JButton button = entry.getValue();
            button.setBackground(aktif ? Theme.SIDEBAR_ACTIVE : Theme.SIDEBAR);
            button.setForeground(aktif ? Color.WHITE : Theme.SIDEBAR_TEXT);
            button.setFont(aktif ? Theme.FONT_BOLD
                    : new Font("Segoe UI", Font.PLAIN, 14));
        }

        Refreshable refreshable = refreshables.get(key);
        if (refreshable != null) {
            try {
                refreshable.refreshData();
            } catch (RuntimeException ex) {
                Theme.showError(this, "Gagal memuat halaman " + key + ".\n\n"
                        + ex.getMessage());
            }
        }
    }

    private void dataChanged() {
        Refreshable current = refreshables.get(currentKey);
        if (current != null) {
            try {
                current.refreshData();
            } catch (RuntimeException ex) {
                Theme.showError(this, ex.getMessage());
            }
        }
        if (!"dashboard".equals(currentKey)) {
            Refreshable dashboard = refreshables.get("dashboard");
            if (dashboard != null) {
                dashboard.refreshData();
            }
        }
    }

    private void logout() {
        if (!Theme.confirm(this, "Keluar dari aplikasi?")) {
            return;
        }
        dispose();
        new LoginFrame().setVisible(true);
    }

    private String initial(String nama) {
        if (nama == null || nama.trim().isEmpty()) {
            return "A";
        }
        String[] bagian = nama.trim().split("\\s+");
        if (bagian.length == 1) {
            return bagian[0].substring(0, 1).toUpperCase();
        }
        return (bagian[0].substring(0, 1) + bagian[1].substring(0, 1)).toUpperCase();
    }

    // Referensi service agar mudah dikembangkan tanpa mengubah kontrak MainFrame.
    public InformasiService getInformasiService() {
        return informasiService;
    }
}
