package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.PlayerStats;
import de.duellplugin.models.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RankCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public RankCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        String prefix = plugin.getPrefix();
        PlayerStats stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            player.sendMessage(prefix + "§eAktueller Rang: " + stats.getRank().getDisplayName());
            player.sendMessage(prefix + "§7Dein ELO: §6" + stats.getElo());
            player.sendMessage(prefix + "§7Kaufbare Ränge:");
            for (Rank rank : Rank.values()) {
                if (rank == Rank.SPIELER) continue;
                String status;
                if (stats.getRank().ordinal() >= rank.ordinal()) {
                    status = "§a✔ Besessen";
                } else if (stats.getElo() >= rank.getEloRequired()) {
                    status = "§e" + rank.getEloCost() + " ELO Kosten";
                } else {
                    status = "§c" + rank.getEloRequired() + " ELO benötigt";
                }
                player.sendMessage("  §7- " + rank.getDisplayName() + " §8» §7" + status);
            }
            player.sendMessage(prefix + "§7Nutze §f/rang kaufen <rang> §7um einen Rang zu kaufen.");
            return true;
        }

        if (args[0].equalsIgnoreCase("kaufen") || args[0].equalsIgnoreCase("buy")) {
            if (args.length < 2) {
                player.sendMessage(prefix + "§cBenutzung: §f/rang kaufen <rang>");
                return true;
            }

            Rank targetRank = Rank.fromName(args[1]);
            if (targetRank == null || targetRank == Rank.SPIELER) {
                player.sendMessage(prefix + "§cUnbekannter Rang: §e" + args[1]);
                player.sendMessage(prefix + "§7Verfügbare Ränge: VIP, VIP_PLUS, ELITE, LEGENDE");
                return true;
            }

            if (stats.getRank().ordinal() >= targetRank.ordinal()) {
                player.sendMessage(prefix + "§cDu hast diesen Rang bereits!");
                return true;
            }

            if (stats.getElo() < targetRank.getEloRequired()) {
                player.sendMessage(prefix + "§cDu brauchst mindestens §6" + targetRank.getEloRequired()
                        + " ELO §cfür diesen Rang. Du hast §6" + stats.getElo() + " ELO§c.");
                return true;
            }

            if (stats.getElo() < targetRank.getEloCost()) {
                player.sendMessage(prefix + "§cNicht genug ELO! Kosten: §6" + targetRank.getEloCost()
                        + " ELO§c, du hast §6" + stats.getElo() + " ELO§c.");
                return true;
            }

            // Purchase the rank
            stats.setElo(stats.getElo() - targetRank.getEloCost());
            stats.setRank(targetRank);
            plugin.getStatsManager().saveStats();

            player.sendMessage(prefix + "§aGlückwunsch! Du hast den Rang "
                    + targetRank.getDisplayName() + " §agekauft!");
            player.sendMessage(prefix + "§7Verbleibende ELO: §6" + stats.getElo());
            return true;
        }

        player.sendMessage(prefix + "§cBenutzung: §f/rang [info | kaufen <rang>]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("info", "kaufen");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("kaufen") || args[0].equalsIgnoreCase("buy"))) {
            List<String> result = new ArrayList<>();
            String partial = args[1].toLowerCase();
            for (Rank rank : Rank.values()) {
                if (rank == Rank.SPIELER) continue;
                String name = rank.name().toLowerCase();
                if (name.startsWith(partial)) result.add(rank.name());
            }
            return result;
        }
        return List.of();
    }
}
