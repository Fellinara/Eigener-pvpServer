package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.gui.StatsGUI;
import de.duellplugin.models.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StatsCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public StatsCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        if (args.length == 0) {
            new StatsGUI(plugin).open(player);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target != null && target.isOnline()) {
            PlayerStats stats = plugin.getStatsManager().getOrCreateStats(target.getUniqueId(), target.getName());
            sendStats(player, target.getName(), stats);
        } else {
            player.sendMessage("§cSpieler nicht gefunden oder offline!");
        }
        return true;
    }

    private void sendStats(Player viewer, String targetName, PlayerStats stats) {
        viewer.sendMessage("§6§l━━━ Statistiken: " + targetName + " ━━━");
        viewer.sendMessage("§7ELO: §f" + stats.getElo() + " " + StatsGUI.getEloRank(stats.getElo()));
        viewer.sendMessage("§7Siege: §a" + stats.getWins() + " §7| Niederlagen: §c" + stats.getLosses());
        viewer.sendMessage("§7K/D: §f" + String.format("%.2f", stats.getKD())
                + " §7| Gewinnrate: §f" + String.format("%.1f%%", stats.getWinRate()));
        viewer.sendMessage("§7Kill-Streak: §e" + stats.getKillStreak()
                + " §7| Bestleistung: §6" + stats.getBestKillStreak());
        viewer.sendMessage("§7Bot-Siege: §a" + stats.getBotWins()
                + " §7| Höchstes Level: §6" + stats.getHighestBotLevel());
        viewer.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    players.add(p.getName());
                }
            }
            return players;
        }
        return List.of();
    }
}
