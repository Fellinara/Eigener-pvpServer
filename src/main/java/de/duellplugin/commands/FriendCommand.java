package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public FriendCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }

        String prefix = plugin.getPrefix();

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add", "hinzufügen" -> {
                if (args.length < 2) { player.sendMessage(prefix + "§cBenutzung: §f/freund add <spieler>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { player.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                plugin.getFriendManager().sendRequest(player, target);
            }
            case "remove", "entfernen" -> {
                if (args.length < 2) { player.sendMessage(prefix + "§cBenutzung: §f/freund remove <spieler>"); return true; }
                // Look up by name (online or offline)
                UUID targetUUID = getUUIDByName(args[1]);
                if (targetUUID == null) { player.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                plugin.getFriendManager().removeFriend(player, targetUUID);
            }
            case "accept", "annehmen" -> {
                if (args.length < 2) { player.sendMessage(prefix + "§cBenutzung: §f/freund annehmen <spieler>"); return true; }
                UUID senderUUID = getUUIDByName(args[1]);
                if (senderUUID == null) { player.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                plugin.getFriendManager().acceptRequest(player, senderUUID);
            }
            case "deny", "ablehnen" -> {
                if (args.length < 2) { player.sendMessage(prefix + "§cBenutzung: §f/freund ablehnen <spieler>"); return true; }
                UUID senderUUID = getUUIDByName(args[1]);
                if (senderUUID == null) { player.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                plugin.getFriendManager().denyRequest(player, senderUUID);
            }
            case "list", "liste" -> {
                var friendUUIDs = plugin.getFriendManager().getFriends(player.getUniqueId());
                if (friendUUIDs.isEmpty()) {
                    player.sendMessage(prefix + "§7Du hast noch keine Freunde. §e/freund add <spieler>");
                    return true;
                }
                player.sendMessage("§6§l━━━ Freunde (" + friendUUIDs.size() + ") ━━━");
                for (UUID uid : friendUUIDs) {
                    Player friend = Bukkit.getPlayer(uid);
                    String name = friend != null ? friend.getName()
                            : Bukkit.getOfflinePlayer(uid).getName();
                    if (name == null) name = uid.toString().substring(0, 8);
                    boolean online = friend != null && friend.isOnline();
                    player.sendMessage("§7- §e" + name + (online ? " §a[Online]" : " §8[Offline]"));
                }
                player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━");
            }
            case "requests", "anfragen" -> {
                var requests = plugin.getFriendManager().getIncomingRequests(player.getUniqueId());
                if (requests.isEmpty()) {
                    player.sendMessage(prefix + "§7Keine offenen Freundschaftsanfragen.");
                    return true;
                }
                player.sendMessage("§6§l━━━ Offene Anfragen ━━━");
                for (UUID uid : requests) {
                    Player req = Bukkit.getPlayer(uid);
                    String name = req != null ? req.getName()
                            : Bukkit.getOfflinePlayer(uid).getName();
                    if (name == null) name = uid.toString().substring(0, 8);
                    player.sendMessage("§7- §e" + name + " §8» §a/freund annehmen " + name + " §8| §c/freund ablehnen " + name);
                }
                player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━");
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l━━━ Freunde-Befehle ━━━");
        player.sendMessage("§e/freund add <spieler> §7- Anfrage senden");
        player.sendMessage("§e/freund remove <spieler> §7- Entfreunden");
        player.sendMessage("§e/freund annehmen <spieler> §7- Anfrage annehmen");
        player.sendMessage("§e/freund ablehnen <spieler> §7- Anfrage ablehnen");
        player.sendMessage("§e/freund liste §7- Freundesliste");
        player.sendMessage("§e/freund anfragen §7- Eingehende Anfragen");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━");
    }

    private UUID getUUIDByName(String name) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) return online.getUniqueId();
        var offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove", "annehmen", "ablehnen", "liste", "anfragen");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")
                || args[0].equalsIgnoreCase("annehmen") || args[0].equalsIgnoreCase("ablehnen"))) {
            List<String> names = new ArrayList<>();
            String partial = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!(sender instanceof Player sp) || !p.equals(sp)) {
                    if (p.getName().toLowerCase().startsWith(partial)) names.add(p.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
