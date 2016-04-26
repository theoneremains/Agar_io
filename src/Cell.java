
import java.awt.*;

/**
 * Cell class : Creates cells, checks the collision of cells
 * Updates the position of cells
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
        cell.setColor(cellColor);
        cell.fillOval(x, y, cellRad*2, cellRad*2);
    }

    public void updateCellPos(boolean right, boolean left, boolean up, boolean down)
    {
        if(right)
        {
            if(x + cellRad < MainClass.SCREEN_WIDTH)
                x += speedX;
        }
        if(left)
        {
            if(x > -cellRad)
                x -= speedX;
        }
        if(up)
        {
            if(y > -cellRad)
                y -= speedY;
        }
        if(down)
        {
            if(y + cellRad < MainClass.SCREEN_HEIGHT)
                y += speedY;
        }
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
