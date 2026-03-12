package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.HomeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DelHomeCommand implements CommandExecutor {

    private final KlassenPlugin plugin;

    public DelHomeCommand(KlassenPlugin plugin) {
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

        if (args.length == 0) {
            player.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /delhome <name>"));
            return true;
        }

        HomeManager homeManager = plugin.getHomeManager();
        String homeName = args[0];

        boolean deleted = homeManager.deleteHome(player.getUniqueId(), homeName);
        if (!deleted) {
            String msg = plugin.getMessage("home-not-found").replace("{name}", homeName);
            player.sendMessage(KlassenPlugin.colorizeComponent(msg));
        } else {
            String msg = plugin.getMessage("home-deleted").replace("{name}", homeName);
            player.sendMessage(KlassenPlugin.colorizeComponent(msg));
        }
        return true;
    }
}
