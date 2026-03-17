
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

/**
 * DevLogDialog : Developer log / cheat window for the Agar.io game.
 * Opened and closed via Ctrl+I (Windows/Linux) or Cmd+I (macOS).
 * While open the game loop is paused.
 *
 * Displays every editable game mechanic in one place:
 *   - Player identity (name, radius, score, position)
 *   - Movement (speed X/Y with manual override toggle)
 *   - Roguelite state (magnet radius, regen level, split shield factor, speed bonus)
 *   - World settings (cell density, max food cells)
 *   - Shave / erosion rate (global multiplier)
 *   - Live counts (NPC alive, food cells)
 *   - Upgrade history (read-only)
 *
 * @author Kamil Yunus Özkaya
 */
public class DevLogDialog extends JDialog {

    private final GamePanel gamePanel;

    // Input fields — player
    private JTextField tfName;
    private JTextField tfRadius;
    private JTextField tfScore;
    private JTextField tfSpeedX;
    private JTextField tfSpeedY;
    private JCheckBox  cbSpeedOverride;
    private JTextField tfPosX;
    private JTextField tfPosY;

    // Input fields — roguelite mechanics
    private JTextField tfMagnetRadius;
    private JTextField tfRegenLevel;
    private JLabel     lblShieldFactor;
    private JLabel     lblSpeedBonus;

    // Input fields — world
    private JTextField tfCellDensity;
    private JTextField tfShaveRate;

    // Read-only labels
    private JLabel     lblNpcAlive;
    private JLabel     lblFoodCount;
    private JLabel     lblMaxCells;
    private JLabel     lblUpgrades;

    /**
     * Constructs the developer log dialog.
     * @param owner    The parent JFrame (MainClass).
     * @param gamePanel Reference to the running GamePanel for reading/writing state.
     */
    public DevLogDialog(JFrame owner, GamePanel gamePanel) {
        super(owner, "Dev Log  [Ctrl+I / Cmd+I to close]", false); // non-modal
        this.gamePanel = gamePanel;

        setSize(460, 650);
        setLocationRelativeTo(owner);
        setResizable(false);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        // Resume game when the user closes with the X button
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                applyAndClose();
            }
        });

        buildUI();
        populateFields();

        // Ctrl+I / Cmd+I also closes the dialog
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "closeDevLog");
        getRootPane().getActionMap().put("closeDevLog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyAndClose();
            }
        });
    }

    /** Builds the dialog layout with labeled rows of JTextFields. */
    private void buildUI() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(14, 18, 10, 18));
        content.setBackground(new Color(30, 30, 40));

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(4, 4, 4, 10);
        lc.gridx = 0;

        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(4, 0, 4, 4);
        fc.gridx = 1;

        Color labelColor = new Color(180, 220, 255);
        Color mechColor  = new Color(180, 255, 200);
        Color worldColor = new Color(255, 220, 160);
        Color readColor  = new Color(120, 255, 120);
        Color fieldBg    = new Color(50, 50, 65);
        Color fieldFg    = Color.WHITE;
        Font  labelFont  = new Font("Arial", Font.BOLD, 13);
        Font  fieldFont  = new Font("Arial", Font.PLAIN, 13);
        Font  secFont    = new Font("Arial", Font.BOLD, 12);

        int row = 0;

        // ── Section: Title ───────────────────────────────────────────────
        row = addSectionTitle(content, "  Developer Log \u2014 Editable World State",
            Color.YELLOW, new Font("Arial", Font.BOLD, 14), row);

        // ── Section: Player ──────────────────────────────────────────────
        row = addSectionHeader(content, "[ PLAYER ]", secFont, labelColor, row);

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Player Name:", labelFont, labelColor), lc);
        tfName = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfName, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Player Radius:", labelFont, labelColor), lc);
        tfRadius = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfRadius, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Score:", labelFont, labelColor), lc);
        tfScore = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfScore, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Speed X:", labelFont, labelColor), lc);
        tfSpeedX = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfSpeedX, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Speed Y:", labelFont, labelColor), lc);
        tfSpeedY = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfSpeedY, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Manual Speed Override:", labelFont, labelColor), lc);
        cbSpeedOverride = new JCheckBox();
        cbSpeedOverride.setBackground(new Color(30, 30, 40));
        cbSpeedOverride.setForeground(labelColor);
        cbSpeedOverride.setFont(labelFont);
        cbSpeedOverride.setToolTipText("When checked, dynamic speed recalculation is skipped");
        content.add(cbSpeedOverride, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Position X (world):", labelFont, labelColor), lc);
        tfPosX = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfPosX, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Position Y (world):", labelFont, labelColor), lc);
        tfPosY = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfPosY, fc);
        row++;

        // ── Section: Roguelite Mechanics ─────────────────────────────────
        row = addSectionHeader(content, "[ ROGUELITE MECHANICS ]", secFont, mechColor, row);

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Magnet Radius (world px):", labelFont, mechColor), lc);
        tfMagnetRadius = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfMagnetRadius, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Regen Level:", labelFont, mechColor), lc);
        tfRegenLevel = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfRegenLevel, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Split Shield Factor:", labelFont, mechColor), lc);
        lblShieldFactor = new JLabel();
        lblShieldFactor.setForeground(readColor);
        lblShieldFactor.setFont(fieldFont);
        content.add(lblShieldFactor, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Speed Bonus:", labelFont, mechColor), lc);
        lblSpeedBonus = new JLabel();
        lblSpeedBonus.setForeground(readColor);
        lblSpeedBonus.setFont(fieldFont);
        content.add(lblSpeedBonus, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Upgrades Taken:", labelFont, mechColor), lc);
        lblUpgrades = new JLabel();
        lblUpgrades.setForeground(readColor);
        lblUpgrades.setFont(new Font("Arial", Font.PLAIN, 11));
        content.add(lblUpgrades, fc);
        row++;

        // ── Section: World ───────────────────────────────────────────────
        row = addSectionHeader(content, "[ WORLD ]", secFont, worldColor, row);

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Cell Density (cells/M px):", labelFont, worldColor), lc);
        tfCellDensity = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfCellDensity, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Division Rate Multiplier:", labelFont, worldColor), lc);
        tfShaveRate = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfShaveRate, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Max Food Cells:", labelFont, worldColor), lc);
        lblMaxCells = new JLabel();
        lblMaxCells.setForeground(readColor);
        lblMaxCells.setFont(fieldFont);
        content.add(lblMaxCells, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("NPC Alive:", labelFont, worldColor), lc);
        lblNpcAlive = new JLabel();
        lblNpcAlive.setForeground(readColor);
        lblNpcAlive.setFont(fieldFont);
        content.add(lblNpcAlive, fc);
        row++;

        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Food Cells:", labelFont, worldColor), lc);
        lblFoodCount = new JLabel();
        lblFoodCount.setForeground(readColor);
        lblFoodCount.setFont(fieldFont);
        content.add(lblFoodCount, fc);
        row++;

        // ── Buttons row ──────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(new Color(30, 30, 40));

        JButton btnRefresh = makeButton("Refresh", new Color(60, 80, 120));
        btnRefresh.addActionListener(e -> populateFields());

        JButton btnApply = makeButton("Apply & Resume", new Color(40, 120, 60));
        btnApply.addActionListener(e -> applyAndClose());

        btnRow.add(btnRefresh);
        btnRow.add(btnApply);

        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 0; bc.gridy = row; bc.gridwidth = 2;
        bc.anchor = GridBagConstraints.EAST;
        bc.insets = new Insets(12, 0, 0, 0);
        content.add(btnRow, bc);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBackground(new Color(30, 30, 40));
        scroll.getViewport().setBackground(new Color(30, 30, 40));
        scroll.setBorder(null);
        setContentPane(scroll);
    }

    /** Populates all fields from the current game state. */
    private void populateFields() {
        Cell p = gamePanel.getPlayerCell();
        tfName.setText(GamePanel.playerName);
        tfRadius.setText(String.format("%.2f", p.cellRad));
        tfScore.setText(String.valueOf(gamePanel.getHUD().score));
        tfSpeedX.setText(String.format("%.2f", p.speedX));
        tfSpeedY.setText(String.format("%.2f", p.speedY));
        cbSpeedOverride.setSelected(gamePanel.devSpeedOverride);
        tfPosX.setText(String.format("%.0f", p.x + p.cellRad));
        tfPosY.setText(String.format("%.0f", p.y + p.cellRad));

        // Roguelite
        tfMagnetRadius.setText(String.format("%.1f", gamePanel.magnetRadius));
        tfRegenLevel.setText(String.valueOf(gamePanel.regenLevel));
        lblShieldFactor.setText(String.format("%.3f", gamePanel.splitShieldFactor));
        lblSpeedBonus.setText(String.format("+%.2f", gamePanel.playerSpeedBonus));

        // Upgrade history (compact)
        Map<UpgradeType, Integer> upgCounts = gamePanel.getUpgradeManager().getAppliedCounts();
        if (upgCounts.isEmpty()) {
            lblUpgrades.setText("none");
        } else {
            StringBuilder sb = new StringBuilder("<html>");
            for (Map.Entry<UpgradeType, Integer> e : upgCounts.entrySet()) {
                sb.append(e.getKey().displayName);
                if (e.getValue() > 1) sb.append(" x").append(e.getValue());
                sb.append("&nbsp; ");
            }
            sb.append("</html>");
            lblUpgrades.setText(sb.toString());
        }

        // World
        tfCellDensity.setText(String.format("%.2f", GamePanel.cellDensity));
        tfShaveRate.setText(String.format("%.2f", GamePanel.shaveRateMultiplier));
        lblMaxCells.setText(String.valueOf(gamePanel.getMaxCells()));

        // Live counts
        long npcAlive = gamePanel.getNPCList().stream().filter(n -> n.alive).count();
        lblNpcAlive.setText(String.valueOf(npcAlive) + " / " + gamePanel.getNPCList().size());
        lblFoodCount.setText(String.valueOf(gamePanel.getFoodCells().size()));
    }

    /**
     * Reads edited values, applies them to the game state, then closes the dialog
     * and unpauses the game. Invalid numeric entries are silently ignored.
     */
    private void applyAndClose() {
        Cell p = gamePanel.getPlayerCell();

        // Player name
        String name = tfName.getText().trim();
        if (!name.isEmpty()) GamePanel.playerName = name;

        // Player radius
        try {
            double r = Double.parseDouble(tfRadius.getText().trim());
            if (r > 0) p.cellRad = r;
        } catch (NumberFormatException ignored) {}

        // Score
        try {
            int s = Integer.parseInt(tfScore.getText().trim());
            if (s >= 0) {
                gamePanel.getHUD().score = s;
                if (s > GamePanel.highscore) GamePanel.highscore = s;
            }
        } catch (NumberFormatException ignored) {}

        // Speed override
        gamePanel.devSpeedOverride = cbSpeedOverride.isSelected();
        try {
            double sx = Double.parseDouble(tfSpeedX.getText().trim());
            double sy = Double.parseDouble(tfSpeedY.getText().trim());
            if (sx > 0) p.speedX = sx;
            if (sy > 0) p.speedY = sy;
        } catch (NumberFormatException ignored) {}

        // Position (user enters center; store as top-left)
        try {
            double cx = Double.parseDouble(tfPosX.getText().trim());
            double cy = Double.parseDouble(tfPosY.getText().trim());
            if (cx > 0 && cy > 0) {
                p.x = cx - p.cellRad;
                p.y = cy - p.cellRad;
            }
        } catch (NumberFormatException ignored) {}

        // Magnet radius
        try {
            double mr = Double.parseDouble(tfMagnetRadius.getText().trim());
            if (mr >= 0) gamePanel.magnetRadius = mr;
        } catch (NumberFormatException ignored) {}

        // Regen level
        try {
            int rl = Integer.parseInt(tfRegenLevel.getText().trim());
            if (rl >= 0) gamePanel.regenLevel = rl;
        } catch (NumberFormatException ignored) {}

        // Cell density
        try {
            double d = Double.parseDouble(tfCellDensity.getText().trim());
            if (d > 0) GamePanel.cellDensity = d;
        } catch (NumberFormatException ignored) {}

        // Shave rate multiplier
        try {
            double sr = Double.parseDouble(tfShaveRate.getText().trim());
            if (sr > 0) GamePanel.shaveRateMultiplier = sr;
        } catch (NumberFormatException ignored) {}

        gamePanel.paused = false;
        dispose();
    }

    // ── UI helpers ────────────────────────────────────────────────────────

    /** Adds a yellow section title spanning both columns. Returns next row index. */
    private int addSectionTitle(JPanel panel, String text, Color color, Font font, int row) {
        JLabel title = new JLabel(text);
        title.setForeground(color);
        title.setFont(font);
        GridBagConstraints tc = new GridBagConstraints();
        tc.gridx = 0; tc.gridy = row; tc.gridwidth = 2;
        tc.anchor = GridBagConstraints.WEST;
        tc.insets = new Insets(0, 0, 10, 0);
        panel.add(title, tc);
        return row + 1;
    }

    /** Adds a colored section header spanning both columns. Returns next row index. */
    private int addSectionHeader(JPanel panel, String text, Font font, Color color, int row) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(color);
        lbl.setFont(font);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(10, 0, 4, 0);
        panel.add(lbl, gc);
        return row + 1;
    }

    private JLabel makeLabel(String text, Font font, Color fg) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(fg);
        return lbl;
    }

    private JTextField makeField(Font font, Color bg, Color fg) {
        JTextField tf = new JTextField();
        tf.setFont(font);
        tf.setBackground(bg);
        tf.setForeground(fg);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 110)),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        return tf;
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.brighter()),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        return btn;
    }
}
