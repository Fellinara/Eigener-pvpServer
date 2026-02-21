package de.duellplugin.managers;

import de.duellplugin.ChatUtils;
import de.duellplugin.DuellPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NpcManager {

    /** Metadata key attached to every NPC entity. Value = NPC type string. */
    public static final String META_KEY = "duell_npc_type";

    /** Supported NPC types and their display names. */
    public enum NpcType {
        MAIN("§6§l◆ PvP Server"),
        KITS("§e§l⚔ Kits"),
        STATS("§b§l📊 Statistiken"),
        DUELL("§c§l⚔ Duell"),
        BOT("§4§l☠ Bot-Kampf"),
        FREUND("§a§l☺ Freunde"),
        PARTY("§d§l♜ Party");

        private final String defaultName;

        NpcType(String defaultName) {
            this.defaultName = defaultName;
        }

        public String getDefaultName() {
            return defaultName;
        }

        public static NpcType fromString(String s) {
            for (NpcType t : values()) {
                if (t.name().equalsIgnoreCase(s)) return t;
            }
            return null;
        }
    }

    /** Persistent NPC data stored on disk. */
    public static final class NpcData {
        private final UUID entityUUID;
        private final NpcType type;
        private final String customName;
        private final Location location;

        NpcData(UUID entityUUID, NpcType type, String customName, Location location) {
            this.entityUUID = entityUUID;
            this.type = type;
            this.customName = customName;
            this.location = location;
        }

        public UUID entityUUID() { return entityUUID; }
        public NpcType type() { return type; }
        public String customName() { return customName; }
        public Location location() { return location; }
    }

    private final DuellPlugin plugin;
    private final Map<UUID, NpcData> npcs; // entity UUID → NpcData
    private final File npcFile;

    public NpcManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.npcs = new HashMap<>();
        this.npcFile = new File(plugin.getDataFolder(), "npcs.yml");
        // Spawn NPCs on first server tick (world is loaded by then)
        plugin.getServer().getScheduler().runTask(plugin, this::loadAndSpawn);
    }

    // ── API ───────────────────────────────────────────

    public boolean isNpc(UUID entityUUID) {
        return npcs.containsKey(entityUUID);
    }

    public NpcType getNpcType(UUID entityUUID) {
        NpcData data = npcs.get(entityUUID);
        return data != null ? data.type() : null;
    }

    /** Configures common properties on a freshly spawned NPC Villager. */
    private void configureNpc(Villager npc, NpcType type, String displayName) {
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setCustomName(displayName);
        npc.setCustomNameVisible(true);
        npc.setVillagerType(Villager.Type.PLAINS);
        npc.setProfession(Villager.Profession.NONE);
        npc.setRemoveWhenFarAway(false);
        npc.setMetadata(META_KEY, new FixedMetadataValue(plugin, type.name()));
    }

    /**
     * Spawns a new NPC at the player's location.
     * @param type  NpcType enum value
     * @param name  Custom display name (null = use default for type)
     * @param loc   Spawn location
     * @return entity UUID of the spawned NPC
     */
    public UUID spawnNpc(NpcType type, String name, Location loc) {
        String displayName = name != null ? ChatUtils.color(name) : type.getDefaultName();
        Villager npc = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        configureNpc(npc, type, displayName);
        UUID uid = npc.getUniqueId();
        npcs.put(uid, new NpcData(uid, type, displayName, loc.clone()));
        saveNpcs();
        return uid;
    }

    /**
     * Removes an NPC entity and deletes it from the config.
     */
    public boolean removeNpc(UUID entityUUID) {
        NpcData data = npcs.remove(entityUUID);
        if (data == null) return false;
        // Remove the actual entity if still loaded
        var entity = plugin.getServer().getEntity(entityUUID);
        if (entity != null) entity.remove();
        saveNpcs();
        return true;
    }

    public Collection<NpcData> getAllNpcs() {
        return Collections.unmodifiableCollection(npcs.values());
    }

    // ── Load / Save ──────────────────────────────────

    private void loadAndSpawn() {
        if (!npcFile.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(npcFile);
        ConfigurationSection section = config.getConfigurationSection("npcs");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uid = UUID.fromString(uuidStr);
                ConfigurationSection cs = section.getConfigurationSection(uuidStr);
                if (cs == null) continue;

                String typeStr = cs.getString("type", "MAIN");
                NpcType type = NpcType.fromString(typeStr);
                if (type == null) type = NpcType.MAIN;
                String customName = cs.getString("name", type.getDefaultName());
                Location loc = deserializeLoc(cs);
                if (loc == null) continue;

                // Check if entity still exists in the world
                var existing = plugin.getServer().getEntity(uid);
                if (existing instanceof Villager v) {
                    // Re-attach metadata in case it was lost
                    v.setMetadata(META_KEY, new FixedMetadataValue(plugin, type.name()));
                    npcs.put(uid, new NpcData(uid, type, customName, loc));
                } else {
                    // Entity gone – respawn it
                    Villager npc = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
                    configureNpc(npc, type, customName);
                    UUID newUid = npc.getUniqueId();
                    npcs.put(newUid, new NpcData(newUid, type, customName, loc));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Laden von NPC " + uuidStr + ": " + e.getMessage());
            }
        }
        // Re-save with updated UUIDs (in case entities were respawned)
        saveNpcs();
        plugin.getLogger().info(npcs.size() + " NPCs geladen.");
    }

    public void saveNpcs() {
        FileConfiguration config = new YamlConfiguration();
        for (NpcData data : npcs.values()) {
            String path = "npcs." + data.entityUUID();
            config.set(path + ".type", data.type().name());
            config.set(path + ".name", data.customName());
            serializeLoc(config, path, data.location());
        }
        try {
            config.save(npcFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der NPCs: " + e.getMessage());
        }
    }

    private void serializeLoc(FileConfiguration config, String path, Location loc) {
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
    }

    private Location deserializeLoc(ConfigurationSection cs) {
        String worldName = cs.getString("world");
        if (worldName == null) return null;
        var world = plugin.getServer().getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                cs.getDouble("x"), cs.getDouble("y"), cs.getDouble("z"),
                (float) cs.getDouble("yaw"), (float) cs.getDouble("pitch"));
    }
}
