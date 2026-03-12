package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class PluginToggleCommand implements CommandExecutor {

    private final KlassenPlugin plugin;

    public PluginToggleCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.admin")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cBenutzung: /plugintoggle <enable|disable>"));
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "enable", "aktivieren", "on" -> {
                if (plugin.isPluginEnabled()) {
                    sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("plugin-already-enabled")));
                } else {
                    plugin.setPluginEnabled(true);
                    plugin.getServer().broadcast(
                            KlassenPlugin.colorizeComponent(plugin.getMessage("plugin-enabled")));
                }
            }
            case "disable", "deaktivieren", "off" -> {
                if (!plugin.isPluginEnabled()) {
                    sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("plugin-already-disabled")));
                } else {
                    plugin.setPluginEnabled(false);
                    plugin.getServer().broadcast(
                            KlassenPlugin.colorizeComponent(plugin.getMessage("plugin-disabled")));
                }
            }
            default -> sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cBenutzung: /plugintoggle <enable|disable>"));
        }

        return true;
    }
}
