package ihsse.gui;

import ihsse.model.Admin;
import ihsse.model.Pengunjung;
import ihsse.model.SertifikatInduksi;
import ihsse.service.InduksiService;
import ihsse.service.PengunjungService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel safety induction dan sertifikat.
 */
public class InduksiPanel extends JPanel implements Refreshable {

    private final PengunjungService pengunjungService;
    private final InduksiService induksiService;
    private final Admin admin;
    private final Runnable onChange;

    private final DefaultTableModel pengunjungModel;
    private final DefaultTableModel sertifikatModel;
    private final JTable pengunjungTable;
    private final JTable sertifikatTable;
    private final List<Pengunjung> daftarPengunjung = new ArrayList<>();
    private final List<SertifikatInduksi> daftarSertifikat = new ArrayList<>();

    public InduksiPanel(PengunjungService pengunjungService,
                        InduksiService induksiService,
                        Admin admin, Runnable onChange) {
        this.pengunjungService = pengunjungService;
        this.induksiService = induksiService;
        this.admin = admin;
        this.onChange = onChange;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        JPanel info = Theme.card();
        info.setLayout(new BorderLayout(12, 0));
        JLabel judul = Theme.bold("Materi Wajib Safety Induction");
        judul.setPreferredSize(new Dimension(230, 0));
        info.add(judul, BorderLayout.WEST);
        JTextArea materi = Theme.textArea(4);
        materi.setEditable(false);
        materi.setOpaque(false);
        materi.setRows(4);
        StringBuilder teks = new StringBuilder();
        int nomor = 1;
        for (String item : InduksiService.MATERI_WAJIB) {
            teks.append(nomor++).append(". ").append(item).append('\n');
        }
        materi.setText(teks.toString());
        info.add(materi, BorderLayout.CENTER);
        add(info, BorderLayout.NORTH);

        pengunjungModel = new DefaultTableModel(
                new Object[]{"ID", "Nama", "Instansi", "Tgl Daftar", "Status Induksi"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pengunjungTable = Theme.table();
        pengunjungTable.setModel(pengunjungModel);
        pengunjungTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(pengunjungTable, 80, 190, 200, 110, 130);
        pengunjungTable.getColumnModel().getColumn(4)
                .setCellRenderer(Theme.statusRenderer());

        sertifikatModel = new DefaultTableModel(
                new Object[]{"Nomor", "Tgl Terbit", "Peserta", "Fasilitator",
                        "Tgl Selesai"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        sertifikatTable = Theme.table();
        sertifikatTable.setModel(sertifikatModel);
        sertifikatTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(sertifikatTable, 190, 100, 200, 190, 110);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buatCardPengunjung(), buatCardSertifikat());
        split.setResizeWeight(0.52);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buatCardPengunjung() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel judul = new JPanel();
        judul.setOpaque(false);
        judul.setLayout(new javax.swing.BoxLayout(judul, javax.swing.BoxLayout.Y_AXIS));
        judul.add(Theme.bold("Peserta Induksi"));
        JLabel ket = Theme.muted("Pilih pengunjung yang sudah menyelesaikan materi.");
        ket.setFont(Theme.FONT_SMALL);
        judul.add(ket);
        header.add(judul, BorderLayout.WEST);

        JPanel tombol = Theme.rightToolbar();
        JButton selesaikan = Theme.primaryButton("Selesaikan Induksi");
        JButton muat = Theme.secondaryButton("Muat Ulang");
        selesaikan.addActionListener(e -> selesaikanInduksi());
        muat.addActionListener(e -> refreshData());
        tombol.add(selesaikan);
        tombol.add(muat);
        header.add(tombol, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JScrollPane scroll = Theme.scroll(pengunjungTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buatCardSertifikat() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.bold("Sertifikat Induksi"), BorderLayout.WEST);

        JPanel tombol = Theme.rightToolbar();
        JButton lihat = Theme.secondaryButton("Lihat / Cetak");
        lihat.addActionListener(e -> lihatSertifikat());
        tombol.add(lihat);
        header.add(tombol, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JScrollPane scroll = Theme.scroll(sertifikatTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refreshData() {
        daftarPengunjung.clear();
        pengunjungModel.setRowCount(0);
        for (Pengunjung pengunjung : pengunjungService.semuaPengunjung()) {
            daftarPengunjung.add(pengunjung);
            pengunjungModel.addRow(new Object[]{
                    pengunjung.getId(),
                    pengunjung.getNama(),
                    pengunjung.getInstansi(),
                    FormatUtil.tanggal(pengunjung.getTanggalDaftar()),
                    pengunjung.isSudahInduksi() ? "Selesai" : "Belum"
            });
        }

        daftarSertifikat.clear();
        sertifikatModel.setRowCount(0);
        for (SertifikatInduksi sertifikat : induksiService.semuaSertifikat()) {
            daftarSertifikat.add(sertifikat);
            sertifikatModel.addRow(new Object[]{
                    sertifikat.getNomorDokumen(),
                    FormatUtil.tanggal(sertifikat.getTanggalTerbit()),
                    sertifikat.getPengunjung().getNama(),
                    sertifikat.getNamaFasilitator(),
                    FormatUtil.tanggal(sertifikat.getTanggalSelesai())
            });
        }
    }

    private void selesaikanInduksi() {
        int row = pengunjungTable.getSelectedRow();
        if (row < 0 || row >= daftarPengunjung.size()) {
            Theme.showInfo(this, "Pilih pengunjung pada tabel terlebih dahulu.");
            return;
        }
        Pengunjung pengunjung = daftarPengunjung.get(row);
        if (pengunjung.isSudahInduksi()) {
            Theme.showInfo(this, "Pengunjung " + pengunjung.getNama()
                    + " sudah memiliki sertifikat induksi.");
            return;
        }

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JTextArea info = Theme.textArea(5);
        info.setEditable(false);
        info.setOpaque(false);
        info.setText("Peserta: " + pengunjung.getNama() + "\n"
                + "Instansi: " + pengunjung.getInstansi() + "\n\n"
                + "Materi yang dinyatakan selesai:\n"
                + "1. " + InduksiService.MATERI_WAJIB.get(0) + "\n"
                + "2. " + InduksiService.MATERI_WAJIB.get(1) + "\n"
                + "3. " + InduksiService.MATERI_WAJIB.get(2) + "\n"
                + "4. " + InduksiService.MATERI_WAJIB.get(3));
        panel.add(info, BorderLayout.NORTH);

        JPanel form = Theme.formPanel();
        JTextField fasilitator = Theme.textField();
        fasilitator.setText(admin.getNama() + ", S.K.M.");
        javax.swing.JPanel formWrap = new javax.swing.JPanel(new BorderLayout());
        formWrap.setOpaque(false);
        formWrap.add(Theme.bold("Nama Fasilitator"), BorderLayout.NORTH);
        formWrap.add(fasilitator, BorderLayout.CENTER);
        panel.add(formWrap, BorderLayout.CENTER);

        int pilihan = JOptionPane.showConfirmDialog(this, panel,
                "Selesaikan Induksi - " + pengunjung.getNama(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (pilihan != JOptionPane.OK_OPTION) {
            return;
        }
        String namaFasilitator = fasilitator.getText();
        if (namaFasilitator == null || namaFasilitator.trim().isEmpty()) {
            Theme.showError(this, "Nama fasilitator wajib diisi.");
            return;
        }

        try {
            SertifikatInduksi sertifikat = induksiService.selesaikanInduksi(
                    pengunjung, admin, namaFasilitator.trim());
            refreshData();
            onChange.run();
            DokumenPreviewDialog.show(this,
                    "Sertifikat - " + sertifikat.getNomorDokumen(), sertifikat.cetak());
        } catch (RuntimeException ex) {
            Theme.showError(this, "Gagal menyelesaikan induksi: " + ex.getMessage());
        }
    }

    private void lihatSertifikat() {
        int row = sertifikatTable.getSelectedRow();
        if (row < 0 || row >= daftarSertifikat.size()) {
            Theme.showInfo(this, "Pilih sertifikat pada tabel terlebih dahulu.");
            return;
        }
        SertifikatInduksi sertifikat = daftarSertifikat.get(row);
        DokumenPreviewDialog.show(this,
                "Sertifikat - " + sertifikat.getNomorDokumen(), sertifikat.cetak());
    }
}
