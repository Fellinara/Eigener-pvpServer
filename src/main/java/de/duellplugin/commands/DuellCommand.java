package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.gui.DuellGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DuellCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public DuellCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DuellPlugin§8] ");

        if (args.length == 0) {
            new DuellGUI(plugin).open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "fordern", "challenge" -> {
                if (args.length < 2) {
                    player.sendMessage(prefix + "§cBenutze: /duell fordern <Spieler>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(prefix + "§cSpieler nicht gefunden!");
                    return true;
                }
                plugin.getDuellManager().sendRequest(player, target);
            }
            case "annehmen", "accept" -> plugin.getDuellManager().acceptRequest(player);
            case "ablehnen", "decline" -> plugin.getDuellManager().declineRequest(player);
            case "info" -> {
                player.sendMessage("§6§l━━━ Duell-Plugin Info ━━━");
                player.sendMessage("§e/duell §7- Duell-Menü öffnen");
                player.sendMessage("§e/duell fordern <Spieler> §7- Spieler herausfordern");
                player.sendMessage("§e/duell annehmen §7- Duell annehmen");
                player.sendMessage("§e/duell ablehnen §7- Duell ablehnen");
                player.sendMessage("§e/bot [level] §7- Bot-Kampf starten");
                player.sendMessage("§e/kit [name] §7- Kit auswählen");
                player.sendMessage("§e/stats [spieler] §7- Statistiken anzeigen");
                player.sendMessage("§e/arena §7- Arena-Verwaltung (Admin)");
                player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━");
            }
            default -> player.sendMessage(prefix + "§cUnbekannter Befehl! §e/duell info");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("fordern", "annehmen", "ablehnen", "info"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("fordern") || args[0].equalsIgnoreCase("challenge"))) {
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equalsIgnoreCase(sender.getName())) {
                    players.add(p.getName());
                }
            }
            return filterStartsWith(players, args[1]);
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
