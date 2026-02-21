package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.gui.KitGUI;
import de.duellplugin.models.Kit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class KitCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public KitCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        if (args.length == 0) {
            new KitGUI(plugin).open(player);
            return true;
        }

        String kitName = args[0].toLowerCase();
        Kit kit = plugin.getKitManager().getKit(kitName);

        if (kit == null) {
            String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DuellPlugin§8] ");
            player.sendMessage(prefix + "§cKit '§6" + kitName + "§c' nicht gefunden!");
            player.sendMessage(prefix + "§7Verfügbare Kits:");
            for (Kit k : plugin.getKitManager().getAllKits()) {
                player.sendMessage("§7 - §e" + k.getName() + " §7- " + k.getDisplayName());
            }
            return true;
        }

        var stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());
        stats.setSelectedKit(kit.getName());
        plugin.getStatsManager().saveStats();

        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DuellPlugin§8] ");
        player.sendMessage(prefix + "§aKit §6" + kit.getDisplayName() + " §aausgewählt!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> kitNames = new ArrayList<>();
            for (Kit k : plugin.getKitManager().getAllKits()) {
                if (k.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    kitNames.add(k.getName());
                }
            }
            return kitNames;
        }
        return List.of();
    }
}
