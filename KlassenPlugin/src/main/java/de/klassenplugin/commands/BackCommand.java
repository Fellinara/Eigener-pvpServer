package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /back — Teleports the player to their last location (before a teleport or after death).
 */
public class BackCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public BackCommand(KlassenPlugin plugin) {
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

        if (!player.hasPermission("klassenplugin.back")) {
            player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        if (!plugin.getBackManager().hasLastLocation(player.getUniqueId())) {
            player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("back-no-location")));
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

        Location backLoc = plugin.getBackManager().getLastLocation(player.getUniqueId());
        Location currentLoc = player.getLocation();
        // Swap: set current as new "last" so the player can /back again to come back
        plugin.getBackManager().setLastLocation(player.getUniqueId(), currentLoc);
        plugin.getCooldownManager().setCooldown(player.getUniqueId());
        player.teleport(backLoc);
        player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("back-teleported")));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
