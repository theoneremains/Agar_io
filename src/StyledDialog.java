
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * StyledDialog : Modern dark-themed dialog replacements for JOptionPane.
 * Provides input dialogs, confirmation dialogs, and message dialogs
 * that match the game's modern UI style.
 * @author Kamil Yunus Ozkaya
 */
public class StyledDialog {

    private static final Color BG_COLOR = new Color(30, 35, 50);
    private static final Color PANEL_COLOR = new Color(40, 45, 60);
    private static final Color TEXT_COLOR = new Color(220, 230, 255);
    private static final Color ACCENT_COLOR = new Color(80, 140, 220);
    private static final Color FIELD_BG = new Color(50, 55, 70);
    private static final Color FIELD_BORDER = new Color(70, 80, 110);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font FIELD_FONT = new Font("SansSerif", Font.PLAIN, 14);

    /**
     * Shows a modern styled input dialog with a single text field.
     * @param parent parent component
     * @param message the prompt message
     * @param defaultValue default text in the input field
     * @return the entered text, or null if cancelled
     */
    public static String showInputDialog(Component parent, String message, String defaultValue) {
        JFrame owner = getFrame(parent);
        JDialog dialog = new JDialog(owner, "", true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        String[] result = {null};

        JPanel content = createRoundedPanel();
        content.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 20, 6, 20);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Message label
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 24, 8, 24);
        JLabel label = new JLabel(message);
        label.setFont(TITLE_FONT);
        label.setForeground(TEXT_COLOR);
        content.add(label, gbc);

        // Input field
        gbc.gridy = 1;
        gbc.insets = new Insets(4, 24, 12, 24);
        JTextField field = new JTextField(defaultValue != null ? defaultValue : "", 18);
        styleField(field);
        content.add(field, gbc);

        // Buttons
        gbc.gridy = 2;
        gbc.insets = new Insets(4, 24, 20, 24);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnPanel.setOpaque(false);

        StyledButton okBtn = new StyledButton("OK", new Color(40, 120, 70));
        okBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        okBtn.setPreferredSize(new Dimension(100, 38));
        okBtn.addActionListener(e -> {
            Sound.playClickSound();
            result[0] = field.getText();
            dialog.dispose();
        });

        StyledButton cancelBtn = new StyledButton("CANCEL", new Color(100, 60, 60));
        cancelBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        cancelBtn.setPreferredSize(new Dimension(100, 38));
        cancelBtn.addActionListener(e -> {
            Sound.playClickSound();
            dialog.dispose();
        });

        btnPanel.add(okBtn);
        btnPanel.add(cancelBtn);
        content.add(btnPanel, gbc);

        // Enter key submits
        field.addActionListener(e -> {
            result[0] = field.getText();
            dialog.dispose();
        });

        // Escape key cancels
        content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        content.getActionMap().put("cancel", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dialog.dispose(); }
        });

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        field.requestFocusInWindow();
        field.selectAll();
        dialog.setVisible(true);

        return result[0];
    }

    /**
     * Shows a modern styled confirmation dialog (Yes/No).
     * @param parent parent component
     * @param message the confirmation message
     * @param title the dialog title (shown as header text)
     * @return true if user clicked Yes, false otherwise
     */
    public static boolean showConfirmDialog(Component parent, String message, String title) {
        JFrame owner = getFrame(parent);
        JDialog dialog = new JDialog(owner, "", true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        boolean[] result = {false};

        JPanel content = createRoundedPanel();
        content.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 24, 4, 24);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(ACCENT_COLOR);
        content.add(titleLabel, gbc);

        // Message
        gbc.gridy = 1;
        gbc.insets = new Insets(4, 24, 16, 24);
        JLabel msgLabel = new JLabel("<html><body style='width:260px'>" + message + "</body></html>");
        msgLabel.setFont(LABEL_FONT);
        msgLabel.setForeground(TEXT_COLOR);
        content.add(msgLabel, gbc);

        // Buttons
        gbc.gridy = 2;
        gbc.insets = new Insets(4, 24, 20, 24);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnPanel.setOpaque(false);

        StyledButton yesBtn = new StyledButton("YES", new Color(40, 120, 70));
        yesBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        yesBtn.setPreferredSize(new Dimension(100, 38));
        yesBtn.addActionListener(e -> {
            Sound.playClickSound();
            result[0] = true;
            dialog.dispose();
        });

        StyledButton noBtn = new StyledButton("NO", new Color(100, 60, 60));
        noBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        noBtn.setPreferredSize(new Dimension(100, 38));
        noBtn.addActionListener(e -> {
            Sound.playClickSound();
            dialog.dispose();
        });

        btnPanel.add(yesBtn);
        btnPanel.add(noBtn);
        content.add(btnPanel, gbc);

        // Escape = No
        content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "no");
        content.getActionMap().put("no", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dialog.dispose(); }
        });

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        return result[0];
    }

    /**
     * Shows a modern styled message dialog (info or warning).
     * @param parent parent component
     * @param message the message text
     * @param title the dialog title
     * @param isWarning true for warning style (orange accent), false for info (blue accent)
     */
    public static void showMessageDialog(Component parent, String message, String title, boolean isWarning) {
        JFrame owner = getFrame(parent);
        JDialog dialog = new JDialog(owner, "", true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel content = createRoundedPanel();
        content.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 24, 4, 24);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(isWarning ? new Color(220, 160, 50) : ACCENT_COLOR);
        content.add(titleLabel, gbc);

        // Message
        gbc.gridy = 1;
        gbc.insets = new Insets(4, 24, 16, 24);
        JLabel msgLabel = new JLabel("<html><body style='width:280px'>" + message + "</body></html>");
        msgLabel.setFont(LABEL_FONT);
        msgLabel.setForeground(TEXT_COLOR);
        content.add(msgLabel, gbc);

        // OK button
        gbc.gridy = 2;
        gbc.insets = new Insets(4, 24, 20, 24);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        StyledButton okBtn = new StyledButton("OK", new Color(50, 80, 130));
        okBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        okBtn.setPreferredSize(new Dimension(100, 38));
        okBtn.addActionListener(e -> {
            Sound.playClickSound();
            dialog.dispose();
        });
        content.add(okBtn, gbc);

        // Enter/Escape close
        content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "close");
        content.getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dialog.dispose(); }
        });

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /** Creates a rounded panel with dark background and subtle border */
    private static JPanel createRoundedPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Shadow
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fill(new RoundRectangle2D.Float(4, 4, getWidth() - 4, getHeight() - 4, 20, 20));

                // Background
                g2.setColor(BG_COLOR);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 4, getHeight() - 4, 20, 20));

                // Border
                g2.setColor(new Color(60, 70, 100));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 5, getHeight() - 5, 20, 20));

                g2.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    /** Styles a text field to match the dark theme */
    private static void styleField(JTextField tf) {
        tf.setFont(FIELD_FONT);
        tf.setBackground(FIELD_BG);
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BORDER, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    /** Gets the parent JFrame from a component */
    private static JFrame getFrame(Component c) {
        if (c instanceof JFrame) return (JFrame) c;
        if (c != null) return (JFrame) SwingUtilities.getWindowAncestor(c);
        return null;
    }
}
