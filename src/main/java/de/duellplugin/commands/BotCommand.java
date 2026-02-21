package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.gui.BotGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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

        if (args.length == 0) {
            new BotGUI(plugin).open(player);
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DuellPlugin§8] ");
            player.sendMessage(prefix + "§cUngültiges Level! Benutze eine Zahl von 1-100.");
            return true;
        }

        if (level < 1 || level > 100) {
            String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DuellPlugin§8] ");
            player.sendMessage(prefix + "§cLevel muss zwischen 1 und 100 liegen!");
            return true;
        }

        plugin.getBotManager().startBotFight(player, level);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> levels = new ArrayList<>();
            for (int i = 1; i <= 100; i += 10) {
                String lvl = String.valueOf(i);
                if (lvl.startsWith(args[0])) {
                    levels.add(lvl);
                }
            }
            return levels;
        }
        return List.of();
    }
}
