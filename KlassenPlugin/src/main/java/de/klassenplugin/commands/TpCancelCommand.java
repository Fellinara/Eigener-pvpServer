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
 * /tpcancel — Cancels the /tpa request the player has sent.
 */
public class TpCancelCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public TpCancelCommand(KlassenPlugin plugin) {
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

        UUID targetUUID = plugin.getTpaManager().getTarget(requester.getUniqueId());
        if (targetUUID == null) {
            requester.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("tpa-no-outgoing")));
            return true;
        }

        plugin.getTpaManager().clearRequest(targetUUID);

        requester.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("tpa-cancelled-sender")));

        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null && target.isOnline()) {
            String msg = plugin.getMessage("tpa-cancelled-receiver")
                    .replace("{player}", requester.getName());
            target.sendMessage(KlassenPlugin.colorizeComponent(msg));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
