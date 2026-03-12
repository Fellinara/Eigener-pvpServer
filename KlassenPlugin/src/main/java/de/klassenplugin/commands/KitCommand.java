package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * /kit &lt;name&gt; — Gives the player a kit (replaces inventory).
 */
public class KitCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public KitCommand(KlassenPlugin plugin) {
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

        if (!player.hasPermission("klassenplugin.kit")) {
            player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /kit <Name>"));
            return true;
        }

        String kitName = args[0];
        if (!plugin.getKitManager().hasKit(kitName)) {
            String msg = plugin.getMessage("kit-not-found").replace("{name}", kitName);
            player.sendMessage(KlassenPlugin.colorizeComponent(msg));
            return true;
        }

        ItemStack[] contents = plugin.getKitManager().getKit(kitName);

        // setContents replaces all slots; no need to clear first
        player.getInventory().setContents(contents);

        String msg = plugin.getMessage("kit-received").replace("{name}", kitName);
        player.sendMessage(KlassenPlugin.colorizeComponent(msg));
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
