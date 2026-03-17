/**
 * UpgradeType : Enumeration of all available roguelite upgrade types.
 * To add a new upgrade, add an entry here and handle it in
 * {@link UpgradeManager#applyUpgrade} (player) and
 * {@link UpgradeManager#applyNPCUpgrade} (NPC) as appropriate.
 *
 * Values of upgrades are intentionally omitted from display text — they are
 * calculated internally via {@link GameConstants}.
 *
 * @author Mert Efe Sevim
 */
public enum UpgradeType {

    // ── Available to both player and NPCs ────────────────────────────────

    SPEED_BOOST(
        "Speed Boost",
        "Your cell surges with new speed, leaving rivals behind."
    ),
    SIZE_BOOST(
        "Size Surge",
        "Your cell grows dramatically larger, dominating the arena."
    ),
    REGENERATION(
        "Regeneration",
        "Your cell slowly recovers lost size over time."
    ),
    SPLIT_SHIELD(
        "Split Shield",
        "Cell division deals less damage — you keep more of yourself."
    ),

    // ── Player-only upgrades ─────────────────────────────────────────────

    DENSITY_BOOST(
        "Bountiful World",
        "The world fills with more food — a feast awaits."
    ),
    DIVERGENCY_BOOST(
        "Big Feast",
        "Larger food cells begin appearing across the world."
    ),
    MAGNET(
        "Magnet",
        "Nearby food cells are drawn toward you like a magnet."
    ),
    DODGE(
        "Dodge  [EPIC]",
        "Press SPACE to dash a distance — one time upgrade!"
    );

    /** Short name shown on the upgrade card button */
    public final String displayName;

    /** Flavor description shown above the upgrade card button */
    public final String description;

    UpgradeType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
