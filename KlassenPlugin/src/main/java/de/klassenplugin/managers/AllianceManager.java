package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AllianceManager {
    private final KlassenPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, UUID> alliances = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private final Map<UUID, Long> inviteTimes = new HashMap<>();
    private static final long INVITE_EXPIRE_MS = 60_000L;

    public AllianceManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "alliances.yml");
        load();
    }

    public void load() {
        if (!dataFile.exists()) { try { plugin.getDataFolder().mkdirs(); dataFile.createNewFile(); } catch (IOException e) { plugin.getLogger().severe("alliances.yml: " + e.getMessage()); } }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        alliances.clear();
        if (dataConfig.contains("alliances")) {
            for (String key : dataConfig.getConfigurationSection("alliances").getKeys(false)) {
                try {
                    UUID a = UUID.fromString(key);
                    UUID b = UUID.fromString(Objects.requireNonNull(dataConfig.getString("alliances." + key)));
                    alliances.put(a, b); alliances.put(b, a);
                } catch (Exception ignored) {}
            }
        }
    }

    public void save() {
        dataConfig.set("alliances", null);
        Set<UUID> saved = new HashSet<>();
        for (Map.Entry<UUID, UUID> e : alliances.entrySet()) {
            if (!saved.contains(e.getKey()) && !saved.contains(e.getValue())) {
                dataConfig.set("alliances." + e.getKey(), e.getValue().toString());
                saved.add(e.getKey()); saved.add(e.getValue());
            }
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { plugin.getLogger().severe("alliances save: " + e.getMessage()); }
    }

    public UUID getAlly(UUID id) { return alliances.get(id); }
    public boolean areAllied(UUID a, UUID b) { UUID ally = alliances.get(a); return ally != null && ally.equals(b); }
    public boolean hasAlliance(UUID id) { return alliances.containsKey(id); }

    public void sendInvite(UUID from, UUID to) { pendingInvites.put(to, from); inviteTimes.put(to, System.currentTimeMillis()); }

    public UUID getInviter(UUID invitee) {
        UUID inv = pendingInvites.get(invitee);
        if (inv == null) return null;
        Long t = inviteTimes.get(invitee);
        if (t == null || System.currentTimeMillis() - t > INVITE_EXPIRE_MS) { pendingInvites.remove(invitee); inviteTimes.remove(invitee); return null; }
        return inv;
    }

    public void clearInvite(UUID invitee) { pendingInvites.remove(invitee); inviteTimes.remove(invitee); }

    public void formAlliance(UUID a, UUID b) {
        breakAlliance(a); breakAlliance(b);
        alliances.put(a, b); alliances.put(b, a); save();
    }

    public void breakAlliance(UUID id) {
        UUID ally = alliances.remove(id);
        if (ally != null) alliances.remove(ally);
        save();
    }
}
