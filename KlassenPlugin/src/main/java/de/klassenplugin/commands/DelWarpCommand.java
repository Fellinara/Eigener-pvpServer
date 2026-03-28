package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DelWarpCommand implements CommandExecutor {

    private final KlassenPlugin plugin;

    public DelWarpCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.delwarp")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /delwarp <name>"));
            return true;
        }

        String warpName = args[0];
        boolean deleted = plugin.getWarpManager().deleteWarp(warpName);

        if (!deleted) {
            String msg = plugin.getMessage("warp-not-found").replace("{name}", warpName);
            sender.sendMessage(KlassenPlugin.colorizeComponent(msg));
        } else {
            String msg = plugin.getMessage("warp-deleted").replace("{name}", warpName);
            sender.sendMessage(KlassenPlugin.colorizeComponent(msg));
        }
        return true;
    }
}
