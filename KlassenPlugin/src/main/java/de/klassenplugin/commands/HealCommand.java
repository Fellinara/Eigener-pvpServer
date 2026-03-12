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
 * /heal [player] — Fully heals a player.
 * Without an argument: heals yourself. With an argument: heals another player (requires extra permission).
 */
public class HealCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public HealCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.heal")) {
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

        target.setHealth(target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        target.setFireTicks(0);

        String msgTarget = plugin.getMessage("heal-healed");
        target.sendMessage(KlassenPlugin.colorizeComponent(msgTarget));

        if (!target.equals(sender)) {
            String msgSender = plugin.getMessage("heal-healed-other").replace("{player}", target.getName());
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
