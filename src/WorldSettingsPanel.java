
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * WorldSettingsPanel : Pre-game settings screen where the player configures
 * their name, NPC count, world dimensions, cell density, and manages save files.
 * Replaces the old separate input dialogs for name and NPC count.
 * @author Kamil Yunus Ozkaya
 */
public class WorldSettingsPanel extends JPanel {

    /** Phase for animated background gradient */
    private float bgPhase = 0f;
    private Timer bgTimer;

    private JTextField tfPlayerName;
    private JTextField tfNpcCount;
    private JTextField tfWorldWidth;
    private JTextField tfWorldHeight;
    private JTextField tfCellDensity;

    /** The settings object backing this panel */
    private GameSettings settings;

    public WorldSettingsPanel(MainClass mainClass) {
        settings = new GameSettings();
        settings.readFromGame();

        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
        setPreferredSize(new Dimension(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT));
        setFocusable(true);
        setLayout(null);

        int centerX = MainClass.SCREEN_WIDTH / 2;
        int centerY = MainClass.SCREEN_HEIGHT / 2;
        int fieldW = 160;
        int fieldH = 28;
        int labelW = 140;
        int rowH = 38;
        int leftX = centerX - 160;
        int rightX = centerX + 10;

        // Starting Y position for the form (offset upward to fit everything)
        int startY = centerY - 160;

        // --- Player Name ---
        JLabel nameLabel = createLabel("Player Name:");
        nameLabel.setBounds(leftX, startY, labelW, fieldH);
        tfPlayerName = createField(settings.playerName);
        tfPlayerName.setBounds(rightX, startY, fieldW, fieldH);
        add(nameLabel);
        add(tfPlayerName);

        // --- NPC Count ---
        int y = startY + rowH;
        JLabel npcLabel = createLabel("NPC Count (min 3):");
        npcLabel.setBounds(leftX, y, labelW, fieldH);
        tfNpcCount = createField(String.valueOf(settings.npcCount));
        tfNpcCount.setBounds(rightX, y, fieldW, fieldH);
        add(npcLabel);
        add(tfNpcCount);

        // --- World Width ---
        y += rowH;
        JLabel wwLabel = createLabel("World Width:");
        wwLabel.setBounds(leftX, y, labelW, fieldH);
        tfWorldWidth = createField(String.valueOf(settings.worldWidth));
        tfWorldWidth.setBounds(rightX, y, fieldW, fieldH);
        add(wwLabel);
        add(tfWorldWidth);

        // --- World Height ---
        y += rowH;
        JLabel whLabel = createLabel("World Height:");
        whLabel.setBounds(leftX, y, labelW, fieldH);
        tfWorldHeight = createField(String.valueOf(settings.worldHeight));
        tfWorldHeight.setBounds(rightX, y, fieldW, fieldH);
        add(whLabel);
        add(tfWorldHeight);

        // --- Cell Density ---
        y += rowH;
        JLabel densLabel = createLabel("Cell Density:");
        densLabel.setBounds(leftX, y, labelW, fieldH);
        tfCellDensity = createField(String.format("%.2f", settings.cellDensity));
        tfCellDensity.setBounds(rightX, y, fieldW, fieldH);
        add(densLabel);
        add(tfCellDensity);

        JLabel densHint = new JLabel("cells/M px");
        densHint.setForeground(new Color(140, 160, 190));
        densHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        densHint.setBounds(rightX + fieldW + 6, y, 80, fieldH);
        add(densHint);

        // --- Buttons row ---
        y += rowH + 16;
        int btnW = 130;
        int btnH = MainClass.BUTTON_HEIGHT;
        int btnGap = 10;
        int totalBtnWidth = btnW * 2 + btnGap;
        int btnStartX = centerX - totalBtnWidth / 2;

        StyledButton startBtn = new StyledButton("START GAME", new Color(40, 140, 70));
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        startBtn.setBounds(btnStartX, y, btnW, btnH);
        add(startBtn);

        StyledButton backBtn = new StyledButton("BACK", new Color(100, 60, 60));
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        backBtn.setBounds(btnStartX + btnW + btnGap, y, btnW, btnH);
        add(backBtn);

        // --- Save/Load/Default row ---
        y += btnH + 16;
        int smallBtnW = 100;
        int smallBtnGap = 8;
        int totalSmallWidth = smallBtnW * 4 + smallBtnGap * 3;
        int smallStartX = centerX - totalSmallWidth / 2;

        StyledButton saveBtn = new StyledButton("SAVE", new Color(50, 90, 130));
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        saveBtn.setBounds(smallStartX, y, smallBtnW, btnH - 6);
        add(saveBtn);

        StyledButton loadBtn = new StyledButton("LOAD", new Color(50, 90, 130));
        loadBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        loadBtn.setBounds(smallStartX + smallBtnW + smallBtnGap, y, smallBtnW, btnH - 6);
        add(loadBtn);

        StyledButton renameBtn = new StyledButton("RENAME", new Color(80, 80, 50));
        renameBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        renameBtn.setBounds(smallStartX + (smallBtnW + smallBtnGap) * 2, y, smallBtnW, btnH - 6);
        add(renameBtn);

        StyledButton defaultBtn = new StyledButton("DEFAULT", new Color(120, 80, 40));
        defaultBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        defaultBtn.setBounds(smallStartX + (smallBtnW + smallBtnGap) * 3, y, smallBtnW, btnH - 6);
        add(defaultBtn);

        // --- Background animation ---
        bgTimer = new Timer(30, e -> {
            bgPhase = (bgPhase + 0.003f) % 1f;
            repaint();
        });
        bgTimer.start();

        // --- Action listeners ---

        startBtn.addActionListener(e -> {
            Sound.playClickSound();
            if (!readFieldsIntoSettings()) return;
            settings.applyToGame();
            stopBgTimer();
            mainClass.gamePanel = new GamePanel(mainClass, settings.npcCount);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.gamePanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.gamePanel.requestFocusInWindow();
        });

        backBtn.addActionListener(e -> {
            Sound.playClickSound();
            stopBgTimer();
            mainClass.mainPanel = new MainPanel(mainClass);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.mainPanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.mainPanel.requestFocusInWindow();
        });

        saveBtn.addActionListener(e -> {
            Sound.playClickSound();
            List<String> saves = GameSettings.listSaves();
            if (saves.size() >= GameSettings.MAX_SAVES) {
                StyledDialog.showMessageDialog(mainClass,
                    "Maximum " + GameSettings.MAX_SAVES + " save files allowed.<br>Delete or overwrite an existing save.",
                    "Save Limit Reached", true);
                // Offer to overwrite
                String[] options = saves.toArray(new String[0]);
                String choice = showSavePickerDialog(mainClass, "Overwrite a save:", options);
                if (choice != null) {
                    if (!readFieldsIntoSettings()) return;
                    settings.save(choice);
                    StyledDialog.showMessageDialog(mainClass,
                        "Settings saved to '" + choice + "'.", "Saved", false);
                }
                return;
            }
            String name = StyledDialog.showInputDialog(mainClass, "Save name:", "Save " + (saves.size() + 1));
            if (name == null || name.trim().isEmpty()) return;
            name = name.trim();
            if (!readFieldsIntoSettings()) return;
            settings.save(name);
            StyledDialog.showMessageDialog(mainClass,
                "Settings saved to '" + name + "'.", "Saved", false);
        });

        loadBtn.addActionListener(e -> {
            Sound.playClickSound();
            List<String> saves = GameSettings.listSaves();
            if (saves.isEmpty()) {
                StyledDialog.showMessageDialog(mainClass, "No save files found.", "Load", true);
                return;
            }
            String[] options = saves.toArray(new String[0]);
            String choice = showSavePickerDialog(mainClass, "Select a save to load:", options);
            if (choice != null) {
                settings.load(choice);
                populateFields();
            }
        });

        renameBtn.addActionListener(e -> {
            Sound.playClickSound();
            List<String> saves = GameSettings.listSaves();
            if (saves.isEmpty()) {
                StyledDialog.showMessageDialog(mainClass, "No save files found.", "Rename", true);
                return;
            }
            String[] options = saves.toArray(new String[0]);
            String choice = showSavePickerDialog(mainClass, "Select a save to rename:", options);
            if (choice != null) {
                String newName = StyledDialog.showInputDialog(mainClass, "New name for '" + choice + "':", choice);
                if (newName == null || newName.trim().isEmpty()) return;
                newName = newName.trim();
                if (GameSettings.renameSave(choice, newName)) {
                    StyledDialog.showMessageDialog(mainClass,
                        "Renamed '" + choice + "' to '" + newName + "'.", "Renamed", false);
                } else {
                    StyledDialog.showMessageDialog(mainClass,
                        "Could not rename. A save with that name may already exist.", "Error", true);
                }
            }
        });

        defaultBtn.addActionListener(e -> {
            Sound.playClickSound();
            boolean confirmed = StyledDialog.showConfirmDialog(mainClass,
                "Restore all settings to default values?", "Restore Defaults");
            if (confirmed) {
                settings.restoreDefaults();
                populateFields();
            }
        });
    }

    /** Reads the text fields into the settings object. Returns false if validation fails. */
    private boolean readFieldsIntoSettings() {
        String name = tfPlayerName.getText().trim();
        if (name.isEmpty()) name = GameSettings.DEFAULT_PLAYER_NAME;
        settings.playerName = name;

        try {
            settings.npcCount = Math.max(3, Integer.parseInt(tfNpcCount.getText().trim()));
        } catch (NumberFormatException ex) {
            StyledDialog.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                "Please enter a valid number for NPC count.", "Invalid Input", true);
            return false;
        }

        try {
            int w = Integer.parseInt(tfWorldWidth.getText().trim());
            int h = Integer.parseInt(tfWorldHeight.getText().trim());
            if (w < 800 || h < 600) {
                StyledDialog.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                    "Minimum world size is 800 x 600.", "Invalid Size", true);
                return false;
            }
            settings.worldWidth = w;
            settings.worldHeight = h;
        } catch (NumberFormatException ex) {
            StyledDialog.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                "Please enter valid numbers for world size.", "Invalid Input", true);
            return false;
        }

        try {
            double d = Double.parseDouble(tfCellDensity.getText().trim());
            if (d <= 0) {
                StyledDialog.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                    "Cell density must be a positive number.", "Invalid Density", true);
                return false;
            }
            settings.cellDensity = d;
        } catch (NumberFormatException ex) {
            StyledDialog.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                "Please enter a valid number for cell density.", "Invalid Input", true);
            return false;
        }

        return true;
    }

    /** Populates the text fields from the current settings object */
    private void populateFields() {
        tfPlayerName.setText(settings.playerName);
        tfNpcCount.setText(String.valueOf(settings.npcCount));
        tfWorldWidth.setText(String.valueOf(settings.worldWidth));
        tfWorldHeight.setText(String.valueOf(settings.worldHeight));
        tfCellDensity.setText(String.format("%.2f", settings.cellDensity));
    }

    /** Shows a picker dialog for selecting a save file. Returns the chosen name or null. */
    private String showSavePickerDialog(MainClass mainClass, String message, String[] options) {
        if (options.length == 0) return null;

        JFrame owner = mainClass;
        JDialog dialog = new JDialog(owner, "", true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        String[] result = {null};

        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(4, 4, getWidth() - 4, getHeight() - 4, 20, 20));
                g2.setColor(new Color(30, 35, 50));
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth() - 4, getHeight() - 4, 20, 20));
                g2.setColor(new Color(60, 70, 100));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth() - 5, getHeight() - 5, 20, 20));
                g2.dispose();
            }
        };
        content.setOpaque(false);
        content.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 24, 6, 24);

        // Message label
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 24, 12, 24);
        JLabel label = new JLabel(message);
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        label.setForeground(new Color(220, 230, 255));
        content.add(label, gbc);

        // One button per save
        for (int i = 0; i < options.length; i++) {
            gbc.gridy = i + 1;
            gbc.insets = new Insets(4, 24, 4, 24);
            String opt = options[i];
            StyledButton btn = new StyledButton(opt, new Color(50, 80, 130));
            btn.setFont(new Font("SansSerif", Font.BOLD, 14));
            btn.setPreferredSize(new Dimension(220, 38));
            btn.addActionListener(ev -> {
                Sound.playClickSound();
                result[0] = opt;
                dialog.dispose();
            });
            content.add(btn, gbc);
        }

        // Cancel button
        gbc.gridy = options.length + 1;
        gbc.insets = new Insets(12, 24, 20, 24);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        StyledButton cancelBtn = new StyledButton("CANCEL", new Color(100, 60, 60));
        cancelBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        cancelBtn.setPreferredSize(new Dimension(120, 38));
        cancelBtn.addActionListener(ev -> {
            Sound.playClickSound();
            dialog.dispose();
        });
        content.add(cancelBtn, gbc);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        return result[0];
    }

    private void stopBgTimer() {
        if (bgTimer != null) bgTimer.stop();
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(180, 200, 240));
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        return label;
    }

    private JTextField createField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setBackground(new Color(40, 45, 60));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 80, 110)),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        return tf;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = MainClass.SCREEN_WIDTH;
        int h = MainClass.SCREEN_HEIGHT;

        // Animated gradient background
        Color c1 = Color.getHSBColor(bgPhase, 0.35f, 0.18f);
        Color c2 = Color.getHSBColor((bgPhase + 0.25f) % 1f, 0.30f, 0.12f);
        g2d.setPaint(new GradientPaint(0, 0, c1, w, h, c2));
        g2d.fillRect(0, 0, w, h);

        // Subtle floating circles
        Composite orig = g2d.getComposite();
        for (int i = 0; i < 6; i++) {
            float hue = (bgPhase + i * 0.15f) % 1f;
            Color circleColor = Color.getHSBColor(hue, 0.3f, 0.22f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.05f));
            g2d.setColor(circleColor);
            int cx = (int) (w * 0.2 + (w * 0.6) * Math.sin(bgPhase * Math.PI + i * 1.0));
            int cy = (int) (h * 0.2 + (h * 0.6) * Math.cos(bgPhase * Math.PI * 0.6 + i * 1.3));
            int sz = 80 + i * 40;
            g2d.fillOval(cx - sz, cy - sz, sz * 2, sz * 2);
        }
        g2d.setComposite(orig);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // High score
        g2d.setColor(new Color(180, 200, 255, 180));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2d.drawString("High Score: " + GamePanel.highscore, 15, 25);

        // Title
        String title = "World Settings";
        g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = (MainClass.SCREEN_WIDTH - fm.stringWidth(title)) / 2;
        g2d.setColor(new Color(220, 240, 255));
        g2d.drawString(title, tx, MainClass.SCREEN_HEIGHT / 2 - 220);
    }
}
