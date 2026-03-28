package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KlassenPluginCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public KlassenPluginCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&8[&bKlassenPlugin&8] &eBenutzung: /klassenplugin <ban|unban|version>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "ban" -> handleBan(sender, args);
            case "unban" -> handleUnban(sender, args);
            case "version" -> handleVersion(sender);
            default -> sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cUnbekannter Unterbefehl. Benutze: /klassenplugin <ban|unban|version>"));
        }
        return true;
    }

    private void handleBan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("klassenplugin.ban")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /klassenplugin ban <Spieler>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cSpieler ist nicht online!"));
            return;
        }
        if (target.getAddress() == null) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cIP-Adresse des Spielers konnte nicht ermittelt werden!"));
            return;
        }
        String ip = target.getAddress().getAddress().getHostAddress();
        String reason = "Gebannt von " + sender.getName();

        @SuppressWarnings("deprecation")
        var banList = Bukkit.getBanList(BanList.Type.IP);
        banList.addBan(ip, reason, (java.util.Date) null, sender.getName());

        target.kick(KlassenPlugin.colorizeComponent("&cDu wurdest gebannt!\n&eGrund: " + reason));

        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&aDie IP &e" + ip + " &avon &e" + target.getName() + " &awurde gebannt!"));
        plugin.getLogger().info("[KlassenPlugin] " + sender.getName() + " hat die IP " + ip
                + " von " + target.getName() + " gebannt.");
    }

    private void handleUnban(CommandSender sender, String[] args) {
        if (!sender.hasPermission("klassenplugin.ban")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /klassenplugin unban <IP>"));
            return;
        }
        String ip = args[1];
        @SuppressWarnings("deprecation")
        var banList = Bukkit.getBanList(BanList.Type.IP);
        banList.pardon(ip);
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&aDie IP &e" + ip + " &awurde entbannt!"));
        plugin.getLogger().info("[KlassenPlugin] " + sender.getName() + " hat die IP " + ip + " entbannt.");
    }

    private void handleVersion(CommandSender sender) {
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bKlassenPlugin&8] &eVersion: &b" + plugin.getDescription().getVersion()));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filterStart(Arrays.asList("ban", "unban", "version"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("ban")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }

    private List<String> filterStart(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) result.add(s);
        }
        return result;
    }
}
