package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.HomeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetHomeCommand implements CommandExecutor {

    private final KlassenPlugin plugin;

    public SetHomeCommand(KlassenPlugin plugin) {
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

        int maxHomes = homeManager.getMaxHomes();
        if (maxHomes > 0 && !homeManager.hasHome(player.getUniqueId(), homeName)
                && homeManager.getHomeCount(player.getUniqueId()) >= maxHomes) {
            String msg = plugin.getMessage("home-limit").replace("{max}", String.valueOf(maxHomes));
            player.sendMessage(KlassenPlugin.colorizeComponent(msg));
            return true;
        }

        homeManager.setHome(player.getUniqueId(), homeName, player.getLocation());
        String msg = plugin.getMessage("home-set").replace("{name}", homeName);
        player.sendMessage(KlassenPlugin.colorizeComponent(msg));
        return true;
    }
}
