package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.HomeManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomeCommand implements CommandExecutor {

    private final KlassenPlugin plugin;

    public HomeCommand(KlassenPlugin plugin) {
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

        if (!player.hasPermission("klassenplugin.home")) {
            player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        HomeManager homeManager = plugin.getHomeManager();
        String homeName = args.length > 0 ? args[0] : "home";

        Location loc = homeManager.getHome(player.getUniqueId(), homeName);
        if (loc == null) {
            String msg = plugin.getMessage("home-not-found").replace("{name}", homeName);
            player.sendMessage(KlassenPlugin.colorizeComponent(msg));
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
        player.teleport(loc);
        String msg = plugin.getMessage("home-teleported").replace("{name}", homeName);
        player.sendMessage(KlassenPlugin.colorizeComponent(msg));
        return true;
    }
}
