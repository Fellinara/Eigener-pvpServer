package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class WarpsCommand implements CommandExecutor {

    private final KlassenPlugin plugin;

    public WarpsCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!plugin.isPluginEnabled() && sender instanceof Player) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cDas Plugin ist momentan deaktiviert!"));
            return true;
        }

        if (!sender.hasPermission("klassenplugin.warp")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        Set<String> warps = plugin.getWarpManager().getWarps();

        if (warps.isEmpty()) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("warps-empty")));
        } else {
            String warpList = String.join("&8, &e", warps);
            String msg = plugin.getMessage("warps-list").replace("{warps}", warpList);
            sender.sendMessage(KlassenPlugin.colorizeComponent(msg));
        }
        return true;
    }
}
