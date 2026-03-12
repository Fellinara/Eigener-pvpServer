package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * /feed [player] — Fully feeds a player.
 */
public class FeedCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public FeedCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.feed")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                String msg = plugin.getMessage("player-not-found").replace("{player}", args[0]);
                sender.sendMessage(KlassenPlugin.colorizeComponent(msg));
                return true;
            }
        } else {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only")));
                return true;
            }
            target = p;
        }

        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.setExhaustion(0f);

        String msgTarget = plugin.getMessage("feed-fed");
        target.sendMessage(KlassenPlugin.colorizeComponent(msgTarget));

        if (!target.equals(sender)) {
            String msgSender = plugin.getMessage("feed-fed-other").replace("{player}", target.getName());
            sender.sendMessage(KlassenPlugin.colorizeComponent(msgSender));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String input = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(input)) names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }
}
