import java.util.*;

/**
 * UpgradeManager : Manages roguelite upgrade progression for a single entity
 * (the player or one NPC).
 *
 * <p>For the <strong>player</strong>: watches score thresholds, shows 3 random
 * choices via {@link GamePanel#showUpgradeSelection()}, and applies the chosen
 * upgrade to the live game state.  Kill-based upgrades can also be triggered
 * immediately by {@link #triggerKillUpgrade()}.
 *
 * <p>For <strong>NPCs</strong>: same threshold tracking, but upgrades are applied
 * automatically and silently via {@link #checkAndAutoApplyForNPC}.
 * When an NPC kills another NPC, {@link #triggerNPCKillUpgrade} applies a bonus upgrade.
 *
 * <p>Adding a new upgrade requires only:
 * <ol>
 *   <li>Extend {@link UpgradeType}.</li>
 *   <li>Add a {@code case} to {@link #applyUpgrade} (player) and, if applicable
 *       to NPCs, to {@link #applyNPCUpgrade}.</li>
 *   <li>If the upgrade is NPC-eligible, add the type to {@link #NPC_ELIGIBLE}.</li>
 * </ol>
 *
 * @author Mert Efe Sevim
 */
public class UpgradeManager {

    /**
     * Upgrade types that NPCs may receive automatically.
     * Excludes player-only perks (Dodge, Magnet, Density/Divergency world changes).
     */
    private static final UpgradeType[] NPC_ELIGIBLE = {
        UpgradeType.SPEED_BOOST,
        UpgradeType.SIZE_BOOST,
        UpgradeType.REGENERATION,
        UpgradeType.SPLIT_SHIELD,
    };

    private int nextThresholdIndex = 0;
    private boolean upgradeReady = false;
    private List<UpgradeType> currentChoices = new ArrayList<>();
    private boolean dodgeUnlocked = false;
    private final Random random = new Random();

    /**
     * Tracks how many times each upgrade type has been applied to the player.
     * Used by {@link GameRenderer} to display a compact upgrade history.
     */
    private final Map<UpgradeType, Integer> appliedCounts = new EnumMap<>(UpgradeType.class);

    // ── Player Score Checking ────────────────────────────────────────────

    /**
     * Called every game tick to check whether the player has crossed the next
     * upgrade threshold.  No-op when an upgrade choice is already pending.
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
            currentChoices = pickPlayerChoices();
        }
    }

    // ── Kill-based Upgrade (Player) ──────────────────────────────────────

    /**
     * Immediately marks an upgrade as ready (bypassing score thresholds) so the
     * player is offered a choice after killing an NPC.
     * No-op when an upgrade is already pending.
     */
    public void triggerKillUpgrade() {
        if (upgradeReady) return;
        upgradeReady = true;
        currentChoices = pickPlayerChoices();
        // Note: nextThresholdIndex is NOT incremented — score thresholds are preserved.
    }

    // ── NPC Auto-Upgrade ─────────────────────────────────────────────────

    /**
     * Checks the NPC's score against thresholds.  If a threshold is crossed,
     * a random NPC-eligible upgrade is applied immediately with no UI.
     *
     * @param score current NPC score
     * @param npc   the NPC to upgrade
     */
    public void checkAndAutoApplyForNPC(int score, NPC npc) {
        int[] thresholds = GameConstants.UPGRADE_SCORE_THRESHOLDS;
        if (nextThresholdIndex >= thresholds.length) return;
        if (score >= thresholds[nextThresholdIndex]) {
            nextThresholdIndex++;
            List<UpgradeType> choices = pickNPCChoices();
            if (!choices.isEmpty()) {
                applyNPCUpgrade(choices.get(random.nextInt(choices.size())), npc);
            }
        }
    }

    /**
     * Immediately applies a random NPC-eligible upgrade to an NPC as a bonus for
     * killing another NPC.  Does not consume a score threshold slot.
     *
     * @param npc the NPC that made the kill
     */
    public void triggerNPCKillUpgrade(NPC npc) {
        List<UpgradeType> choices = pickNPCChoices();
        if (!choices.isEmpty()) {
            applyNPCUpgrade(choices.get(random.nextInt(choices.size())), npc);
        }
    }

    /**
     * Applies an upgrade directly to an NPC's state.
     * Only NPC-eligible upgrade types produce an effect here.
     *
     * @param type the upgrade to apply
     * @param npc  the target NPC
     */
    public void applyNPCUpgrade(UpgradeType type, NPC npc) {
        switch (type) {
            case SPEED_BOOST:
                npc.speedBonus += GameConstants.SPEED_UPGRADE_AMOUNT;
                break;
            case SIZE_BOOST:
                npc.cell.cellRad = GameConstants.growRadius(
                    npc.cell.cellRad, GameConstants.SIZE_UPGRADE_AMOUNT);
                npc.score = GameConstants.scoreFromRadius(npc.cell.cellRad);
                break;
            case REGENERATION:
                npc.regenLevel++;
                break;
            case SPLIT_SHIELD:
                npc.splitShieldFactor = Math.max(GameConstants.SPLIT_SHIELD_MIN,
                    npc.splitShieldFactor - GameConstants.SPLIT_SHIELD_PER_LEVEL);
                break;
            default:
                break; // player-only upgrades are silently ignored for NPCs
        }
        npc.upgradeCount++;
    }

    // ── Player Accessors ─────────────────────────────────────────────────

    /** @return true when an upgrade choice is waiting to be selected by the player */
    public boolean isUpgradeReady() { return upgradeReady; }

    /** @return the list of upgrade types the player may currently choose from */
    public List<UpgradeType> getCurrentChoices() { return currentChoices; }

    /** @return true once the player has selected the Dodge upgrade */
    public boolean hasDodge() { return dodgeUnlocked; }

    /**
     * Returns a read-only view of how many times each upgrade type has been
     * applied to the player.  Used by GameRenderer for the upgrade history panel.
     */
    public Map<UpgradeType, Integer> getAppliedCounts() {
        return Collections.unmodifiableMap(appliedCounts);
    }

    // ── Apply Player Upgrade ─────────────────────────────────────────────

    /**
     * Applies the chosen upgrade to the live player-side game state and clears
     * the pending upgrade flag so normal gameplay resumes.
     *
     * @param type the upgrade chosen by the player
     * @param game the active GamePanel
     */
    public void applyUpgrade(UpgradeType type, GamePanel game) {
        upgradeReady = false;
        currentChoices.clear();

        // Track applied count for HUD display
        appliedCounts.merge(type, 1, Integer::sum);

        switch (type) {
            case SPEED_BOOST:
                game.playerSpeedBonus += GameConstants.SPEED_UPGRADE_AMOUNT;
                break;
            case SIZE_BOOST:
                game.getPlayerCell().cellRad = GameConstants.growRadius(
                    game.getPlayerCell().cellRad, GameConstants.SIZE_UPGRADE_AMOUNT);
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
            case MAGNET:
                game.magnetLevel++;
                game.magnetRadius += GameConstants.MAGNET_RADIUS_PER_LEVEL;
                break;
            case REGENERATION:
                game.regenLevel++;
                break;
            case SPLIT_SHIELD:
                game.splitShieldFactor = Math.max(GameConstants.SPLIT_SHIELD_MIN,
                    game.splitShieldFactor - GameConstants.SPLIT_SHIELD_PER_LEVEL);
                break;
            case DODGE:
                dodgeUnlocked = true;
                break;
        }
    }

    // ── Internal Helpers ─────────────────────────────────────────────────

    /**
     * Picks {@code UPGRADE_CHOICES} random player-eligible upgrades.
     * DODGE is excluded once already unlocked.
     */
    private List<UpgradeType> pickPlayerChoices() {
        List<UpgradeType> pool = new ArrayList<>();
        for (UpgradeType type : UpgradeType.values()) {
            if (type == UpgradeType.DODGE && dodgeUnlocked) continue;
            pool.add(type);
        }
        Collections.shuffle(pool, random);
        int count = Math.min(GameConstants.UPGRADE_CHOICES, pool.size());
        return new ArrayList<>(pool.subList(0, count));
    }

    /** Picks a random list of NPC-eligible upgrades (up to UPGRADE_CHOICES). */
    private List<UpgradeType> pickNPCChoices() {
        List<UpgradeType> pool = new ArrayList<>(Arrays.asList(NPC_ELIGIBLE));
        Collections.shuffle(pool, random);
        int count = Math.min(GameConstants.UPGRADE_CHOICES, pool.size());
        return new ArrayList<>(pool.subList(0, count));
    }
}
