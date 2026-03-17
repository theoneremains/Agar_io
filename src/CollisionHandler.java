import java.awt.*;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CollisionHandler : Manages all collision detection and resolution logic,
 * including eating, shave/erosion mechanics, and bounce effects.
 *
 * <p><strong>Shave mechanic</strong> replaces the old sustained-contact division:
 * when a slightly-larger cell overlaps a smaller one (within the canDivide zone),
 * the smaller cell continuously loses area proportional to the overlap depth and
 * the attacker's speed.  Lost area accumulates and spawns as food cells at the
 * contact point once a threshold is reached.  Split Shield upgrades reduce the
 * shave damage taken.
 *
 * @author Kamil Yunus Ozkaya
 */
public class CollisionHandler {

    private final GamePanel game;

    /**
     * Accumulated shaved area per cell-pair.
     * Key = contactKey(attacker, target), value = accumulated area.
     * When accumulated area exceeds {@link GameConstants#SHAVE_MIN_FOOD_AREA},
     * a food cell is spawned at the contact point and the accumulator resets.
     */
    private HashMap<Long, double[]> shaveAccumulators = new HashMap<>();

    /** Timestamp of last bounce effect to prevent spam */
    private long lastBounceTime = 0;

    /** Tick counter for spacing out contact visual effects */
    private int shaveTickCounter = 0;

    public CollisionHandler(GamePanel game) {
        this.game = game;
    }

    /**
     * Runs all collision checks for a single game tick.
     * Handles: player↔food, player↔NPC, NPC↔food, NPC↔NPC, NPC↔player eating,
     * plus shave (erosion) and bounce interactions.
     */
    public void update() {
        if (game.isGameOver()) return;

        checkPlayerEatsFood();
        checkPlayerEatsNPCs();
        checkNPCsEatFood();
        checkNPCsEatNPCs();
        checkNPCsEatPlayer();
        updateShaving();
        updateBounceEffects();
        shaveTickCounter++;
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
                // Offer an upgrade for killing an NPC
                game.offerKillUpgrade();
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
                    // Bonus upgrade for the NPC that made the kill
                    predator.upgradeManager.triggerNPCKillUpgrade(predator);
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

    // ── Shave (Erosion) Mechanics ─────────────────────────────────────────

    /**
     * Processes all shave interactions for the current tick.
     * When a cell is slightly larger than another (canDivide zone) and they
     * overlap, the smaller cell loses area proportional to the overlap depth
     * and the attacker's speed.  Accumulated shaved area spawns as food at
     * the contact point.
     */
    private void updateShaving() {
        if (game.isGameOver()) return;

        HashMap<Long, double[]> newAccumulators = new HashMap<>();
        Cell player = game.getPlayerCell();
        CopyOnWriteArrayList<Cell> foodCells = game.getFoodCells();
        CopyOnWriteArrayList<NPC> npcList = game.getNPCList();

        // Player shaves food cells
        for (int i = 0; i < foodCells.size(); i++) {
            if (i >= foodCells.size()) break;
            Cell food = foodCells.get(i);
            if (player.canDivide(food) && player.isTouching(food)) {
                long key = contactKey(player, food);
                shaveTarget(food, player, null, false, key, newAccumulators);
                if (food.cellRad < GameConstants.MIN_DIVIDE_RADIUS) {
                    foodCells.remove(i);
                    i--;
                }
            }
        }

        // Player shaves NPCs
        for (NPC npc : npcList) {
            if (!npc.alive) continue;
            if (player.canDivide(npc.cell) && player.isTouching(npc.cell)) {
                long key = contactKey(player, npc.cell);
                shaveTarget(npc.cell, player, npc, false, key, newAccumulators);
            }
        }

        // NPCs shave food cells
        for (NPC npc : npcList) {
            if (!npc.alive) continue;
            for (int i = 0; i < foodCells.size(); i++) {
                if (i >= foodCells.size()) break;
                Cell food = foodCells.get(i);
                if (npc.cell.canDivide(food) && npc.cell.isTouching(food)) {
                    long key = contactKey(npc.cell, food);
                    shaveTarget(food, npc.cell, null, false, key, newAccumulators);
                    if (food.cellRad < GameConstants.MIN_DIVIDE_RADIUS) {
                        foodCells.remove(i);
                        i--;
                    }
                }
            }
        }

        // NPCs shave other NPCs
        for (NPC attacker : npcList) {
            if (!attacker.alive) continue;
            for (NPC target : npcList) {
                if (!target.alive || attacker == target) continue;
                if (attacker.cell.canDivide(target.cell) && attacker.cell.isTouching(target.cell)) {
                    long key = contactKey(attacker.cell, target.cell);
                    shaveTarget(target.cell, attacker.cell, target, false, key, newAccumulators);
                }
            }
        }

        // NPCs shave the player
        if (!game.isGameOver()) {
            for (NPC npc : npcList) {
                if (!npc.alive) continue;
                if (npc.cell.canDivide(player) && npc.cell.isTouching(player)) {
                    long key = contactKey(npc.cell, player);
                    shaveTarget(player, npc.cell, null, true, key, newAccumulators);
                }
            }
        }

        shaveAccumulators = newAccumulators;
    }

    /**
     * Core shave logic: erodes the target cell by an amount based on overlap
     * depth and attacker speed, accumulating debris for food cell spawning.
     *
     * @param target      the cell being eroded
     * @param attacker    the larger cell doing the eroding
     * @param targetNpc   the NPC owning target (null if food or player)
     * @param isPlayer    true when the target is the player cell
     * @param key         contact key for this pair
     * @param accumulators live accumulator map to update
     */
    private void shaveTarget(Cell target, Cell attacker, NPC targetNpc, boolean isPlayer,
                             long key, HashMap<Long, double[]> accumulators) {
        // Calculate overlap depth
        double dx = attacker.getCenterX() - target.getCenterX();
        double dy = attacker.getCenterY() - target.getCenterY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        double overlapDepth = (attacker.cellRad + target.cellRad) - dist;

        if (overlapDepth < GameConstants.SHAVE_OVERLAP_THRESHOLD) return;

        // Speed factor: faster attacker = more damage
        double attackerSpeed = Math.sqrt(attacker.speedX * attacker.speedX
                                       + attacker.speedY * attacker.speedY);
        double speedFactor = Math.max(0.5, attackerSpeed / GameConstants.DEFAULT_SPEED);

        // Shaved area this tick (multiplied by global shave-rate setting)
        double shavedArea = overlapDepth * speedFactor * GameConstants.SHAVE_RATE * GamePanel.shaveRateMultiplier;

        // Apply Split Shield damage reduction
        double shieldMultiplier;
        if (isPlayer) {
            shieldMultiplier = game.splitShieldFactor;
        } else if (targetNpc != null) {
            shieldMultiplier = targetNpc.splitShieldFactor;
        } else {
            shieldMultiplier = GameConstants.SPLIT_SHIELD_BASE; // food: no shield
        }
        shavedArea *= shieldMultiplier;

        // Apply erosion to target
        double targetArea = target.cellRad * target.cellRad;
        double newArea = targetArea - shavedArea;
        double minArea = GameConstants.MIN_DIVIDE_RADIUS * GameConstants.MIN_DIVIDE_RADIUS;
        if (newArea < minArea) newArea = minArea;
        double newRad = Math.sqrt(newArea);

        if (target.cellRad - newRad < 0.001) return; // negligible change

        target.cellRad = newRad;

        // Update score for the target
        if (isPlayer) {
            game.updatePlayerScore();
        } else if (targetNpc != null) {
            targetNpc.score = GameConstants.scoreFromRadius(newRad);
        }

        // Accumulate shaved area for food spawning
        double[] acc = shaveAccumulators.getOrDefault(key, new double[]{0});
        acc[0] += shavedArea;
        accumulators.put(key, acc);

        // Spawn food cell when enough area has accumulated
        if (acc[0] >= GameConstants.SHAVE_MIN_FOOD_AREA && dist > 0.01) {
            double foodRad = Math.sqrt(acc[0]);
            // Contact point on target's surface facing the attacker
            double contactX = target.getCenterX() + (dx / dist) * target.cellRad;
            double contactY = target.getCenterY() + (dy / dist) * target.cellRad;

            Cell foodCell = new Cell((int) contactX, (int) contactY, foodRad);
            foodCell.cellColor = target.cellColor;
            foodCell.spawnAlpha = 1f;
            game.getFoodCells().add(foodCell);

            // Small eat-effect burst at the contact point
            game.getEatEffects().add(new EatEffect(contactX, contactY, foodRad, target.cellColor));

            acc[0] = 0; // reset accumulator
        }

        // Periodic visual contact effect
        if (shaveTickCounter % GameConstants.SHAVE_EFFECT_INTERVAL == 0) {
            CopyOnWriteArrayList<ContactEffect> effects = game.getContactEffects();
            if (effects.size() < GameConstants.MAX_CONTACT_EFFECTS) {
                double midX = (attacker.getCenterX() + target.getCenterX()) / 2;
                double midY = (attacker.getCenterY() + target.getCenterY()) / 2;
                effects.add(new ContactEffect(midX, midY,
                    Math.min(attacker.cellRad, target.cellRad), target.cellColor, true));
            }
        }
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

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Generates a unique contact key for a pair of cells.
     */
    private static long contactKey(Cell attacker, Cell target) {
        return ((long) System.identityHashCode(attacker) << 32)
             | (System.identityHashCode(target) & 0xFFFFFFFFL);
    }
}
