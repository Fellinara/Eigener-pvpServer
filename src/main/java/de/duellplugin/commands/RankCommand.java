package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.PlayerStats;
import de.duellplugin.models.Rank;
import org.bukkit.Bukkit;
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
        String prefix = plugin.getPrefix();

        // /rang setrang <player> <rang> — admin only, works from console too
        if (args.length >= 1 && args[0].equalsIgnoreCase("setrang")) {
            if (!sender.hasPermission("duell.setrang")) {
                sender.sendMessage(prefix + "§cDu hast keine Berechtigung!");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(prefix + "§cBenutzung: §f/rang setrang <spieler> <rang>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(prefix + "§cSpieler §6" + args[1] + " §cnicht gefunden!");
                return true;
            }
            Rank targetRank = Rank.fromName(args[2]);
            if (targetRank == null) {
                sender.sendMessage(prefix + "§cUnbekannter Rang: §e" + args[2]);
                return true;
            }
            PlayerStats targetStats = plugin.getStatsManager()
                    .getOrCreateStats(target.getUniqueId(), target.getName());
            targetStats.setRank(targetRank);
            plugin.getStatsManager().saveStats();
            // Refresh tab list name for the target player
            target.setPlayerListName(targetRank.getDisplayName() + " §7" + target.getName());
            sender.sendMessage(prefix + "§aRang von §6" + target.getName()
                    + " §awurde auf " + targetRank.getDisplayName() + " §agesetzt.");
            target.sendMessage(prefix + "§aDein Rang wurde auf " + targetRank.getDisplayName() + " §agesetzt!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler (außer setrang)!");
            return true;
        }

        PlayerStats stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            player.sendMessage(prefix + "§eAktueller Rang: " + stats.getRank().getDisplayName());
            player.sendMessage(prefix + "§7Dein ELO: §6" + stats.getElo());
            player.sendMessage(prefix + "§7Kaufbare Ränge:");
            for (Rank rank : Rank.values()) {
                if (rank == Rank.SPIELER || rank.isStaff()) continue;
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
            if (targetRank == null || targetRank == Rank.SPIELER || targetRank.isStaff()) {
                player.sendMessage(prefix + "§cUnbekannter oder nicht kaufbarer Rang: §e" + args[1]);
                player.sendMessage(prefix + "§7Kaufbare Ränge: VIP, VIP_PLUS, ELITE, LEGENDE");
                return true;
            }

            if (stats.getRank().ordinal() >= targetRank.ordinal()) {
                player.sendMessage(prefix + "§cDu hast diesen Rang bereits (oder einen höheren)!");
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

            stats.setElo(stats.getElo() - targetRank.getEloCost());
            stats.setRank(targetRank);
            plugin.getStatsManager().saveStats();
            // Refresh tab list name
            player.setPlayerListName(targetRank.getDisplayName() + " §7" + player.getName());

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
            List<String> subs = new ArrayList<>(List.of("info", "kaufen"));
            if (sender.hasPermission("duell.setrang")) subs.add("setrang");
            return subs;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("kaufen") || args[0].equalsIgnoreCase("buy")) {
                List<String> result = new ArrayList<>();
                String partial = args[1].toLowerCase();
                for (Rank rank : Rank.values()) {
                    if (rank == Rank.SPIELER || rank.isStaff()) continue;
                    if (rank.name().toLowerCase().startsWith(partial)) result.add(rank.name());
                }
                return result;
            }
            if (args[0].equalsIgnoreCase("setrang") && sender.hasPermission("duell.setrang")) {
                List<String> names = new ArrayList<>();
                String partial = args[1].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(partial)) names.add(p.getName());
                }
                return names;
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setrang") && sender.hasPermission("duell.setrang")) {
            List<String> result = new ArrayList<>();
            String partial = args[2].toLowerCase();
            for (Rank rank : Rank.values()) {
                if (rank.name().toLowerCase().startsWith(partial)) result.add(rank.name());
            }
            return result;
        }
        return List.of();
    }
}
