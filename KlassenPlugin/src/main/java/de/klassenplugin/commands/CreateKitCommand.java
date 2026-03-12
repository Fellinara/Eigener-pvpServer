package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /createkit &lt;name&gt; — Saves the executing player's current inventory as a new kit.
 * Requires {@code klassenplugin.createkit} permission (default: op).
 */
public class CreateKitCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public CreateKitCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only")));
            return true;
        }

        if (!player.hasPermission("klassenplugin.createkit")) {
            player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /createkit <Name>"));
            return true;
        }

        String kitName = args[0];

        // Save all 41 slots: 0-35 main inventory, 36-39 armour, 40 off-hand
        ItemStack[] contents = player.getInventory().getContents();

        plugin.getKitManager().createKit(kitName, contents);

        String msg = plugin.getMessage("kit-created").replace("{name}", kitName);
        player.sendMessage(KlassenPlugin.colorizeComponent(msg));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
