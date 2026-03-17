
import java.io.*;
import java.util.*;

/**
 * GameSettings : Manages game configuration with save/load support.
 * Settings are stored as key=value text files in a "saves" directory.
 * Supports up to 3 save slots with rename and default restore capability.
 * Also manages an automatic "autosave" slot (not counted toward the 3-file limit)
 * that persists the most recent session's settings across launches.
 * @author Kamil Yunus Ozkaya
 */
public class GameSettings {

    /** Maximum number of user-visible save files allowed */
    public static final int MAX_SAVES = GameConstants.MAX_SAVE_FILES;

    /** Directory where save files are stored */
    private static final String SAVES_DIR = GameConstants.SAVES_DIR;

    /** File extension for save files */
    private static final String SAVE_EXT = GameConstants.SAVE_EXT;

    /**
     * Reserved save name for automatic persistence of the last session.
     * This file is excluded from the user-visible save list and the 3-file limit.
     */
    public static final String AUTOSAVE_NAME = "autosave";

    // Default values
    public static final String DEFAULT_PLAYER_NAME = "Player";
    public static final int DEFAULT_NPC_COUNT = 10;
    public static final int DEFAULT_WORLD_WIDTH = GameConstants.DEFAULT_WORLD_WIDTH;
    public static final int DEFAULT_WORLD_HEIGHT = GameConstants.DEFAULT_WORLD_HEIGHT;
    public static final double DEFAULT_CELL_DENSITY = GameConstants.DEFAULT_CELL_DENSITY;
    public static final boolean DEFAULT_SOUND_ENABLED = true;
    public static final boolean DEFAULT_FULLSCREEN = true;
    public static final int DEFAULT_PLAYER_COLOR_INDEX = 0;

    // Default NPC difficulty distribution (must sum to DEFAULT_NPC_COUNT)
    public static final int DEFAULT_NPC_EASY   = 4;
    public static final int DEFAULT_NPC_MEDIUM = 3;
    public static final int DEFAULT_NPC_HARD   = 3;

    /** Default shave-rate multiplier (1.0 = standard erosion speed) */
    public static final double DEFAULT_SHAVE_RATE_MULTIPLIER = 1.0;

    // Current settings
    public String playerName = DEFAULT_PLAYER_NAME;
    public int npcCount = DEFAULT_NPC_COUNT;
    public int worldWidth = DEFAULT_WORLD_WIDTH;
    public int worldHeight = DEFAULT_WORLD_HEIGHT;
    public double cellDensity = DEFAULT_CELL_DENSITY;
    public boolean soundEnabled = DEFAULT_SOUND_ENABLED;
    public boolean fullscreen = DEFAULT_FULLSCREEN;
    public int playerColorIndex = DEFAULT_PLAYER_COLOR_INDEX;

    // NPC difficulty distribution (0 = use round-robin based on npcCount)
    public int npcEasyCount   = DEFAULT_NPC_EASY;
    public int npcMediumCount = DEFAULT_NPC_MEDIUM;
    public int npcHardCount   = DEFAULT_NPC_HARD;

    /** Multiplier applied to the shave/erosion rate (configurable in advanced settings) */
    public double shaveRateMultiplier = DEFAULT_SHAVE_RATE_MULTIPLIER;

    /** Creates settings with default values */
    public GameSettings() {}

    /** Resets all settings to their default values */
    public void restoreDefaults() {
        playerName = DEFAULT_PLAYER_NAME;
        npcCount = DEFAULT_NPC_COUNT;
        worldWidth = DEFAULT_WORLD_WIDTH;
        worldHeight = DEFAULT_WORLD_HEIGHT;
        cellDensity = DEFAULT_CELL_DENSITY;
        soundEnabled = DEFAULT_SOUND_ENABLED;
        fullscreen = DEFAULT_FULLSCREEN;
        playerColorIndex = DEFAULT_PLAYER_COLOR_INDEX;
        npcEasyCount   = DEFAULT_NPC_EASY;
        npcMediumCount = DEFAULT_NPC_MEDIUM;
        npcHardCount   = DEFAULT_NPC_HARD;
        shaveRateMultiplier = DEFAULT_SHAVE_RATE_MULTIPLIER;
    }

    /**
     * Saves settings to a file in the saves directory.
     * @param fileName the save file name (without extension)
     * @return true if save was successful
     */
    public boolean save(String fileName) {
        File dir = new File(SAVES_DIR);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, fileName + SAVE_EXT);
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("playerName=" + playerName);
            pw.println("npcCount=" + npcCount);
            pw.println("worldWidth=" + worldWidth);
            pw.println("worldHeight=" + worldHeight);
            pw.println("cellDensity=" + cellDensity);
            pw.println("soundEnabled=" + soundEnabled);
            pw.println("fullscreen=" + fullscreen);
            pw.println("playerColorIndex=" + playerColorIndex);
            pw.println("npcEasyCount=" + npcEasyCount);
            pw.println("npcMediumCount=" + npcMediumCount);
            pw.println("npcHardCount=" + npcHardCount);
            pw.println("shaveRateMultiplier=" + shaveRateMultiplier);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads settings from a file in the saves directory.
     * @param fileName the save file name (without extension)
     * @return true if load was successful
     */
    public boolean load(String fileName) {
        File file = new File(SAVES_DIR, fileName + SAVE_EXT);
        if (!file.exists()) return false;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || !line.contains("=")) continue;
                String[] parts = line.split("=", 2);
                String key = parts[0].trim();
                String val = parts[1].trim();
                try {
                    switch (key) {
                        case "playerName": playerName = val; break;
                        case "npcCount": npcCount = Math.max(3, Integer.parseInt(val)); break;
                        case "worldWidth": worldWidth = Math.max(800, Integer.parseInt(val)); break;
                        case "worldHeight": worldHeight = Math.max(600, Integer.parseInt(val)); break;
                        case "cellDensity": cellDensity = Math.max(1, Double.parseDouble(val)); break;
                        case "soundEnabled": soundEnabled = Boolean.parseBoolean(val); break;
                        case "fullscreen": fullscreen = Boolean.parseBoolean(val); break;
                        case "playerColorIndex": playerColorIndex = Integer.parseInt(val); break;
                        case "npcEasyCount": npcEasyCount = Math.max(0, Integer.parseInt(val)); break;
                        case "npcMediumCount": npcMediumCount = Math.max(0, Integer.parseInt(val)); break;
                        case "npcHardCount": npcHardCount = Math.max(0, Integer.parseInt(val)); break;
                        case "shaveRateMultiplier": shaveRateMultiplier = Math.max(0.1, Double.parseDouble(val)); break;
                    }
                } catch (NumberFormatException ignored) {}
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns a list of existing user save file names (without extension),
     * sorted by last-modified date (most recent first).
     * The reserved autosave file is excluded from this list.
     */
    public static List<String> listSaves() {
        List<String> saves = new ArrayList<>();
        File dir = new File(SAVES_DIR);
        if (!dir.exists()) return saves;
        File[] files = dir.listFiles((d, name) -> name.endsWith(SAVE_EXT));
        if (files == null) return saves;
        // Exclude the autosave slot
        for (File f : files) {
            String name = f.getName();
            String baseName = name.substring(0, name.length() - SAVE_EXT.length());
            if (!baseName.equals(AUTOSAVE_NAME)) {
                saves.add(baseName);
            }
        }
        Collections.sort(saves);
        return saves;
    }

    /**
     * Returns the name of the most recently modified user save file (without extension),
     * or {@code null} if no user saves exist.
     */
    public static String getMostRecentSave() {
        File dir = new File(SAVES_DIR);
        if (!dir.exists()) return null;
        File[] files = dir.listFiles((d, name) -> name.endsWith(SAVE_EXT));
        if (files == null || files.length == 0) return null;

        File newest = null;
        for (File f : files) {
            String baseName = f.getName().substring(0, f.getName().length() - SAVE_EXT.length());
            if (baseName.equals(AUTOSAVE_NAME)) continue; // skip reserved
            if (newest == null || f.lastModified() > newest.lastModified()) {
                newest = f;
            }
        }
        if (newest == null) return null;
        String name = newest.getName();
        return name.substring(0, name.length() - SAVE_EXT.length());
    }

    /**
     * Returns true if the autosave file exists.
     */
    public static boolean autosaveExists() {
        return new File(SAVES_DIR, AUTOSAVE_NAME + SAVE_EXT).exists();
    }

    /**
     * Deletes a save file.
     * @param fileName the save file name (without extension)
     * @return true if deleted
     */
    public static boolean deleteSave(String fileName) {
        File file = new File(SAVES_DIR, fileName + SAVE_EXT);
        return file.exists() && file.delete();
    }

    /**
     * Renames a save file.
     * @param oldName the current name (without extension)
     * @param newName the new name (without extension)
     * @return true if renamed successfully
     */
    public static boolean renameSave(String oldName, String newName) {
        File oldFile = new File(SAVES_DIR, oldName + SAVE_EXT);
        File newFile = new File(SAVES_DIR, newName + SAVE_EXT);
        if (!oldFile.exists() || newFile.exists()) return false;
        return oldFile.renameTo(newFile);
    }

    /**
     * Applies the current settings to the game's static fields.
     */
    public void applyToGame() {
        GamePanel.playerName = playerName;
        GamePanel.playerColor = GameConstants.CELL_COLORS[playerColorIndex % GameConstants.CELL_COLORS.length];
        GamePanel.playerColorIndex = playerColorIndex % GameConstants.CELL_COLORS.length;
        GamePanel.cellDensity = cellDensity;
        MainClass.WORLD_WIDTH = worldWidth;
        MainClass.WORLD_HEIGHT = worldHeight;
        Sound.soundEnabled = soundEnabled;
        MainClass.fullscreen = fullscreen;
        // NPC difficulty distribution
        GamePanel.npcEasyCount   = npcEasyCount;
        GamePanel.npcMediumCount = npcMediumCount;
        GamePanel.npcHardCount   = npcHardCount;
        // Shave rate multiplier
        GamePanel.shaveRateMultiplier = shaveRateMultiplier;
    }

    /**
     * Reads current settings from the game's static fields.
     */
    public void readFromGame() {
        playerName = GamePanel.playerName;
        playerColorIndex = GamePanel.playerColorIndex;
        cellDensity = GamePanel.cellDensity;
        worldWidth = MainClass.WORLD_WIDTH;
        worldHeight = MainClass.WORLD_HEIGHT;
        soundEnabled = Sound.soundEnabled;
        fullscreen = MainClass.fullscreen;
        npcEasyCount   = GamePanel.npcEasyCount;
        npcMediumCount = GamePanel.npcMediumCount;
        npcHardCount   = GamePanel.npcHardCount;
        shaveRateMultiplier = GamePanel.shaveRateMultiplier;
    }
}
