
import java.awt.*;
import java.util.Random;

/**
 * NPC class : Represents an AI-controlled cell that moves randomly around the world,
 * eats smaller food cells and other NPCs, and tracks its own score.
 * NPCs have random names and behave like autonomous players.
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

    // Movement direction flags (randomized periodically)
    private boolean right, left, up, down;

    // Ticks until next direction change
    private int directionTimer = 0;

    private static final Random rng = new Random();

    // Dynamic speed constants (same as player)
    private static final int BASE_SPEED  = 7;
    private static final int INITIAL_RAD = 18;
    private static final int MIN_SPEED   = 3;

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
    public NPC(int cx, int cy, int radius, java.util.Set<String> usedNames) {
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

    /** Updates the NPC's speed based on its current radius (same formula as player) */
    public void updateSpeed() {
        int dynSpeed = Math.max(MIN_SPEED, (int)(BASE_SPEED * (double) INITIAL_RAD / cell.cellRad));
        cell.speedX = dynSpeed;
        cell.speedY = dynSpeed;
    }

    /** Randomizes movement direction */
    private void randomizeDirection() {
        right = rng.nextBoolean();
        left  = !right && rng.nextBoolean();
        up    = rng.nextBoolean();
        down  = !up && rng.nextBoolean();
        directionTimer = 30 + rng.nextInt(70); // change direction every 30-100 ticks
    }

    /**
     * Updates NPC position and movement AI each tick.
     * Periodically changes direction randomly.
     */
    public void update() {
        if (!alive) return;

        directionTimer--;
        if (directionTimer <= 0) {
            randomizeDirection();
        }

        updateSpeed();
        cell.updateCellPos(right, left, up, down);
    }

    /**
     * Grows this NPC by eating a cell of the given radius, using volume-based growth.
     * @param eatenRad radius of the eaten cell
     */
    public void grow(int eatenRad) {
        int newRad = (int) Math.pow(Math.pow(cell.cellRad, 3) + Math.pow(eatenRad, 3), 1.0 / 3);
        if (newRad <= cell.cellRad) newRad = cell.cellRad + eatenRad;
        cell.cellRad = newRad;
        score += eatenRad;
    }
}
