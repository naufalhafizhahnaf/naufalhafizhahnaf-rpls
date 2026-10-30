package ihsse.gui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.Window;
import java.awt.print.PrinterException;

/**
 * Dialog pratinjau dokumen (visitor pass, sertifikat, inspeksi, insiden)
 * dengan tombol cetak dan salin.
 */
public final class DokumenPreviewDialog extends JDialog {

    private final JTextArea textArea;

    private DokumenPreviewDialog(Window owner, String judul, String isi) {
        super(owner, judul, Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 12));
        getRootPane().setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18));

        add(Theme.title(judul), BorderLayout.NORTH);

        textArea = new JTextArea(isi);
        textArea.setEditable(false);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        textArea.setLineWrap(false);
        textArea.setBackground(Theme.CARD);
        textArea.setForeground(Theme.TEXT);
        textArea.setCaretPosition(0);
        JScrollPane scrollPane = Theme.scroll(textArea);
        Dimension layar = Toolkit.getDefaultToolkit().getScreenSize();
        scrollPane.setPreferredSize(new Dimension(
                Math.min(820, layar.width - 120),
                Math.min(540, layar.height - 180)));
        add(scrollPane, BorderLayout.CENTER);

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tombol.setOpaque(false);
        JButton salin = Theme.secondaryButton("Salin Teks");
        salin.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(textArea.getText()), null);
            Theme.showInfo(this, "Dokumen disalin ke clipboard.");
        });
        JButton cetak = Theme.primaryButton("Cetak");
        cetak.addActionListener(e -> {
            try {
                textArea.print();
            } catch (PrinterException ex) {
                Theme.showError(this, "Gagal mencetak: " + ex.getMessage());
            }
        });
        JButton tutup = Theme.secondaryButton("Tutup");
        tutup.addActionListener(e -> dispose());
        tombol.add(salin);
        tombol.add(cetak);
        tombol.add(tutup);
        add(tombol, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    public static void show(Component parent, String judul, String isi) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        DokumenPreviewDialog dialog = new DokumenPreviewDialog(owner, judul, isi);
        dialog.setVisible(true);
    }
}
