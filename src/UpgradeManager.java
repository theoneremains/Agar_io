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
 * <p>Upgrade levels go from 1 to {@code MAX_UPGRADE_LEVEL} (5).  Each offer
 * specifies a target level; the player may occasionally receive a +2 jump
 * (skipping a level), which is rarer (EPIC or LEGENDARY tier depending on
 * the current level).  Rarity weights are defined in {@link UpgradeOffer}.
 *
 * <p>Adding a new upgrade requires only:
 * <ol>
 *   <li>Extend {@link UpgradeType}.</li>
 *   <li>Add a {@code case} to {@link #applyUpgradeOnce} (player) and, if applicable
 *       to NPCs, to {@link #applyNPCUpgrade}.</li>
 *   <li>If the upgrade is NPC-eligible, add the type to {@link #NPC_ELIGIBLE}.</li>
 * </ol>
 *
 * @author Mert Efe Sevim
 */
public class UpgradeManager {

    /** Maximum level any single upgrade type can reach. */
    public static final int MAX_UPGRADE_LEVEL = 5;

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
    private boolean upgradeReady   = false;
    private List<UpgradeOffer> currentChoices = new ArrayList<>();
    private boolean dodgeUnlocked  = false;
    private final Random random    = new Random();

    /**
     * Tracks the current level of each upgrade type applied to the player.
     * Level = number of times each type has been effectively applied.
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
            upgradeReady   = true;
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
        upgradeReady   = true;
        currentChoices = pickPlayerChoices();
        // Note: nextThresholdIndex is NOT incremented — score thresholds are preserved.
    }

    /**
     * Cancels any pending upgrade selection (e.g. when game over happens before
     * the player could choose).
     */
    public void cancelPendingUpgrade() {
        upgradeReady = false;
        currentChoices.clear();
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

    /** @return the list of upgrade offers the player may currently choose from */
    public List<UpgradeOffer> getCurrentChoices() { return currentChoices; }

    /** @return true once the player has selected the Dodge upgrade */
    public boolean hasDodge() { return dodgeUnlocked; }

    /**
     * Returns a read-only view of the current level for each upgrade type applied
     * to the player.  Used by GameRenderer for the upgrade history panel.
     */
    public Map<UpgradeType, Integer> getAppliedCounts() {
        return Collections.unmodifiableMap(appliedCounts);
    }

    /**
     * Returns the total number of upgrade levels applied (sum of all upgrade levels).
     * Used by the Evolving Mode to calculate how many upgrades to seed on NPCs.
     */
    public int getTotalAppliedLevels() {
        int total = 0;
        for (int v : appliedCounts.values()) total += v;
        return total;
    }

    /**
     * Applies a single random NPC-eligible upgrade to the given NPC.
     * Used for seeding initial upgrades on NPCs at the start of each evolving mode stage.
     * @param npc the target NPC
     */
    public void applyRandomNPCUpgrade(NPC npc) {
        List<UpgradeType> choices = pickNPCChoices();
        if (!choices.isEmpty()) {
            applyNPCUpgrade(choices.get(random.nextInt(choices.size())), npc);
        }
    }

    // ── Apply Player Upgrade ─────────────────────────────────────────────

    /**
     * Applies the chosen upgrade offer to the live player-side game state and
     * clears the pending upgrade flag so normal gameplay resumes.
     * If the offer jumps multiple levels (+2), the underlying effect is applied
     * once per level gained.
     *
     * @param offer the upgrade offer chosen by the player
     * @param game  the active GamePanel
     */
    public void applyUpgrade(UpgradeOffer offer, GamePanel game) {
        upgradeReady = false;
        currentChoices.clear();

        int currentLevel  = appliedCounts.getOrDefault(offer.type, 0);
        int levelsToApply = offer.targetLevel - currentLevel;

        // Record the new level
        appliedCounts.put(offer.type, offer.targetLevel);

        // DODGE is one-time-only — just unlock it regardless of jump size
        if (offer.type == UpgradeType.DODGE) {
            dodgeUnlocked = true;
            return;
        }

        // Apply each gained level individually
        for (int i = 0; i < levelsToApply; i++) {
            applyUpgradeOnce(offer.type, game);
        }
    }

    // ── Internal Helpers ─────────────────────────────────────────────────

    /**
     * Returns the maximum level for a given upgrade type.
     * DODGE is a one-time unlock (level 1); all other types cap at
     * {@link #MAX_UPGRADE_LEVEL}.
     */
    private int getMaxLevel(UpgradeType type) {
        return type == UpgradeType.DODGE ? 1 : MAX_UPGRADE_LEVEL;
    }

    /**
     * Picks {@code UPGRADE_CHOICES} random player-eligible upgrade offers using
     * a weighted rarity algorithm.
     *
     * <p>For each upgrade type that is not yet at max level, up to two offer
     * candidates are generated: a +1 level offer (normal) and a +2 level offer
     * (rarer, epic/legendary).  Offers are then weighted-sampled ensuring that
     * at most one offer per upgrade type appears in the final set, so a +2 jump
     * offer automatically excludes a +1 offer for the same type.
     *
     * <p>DODGE is excluded once already unlocked.
     */
    private List<UpgradeOffer> pickPlayerChoices() {
        List<UpgradeOffer> allOffers = new ArrayList<>();

        for (UpgradeType type : UpgradeType.values()) {
            if (type == UpgradeType.DODGE && dodgeUnlocked) continue;

            int currentLevel = appliedCounts.getOrDefault(type, 0);
            int maxLevel     = getMaxLevel(type);

            if (currentLevel >= maxLevel) continue;

            // +1 level offer (always available if not at cap)
            int t1 = currentLevel + 1;
            if (t1 <= maxLevel) {
                allOffers.add(new UpgradeOffer(type, t1, currentLevel));
            }

            // +2 level offer (skip a level — rarer; not applicable for DODGE)
            int t2 = currentLevel + 2;
            if (type != UpgradeType.DODGE && t2 <= maxLevel) {
                allOffers.add(new UpgradeOffer(type, t2, currentLevel));
            }
        }

        // Weighted-sample UPGRADE_CHOICES offers, one per upgrade type
        List<UpgradeOffer> result    = new ArrayList<>();
        Set<UpgradeType>   usedTypes = new HashSet<>();
        List<UpgradeOffer> remaining = new ArrayList<>(allOffers);

        for (int i = 0; i < GameConstants.UPGRADE_CHOICES; i++) {
            // Collect eligible offers (types not yet chosen)
            List<UpgradeOffer> eligible = new ArrayList<>();
            for (UpgradeOffer o : remaining) {
                if (!usedTypes.contains(o.type)) eligible.add(o);
            }
            if (eligible.isEmpty()) break;

            UpgradeOffer chosen = weightedPickOne(eligible);
            result.add(chosen);
            usedTypes.add(chosen.type);
            // Remove all offers for this type so the +1 and +2 don't both appear
            remaining.removeIf(o -> o.type == chosen.type);
        }

        return result;
    }

    /**
     * Selects one offer from the list using weighted random sampling.
     * Falls back to uniform random if all weights are zero.
     */
    private UpgradeOffer weightedPickOne(List<UpgradeOffer> offers) {
        double totalWeight = 0;
        for (UpgradeOffer o : offers) totalWeight += o.getWeight();

        if (totalWeight <= 0) {
            return offers.get(random.nextInt(offers.size()));
        }

        double r = random.nextDouble() * totalWeight;
        for (UpgradeOffer o : offers) {
            r -= o.getWeight();
            if (r <= 0) return o;
        }
        return offers.get(offers.size() - 1);
    }

    /** Picks a random list of NPC-eligible upgrade types (up to UPGRADE_CHOICES). */
    private List<UpgradeType> pickNPCChoices() {
        List<UpgradeType> pool = new ArrayList<>(Arrays.asList(NPC_ELIGIBLE));
        Collections.shuffle(pool, random);
        int count = Math.min(GameConstants.UPGRADE_CHOICES, pool.size());
        return new ArrayList<>(pool.subList(0, count));
    }

    /**
     * Applies a single level of an upgrade to the player's game state.
     * Called once per gained level when an offer is accepted.
     */
    private void applyUpgradeOnce(UpgradeType type, GamePanel game) {
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
}
