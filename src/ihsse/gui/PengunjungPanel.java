package ihsse.gui;

import ihsse.model.Admin;
import ihsse.model.Pengunjung;
import ihsse.model.VisitorPass;
import ihsse.service.PengunjungService;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel registrasi pengunjung dan visitor pass.
 */
public class PengunjungPanel extends JPanel implements Refreshable {

    private final PengunjungService service;
    private final Admin admin;
    private final Runnable onChange;

    private final JTextField namaField = Theme.textField();
    private final JTextField identitasField = Theme.textField();
    private final JTextField teleponField = Theme.textField();
    private final JTextField instansiField = Theme.textField();
    private final JTextField tujuanField = Theme.textField();

    private final DefaultTableModel pengunjungModel;
    private final DefaultTableModel passModel;
    private final JTable pengunjungTable;
    private final JTable passTable;
    private final JTabbedPane tabs = new JTabbedPane();
    private final List<Pengunjung> daftarPengunjung = new ArrayList<>();
    private final List<VisitorPass> daftarPass = new ArrayList<>();

    public PengunjungPanel(PengunjungService service, Admin admin, Runnable onChange) {
        this.service = service;
        this.admin = admin;
        this.onChange = onChange;

        setLayout(new BorderLayout());
        setOpaque(false);

        pengunjungModel = new DefaultTableModel(
                new Object[]{"ID", "Nama", "No. Identitas", "Telepon", "Instansi",
                        "Tujuan Kunjungan", "Tgl Daftar", "Induksi"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pengunjungTable = Theme.table();
        pengunjungTable.setModel(pengunjungModel);
        pengunjungTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(pengunjungTable, 75, 170, 130, 120, 160, 230, 100, 80);

        passModel = new DefaultTableModel(
                new Object[]{"Nomor", "Tgl Terbit", "Pengunjung", "Tgl Kunjungan",
                        "Jam", "Berlaku Sampai", "Area Tujuan", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        passTable = Theme.table();
        passTable.setModel(passModel);
        passTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(passTable, 165, 90, 170, 110, 65, 105, 220, 95);
        passTable.getColumnModel().getColumn(7).setCellRenderer(Theme.statusRenderer());

        tabs.setFont(Theme.FONT);
        tabs.addTab("Registrasi Pengunjung", buatTabRegistrasi());
        tabs.addTab("Visitor Pass", buatTabPass());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buatTabRegistrasi() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));
        card.add(Theme.bold("Form Registrasi Pengunjung"), BorderLayout.NORTH);

        JPanel form = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(form, gbc, 0, "Nama Lengkap", namaField);
        Theme.addFormRow(form, gbc, 1, "No. Identitas", identitasField);
        Theme.addFormRow(form, gbc, 2, "No. Telepon", teleponField);
        Theme.addFormRow(form, gbc, 3, "Instansi", instansiField);
        Theme.addFormRow(form, gbc, 4, "Tujuan Kunjungan", tujuanField);

        JPanel aksi = Theme.rightToolbar();
        JButton reset = Theme.secondaryButton("Reset");
        JButton simpan = Theme.primaryButton("Daftarkan Pengunjung");
        reset.addActionListener(e -> bersihkanForm());
        simpan.addActionListener(e -> registrasi());
        aksi.add(reset);
        aksi.add(simpan);
        Theme.addFormRow(form, gbc, 5, "", aksi);

        card.add(form, BorderLayout.CENTER);
        panel.add(card, BorderLayout.NORTH);

        JPanel tabelCard = Theme.card();
        tabelCard.setLayout(new BorderLayout(0, 10));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.bold("Daftar Pengunjung"), BorderLayout.WEST);
        JPanel tombol = Theme.rightToolbar();
        JButton pass = Theme.primaryButton("Terbitkan Pass");
        JButton muat = Theme.secondaryButton("Muat Ulang");
        pass.addActionListener(e -> {
            Pengunjung pengunjung = terpilih();
            if (pengunjung != null) {
                terbitkanPass(pengunjung);
            }
        });
        muat.addActionListener(e -> refreshData());
        tombol.add(pass);
        tombol.add(muat);
        header.add(tombol, BorderLayout.EAST);
        tabelCard.add(header, BorderLayout.NORTH);

        JScrollPane scroll = Theme.scroll(pengunjungTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        tabelCard.add(scroll, BorderLayout.CENTER);
        panel.add(tabelCard, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buatTabPass() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel judul = new JPanel();
        judul.setOpaque(false);
        judul.setLayout(new javax.swing.BoxLayout(judul, javax.swing.BoxLayout.Y_AXIS));
        judul.add(Theme.bold("Daftar Visitor Pass"));
        JLabelKeterangan.tambah(judul, "Pass berlaku sesuai masa berlaku yang dipilih.");
        header.add(judul, BorderLayout.WEST);

        JPanel tombol = Theme.rightToolbar();
        JButton terbit = Theme.primaryButton("Terbitkan Pass");
        JButton lihat = Theme.secondaryButton("Lihat / Cetak");
        JButton muat = Theme.secondaryButton("Muat Ulang");
        terbit.addActionListener(e -> terbitkanPass(null));
        lihat.addActionListener(e -> lihatPass());
        muat.addActionListener(e -> refreshData());
        tombol.add(terbit);
        tombol.add(lihat);
        tombol.add(muat);
        header.add(tombol, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JScrollPane scroll = Theme.scroll(passTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.add(scroll, BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void refreshData() {
        daftarPengunjung.clear();
        pengunjungModel.setRowCount(0);
        for (Pengunjung pengunjung : service.semuaPengunjung()) {
            daftarPengunjung.add(pengunjung);
            pengunjungModel.addRow(new Object[]{
                    pengunjung.getId(),
                    pengunjung.getNama(),
                    pengunjung.getNomorIdentitas(),
                    pengunjung.getNoTelepon(),
                    pengunjung.getInstansi(),
                    pengunjung.getTujuanKunjungan(),
                    FormatUtil.tanggal(pengunjung.getTanggalDaftar()),
                    pengunjung.isSudahInduksi() ? "Sudah" : "Belum"
            });
        }

        daftarPass.clear();
        passModel.setRowCount(0);
        for (VisitorPass pass : service.semuaPass()) {
            daftarPass.add(pass);
            passModel.addRow(new Object[]{
                    pass.getNomorDokumen(),
                    FormatUtil.tanggal(pass.getTanggalTerbit()),
                    pass.getPengunjung().getNama(),
                    FormatUtil.tanggal(pass.getTanggalKunjungan()),
                    FormatUtil.jam(pass.getJamMasuk()),
                    FormatUtil.tanggal(pass.getBerlakuSampai()),
                    pass.getAreaTujuan(),
                    pass.isMasihBerlaku(LocalDate.now()) ? "Aktif" : "Kedaluwarsa"
            });
        }
    }

    private void registrasi() {
        try {
            String nama = required(namaField, "Nama lengkap");
            String identitas = required(identitasField, "No. identitas");
            String telepon = required(teleponField, "No. telepon");
            String instansi = required(instansiField, "Instansi");
            String tujuan = required(tujuanField, "Tujuan kunjungan");

            Pengunjung pengunjung = service.registrasi(
                    nama, identitas, telepon, instansi, tujuan);
            bersihkanForm();
            refreshData();
            onChange.run();
            Theme.showInfo(this, "Pengunjung berhasil diregistrasi.\n\nID: "
                    + pengunjung.getId() + "\nNama: " + pengunjung.getNama());
        } catch (IllegalArgumentException ex) {
            Theme.showError(this, ex.getMessage());
        } catch (RuntimeException ex) {
            Theme.showError(this, "Registrasi gagal: " + ex.getMessage());
        }
    }

    private void bersihkanForm() {
        namaField.setText("");
        identitasField.setText("");
        teleponField.setText("");
        instansiField.setText("");
        tujuanField.setText("");
        namaField.requestFocusInWindow();
    }

    private Pengunjung terpilih() {
        int row = pengunjungTable.getSelectedRow();
        if (row < 0 || row >= daftarPengunjung.size()) {
            Theme.showInfo(this, "Pilih pengunjung pada tabel terlebih dahulu.");
            return null;
        }
        return daftarPengunjung.get(row);
    }

    private void terbitkanPass(Pengunjung pengunjung) {
        if (daftarPengunjung.isEmpty()) {
            Theme.showInfo(this, "Belum ada pengunjung. Daftarkan pengunjung terlebih dahulu.");
            return;
        }
        VisitorPass pass = VisitorPassDialog.show(this, service, admin, pengunjung);
        if (pass != null) {
            refreshData();
            onChange.run();
            tabs.setSelectedIndex(1);
            DokumenPreviewDialog.show(this, "Visitor Pass - " + pass.getNomorDokumen(),
                    pass.cetak());
        }
    }

    private void lihatPass() {
        int row = passTable.getSelectedRow();
        if (row < 0 || row >= daftarPass.size()) {
            Theme.showInfo(this, "Pilih visitor pass pada tabel terlebih dahulu.");
            return;
        }
        VisitorPass pass = daftarPass.get(row);
        DokumenPreviewDialog.show(this, "Visitor Pass - " + pass.getNomorDokumen(),
                pass.cetak());
    }

    private String required(JTextField field, String label) {
        if (field.getText() == null || field.getText().trim().isEmpty()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        return field.getText().trim();
    }

    /**
     * Helper kecil agar tidak bergantung pada import JLabel di banyak tempat.
     */
    private static final class JLabelKeterangan {
        private static void tambah(JPanel panel, String teks) {
            javax.swing.JLabel label = Theme.muted(teks);
            label.setFont(Theme.FONT_SMALL);
            panel.add(label);
        }
    }
}
