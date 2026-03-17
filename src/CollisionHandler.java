import java.awt.*;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CollisionHandler : Manages all collision detection and resolution logic,
 * including eating, division mechanics, and bounce effects.
 * Extracted from GamePanel to improve code organization and readability.
 * @author Kamil Yunus Ozkaya
 */
public class CollisionHandler {

    private final GamePanel game;

    /** Tracks sustained contact duration for division mechanics (cell pair → ticks) */
    private HashMap<Long, Integer> divisionContacts = new HashMap<>();

    /** Timestamp of last bounce effect to prevent spam */
    private long lastBounceTime = 0;

    public CollisionHandler(GamePanel game) {
        this.game = game;
    }

    /**
     * Runs all collision checks for a single game tick.
     * Handles: player↔food, player↔NPC, NPC↔food, NPC↔NPC, NPC↔player eating,
     * plus all division and bounce interactions.
     */
    public void update() {
        if (game.isGameOver()) return;

        if (!game.isEasterEggActive()) {
            checkPlayerEatsFood();
            checkPlayerEatsNPCs();
        }
        checkNPCsEatFood();
        checkNPCsEatNPCs();
        checkNPCsEatPlayer();
        updateDivisions();
        updateBounceEffects();
    }

    // ── Eating Collisions ────────────────────────────────────────────────

    private void checkPlayerEatsFood() {
        Cell player = game.getPlayerCell();
        CopyOnWriteArrayList<Cell> foodCells = game.getFoodCells();

        for (int i = 0; i < foodCells.size(); i++) {
            if (i >= foodCells.size()) break;
            Cell food = foodCells.get(i);
            if (Cell.checkEatCollision(player, food)) {
                double eatenRad = food.cellRad;
                double eatCX = food.getCenterX();
                double eatCY = food.getCenterY();
                Color eatColor = food.cellColor;
                foodCells.remove(i);
                Sound.playEatSound();
                game.getEatEffects().add(new EatEffect(eatCX, eatCY, eatenRad, eatColor));
                player.cellRad = GameConstants.growRadius(player.cellRad, eatenRad);
                game.updatePlayerScore();
                i--;
            }
        }
    }

    private void checkPlayerEatsNPCs() {
        Cell player = game.getPlayerCell();

        for (NPC npc : game.getNPCList()) {
            if (!npc.alive) continue;
            if (Cell.checkEatCollision(player, npc.cell)) {
                double eatenRad = npc.cell.cellRad;
                npc.alive = false;
                npc.score = GameConstants.scoreFromRadius(npc.cell.cellRad);
                Sound.playEatSound();
                game.getEatEffects().add(new EatEffect(
                    npc.cell.getCenterX(), npc.cell.getCenterY(), eatenRad, npc.cell.cellColor));
                player.cellRad = GameConstants.growRadius(player.cellRad, eatenRad);
                game.updatePlayerScore();
            }
        }
    }

    private void checkNPCsEatFood() {
        CopyOnWriteArrayList<Cell> foodCells = game.getFoodCells();

        for (NPC npc : game.getNPCList()) {
            if (!npc.alive) continue;
            for (int i = 0; i < foodCells.size(); i++) {
                if (i >= foodCells.size()) break;
                Cell food = foodCells.get(i);
                if (Cell.checkEatCollision(npc.cell, food)) {
                    double eatenRad = food.cellRad;
                    Color eatColor = food.cellColor;
                    foodCells.remove(i);
                    npc.grow(eatenRad);
                    game.getEatEffects().add(new EatEffect(
                        food.getCenterX(), food.getCenterY(), eatenRad, eatColor));
                    i--;
                }
            }
        }
    }

    private void checkNPCsEatNPCs() {
        CopyOnWriteArrayList<NPC> npcList = game.getNPCList();

        for (NPC predator : npcList) {
            if (!predator.alive) continue;
            for (NPC prey : npcList) {
                if (!prey.alive || predator == prey) continue;
                if (Cell.checkEatCollision(predator.cell, prey.cell)) {
                    double eatenRad = prey.cell.cellRad;
                    prey.alive = false;
                    prey.score = GameConstants.scoreFromRadius(prey.cell.cellRad);
                    predator.grow(eatenRad);
                    game.getEatEffects().add(new EatEffect(
                        prey.cell.getCenterX(), prey.cell.getCenterY(), eatenRad, prey.cell.cellColor));
                }
            }
        }
    }

    private void checkNPCsEatPlayer() {
        if (game.isGameOver()) return;
        Cell player = game.getPlayerCell();

        for (NPC npc : game.getNPCList()) {
            if (!npc.alive) continue;
            if (Cell.checkEatCollision(npc.cell, player)) {
                game.onPlayerEaten(npc);
                break;
            }
        }
    }

    // ── Division Mechanics ───────────────────────────────────────────────

    private void updateDivisions() {
        if (game.isGameOver()) return;

        HashMap<Long, Integer> newContacts = new HashMap<>();
        Cell player = game.getPlayerCell();
        CopyOnWriteArrayList<Cell> foodCells = game.getFoodCells();
        CopyOnWriteArrayList<NPC> npcList = game.getNPCList();

        // Player divides food cells
        for (int i = 0; i < foodCells.size(); i++) {
            if (i >= foodCells.size()) break;
            Cell food = foodCells.get(i);
            if (player.canDivide(food) && player.isTouching(food)) {
                long key = contactKey(player, food);
                int ticks = divisionContacts.getOrDefault(key, 0) + 1;
                if (ticks >= GameConstants.DIVISION_CONTACT_TICKS) {
                    divideFoodCell(i, player);
                    i--;
                } else {
                    newContacts.put(key, ticks);
                    spawnDivisionContactEffect(player, food, ticks);
                }
            }
        }

        // Player divides NPC cells
        for (NPC npc : npcList) {
            if (!npc.alive) continue;
            if (player.canDivide(npc.cell) && player.isTouching(npc.cell)) {
                long key = contactKey(player, npc.cell);
                int ticks = divisionContacts.getOrDefault(key, 0) + 1;
                if (ticks >= GameConstants.DIVISION_CONTACT_TICKS) {
                    divideEntityCell(npc.cell, player, npc);
                } else {
                    newContacts.put(key, ticks);
                }
            }
        }

        // NPCs divide food cells
        for (NPC npc : npcList) {
            if (!npc.alive) continue;
            for (int i = 0; i < foodCells.size(); i++) {
                if (i >= foodCells.size()) break;
                Cell food = foodCells.get(i);
                if (npc.cell.canDivide(food) && npc.cell.isTouching(food)) {
                    long key = contactKey(npc.cell, food);
                    int ticks = divisionContacts.getOrDefault(key, 0) + 1;
                    if (ticks >= GameConstants.DIVISION_CONTACT_TICKS) {
                        divideFoodCell(i, npc.cell);
                        i--;
                    } else {
                        newContacts.put(key, ticks);
                    }
                }
            }
        }

        // NPCs divide other NPCs
        for (NPC predator : npcList) {
            if (!predator.alive) continue;
            for (NPC prey : npcList) {
                if (!prey.alive || predator == prey) continue;
                if (predator.cell.canDivide(prey.cell) && predator.cell.isTouching(prey.cell)) {
                    long key = contactKey(predator.cell, prey.cell);
                    int ticks = divisionContacts.getOrDefault(key, 0) + 1;
                    if (ticks >= GameConstants.DIVISION_CONTACT_TICKS) {
                        divideEntityCell(prey.cell, predator.cell, prey);
                    } else {
                        newContacts.put(key, ticks);
                    }
                }
            }
        }

        // NPCs divide the player
        if (!game.isGameOver()) {
            for (NPC npc : npcList) {
                if (!npc.alive) continue;
                if (npc.cell.canDivide(player) && npc.cell.isTouching(player)) {
                    long key = contactKey(npc.cell, player);
                    int ticks = divisionContacts.getOrDefault(key, 0) + 1;
                    if (ticks >= GameConstants.DIVISION_CONTACT_TICKS) {
                        dividePlayerCell(npc.cell);
                    } else {
                        newContacts.put(key, ticks);
                    }
                }
            }
        }

        divisionContacts = newContacts;
    }

    // ── Bounce Effects ───────────────────────────────────────────────────

    private void updateBounceEffects() {
        long now = System.currentTimeMillis();
        if (now - lastBounceTime < GameConstants.BOUNCE_COOLDOWN_MS) return;

        Cell player = game.getPlayerCell();
        CopyOnWriteArrayList<ContactEffect> effects = game.getContactEffects();

        // Bounce: player touches food it can't eat or divide
        for (Cell food : game.getFoodCells()) {
            if (!player.canEat(food) && !player.canDivide(food)
                && food.cellRad > player.cellRad + 0.5 && player.isTouching(food)) {
                if (effects.size() < GameConstants.MAX_CONTACT_EFFECTS) {
                    double midX = (player.getCenterX() + food.getCenterX()) / 2;
                    double midY = (player.getCenterY() + food.getCenterY()) / 2;
                    effects.add(new ContactEffect(midX, midY,
                        Math.min(player.cellRad, food.cellRad), new Color(255, 200, 100), false));
                    Sound.playBounceSound();
                    lastBounceTime = now;
                    return; // One bounce per cooldown
                }
            }
        }

        // Bounce: player touches NPC it can't eat or divide
        for (NPC npc : game.getNPCList()) {
            if (!npc.alive) continue;
            if (!player.canEat(npc.cell) && !player.canDivide(npc.cell)
                && npc.cell.cellRad > player.cellRad + 0.5 && player.isTouching(npc.cell)) {
                if (effects.size() < GameConstants.MAX_CONTACT_EFFECTS) {
                    double midX = (player.getCenterX() + npc.cell.getCenterX()) / 2;
                    double midY = (player.getCenterY() + npc.cell.getCenterY()) / 2;
                    effects.add(new ContactEffect(midX, midY,
                        Math.min(player.cellRad, npc.cell.cellRad), new Color(255, 100, 100), false));
                    Sound.playBounceSound();
                    lastBounceTime = now;
                    return;
                }
            }
        }
    }

    // ── Division Helpers ─────────────────────────────────────────────────

    /**
     * Generates a unique contact key for a pair of cells.
     */
    private static long contactKey(Cell attacker, Cell target) {
        return ((long) System.identityHashCode(attacker) << 32)
             | (System.identityHashCode(target) & 0xFFFFFFFFL);
    }

    /**
     * Computes division geometry: angle, positions of two halves.
     */
    private static double[] computeDivisionGeometry(Cell target, Cell attacker) {
        double cellCX = target.getCenterX();
        double cellCY = target.getCenterY();
        double attackCX = attacker.getCenterX();
        double attackCY = attacker.getCenterY();

        double contactAngle = Math.atan2(cellCY - attackCY, cellCX - attackCX);
        double divAngle = contactAngle + Math.PI / 2;
        double moveDist = target.cellRad * GameConstants.DIVISION_SEPARATION;

        return new double[] {
            cellCX, cellCY,
            cellCX + Math.cos(divAngle) * moveDist,   // posAX
            cellCY + Math.sin(divAngle) * moveDist,   // posAY
            cellCX - Math.cos(divAngle) * moveDist,   // posBX
            cellCY - Math.sin(divAngle) * moveDist,   // posBY
            divAngle
        };
    }

    private void divideFoodCell(int foodIndex, Cell attacker) {
        CopyOnWriteArrayList<Cell> foodCells = game.getFoodCells();
        Cell food = foodCells.get(foodIndex);
        double newRad = food.cellRad / Math.sqrt(2);

        double[] geo = computeDivisionGeometry(food, attacker);
        double cellCX = geo[0], cellCY = geo[1];
        double posAX = geo[2], posAY = geo[3];
        double posBX = geo[4], posBY = geo[5];
        double divAngle = geo[6];

        game.getDivisionEffects().add(new DivisionEffect(
            cellCX, cellCY, posAX, posAY, posBX, posBY, newRad, food.cellColor, divAngle));
        foodCells.remove(foodIndex);

        Cell halfA = new Cell((int) posAX, (int) posAY, newRad);
        halfA.cellColor = food.cellColor;
        halfA.spawnAlpha = 1f;
        Cell halfB = new Cell((int) posBX, (int) posBY, newRad);
        halfB.cellColor = food.cellColor;
        halfB.spawnAlpha = 1f;
        foodCells.add(halfA);
        foodCells.add(halfB);

        Sound.playDivisionSound();
    }

    private void divideEntityCell(Cell targetCell, Cell attacker, NPC targetNpc) {
        double newRad = targetCell.cellRad / Math.sqrt(2);

        double[] geo = computeDivisionGeometry(targetCell, attacker);
        double cellCX = geo[0], cellCY = geo[1];
        double posAX = geo[2], posAY = geo[3];
        double posBX = geo[4], posBY = geo[5];
        double divAngle = geo[6];

        boolean aIsSafer = isPositionASafer(posAX, posAY, posBX, posBY, newRad, attacker);
        double safeX  = aIsSafer ? posAX : posBX;
        double safeY  = aIsSafer ? posAY : posBY;
        double otherX = aIsSafer ? posBX : posAX;
        double otherY = aIsSafer ? posBY : posAY;

        game.getDivisionEffects().add(new DivisionEffect(
            cellCX, cellCY, posAX, posAY, posBX, posBY, newRad, targetCell.cellColor, divAngle));

        targetCell.cellRad = newRad;
        targetCell.x = safeX - newRad;
        targetCell.y = safeY - newRad;
        if (targetNpc != null) {
            targetNpc.updateSpeed();
            targetNpc.score = GameConstants.scoreFromRadius(newRad);
        }

        Cell foodHalf = new Cell((int) otherX, (int) otherY, newRad);
        foodHalf.cellColor = targetCell.cellColor;
        foodHalf.spawnAlpha = 1f;
        game.getFoodCells().add(foodHalf);

        Sound.playDivisionSound();
    }

    private void dividePlayerCell(Cell attacker) {
        Cell player = game.getPlayerCell();
        double newRad = player.cellRad / Math.sqrt(2);

        double[] geo = computeDivisionGeometry(player, attacker);
        double cellCX = geo[0], cellCY = geo[1];
        double posAX = geo[2], posAY = geo[3];
        double posBX = geo[4], posBY = geo[5];
        double divAngle = geo[6];

        boolean aIsSafer = isPositionASafer(posAX, posAY, posBX, posBY, newRad, attacker);
        double safeX  = aIsSafer ? posAX : posBX;
        double safeY  = aIsSafer ? posAY : posBY;
        double otherX = aIsSafer ? posBX : posAX;
        double otherY = aIsSafer ? posBY : posAY;

        game.getDivisionEffects().add(new DivisionEffect(
            cellCX, cellCY, posAX, posAY, posBX, posBY, newRad, player.cellColor, divAngle));

        player.cellRad = newRad;
        player.x = safeX - newRad;
        player.y = safeY - newRad;
        game.updatePlayerScore();

        Cell foodHalf = new Cell((int) otherX, (int) otherY, newRad);
        foodHalf.cellColor = player.cellColor;
        foodHalf.spawnAlpha = 1f;
        game.getFoodCells().add(foodHalf);

        Sound.playDivisionSound();
    }

    private boolean isPositionASafer(double posAX, double posAY, double posBX, double posBY,
                                      double newRadius, Cell excludeCell) {
        double dangerA = 0;
        double dangerB = 0;
        double newArea = newRadius * newRadius;
        Cell player = game.getPlayerCell();

        if (excludeCell != player && !game.isGameOver()) {
            double pArea = player.cellRad * player.cellRad;
            if (pArea > newArea) {
                double pCX = player.getCenterX();
                double pCY = player.getCenterY();
                dangerA += pArea / Math.max(1, GameConstants.distSq(posAX, posAY, pCX, pCY));
                dangerB += pArea / Math.max(1, GameConstants.distSq(posBX, posBY, pCX, pCY));
            }
        }

        for (NPC npc : game.getNPCList()) {
            if (!npc.alive || npc.cell == excludeCell) continue;
            double nArea = npc.cell.cellRad * npc.cell.cellRad;
            if (nArea > newArea) {
                double nCX = npc.cell.getCenterX();
                double nCY = npc.cell.getCenterY();
                dangerA += nArea / Math.max(1, GameConstants.distSq(posAX, posAY, nCX, nCY));
                dangerB += nArea / Math.max(1, GameConstants.distSq(posBX, posBY, nCX, nCY));
            }
        }

        return dangerA <= dangerB;
    }

    private void spawnDivisionContactEffect(Cell attacker, Cell target, int ticks) {
        CopyOnWriteArrayList<ContactEffect> effects = game.getContactEffects();
        if (ticks % 30 == 1 && effects.size() < GameConstants.MAX_DIVISION_EFFECTS) {
            double midX = (attacker.getCenterX() + target.getCenterX()) / 2;
            double midY = (attacker.getCenterY() + target.getCenterY()) / 2;
            effects.add(new ContactEffect(midX, midY,
                Math.min(attacker.cellRad, target.cellRad), target.cellColor, true));
        }
    }
}
