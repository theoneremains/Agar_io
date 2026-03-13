
import java.io.*;
import java.util.*;

/**
 * GameSettings : Manages game configuration with save/load support.
 * Settings are stored as key=value text files in a "saves" directory.
 * Supports up to 3 save slots with rename and default restore capability.
 * @author Kamil Yunus Ozkaya
 */
public class GameSettings {

    /** Maximum number of save files allowed */
    public static final int MAX_SAVES = 3;

    /** Directory where save files are stored */
    private static final String SAVES_DIR = "saves";

    /** File extension for save files */
    private static final String SAVE_EXT = ".cfg";

    // Default values
    public static final String DEFAULT_PLAYER_NAME = "Player";
    public static final int DEFAULT_NPC_COUNT = 3;
    public static final int DEFAULT_WORLD_WIDTH = 3840;
    public static final int DEFAULT_WORLD_HEIGHT = 2160;
    public static final double DEFAULT_CELL_DENSITY = 200.0;
    public static final boolean DEFAULT_SOUND_ENABLED = true;
    public static final boolean DEFAULT_FULLSCREEN = true;
    public static final int DEFAULT_PLAYER_COLOR_INDEX = 0;

    // Current settings
    public String playerName = DEFAULT_PLAYER_NAME;
    public int npcCount = DEFAULT_NPC_COUNT;
    public int worldWidth = DEFAULT_WORLD_WIDTH;
    public int worldHeight = DEFAULT_WORLD_HEIGHT;
    public double cellDensity = DEFAULT_CELL_DENSITY;
    public boolean soundEnabled = DEFAULT_SOUND_ENABLED;
    public boolean fullscreen = DEFAULT_FULLSCREEN;
    public int playerColorIndex = DEFAULT_PLAYER_COLOR_INDEX;

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
     * Returns a list of existing save file names (without extension), sorted alphabetically.
     */
    public static List<String> listSaves() {
        List<String> saves = new ArrayList<>();
        File dir = new File(SAVES_DIR);
        if (!dir.exists()) return saves;
        File[] files = dir.listFiles((d, name) -> name.endsWith(SAVE_EXT));
        if (files == null) return saves;
        for (File f : files) {
            String name = f.getName();
            saves.add(name.substring(0, name.length() - SAVE_EXT.length()));
        }
        Collections.sort(saves);
        return saves;
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
        GamePanel.playerColor = GamePanel.colors[playerColorIndex % GamePanel.colors.length];
        GamePanel.playerColorIndex = playerColorIndex % GamePanel.colors.length;
        GamePanel.cellDensity = cellDensity;
        MainClass.WORLD_WIDTH = worldWidth;
        MainClass.WORLD_HEIGHT = worldHeight;
        Sound.soundEnabled = soundEnabled;
        MainClass.fullscreen = fullscreen;
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
    }
}
