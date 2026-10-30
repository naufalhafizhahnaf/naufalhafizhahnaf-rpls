package ihsse.gui;

import ihsse.model.Admin;
import ihsse.model.LaporanInsiden;
import ihsse.model.StatusInsiden;
import ihsse.service.InsidenService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel pelaporan insiden dan proses tindak lanjut oleh admin.
 */
public class InsidenPanel extends JPanel implements Refreshable {

    private final InsidenService service;
    private final Admin admin;
    private final Runnable onChange;

    private final JTextField namaPelaporField = Theme.textField();
    private final JTextField kontakField = Theme.textField();
    private final JTextField lokasiField = Theme.textField();
    private final JTextField waktuField = Theme.textField();
    private final JTextArea deskripsiArea = Theme.textArea(4);

    private final JComboBox<String> filterCombo = Theme.comboBox(
            "Semua", "Baru", "Diproses", "Selesai");
    private final DefaultTableModel insidenModel;
    private final JTable insidenTable;
    private final List<LaporanInsiden> daftarInsiden = new ArrayList<>();
    private final JTabbedPane tabs = new JTabbedPane();

    public InsidenPanel(InsidenService service, Admin admin, Runnable onChange) {
        this.service = service;
        this.admin = admin;
        this.onChange = onChange;

        setLayout(new BorderLayout());
        setOpaque(false);

        insidenModel = new DefaultTableModel(
                new Object[]{"Nomor", "Tgl Terbit", "Pelapor", "Lokasi",
                        "Waktu Kejadian", "Status", "Ditangani Oleh", "Catatan"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        insidenTable = Theme.table();
        insidenTable.setModel(insidenModel);
        insidenTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(insidenTable, 170, 90, 150, 190, 125, 90, 150, 230);
        insidenTable.getColumnModel().getColumn(5)
                .setCellRenderer(Theme.statusRenderer());

        tabs.setFont(Theme.FONT);
        tabs.addTab("Laporkan Insiden", buatTabLapor());
        tabs.addTab("Daftar & Proses", buatTabDaftar());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buatTabLapor() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        header.add(Theme.bold("Form Laporan Insiden"));
        JLabel ket = Theme.muted("Laporan akan masuk dengan status Baru dan diverifikasi admin.");
        ket.setFont(Theme.FONT_SMALL);
        header.add(ket);
        card.add(header, BorderLayout.NORTH);

        waktuField.setText(FormatUtil.waktuInput(
                LocalDateTime.now().withSecond(0).withNano(0)));

        JPanel form = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(form, gbc, 0, "Nama Pelapor", namaPelaporField);
        Theme.addFormRow(form, gbc, 1, "Kontak", kontakField);
        Theme.addFormRow(form, gbc, 2, "Lokasi", lokasiField);
        Theme.addFormRow(form, gbc, 3, "Waktu Kejadian", waktuField);
        JScrollPane deskripsiScroll = Theme.scroll(deskripsiArea);
        deskripsiScroll.setPreferredSize(new Dimension(0, 95));
        Theme.addFormRow(form, gbc, 4, "Deskripsi", deskripsiScroll);

        JPanel aksi = Theme.rightToolbar();
        JButton bersih = Theme.secondaryButton("Bersihkan");
        JButton kirim = Theme.primaryButton("Kirim Laporan");
        bersih.addActionListener(e -> bersihkanForm());
        kirim.addActionListener(e -> kirimLaporan());
        aksi.add(bersih);
        aksi.add(kirim);
        Theme.addFormRow(form, gbc, 5, "", aksi);

        card.add(form, BorderLayout.CENTER);
        panel.add(card, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buatTabDaftar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel kiri = Theme.toolbar();
        kiri.add(Theme.bold("Filter Status"));
        kiri.add(filterCombo);
        header.add(kiri, BorderLayout.WEST);

        JPanel tombol = Theme.rightToolbar();
        JButton proses = Theme.primaryButton("Proses");
        JButton lihat = Theme.secondaryButton("Lihat / Cetak");
        JButton muat = Theme.secondaryButton("Muat Ulang");
        proses.addActionListener(e -> proses());
        lihat.addActionListener(e -> lihat());
        muat.addActionListener(e -> refreshData());
        tombol.add(proses);
        tombol.add(lihat);
        tombol.add(muat);
        header.add(tombol, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        filterCombo.addActionListener(e -> loadInsiden());
        JScrollPane scroll = Theme.scroll(insidenTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.add(scroll, BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void refreshData() {
        loadInsiden();
    }

    private void loadInsiden() {
        String filter = String.valueOf(filterCombo.getSelectedItem());
        List<LaporanInsiden> data;
        if ("Semua".equals(filter)) {
            data = service.semuaInsiden();
        } else {
            data = service.berdasarkanStatus(statusDariLabel(filter));
        }

        daftarInsiden.clear();
        insidenModel.setRowCount(0);
        for (LaporanInsiden laporan : data) {
            daftarInsiden.add(laporan);
            insidenModel.addRow(new Object[]{
                    laporan.getNomorDokumen(),
                    FormatUtil.tanggal(laporan.getTanggalTerbit()),
                    laporan.getNamaPelapor(),
                    laporan.getLokasi(),
                    FormatUtil.waktu(laporan.getWaktuKejadian()),
                    laporan.getStatus().getLabel(),
                    laporan.getDitanganiOleh() == null ? "-"
                            : laporan.getDitanganiOleh().getNama(),
                    laporan.getCatatanAdmin()
            });
        }
    }

    private void kirimLaporan() {
        try {
            String nama = required(namaPelaporField, "Nama pelapor");
            String kontak = required(kontakField, "Kontak");
            String lokasi = required(lokasiField, "Lokasi");
            String deskripsi = required(deskripsiArea, "Deskripsi");
            LocalDateTime waktu = FormatUtil.parseWaktu(waktuField.getText());

            LaporanInsiden laporan = service.laporInsiden(
                    nama, kontak, lokasi, deskripsi, waktu);
            bersihkanForm();
            loadInsiden();
            onChange.run();
            Theme.showInfo(this, "Laporan insiden berhasil dikirim.\n\nNomor: "
                    + laporan.getNomorDokumen() + "\nStatus: "
                    + laporan.getStatus().getLabel());
        } catch (IllegalArgumentException ex) {
            Theme.showError(this, ex.getMessage());
        } catch (RuntimeException ex) {
            Theme.showError(this, "Gagal mengirim laporan: " + ex.getMessage());
        }
    }

    private void bersihkanForm() {
        namaPelaporField.setText("");
        kontakField.setText("");
        lokasiField.setText("");
        waktuField.setText(FormatUtil.waktuInput(
                LocalDateTime.now().withSecond(0).withNano(0)));
        deskripsiArea.setText("");
        namaPelaporField.requestFocusInWindow();
    }

    private void proses() {
        int row = insidenTable.getSelectedRow();
        if (row < 0 || row >= daftarInsiden.size()) {
            Theme.showInfo(this, "Pilih laporan insiden pada tabel terlebih dahulu.");
            return;
        }
        LaporanInsiden laporan = daftarInsiden.get(row);
        if (laporan.getStatus() == StatusInsiden.SELESAI) {
            Theme.showInfo(this, "Laporan " + laporan.getNomorDokumen()
                    + " sudah SELESAI dan tidak bisa diproses lagi.");
            return;
        }

        List<StatusInsiden> berikutnya = new ArrayList<>();
        for (StatusInsiden status : StatusInsiden.values()) {
            if (laporan.getStatus().bolehBerubahKe(status)) {
                berikutnya.add(status);
            }
        }
        if (berikutnya.isEmpty()) {
            Theme.showInfo(this, "Tidak ada status berikutnya untuk laporan ini.");
            return;
        }

        JComboBox<String> statusCombo = Theme.comboBox();
        for (StatusInsiden status : berikutnya) {
            statusCombo.addItem(status.getLabel());
        }

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JLabel info = Theme.bold("Status saat ini: " + laporan.getStatus().getLabel());
        panel.add(info, BorderLayout.NORTH);

        JPanel form = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(form, gbc, 0, "Status Berikutnya", statusCombo);
        JTextArea catatan = Theme.textArea(3);
        catatan.setText("-".equals(laporan.getCatatanAdmin())
                ? "" : laporan.getCatatanAdmin());
        JScrollPane catatanScroll = Theme.scroll(catatan);
        catatanScroll.setPreferredSize(new Dimension(0, 70));
        Theme.addFormRow(form, gbc, 1, "Catatan Admin", catatanScroll);
        panel.add(form, BorderLayout.CENTER);

        int pilihan = JOptionPane.showConfirmDialog(this, panel,
                "Proses Insiden - " + laporan.getNomorDokumen(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (pilihan != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            StatusInsiden statusBaru = berikutnya.get(statusCombo.getSelectedIndex());
            LaporanInsiden hasil = service.prosesInsiden(
                    laporan.getNomorDokumen(), statusBaru, catatan.getText(), admin);
            loadInsiden();
            onChange.run();
            DokumenPreviewDialog.show(this,
                    "Laporan Insiden - " + hasil.getNomorDokumen(), hasil.cetak());
        } catch (RuntimeException ex) {
            Theme.showError(this, "Gagal memproses insiden: " + ex.getMessage());
        }
    }

    private void lihat() {
        int row = insidenTable.getSelectedRow();
        if (row < 0 || row >= daftarInsiden.size()) {
            Theme.showInfo(this, "Pilih laporan insiden pada tabel terlebih dahulu.");
            return;
        }
        LaporanInsiden laporan = daftarInsiden.get(row);
        DokumenPreviewDialog.show(this,
                "Laporan Insiden - " + laporan.getNomorDokumen(), laporan.cetak());
    }

    private StatusInsiden statusDariLabel(String label) {
        for (StatusInsiden status : StatusInsiden.values()) {
            if (status.getLabel().equalsIgnoreCase(label)) {
                return status;
            }
        }
        return StatusInsiden.BARU;
    }

    private String required(JTextField field, String label) {
        if (field.getText() == null || field.getText().trim().isEmpty()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        return field.getText().trim();
    }

    private String required(JTextArea area, String label) {
        if (area.getText() == null || area.getText().trim().isEmpty()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        return area.getText().trim();
    }
}
