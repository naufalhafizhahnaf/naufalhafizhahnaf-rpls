package ihsse.gui;

import ihsse.model.FasilitasKeselamatan;
import ihsse.model.InformasiHSSE;
import ihsse.model.LaporanInsiden;
import ihsse.model.StatusInsiden;
import ihsse.service.InformasiService;
import ihsse.service.InsidenService;
import ihsse.service.StatistikService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dashboard ringkasan angka dan aktivitas terbaru.
 */
public class DashboardPanel extends JPanel implements Refreshable {

    private final StatistikService statistikService;
    private final InsidenService insidenService;
    private final InformasiService informasiService;

    private final Map<String, StatCard> cards = new LinkedHashMap<>();
    private final DefaultTableModel insidenModel;
    private final DefaultTableModel fasilitasModel;
    private final JLabel statusLabel = new JLabel(" ");

    public DashboardPanel(StatistikService statistikService,
                          InsidenService insidenService,
                          InformasiService informasiService) {
        this.statistikService = statistikService;
        this.insidenService = insidenService;
        this.informasiService = informasiService;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        JPanel stats = new JPanel(new GridLayout(2, 3, 14, 14));
        stats.setOpaque(false);
        cards.put("pengunjung", new StatCard("Pengunjung",
                "Total pengunjung terdaftar", new Color(52, 120, 196)));
        cards.put("pass", new StatCard("Visitor Pass",
                "Pass yang sudah diterbitkan", new Color(14, 159, 110)));
        cards.put("sertifikat", new StatCard("Sertifikat Induksi",
                "Peserta yang sudah selesai", new Color(120, 92, 190)));
        cards.put("inspeksi", new StatCard("Laporan Inspeksi",
                "Pemeriksaan fasilitas", new Color(220, 140, 35)));
        cards.put("insiden_baru", new StatCard("Insiden Baru",
                "Menunggu tindak lanjut", new Color(214, 69, 69)));
        cards.put("insiden_selesai", new StatCard("Insiden Selesai",
                "Sudah ditangani admin", new Color(35, 148, 96)));
        for (StatCard card : cards.values()) {
            stats.add(card);
        }
        add(stats, BorderLayout.NORTH);

        JPanel bawah = new JPanel(new GridLayout(1, 2, 16, 0));
        bawah.setOpaque(false);

        insidenModel = new DefaultTableModel(
                new Object[]{"Nomor", "Waktu", "Pelapor", "Lokasi", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bawah.add(buatTabelCard("Laporan Insiden Terbaru",
                "5 laporan terakhir dari pengguna", insidenModel, 4));

        fasilitasModel = new DefaultTableModel(
                new Object[]{"Kode", "Fasilitas", "Kondisi", "Kedaluwarsa"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bawah.add(buatTabelCard("Kondisi Fasilitas Keselamatan",
                "APAR, hydrant, tangga darurat, titik kumpul", fasilitasModel, 2));

        add(bawah, BorderLayout.CENTER);

        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.MUTED);
        statusLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel buatTabelCard(String judul, String keterangan,
                                 DefaultTableModel model, int statusColumn) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = Theme.bold(judul);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel sub = Theme.muted(keterangan);
        sub.setFont(Theme.FONT_SMALL);
        header.add(title);
        header.add(javax.swing.Box.createVerticalStrut(2));
        header.add(sub);
        card.add(header, BorderLayout.NORTH);

        JTable table = Theme.table();
        table.setModel(model);
        table.setRowHeight(28);
        if (statusColumn >= 0 && statusColumn < table.getColumnCount()) {
            table.getColumnModel().getColumn(statusColumn)
                    .setCellRenderer(Theme.statusRenderer());
        }
        Theme.styleTableColumns(table, 150, 140, 130, 160, 90);
        JScrollPane scrollPane = Theme.scroll(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refreshData() {
        cards.get("pengunjung").setValue(statistikService.jumlahPengunjung());
        cards.get("pass").setValue(statistikService.jumlahVisitorPass());
        cards.get("sertifikat").setValue(statistikService.jumlahSertifikat());
        cards.get("inspeksi").setValue(statistikService.jumlahInspeksi());
        cards.get("insiden_baru").setValue(
                statistikService.jumlahInsiden(StatusInsiden.BARU));
        cards.get("insiden_selesai").setValue(
                statistikService.jumlahInsiden(StatusInsiden.SELESAI));

        insidenModel.setRowCount(0);
        for (LaporanInsiden laporan : statistikService.insidenTerbaru(5)) {
            insidenModel.addRow(new Object[]{
                    laporan.getNomorDokumen(),
                    FormatUtil.waktu(laporan.getWaktuKejadian()),
                    laporan.getNamaPelapor(),
                    laporan.getLokasi(),
                    laporan.getStatus().getLabel()
            });
        }

        fasilitasModel.setRowCount(0);
        for (InformasiHSSE informasi
                : informasiService.informasiBerdasarkanKategori("Safety")) {
            if (informasi instanceof FasilitasKeselamatan) {
                FasilitasKeselamatan fasilitas = (FasilitasKeselamatan) informasi;
                fasilitasModel.addRow(new Object[]{
                        fasilitas.getKode(),
                        fasilitas.getNama(),
                        fasilitas.getKondisi().getLabel(),
                        FormatUtil.tanggal(fasilitas.getMasaKedaluwarsa())
                });
            }
        }

        statusLabel.setText("Terakhir diperbarui: "
                + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private static final class StatCard extends JPanel {

        private final JLabel valueLabel = new JLabel("0");

        private StatCard(String judul, String keterangan, Color accent) {
            setLayout(new BorderLayout(0, 6));
            setBackground(Theme.CARD);
            setBorder(new CompoundBorder(
                    new MatteBorder(0, 5, 0, 0, accent),
                    new CompoundBorder(
                            new LineBorder(Theme.BORDER, 1, true),
                            new EmptyBorder(13, 16, 13, 16))));

            valueLabel.setFont(Theme.BIG_NUMBER);
            valueLabel.setForeground(accent);
            JLabel title = new JLabel(judul);
            title.setFont(Theme.FONT_BOLD);
            title.setForeground(Theme.TEXT);
            JLabel hint = new JLabel(keterangan);
            hint.setFont(Theme.FONT_SMALL);
            hint.setForeground(Theme.MUTED);

            JPanel labels = new JPanel();
            labels.setOpaque(false);
            labels.setLayout(new javax.swing.BoxLayout(
                    labels, javax.swing.BoxLayout.Y_AXIS));
            labels.add(title);
            labels.add(hint);

            add(valueLabel, BorderLayout.NORTH);
            add(labels, BorderLayout.CENTER);
        }

        private void setValue(int nilai) {
            valueLabel.setText(String.valueOf(nilai));
        }
    }
}
