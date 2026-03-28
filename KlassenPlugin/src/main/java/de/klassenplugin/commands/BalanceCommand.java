package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class BalanceCommand implements TabExecutor {
    private final KlassenPlugin plugin;
    public BalanceCommand(KlassenPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only")));
                return true;
            }
            double bal = plugin.getEconomyManager().getBalance(p.getUniqueId());
            sender.sendMessage(KlassenPlugin.colorizeComponent("&8[&6Economy&8] &eDein Kontostand: &6" + plugin.getEconomyManager().format(bal)));
        } else {
            if (!sender.hasPermission("klassenplugin.economy.admin")) {
                sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
                return true;
            }
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            double bal = plugin.getEconomyManager().getBalance(target.getUniqueId());
            sender.sendMessage(KlassenPlugin.colorizeComponent("&8[&6Economy&8] &eKontostand von &b" + args[0] + "&e: &6" + plugin.getEconomyManager().format(bal)));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("klassenplugin.economy.admin")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) names.add(p.getName());
            return names;
        }
        return List.of();
    }
}
