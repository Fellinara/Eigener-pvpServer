package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArenaCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public ArenaCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        if (!player.hasPermission("duell.admin")) {
            player.sendMessage(plugin.getConfig().getString("messages.no-permission",
                    "§cDu hast keine Berechtigung!"));
            return true;
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DuellPlugin§8] ");

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create", "erstellen" -> {
                if (args.length < 2) {
                    player.sendMessage(prefix + "§cBenutze: /arena create <Name>");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (plugin.getArenaManager().arenaExists(name)) {
                    player.sendMessage(prefix + "§cEine Arena mit dem Namen '§6" + name + "§c' existiert bereits!");
                    return true;
                }
                plugin.getArenaManager().createArena(name);
                player.sendMessage(prefix + "§aArena '§6" + name + "§a' erstellt!");
                player.sendMessage(prefix + "§7Setze die Spawns mit §e/arena setspawn " + name + " <1|2>");
            }
            case "delete", "loeschen" -> {
                if (args.length < 2) {
                    player.sendMessage(prefix + "§cBenutze: /arena delete <Name>");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (!plugin.getArenaManager().arenaExists(name)) {
                    player.sendMessage(prefix + "§cArena '§6" + name + "§c' existiert nicht!");
                    return true;
                }
                plugin.getArenaManager().deleteArena(name);
                player.sendMessage(prefix + "§aArena '§6" + name + "§a' gelöscht!");
            }
            case "setspawn" -> {
                if (args.length < 3) {
                    player.sendMessage(prefix + "§cBenutze: /arena setspawn <Name> <1|2>");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (!plugin.getArenaManager().arenaExists(name)) {
                    player.sendMessage(prefix + "§cArena '§6" + name + "§c' existiert nicht!");
                    return true;
                }
                int spawn;
                try {
                    spawn = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(prefix + "§cBenutze 1 oder 2 für den Spawn!");
                    return true;
                }
                if (spawn != 1 && spawn != 2) {
                    player.sendMessage(prefix + "§cBenutze 1 oder 2 für den Spawn!");
                    return true;
                }
                plugin.getArenaManager().setSpawn(name, spawn, player.getLocation());
                player.sendMessage(prefix + "§aSpawn §6" + spawn + " §afür Arena '§6" + name + "§a' gesetzt!");
            }
            case "list", "liste" -> {
                player.sendMessage("§6§l━━━ Arenen ━━━");
                var arenas = plugin.getArenaManager().getAllArenas();
                if (arenas.isEmpty()) {
                    player.sendMessage("§7Keine Arenen vorhanden.");
                } else {
                    for (var arena : arenas) {
                        String status = arena.isReady()
                                ? (arena.isInUse() ? "§6⚔ Besetzt" : "§a✔ Bereit")
                                : "§c✘ Unvollständig";
                        player.sendMessage("§e" + arena.getName() + " §7- " + status);
                    }
                }
                player.sendMessage("§6§l━━━━━━━━━━━━━━━");
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l━━━ Arena-Verwaltung ━━━");
        player.sendMessage("§e/arena create <Name> §7- Arena erstellen");
        player.sendMessage("§e/arena delete <Name> §7- Arena löschen");
        player.sendMessage("§e/arena setspawn <Name> <1|2> §7- Spawn setzen");
        player.sendMessage("§e/arena list §7- Alle Arenen anzeigen");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("create", "delete", "setspawn", "list"), args[0]);
        }
        if (args.length == 2) {
            List<String> arenaNames = new ArrayList<>();
            for (var arena : plugin.getArenaManager().getAllArenas()) {
                arenaNames.add(arena.getName());
            }
            return filterStartsWith(arenaNames, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setspawn")) {
            return filterStartsWith(Arrays.asList("1", "2"), args[2]);
        }
        return List.of();
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }
}
