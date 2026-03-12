package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HomeManager {

    private final KlassenPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    // UUID -> (homeName -> Location)
    private final Map<UUID, Map<String, Location>> homes = new HashMap<>();

    public HomeManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "homes.yml");
        loadHomes();
    }

    private void loadHomes() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Fehler beim Erstellen von homes.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (!dataConfig.contains("homes")) return;

        org.bukkit.configuration.ConfigurationSection homesSection = dataConfig.getConfigurationSection("homes");
        if (homesSection == null) return;

        for (String uuidStr : homesSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, Location> playerHomes = new HashMap<>();
                org.bukkit.configuration.ConfigurationSection playerSection =
                        dataConfig.getConfigurationSection("homes." + uuidStr);
                if (playerSection == null) continue;
                for (String homeName : playerSection.getKeys(false)) {
                    Location loc = loadLocation("homes." + uuidStr + "." + homeName);
                    if (loc != null) {
                        playerHomes.put(homeName.toLowerCase(), loc);
                    }
                }
                homes.put(uuid, playerHomes);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ungültige UUID in homes.yml: " + uuidStr);
            }
        }
    }

    private Location loadLocation(String path) {
        if (!dataConfig.contains(path + ".world")) return null;
        try {
            String worldName = dataConfig.getString(path + ".world");
            if (worldName == null) return null;
            org.bukkit.World world = plugin.getServer().getWorld(worldName);
            if (world == null) return null;
            double x = dataConfig.getDouble(path + ".x");
            double y = dataConfig.getDouble(path + ".y");
            double z = dataConfig.getDouble(path + ".z");
            float yaw = (float) dataConfig.getDouble(path + ".yaw");
            float pitch = (float) dataConfig.getDouble(path + ".pitch");
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    public void saveHomes() {
        dataConfig.set("homes", null);
        for (Map.Entry<UUID, Map<String, Location>> entry : homes.entrySet()) {
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<String, Location> homeEntry : entry.getValue().entrySet()) {
                String path = "homes." + uuidStr + "." + homeEntry.getKey();
                Location loc = homeEntry.getValue();
                dataConfig.set(path + ".world", loc.getWorld().getName());
                dataConfig.set(path + ".x", loc.getX());
                dataConfig.set(path + ".y", loc.getY());
                dataConfig.set(path + ".z", loc.getZ());
                dataConfig.set(path + ".yaw", loc.getYaw());
                dataConfig.set(path + ".pitch", loc.getPitch());
            }
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Homes: " + e.getMessage());
        }
    }

    public void setHome(UUID playerId, String name, Location location) {
        homes.computeIfAbsent(playerId, k -> new HashMap<>()).put(name.toLowerCase(), location);
        saveHomes();
    }

    public Location getHome(UUID playerId, String name) {
        Map<String, Location> playerHomes = homes.get(playerId);
        if (playerHomes == null) return null;
        return playerHomes.get(name.toLowerCase());
    }

    public boolean deleteHome(UUID playerId, String name) {
        Map<String, Location> playerHomes = homes.get(playerId);
        if (playerHomes == null) return false;
        boolean removed = playerHomes.remove(name.toLowerCase()) != null;
        if (removed) saveHomes();
        return removed;
    }

    public Set<String> getHomes(UUID playerId) {
        Map<String, Location> playerHomes = homes.get(playerId);
        if (playerHomes == null) return new HashSet<>();
        return playerHomes.keySet();
    }

    public int getHomeCount(UUID playerId) {
        Map<String, Location> playerHomes = homes.get(playerId);
        return playerHomes == null ? 0 : playerHomes.size();
    }

    public boolean hasHome(UUID playerId, String name) {
        Map<String, Location> playerHomes = homes.get(playerId);
        if (playerHomes == null) return false;
        return playerHomes.containsKey(name.toLowerCase());
    }

    public int getMaxHomes() {
        return plugin.getConfig().getInt("max-homes", 5);
    }
}
