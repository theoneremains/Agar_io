
import java.awt.*;

/**
 * Cell class : The fundamental entity in the game world.
 * Represents both food cells and the basis for player/NPC cells.
 * Supports smooth spawn animation, toroidal world wrapping, and area-based collision.
 * Cell radius and coordinates are double-precision for smooth area-based growth.
 * @author Kamil Yunus Özkaya
 */
public class Cell {

    public double speedX = 5;
    public double speedY = 5;
    public double cellRad;
    public double x;
    public double y;
    public Color cellColor = Color.BLACK;

    /** Spawn animation progress: 0 = just created (invisible), 1 = fully visible */
    public float spawnAlpha = 0f;

    /**
     * Creates a cell at the given center position with the specified radius.
     * @param centerX center X in world coordinates
     * @param centerY center Y in world coordinates
     * @param radius  the cell radius
     */
    public Cell(int centerX, int centerY, double radius) {
        this.cellRad = radius;
        this.x = centerX - radius;
        this.y = centerY - radius;
    }

    /** Returns the center X coordinate */
    public double getCenterX() {
        return x + cellRad;
    }

    /** Returns the center Y coordinate */
    public double getCenterY() {
        return y + cellRad;
    }

    /**
     * Draws this cell as a filled circle at its world position.
     * @param g2d     the graphics context
     * @param drawRad the visual radius to use (may differ from cellRad during animations)
     */
    public void drawCell(Graphics2D g2d, int drawRad) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(cellColor);
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        g2d.fillOval(ix, iy, drawRad * 2, drawRad * 2);
    }

    /**
     * Updates position based on movement flags, then wraps toroidally.
     * @param right moving right
     * @param left  moving left
     * @param up    moving up
     * @param down  moving down
     */
    public void updateCellPos(boolean right, boolean left, boolean up, boolean down) {
        if (right) x += speedX;
        if (left)  x -= speedX;
        if (up)    y -= speedY;
        if (down)  y += speedY;

        // Toroidal world wrapping
        double cx = ((x + cellRad) % MainClass.WORLD_WIDTH  + MainClass.WORLD_WIDTH)  % MainClass.WORLD_WIDTH;
        double cy = ((y + cellRad) % MainClass.WORLD_HEIGHT + MainClass.WORLD_HEIGHT) % MainClass.WORLD_HEIGHT;
        x = cx - cellRad;
        y = cy - cellRad;
    }

    // ── Collision Detection ──────────────────────────────────────────────

    /**
     * Checks whether the eater can eat the prey.
     * Requires eater's area >= 2x prey's area, and prey's center must be inside eater's circle.
     * @param eater the eating cell
     * @param prey  the cell being eaten
     * @return true if the eater can eat the prey at this position
     */
    public static boolean checkEatCollision(Cell eater, Cell prey) {
        if (!eater.canEat(prey)) return false;
        double dx = prey.getCenterX() - eater.getCenterX();
        double dy = prey.getCenterY() - eater.getCenterY();
        return dx * dx + dy * dy <= eater.cellRad * eater.cellRad;
    }

    /**
     * Whether this cell's area is at least 2x the other's area (can eat outright).
     */
    public boolean canEat(Cell other) {
        return cellRad * cellRad >= 2.0 * other.cellRad * other.cellRad;
    }

    /**
     * Whether this cell can trigger division on the other cell.
     * Division occurs when this cell is bigger but less than 2x the area,
     * and the target cell is large enough to divide.
     */
    public boolean canDivide(Cell other) {
        if (other.cellRad < GameConstants.MIN_DIVIDE_RADIUS) return false;
        double myArea = cellRad * cellRad;
        double otherArea = other.cellRad * other.cellRad;
        return myArea > otherArea * 1.01 && myArea < 2.0 * otherArea;
    }

    /**
     * Whether this cell is physically touching (overlapping) the other cell.
     */
    public boolean isTouching(Cell other) {
        double dx = other.getCenterX() - getCenterX();
        double dy = other.getCenterY() - getCenterY();
        return Math.sqrt(dx * dx + dy * dy) < cellRad + other.cellRad;
    }
}
