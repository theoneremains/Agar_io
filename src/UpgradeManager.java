import java.util.*;

/**
 * UpgradeManager : Manages roguelite upgrade progression throughout a game session.
 * Watches the player's score against a series of thresholds; when a threshold is
 * crossed it randomly selects {@code UPGRADE_CHOICES} options for the player to
 * choose from and applies the selected upgrade to the game state.
 *
 * Adding a new upgrade requires only: extending {@link UpgradeType} and adding a
 * case to {@link #applyUpgrade}.
 *
 * @author Mert Efe Sevim
 */
public class UpgradeManager {

    private int nextThresholdIndex = 0;
    private boolean upgradeReady = false;
    private List<UpgradeType> currentChoices = new ArrayList<>();
    private boolean dodgeUnlocked = false;
    private final Random random = new Random();

    // ── Score Checking ───────────────────────────────────────────────────

    /**
     * Called every game tick to check whether the player has crossed the next
     * upgrade threshold.  If the upgrade flag is already set (waiting for the
     * player to choose) this method is a no-op.
     *
     * @param score current player score
     */
    public void checkScore(int score) {
        if (upgradeReady) return;
        int[] thresholds = GameConstants.UPGRADE_SCORE_THRESHOLDS;
        if (nextThresholdIndex >= thresholds.length) return;
        if (score >= thresholds[nextThresholdIndex]) {
            nextThresholdIndex++;
            upgradeReady = true;
            currentChoices = pickChoices();
        }
    }

    // ── Accessors ────────────────────────────────────────────────────────

    /** @return true when an upgrade choice is waiting to be selected */
    public boolean isUpgradeReady() { return upgradeReady; }

    /** @return the list of upgrade types the player may currently choose from */
    public List<UpgradeType> getCurrentChoices() { return currentChoices; }

    /** @return true once the player has selected the Dodge upgrade */
    public boolean hasDodge() { return dodgeUnlocked; }

    // ── Apply Upgrade ────────────────────────────────────────────────────

    /**
     * Applies the chosen upgrade to the live game state and clears the pending
     * upgrade flag so normal gameplay resumes.
     *
     * @param type the upgrade chosen by the player
     * @param game the active GamePanel (used to access/modify player state)
     */
    public void applyUpgrade(UpgradeType type, GamePanel game) {
        upgradeReady = false;
        currentChoices.clear();

        switch (type) {
            case SPEED_BOOST:
                game.playerSpeedBonus += GameConstants.SPEED_UPGRADE_AMOUNT;
                break;
            case SIZE_BOOST:
                Cell player = game.getPlayerCell();
                player.cellRad = GameConstants.growRadius(player.cellRad, GameConstants.SIZE_UPGRADE_AMOUNT);
                game.updatePlayerScore();
                break;
            case DENSITY_BOOST:
                GamePanel.cellDensity *= GameConstants.DENSITY_UPGRADE_FACTOR;
                break;
            case DIVERGENCY_BOOST:
                game.foodSmallChance = Math.max(0.30,
                    game.foodSmallChance + GameConstants.DIVERGENCY_SMALL_SHIFT);
                game.foodMediumChance = Math.min(0.45,
                    game.foodMediumChance + GameConstants.DIVERGENCY_MEDIUM_SHIFT);
                break;
            case DODGE:
                dodgeUnlocked = true;
                break;
        }
    }

    // ── Internal Helpers ─────────────────────────────────────────────────

    /**
     * Randomly picks {@code UPGRADE_CHOICES} upgrade types from the available pool.
     * DODGE is excluded from the pool once it has been unlocked.
     */
    private List<UpgradeType> pickChoices() {
        List<UpgradeType> pool = new ArrayList<>();
        for (UpgradeType type : UpgradeType.values()) {
            if (type == UpgradeType.DODGE && dodgeUnlocked) continue;
            pool.add(type);
        }
        Collections.shuffle(pool, random);
        int count = Math.min(GameConstants.UPGRADE_CHOICES, pool.size());
        return new ArrayList<>(pool.subList(0, count));
    }
}
