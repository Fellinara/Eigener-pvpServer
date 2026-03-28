package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * /tpaccept — Accepts a pending /tpa request.
 * The requester is teleported to the target (the player who accepts).
 */
public class TpAcceptCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public TpAcceptCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player target)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only")));
            return true;
        }

        if (!plugin.isPluginEnabled()) {
            target.sendMessage(KlassenPlugin.colorizeComponent("&cDas Plugin ist momentan deaktiviert!"));
            return true;
        }

        UUID requesterUUID = plugin.getTpaManager().getRequester(target.getUniqueId());
        if (requesterUUID == null) {
            target.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("tpa-no-request")));
            return true;
        }

        if (plugin.getCombatManager().isTagged(target.getUniqueId())) {
            int seconds = plugin.getCombatManager().getRemainingSeconds(target.getUniqueId());
            target.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cDu bist im Kampf! Warte noch &e" + seconds + "s &cbevor du /tpaccept nutzen kannst."));
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterUUID);
        plugin.getTpaManager().clearRequest(target.getUniqueId());

        if (requester == null || !requester.isOnline()) {
            target.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("tpa-requester-offline")));
            return true;
        }

        // Save last location before teleporting
        plugin.getBackManager().setLastLocation(requester.getUniqueId(), requester.getLocation());

        requester.teleport(target.getLocation());

        String msgToTarget = plugin.getMessage("tpa-accepted")
                .replace("{player}", requester.getName());
        target.sendMessage(KlassenPlugin.colorizeComponent(msgToTarget));

        String msgToRequester = plugin.getMessage("tpa-teleported")
                .replace("{player}", target.getName());
        requester.sendMessage(KlassenPlugin.colorizeComponent(msgToRequester));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
