package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.gui.BotGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class BotCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public BotCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("start")) {
            plugin.getBotManager().startBotFight(player);
        } else {
            new BotGUI(plugin).open(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("start");
        return List.of();
    }
}
