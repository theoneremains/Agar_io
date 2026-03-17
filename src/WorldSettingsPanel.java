
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * WorldSettingsPanel : Pre-game settings screen where the player configures
 * their name, NPC count, world dimensions, cell density, and manages save files.
 * On opening, the most recent save (or autosave) is loaded automatically so the
 * player's last session settings are preserved.
 * @author Kamil Yunus Ozkaya
 */
@SuppressWarnings({"serial", "this-escape"})
public class WorldSettingsPanel extends JPanel {

    private final MenuBackground menuBg;
    private final GameSettings settings;
    private final MainClass mainClass;

    private JTextField tfPlayerName;
    private JTextField tfNpcCount;
    private JTextField tfWorldWidth;
    private JTextField tfWorldHeight;
    private JTextField tfCellDensity;

    /** Message shown when a save is auto-loaded on panel creation */
    private String autoLoadMessage = null;

    public WorldSettingsPanel(MainClass mainClass) {
        this.mainClass = mainClass;
        settings = new GameSettings();
        settings.readFromGame();

        // ── Auto-load last session settings ─────────────────────────────
        // Try autosave first; fall back to most-recently-modified user save.
        if (GameSettings.autosaveExists()) {
            settings.load(GameSettings.AUTOSAVE_NAME);
            autoLoadMessage = "Last session settings loaded automatically.";
        } else {
            String recent = GameSettings.getMostRecentSave();
            if (recent != null) {
                settings.load(recent);
                autoLoadMessage = "Loaded save: \"" + recent + "\"";
            }
        }

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
        int startY = centerY - 160;

        // --- Form fields ---
        addFieldRow("Player Name:", settings.playerName, leftX, rightX, startY, labelW, fieldW, fieldH, true);
        addFieldRow("NPC Count (min 3):", String.valueOf(settings.npcCount), leftX, rightX, startY + rowH, labelW, fieldW, fieldH, false);
        addFieldRow("World Width:", String.valueOf(settings.worldWidth), leftX, rightX, startY + rowH * 2, labelW, fieldW, fieldH, false);
        addFieldRow("World Height:", String.valueOf(settings.worldHeight), leftX, rightX, startY + rowH * 3, labelW, fieldW, fieldH, false);
        addFieldRow("Cell Density:", String.format("%.2f", settings.cellDensity), leftX, rightX, startY + rowH * 4, labelW, fieldW, fieldH, false);

        // Density hint
        JLabel densHint = new JLabel("cells/M px");
        densHint.setForeground(new Color(140, 160, 190));
        densHint.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 11));
        densHint.setBounds(rightX + fieldW + 6, startY + rowH * 4, 80, fieldH);
        add(densHint);

        // --- Buttons row ---
        int y = startY + rowH * 5 + 16;
        int btnW = 120;
        int btnH = GameConstants.BUTTON_HEIGHT;
        int btnGap = 10;
        int totalBtnWidth = btnW * 3 + btnGap * 2;
        int btnStartX = centerX - totalBtnWidth / 2;

        StyledButton startBtn = new StyledButton("START GAME", GameConstants.BTN_GREEN);
        startBtn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 14));
        startBtn.setBounds(btnStartX, y, btnW, btnH);
        add(startBtn);

        StyledButton advancedBtn = new StyledButton("ADVANCED", new Color(60, 60, 110));
        advancedBtn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 13));
        advancedBtn.setBounds(btnStartX + btnW + btnGap, y, btnW, btnH);
        add(advancedBtn);

        StyledButton backBtn = new StyledButton("BACK", new Color(100, 60, 60));
        backBtn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 14));
        backBtn.setBounds(btnStartX + (btnW + btnGap) * 2, y, btnW, btnH);
        add(backBtn);

        // --- Save/Load/Default row ---
        y += btnH + 16;
        int smallBtnW = 100;
        int smallBtnGap = 8;
        int totalSmallWidth = smallBtnW * 4 + smallBtnGap * 3;
        int smallStartX = centerX - totalSmallWidth / 2;

        StyledButton saveBtn = createSmallButton("SAVE", new Color(50, 90, 130));
        saveBtn.setBounds(smallStartX, y, smallBtnW, btnH - 6);
        add(saveBtn);

        StyledButton loadBtn = createSmallButton("LOAD", new Color(50, 90, 130));
        loadBtn.setBounds(smallStartX + smallBtnW + smallBtnGap, y, smallBtnW, btnH - 6);
        add(loadBtn);

        StyledButton renameBtn = createSmallButton("RENAME", new Color(80, 80, 50));
        renameBtn.setBounds(smallStartX + (smallBtnW + smallBtnGap) * 2, y, smallBtnW, btnH - 6);
        add(renameBtn);

        StyledButton defaultBtn = createSmallButton("DEFAULT", new Color(120, 80, 40));
        defaultBtn.setBounds(smallStartX + (smallBtnW + smallBtnGap) * 3, y, smallBtnW, btnH - 6);
        add(defaultBtn);

        // --- Background animation ---
        menuBg = new MenuBackground(6, this);
        menuBg.start();

        // --- Action listeners ---
        startBtn.addActionListener(e -> {
            Sound.playClickSound();
            if (!readFieldsIntoSettings()) return;
            // Auto-save settings for next session before launching game
            settings.save(GameSettings.AUTOSAVE_NAME);
            settings.applyToGame();
            menuBg.stop();
            mainClass.gamePanel = new GamePanel(mainClass, settings.npcCount);
            mainClass.getContentPane().removeAll();
            mainClass.getContentPane().add(mainClass.gamePanel);
            mainClass.revalidate();
            mainClass.repaint();
            mainClass.gamePanel.requestFocusInWindow();
        });

        advancedBtn.addActionListener(e -> {
            Sound.playClickSound();
            showAdvancedSettingsDialog();
        });

        backBtn.addActionListener(e -> {
            Sound.playClickSound();
            menuBg.stop();
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
                String choice = showSavePickerDialog(mainClass, "Overwrite a save:", saves.toArray(new String[0]));
                if (choice != null) {
                    if (!readFieldsIntoSettings()) return;
                    settings.save(choice);
                    StyledDialog.showMessageDialog(mainClass, "Settings saved to '" + choice + "'.", "Saved", false);
                }
                return;
            }
            String name = StyledDialog.showInputDialog(mainClass, "Save name:", "Save " + (saves.size() + 1));
            if (name == null || name.trim().isEmpty()) return;
            name = name.trim();
            if (!readFieldsIntoSettings()) return;
            settings.save(name);
            StyledDialog.showMessageDialog(mainClass, "Settings saved to '" + name + "'.", "Saved", false);
        });

        loadBtn.addActionListener(e -> {
            Sound.playClickSound();
            List<String> saves = GameSettings.listSaves();
            if (saves.isEmpty()) {
                StyledDialog.showMessageDialog(mainClass, "No save files found.", "Load", true);
                return;
            }
            String choice = showSavePickerDialog(mainClass, "Select a save to load:", saves.toArray(new String[0]));
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
            String choice = showSavePickerDialog(mainClass, "Select a save to rename:", saves.toArray(new String[0]));
            if (choice != null) {
                String newName = StyledDialog.showInputDialog(mainClass, "New name for '" + choice + "':", choice);
                if (newName == null || newName.trim().isEmpty()) return;
                newName = newName.trim();
                if (GameSettings.renameSave(choice, newName)) {
                    StyledDialog.showMessageDialog(mainClass, "Renamed '" + choice + "' to '" + newName + "'.", "Renamed", false);
                } else {
                    StyledDialog.showMessageDialog(mainClass, "Could not rename. A save with that name may already exist.", "Error", true);
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

    // ── Advanced Settings Dialog ──────────────────────────────────────────

    /**
     * Opens a modal advanced-settings dialog where the player can configure
     * per-difficulty NPC counts and the shave (cell division) rate multiplier.
     */
    private void showAdvancedSettingsDialog() {
        JDialog dialog = new JDialog(mainClass, "", true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(4, 4, getWidth() - 4, getHeight() - 4, 20, 20));
                g2.setColor(new Color(25, 30, 45));
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
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(18, 24, 8, 24);
        gbc.anchor = GridBagConstraints.WEST;

        Font titleFont  = new Font(GameConstants.FONT_FAMILY, Font.BOLD, 16);
        Font labelFont  = new Font(GameConstants.FONT_FAMILY, Font.BOLD, 13);
        Font fieldFont  = new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 13);
        Color labelColor = new Color(180, 210, 255);

        JLabel titleLbl = new JLabel("Advanced Settings");
        titleLbl.setFont(titleFont);
        titleLbl.setForeground(Color.YELLOW);
        content.add(titleLbl, gbc);

        // Subtitle
        gbc.gridy = 1; gbc.insets = new Insets(0, 24, 14, 24);
        JLabel subLbl = new JLabel("<html><font color='#9090C0' size='3'>Configure NPC difficulty distribution and<br>cell erosion speed. Leave counts at 0 to<br>use round-robin based on total NPC count.</font></html>");
        subLbl.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 11));
        content.add(subLbl, gbc);

        // ---- Field rows ----
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

        JTextField tfEasy   = addAdvRow(content, gbc, 2, "Easy NPCs:",   String.valueOf(settings.npcEasyCount),   labelFont, fieldFont, labelColor);
        JTextField tfMedium = addAdvRow(content, gbc, 3, "Medium NPCs:", String.valueOf(settings.npcMediumCount), labelFont, fieldFont, labelColor);
        JTextField tfHard   = addAdvRow(content, gbc, 4, "Hard NPCs:",   String.valueOf(settings.npcHardCount),   labelFont, fieldFont, labelColor);
        JTextField tfShave  = addAdvRow(content, gbc, 5, "Division Rate (multiplier):", String.format("%.2f", settings.shaveRateMultiplier), labelFont, fieldFont, labelColor);

        // Hint under shave rate
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 24, 12, 24);
        JLabel shaveHint = new JLabel("<html><font color='#7080A0' size='3'>1.0 = default erosion speed. Higher = faster division.</font></html>");
        shaveHint.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 10));
        content.add(shaveHint, gbc);

        // ---- Buttons ----
        gbc.gridy = 7; gbc.gridwidth = 2; gbc.insets = new Insets(8, 24, 20, 24);
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnPanel.setOpaque(false);

        StyledButton applyBtn = new StyledButton("APPLY", GameConstants.BTN_GREEN);
        applyBtn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 13));
        applyBtn.setPreferredSize(new Dimension(110, 36));
        btnPanel.add(applyBtn);

        StyledButton cancelBtn2 = new StyledButton("CANCEL", new Color(100, 60, 60));
        cancelBtn2.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 13));
        cancelBtn2.setPreferredSize(new Dimension(110, 36));
        btnPanel.add(cancelBtn2);

        content.add(btnPanel, gbc);

        applyBtn.addActionListener(ev -> {
            try {
                int easy   = Math.max(0, Integer.parseInt(tfEasy.getText().trim()));
                int medium = Math.max(0, Integer.parseInt(tfMedium.getText().trim()));
                int hard   = Math.max(0, Integer.parseInt(tfHard.getText().trim()));
                double shave = Math.max(0.1, Double.parseDouble(tfShave.getText().trim()));
                settings.npcEasyCount   = easy;
                settings.npcMediumCount = medium;
                settings.npcHardCount   = hard;
                settings.shaveRateMultiplier = shave;
                // Update total NPC count field if distribution sums > 0
                int total = easy + medium + hard;
                if (total > 0) {
                    settings.npcCount = Math.max(GameConstants.MIN_NPC_COUNT, total);
                    tfNpcCount.setText(String.valueOf(settings.npcCount));
                }
                dialog.dispose();
            } catch (NumberFormatException ex) {
                StyledDialog.showMessageDialog(mainClass,
                    "Please enter valid numbers for all fields.", "Invalid Input", true);
            }
        });
        cancelBtn2.addActionListener(ev -> dialog.dispose());

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(mainClass);
        dialog.setVisible(true);
    }

    /** Adds a label + text-field row to the advanced dialog, returns the text field. */
    private JTextField addAdvRow(JPanel panel, GridBagConstraints gbc, int row,
                                 String label, String value,
                                 Font labelFont, Font fieldFont, Color labelColor) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 24, 5, 8);
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        JLabel lbl = new JLabel(label);
        lbl.setFont(labelFont);
        lbl.setForeground(labelColor);
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 4, 5, 24);
        JTextField tf = createField(value);
        tf.setFont(fieldFont);
        panel.add(tf, gbc);
        return tf;
    }

    // ── Field Management ─────────────────────────────────────────────────

    private void addFieldRow(String labelText, String value, int leftX, int rightX, int y,
                             int labelW, int fieldW, int fieldH, boolean isFirst) {
        JLabel label = createLabel(labelText);
        label.setBounds(leftX, y, labelW, fieldH);
        add(label);

        JTextField tf = createField(value);
        tf.setBounds(rightX, y, fieldW, fieldH);
        add(tf);

        // Store references to fields for later access
        if (isFirst) {
            tfPlayerName = tf;
        } else if (tfNpcCount == null) {
            tfNpcCount = tf;
        } else if (tfWorldWidth == null) {
            tfWorldWidth = tf;
        } else if (tfWorldHeight == null) {
            tfWorldHeight = tf;
        } else if (tfCellDensity == null) {
            tfCellDensity = tf;
        }
    }

    private boolean readFieldsIntoSettings() {
        String name = tfPlayerName.getText().trim();
        if (name.isEmpty()) name = GameSettings.DEFAULT_PLAYER_NAME;
        settings.playerName = name;

        try {
            settings.npcCount = Math.max(GameConstants.MIN_NPC_COUNT, Integer.parseInt(tfNpcCount.getText().trim()));
        } catch (NumberFormatException ex) {
            StyledDialog.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                "Please enter a valid number for NPC count.", "Invalid Input", true);
            return false;
        }

        try {
            int w = Integer.parseInt(tfWorldWidth.getText().trim());
            int h = Integer.parseInt(tfWorldHeight.getText().trim());
            if (w < GameConstants.MIN_WORLD_WIDTH || h < GameConstants.MIN_WORLD_HEIGHT) {
                StyledDialog.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                    "Minimum world size is " + GameConstants.MIN_WORLD_WIDTH + " x " + GameConstants.MIN_WORLD_HEIGHT + ".",
                    "Invalid Size", true);
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

    private void populateFields() {
        tfPlayerName.setText(settings.playerName);
        tfNpcCount.setText(String.valueOf(settings.npcCount));
        tfWorldWidth.setText(String.valueOf(settings.worldWidth));
        tfWorldHeight.setText(String.valueOf(settings.worldHeight));
        tfCellDensity.setText(String.format("%.2f", settings.cellDensity));
    }

    // ── Save Picker Dialog ───────────────────────────────────────────────

    private String showSavePickerDialog(MainClass mainClass, String message, String[] options) {
        if (options.length == 0) return null;

        JDialog dialog = new JDialog(mainClass, "", true);
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

        gbc.gridy = 0;
        gbc.insets = new Insets(20, 24, 12, 24);
        JLabel label = new JLabel(message);
        label.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 16));
        label.setForeground(new Color(220, 230, 255));
        content.add(label, gbc);

        for (int i = 0; i < options.length; i++) {
            gbc.gridy = i + 1;
            gbc.insets = new Insets(4, 24, 4, 24);
            String opt = options[i];
            StyledButton btn = new StyledButton(opt, new Color(50, 80, 130));
            btn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 14));
            btn.setPreferredSize(new Dimension(220, 38));
            btn.addActionListener(ev -> {
                Sound.playClickSound();
                result[0] = opt;
                dialog.dispose();
            });
            content.add(btn, gbc);
        }

        gbc.gridy = options.length + 1;
        gbc.insets = new Insets(12, 24, 20, 24);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        StyledButton cancelBtn = new StyledButton("CANCEL", new Color(100, 60, 60));
        cancelBtn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 14));
        cancelBtn.setPreferredSize(new Dimension(120, 38));
        cancelBtn.addActionListener(ev -> {
            Sound.playClickSound();
            dialog.dispose();
        });
        content.add(cancelBtn, gbc);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(mainClass);
        dialog.setVisible(true);

        return result[0];
    }

    // ── UI Helpers ───────────────────────────────────────────────────────

    private StyledButton createSmallButton(String text, Color color) {
        StyledButton btn = new StyledButton(text, color);
        btn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 12));
        return btn;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(180, 200, 240));
        label.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 14));
        return label;
    }

    private JTextField createField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 13));
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
        menuBg.draw((Graphics2D) g, MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        MenuBackground.drawHighScore(g2d, GamePanel.highscore);
        MenuBackground.drawTitle(g2d, "World Settings", 48,
            MainClass.SCREEN_HEIGHT / 2 - 220, MainClass.SCREEN_WIDTH, false);

        // Auto-load notification (subtle, fades in below title)
        if (autoLoadMessage != null) {
            g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.ITALIC, 13));
            g2d.setColor(new Color(140, 200, 140, 210));
            FontMetrics fm = g2d.getFontMetrics();
            int msgX = (MainClass.SCREEN_WIDTH - fm.stringWidth(autoLoadMessage)) / 2;
            g2d.drawString(autoLoadMessage, msgX, MainClass.SCREEN_HEIGHT / 2 - 185);
        }
    }
}
