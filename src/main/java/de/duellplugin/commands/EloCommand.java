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

/**
 * /elo give   <spieler> <menge>  – adds ELO   (OP only)
 * /elo remove <spieler> <menge>  – removes ELO (OP only)
 * /elo set    <spieler> <menge>  – sets ELO    (OP only)
 */
public class EloCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public EloCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("duell.admin")) {
            sender.sendMessage("§cKeine Berechtigung!");
            return true;
        }

        String prefix = plugin.getPrefix();

        if (args.length < 3) {
            sendHelp(sender, prefix);
            return true;
        }

        String action     = args[0].toLowerCase();
        String targetName = args[1];
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + "§cUngültige Menge: §f" + args[2] + " §c(muss eine positive Zahl sein)");
            return true;
        }

        // Try online player first, then offline lookup by name
        PlayerStats stats = null;
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            stats = plugin.getStatsManager().getOrCreateStats(online.getUniqueId(), online.getName());
        } else {
            stats = plugin.getStatsManager().getStatsByName(targetName);
        }

        if (stats == null) {
            sender.sendMessage(prefix + "§cSpieler '§6" + targetName + "§c' nicht gefunden! (Hat der Spieler den Server schon betreten?)");
            return true;
        }

        int oldElo = stats.getElo();

        switch (action) {
            case "give", "geben", "add" -> {
                stats.setElo(oldElo + amount);
                String msg = prefix + "§a" + amount + " ELO an §6" + stats.getName() + " §avergeben. (§7" + oldElo + " §a→ §7" + stats.getElo() + "§a)";
                sender.sendMessage(msg);
                if (online != null) online.sendMessage(prefix + "§aEin Admin hat dir §6+" + amount + " ELO §avergeben! (Gesamt: §7" + stats.getElo() + "§a)");
            }
            case "remove", "nehmen", "sub" -> {
                stats.setElo(Math.max(0, oldElo - amount));
                String msg = prefix + "§c" + amount + " ELO von §6" + stats.getName() + " §centfernt. (§7" + oldElo + " §c→ §7" + stats.getElo() + "§c)";
                sender.sendMessage(msg);
                if (online != null) online.sendMessage(prefix + "§cEin Admin hat dir §6-" + amount + " ELO §centfernt. (Gesamt: §7" + stats.getElo() + "§c)");
            }
            case "set", "setzen" -> {
                stats.setElo(amount);
                String msg = prefix + "§eELO von §6" + stats.getName() + " §eauf §6" + amount + " §egesetzt. (war: §7" + oldElo + "§e)";
                sender.sendMessage(msg);
                if (online != null) online.sendMessage(prefix + "§eEin Admin hat dein ELO auf §6" + amount + " §egesetzt.");
            }
            default -> {
                sendHelp(sender, prefix);
                return true;
            }
        }

        plugin.getStatsManager().saveStats();
        return true;
    }

    private void sendHelp(CommandSender sender, String prefix) {
        sender.sendMessage("§6§l━━━ ELO-Befehle (Admin) ━━━");
        sender.sendMessage("§e/elo give <spieler> <menge>   §7- ELO hinzufügen");
        sender.sendMessage("§e/elo remove <spieler> <menge> §7- ELO entfernen");
        sender.sendMessage("§e/elo set <spieler> <menge>    §7- ELO setzen");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("duell.admin")) return List.of();
        if (args.length == 1) return List.of("give", "remove", "set");
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            String partial = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }
}
