package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Arena;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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
    /** Tracks blocks placed during fights: arenaName → set of "x,y,z" location keys. */
    private final Map<String, Set<String>> arenaPlacedBlocks;
    /** Callbacks to run when an arena becomes available again (for the duel queue). */
    private final List<Runnable> arenaFreeCallbacks;

    public ArenaManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.arenaFile = new File(plugin.getDataFolder(), "arenas.yml");
        this.arenaPlacedBlocks = new HashMap<>();
        this.arenaFreeCallbacks = new ArrayList<>();
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
            if (spawnNumber == 1) arena.setSpawn1(location);
            else arena.setSpawn2(location);
            saveArenas();
        }
    }

    /**
     * Sets one corner of the reset region for the given arena and saves.
     * If both corners are now defined, automatically takes a fresh snapshot.
     */
    public void setRegionPos(String arenaName, int corner, Location location) {
        Arena arena = arenas.get(arenaName.toLowerCase());
        if (arena == null) return;
        if (corner == 1) arena.setRegionPos1(location);
        else arena.setRegionPos2(location);
        saveArenas();
        if (arena.isRegionDefined()) {
            takeSnapshot(arenaName);
        }
    }

    /**
     * Records a block as placed during a fight so it can be broken by duel participants.
     * Call this on BlockPlaceEvent.
     */
    public void addPlacedBlock(String arenaName, Location loc) {
        arenaPlacedBlocks
                .computeIfAbsent(arenaName.toLowerCase(), k -> new HashSet<>())
                .add(locKey(loc));
    }

    /**
     * Returns true if the block at the given location was placed during the current fight in that arena.
     * Original arena blocks will return false.
     */
    public boolean isPlacedBlock(String arenaName, Location loc) {
        Set<String> set = arenaPlacedBlocks.get(arenaName.toLowerCase());
        return set != null && set.contains(locKey(loc));
    }

    /** Removes placed-block tracking for an arena (call on arena reset). */
    private void clearPlacedBlocks(String arenaName) {
        arenaPlacedBlocks.remove(arenaName.toLowerCase());
    }

    private static String locKey(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    /**
     * Registers a callback to be invoked the next time any arena becomes available.
     * Used by the duel queue in DuellManager.
     */
    public void onNextArenaFree(Runnable callback) {
        arenaFreeCallbacks.add(callback);
    }

    /**
     * Captures the current state of every block in the arena's region as a snapshot.
     * This snapshot is used to restore the arena after each fight.
     */
    public void takeSnapshot(String arenaName) {
        Arena arena = arenas.get(arenaName.toLowerCase());
        if (arena == null || !arena.isRegionDefined()) return;

        Location p1 = arena.getRegionPos1();
        Location p2 = arena.getRegionPos2();
        World world = p1.getWorld();

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        List<BlockState> snapshot = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    snapshot.add(world.getBlockAt(x, y, z).getState());
                }
            }
        }
        arena.setSnapshot(snapshot);
        plugin.getLogger().info("Snapshot für Arena '" + arenaName + "' aufgenommen: "
                + snapshot.size() + " Blöcke.");
    }

    /**
     * Restores all blocks in the arena's region to their snapshotted state.
     * If no snapshot exists but the region is defined, takes one first.
     */
    public void resetArena(String arenaName) {
        Arena arena = arenas.get(arenaName.toLowerCase());
        if (arena == null) return;

        List<BlockState> snapshot = arena.getSnapshot();
        if (snapshot == null || snapshot.isEmpty()) {
            if (arena.isRegionDefined()) {
                plugin.getLogger().warning("Kein Snapshot für Arena '" + arenaName
                        + "' – nehme jetzt einen auf. Nutze /arena snapshot vor dem Kampf für bessere Ergebnisse.");
                takeSnapshot(arenaName);
            }
            return;
        }

        // Restore all captured block states
        for (BlockState state : snapshot) {
            // update(force=true, physics=false): force-restores the block without triggering block physics
            state.update(true, false);
        }

        // Clear placed-block tracking for this arena
        clearPlacedBlocks(arenaName);

        // Notify the duel queue that an arena is now free
        if (!arenaFreeCallbacks.isEmpty()) {
            Runnable next = arenaFreeCallbacks.remove(0);
            // Run on next tick so arena state is fully reset first
            plugin.getServer().getScheduler().runTask(plugin, next);
        }
    }

    // ──────────────────────────────────────────────
    // Persistence
    // ──────────────────────────────────────────────

    private void loadArenas() {
        if (!arenaFile.exists()) {
            createDefaultArenas();
            return;
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
        ConfigurationSection section = arenaConfig.getConfigurationSection("arenas");
        if (section == null) {
            createDefaultArenas();
            return;
        }

        for (String name : section.getKeys(false)) {
            ConfigurationSection arenaSection = section.getConfigurationSection(name);
            if (arenaSection == null) continue;

            Location spawn1 = null;
            Location spawn2 = null;
            Location pos1 = null;
            Location pos2 = null;

            if (arenaSection.contains("spawn1")) {
                spawn1 = deserializeLocation(arenaSection.getConfigurationSection("spawn1"));
            }
            if (arenaSection.contains("spawn2")) {
                spawn2 = deserializeLocation(arenaSection.getConfigurationSection("spawn2"));
            }
            if (arenaSection.contains("region.pos1")) {
                pos1 = deserializeLocation(arenaSection.getConfigurationSection("region.pos1"));
            }
            if (arenaSection.contains("region.pos2")) {
                pos2 = deserializeLocation(arenaSection.getConfigurationSection("region.pos2"));
            }

            Arena arena = new Arena(name, spawn1, spawn2);
            arena.setRegionPos1(pos1);
            arena.setRegionPos2(pos2);
            arenas.put(name, arena);
        }
        plugin.getLogger().info(arenas.size() + " Arenen geladen.");

        // Take snapshots for arenas that have regions defined (server is assumed to be in clean state)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Arena arena : arenas.values()) {
                if (arena.isRegionDefined()) {
                    takeSnapshot(arena.getName());
                }
            }
        });
    }

    private void createDefaultArenas() {
        var world = plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().get(0);
        if (world == null) {
            plugin.getLogger().warning("Keine Welt gefunden! Standard-Arenen konnten nicht erstellt werden.");
            return;
        }

        Location worldSpawn = world.getSpawnLocation();
        double baseX = worldSpawn.getX();
        double baseY = worldSpawn.getY();
        double baseZ = worldSpawn.getZ();

        Location spawn1Arena1 = new Location(world, baseX + 20, baseY, baseZ + 20, -135, 0);
        Location spawn2Arena1 = new Location(world, baseX + 40, baseY, baseZ + 40, 45, 0);
        arenas.put("arena1", new Arena("arena1", spawn1Arena1, spawn2Arena1));

        Location spawn1Arena2 = new Location(world, baseX - 20, baseY, baseZ + 20, -45, 0);
        Location spawn2Arena2 = new Location(world, baseX - 40, baseY, baseZ + 40, 135, 0);
        arenas.put("arena2", new Arena("arena2", spawn1Arena2, spawn2Arena2));

        saveArenas();
        plugin.getLogger().info("2 Standard-Arenen erstellt. Nutze /arena setpos1 | setpos2 für den Reset-Bereich.");
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
            if (arena.getRegionPos1() != null) {
                serializeLocation(arenaConfig, path + ".region.pos1", arena.getRegionPos1());
            }
            if (arena.getRegionPos2() != null) {
                serializeLocation(arenaConfig, path + ".region.pos2", arena.getRegionPos2());
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
