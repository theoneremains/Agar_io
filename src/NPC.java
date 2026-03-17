
import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NPC class : Represents an AI-controlled cell that navigates the world intelligently,
 * fleeing from cells bigger than it and chasing cells smaller than it (including the player).
 * Eats smaller food cells and other NPCs, and tracks its own score.
 * NPCs have random names and behave like autonomous players with survival instincts.
 * Each NPC has a difficulty level (EASY, MEDIUM, HARD) that affects its error margin,
 * steering jitter, mood stability, and food-seeking aggression.
 * @author Kamil Yunus Özkaya
 */
public class NPC {

    /** Difficulty levels affecting NPC AI precision and aggression */
    public enum Difficulty {
        EASY(0.15, 0.70, 0.010, 0.8, 300),
        MEDIUM(0.06, 0.35, 0.004, 1.5, 450),
        HARD(0.02, 0.15, 0.001, 2.5, 600);

        /** Chance per tick to ignore navigation and move randomly */
        public final double errorChance;
        /** Angular jitter (radians) added to steering vector */
        public final double steerJitter;
        /** Chance per tick to become distracted */
        public final double moodChangeChance;
        /** Food chase weight multiplier (higher = more aggressive food seeking) */
        public final double foodWeight;
        /** Vision range in pixels */
        public final int visionRange;

        Difficulty(double errorChance, double steerJitter, double moodChangeChance,
                   double foodWeight, int visionRange) {
            this.errorChance = errorChance;
            this.steerJitter = steerJitter;
            this.moodChangeChance = moodChangeChance;
            this.foodWeight = foodWeight;
            this.visionRange = visionRange;
        }
    }

    /** The cell entity representing this NPC in the world */
    public Cell cell;

    /** Display name shown inside the NPC cell */
    public String name;

    /** Score accumulated by eating cells (increased by eaten cell's radius) */
    public int score = 0;

    /** Whether this NPC is still alive */
    public boolean alive = true;

    /** Difficulty level of this NPC */
    public final Difficulty difficulty;

    // ── Roguelite Upgrade State ──────────────────────────────────────────

    /** Each NPC has its own upgrade manager tracking score thresholds */
    public final UpgradeManager upgradeManager = new UpgradeManager();

    /** Bonus speed added by Speed Boost upgrades */
    public double speedBonus = 0.0;

    /** Number of Regeneration upgrade levels received */
    public int regenLevel = 0;

    /**
     * Factor of radius retained when this NPC is divided.
     * Default is {@code 1/√2} (~0.707); increases with Split Shield upgrades.
     */
    public double splitShieldFactor = GameConstants.SPLIT_SHIELD_BASE;

    /** Total upgrade count — used for the upgrade star indicator in the renderer */
    public int upgradeCount = 0;

    // Movement direction flags (set by navigation AI)
    private boolean right, left, up, down;

    // Ticks until next direction change (used only for random fallback)
    private int directionTimer = 0;

    private static final Random rng = new Random();

    // Speed constants
    private static final double DEFAULT_SPEED = GameConstants.DEFAULT_SPEED;

    /** When true the NPC is in a "distracted" phase and will roam instead of chasing */
    private boolean distracted = false;

    /** Ticks remaining in the current distracted phase */
    private int distractedTimer = 0;

    /** Target position for roaming behaviour (world coordinates) */
    private int roamTargetX = -1;
    private int roamTargetY = -1;

    /** Pool of random NPC names */
    private static final String[] NPC_NAMES = {
        "Blob", "Chomper", "Nibbler", "Gulper", "Muncher",
        "Snapper", "Gobbler", "Devourer", "Nomnom", "Slurp",
        "Biter", "Feaster", "Glutton", "Swallower", "Crusher",
        "Tiny", "Bubbles", "Spike", "Shadow", "Flash"
    };

    /**
     * Creates a new NPC with random name, color, and difficulty at the given position.
     * @param cx center X in world coordinates
     * @param cy center Y in world coordinates
     * @param radius initial cell radius
     * @param usedNames names already taken (to avoid duplicates)
     * @param difficulty the AI difficulty level for this NPC
     */
    public NPC(int cx, int cy, double radius, java.util.Set<String> usedNames, Difficulty difficulty) {
        this.difficulty = difficulty;
        this.cell = new Cell(cx, cy, radius);
        this.cell.spawnAlpha = 1f;
        this.cell.cellColor = GameConstants.CELL_COLORS[rng.nextInt(GameConstants.CELL_COLORS.length)];

        // Pick a unique name
        String picked;
        int attempts = 0;
        do {
            picked = NPC_NAMES[rng.nextInt(NPC_NAMES.length)];
            attempts++;
        } while (usedNames.contains(picked) && attempts < 50);
        this.name = picked;
        usedNames.add(picked);

        // Set initial speed
        updateSpeed();
        randomizeDirection();
    }

    /** Updates the NPC's speed — base speed plus any Speed Boost upgrade bonus */
    public void updateSpeed() {
        cell.speedX = DEFAULT_SPEED + speedBonus;
        cell.speedY = DEFAULT_SPEED + speedBonus;
    }

    /** Randomizes movement direction (fallback when no threats or prey nearby) */
    private void randomizeDirection() {
        right = rng.nextBoolean();
        left  = !right && rng.nextBoolean();
        up    = rng.nextBoolean();
        down  = !up && rng.nextBoolean();
        directionTimer = 30 + rng.nextInt(70); // change direction every 30-100 ticks
    }

    /**
     * Updates NPC position and movement AI each tick.
     * Uses navigation helper to flee from bigger cells and chase smaller ones.
     * Falls back to random movement when nothing interesting is nearby.
     * @param playerCell the player's cell (for threat/prey detection)
     * @param npcList all NPCs in the game (for threat/prey detection among NPCs)
     * @param foodList all food cells in the game (for prey detection)
     */
    public void update(Cell playerCell, CopyOnWriteArrayList<NPC> npcList, CopyOnWriteArrayList<Cell> foodList) {
        if (!alive) return;

        updateSpeed();

        // Random mood swing: occasionally become "distracted" and stop chasing
        if (!distracted && rng.nextDouble() < difficulty.moodChangeChance) {
            distracted = true;
            distractedTimer = 60 + rng.nextInt(120); // distracted for 60–180 ticks
        }
        if (distracted) {
            distractedTimer--;
            if (distractedTimer <= 0) distracted = false;
        }

        // Random error: occasionally ignore navigation entirely and move randomly
        if (rng.nextDouble() < difficulty.errorChance) {
            randomizeDirection();
            cell.updateCellPos(right, left, up, down);
            return;
        }

        // Navigation AI: compute steering direction based on threats and prey
        boolean navigated = navigate(playerCell, npcList, foodList);

        if (!navigated) {
            // No threats or prey in vision range — roam towards a random target
            roam();
        }

        cell.updateCellPos(right, left, up, down);
    }

    /**
     * Roaming behaviour: picks a random target in the world and steers towards it.
     * When the NPC reaches the target (or after a timeout), picks a new target.
     * This lets NPCs actively explore when nothing is in their vision range.
     */
    private void roam() {
        double myCX = cell.x + cell.cellRad;
        double myCY = cell.y + cell.cellRad;

        // Pick a new roam target if we don't have one or we're close to it
        if (roamTargetX < 0 || roamTargetY < 0 || directionTimer <= 0) {
            roamTargetX = 40 + rng.nextInt(Math.max(1, MainClass.WORLD_WIDTH - 80));
            roamTargetY = 40 + rng.nextInt(Math.max(1, MainClass.WORLD_HEIGHT - 80));
            directionTimer = 100 + rng.nextInt(200);
        }

        double dx = roamTargetX - myCX;
        double dy = roamTargetY - myCY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 50) {
            // Reached target, pick a new one next tick
            directionTimer = 0;
            return;
        }

        directionTimer--;

        // Steer towards roam target with some jitter
        double angle = Math.atan2(dy, dx);
        angle += (rng.nextDouble() - 0.5) * difficulty.steerJitter * 2;

        right = Math.cos(angle) > 0.1;
        left  = Math.cos(angle) < -0.1;
        down  = Math.sin(angle) > 0.1;
        up    = Math.sin(angle) < -0.1;
    }

    /**
     * Navigation helper: scans nearby cells and steers the NPC.
     * - Flee from cells bigger than this NPC (weighted by proximity and size difference)
     * - Chase cells smaller than this NPC (food cells, smaller NPCs, and smaller player)
     * @return true if navigation found something to react to, false for random fallback
     */
    private boolean navigate(Cell playerCell, CopyOnWriteArrayList<NPC> npcList, CopyOnWriteArrayList<Cell> foodList) {
        double myCX = cell.x + cell.cellRad;
        double myCY = cell.y + cell.cellRad;
        double myRad = cell.cellRad;
        int visionRange = difficulty.visionRange;

        // Accumulated steering vector
        double steerX = 0;
        double steerY = 0;
        boolean hasInput = false;

        // Check player as potential threat or prey
        {
            double pCX = playerCell.x + playerCell.cellRad;
            double pCY = playerCell.y + playerCell.cellRad;
            double dx = pCX - myCX;
            double dy = pCY - myCY;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 0 && dist < visionRange) {
                double pRad = playerCell.cellRad;
                if (pRad > myRad + 0.5) {
                    // Player is bigger — FLEE (stronger force when closer)
                    double weight = 3.0 * (visionRange - dist) / visionRange;
                    steerX -= weight * (dx / dist);
                    steerY -= weight * (dy / dist);
                    hasInput = true;
                } else if (myRad > pRad + 0.5 && !distracted) {
                    // Player is smaller — CHASE (unless distracted)
                    double weight = 2.5 * (visionRange - dist) / visionRange;
                    steerX += weight * (dx / dist);
                    steerY += weight * (dy / dist);
                    hasInput = true;
                }
            }
        }

        // Check other NPCs as threats or prey
        for (NPC other : npcList) {
            if (other == this || !other.alive) continue;
            double oCX = other.cell.x + other.cell.cellRad;
            double oCY = other.cell.y + other.cell.cellRad;
            double dx = oCX - myCX;
            double dy = oCY - myCY;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 0 && dist < visionRange) {
                double oRad = other.cell.cellRad;
                if (oRad > myRad + 0.5) {
                    // Other NPC is bigger — FLEE
                    double weight = 3.0 * (visionRange - dist) / visionRange;
                    steerX -= weight * (dx / dist);
                    steerY -= weight * (dy / dist);
                    hasInput = true;
                } else if (myRad > oRad + 0.5 && !distracted) {
                    // Other NPC is smaller — CHASE (unless distracted)
                    double weight = 2.0 * (visionRange - dist) / visionRange;
                    steerX += weight * (dx / dist);
                    steerY += weight * (dy / dist);
                    hasInput = true;
                }
            }
        }

        // Check food cells as prey — weight scales with difficulty's food aggression
        for (Cell food : foodList) {
            double fCX = food.x + food.cellRad;
            double fCY = food.y + food.cellRad;
            double dx = fCX - myCX;
            double dy = fCY - myCY;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 0 && dist < visionRange && myRad > food.cellRad + 0.5 && !distracted) {
                double weight = difficulty.foodWeight * (visionRange - dist) / visionRange;
                steerX += weight * (dx / dist);
                steerY += weight * (dy / dist);
                hasInput = true;
            }
        }

        if (!hasInput) return false;

        // Add angular jitter to make movement less robotic
        double steerAngle = Math.atan2(steerY, steerX);
        double steerMag = Math.sqrt(steerX * steerX + steerY * steerY);
        steerAngle += (rng.nextDouble() - 0.5) * difficulty.steerJitter * 2;
        steerX = steerMag * Math.cos(steerAngle);
        steerY = steerMag * Math.sin(steerAngle);

        // Convert steering vector to direction flags
        right = steerX > 0.1;
        left  = steerX < -0.1;
        up    = steerY < -0.1;
        down  = steerY > 0.1;

        return true;
    }

    /**
     * Grows this NPC by eating a cell of the given radius, using area-based growth.
     * r3 = sqrt(r1^2 + r2^2) — total area is conserved.
     * Score is set to current radius after growth.
     * @param eatenRad radius of the eaten cell
     */
    public void grow(double eatenRad) {
        double newRad = GameConstants.growRadius(cell.cellRad, eatenRad);
        cell.cellRad = newRad;
        score = GameConstants.scoreFromRadius(newRad);
    }
}
