package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class FarmCodeCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public FarmCodeCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        String prefix = plugin.getPrefix();

        if (args.length == 0) {
            player.sendMessage(prefix + "§eVerfügbare Farmcodes: §f/farmcode <code>");
            player.sendMessage(prefix + "§7Codes: §fCOBWEB, GAPPLE, ARROWS, PEARLS, CLASSIC, BUILD, COMBO, GOMME");
            return true;
        }

        String code = args[0];
        boolean success = plugin.getFarmCodeManager().redeemCode(player, code);
        if (!success) {
            player.sendMessage(prefix + "§cUnbekannter Farmcode: §e" + code);
            player.sendMessage(prefix + "§7Tippe §f/farmcode §7für eine Liste der Codes.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> results = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (String code : plugin.getFarmCodeManager().getCodeNames()) {
                if (code.startsWith(partial)) {
                    results.add(code);
                }
            }
            return results;
        }
        return List.of();
    }
}
