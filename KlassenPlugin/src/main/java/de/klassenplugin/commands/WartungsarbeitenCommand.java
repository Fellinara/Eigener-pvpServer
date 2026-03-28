package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * /wartungsarbeiten [on|off] – toggles maintenance mode.
 */
public class WartungsarbeitenCommand implements CommandExecutor {

    private final KlassenPlugin plugin;

    public WartungsarbeitenCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.maintenance")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        var maintenance = plugin.getMaintenanceManager();

        if (args.length > 0 && args[0].equalsIgnoreCase("off")) {
            if (!maintenance.isActive()) {
                sender.sendMessage(KlassenPlugin.colorizeComponent(
                        "&cWartungsmodus ist bereits deaktiviert!"));
                return true;
            }
            maintenance.disable(sender.getName());
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("on")) {
            if (maintenance.isActive()) {
                sender.sendMessage(KlassenPlugin.colorizeComponent(
                        "&cWartungsmodus ist bereits aktiv!"));
                return true;
            }
            maintenance.enable(sender.getName());
            return true;
        }

        // No argument → toggle.
        if (maintenance.isActive()) {
            maintenance.disable(sender.getName());
        } else {
            maintenance.enable(sender.getName());
        }
        return true;
    }
}
