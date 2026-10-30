package ihsse.gui;

import ihsse.model.StatusInsiden;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tema warna, font, dan komponen standar aplikasi GUI.
 */
public final class Theme {

    public static final Color SIDEBAR = new Color(15, 42, 68);
    public static final Color SIDEBAR_ACTIVE = new Color(24, 61, 96);
    public static final Color SIDEBAR_TEXT = new Color(206, 218, 230);
    public static final Color ACCENT = new Color(14, 159, 110);
    public static final Color ACCENT_DARK = new Color(11, 128, 88);
    public static final Color ACCENT_SOFT = new Color(225, 247, 238);
    public static final Color DANGER = new Color(214, 69, 69);
    public static final Color WARNING = new Color(191, 120, 0);
    public static final Color BACKGROUND = new Color(244, 246, 248);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(33, 37, 41);
    public static final Color MUTED = new Color(108, 117, 125);
    public static final Color BORDER = new Color(222, 226, 230);
    public static final Color TABLE_HEADER = new Color(248, 249, 250);

    public static final Font FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font BIG_NUMBER = new Font("Segoe UI", Font.BOLD, 28);

    private Theme() {
    }

    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // fallback ke look and feel default
        }
        UIManager.put("control", BACKGROUND);
        UIManager.put("info", CARD);
        UIManager.put("nimbusBase", SIDEBAR);
        UIManager.put("nimbusBlueGrey", new Color(210, 216, 222));
        UIManager.put("nimbusLightBackground", CARD);
        UIManager.put("text", TEXT);
        UIManager.put("Table.alternateRowColor", new Color(250, 251, 252));
        UIManager.put("Table.showGrid", false);
        UIManager.put("defaultFont", FONT);
    }

    public static JButton primaryButton(String text) {
        return new RoundedButton(text, ACCENT, Color.WHITE);
    }

    public static JButton secondaryButton(String text) {
        return new RoundedButton(text, new Color(233, 236, 239), TEXT);
    }

    public static JButton dangerButton(String text) {
        return new RoundedButton(text, DANGER, Color.WHITE);
    }

    public static JButton navButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(SIDEBAR_TEXT);
        button.setBackground(SIDEBAR);
        button.setOpaque(true);
        button.setBorder(new EmptyBorder(12, 20, 12, 20));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!SIDEBAR_ACTIVE.equals(button.getBackground())) {
                    button.setBackground(new Color(20, 51, 80));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!SIDEBAR_ACTIVE.equals(button.getBackground())) {
                    button.setBackground(SIDEBAR);
                }
            }
        });
        return button;
    }

    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD);
        panel.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)));
        return panel;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel subtitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SUBTITLE);
        label.setForeground(MUTED);
        return label;
    }

    public static JLabel bold(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BOLD);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT);
        label.setForeground(MUTED);
        return label;
    }

    public static JTextField textField() {
        JTextField field = new JTextField();
        field.setFont(FONT);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(7, 9, 7, 9)));
        return field;
    }

    public static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(FONT);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(7, 9, 7, 9)));
        return field;
    }

    public static JTextArea textArea(int rows) {
        JTextArea area = new JTextArea(rows, 20);
        area.setFont(FONT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(6, 8, 6, 8));
        area.setBackground(CARD);
        area.setForeground(TEXT);
        return area;
    }

    public static JScrollPane scroll(Component component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(new LineBorder(BORDER, 1, true));
        scrollPane.getViewport().setBackground(CARD);
        return scrollPane;
    }

    public static JTable table() {
        JTable table = new JTable();
        table.setFont(FONT);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(237, 240, 243));
        table.setSelectionBackground(ACCENT_SOFT);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setBackground(TABLE_HEADER);
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        return table;
    }

    public static JComboBox<String> comboBox(String... items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setFont(FONT);
        comboBox.setBackground(CARD);
        return comboBox;
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Terjadi Kesalahan",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Informasi",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Konfirmasi",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    public static Color statusColor(String status) {
        if (status == null) {
            return TEXT;
        }
        String value = status.trim();
        if (value.equalsIgnoreCase(StatusInsiden.SELESAI.getLabel())
                || value.equalsIgnoreCase("Aktif")
                || value.equalsIgnoreCase("Baik")
                || value.equalsIgnoreCase("Sudah")) {
            return ACCENT_DARK;
        }
        if (value.equalsIgnoreCase(StatusInsiden.DIPROSES.getLabel())
                || value.equalsIgnoreCase("Rusak Ringan")
                || value.equalsIgnoreCase("Sedang")) {
            return WARNING;
        }
        if (value.equalsIgnoreCase(StatusInsiden.BARU.getLabel())
                || value.equalsIgnoreCase("Kedaluwarsa")
                || value.equalsIgnoreCase("Rusak Berat")
                || value.equalsIgnoreCase("Tinggi")) {
            return DANGER;
        }
        return TEXT;
    }

    public static DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus, int row,
                                                           int column) {
                Component component = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                component.setFont(FONT_BOLD);
                if (!isSelected) {
                    component.setForeground(statusColor(String.valueOf(value)));
                }
                return component;
            }
        };
    }

    public static JPanel formPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new java.awt.GridBagLayout());
        return panel;
    }

    public static void addFormRow(JPanel panel, java.awt.GridBagConstraints gbc,
                                  int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gbc.insets = new java.awt.Insets(6, 0, 6, 12);
        panel.add(bold(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(6, 0, 6, 0);
        panel.add(field, gbc);
    }

    public static void fillForm(JPanel panel) {
        java.awt.GridBagConstraints spacer = new java.awt.GridBagConstraints();
        spacer.gridx = 0;
        spacer.gridy = 999;
        spacer.weighty = 1;
        panel.add(new JLabel(""), spacer);
    }

    public static JPanel labelValue(String label, JComponent value) {
        JPanel panel = new JPanel(new java.awt.BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(bold(label), java.awt.BorderLayout.NORTH);
        panel.add(value, java.awt.BorderLayout.CENTER);
        return panel;
    }

    public static JPanel toolbar() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        return panel;
    }

    public static JPanel rightToolbar() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);
        return panel;
    }

    public static void styleTableColumns(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    public static JLabel badge(String text, Color color) {
        JLabel label = new JLabel("  " + text + "  ");
        label.setFont(FONT_BOLD);
        label.setForeground(color);
        label.setOpaque(true);
        label.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 24));
        label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        return label;
    }

    private static final class RoundedButton extends JButton {

        private final Color baseColor;

        private RoundedButton(String text, Color background, Color foreground) {
            super(text);
            this.baseColor = background;
            setFont(FONT_BOLD);
            setForeground(foreground);
            setBackground(background);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 16, 9, 16));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) {
                        setBackground(baseColor.brighter());
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(baseColor);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isEnabled() ? getBackground() : new Color(205, 210, 216));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }
}
