package de.fellinara.risiko.managers;

import de.fellinara.risiko.RisikoPlugin;
import de.fellinara.risiko.models.Kingdom;
import de.fellinara.risiko.models.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Verwaltet das Laden und Speichern der Spielerdaten in einer YAML-Datei.
 */
public class DataManager {

    private final RisikoPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    // Cache: UUID -> PlayerData
    private final Map<UUID, PlayerData> playerCache = new HashMap<>();

    public DataManager(RisikoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialisiert den DataManager: Datei erstellen und Daten laden.
     */
    public void init() {
        dataFile = new File(plugin.getDataFolder(), "players.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Konnte players.yml nicht erstellen!", e);
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
    }

    /**
     * Lädt alle Spielerdaten aus der YAML-Datei in den Cache.
     */
    private void loadAll() {
        playerCache.clear();
        if (!dataConfig.contains("players")) return;

        for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String path = "players." + uuidStr;
                String name = dataConfig.getString(path + ".name", "Unknown");
                PlayerData data = new PlayerData(uuid, name);

                String kingdomStr = dataConfig.getString(path + ".kingdom", null);
                if (kingdomStr != null) {
                    data.setKingdom(Kingdom.fromString(kingdomStr));
                }
                data.setKing(dataConfig.getBoolean(path + ".king", false));
                data.setHearts(dataConfig.getInt(path + ".hearts", plugin.getConfig().getInt("default-hearts", 3)));
                data.setInGame(dataConfig.getBoolean(path + ".inGame", false));
                data.setBanned(dataConfig.getBoolean(path + ".banned", false));

                playerCache.put(uuid, data);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ungültige UUID in players.yml: " + uuidStr);
            }
        }
        plugin.getLogger().info("Spielerdaten geladen: " + playerCache.size() + " Einträge.");
    }

    /**
     * Speichert alle Spielerdaten aus dem Cache in die YAML-Datei.
     */
    public void saveAll() {
        // Alte Daten löschen
        dataConfig.set("players", null);

        for (Map.Entry<UUID, PlayerData> entry : playerCache.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerData data = entry.getValue();
            dataConfig.set(path + ".name", data.getName());
            dataConfig.set(path + ".kingdom", data.getKingdom() != null ? data.getKingdom().getId() : null);
            dataConfig.set(path + ".king", data.isKing());
            dataConfig.set(path + ".hearts", data.getHearts());
            dataConfig.set(path + ".inGame", data.isInGame());
            dataConfig.set(path + ".banned", data.isBanned());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte players.yml nicht speichern!", e);
        }
    }

    /**
     * Gibt die PlayerData für eine UUID zurück (erstellt eine neue, falls nicht vorhanden).
     */
    public PlayerData getOrCreate(UUID uuid, String name) {
        PlayerData data = playerCache.get(uuid);
        if (data == null) {
            data = new PlayerData(uuid, name);
            data.setHearts(plugin.getConfig().getInt("default-hearts", 3));
            playerCache.put(uuid, data);
        } else {
            data.setName(name);
        }
        return data;
    }

    /**
     * Gibt die PlayerData für eine UUID zurück (null, wenn nicht vorhanden).
     */
    public PlayerData get(UUID uuid) {
        return playerCache.get(uuid);
    }

    /**
     * Gibt alle gecachten PlayerData-Einträge zurück.
     */
    public Map<UUID, PlayerData> getAll() {
        return playerCache;
    }

    /**
     * Speichert einen einzelnen Spieler sofort.
     */
    public void save(UUID uuid) {
        PlayerData data = playerCache.get(uuid);
        if (data == null) return;

        String path = "players." + uuid;
        dataConfig.set(path + ".name", data.getName());
        dataConfig.set(path + ".kingdom", data.getKingdom() != null ? data.getKingdom().getId() : null);
        dataConfig.set(path + ".king", data.isKing());
        dataConfig.set(path + ".hearts", data.getHearts());
        dataConfig.set(path + ".inGame", data.isInGame());
        dataConfig.set(path + ".banned", data.isBanned());

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Spielerdaten nicht speichern!", e);
        }
    }
}
