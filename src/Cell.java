
import java.awt.*;
import java.awt.RenderingHints;

/**
 * Cell class : Creates cells, checks the collision of cells
 * Updates the position of cells
 * Supports smooth spawn animation via spawnAlpha (0=invisible, 1=fully visible)
 * Player cell wraps toroidally around world boundaries instead of stopping
 * @author Kamil Yunus Özkaya
 */
public class Cell
{
    public int speedX = 5;
    public int speedY = 5;
    public int cellRad;
    public int x;
    public int y;
    public int screenWidth;
    public int screenHeight;
    private int radiusDifference = 4;
    public Color cellColor = Color.BLACK;
    /** Spawn animation progress: 0 = just created (invisible), 1 = fully visible */
    public float spawnAlpha = 0f;
    public Cell(int screenWidth, int screenHeight, int cellRad)
    {
        this.cellRad = cellRad;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.x = (screenWidth - cellRad);
        this.y = (screenHeight - cellRad);
    }

    public void drawCell(Graphics2D cell,int cellRad)
    {
        cell.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        cell.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);
        cell.setColor(cellColor);
        cell.fillOval(x, y, cellRad*2, cellRad*2);
    }

    public void updateCellPos(boolean right, boolean left, boolean up, boolean down)
    {
        if(right) x += speedX;
        if(left)  x -= speedX;
        if(up)    y -= speedY;
        if(down)  y += speedY;

        // Toroidal world wrapping: player emerges from opposite side when reaching any edge
        int cx = ((x + cellRad) % MainClass.WORLD_WIDTH  + MainClass.WORLD_WIDTH)  % MainClass.WORLD_WIDTH;
        int cy = ((y + cellRad) % MainClass.WORLD_HEIGHT + MainClass.WORLD_HEIGHT) % MainClass.WORLD_HEIGHT;
        x = cx - cellRad;
        y = cy - cellRad;
    }
    //Checks the collision of the cells based on circle collision
    public boolean isCollision (Cell playerCell, Cell randomCell){
        if(playerCell.cellRad>randomCell.cellRad + radiusDifference){
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
    public int getX()
    {
        return x;
    }
    public int getY(){
        return y;
    }
}
