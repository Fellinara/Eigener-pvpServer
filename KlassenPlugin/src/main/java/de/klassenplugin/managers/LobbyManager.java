package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class LobbyManager {

    private final KlassenPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private Location lobby;
    private Location spawn;

    public LobbyManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "locations.yml");
        loadLocations();
    }

    private void loadLocations() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Fehler beim Erstellen von locations.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        lobby = loadLocation("lobby");
        spawn = loadLocation("spawn");
    }

    private Location loadLocation(String key) {
        if (!dataConfig.contains(key + ".world")) return null;
        try {
            String worldName = dataConfig.getString(key + ".world");
            if (worldName == null) return null;
            org.bukkit.World world = plugin.getServer().getWorld(worldName);
            if (world == null) return null;
            double x = dataConfig.getDouble(key + ".x");
            double y = dataConfig.getDouble(key + ".y");
            double z = dataConfig.getDouble(key + ".z");
            float yaw = (float) dataConfig.getDouble(key + ".yaw");
            float pitch = (float) dataConfig.getDouble(key + ".pitch");
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveLocation(String key, Location location) {
        if (location == null) {
            dataConfig.set(key, null);
            return;
        }
        dataConfig.set(key + ".world", location.getWorld().getName());
        dataConfig.set(key + ".x", location.getX());
        dataConfig.set(key + ".y", location.getY());
        dataConfig.set(key + ".z", location.getZ());
        dataConfig.set(key + ".yaw", location.getYaw());
        dataConfig.set(key + ".pitch", location.getPitch());
    }

    public void saveLobby() {
        saveLocation("lobby", lobby);
        saveLocation("spawn", spawn);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Locations: " + e.getMessage());
        }
    }

    public Location getLobby() {
        return lobby;
    }

    public void setLobby(Location location) {
        this.lobby = location;
        saveLocation("lobby", location);
        saveToFile();
    }

    public boolean hasLobby() {
        return lobby != null;
    }

    public Location getSpawn() {
        return spawn;
    }

    public void setSpawn(Location location) {
        this.spawn = location;
        saveLocation("spawn", location);
        saveToFile();
    }

    public boolean hasSpawn() {
        return spawn != null;
    }

    private void saveToFile() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Locations: " + e.getMessage());
        }
    }
}
