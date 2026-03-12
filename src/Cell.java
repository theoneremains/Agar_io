
import java.awt.*;
import java.awt.RenderingHints;

/**
 * Cell class : Creates cells, checks the collision of cells
 * Updates the position of cells
 * Supports smooth spawn animation via spawnAlpha (0=invisible, 1=fully visible)
 * Player cell wraps toroidally around world boundaries instead of stopping
 * Cell radius and speed are double-precision for smooth area-based growth
 * Eating requires 2x area advantage; smaller advantage triggers cell division
 * @author Kamil Yunus Özkaya
 */
public class Cell
{
    public double speedX = 5;
    public double speedY = 5;
    public double cellRad;
    public double x;
    public double y;
    public int screenWidth;
    public int screenHeight;
    private double radiusDifference = 0.5;
    public Color cellColor = Color.BLACK;
    /** Spawn animation progress: 0 = just created (invisible), 1 = fully visible */
    public float spawnAlpha = 0f;
    public Cell(int screenWidth, int screenHeight, double cellRad)
    {
        this.cellRad = cellRad;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.x = (screenWidth - cellRad);
        this.y = (screenHeight - cellRad);
    }

    public void drawCell(Graphics2D cell, int cellRad)
    {
        cell.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        cell.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);
        cell.setColor(cellColor);
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        cell.fillOval(ix, iy, cellRad*2, cellRad*2);
    }

    public void updateCellPos(boolean right, boolean left, boolean up, boolean down)
    {
        if(right) x += speedX;
        if(left)  x -= speedX;
        if(up)    y -= speedY;
        if(down)  y += speedY;

        // Toroidal world wrapping: player emerges from opposite side when reaching any edge
        double cx = ((x + cellRad) % MainClass.WORLD_WIDTH  + MainClass.WORLD_WIDTH)  % MainClass.WORLD_WIDTH;
        double cy = ((y + cellRad) % MainClass.WORLD_HEIGHT + MainClass.WORLD_HEIGHT) % MainClass.WORLD_HEIGHT;
        x = cx - cellRad;
        y = cy - cellRad;
    }
    /** Minimum radius a cell must have to be eligible for division */
    private static final double MIN_DIVIDE_RADIUS = 0.7;

    /**
     * Checks collision for eating: requires the eater's area to be at least 2x the prey's area,
     * and the prey's center must be within the eater's circle.
     */
    public boolean isCollision (Cell playerCell, Cell randomCell){
        if(playerCell.canEat(randomCell)){
            double playerX,playerY,randomX,randomY;
            playerX = playerCell.getX() + playerCell.cellRad;
            playerY = playerCell.getY() + playerCell.cellRad;
            randomX = randomCell.getX() + randomCell.cellRad;
            randomY = randomCell.getY() + randomCell.cellRad;
            if(Math.pow(randomX - playerX,2) + Math.pow(randomY - playerY,2) <= Math.pow(playerCell.cellRad,2))
                return true;
        }
        return false;
    }

    /**
     * Whether this cell can eat the other cell directly (area >= 2x other's area).
     * @param other the potential prey cell
     * @return true if this cell's area is at least 2x the other's area
     */
    public boolean canEat(Cell other) {
        return cellRad * cellRad >= 2.0 * other.cellRad * other.cellRad;
    }

    /**
     * Whether this cell can trigger division on the other cell.
     * Division occurs when this cell is bigger but has less than 2x the area,
     * and the target cell is large enough to divide.
     * @param other the potential division target
     * @return true if division is possible
     */
    public boolean canDivide(Cell other) {
        if (other.cellRad < MIN_DIVIDE_RADIUS) return false;
        double myArea = cellRad * cellRad;
        double otherArea = other.cellRad * other.cellRad;
        return myArea > otherArea * 1.01 && myArea < 2.0 * otherArea;
    }

    /**
     * Whether this cell is physically touching (overlapping) the other cell.
     * Used for division contact tracking.
     * @param other the other cell to check against
     * @return true if the circles overlap
     */
    public boolean isTouching(Cell other) {
        double cx1 = x + cellRad;
        double cy1 = y + cellRad;
        double cx2 = other.x + other.cellRad;
        double cy2 = other.y + other.cellRad;
        double dx = cx2 - cx1;
        double dy = cy2 - cy1;
        return Math.sqrt(dx * dx + dy * dy) < cellRad + other.cellRad;
    }
    public double getX()
    {
        return x;
    }
    public double getY(){
        return y;
    }
}
