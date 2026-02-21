package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.managers.NpcManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NpcCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public NpcCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }
        if (!player.hasPermission("duell.admin")) {
            player.sendMessage(plugin.getPrefix() + "§cKeine Berechtigung!");
            return true;
        }

        String prefix = plugin.getPrefix();

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create", "erstellen" -> {
                if (args.length < 2) {
                    player.sendMessage(prefix + "§cBenutzung: §f/npc create <typ> [name]");
                    player.sendMessage(prefix + "§7Typen: §e" + getTypeList());
                    return true;
                }
                NpcManager.NpcType type = NpcManager.NpcType.fromString(args[1]);
                if (type == null) {
                    player.sendMessage(prefix + "§cUnbekannter Typ '§6" + args[1] + "§c'. Verfügbare Typen: §e" + getTypeList());
                    return true;
                }
                // Optional custom name: join remaining args
                String name = null;
                if (args.length >= 3) {
                    StringBuilder sb = new StringBuilder(args[2]);
                    for (int i = 3; i < args.length; i++) sb.append(" ").append(args[i]);
                    name = sb.toString();
                }
                UUID uid = plugin.getNpcManager().spawnNpc(type, name, player.getLocation());
                player.sendMessage(prefix + "§aNPC (§6" + type.name() + "§a) erstellt! UUID: §7" + uid);
            }
            case "remove", "löschen" -> {
                // Remove the NPC the player is looking at, or by UUID if provided
                if (args.length >= 2) {
                    try {
                        UUID uid = UUID.fromString(args[1]);
                        boolean removed = plugin.getNpcManager().removeNpc(uid);
                        player.sendMessage(removed
                                ? prefix + "§aNPC entfernt."
                                : prefix + "§cKein NPC mit dieser UUID gefunden!");
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(prefix + "§cUngültige UUID!");
                    }
                } else {
                    player.sendMessage(prefix + "§cBenutzung: §f/npc remove <uuid>");
                }
            }
            case "list", "liste" -> {
                var all = plugin.getNpcManager().getAllNpcs();
                if (all.isEmpty()) {
                    player.sendMessage(prefix + "§7Keine NPCs vorhanden.");
                    return true;
                }
                player.sendMessage("§6§l━━━ NPCs (" + all.size() + ") ━━━");
                for (var npc : all) {
                    player.sendMessage("§7- §e" + npc.type().name()
                            + " §7» §f" + npc.customName()
                            + " §8[" + npc.entityUUID().toString().substring(0, 8) + "...]");
                }
                player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━");
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l━━━ NPC-Befehle ━━━");
        player.sendMessage("§e/npc create <typ> [name] §7- NPC erstellen (an deinem Standort)");
        player.sendMessage("§e/npc remove <uuid> §7- NPC entfernen");
        player.sendMessage("§e/npc list §7- Alle NPCs anzeigen");
        player.sendMessage("§7Typen: §e" + getTypeList());
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━");
    }

    private String getTypeList() {
        StringBuilder sb = new StringBuilder();
        for (NpcManager.NpcType type : NpcManager.NpcType.values()) {
            if (sb.length() > 0) sb.append("§7, §e");
            sb.append(type.name().toLowerCase());
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("create", "remove", "list");
        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            List<String> types = new ArrayList<>();
            String partial = args[1].toLowerCase();
            for (NpcManager.NpcType t : NpcManager.NpcType.values()) {
                if (t.name().toLowerCase().startsWith(partial)) types.add(t.name().toLowerCase());
            }
            return types;
        }
        return List.of();
    }
}
