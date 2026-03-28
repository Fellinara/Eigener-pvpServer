package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetLobbyCommand implements CommandExecutor {

    private final KlassenPlugin plugin;

    public SetLobbyCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only")));
            return true;
        }

        if (!player.hasPermission("klassenplugin.setlobby")) {
            player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        plugin.getLobbyManager().setLobby(player.getLocation());
        player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("lobby-set")));
        return true;
    }
}
