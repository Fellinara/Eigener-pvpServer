package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RankManager {

    private final KlassenPlugin plugin;
    private File ranksFile;
    private FileConfiguration ranksConfig;

    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private final Map<String, String> prefixes = new HashMap<>();
    private final Map<String, List<String>> rankPermissions = new HashMap<>();
    private final Map<UUID, String> playerRanks = new HashMap<>();

    public RankManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        load();
    }

    public void load() {
        if (!ranksFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                ranksFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Fehler beim Erstellen von ranks.yml: " + e.getMessage());
            }
        }
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);

        prefixes.clear();
        rankPermissions.clear();
        playerRanks.clear();

        if (ranksConfig.contains("ranks")) {
            for (String rankName : ranksConfig.getConfigurationSection("ranks").getKeys(false)) {
                String prefix = ranksConfig.getString("ranks." + rankName + ".prefix", "");
                List<String> perms = ranksConfig.getStringList("ranks." + rankName + ".permissions");
                prefixes.put(rankName, prefix);
                rankPermissions.put(rankName, new ArrayList<>(perms));
            }
        }

        if (ranksConfig.contains("players")) {
            for (String uuidStr : ranksConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String rank = ranksConfig.getString("players." + uuidStr);
                    if (rank != null) playerRanks.put(uuid, rank);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Ungültige UUID in ranks.yml: " + uuidStr);
                }
            }
        }
    }

    public void save() {
        ranksConfig.set("ranks", null);
        ranksConfig.set("players", null);

        for (String rankName : prefixes.keySet()) {
            ranksConfig.set("ranks." + rankName + ".prefix", prefixes.get(rankName));
            ranksConfig.set("ranks." + rankName + ".permissions", rankPermissions.getOrDefault(rankName, new ArrayList<>()));
        }

        for (Map.Entry<UUID, String> entry : playerRanks.entrySet()) {
            ranksConfig.set("players." + entry.getKey().toString(), entry.getValue());
        }

        try {
            ranksConfig.save(ranksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern von ranks.yml: " + e.getMessage());
        }
    }

    public boolean createRank(String name) {
        if (prefixes.containsKey(name)) return false;
        prefixes.put(name, "");
        rankPermissions.put(name, new ArrayList<>());
        save();
        return true;
    }

    public boolean deleteRank(String name) {
        if (!prefixes.containsKey(name)) return false;
        prefixes.remove(name);
        rankPermissions.remove(name);
        playerRanks.entrySet().removeIf(e -> name.equals(e.getValue()));
        save();
        return true;
    }

    public void setPrefix(String rankName, String prefix) {
        prefixes.put(rankName, prefix);
        save();
        // Refresh tab list name for all online players who hold this rank.
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (rankName.equals(playerRanks.get(p.getUniqueId()))) {
                updateTabListName(p);
            }
        }
    }

    public String getPrefix(String rankName) {
        return prefixes.getOrDefault(rankName, "");
    }

    public void addPermission(String rankName, String perm) {
        rankPermissions.computeIfAbsent(rankName, k -> new ArrayList<>()).add(perm);
        save();
    }

    public void removePermission(String rankName, String perm) {
        List<String> perms = rankPermissions.get(rankName);
        if (perms != null) {
            perms.remove(perm);
            save();
        }
    }

    public List<String> getPermissions(String rankName) {
        return Collections.unmodifiableList(rankPermissions.getOrDefault(rankName, new ArrayList<>()));
    }

    public Set<String> getRankNames() {
        return Collections.unmodifiableSet(prefixes.keySet());
    }

    public boolean rankExists(String name) {
        return prefixes.containsKey(name);
    }

    public void assignRank(UUID playerId, String rankName) {
        playerRanks.put(playerId, rankName);
        save();
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            applyRankToPlayer(player);
        }
    }

    public void removePlayerRank(UUID playerId) {
        playerRanks.remove(playerId);
        save();
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            removeRankFromPlayer(player);
        }
    }

    public String getPlayerRank(UUID playerId) {
        return playerRanks.get(playerId);
    }

    public void applyRankToPlayer(Player player) {
        removeRankFromPlayer(player);

        String rankName = playerRanks.get(player.getUniqueId());
        if (rankName == null) {
            String defaultRank = getDefaultRank();
            if (!defaultRank.isEmpty() && rankExists(defaultRank)) {
                rankName = defaultRank;
            }
        }
        if (rankName == null || !rankExists(rankName)) return;

        PermissionAttachment att = player.addAttachment(plugin);
        for (String perm : rankPermissions.getOrDefault(rankName, new ArrayList<>())) {
            att.setPermission(perm, true);
        }
        attachments.put(player.getUniqueId(), att);
        player.setMetadata("klassenpluginRank", new FixedMetadataValue(plugin, rankName));
        updateTabListName(player);
    }

    public void removeRankFromPlayer(Player player) {
        PermissionAttachment att = attachments.remove(player.getUniqueId());
        if (att != null) {
            player.removeAttachment(att);
        }
        player.removeMetadata("klassenpluginRank", plugin);
        player.playerListName(null);
    }

    public String getDefaultRank() {
        return plugin.getConfig().getString("ranks.default-rank", "");
    }

    /**
     * Returns the raw &amp;-colour-coded prefix for the given player,
     * falling back to the default rank. Empty string if no rank/prefix.
     */
    public String getPlayerPrefix(UUID playerId) {
        String rankName = playerRanks.get(playerId);
        if (rankName == null || !rankExists(rankName)) {
            String def = getDefaultRank();
            if (!def.isEmpty() && rankExists(def)) {
                rankName = def;
            } else {
                return "";
            }
        }
        return prefixes.getOrDefault(rankName, "");
    }

    /** Updates the tab-list display name of a player to show their rank prefix. */
    public void updateTabListName(Player player) {
        String prefix = getPlayerPrefix(player.getUniqueId());
        if (prefix.isEmpty()) {
            player.playerListName(null);
            return;
        }
        Component prefixComp = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + " ");
        player.playerListName(Component.text().append(prefixComp)
                .append(Component.text(player.getName())).build());
    }
}
