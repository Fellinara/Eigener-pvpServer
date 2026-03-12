package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * /kits — Lists all available kits.
 */
public class KitsCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public KitsCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!plugin.isPluginEnabled() && sender instanceof Player) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cDas Plugin ist momentan deaktiviert!"));
            return true;
        }

        if (!sender.hasPermission("klassenplugin.kit")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        Set<String> kitNames = plugin.getKitManager().getKitNames();
        if (kitNames.isEmpty()) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("kits-empty")));
        } else {
            String list = String.join("&8, &e", kitNames);
            String msg = plugin.getMessage("kits-list").replace("{kits}", list);
            sender.sendMessage(KlassenPlugin.colorizeComponent(msg));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
