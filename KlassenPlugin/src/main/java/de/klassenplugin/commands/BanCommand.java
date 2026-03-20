package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.AntiCheatManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * /ban <player> [reason] – bans a player with a styled kick screen.
 */
public class BanCommand implements TabExecutor {

    private final KlassenPlugin plugin;

    public BanCommand(KlassenPlugin plugin) {
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
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cBenutzung: /ban <Spieler> [Grund]"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        String reason = args.length >= 2
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : "Kein Grund angegeben";

        if (target == null) {
            // Try to ban offline player by name.
            @SuppressWarnings("deprecation")
            var entry = Bukkit.getBanList(org.bukkit.BanList.Type.NAME)
                    .addBan(args[0], reason, (java.util.Date) null, sender.getName());
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&aSpieler &e" + args[0] + " &awurde (offline) gebannt!"));
            plugin.getLogger().info("[Ban] " + sender.getName() + " hat " + args[0] + " gebannt: " + reason);
            return true;
        }

        // Ban and kick with styled screen.
        @SuppressWarnings("deprecation")
        var entry = target.ban(reason, (java.util.Date) null, sender.getName());
        target.kick(buildBanScreen(reason, sender.getName()));

        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&aSpieler &e" + target.getName() + " &awurde gebannt!"));
        Bukkit.broadcast(KlassenPlugin.colorizeComponent(
                "&c&l[BAN] &e" + target.getName() + " &cwurde von &e" + sender.getName()
                        + " &cgebannt! &7Grund: &f" + reason));
        plugin.getLogger().info("[Ban] " + sender.getName() + " hat " + target.getName() + " gebannt: " + reason);
        return true;
    }

    /**
     * Builds a styled ban kick-screen component.
     */
    private net.kyori.adventure.text.Component buildBanScreen(String reason, String bannedBy) {
        String msg = "\n"
                + "&8" + "▀".repeat(40) + "\n"
                + "\n"
                + "  &4&l⛔  DU WURDEST GEBANNT!  ⛔\n"
                + "\n"
                + "  &c&lGrund:\n"
                + "  &f" + reason + "\n"
                + "\n"
                + "  &7Gebannt von: &c" + bannedBy + "\n"
                + "\n"
                + "  &7Um Einspruch einzulegen, wende dich\n"
                + "  &7an einen Server-Admin.\n"
                + "\n"
                + "&8" + "▄".repeat(40);
        return KlassenPlugin.colorizeComponent(msg);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.ban")) return List.of();
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }
}
