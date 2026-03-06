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
                player.sendMessage(prefix + "§7Setze die Reset-Region mit §e/arena setpos1 " + name + " §7und §e/arena setpos2 " + name);
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
            case "setpos1" -> {
                if (args.length < 2) {
                    player.sendMessage(prefix + "§cBenutze: /arena setpos1 <Name>");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (!plugin.getArenaManager().arenaExists(name)) {
                    player.sendMessage(prefix + "§cArena '§6" + name + "§c' existiert nicht!");
                    return true;
                }
                plugin.getArenaManager().setRegionPos(name, 1, player.getLocation().getBlock().getLocation());
                player.sendMessage(prefix + "§aPosition 1 §7(" + formatLoc(player.getLocation())
                        + "§7) §afür Arena '§6" + name + "§a' gesetzt!");
                var arena = plugin.getArenaManager().getArena(name);
                if (arena != null && arena.isRegionDefined()) {
                    player.sendMessage(prefix + "§aBeide Positionen gesetzt – Snapshot wurde aufgenommen.");
                }
            }
            case "setpos2" -> {
                if (args.length < 2) {
                    player.sendMessage(prefix + "§cBenutze: /arena setpos2 <Name>");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (!plugin.getArenaManager().arenaExists(name)) {
                    player.sendMessage(prefix + "§cArena '§6" + name + "§c' existiert nicht!");
                    return true;
                }
                plugin.getArenaManager().setRegionPos(name, 2, player.getLocation().getBlock().getLocation());
                player.sendMessage(prefix + "§aPosition 2 §7(" + formatLoc(player.getLocation())
                        + "§7) §afür Arena '§6" + name + "§a' gesetzt!");
                var arena = plugin.getArenaManager().getArena(name);
                if (arena != null && arena.isRegionDefined()) {
                    player.sendMessage(prefix + "§aBeide Positionen gesetzt – Snapshot wurde aufgenommen.");
                }
            }
            case "snapshot" -> {
                if (args.length < 2) {
                    player.sendMessage(prefix + "§cBenutze: /arena snapshot <Name>");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (!plugin.getArenaManager().arenaExists(name)) {
                    player.sendMessage(prefix + "§cArena '§6" + name + "§c' existiert nicht!");
                    return true;
                }
                var arena = plugin.getArenaManager().getArena(name);
                if (arena == null || !arena.isRegionDefined()) {
                    player.sendMessage(prefix + "§cFür Arena '§6" + name + "§c' wurden noch keine Regionsgrenzen gesetzt!");
                    player.sendMessage(prefix + "§7Nutze §e/arena setpos1 " + name + " §7und §e/arena setpos2 " + name);
                    return true;
                }
                plugin.getArenaManager().takeSnapshot(name);
                player.sendMessage(prefix + "§aSnapshot für Arena '§6" + name + "§a' aufgenommen!");
            }
            case "setcrystal", "crystal" -> {
                if (args.length < 2) {
                    player.sendMessage(prefix + "§cBenutze: /arena setcrystal <Name>");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (!plugin.getArenaManager().arenaExists(name)) {
                    player.sendMessage(prefix + "§cArena '§6" + name + "§c' existiert nicht!");
                    return true;
                }
                var arena = plugin.getArenaManager().getArena(name);
                boolean newState = (arena == null || !arena.isCrystalArena());
                plugin.getArenaManager().setCrystalArena(name, newState);
                player.sendMessage(prefix + "§aArena '§6" + name + "§a' ist jetzt "
                        + (newState ? "§d§lCrystal-Arena§a!" : "§7keine Crystal-Arena mehr§a."));
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
                        String crystal = arena.isCrystalArena() ? " §d[Crystal]" : "";
                        String region = arena.isRegionDefined() ? "§a✔ Region" : "§c✘ Keine Region";
                        String snap = (arena.getSnapshot() != null && !arena.getSnapshot().isEmpty())
                                ? " §a✔ Snapshot" : " §c✘ Kein Snapshot";
                        player.sendMessage("§e" + arena.getName() + crystal + " §7- " + status + " §7| " + region + snap);
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
        player.sendMessage("§e/arena setpos1 <Name> §7- Reset-Region Ecke 1 setzen");
        player.sendMessage("§e/arena setpos2 <Name> §7- Reset-Region Ecke 2 setzen");
        player.sendMessage("§e/arena snapshot <Name> §7- Jetzt Snapshot aufnehmen");
        player.sendMessage("§e/arena setcrystal <Name> §7- Crystal-Arena umschalten");
        player.sendMessage("§e/arena list §7- Alle Arenen anzeigen");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private String formatLoc(org.bukkit.Location loc) {
        return "§e" + loc.getBlockX() + "§7, §e" + loc.getBlockY() + "§7, §e" + loc.getBlockZ() + " ";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("create", "delete", "setspawn", "setpos1", "setpos2", "snapshot", "setcrystal", "list"), args[0]);
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
