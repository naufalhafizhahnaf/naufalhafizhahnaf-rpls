package ihsse.gui;

import ihsse.dao.AdminDao;
import ihsse.db.Database;
import ihsse.model.Admin;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.util.Arrays;

/**
 * Halaman login admin.
 */
public class LoginFrame extends JFrame {

    private final AdminDao adminDao = new AdminDao();
    private final JTextField usernameField = Theme.textField();
    private final JPasswordField passwordField = Theme.passwordField();
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

    public LoginFrame() {
        setTitle("I-HSSE Safety Induction - Login Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension layar = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(480, layar.width - 60), Math.min(610, layar.height - 80));
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CARD);
        setContentPane(root);

        root.add(buatHeader(), BorderLayout.NORTH);
        root.add(buatForm(), BorderLayout.CENTER);

        cekDatabase();
    }

    private JPanel buatHeader() {
        JPanel header = new GradientPanel();
        header.setPreferredSize(new Dimension(0, 205));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(38, 40, 28, 40));

        JLabel logo = new JLabel("I-HSSE");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 42));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(LEFT_ALIGNMENT);

        JLabel judul = new JLabel("Safety Induction");
        judul.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        judul.setForeground(new Color(214, 235, 228));
        judul.setAlignmentX(LEFT_ALIGNMENT);

        JLabel kampus = new JLabel("Universitas Gunadarma Kampus J6");
        kampus.setFont(Theme.FONT);
        kampus.setForeground(new Color(180, 205, 220));
        kampus.setAlignmentX(LEFT_ALIGNMENT);

        header.add(logo);
        header.add(Box.createVerticalStrut(4));
        header.add(judul);
        header.add(Box.createVerticalStrut(10));
        header.add(kampus);
        return header;
    }

    private JPanel buatForm() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.CARD);
        wrapper.setBorder(new EmptyBorder(24, 42, 28, 42));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 4, 0);

        JLabel heading = new JLabel("Masuk sebagai Admin");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 18));
        heading.setForeground(Theme.TEXT);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 18, 0);
        form.add(heading, gbc);

        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.gridy = 1;
        form.add(Theme.bold("Username"), gbc);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 14, 0);
        form.add(usernameField, gbc);

        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.gridy = 3;
        form.add(Theme.bold("Password"), gbc);
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 8, 0);
        form.add(passwordField, gbc);

        JCheckBox tampilPassword = new JCheckBox("Tampilkan password");
        tampilPassword.setOpaque(false);
        tampilPassword.setFont(Theme.FONT);
        tampilPassword.setForeground(Theme.MUTED);
        tampilPassword.setFocusPainted(false);
        tampilPassword.addActionListener(e -> passwordField.setEchoChar(
                tampilPassword.isSelected() ? (char) 0 : '\u2022'));
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 16, 0);
        form.add(tampilPassword, gbc);

        javax.swing.JButton login = Theme.primaryButton("Masuk");
        login.setPreferredSize(new Dimension(0, 42));
        login.addActionListener(e -> login());
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 12, 0);
        form.add(login, gbc);
        getRootPane().setDefaultButton(login);

        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.MUTED);
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 10, 0);
        form.add(statusLabel, gbc);

        JLabel hint = new JLabel("Login default: admin / admin123", SwingConstants.CENTER);
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.MUTED);
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(hint, gbc);

        wrapper.add(form);
        return wrapper;
    }

    private void cekDatabase() {
        try {
            String versi = Database.testConnection();
            statusLabel.setText("Database terhubung - ihsse_db");
            statusLabel.setToolTipText(versi);
            statusLabel.setForeground(Theme.ACCENT_DARK);
        } catch (Exception | LinkageError ex) {
            statusLabel.setText("Database belum terhubung. Jalankan MySQL XAMPP.");
            statusLabel.setForeground(Theme.DANGER);
        }
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username dan password wajib diisi.");
            statusLabel.setForeground(Theme.DANGER);
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            Admin admin = adminDao.login(username, password);
            if (admin == null) {
                statusLabel.setText("Username atau password salah.");
                statusLabel.setForeground(Theme.DANGER);
                passwordField.setText("");
                passwordField.requestFocusInWindow();
                return;
            }
            Arrays.fill(passwordField.getPassword(), '\0');
            MainFrame mainFrame = new MainFrame(admin);
            mainFrame.setVisible(true);
            dispose();
        } catch (RuntimeException | LinkageError ex) {
            statusLabel.setText("Login gagal: " + ex.getMessage());
            statusLabel.setForeground(Theme.DANGER);
            Theme.showError(this, "Login gagal.\n\n" + ex.getMessage());
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private static final class GradientPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint paint = new GradientPaint(
                    0, 0, new Color(10, 31, 51),
                    getWidth(), getHeight(), new Color(16, 91, 94));
            g2.setPaint(paint);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
