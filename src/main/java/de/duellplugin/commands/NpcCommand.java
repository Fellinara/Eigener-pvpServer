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
                if (args.length >= 2) {
                    try {
                        int number = Integer.parseInt(args[1]);
                        UUID uid = plugin.getNpcManager().getNpcUuidByNumber(number);
                        if (uid == null) {
                            player.sendMessage(prefix + "§cKein NPC mit Nummer §6" + number
                                    + "§c gefunden! §7(/npc list für eine Übersicht)");
                            return true;
                        }
                        boolean removed = plugin.getNpcManager().removeNpc(uid);
                        player.sendMessage(removed
                                ? prefix + "§aNPC §6#" + number + "§a entfernt."
                                : prefix + "§cFehler beim Entfernen des NPCs!");
                    } catch (NumberFormatException e) {
                        player.sendMessage(prefix + "§cBitte eine Nummer angeben. §7Benutze §f/npc list§7 für eine Übersicht.");
                    }
                } else {
                    player.sendMessage(prefix + "§cBenutzung: §f/npc remove <nummer>");
                }
            }
            case "list", "liste" -> {
                var all = plugin.getNpcManager().getAllNpcs();
                if (all.isEmpty()) {
                    player.sendMessage(prefix + "§7Keine NPCs vorhanden.");
                    return true;
                }
                player.sendMessage("§6§l━━━ NPCs (" + all.size() + ") ━━━");
                int num = 1;
                for (var npc : all) {
                    player.sendMessage("§7" + num++ + ". §e" + npc.type().name()
                            + " §7» §f" + npc.customName());
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
        player.sendMessage("§e/npc remove <nummer> §7- NPC nach Nummer entfernen §8(siehe /npc list)");
        player.sendMessage("§e/npc list §7- Alle NPCs mit Nummern anzeigen");
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
