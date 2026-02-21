package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetLobbyCommand implements CommandExecutor {

    private final DuellPlugin plugin;

    public SetLobbyCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        if (!player.hasPermission("duell.admin")) {
            player.sendMessage(plugin.getConfig().getString("messages.no-permission",
                    "§cDu hast keine Berechtigung!"));
            return true;
        }

        plugin.getLobbyManager().setLobbySpawn(player.getLocation());

        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DuellPlugin§8] ");
        player.sendMessage(prefix + "§aLobby-Spawn gesetzt!");
        return true;
    }
}
