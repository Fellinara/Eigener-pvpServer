package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FriendManager {

    private final DuellPlugin plugin;
    /** UUID → Set of friend UUIDs (bidirectional: both sides have each other). */
    private final Map<UUID, Set<UUID>> friends;
    /** Pending requests: sender UUID → set of target UUIDs who have not yet responded. */
    private final Map<UUID, Set<UUID>> sentRequests;
    private final File friendsFile;

    public FriendManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.friends = new HashMap<>();
        this.sentRequests = new HashMap<>();
        this.friendsFile = new File(plugin.getDataFolder(), "friends.yml");
        loadFriends();
    }

    // ── Query ────────────────────────────────────────

    public boolean areFriends(UUID a, UUID b) {
        Set<UUID> set = friends.get(a);
        return set != null && set.contains(b);
    }

    public Set<UUID> getFriends(UUID uuid) {
        return friends.getOrDefault(uuid, Collections.emptySet());
    }

    public boolean hasSentRequest(UUID from, UUID to) {
        Set<UUID> set = sentRequests.get(from);
        return set != null && set.contains(to);
    }

    public boolean hasIncomingRequest(UUID target, UUID from) {
        return hasSentRequest(from, target);
    }

    // ── Commands ─────────────────────────────────────

    public boolean sendRequest(Player sender, Player target) {
        String prefix = plugin.getPrefix();

        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(prefix + "§cDu kannst dir nicht selbst eine Freundschaftsanfrage schicken!");
            return false;
        }
        if (areFriends(sender.getUniqueId(), target.getUniqueId())) {
            sender.sendMessage(prefix + "§cIhr seid bereits befreundet!");
            return false;
        }
        if (hasSentRequest(sender.getUniqueId(), target.getUniqueId())) {
            sender.sendMessage(prefix + "§cDu hast bereits eine Anfrage an §6" + target.getName() + " §cgesendet!");
            return false;
        }
        // Check if target already sent us one – auto-accept
        if (hasSentRequest(target.getUniqueId(), sender.getUniqueId())) {
            acceptRequest(sender, target.getUniqueId());
            return true;
        }
        sentRequests.computeIfAbsent(sender.getUniqueId(), k -> new HashSet<>()).add(target.getUniqueId());
        sender.sendMessage(prefix + "§aFreundschaftsanfrage an §6" + target.getName() + " §agesendet!");
        target.sendMessage(prefix + "§6" + sender.getName() + " §ehat dir eine Freundschaftsanfrage gesendet! "
                + "§a/freund annehmen " + sender.getName());
        return true;
    }

    public boolean acceptRequest(Player accepter, UUID senderUUID) {
        String prefix = plugin.getPrefix();
        if (!hasSentRequest(senderUUID, accepter.getUniqueId())) {
            accepter.sendMessage(prefix + "§cKeine ausstehende Freundschaftsanfrage von diesem Spieler!");
            return false;
        }
        // Remove the pending request
        sentRequests.get(senderUUID).remove(accepter.getUniqueId());

        // Add bidirectional friendship
        friends.computeIfAbsent(accepter.getUniqueId(), k -> new HashSet<>()).add(senderUUID);
        friends.computeIfAbsent(senderUUID, k -> new HashSet<>()).add(accepter.getUniqueId());
        saveFriends();

        accepter.sendMessage(prefix + "§aDu bist jetzt mit §6" + getNameOrId(senderUUID) + " §abefreundet!");
        Player sender = Bukkit.getPlayer(senderUUID);
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(prefix + "§6" + accepter.getName() + " §ahat deine Freundschaftsanfrage angenommen!");
        }
        return true;
    }

    public boolean denyRequest(Player denier, UUID senderUUID) {
        String prefix = plugin.getPrefix();
        if (!hasSentRequest(senderUUID, denier.getUniqueId())) {
            denier.sendMessage(prefix + "§cKeine ausstehende Anfrage von diesem Spieler!");
            return false;
        }
        sentRequests.get(senderUUID).remove(denier.getUniqueId());
        denier.sendMessage(prefix + "§cAnfrage von §6" + getNameOrId(senderUUID) + " §cabgelehnt.");
        Player sender = Bukkit.getPlayer(senderUUID);
        if (sender != null) sender.sendMessage(prefix + "§6" + denier.getName() + " §chat deine Anfrage abgelehnt.");
        return true;
    }

    public boolean removeFriend(Player player, UUID friendUUID) {
        String prefix = plugin.getPrefix();
        if (!areFriends(player.getUniqueId(), friendUUID)) {
            player.sendMessage(prefix + "§cDieser Spieler ist kein Freund von dir!");
            return false;
        }
        friends.get(player.getUniqueId()).remove(friendUUID);
        Set<UUID> otherSide = friends.get(friendUUID);
        if (otherSide != null) otherSide.remove(player.getUniqueId());
        saveFriends();

        player.sendMessage(prefix + "§7Du bist nicht mehr mit §6" + getNameOrId(friendUUID) + " §7befreundet.");
        Player friend = Bukkit.getPlayer(friendUUID);
        if (friend != null) friend.sendMessage(prefix + "§7§6" + player.getName() + " §7hat euch entfreundet.");
        return true;
    }

    /** Returns list of incoming pending requests (senders who sent this player a request). */
    public List<UUID> getIncomingRequests(UUID uuid) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, Set<UUID>> entry : sentRequests.entrySet()) {
            if (entry.getValue().contains(uuid)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    // ── Persistence ───────────────────────────────────

    private void loadFriends() {
        if (!friendsFile.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(friendsFile);
        ConfigurationSection section = config.getConfigurationSection("friends");
        if (section == null) return;
        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                List<String> list = section.getStringList(uuidStr);
                Set<UUID> set = new HashSet<>();
                for (String s : list) {
                    try { set.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                }
                friends.put(uuid, set);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveFriends() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Set<UUID>> entry : friends.entrySet()) {
            List<String> list = new ArrayList<>();
            for (UUID uid : entry.getValue()) list.add(uid.toString());
            config.set("friends." + entry.getKey().toString(), list);
        }
        try {
            config.save(friendsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Freundesliste: " + e.getMessage());
        }
    }

    private String getNameOrId(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();
        // Try offline player
        var op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
    }
}
