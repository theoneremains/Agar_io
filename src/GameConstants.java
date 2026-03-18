import java.awt.*;

/**
 * GameConstants : Centralized constants used across the game.
 * Prevents magic numbers from being scattered throughout the codebase
 * and provides a single source of truth for tunable game parameters.
 * @author Kamil Yunus Ozkaya
 */
public final class GameConstants {

    private GameConstants() {} // Prevent instantiation

    // ── Screen & Window ──────────────────────────────────────────────────
    public static final int DEFAULT_SCREEN_WIDTH  = 1280;
    public static final int DEFAULT_SCREEN_HEIGHT = 720;
    public static final int BUTTON_WIDTH  = 200;
    public static final int BUTTON_HEIGHT = 50;

    // ── World ────────────────────────────────────────────────────────────
    public static final int DEFAULT_WORLD_WIDTH  = 3840;
    public static final int DEFAULT_WORLD_HEIGHT = 2160;
    public static final int MIN_WORLD_WIDTH  = 800;
    public static final int MIN_WORLD_HEIGHT = 600;
    public static final int SPAWN_BORDER = 40;

    // ── Cell Properties ──────────────────────────────────────────────────
    public static final double INITIAL_RADIUS  = 2.0;
    public static final double DEFAULT_SPEED   = 3.0;
    public static final double DEFAULT_CELL_DENSITY = 200.0;

    // ── Food Cell Categories ─────────────────────────────────────────────
    public static final double SMALL_RAD       = 1.0;
    public static final double MEDIUM_RAD_MIN  = 2.0;
    public static final double MEDIUM_RAD_MAX  = 5.0;
    public static final double LARGE_RAD_MIN   = 5.0;
    public static final double LARGE_RAD_MAX   = 10.0;
    public static final double SMALL_CHANCE    = 0.90;
    public static final double MEDIUM_CHANCE   = 0.07;
    // Large = remaining 3%

    // ── NPCs ─────────────────────────────────────────────────────────────
    public static final int MIN_NPC_COUNT = 3;
    public static final double MIN_DIVIDE_RADIUS = 0.7;

    // ── Camera ───────────────────────────────────────────────────────────
    public static final double INITIAL_ZOOM     = 5.0;
    public static final double MIN_ZOOM         = 0.8;
    public static final double ZOOM_LERP        = 0.03;
    public static final double CAMERA_LERP      = 0.15;

    // ── Shave (erosion-based division) ──────────────────────────────────
    /** Fraction of overlap depth converted to area loss per tick */
    public static final double SHAVE_RATE = 0.04;
    /** Accumulated shaved area needed before a food cell is spawned */
    public static final double SHAVE_MIN_FOOD_AREA = 0.25;
    /** Ticks between visual contact effects during shaving */
    public static final int SHAVE_EFFECT_INTERVAL = 20;
    /** Minimum overlap depth (px) before shaving starts */
    public static final double SHAVE_OVERLAP_THRESHOLD = 0.3;

    // ── Timing ───────────────────────────────────────────────────────────
    public static final int GAME_TICK_MS       = 10;   // ~100 FPS
    public static final int CELL_SPAWN_TICK_MS = 500;
    public static final int CELL_SPAWN_BATCH   = 10;
    public static final float SPAWN_ALPHA_STEP = 0.05f;

    // ── Effects ──────────────────────────────────────────────────────────
    public static final int MAX_CONTACT_EFFECTS   = 20;
    public static final int MAX_DIVISION_EFFECTS  = 30;
    public static final long BOUNCE_COOLDOWN_MS   = 200;

    // ── Scoring ──────────────────────────────────────────────────────────
    public static final int SCORE_MULTIPLIER = 10;

    // ── Color Palette ────────────────────────────────────────────────────
    public static final Color[] CELL_COLORS = {
        Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY, Color.GRAY,
        Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE,
        Color.YELLOW, Color.PINK
    };

    // ── UI Theme Colors ──────────────────────────────────────────────────
    public static final Color BTN_GREEN  = new Color(40, 140, 70);
    public static final Color BTN_BLUE   = new Color(50, 80, 140);
    public static final Color BTN_RED    = new Color(140, 50, 50);
    public static final Color BTN_ON     = new Color(40, 130, 70);
    public static final Color BTN_OFF    = new Color(120, 50, 50);

    // ── Fonts ────────────────────────────────────────────────────────────
    public static final String FONT_FAMILY = "SansSerif";
    public static final String FONT_FAMILY_MONO = "Arial";

    // ── Audio ────────────────────────────────────────────────────────────
    public static final int SAMPLE_RATE = 44100;

    // ── Saves ────────────────────────────────────────────────────────────
    public static final int MAX_SAVE_FILES = 3;
    public static final String SAVES_DIR   = "saves";
    public static final String SAVE_EXT    = ".cfg";

    // ── Evolving Mode ────────────────────────────────────────────────────
    /** Directory for Infinite Evolving Cells mode progress saves */
    public static final String EVOLVING_SAVES_DIR = "saves/evolving";
    /** File extension for evolving mode save files */
    public static final String EVOLVING_SAVE_EXT  = ".ecfg";
    /** NPC count at stage 1 */
    public static final int EVOLVING_BASE_NPC_COUNT = 3;
    /** Additional NPCs added per stage */
    public static final int EVOLVING_NPC_INCREMENT  = 2;
    /** Maximum NPC count across all stages */
    public static final int EVOLVING_MAX_NPC_COUNT  = 30;
    /** World width at stage 1 of evolving mode */
    public static final int EVOLVING_INITIAL_WORLD_W = 1280;
    /** World height at stage 1 of evolving mode */
    public static final int EVOLVING_INITIAL_WORLD_H = 720;
    /** World width growth per stage */
    public static final int EVOLVING_WORLD_W_INCREMENT = 256;
    /** World height growth per stage */
    public static final int EVOLVING_WORLD_H_INCREMENT = 144;
    /** UI color for the Evolving Mode button */
    public static final Color BTN_PURPLE = new Color(100, 50, 160);

    // ── Roguelite Upgrades ───────────────────────────────────────────────
    /** Score thresholds at which the player is offered an upgrade choice */
    public static final int[] UPGRADE_SCORE_THRESHOLDS = {
        50, 150, 300, 500, 800, 1200, 1800, 2500, 3500, 5000
    };
    /** Number of upgrade choices presented at each level-up */
    public static final int UPGRADE_CHOICES = 3;
    /** Speed added to player per Speed Boost upgrade */
    public static final double SPEED_UPGRADE_AMOUNT = 0.8;
    /** Radius added (via area formula) per Size Surge upgrade */
    public static final double SIZE_UPGRADE_AMOUNT = 6.0;
    /** Multiplier applied to cell density per Bountiful World upgrade */
    public static final double DENSITY_UPGRADE_FACTOR = 1.4;
    /** Shift in small-food probability per Big Feast upgrade (negative = fewer small) */
    public static final double DIVERGENCY_SMALL_SHIFT = -0.12;
    /** Shift in medium-food probability per Big Feast upgrade */
    public static final double DIVERGENCY_MEDIUM_SHIFT = 0.06;
    /** Pixels the player jumps during a Dodge dash */
    public static final int DODGE_DISTANCE = 130;
    /** Game ticks before Dodge can be used again (~3 seconds) */
    public static final int DODGE_COOLDOWN_TICKS = 300;
    // Upgrade card layout constants (used by GameRenderer + GamePanel)
    public static final int UPGRADE_CARD_WIDTH  = 220;
    public static final int UPGRADE_CARD_HEIGHT = 155;
    public static final int UPGRADE_CARD_GAP    = 30;
    public static final int UPGRADE_BTN_HEIGHT  = 50;
    public static final int UPGRADE_BTN_MARGIN  = 10;
    // ── Magnet upgrade ───────────────────────────────────────────────────
    /** Small base magnet radius the player always has (even without the upgrade) */
    public static final double BASE_MAGNET_RADIUS = 10.0;
    /** World-space radius added per Magnet upgrade level */
    public static final double MAGNET_RADIUS_PER_LEVEL = 70.0;
    /** Pixels per tick that attracted food cells move toward the player */
    public static final double MAGNET_PULL_SPEED = 0.8;
    // ── Regeneration upgrade ─────────────────────────────────────────────
    /** Radius recovered per tick per Regeneration level */
    public static final double REGEN_RATE_PER_LEVEL = 0.0004;
    // ── Split Shield upgrade ─────────────────────────────────────────────
    /** Base shave damage multiplier when no Split Shield (1.0 = full damage) */
    public static final double SPLIT_SHIELD_BASE       = 1.0;
    /** Damage multiplier reduction per Split Shield level */
    public static final double SPLIT_SHIELD_PER_LEVEL  = 0.18;
    /** Minimum damage multiplier (floor) — can never reduce below this */
    public static final double SPLIT_SHIELD_MIN        = 0.25;

    // ── Utility Methods ──────────────────────────────────────────────────

    /**
     * Area-based growth formula: total area is conserved.
     * @param r1 radius of the eater
     * @param r2 radius of the eaten
     * @return new radius after absorbing the eaten cell
     */
    public static double growRadius(double r1, double r2) {
        return Math.sqrt(r1 * r1 + r2 * r2);
    }

    /**
     * Computes score from radius using the standard 10x multiplier.
     */
    public static int scoreFromRadius(double radius) {
        return (int) Math.ceil(radius * SCORE_MULTIPLIER);
    }

    /**
     * Squared distance between two points (avoids sqrt for comparisons).
     */
    public static double distSq(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return dx * dx + dy * dy;
    }
}
