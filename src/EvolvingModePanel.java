import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * EvolvingModePanel : Entry screen for the Infinite Evolving Cells game mode.
 * Allows the player to enter their name, view existing progress, load a save
 * from another player, and start the evolving mode session.
 *
 * <p>In this mode the player progresses through infinite stages. Each cleared
 * stage spawns harder NPCs while the player keeps their upgrades and cell size.
 * Progress (max stage reached, highest score) is automatically saved per player
 * name in saves/evolving/ and loaded when the same name is entered again.
 *
 * @author Kamil Yunus Ozkaya
 */
@SuppressWarnings({"serial", "this-escape"})
public class EvolvingModePanel extends JPanel {

    private final MainClass mainClass;
    private final MenuBackground menuBg;

    private JTextField tfPlayerName;
    private JLabel lblProgressInfo;

    public EvolvingModePanel(MainClass mainClass) {
        this.mainClass = mainClass;

        setSize(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT);
        setPreferredSize(new Dimension(MainClass.SCREEN_WIDTH, MainClass.SCREEN_HEIGHT));
        setFocusable(true);
        setLayout(null);

        int centerX = MainClass.SCREEN_WIDTH / 2;
        int centerY = MainClass.SCREEN_HEIGHT / 2;

        // ── Player Name Row ──────────────────────────────────────────────
        int fieldW = 220;
        int fieldH = 32;
        int labelW = 130;
        int rowY   = centerY - 60;

        JLabel lblName = new JLabel("Player Name:");
        lblName.setForeground(new Color(180, 200, 240));
        lblName.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 15));
        lblName.setBounds(centerX - fieldW / 2 - labelW - 4, rowY, labelW, fieldH);
        add(lblName);

        tfPlayerName = new JTextField(GamePanel.playerName);
        tfPlayerName.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 14));
        tfPlayerName.setBackground(new Color(40, 45, 60));
        tfPlayerName.setForeground(Color.WHITE);
        tfPlayerName.setCaretColor(Color.WHITE);
        tfPlayerName.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 90, 160)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        tfPlayerName.setBounds(centerX - fieldW / 2, rowY, fieldW, fieldH);
        add(tfPlayerName);

        // ── Progress info label ──────────────────────────────────────────
        lblProgressInfo = new JLabel("");
        lblProgressInfo.setForeground(new Color(140, 200, 140));
        lblProgressInfo.setFont(new Font(GameConstants.FONT_FAMILY, Font.ITALIC, 13));
        lblProgressInfo.setHorizontalAlignment(SwingConstants.CENTER);
        lblProgressInfo.setBounds(centerX - 260, rowY + fieldH + 8, 520, 22);
        add(lblProgressInfo);
        refreshProgressInfo(GamePanel.playerName);

        // ── Buttons ──────────────────────────────────────────────────────
        int btnW = 160;
        int btnH = GameConstants.BUTTON_HEIGHT + 4;
        int gap  = 14;
        int totalW = btnW * 2 + gap;
        int btnY = rowY + fieldH + 44;

        StyledButton startBtn = new StyledButton("START", GameConstants.BTN_GREEN);
        startBtn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 16));
        startBtn.setBounds(centerX - totalW / 2, btnY, btnW, btnH);
        add(startBtn);

        StyledButton backBtn = new StyledButton("BACK", new Color(100, 60, 60));
        backBtn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 16));
        backBtn.setBounds(centerX - totalW / 2 + btnW + gap, btnY, btnW, btnH);
        add(backBtn);

        // ── Load save row ─────────────────────────────────────────────────
        int loadBtnW = 200;
        StyledButton loadBtn = new StyledButton("LOAD SAVE", new Color(50, 80, 130));
        loadBtn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 13));
        loadBtn.setBounds(centerX - loadBtnW / 2, btnY + btnH + 12, loadBtnW, btnH - 8);
        add(loadBtn);

        // ── Description label ─────────────────────────────────────────────
        JLabel descLabel = new JLabel(
            "<html><center>" +
            "Survive through infinite stages — each stage spawns more and harder NPCs.<br>" +
            "Your upgrades, size, and score carry over between stages.<br>" +
            "Progress is auto-saved per player name. There is no final victory." +
            "</center></html>");
        descLabel.setForeground(new Color(160, 180, 220));
        descLabel.setFont(new Font(GameConstants.FONT_FAMILY, Font.PLAIN, 13));
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        int descW = 560;
        int descH = 60;
        descLabel.setBounds(centerX - descW / 2, centerY - 160, descW, descH);
        add(descLabel);

        // ── Background ────────────────────────────────────────────────────
        menuBg = new MenuBackground(7, this);
        menuBg.start();

        // ── Action listeners ──────────────────────────────────────────────
        startBtn.addActionListener(e -> {
            Sound.playClickSound();
            String name = tfPlayerName.getText().trim();
            if (name.isEmpty()) name = GameSettings.DEFAULT_PLAYER_NAME;
            startEvolvingGame(name);
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

        loadBtn.addActionListener(e -> {
            Sound.playClickSound();
            showLoadSaveDialog();
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Updates the progress info label to show existing save data for the given name. */
    private void refreshProgressInfo(String name) {
        if (EvolvingProgressSave.saveExists(name)) {
            EvolvingProgressSave existing = EvolvingProgressSave.loadForPlayer(name);
            lblProgressInfo.setText("Existing progress: " + existing.getSummary());
        } else {
            lblProgressInfo.setText("No existing save for this name — starting fresh.");
        }
    }

    /** Opens a dialog to pick an existing evolving mode save and loads it. */
    private void showLoadSaveDialog() {
        List<String> savedNames = EvolvingProgressSave.listSavedPlayerNames();
        if (savedNames.isEmpty()) {
            StyledDialog.showMessageDialog(mainClass,
                "No evolving mode saves found.", "Load Save", true);
            return;
        }

        String[] options = savedNames.toArray(new String[0]);

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
                g2.setColor(new Color(25, 20, 40));
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth() - 4, getHeight() - 4, 20, 20));
                g2.setColor(new Color(90, 60, 130));
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
        JLabel titleLbl = new JLabel("Load Evolving Mode Save");
        titleLbl.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 16));
        titleLbl.setForeground(new Color(200, 180, 255));
        content.add(titleLbl, gbc);

        for (int i = 0; i < options.length; i++) {
            gbc.gridy = i + 1;
            gbc.insets = new Insets(4, 24, 4, 24);
            String playerName = options[i];

            // Build label showing name + summary
            EvolvingProgressSave s = EvolvingProgressSave.loadForPlayer(playerName);
            String btnLabel = playerName + "  [" + s.getSummary() + "]";

            StyledButton btn = new StyledButton(btnLabel, new Color(70, 50, 110));
            btn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 13));
            btn.setPreferredSize(new Dimension(400, 38));
            btn.addActionListener(ev -> {
                Sound.playClickSound();
                result[0] = playerName;
                dialog.dispose();
            });
            content.add(btn, gbc);
        }

        gbc.gridy = options.length + 1;
        gbc.insets = new Insets(12, 24, 20, 24);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        StyledButton cancelBtn = new StyledButton("CANCEL", new Color(100, 60, 60));
        cancelBtn.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 13));
        cancelBtn.setPreferredSize(new Dimension(120, 36));
        cancelBtn.addActionListener(ev -> {
            Sound.playClickSound();
            dialog.dispose();
        });
        content.add(cancelBtn, gbc);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(mainClass);
        dialog.setVisible(true);

        if (result[0] != null) {
            tfPlayerName.setText(result[0]);
            refreshProgressInfo(result[0]);
        }
    }

    /** Applies settings, creates the GamePanel in evolving mode, and starts the game. */
    private void startEvolvingGame(String name) {
        // Apply global settings (color, world size, etc.) from GameSettings
        GameSettings settings = new GameSettings();
        settings.readFromGame();
        settings.playerName = name;
        settings.applyToGame();

        // Load or create evolving progress for this player
        EvolvingProgressSave progress = EvolvingProgressSave.loadForPlayer(name);
        progress.playerName = name; // ensure name is set correctly

        menuBg.stop();
        mainClass.gamePanel = new GamePanel(mainClass, progress);
        mainClass.getContentPane().removeAll();
        mainClass.getContentPane().add(mainClass.gamePanel);
        mainClass.revalidate();
        mainClass.repaint();
        mainClass.gamePanel.requestFocusInWindow();
    }

    // ── Rendering ─────────────────────────────────────────────────────────

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
        MenuBackground.drawTitle(g2d, "Infinite Evolving Cells", 40,
            MainClass.SCREEN_HEIGHT / 2 - 240, MainClass.SCREEN_WIDTH, true);

        // Mode tag under title
        g2d.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, 15));
        g2d.setColor(new Color(180, 130, 255, 210));
        String tag = "\u221E  INFINITE STAGES  \u221E";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(tag, (MainClass.SCREEN_WIDTH - fm.stringWidth(tag)) / 2,
            MainClass.SCREEN_HEIGHT / 2 - 200);
    }
}
