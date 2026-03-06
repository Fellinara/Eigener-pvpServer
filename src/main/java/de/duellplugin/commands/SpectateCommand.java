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
import java.util.UUID;

public class SpectateCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public SpectateCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl nutzen!");
            return true;
        }

        String prefix = plugin.getPrefix();

        // /unspectate or /spectate with no args while already spectating → stop
        if (label.equalsIgnoreCase("unspectate")
                || (args.length == 0 && plugin.getSpectateManager().isSpectating(player.getUniqueId()))) {
            if (!plugin.getSpectateManager().isSpectating(player.getUniqueId())) {
                player.sendMessage(prefix + "§cDu schaust gerade keinem Duell zu.");
                return true;
            }
            plugin.getSpectateManager().stopSpectating(player, true);
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(prefix + "§cBenutze: /spectate <Spieler>");
            return true;
        }

        // Cannot spectate if already in a duel or bot fight
        if (plugin.getDuellManager().isInDuel(player.getUniqueId())
                || plugin.getBotManager().isInBotFight(player.getUniqueId())) {
            player.sendMessage(prefix + "§cDu kannst nicht zuschauen, während du in einem Kampf bist!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(prefix + "§cSpieler §6" + args[0] + " §cnicht gefunden!");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(prefix + "§cDu kannst dir nicht selbst zuschauen!");
            return true;
        }

        plugin.getSpectateManager().startSpectating(player, target.getUniqueId());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
