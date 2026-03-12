package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /msg &lt;player&gt; &lt;message&gt; — Sends a private message to another player.
 */
public class MsgCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public MsgCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!plugin.isPluginEnabled() && sender instanceof Player) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cDas Plugin ist momentan deaktiviert!"));
            return true;
        }

        if (!sender.hasPermission("klassenplugin.msg")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /msg <Spieler> <Nachricht>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            String msg = plugin.getMessage("player-not-found").replace("{player}", args[0]);
            sender.sendMessage(KlassenPlugin.colorizeComponent(msg));
            return true;
        }

        if (target.equals(sender)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("msg-self")));
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        String toSender = plugin.getMessage("msg-sent")
                .replace("{target}", target.getName())
                .replace("{message}", message);
        sender.sendMessage(KlassenPlugin.colorizeComponent(toSender));

        String toTarget = plugin.getMessage("msg-received")
                .replace("{sender}", sender.getName())
                .replace("{message}", message);
        target.sendMessage(KlassenPlugin.colorizeComponent(toTarget));

        // Track conversation for /r
        if (sender instanceof Player senderPlayer) {
            plugin.getMsgManager().setLastConversation(senderPlayer.getUniqueId(), target.getUniqueId());
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
                if (p != sender && p.getName().toLowerCase().startsWith(input)) names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }
}
