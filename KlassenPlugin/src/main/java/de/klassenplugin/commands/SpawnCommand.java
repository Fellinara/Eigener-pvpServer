package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.LobbyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand implements CommandExecutor {

    private final KlassenPlugin plugin;

    public SpawnCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only")));
            return true;
        }

        if (!plugin.isPluginEnabled()) {
            player.sendMessage(KlassenPlugin.colorizeComponent("&cDas Plugin ist momentan deaktiviert!"));
            return true;
        }

        if (!player.hasPermission("klassenplugin.spawn")) {
            player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        LobbyManager lobbyManager = plugin.getLobbyManager();

        if (!lobbyManager.hasSpawn()) {
            player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("spawn-not-set")));
            return true;
        }

        int cooldown = plugin.getConfig().getInt("teleport-cooldown", 3);
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), cooldown)) {
            int remaining = plugin.getCooldownManager().getRemainingSeconds(player.getUniqueId(), cooldown);
            String msg = plugin.getMessage("teleport-cooldown")
                    .replace("{seconds}", String.valueOf(remaining));
            player.sendMessage(KlassenPlugin.colorizeComponent(msg));
            return true;
        }

        plugin.getCooldownManager().setCooldown(player.getUniqueId());
        plugin.getBackManager().setLastLocation(player.getUniqueId(), player.getLocation());
        player.teleport(lobbyManager.getSpawn());
        player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("spawn-teleported")));
        return true;
    }
}
