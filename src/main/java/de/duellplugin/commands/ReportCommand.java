package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /reporten <spieler> [grund]
 * Reports a player and notifies all online OPs.
 */
public class ReportCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public ReportCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player reporter)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }

        String prefix = plugin.getPrefix();

        if (args.length < 1) {
            reporter.sendMessage(prefix + "§cBenutzung: §f/reporten <spieler> [grund]");
            return true;
        }

        String targetName = args[0];
        if (targetName.equalsIgnoreCase(reporter.getName())) {
            reporter.sendMessage(prefix + "§cDu kannst dich nicht selbst reporten!");
            return true;
        }

        // Build reason
        String reason = "Kein Grund angegeben";
        if (args.length >= 2) {
            StringBuilder sb = new StringBuilder(args[1]);
            for (int i = 2; i < args.length; i++) sb.append(" ").append(args[i]);
            reason = sb.toString();
        }

        // Build OP notification
        String opMsg = "§c§l[Report] §6" + reporter.getName()
                + " §chat §6" + targetName + " §cgemeldet!"
                + "\n§7Grund: §f" + reason;

        // Notify all online OPs
        int notified = 0;
        for (Player op : Bukkit.getOnlinePlayers()) {
            if (op.isOp()) {
                op.sendMessage(opMsg);
                notified++;
            }
        }

        reporter.sendMessage(prefix + "§aDein Report wurde §6" + notified + " §aAdmin(s) weitergeleitet!");
        if (notified == 0) {
            reporter.sendMessage(prefix + "§7Aktuell ist kein Admin online. Bitte versuche es später erneut.");
        }

        // Log to console
        plugin.getLogger().info("[Report] " + reporter.getName() + " hat " + targetName
                + " gemeldet. Grund: " + reason);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)
                        && !(sender instanceof Player player && p.equals(player))) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
