package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Arena;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {

    private final DuellPlugin plugin;
    private final Map<String, Arena> arenas;
    private final File arenaFile;
    private FileConfiguration arenaConfig;

    public ArenaManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.arenaFile = new File(plugin.getDataFolder(), "arenas.yml");
        loadArenas();
    }

    public void createArena(String name) {
        arenas.put(name.toLowerCase(), new Arena(name.toLowerCase(), null, null));
        saveArenas();
    }

    public void deleteArena(String name) {
        arenas.remove(name.toLowerCase());
        saveArenas();
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getAllArenas() {
        return arenas.values();
    }

    public Arena getAvailableArena() {
        for (Arena arena : arenas.values()) {
            if (arena.isReady() && !arena.isInUse()) {
                return arena;
            }
        }
        return null;
    }

    public boolean arenaExists(String name) {
        return arenas.containsKey(name.toLowerCase());
    }

    public void setSpawn(String name, int spawnNumber, Location location) {
        Arena arena = arenas.get(name.toLowerCase());
        if (arena != null) {
            if (spawnNumber == 1) {
                arena.setSpawn1(location);
            } else {
                arena.setSpawn2(location);
            }
            saveArenas();
        }
    }

    private void loadArenas() {
        if (!arenaFile.exists()) {
            return;
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
        ConfigurationSection section = arenaConfig.getConfigurationSection("arenas");
        if (section == null) return;

        for (String name : section.getKeys(false)) {
            ConfigurationSection arenaSection = section.getConfigurationSection(name);
            if (arenaSection == null) continue;

            Location spawn1 = null;
            Location spawn2 = null;

            if (arenaSection.contains("spawn1")) {
                spawn1 = deserializeLocation(arenaSection.getConfigurationSection("spawn1"));
            }
            if (arenaSection.contains("spawn2")) {
                spawn2 = deserializeLocation(arenaSection.getConfigurationSection("spawn2"));
            }

            arenas.put(name, new Arena(name, spawn1, spawn2));
        }
        plugin.getLogger().info(arenas.size() + " Arenen geladen.");
    }

    public void saveArenas() {
        arenaConfig = new YamlConfiguration();
        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            String path = "arenas." + entry.getKey();
            Arena arena = entry.getValue();

            if (arena.getSpawn1() != null) {
                serializeLocation(arenaConfig, path + ".spawn1", arena.getSpawn1());
            }
            if (arena.getSpawn2() != null) {
                serializeLocation(arenaConfig, path + ".spawn2", arena.getSpawn2());
            }
        }

        try {
            arenaConfig.save(arenaFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Arenen: " + e.getMessage());
        }
    }

    private void serializeLocation(FileConfiguration config, String path, Location loc) {
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
    }

    private Location deserializeLocation(ConfigurationSection section) {
        if (section == null) return null;
        String worldName = section.getString("world");
        if (worldName == null) return null;
        var world = plugin.getServer().getWorld(worldName);
        if (world == null) return null;

        return new Location(
                world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
    }
}
