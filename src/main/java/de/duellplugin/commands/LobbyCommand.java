package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyCommand implements CommandExecutor {

    private final DuellPlugin plugin;

    public LobbyCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        if (plugin.getDuellManager().isInDuel(player.getUniqueId())) {
            player.sendMessage("§cDu bist in einem Duell! Du kannst nicht zur Lobby!");
            return true;
        }

        if (plugin.getBotManager().isInBotFight(player.getUniqueId())) {
            player.sendMessage("§cDu bist in einem Bot-Kampf! Du kannst nicht zur Lobby!");
            return true;
        }

        plugin.getLobbyManager().sendToLobby(player);
        player.sendMessage("§aDu wurdest zur Lobby teleportiert!");
        return true;
    }
}
