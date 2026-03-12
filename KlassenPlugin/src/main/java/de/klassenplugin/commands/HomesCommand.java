package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.HomeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class HomesCommand implements CommandExecutor {

    private final KlassenPlugin plugin;

    public HomesCommand(KlassenPlugin plugin) {
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
        Set<String> homes = homeManager.getHomes(player.getUniqueId());

        if (homes.isEmpty()) {
            player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("homes-empty")));
        } else {
            String homeList = String.join("&8, &e", homes);
            String msg = plugin.getMessage("homes-list").replace("{homes}", homeList);
            player.sendMessage(KlassenPlugin.colorizeComponent(msg));
        }
        return true;
    }
}
