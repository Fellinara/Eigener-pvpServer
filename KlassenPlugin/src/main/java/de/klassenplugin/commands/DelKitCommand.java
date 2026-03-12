package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * /delkit &lt;name&gt; — Deletes an existing kit.
 */
public class DelKitCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public DelKitCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.createkit")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /delkit <Name>"));
            return true;
        }

        String kitName = args[0];
        if (!plugin.getKitManager().deleteKit(kitName)) {
            String msg = plugin.getMessage("kit-not-found").replace("{name}", kitName);
            sender.sendMessage(KlassenPlugin.colorizeComponent(msg));
        } else {
            String msg = plugin.getMessage("kit-deleted").replace("{name}", kitName);
            sender.sendMessage(KlassenPlugin.colorizeComponent(msg));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String input = args[0].toLowerCase();
            for (String name : plugin.getKitManager().getKitNames()) {
                if (name.startsWith(input)) names.add(name);
            }
            return names;
        }
        return List.of();
    }
}
