package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlaytimeCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public PlaytimeCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();

        Player target;
        String targetName;

        if (args.length >= 1) {
            // Looking up another player
            target = Bukkit.getPlayer(args[0]);
            targetName = args[0];
        } else if (sender instanceof Player) {
            target = (Player) sender;
            targetName = target.getName();
        } else {
            sender.sendMessage("§cBenutze: /playtime <Spieler>");
            return true;
        }

        PlayerStats stats;
        if (target != null) {
            stats = plugin.getStatsManager().getOrCreateStats(target.getUniqueId(), target.getName());
            targetName = target.getName();
        } else {
            // Try offline lookup by name
            stats = plugin.getStatsManager().getStatsByName(targetName);
            if (stats == null) {
                sender.sendMessage(prefix + "§cSpieler §6" + targetName + " §cwurde nicht gefunden.");
                return true;
            }
        }

        long totalSeconds = stats.getPlaytimeSeconds();
        sender.sendMessage(prefix + "§e⏱ Spielzeit von §6" + stats.getName() + "§e: §a" + formatTime(totalSeconds));
        return true;
    }

    private String formatTime(long totalSeconds) {
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }
}
