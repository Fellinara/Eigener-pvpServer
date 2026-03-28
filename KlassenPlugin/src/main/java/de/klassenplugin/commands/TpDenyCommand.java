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
 * /tpdeny — Denies a pending /tpa request.
 */
public class TpDenyCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public TpDenyCommand(KlassenPlugin plugin) {
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

        plugin.getTpaManager().clearRequest(target.getUniqueId());

        Player requester = Bukkit.getPlayer(requesterUUID);

        String msgToTarget = plugin.getMessage("tpa-denied-sender")
                .replace("{player}", requester != null ? requester.getName() : "?");
        target.sendMessage(KlassenPlugin.colorizeComponent(msgToTarget));

        if (requester != null && requester.isOnline()) {
            String msgToRequester = plugin.getMessage("tpa-denied-receiver")
                    .replace("{player}", target.getName());
            requester.sendMessage(KlassenPlugin.colorizeComponent(msgToRequester));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
