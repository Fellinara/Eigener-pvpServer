package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class PayCommand implements TabExecutor {
    private final KlassenPlugin plugin;
    public PayCommand(KlassenPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only"))); return true; }
        if (!p.hasPermission("klassenplugin.economy.pay")) { p.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission"))); return true; }
        if (args.length < 2) { p.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /pay <Spieler> <Betrag>")); return true; }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { p.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-not-found").replace("{player}", args[0]))); return true; }
        if (target.equals(p)) { p.sendMessage(KlassenPlugin.colorizeComponent("&cDu kannst dir nicht selbst Geld überweisen!")); return true; }
        double amount;
        try { amount = Double.parseDouble(args[1]); } catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültiger Betrag!")); return true; }
        if (amount <= 0) { p.sendMessage(KlassenPlugin.colorizeComponent("&cBetrag muss > 0 sein!")); return true; }
        EconomyManager eco = plugin.getEconomyManager();
        if (!eco.withdraw(p.getUniqueId(), amount)) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cNicht genug Geld! Kontostand: &6" + eco.format(eco.getBalance(p.getUniqueId())))); return true;
        }
        eco.deposit(target.getUniqueId(), amount);
        eco.save();
        p.sendMessage(KlassenPlugin.colorizeComponent("&aDu hast &6" + eco.format(amount) + " &aan &b" + target.getName() + " &aüberwiesen!"));
        target.sendMessage(KlassenPlugin.colorizeComponent("&aDu hast &6" + eco.format(amount) + " &avon &b" + p.getName() + " &aerhalten!"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) names.add(p.getName());
            return names;
        }
        return List.of();
    }
}
