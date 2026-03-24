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
 * /tpa &lt;player&gt; — Sends a teleport-to request to another player.
 */
public class TpaCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public TpaCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player requester)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only")));
            return true;
        }

        if (!plugin.isPluginEnabled()) {
            requester.sendMessage(KlassenPlugin.colorizeComponent("&cDas Plugin ist momentan deaktiviert!"));
            return true;
        }

        if (!requester.hasPermission("klassenplugin.tpa")) {
            requester.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        if (plugin.getCombatManager().isTagged(requester.getUniqueId())) {
            int seconds = plugin.getCombatManager().getRemainingSeconds(requester.getUniqueId());
            requester.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cDu bist im Kampf! Warte noch &e" + seconds + "s &cbevor du /tpa nutzen kannst."));
            return true;
        }

        if (args.length == 0) {
            requester.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /tpa <Spieler>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            String msg = plugin.getMessage("player-not-found").replace("{player}", args[0]);
            requester.sendMessage(KlassenPlugin.colorizeComponent(msg));
            return true;
        }

        if (target.equals(requester)) {
            requester.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("tpa-self")));
            return true;
        }

        plugin.getTpaManager().sendRequest(requester.getUniqueId(), target.getUniqueId());

        String msgToTarget = plugin.getMessage("tpa-request-received")
                .replace("{player}", requester.getName());
        target.sendMessage(KlassenPlugin.colorizeComponent(msgToTarget));

        String msgToRequester = plugin.getMessage("tpa-request-sent")
                .replace("{player}", target.getName());
        requester.sendMessage(KlassenPlugin.colorizeComponent(msgToRequester));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String input = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != sender && p.getName().toLowerCase().startsWith(input)) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
