/**
 * UpgradeOffer : Represents a single upgrade choice offered to the player.
 * Encapsulates the upgrade type, the target level after applying, the
 * from-level used for rarity calculation, and pre-computed display text.
 *
 * <p>Rarity tiers and their approximate drop weights:
 * <ul>
 *   <li>COMMON  (level 1, +1 jump)  — weight 100</li>
 *   <li>UNCOMMON (level 2, +1 jump) — weight 55</li>
 *   <li>RARE    (level 3, +1; or level 4, +1) — weight 28 / 11</li>
 *   <li>EPIC    (+2 jump from level 0–2) — base × 0.25</li>
 *   <li>LEGENDARY (level 5, or +2 jump from level 3+) — weight 4 / base × 0.08</li>
 * </ul>
 *
 * @author Mert Efe Sevim
 */
public class UpgradeOffer {

    /** Base weight values indexed by target level (index 0 unused). */
    private static final double[] BASE_WEIGHTS = { 0, 100.0, 55.0, 28.0, 11.0, 4.0 };

    /** Weight multiplier applied when the jump is +2 and from-level is 0–2 (EPIC). */
    public static final double EPIC_JUMP_MULT      = 0.25;
    /** Weight multiplier applied when the jump is +2 and from-level is 3+ (LEGENDARY). */
    public static final double LEGENDARY_JUMP_MULT = 0.08;

    /** The type of upgrade being offered. */
    public final UpgradeType type;

    /** The level the player will be at after accepting this offer. */
    public final int targetLevel;

    /** The player's current level for this upgrade type before accepting. */
    public final int fromLevel;

    /**
     * Display name shown on the upgrade button, including level suffix and
     * rarity tag if applicable.
     */
    public final String displayName;

    /** Flavor description shown on the upgrade card (same as {@link UpgradeType#description}). */
    public final String description;

    /**
     * Constructs an upgrade offer.
     *
     * @param type        the upgrade type
     * @param targetLevel the level after taking this offer
     * @param fromLevel   the player's current level before taking this offer
     */
    public UpgradeOffer(UpgradeType type, int targetLevel, int fromLevel) {
        this.type        = type;
        this.targetLevel = targetLevel;
        this.fromLevel   = fromLevel;
        this.description = type.description;
        this.displayName = buildDisplayName(type, targetLevel, fromLevel);
    }

    /**
     * Returns the weighted rarity value used for random selection.
     * Higher values mean the offer appears more often.
     */
    public double getWeight() {
        double base = (targetLevel >= 1 && targetLevel < BASE_WEIGHTS.length)
                      ? BASE_WEIGHTS[targetLevel] : 0;
        int jump = targetLevel - fromLevel;
        if (jump == 2) {
            return base * (fromLevel >= 3 ? LEGENDARY_JUMP_MULT : EPIC_JUMP_MULT);
        }
        return base;
    }

    /**
     * Returns a short rarity tag string, or an empty string for COMMON/UNCOMMON
     * upgrades that don't warrant a special label.
     */
    public String getRarityTag() {
        int jump = targetLevel - fromLevel;
        if (targetLevel == 5)               return "\u26DC LEGENDARY";
        if (jump == 2 && fromLevel >= 3)    return "\u26DC LEGENDARY";
        if (jump == 2)                      return "\u2605 EPIC";
        if (targetLevel >= 4)               return "\u25C6 RARE";
        return "";
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private static String buildDisplayName(UpgradeType type, int targetLevel, int fromLevel) {
        // DODGE is a one-time unlock — keep its existing display name unchanged
        if (type == UpgradeType.DODGE) return type.displayName;

        int jump = targetLevel - fromLevel;
        String rarity;
        if (targetLevel == 5)            rarity = "\u26DC LEGENDARY";
        else if (jump == 2 && fromLevel >= 3) rarity = "\u26DC LEGENDARY";
        else if (jump == 2)              rarity = "\u2605 EPIC";
        else if (targetLevel >= 4)       rarity = "\u25C6 RARE";
        else                             rarity = "";

        String base = type.displayName + " Lv." + targetLevel;
        return rarity.isEmpty() ? base : base + "  " + rarity;
    }
}
