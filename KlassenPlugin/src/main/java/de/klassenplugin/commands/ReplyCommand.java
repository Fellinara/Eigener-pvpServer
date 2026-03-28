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
 * /r &lt;message&gt; — Replies to the last private message.
 */
public class ReplyCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public ReplyCommand(KlassenPlugin plugin) {
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

        if (!player.hasPermission("klassenplugin.msg")) {
            player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /r <Nachricht>"));
            return true;
        }

        if (!plugin.getMsgManager().hasLastConversation(player.getUniqueId())) {
            player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("reply-no-one")));
            return true;
        }

        UUID targetUUID = plugin.getMsgManager().getLastConversation(player.getUniqueId());
        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline()) {
            player.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("reply-offline")));
            return true;
        }

        String message = String.join(" ", args);

        String toSender = plugin.getMessage("msg-sent")
                .replace("{target}", target.getName())
                .replace("{message}", message);
        player.sendMessage(KlassenPlugin.colorizeComponent(toSender));

        String toTarget = plugin.getMessage("msg-received")
                .replace("{sender}", player.getName())
                .replace("{message}", message);
        target.sendMessage(KlassenPlugin.colorizeComponent(toTarget));

        plugin.getMsgManager().setLastConversation(player.getUniqueId(), targetUUID);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
