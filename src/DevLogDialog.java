
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * DevLogDialog : Developer log / cheat window for the Agar.io game.
 * Opened and closed via Ctrl+I (Windows/Linux) or Cmd+I (macOS).
 * While open the game loop is paused.
 * Displays current world state and allows editing all key values,
 * including cell density for controlling food cell spawn rates.
 * @author Kamil Yunus Özkaya
 */
public class DevLogDialog extends JDialog {

    private final GamePanel gamePanel;

    // Input fields
    private JTextField tfName;
    private JTextField tfRadius;
    private JTextField tfScore;
    private JTextField tfSpeedX;
    private JTextField tfSpeedY;
    private JCheckBox  cbSpeedOverride;
    private JTextField tfPosX;
    private JTextField tfPosY;
    private JLabel     lblEnemyCount;
    private JTextField tfCellDensity;
    private JLabel     lblMaxCells;

    /**
     * Constructs the developer log dialog.
     * @param owner    The parent JFrame (MainClass).
     * @param gamePanel Reference to the running GamePanel for reading/writing state.
     */
    public DevLogDialog(JFrame owner, GamePanel gamePanel) {
        super(owner, "Dev Log  [Ctrl+I / Cmd+I to close]", false); // non-modal
        this.gamePanel = gamePanel;

        setSize(420, 500);
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
        lc.insets = new Insets(5, 4, 5, 10);
        lc.gridx = 0;

        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(5, 0, 5, 4);
        fc.gridx = 1;

        Color labelColor = new Color(180, 220, 255);
        Color fieldBg    = new Color(50, 50, 65);
        Color fieldFg    = Color.WHITE;
        Font  labelFont  = new Font("Arial", Font.BOLD, 13);
        Font  fieldFont  = new Font("Arial", Font.PLAIN, 13);

        int row = 0;

        // ---- Section title ----
        JLabel title = new JLabel("  Developer Log \u2014 Editable World State");
        title.setForeground(Color.YELLOW);
        title.setFont(new Font("Arial", Font.BOLD, 14));
        GridBagConstraints tc = new GridBagConstraints();
        tc.gridx = 0; tc.gridy = row; tc.gridwidth = 2;
        tc.anchor = GridBagConstraints.WEST;
        tc.insets = new Insets(0, 0, 10, 0);
        content.add(title, tc);
        row++;

        // ---- Player Name ----
        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Player Name:", labelFont, labelColor), lc);
        tfName = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfName, fc);
        row++;

        // ---- Player Radius ----
        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Player Radius:", labelFont, labelColor), lc);
        tfRadius = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfRadius, fc);
        row++;

        // ---- Score ----
        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Score:", labelFont, labelColor), lc);
        tfScore = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfScore, fc);
        row++;

        // ---- Speed X ----
        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Speed X:", labelFont, labelColor), lc);
        tfSpeedX = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfSpeedX, fc);
        row++;

        // ---- Speed Y ----
        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Speed Y:", labelFont, labelColor), lc);
        tfSpeedY = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfSpeedY, fc);
        row++;

        // ---- Speed override checkbox ----
        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Manual Speed Override:", labelFont, labelColor), lc);
        cbSpeedOverride = new JCheckBox();
        cbSpeedOverride.setBackground(new Color(30, 30, 40));
        cbSpeedOverride.setForeground(labelColor);
        cbSpeedOverride.setFont(labelFont);
        cbSpeedOverride.setToolTipText("When checked, dynamic speed recalculation is skipped");
        content.add(cbSpeedOverride, fc);
        row++;

        // ---- Position X ----
        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Position X (world):", labelFont, labelColor), lc);
        tfPosX = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfPosX, fc);
        row++;

        // ---- Position Y ----
        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Position Y (world):", labelFont, labelColor), lc);
        tfPosY = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfPosY, fc);
        row++;

        // ---- Enemy cell count (read-only) ----
        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Enemy Cells:", labelFont, labelColor), lc);
        lblEnemyCount = new JLabel();
        lblEnemyCount.setForeground(new Color(120, 255, 120));
        lblEnemyCount.setFont(fieldFont);
        content.add(lblEnemyCount, fc);
        row++;

        // ---- Cell Density (editable) ----
        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Cell Density (cells/M px):", labelFont, labelColor), lc);
        tfCellDensity = makeField(fieldFont, fieldBg, fieldFg);
        content.add(tfCellDensity, fc);
        row++;

        // ---- Max cells (read-only, computed from density) ----
        lc.gridy = row; fc.gridy = row;
        content.add(makeLabel("Max Food Cells:", labelFont, labelColor), lc);
        lblMaxCells = new JLabel();
        lblMaxCells.setForeground(new Color(120, 255, 120));
        lblMaxCells.setFont(fieldFont);
        content.add(lblMaxCells, fc);
        row++;

        // ---- Buttons row ----
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

        setContentPane(content);
    }

    /** Populates all fields from the current game state. */
    private void populateFields() {
        Cell p = gamePanel.playerCell;
        tfName.setText(GamePanel.playerName);
        tfRadius.setText(String.format("%.2f", p.cellRad));
        tfScore.setText(String.valueOf(gamePanel.hud.score));
        tfSpeedX.setText(String.format("%.2f", p.speedX));
        tfSpeedY.setText(String.format("%.2f", p.speedY));
        cbSpeedOverride.setSelected(gamePanel.devSpeedOverride);
        // x/y stored as top-left; report the center for clarity
        tfPosX.setText(String.format("%.0f", p.x + p.cellRad));
        tfPosY.setText(String.format("%.0f", p.y + p.cellRad));
        lblEnemyCount.setText(String.valueOf(gamePanel.celllist.size()));
        tfCellDensity.setText(String.format("%.2f", GamePanel.cellDensity));
        lblMaxCells.setText(String.valueOf(gamePanel.getMaxCells()));
    }

    /**
     * Reads edited values, applies them to the game state, then closes the dialog
     * and unpauses the game. Invalid numeric entries are silently ignored.
     */
    private void applyAndClose() {
        Cell p = gamePanel.playerCell;

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
                gamePanel.hud.score = s;
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

        // Cell density
        try {
            double d = Double.parseDouble(tfCellDensity.getText().trim());
            if (d > 0) GamePanel.cellDensity = d;
        } catch (NumberFormatException ignored) {}

        gamePanel.paused = false;
        dispose();
    }

    // ---- UI helpers ----

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
