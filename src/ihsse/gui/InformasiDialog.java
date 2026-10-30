package ihsse.gui;

import ihsse.model.AreaMerokok;
import ihsse.model.FasilitasKeamanan;
import ihsse.model.FasilitasKeselamatan;
import ihsse.model.FasilitasLingkungan;
import ihsse.model.InformasiHSSE;
import ihsse.model.KondisiFasilitas;
import ihsse.model.TingkatRisiko;
import ihsse.model.ZonaRisiko;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Toolkit;
import java.awt.Window;
import java.time.LocalDate;

/**
 * Dialog tambah/ubah informasi HSSE dengan field dinamis sesuai jenis konten.
 */
public class InformasiDialog extends JDialog {

    private static final String[] TIPE_LABEL = {
            "Area Merokok (Health)",
            "Fasilitas Keamanan (Security)",
            "Fasilitas Keselamatan (Safety)",
            "Zona Risiko (Safety)",
            "Fasilitas Lingkungan (Environment)"
    };
    private static final String[] TIPE_KEY = {
            "AreaMerokok", "FasilitasKeamanan", "FasilitasKeselamatan",
            "ZonaRisiko", "FasilitasLingkungan"
    };
    private static final String[] KONDISI_LABEL = {
            "Baik", "Rusak Ringan", "Rusak Berat", "Kedaluwarsa"
    };
    private static final KondisiFasilitas[] KONDISI = {
            KondisiFasilitas.BAIK,
            KondisiFasilitas.RUSAK_RINGAN,
            KondisiFasilitas.RUSAK_BERAT,
            KondisiFasilitas.KEDALUWARSA
    };

    private final JComboBox<String> tipeCombo = Theme.comboBox(TIPE_LABEL);
    private final JTextField kodeField = Theme.textField();
    private final JTextField namaField = Theme.textField();
    private final JTextField lokasiField = Theme.textField();
    private final JTextArea deskripsiArea = Theme.textArea(4);

    private final JTextField luasField = Theme.textField();
    private final JTextField aturanField = Theme.textField();
    private final JTextField sanksiField = Theme.textField();

    private final JTextField jenisKeamananField = Theme.textField();
    private final JTextField jumlahKeamananField = Theme.textField();
    private final JComboBox<String> kondisiKeamananCombo = Theme.comboBox(KONDISI_LABEL);

    private final JTextField jenisKeselamatanField = Theme.textField();
    private final JTextField jumlahKeselamatanField = Theme.textField();
    private final JComboBox<String> kondisiKeselamatanCombo = Theme.comboBox(KONDISI_LABEL);
    private final JTextField kedaluwarsaField = Theme.textField();

    private final JComboBox<String> risikoCombo = Theme.comboBox(
            "Rendah", "Sedang", "Tinggi");
    private final JTextArea prosedurArea = Theme.textArea(3);
    private final JTextField titikKumpulField = Theme.textField();

    private final JTextField jenisLingkunganField = Theme.textField();
    private final JTextField jamField = Theme.textField();
    private final JTextField penanggungJawabField = Theme.textField();

    private final CardLayout detailLayout = new CardLayout();
    private final JPanel detailPanel = new JPanel(detailLayout);
    private InformasiHSSE hasil;

    private InformasiDialog(Frame owner, InformasiHSSE existing) {
        super(owner, existing == null ? "Tambah Informasi HSSE" : "Ubah Informasi HSSE",
                true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Dimension layar = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(700, layar.width - 60), Math.min(760, layar.height - 80));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 12));

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.CARD);
        root.setBorder(new EmptyBorder(18, 22, 16, 22));
        setContentPane(root);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        header.add(Theme.title(existing == null ? "Tambah Informasi HSSE"
                : "Ubah Informasi HSSE"));
        header.add(javax.swing.Box.createVerticalStrut(3));
        header.add(Theme.muted("Isi data sesuai jenis konten. Format tanggal: yyyy-MM-dd."));
        root.add(header, BorderLayout.NORTH);

        JPanel form = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(form, gbc, 0, "Jenis Konten", tipeCombo);
        Theme.addFormRow(form, gbc, 1, "Kode", kodeField);
        Theme.addFormRow(form, gbc, 2, "Nama", namaField);
        Theme.addFormRow(form, gbc, 3, "Lokasi", lokasiField);
        JScrollPane deskripsiScroll = Theme.scroll(deskripsiArea);
        deskripsiScroll.setPreferredSize(new Dimension(0, 82));
        Theme.addFormRow(form, gbc, 4, "Deskripsi", deskripsiScroll);
        Theme.addFormRow(form, gbc, 5, "Detail", detailPanel);

        detailPanel.setOpaque(false);
        detailPanel.add(buatPanelAreaMerokok(), TIPE_KEY[0]);
        detailPanel.add(buatPanelKeamanan(), TIPE_KEY[1]);
        detailPanel.add(buatPanelKeselamatan(), TIPE_KEY[2]);
        detailPanel.add(buatPanelRisiko(), TIPE_KEY[3]);
        detailPanel.add(buatPanelLingkungan(), TIPE_KEY[4]);

        tipeCombo.addActionListener(e -> detailLayout.show(detailPanel,
                TIPE_KEY[tipeCombo.getSelectedIndex()]));
        detailLayout.show(detailPanel, TIPE_KEY[0]);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(Theme.CARD);
        root.add(formScroll, BorderLayout.CENTER);

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tombol.setOpaque(false);
        JButton batal = Theme.secondaryButton("Batal");
        JButton simpan = Theme.primaryButton("Simpan");
        batal.addActionListener(e -> dispose());
        simpan.addActionListener(e -> simpan());
        tombol.add(batal);
        tombol.add(simpan);
        root.add(tombol, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(simpan);

        if (existing != null) {
            isiForm(existing);
        }
        kodeField.requestFocusInWindow();
    }

    private JPanel buatPanelAreaMerokok() {
        JPanel panel = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(panel, gbc, 0, "Luas Area (m2)", luasField);
        Theme.addFormRow(panel, gbc, 1, "Aturan", aturanField);
        Theme.addFormRow(panel, gbc, 2, "Sanksi", sanksiField);
        return panel;
    }

    private JPanel buatPanelKeamanan() {
        JPanel panel = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(panel, gbc, 0, "Jenis Fasilitas", jenisKeamananField);
        Theme.addFormRow(panel, gbc, 1, "Jumlah", jumlahKeamananField);
        Theme.addFormRow(panel, gbc, 2, "Kondisi", kondisiKeamananCombo);
        return panel;
    }

    private JPanel buatPanelKeselamatan() {
        JPanel panel = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(panel, gbc, 0, "Jenis Fasilitas", jenisKeselamatanField);
        Theme.addFormRow(panel, gbc, 1, "Jumlah", jumlahKeselamatanField);
        Theme.addFormRow(panel, gbc, 2, "Kondisi", kondisiKeselamatanCombo);
        Theme.addFormRow(panel, gbc, 3, "Masa Kedaluwarsa", kedaluwarsaField);
        return panel;
    }

    private JPanel buatPanelRisiko() {
        JPanel panel = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(panel, gbc, 0, "Tingkat Risiko", risikoCombo);
        JScrollPane prosedurScroll = Theme.scroll(prosedurArea);
        prosedurScroll.setPreferredSize(new Dimension(0, 65));
        Theme.addFormRow(panel, gbc, 1, "Prosedur Evakuasi", prosedurScroll);
        Theme.addFormRow(panel, gbc, 2, "Titik Kumpul", titikKumpulField);
        return panel;
    }

    private JPanel buatPanelLingkungan() {
        JPanel panel = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(panel, gbc, 0, "Jenis Fasilitas", jenisLingkunganField);
        Theme.addFormRow(panel, gbc, 1, "Jam Operasional", jamField);
        Theme.addFormRow(panel, gbc, 2, "Penanggung Jawab", penanggungJawabField);
        return panel;
    }

    private void isiForm(InformasiHSSE existing) {
        tipeCombo.setSelectedIndex(indexTipe(existing));
        tipeCombo.setEnabled(false);
        kodeField.setText(existing.getKode());
        kodeField.setEnabled(false);
        namaField.setText(existing.getNama());
        lokasiField.setText(existing.getLokasi());
        deskripsiArea.setText(existing.getDeskripsi());

        if (existing instanceof AreaMerokok) {
            AreaMerokok area = (AreaMerokok) existing;
            luasField.setText(String.valueOf(area.getLuasArea()));
            aturanField.setText(area.getAturan());
            sanksiField.setText(area.getSanksi());
        } else if (existing instanceof FasilitasKeamanan) {
            FasilitasKeamanan fasilitas = (FasilitasKeamanan) existing;
            jenisKeamananField.setText(fasilitas.getJenis());
            jumlahKeamananField.setText(String.valueOf(fasilitas.getJumlah()));
            kondisiKeamananCombo.setSelectedItem(fasilitas.getKondisi().getLabel());
        } else if (existing instanceof FasilitasKeselamatan) {
            FasilitasKeselamatan fasilitas = (FasilitasKeselamatan) existing;
            jenisKeselamatanField.setText(fasilitas.getJenis());
            jumlahKeselamatanField.setText(String.valueOf(fasilitas.getJumlah()));
            kondisiKeselamatanCombo.setSelectedItem(fasilitas.getKondisi().getLabel());
            kedaluwarsaField.setText(FormatUtil.tanggalInput(
                    fasilitas.getMasaKedaluwarsa()));
        } else if (existing instanceof ZonaRisiko) {
            ZonaRisiko zona = (ZonaRisiko) existing;
            risikoCombo.setSelectedItem(zona.getTingkatRisiko().getLabel());
            prosedurArea.setText(zona.getProsedurEvakuasi());
            titikKumpulField.setText(zona.getTitikKumpul());
        } else if (existing instanceof FasilitasLingkungan) {
            FasilitasLingkungan fasilitas = (FasilitasLingkungan) existing;
            jenisLingkunganField.setText(fasilitas.getJenis());
            jamField.setText(fasilitas.getJamOperasional());
            penanggungJawabField.setText(fasilitas.getPenanggungJawab());
        }
    }

    private void simpan() {
        try {
            String kode = required(kodeField, "Kode");
            String nama = required(namaField, "Nama");
            String lokasi = required(lokasiField, "Lokasi");
            String deskripsi = required(deskripsiArea, "Deskripsi");
            String tipe = TIPE_KEY[tipeCombo.getSelectedIndex()];

            if ("AreaMerokok".equals(tipe)) {
                hasil = new AreaMerokok(kode, nama, lokasi, deskripsi,
                        parseDouble(luasField, "Luas area"),
                        required(aturanField, "Aturan"),
                        required(sanksiField, "Sanksi"));
            } else if ("FasilitasKeamanan".equals(tipe)) {
                hasil = new FasilitasKeamanan(kode, nama, lokasi, deskripsi,
                        required(jenisKeamananField, "Jenis fasilitas"),
                        parseInt(jumlahKeamananField, "Jumlah"),
                        kondisiDari(String.valueOf(kondisiKeamananCombo.getSelectedItem())));
            } else if ("FasilitasKeselamatan".equals(tipe)) {
                LocalDate kedaluwarsa = FormatUtil.parseTanggalOpsional(
                        kedaluwarsaField.getText());
                hasil = new FasilitasKeselamatan(kode, nama, lokasi, deskripsi,
                        required(jenisKeselamatanField, "Jenis fasilitas"),
                        parseInt(jumlahKeselamatanField, "Jumlah"),
                        kondisiDari(String.valueOf(kondisiKeselamatanCombo.getSelectedItem())),
                        kedaluwarsa);
            } else if ("ZonaRisiko".equals(tipe)) {
                hasil = new ZonaRisiko(kode, nama, lokasi, deskripsi,
                        risikoDari(String.valueOf(risikoCombo.getSelectedItem())),
                        required(prosedurArea, "Prosedur evakuasi"),
                        required(titikKumpulField, "Titik kumpul"));
            } else {
                hasil = new FasilitasLingkungan(kode, nama, lokasi, deskripsi,
                        required(jenisLingkunganField, "Jenis fasilitas"),
                        required(jamField, "Jam operasional"),
                        required(penanggungJawabField, "Penanggung jawab"));
            }
            dispose();
        } catch (IllegalArgumentException ex) {
            Theme.showError(this, ex.getMessage());
        }
    }

    private int indexTipe(InformasiHSSE informasi) {
        if (informasi instanceof AreaMerokok) {
            return 0;
        }
        if (informasi instanceof FasilitasKeamanan) {
            return 1;
        }
        if (informasi instanceof FasilitasKeselamatan) {
            return 2;
        }
        if (informasi instanceof ZonaRisiko) {
            return 3;
        }
        return 4;
    }

    private String required(JTextField field, String label) {
        String nilai = field.getText();
        if (nilai == null || nilai.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        return nilai.trim();
    }

    private String required(JTextArea area, String label) {
        String nilai = area.getText();
        if (nilai == null || nilai.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        return nilai.trim();
    }

    private int parseInt(JTextField field, String label) {
        try {
            int nilai = Integer.parseInt(field.getText().trim());
            if (nilai < 1) {
                throw new NumberFormatException();
            }
            return nilai;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " harus angka bulat minimal 1.");
        }
    }

    private double parseDouble(JTextField field, String label) {
        try {
            double nilai = Double.parseDouble(field.getText().trim());
            if (nilai <= 0) {
                throw new NumberFormatException();
            }
            return nilai;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " harus angka lebih dari 0.");
        }
    }

    private KondisiFasilitas kondisiDari(String label) {
        for (int i = 0; i < KONDISI_LABEL.length; i++) {
            if (KONDISI_LABEL[i].equals(label)) {
                return KONDISI[i];
            }
        }
        return KondisiFasilitas.BAIK;
    }

    private TingkatRisiko risikoDari(String label) {
        if ("Rendah".equals(label)) {
            return TingkatRisiko.RENDAH;
        }
        if ("Sedang".equals(label)) {
            return TingkatRisiko.SEDANG;
        }
        return TingkatRisiko.TINGGI;
    }

    public static InformasiHSSE show(Component parent, InformasiHSSE existing) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        Frame frame = owner instanceof Frame ? (Frame) owner : null;
        InformasiDialog dialog = new InformasiDialog(frame, existing);
        dialog.setVisible(true);
        return dialog.hasil;
    }
}
