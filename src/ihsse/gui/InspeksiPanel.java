package ihsse.gui;

import ihsse.model.Admin;
import ihsse.model.FasilitasKeselamatan;
import ihsse.model.InformasiHSSE;
import ihsse.model.KondisiFasilitas;
import ihsse.model.LaporanInspeksi;
import ihsse.service.InformasiService;
import ihsse.service.InspeksiService;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel inspeksi fasilitas keselamatan.
 */
public class InspeksiPanel extends JPanel implements Refreshable {

    private static final String[] KONDISI_LABEL = {
            "Baik", "Rusak Ringan", "Rusak Berat", "Kedaluwarsa"
    };
    private static final KondisiFasilitas[] KONDISI = {
            KondisiFasilitas.BAIK,
            KondisiFasilitas.RUSAK_RINGAN,
            KondisiFasilitas.RUSAK_BERAT,
            KondisiFasilitas.KEDALUWARSA
    };

    private final InformasiService informasiService;
    private final InspeksiService inspeksiService;
    private final Admin admin;
    private final Runnable onChange;

    private final JComboBox<FasilitasKeselamatan> fasilitasCombo = new JComboBox<>();
    private final JTextField tanggalCekField = Theme.textField();
    private final JComboBox<String> kondisiCombo = Theme.comboBox(KONDISI_LABEL);
    private final JTextField kedaluwarsaField = Theme.textField();
    private final JTextField tindakanField = Theme.textField();
    private final JTextArea catatanArea = Theme.textArea(3);

    private final DefaultTableModel laporanModel;
    private final JTable laporanTable;
    private final List<FasilitasKeselamatan> daftarFasilitas = new ArrayList<>();
    private final List<LaporanInspeksi> daftarLaporan = new ArrayList<>();
    private boolean sedangMemuatFasilitas;

    public InspeksiPanel(InformasiService informasiService,
                         InspeksiService inspeksiService,
                         Admin admin, Runnable onChange) {
        this.informasiService = informasiService;
        this.inspeksiService = inspeksiService;
        this.admin = admin;
        this.onChange = onChange;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        JPanel formCard = Theme.card();
        formCard.setLayout(new BorderLayout(0, 10));
        formCard.add(Theme.bold("Form Inspeksi Fasilitas"), BorderLayout.NORTH);

        fasilitasCombo.setFont(Theme.FONT);
        fasilitasCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                           int index, boolean isSelected,
                                                           boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                if (value instanceof FasilitasKeselamatan) {
                    FasilitasKeselamatan fasilitas = (FasilitasKeselamatan) value;
                    setText(fasilitas.getKode() + " - " + fasilitas.getNama()
                            + " (" + fasilitas.getLokasi() + ")");
                }
                return this;
            }
        });
        fasilitasCombo.addActionListener(e -> {
            if (!sedangMemuatFasilitas) {
                isiDariFasilitas();
            }
        });

        tanggalCekField.setText(LocalDate.now().toString());
        kedaluwarsaField.setText("");

        JPanel form = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(form, gbc, 0, "Fasilitas", fasilitasCombo);
        Theme.addFormRow(form, gbc, 1, "Tanggal Cek", tanggalCekField);
        Theme.addFormRow(form, gbc, 2, "Kondisi", kondisiCombo);
        Theme.addFormRow(form, gbc, 3, "Masa Kedaluwarsa", kedaluwarsaField);
        Theme.addFormRow(form, gbc, 4, "Tindakan", tindakanField);
        JScrollPane catatanScroll = Theme.scroll(catatanArea);
        catatanScroll.setPreferredSize(new Dimension(0, 66));
        Theme.addFormRow(form, gbc, 5, "Catatan", catatanScroll);

        JPanel aksi = Theme.rightToolbar();
        JButton bersih = Theme.secondaryButton("Bersihkan");
        JButton simpan = Theme.primaryButton("Simpan Inspeksi");
        bersih.addActionListener(e -> bersihkanForm());
        simpan.addActionListener(e -> simpan());
        aksi.add(bersih);
        aksi.add(simpan);
        Theme.addFormRow(form, gbc, 6, "", aksi);
        formCard.add(form, BorderLayout.CENTER);
        add(formCard, BorderLayout.NORTH);

        laporanModel = new DefaultTableModel(
                new Object[]{"Nomor", "Tgl Terbit", "Fasilitas", "Tgl Cek", "Kondisi",
                        "Kedaluwarsa", "Petugas", "Tindakan"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        laporanTable = Theme.table();
        laporanTable.setModel(laporanModel);
        laporanTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(laporanTable, 175, 95, 190, 95, 105, 105, 150, 280);
        laporanTable.getColumnModel().getColumn(4)
                .setCellRenderer(Theme.statusRenderer());

        JPanel tableCard = Theme.card();
        tableCard.setLayout(new BorderLayout(0, 10));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.bold("Riwayat Inspeksi"), BorderLayout.WEST);
        JPanel tombol = Theme.rightToolbar();
        JButton lihat = Theme.secondaryButton("Lihat / Cetak");
        JButton muat = Theme.secondaryButton("Muat Ulang");
        lihat.addActionListener(e -> lihatLaporan());
        muat.addActionListener(e -> refreshData());
        tombol.add(lihat);
        tombol.add(muat);
        header.add(tombol, BorderLayout.EAST);
        tableCard.add(header, BorderLayout.NORTH);
        JScrollPane scroll = Theme.scroll(laporanTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        tableCard.add(scroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        String kodeTerpilih = null;
        if (fasilitasCombo.getSelectedItem() instanceof FasilitasKeselamatan) {
            kodeTerpilih = ((FasilitasKeselamatan) fasilitasCombo.getSelectedItem()).getKode();
        }

        sedangMemuatFasilitas = true;
        daftarFasilitas.clear();
        fasilitasCombo.removeAllItems();
        for (InformasiHSSE informasi
                : informasiService.informasiBerdasarkanKategori("Safety")) {
            if (informasi instanceof FasilitasKeselamatan) {
                FasilitasKeselamatan fasilitas = (FasilitasKeselamatan) informasi;
                daftarFasilitas.add(fasilitas);
                fasilitasCombo.addItem(fasilitas);
                if (kodeTerpilih != null && kodeTerpilih.equals(fasilitas.getKode())) {
                    fasilitasCombo.setSelectedItem(fasilitas);
                }
            }
        }
        sedangMemuatFasilitas = false;
        if (fasilitasCombo.getSelectedItem() != null) {
            isiDariFasilitas();
        }

        daftarLaporan.clear();
        laporanModel.setRowCount(0);
        for (LaporanInspeksi laporan : inspeksiService.semuaLaporan()) {
            daftarLaporan.add(laporan);
            laporanModel.addRow(new Object[]{
                    laporan.getNomorDokumen(),
                    FormatUtil.tanggal(laporan.getTanggalTerbit()),
                    laporan.getFasilitas().getKode() + " - "
                            + laporan.getFasilitas().getNama(),
                    FormatUtil.tanggal(laporan.getTanggalCek()),
                    laporan.getKondisi().getLabel(),
                    FormatUtil.tanggal(laporan.getMasaKedaluwarsa()),
                    laporan.getNamaPetugas(),
                    laporan.getTindakan()
            });
        }
    }

    private void isiDariFasilitas() {
        Object value = fasilitasCombo.getSelectedItem();
        if (!(value instanceof FasilitasKeselamatan)) {
            return;
        }
        FasilitasKeselamatan fasilitas = (FasilitasKeselamatan) value;
        kondisiCombo.setSelectedItem(fasilitas.getKondisi().getLabel());
        kedaluwarsaField.setText(FormatUtil.tanggalInput(
                fasilitas.getMasaKedaluwarsa()));
    }

    private void bersihkanForm() {
        tanggalCekField.setText(LocalDate.now().toString());
        kedaluwarsaField.setText("");
        tindakanField.setText("");
        catatanArea.setText("");
        kondisiCombo.setSelectedIndex(0);
        if (fasilitasCombo.getItemCount() > 0) {
            fasilitasCombo.setSelectedIndex(0);
        }
    }

    private void simpan() {
        try {
            Object value = fasilitasCombo.getSelectedItem();
            if (!(value instanceof FasilitasKeselamatan)) {
                throw new IllegalArgumentException("Fasilitas keselamatan wajib dipilih.");
            }
            FasilitasKeselamatan fasilitas = (FasilitasKeselamatan) value;
            LocalDate tanggalCek = FormatUtil.parseTanggal(tanggalCekField.getText());
            KondisiFasilitas kondisi = kondisiDari(
                    String.valueOf(kondisiCombo.getSelectedItem()));
            LocalDate kedaluwarsa = FormatUtil.parseTanggalOpsional(
                    kedaluwarsaField.getText());
            String tindakan = tindakanField.getText();
            if (tindakan == null || tindakan.trim().isEmpty()) {
                throw new IllegalArgumentException("Tindakan wajib diisi.");
            }
            String catatan = catatanArea.getText();
            if (catatan == null || catatan.trim().isEmpty()) {
                catatan = "-";
            }

            LaporanInspeksi laporan = inspeksiService.catatInspeksi(
                    fasilitas, tanggalCek, kondisi, kedaluwarsa, admin,
                    tindakan.trim(), catatan.trim());
            refreshData();
            onChange.run();
            DokumenPreviewDialog.show(this,
                    "Laporan Inspeksi - " + laporan.getNomorDokumen(), laporan.cetak());
            tindakanField.setText("");
            catatanArea.setText("");
        } catch (IllegalArgumentException ex) {
            Theme.showError(this, ex.getMessage());
        } catch (RuntimeException ex) {
            Theme.showError(this, "Gagal menyimpan inspeksi: " + ex.getMessage());
        }
    }

    private void lihatLaporan() {
        int row = laporanTable.getSelectedRow();
        if (row < 0 || row >= daftarLaporan.size()) {
            Theme.showInfo(this, "Pilih laporan inspeksi pada tabel terlebih dahulu.");
            return;
        }
        LaporanInspeksi laporan = daftarLaporan.get(row);
        DokumenPreviewDialog.show(this,
                "Laporan Inspeksi - " + laporan.getNomorDokumen(), laporan.cetak());
    }

    private KondisiFasilitas kondisiDari(String label) {
        for (int i = 0; i < KONDISI_LABEL.length; i++) {
            if (KONDISI_LABEL[i].equals(label)) {
                return KONDISI[i];
            }
        }
        return KondisiFasilitas.BAIK;
    }
}
