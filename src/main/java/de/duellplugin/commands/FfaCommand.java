package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class FfaCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public FfaCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            int count = plugin.getFfaManager().getPlayerCount();
            boolean spawnSet = plugin.getFfaManager().getFfaSpawn() != null;
            sender.sendMessage(prefix + "§e--- FFA Informationen ---");
            sender.sendMessage(prefix + "§7Aktive Spieler: §a" + count);
            sender.sendMessage(prefix + "§7Spawn gesetzt: " + (spawnSet ? "§a✔" : "§c✘"));
            sender.sendMessage(prefix + "§7Befehle: §f/ffa join §7| §f/ffa leave");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join", "beitreten" -> plugin.getFfaManager().joinFfa(player);
            case "leave", "verlassen" -> plugin.getFfaManager().leaveFfa(player);
            case "setspawn" -> {
                if (!player.hasPermission("duell.admin")) {
                    player.sendMessage(prefix + "§cDu hast keine Berechtigung!");
                    return true;
                }
                plugin.getFfaManager().setFfaSpawn(player.getLocation());
                player.sendMessage(prefix + "§aFFA-Spawn wurde auf deine aktuelle Position gesetzt.");
            }
            default -> player.sendMessage(prefix + "§cBenutzung: §f/ffa [join | leave | info | setspawn]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new java.util.ArrayList<>(List.of("join", "leave", "info"));
            if (sender.hasPermission("duell.admin")) subs.add("setspawn");
            return subs;
        }
        return List.of();
    }
}
