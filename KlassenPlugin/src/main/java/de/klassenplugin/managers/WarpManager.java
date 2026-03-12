package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WarpManager {

    private final KlassenPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<String, Location> warps = new HashMap<>();

    public WarpManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "warps.yml");
        loadWarps();
    }

    private void loadWarps() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Fehler beim Erstellen von warps.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (!dataConfig.contains("warps")) return;

        org.bukkit.configuration.ConfigurationSection warpsSection = dataConfig.getConfigurationSection("warps");
        if (warpsSection == null) return;

        for (String name : warpsSection.getKeys(false)) {
            Location loc = loadLocation("warps." + name);
            if (loc != null) {
                warps.put(name.toLowerCase(), loc);
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

    public void saveWarps() {
        dataConfig.set("warps", null);
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            String path = "warps." + entry.getKey();
            Location loc = entry.getValue();
            dataConfig.set(path + ".world", loc.getWorld().getName());
            dataConfig.set(path + ".x", loc.getX());
            dataConfig.set(path + ".y", loc.getY());
            dataConfig.set(path + ".z", loc.getZ());
            dataConfig.set(path + ".yaw", loc.getYaw());
            dataConfig.set(path + ".pitch", loc.getPitch());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Warps: " + e.getMessage());
        }
    }

    public void setWarp(String name, Location location) {
        warps.put(name.toLowerCase(), location);
        saveWarps();
    }

    public Location getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public boolean deleteWarp(String name) {
        boolean removed = warps.remove(name.toLowerCase()) != null;
        if (removed) saveWarps();
        return removed;
    }

    public Set<String> getWarps() {
        return warps.keySet();
    }

    public boolean hasWarp(String name) {
        return warps.containsKey(name.toLowerCase());
    }
}
