package ihsse.gui;

import ihsse.model.Admin;
import ihsse.model.Pengunjung;
import ihsse.model.VisitorPass;
import ihsse.service.PengunjungService;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Toolkit;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Dialog penerbitan visitor pass.
 */
public class VisitorPassDialog extends JDialog {

    private final PengunjungService service;
    private final Admin admin;
    private final JComboBox<Pengunjung> pengunjungCombo;
    private final JTextField tanggalField = Theme.textField();
    private final JTextField jamField = Theme.textField();
    private final JSpinner masaSpinner = new JSpinner(
            new SpinnerNumberModel(1, 1, 30, 1));
    private final JTextField areaField = Theme.textField();
    private VisitorPass hasil;

    private VisitorPassDialog(Frame owner, PengunjungService service, Admin admin,
                              Pengunjung selected) {
        super(owner, "Terbitkan Visitor Pass", true);
        this.service = service;
        this.admin = admin;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Dimension layar = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(590, layar.width - 60), Math.min(500, layar.height - 80));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 12));

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.CARD);
        root.setBorder(new EmptyBorder(18, 22, 16, 22));
        setContentPane(root);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        header.add(Theme.title("Terbitkan Visitor Pass"));
        header.add(javax.swing.Box.createVerticalStrut(3));
        header.add(Theme.muted("Tanggal: yyyy-MM-dd, jam: HH:mm."));
        root.add(header, BorderLayout.NORTH);

        List<Pengunjung> daftar = service.semuaPengunjung();
        pengunjungCombo = new JComboBox<>(daftar.toArray(new Pengunjung[0]));
        pengunjungCombo.setFont(Theme.FONT);
        pengunjungCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                           int index, boolean isSelected,
                                                           boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                if (value instanceof Pengunjung) {
                    Pengunjung pengunjung = (Pengunjung) value;
                    setText(pengunjung.getId() + " - " + pengunjung.getNama()
                            + " (" + pengunjung.getInstansi() + ")");
                }
                return this;
            }
        });

        if (selected != null) {
            for (int i = 0; i < pengunjungCombo.getItemCount(); i++) {
                Pengunjung item = pengunjungCombo.getItemAt(i);
                if (item.getId().equals(selected.getId())) {
                    pengunjungCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        tanggalField.setText(LocalDate.now().toString());
        jamField.setText(LocalTime.now().withSecond(0).withNano(0).toString());
        areaField.setText("Lantai 6 - Ruang Seminar");

        JPanel form = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(form, gbc, 0, "Pengunjung", pengunjungCombo);
        Theme.addFormRow(form, gbc, 1, "Tanggal Kunjungan", tanggalField);
        Theme.addFormRow(form, gbc, 2, "Jam Masuk", jamField);
        Theme.addFormRow(form, gbc, 3, "Masa Berlaku (hari)", masaSpinner);
        Theme.addFormRow(form, gbc, 4, "Area Tujuan", areaField);
        root.add(form, BorderLayout.CENTER);

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tombol.setOpaque(false);
        JButton batal = Theme.secondaryButton("Batal");
        JButton simpan = Theme.primaryButton("Terbitkan");
        batal.addActionListener(e -> dispose());
        simpan.addActionListener(e -> simpan());
        tombol.add(batal);
        tombol.add(simpan);
        root.add(tombol, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(simpan);
    }

    private void simpan() {
        try {
            Pengunjung pengunjung = (Pengunjung) pengunjungCombo.getSelectedItem();
            if (pengunjung == null) {
                throw new IllegalArgumentException("Pengunjung wajib dipilih.");
            }
            LocalDate tanggal = FormatUtil.parseTanggal(tanggalField.getText());
            LocalTime jam = FormatUtil.parseJam(jamField.getText());
            int masa = (Integer) masaSpinner.getValue();
            String area = areaField.getText();
            if (area == null || area.trim().isEmpty()) {
                throw new IllegalArgumentException("Area tujuan wajib diisi.");
            }

            hasil = service.terbitkanVisitorPass(
                    pengunjung, admin, tanggal, jam, masa, area.trim());
            dispose();
        } catch (IllegalArgumentException ex) {
            Theme.showError(this, ex.getMessage());
        } catch (RuntimeException ex) {
            Theme.showError(this, "Gagal menerbitkan pass: " + ex.getMessage());
        }
    }

    public static VisitorPass show(Component parent, PengunjungService service,
                                   Admin admin, Pengunjung selected) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        Frame frame = owner instanceof Frame ? (Frame) owner : null;
        VisitorPassDialog dialog = new VisitorPassDialog(frame, service, admin, selected);
        dialog.setVisible(true);
        return dialog.hasil;
    }
}
