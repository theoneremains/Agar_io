import java.io.*;
import java.util.*;

/**
 * EvolvingProgressSave : Manages per-player progress for the Infinite Evolving Cells mode.
 * Progress is stored in saves/evolving/<sanitized_player_name>.ecfg files.
 * Tracks the highest stage the player has reached and their highest score in this mode.
 * Files are automatically saved whenever a new stage is started or the player dies.
 * @author Kamil Yunus Ozkaya
 */
public class EvolvingProgressSave {

    private static final String EVOLVING_DIR = GameConstants.EVOLVING_SAVES_DIR;
    private static final String EVOLVING_EXT = GameConstants.EVOLVING_SAVE_EXT;

    /** Player name associated with this progress file */
    public String playerName = "Player";

    /** The highest stage this player has reached (stages started, not just cleared) */
    public int maxStageReached = 0;

    /** The highest score achieved in any single run of this mode */
    public int highestScore = 0;

    public EvolvingProgressSave() {}

    public EvolvingProgressSave(String playerName) {
        this.playerName = playerName;
    }

    /**
     * Saves progress to saves/evolving/<sanitizedName>.ecfg.
     * Creates the directory if it does not exist.
     * @return true if the save succeeded
     */
    public boolean save() {
        File dir = new File(EVOLVING_DIR);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, sanitizeFileName(playerName) + EVOLVING_EXT);
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("playerName=" + playerName);
            pw.println("maxStageReached=" + maxStageReached);
            pw.println("highestScore=" + highestScore);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads progress for the given player name from its save file.
     * @param name the player name whose file to load
     * @return true if a save file was found and successfully loaded
     */
    public boolean load(String name) {
        File file = new File(EVOLVING_DIR, sanitizeFileName(name) + EVOLVING_EXT);
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
                        case "playerName":      playerName      = val; break;
                        case "maxStageReached": maxStageReached = Math.max(0, Integer.parseInt(val)); break;
                        case "highestScore":    highestScore    = Math.max(0, Integer.parseInt(val)); break;
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
     * Updates maxStageReached and highestScore with new records, then saves the file.
     * @param stage the current stage number
     * @param score the current score
     * @return true if any record was broken (new personal best)
     */
    public boolean updateAndSave(int stage, int score) {
        boolean updated = false;
        if (stage > maxStageReached) { maxStageReached = stage; updated = true; }
        if (score > highestScore)    { highestScore    = score;  updated = true; }
        save();
        return updated;
    }

    /**
     * Returns true if a save file exists for the given player name.
     * @param playerName the player name to check
     */
    public static boolean saveExists(String playerName) {
        return new File(EVOLVING_DIR, sanitizeFileName(playerName) + EVOLVING_EXT).exists();
    }

    /**
     * Loads and returns an EvolvingProgressSave for the given player name.
     * If no save exists, returns a fresh save with maxStageReached=0.
     * @param playerName the player whose progress to load
     */
    public static EvolvingProgressSave loadForPlayer(String playerName) {
        EvolvingProgressSave save = new EvolvingProgressSave(playerName);
        save.load(playerName);
        return save;
    }

    /**
     * Returns a sorted list of all player names that have evolving mode saves.
     * Player names are read from inside the save files (not inferred from filenames).
     */
    public static List<String> listSavedPlayerNames() {
        List<String> names = new ArrayList<>();
        File dir = new File(EVOLVING_DIR);
        if (!dir.exists()) return names;
        File[] files = dir.listFiles((d, n) -> n.endsWith(EVOLVING_EXT));
        if (files == null) return names;
        for (File f : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("playerName=")) {
                        names.add(line.substring("playerName=".length()).trim());
                        break;
                    }
                }
            } catch (IOException ignored) {}
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Returns a human-readable summary of this save's records.
     * Example: "Max Stage: 5  |  Best Score: 1234"
     */
    public String getSummary() {
        if (maxStageReached == 0) return "No previous progress";
        return "Max Stage: " + maxStageReached + "  |  Best Score: " + highestScore;
    }

    /** Replaces characters unsafe for filenames with underscores. */
    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
