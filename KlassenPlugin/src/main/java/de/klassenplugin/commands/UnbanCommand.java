package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * /unban <player|IP> – removes a ban by player-name or IP address.
 */
public class UnbanCommand implements CommandExecutor {

    private final KlassenPlugin plugin;

    public UnbanCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.ban")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /unban <Spieler|IP>"));
            return true;
        }

        String target = args[0];
        boolean isIp = target.matches("\\d{1,3}(\\.\\d{1,3}){3}");

        if (isIp) {
            @SuppressWarnings("deprecation")
            var banList = Bukkit.getBanList(BanList.Type.IP);
            if (banList.isBanned(target)) {
                banList.pardon(target);
                sender.sendMessage(KlassenPlugin.colorizeComponent(
                        "&aIP &e" + target + " &awurde entbannt!"));
                plugin.getLogger().info("[Unban] " + sender.getName() + " hat IP " + target + " entbannt.");
            } else {
                sender.sendMessage(KlassenPlugin.colorizeComponent(
                        "&cIP &e" + target + " &cist nicht gebannt!"));
            }
        } else {
            @SuppressWarnings("deprecation")
            var banList = Bukkit.getBanList(BanList.Type.NAME);
            if (banList.isBanned(target)) {
                banList.pardon(target);
                sender.sendMessage(KlassenPlugin.colorizeComponent(
                        "&aSpieler &e" + target + " &awurde entbannt!"));
                plugin.getLogger().info("[Unban] " + sender.getName() + " hat " + target + " entbannt.");
            } else {
                sender.sendMessage(KlassenPlugin.colorizeComponent(
                        "&cSpieler &e" + target + " &cist nicht gebannt!"));
            }
        }
        return true;
    }
}
