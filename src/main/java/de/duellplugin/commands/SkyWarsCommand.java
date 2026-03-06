package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.SkyWarsArena;
import de.duellplugin.models.SkyWarsGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SkyWarsCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public SkyWarsCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        boolean isAdmin = sender.hasPermission("duell.admin");

        switch (sub) {
            // ── Admin commands ────────────────────────────────────────────
            case "create", "erstellen" -> {
                if (!isAdmin) { sender.sendMessage(prefix + "§cKein Zugriff!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§cBenutzung: /sw create <name>"); return true; }
                String name = args[1].toLowerCase();
                if (plugin.getSkyWarsManager().createArena(name)) {
                    sender.sendMessage(prefix + "§aSkyWars-Arena §6" + name + " §aerstellt!");
                } else {
                    sender.sendMessage(prefix + "§cArena existiert bereits!");
                }
            }
            case "delete", "löschen" -> {
                if (!isAdmin) { sender.sendMessage(prefix + "§cKein Zugriff!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§cBenutzung: /sw delete <name>"); return true; }
                if (plugin.getSkyWarsManager().deleteArena(args[1])) {
                    sender.sendMessage(prefix + "§cArena §6" + args[1] + " §cgelöscht!");
                } else {
                    sender.sendMessage(prefix + "§cArena nicht gefunden!");
                }
            }
            case "addspawn" -> {
                if (!isAdmin) { sender.sendMessage(prefix + "§cKein Zugriff!"); return true; }
                if (!(sender instanceof Player p)) { sender.sendMessage(prefix + "§cNur für Spieler!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§cBenutzung: /sw addspawn <arena>"); return true; }
                if (plugin.getSkyWarsManager().addSpawn(args[1], p.getLocation())) {
                    sender.sendMessage(prefix + "§aSpawn zu Arena §6" + args[1] + " §ahinzugefügt!");
                } else {
                    sender.sendMessage(prefix + "§cArena nicht gefunden!");
                }
            }
            case "addchest" -> {
                if (!isAdmin) { sender.sendMessage(prefix + "§cKein Zugriff!"); return true; }
                if (!(sender instanceof Player p)) { sender.sendMessage(prefix + "§cNur für Spieler!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§cBenutzung: /sw addchest <arena>"); return true; }
                // Register the block the player is standing on/looking at as chest location
                if (plugin.getSkyWarsManager().addChest(args[1], p.getLocation().getBlock().getLocation())) {
                    sender.sendMessage(prefix + "§aKisten-Position in Arena §6" + args[1] + " §aregistriert!");
                } else {
                    sender.sendMessage(prefix + "§cArena nicht gefunden!");
                }
            }
            case "setminplayers" -> {
                if (!isAdmin) { sender.sendMessage(prefix + "§cKein Zugriff!"); return true; }
                if (args.length < 3) { sender.sendMessage(prefix + "§cBenutzung: /sw setminplayers <arena> <zahl>"); return true; }
                try {
                    int n = Integer.parseInt(args[2]);
                    if (plugin.getSkyWarsManager().setMinPlayers(args[1], n)) {
                        sender.sendMessage(prefix + "§aMindestspieler für §6" + args[1] + " §aauf §6" + n + " §agesetzt!");
                    } else {
                        sender.sendMessage(prefix + "§cArena nicht gefunden!");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(prefix + "§cUngültige Zahl!");
                }
            }
            case "start" -> {
                if (!isAdmin) { sender.sendMessage(prefix + "§cKein Zugriff!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§cBenutzung: /sw start <arena>"); return true; }
                plugin.getSkyWarsManager().startGame(args[1]);
                sender.sendMessage(prefix + "§aSkyWars in Arena §6" + args[1] + " §aerzwungen gestartet!");
            }
            case "startbot", "bot" -> {
                if (!isAdmin) { sender.sendMessage(prefix + "§cKein Zugriff!"); return true; }
                if (!(sender instanceof Player p)) { sender.sendMessage(prefix + "§cNur für Spieler!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§cBenutzung: /sw startbot <arena> [botanzahl]"); return true; }
                int botCount = args.length >= 3 ? parseIntOrDefault(args[2], 3) : 3;
                plugin.getSkyWarsManager().startBotGame(p, args[1], botCount);
            }
            // ── Player commands ───────────────────────────────────────────
            case "join", "beitreten" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(prefix + "§cNur für Spieler!"); return true; }
                String arenaName = args.length >= 2 ? args[1] : null;
                plugin.getSkyWarsManager().joinGame(p, arenaName);
            }
            case "leave", "verlassen" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(prefix + "§cNur für Spieler!"); return true; }
                plugin.getSkyWarsManager().leaveGame(p);
            }
            case "list", "liste" -> {
                sender.sendMessage(prefix + "§6SkyWars-Arenen:");
                for (SkyWarsArena arena : plugin.getSkyWarsManager().getAllArenas()) {
                    String status = arena.isInUse() ? "§cBelegt" : "§aFrei";
                    sender.sendMessage("  §7" + arena.getName() + " §8- " + status
                            + " §7(" + arena.getSpawns().size() + " Spawns, "
                            + arena.getChestLocations().size() + " Kisten, "
                            + "min. " + arena.getMinPlayers() + " Spieler)");
                }
            }
            case "info" -> {
                if (args.length < 2) { sender.sendMessage(prefix + "§cBenutzung: /sw info <arena>"); return true; }
                SkyWarsArena arena = plugin.getSkyWarsManager().getArena(args[1]);
                if (arena == null) { sender.sendMessage(prefix + "§cArena nicht gefunden!"); return true; }
                sender.sendMessage(prefix + "§6Arena: §e" + arena.getName());
                sender.sendMessage("  §7Spawns: §e" + arena.getSpawns().size());
                sender.sendMessage("  §7Kisten: §e" + arena.getChestLocations().size());
                sender.sendMessage("  §7Min. Spieler: §e" + arena.getMinPlayers());
                sender.sendMessage("  §7Max. Spieler: §e" + arena.getMaxPlayers());
                sender.sendMessage("  §7Status: " + (arena.isInUse() ? "§cIn Benutzung" : "§aFrei"));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        String p = plugin.getPrefix();
        sender.sendMessage(p + "§6§lSkyWars Befehle:");
        sender.sendMessage("  §e/sw join [arena] §7- Einer Arena beitreten");
        sender.sendMessage("  §e/sw leave §7- Arena verlassen");
        sender.sendMessage("  §e/sw list §7- Alle Arenen anzeigen");
        sender.sendMessage("  §e/sw info <arena> §7- Arena-Infos");
        if (sender.hasPermission("duell.admin")) {
            sender.sendMessage("  §c/sw create <name> §7- Arena erstellen");
            sender.sendMessage("  §c/sw delete <name> §7- Arena löschen");
            sender.sendMessage("  §c/sw addspawn <arena> §7- Spawn hinzufügen");
            sender.sendMessage("  §c/sw addchest <arena> §7- Kisten-Position registrieren");
            sender.sendMessage("  §c/sw setminplayers <arena> <n> §7- Mindest-Spieler setzen");
            sender.sendMessage("  §c/sw start <arena> §7- Spiel erzwingen starten");
            sender.sendMessage("  §c/sw startbot <arena> [anzahl] §7- Bot-Spiel starten");
        }
    }

    private int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("join", "leave", "list", "info"));
            if (sender.hasPermission("duell.admin")) {
                subs.addAll(Arrays.asList("create", "delete", "addspawn", "addchest",
                        "setminplayers", "start", "startbot"));
            }
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            List<String> subs = Arrays.asList("join", "info", "delete", "addspawn", "addchest",
                    "setminplayers", "start", "startbot");
            if (subs.contains(args[0].toLowerCase())) {
                return plugin.getSkyWarsManager().getAllArenas().stream()
                        .map(SkyWarsArena::getName)
                        .filter(n -> n.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }
}
