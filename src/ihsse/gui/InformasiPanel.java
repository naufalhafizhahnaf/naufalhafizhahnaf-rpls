package ihsse.gui;

import ihsse.model.DenahGedung;
import ihsse.model.InformasiHSSE;
import ihsse.model.VideoTutorial;
import ihsse.service.InformasiService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel pengelolaan informasi HSSE, video tutorial, dan denah gedung.
 */
public class InformasiPanel extends JPanel implements Refreshable {

    private final InformasiService service;
    private final Runnable onChange;

    private final JTextField searchField = Theme.textField();
    private final JComboBox<String> kategoriCombo = Theme.comboBox(
            "Semua", "Health", "Security", "Safety", "Environment");

    private final DefaultTableModel infoModel;
    private final DefaultTableModel videoModel;
    private final DefaultTableModel denahModel;
    private final JTable infoTable;
    private final List<InformasiHSSE> data = new ArrayList<>();

    public InformasiPanel(InformasiService service, Runnable onChange) {
        this.service = service;
        this.onChange = onChange;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel kiri = Theme.toolbar();
        kiri.add(Theme.bold("Cari"));
        searchField.setPreferredSize(new Dimension(210, 34));
        kiri.add(searchField);
        kiri.add(Theme.bold("Kategori"));
        kategoriCombo.setPreferredSize(new Dimension(135, 34));
        kiri.add(kategoriCombo);

        JPanel kanan = Theme.rightToolbar();
        JButton tambah = Theme.primaryButton("+ Tambah");
        JButton ubah = Theme.secondaryButton("Ubah");
        JButton hapus = Theme.dangerButton("Hapus");
        JButton muat = Theme.secondaryButton("Muat Ulang");
        kanan.add(tambah);
        kanan.add(ubah);
        kanan.add(hapus);
        kanan.add(muat);

        toolbar.add(kiri, BorderLayout.WEST);
        toolbar.add(kanan, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        infoModel = new DefaultTableModel(
                new Object[]{"Kode", "Kategori", "Jenis", "Nama", "Lokasi", "Deskripsi"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        infoTable = Theme.table();
        infoTable.setModel(infoModel);
        infoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(infoTable, 70, 90, 135, 230, 220, 330);
        infoTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ubah();
                }
            }
        });

        JPanel tabInfo = new JPanel(new BorderLayout());
        tabInfo.setOpaque(false);
        tabInfo.add(Theme.scroll(infoTable), BorderLayout.CENTER);

        videoModel = new DefaultTableModel(
                new Object[]{"ID", "Topik", "Judul", "Durasi", "URL"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable videoTable = Theme.table();
        videoTable.setModel(videoModel);
        videoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(videoTable, 70, 100, 260, 130, 300);
        JPanel tabVideo = new JPanel(new BorderLayout());
        tabVideo.setOpaque(false);
        tabVideo.add(Theme.scroll(videoTable), BorderLayout.CENTER);

        denahModel = new DefaultTableModel(
                new Object[]{"Lantai", "Nama File", "Deskripsi", "Fasilitas Penting"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable denahTable = Theme.table();
        denahTable.setModel(denahModel);
        denahTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(denahTable, 70, 170, 380, 360);
        JPanel tabDenah = new JPanel(new BorderLayout());
        tabDenah.setOpaque(false);
        tabDenah.add(Theme.scroll(denahTable), BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT);
        tabs.addTab("Data HSSE", tabInfo);
        tabs.addTab("Video Tutorial", tabVideo);
        tabs.addTab("Denah Gedung", tabDenah);
        add(tabs, BorderLayout.CENTER);

        tambah.addActionListener(e -> tambah());
        ubah.addActionListener(e -> ubah());
        hapus.addActionListener(e -> hapus());
        muat.addActionListener(e -> refreshData());
        kategoriCombo.addActionListener(e -> loadInfo());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                loadInfo();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                loadInfo();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                loadInfo();
            }
        });
    }

    @Override
    public void refreshData() {
        loadInfo();
        loadVideo();
        loadDenah();
    }

    private void loadInfo() {
        String kategori = String.valueOf(kategoriCombo.getSelectedItem());
        String cari = searchField.getText().trim().toLowerCase();

        data.clear();
        infoModel.setRowCount(0);
        for (InformasiHSSE informasi : service.semuaInformasi()) {
            if (!"Semua".equals(kategori)
                    && !informasi.getKategori().equalsIgnoreCase(kategori)) {
                continue;
            }
            String gabungan = (informasi.getKode() + " " + informasi.getNama() + " "
                    + informasi.getLokasi() + " " + informasi.getDeskripsi() + " "
                    + informasi.getJenis()).toLowerCase();
            if (!cari.isEmpty() && !gabungan.contains(cari)) {
                continue;
            }
            data.add(informasi);
            infoModel.addRow(new Object[]{
                    informasi.getKode(),
                    informasi.getKategori(),
                    informasi.getJenis(),
                    informasi.getNama(),
                    informasi.getLokasi(),
                    informasi.getDeskripsi()
            });
        }
    }

    private void loadVideo() {
        videoModel.setRowCount(0);
        for (VideoTutorial video : service.semuaVideo()) {
            videoModel.addRow(new Object[]{
                    video.getId(),
                    video.getTopik(),
                    video.getJudul(),
                    video.getDurasiFormat(),
                    video.getUrl()
            });
        }
    }

    private void loadDenah() {
        denahModel.setRowCount(0);
        for (DenahGedung denah : service.semuaDenah()) {
            denahModel.addRow(new Object[]{
                    "Lantai " + denah.getLantai(),
                    denah.getNamaFile(),
                    denah.getDeskripsi(),
                    String.join(", ", denah.getFasilitasPenting())
            });
        }
    }

    private InformasiHSSE terpilih() {
        int row = infoTable.getSelectedRow();
        if (row < 0 || row >= data.size()) {
            Theme.showInfo(this, "Pilih salah satu data informasi terlebih dahulu.");
            return null;
        }
        return data.get(row);
    }

    private void tambah() {
        InformasiHSSE informasi = InformasiDialog.show(this, null);
        if (informasi == null) {
            return;
        }
        try {
            service.tambahInformasi(informasi);
            onChange.run();
            refreshData();
            Theme.showInfo(this, "Informasi " + informasi.getKode() + " berhasil disimpan.");
        } catch (RuntimeException ex) {
            Theme.showError(this, ex.getMessage());
        }
    }

    private void ubah() {
        InformasiHSSE lama = terpilih();
        if (lama == null) {
            return;
        }
        InformasiHSSE baru = InformasiDialog.show(this, lama);
        if (baru == null) {
            return;
        }
        try {
            service.ubahInformasi(baru);
            onChange.run();
            refreshData();
            Theme.showInfo(this, "Informasi " + baru.getKode() + " berhasil diubah.");
        } catch (RuntimeException ex) {
            Theme.showError(this, ex.getMessage());
        }
    }

    private void hapus() {
        InformasiHSSE informasi = terpilih();
        if (informasi == null) {
            return;
        }
        if (!Theme.confirm(this, "Hapus informasi " + informasi.getKode()
                + " - " + informasi.getNama() + "?")) {
            return;
        }
        try {
            service.hapusInformasi(informasi.getKode());
            onChange.run();
            refreshData();
            Theme.showInfo(this, "Informasi " + informasi.getKode() + " berhasil dihapus.");
        } catch (RuntimeException ex) {
            Theme.showError(this, ex.getMessage());
        }
    }
}
