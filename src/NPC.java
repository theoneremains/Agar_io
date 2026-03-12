
import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NPC class : Represents an AI-controlled cell that navigates the world intelligently,
 * fleeing from cells bigger than it and chasing cells smaller than it (including the player).
 * Eats smaller food cells and other NPCs, and tracks its own score.
 * NPCs have random names and behave like autonomous players with survival instincts.
 * @author Kamil Yunus Özkaya
 */
public class NPC {
    /** The cell entity representing this NPC in the world */
    public Cell cell;

    /** Display name shown inside the NPC cell */
    public String name;

    /** Score accumulated by eating cells (increased by eaten cell's radius) */
    public int score = 0;

    /** Whether this NPC is still alive */
    public boolean alive = true;

    // Movement direction flags (set by navigation AI)
    private boolean right, left, up, down;

    // Ticks until next direction change (used only for random fallback)
    private int directionTimer = 0;

    private static final Random rng = new Random();

    // Speed constants — fixed speed of 3 (no longer scales with size)
    private static final double DEFAULT_SPEED = 3;

    // Navigation constants
    /** How far the NPC can "see" threats and prey */
    private static final int VISION_RANGE = 400;

    /** Chance (0–1) per tick that the NPC ignores its navigation and makes a random move */
    private static final double ERROR_CHANCE = 0.08;

    /** Maximum angular jitter (radians) added to the steering vector each tick */
    private static final double STEER_JITTER = 0.45;

    /** Chance per tick that an NPC spontaneously changes behaviour (e.g. stops chasing) */
    private static final double MOOD_CHANGE_CHANCE = 0.005;

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
     * Creates a new NPC with random name and color at the given position.
     * @param cx center X in world coordinates
     * @param cy center Y in world coordinates
     * @param radius initial cell radius
     * @param usedNames names already taken (to avoid duplicates)
     */
    public NPC(int cx, int cy, double radius, java.util.Set<String> usedNames) {
        this.cell = new Cell(cx, cy, radius);
        this.cell.spawnAlpha = 1f;
        this.cell.cellColor = GamePanel.colors[rng.nextInt(GamePanel.colors.length)];

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

    /** Updates the NPC's speed — fixed at DEFAULT_SPEED (no size scaling) */
    public void updateSpeed() {
        cell.speedX = DEFAULT_SPEED;
        cell.speedY = DEFAULT_SPEED;
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
        if (!distracted && rng.nextDouble() < MOOD_CHANGE_CHANCE) {
            distracted = true;
            distractedTimer = 60 + rng.nextInt(120); // distracted for 60–180 ticks
        }
        if (distracted) {
            distractedTimer--;
            if (distractedTimer <= 0) distracted = false;
        }

        // Random error: occasionally ignore navigation entirely and move randomly
        if (rng.nextDouble() < ERROR_CHANCE) {
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
        angle += (rng.nextDouble() - 0.5) * STEER_JITTER * 2;

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

            if (dist > 0 && dist < VISION_RANGE) {
                double pRad = playerCell.cellRad;
                if (pRad > myRad + 0.5) {
                    // Player is bigger — FLEE (stronger force when closer)
                    double weight = 3.0 * (VISION_RANGE - dist) / VISION_RANGE;
                    steerX -= weight * (dx / dist);
                    steerY -= weight * (dy / dist);
                    hasInput = true;
                } else if (myRad > pRad + 0.5 && !distracted) {
                    // Player is smaller — CHASE (unless distracted)
                    double weight = 2.0 * (VISION_RANGE - dist) / VISION_RANGE;
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

            if (dist > 0 && dist < VISION_RANGE) {
                double oRad = other.cell.cellRad;
                if (oRad > myRad + 0.5) {
                    // Other NPC is bigger — FLEE
                    double weight = 3.0 * (VISION_RANGE - dist) / VISION_RANGE;
                    steerX -= weight * (dx / dist);
                    steerY -= weight * (dy / dist);
                    hasInput = true;
                } else if (myRad > oRad + 0.5 && !distracted) {
                    // Other NPC is smaller — CHASE (unless distracted)
                    double weight = 1.5 * (VISION_RANGE - dist) / VISION_RANGE;
                    steerX += weight * (dx / dist);
                    steerY += weight * (dy / dist);
                    hasInput = true;
                }
            }
        }

        // Check food cells as prey (always smaller, low priority chase)
        for (Cell food : foodList) {
            double fCX = food.x + food.cellRad;
            double fCY = food.y + food.cellRad;
            double dx = fCX - myCX;
            double dy = fCY - myCY;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 0 && dist < VISION_RANGE && myRad > food.cellRad + 0.5 && !distracted) {
                double weight = 1.0 * (VISION_RANGE - dist) / VISION_RANGE;
                steerX += weight * (dx / dist);
                steerY += weight * (dy / dist);
                hasInput = true;
            }
        }

        if (!hasInput) return false;

        // Add angular jitter to make movement less robotic
        double steerAngle = Math.atan2(steerY, steerX);
        double steerMag = Math.sqrt(steerX * steerX + steerY * steerY);
        steerAngle += (rng.nextDouble() - 0.5) * STEER_JITTER * 2;
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
        double newRad = Math.sqrt(cell.cellRad * cell.cellRad + eatenRad * eatenRad);
        cell.cellRad = newRad;
        score = (int) Math.ceil(newRad);
    }
}
