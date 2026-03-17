/**
 * UpgradeType : Enumeration of all available roguelite upgrade types.
 * To add a new upgrade, add an entry here and handle it in UpgradeManager.applyUpgrade().
 * Values of upgrades are intentionally omitted from display names and descriptions.
 * @author Mert Efe Sevim
 */
public enum UpgradeType {

    SPEED_BOOST(
        "Speed Boost",
        "Your cell surges with new speed, leaving rivals behind."
    ),
    SIZE_BOOST(
        "Size Surge",
        "Your cell grows dramatically larger, dominating the arena."
    ),
    DENSITY_BOOST(
        "Bountiful World",
        "The world fills with more food — a feast awaits."
    ),
    DIVERGENCY_BOOST(
        "Big Feast",
        "Larger food cells begin appearing across the world."
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
