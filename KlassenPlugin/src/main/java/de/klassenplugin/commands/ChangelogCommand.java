package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.WeeklyChangelogManager;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChangelogCommand implements TabExecutor {
    private final KlassenPlugin plugin;
    public ChangelogCommand(KlassenPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        WeeklyChangelogManager clm = plugin.getWeeklyChangelogManager();
        if (args.length > 0 && args[0].equalsIgnoreCase("forceroll")) {
            if (!sender.hasPermission("klassenplugin.changelog.admin")) { sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission"))); return true; }
            clm.forceRollNewWeek();
            sender.sendMessage(KlassenPlugin.colorizeComponent("&aNeue Woche gestartet!"));
            return true;
        }
        long dur = plugin.getConfig().getLong("changelog.week-duration-hours", 168L) * 3600_000L;
        long next = clm.getCurrentWeekStart() + dur;
        String nextStr = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(next));
        sender.sendMessage(KlassenPlugin.colorizeComponent("&8[&6Changelog&8] &eWoche &b" + clm.getWeekNumber() + " &7| Nächster Reset: &e" + nextStr));
        sender.sendMessage(KlassenPlugin.colorizeComponent("&eAktuelle Verkaufsboosts:"));
        if (clm.getCurrentBoosts().isEmpty()) sender.sendMessage(KlassenPlugin.colorizeComponent("  &7Keine Boosts."));
        else for (Map.Entry<String, Double> e : clm.getCurrentBoosts().entrySet()) {
            double sell = plugin.getShopManager().getSellPrice(e.getKey());
            String sellStr = sell >= 0 ? " &7(aktuell: &a" + String.format("%.2f", sell) + "&7)" : "";
            sender.sendMessage(KlassenPlugin.colorizeComponent("  &b" + e.getKey() + " &7: &a+" + String.format("%.0f%%", (e.getValue()-1)*100) + " Bonus" + sellStr));
        }
        if (!clm.getLastWeekBoosts().isEmpty()) sender.sendMessage(KlassenPlugin.colorizeComponent("&7Letzte Woche: &c" + String.join(", ", clm.getLastWeekBoosts().keySet())));
        double infl = plugin.getEconomyManager().getInflationMultiplier();
        String ic = infl > 1.1 ? "&c" : infl < 0.9 ? "&a" : "&e";
        sender.sendMessage(KlassenPlugin.colorizeComponent("&7Wirtschaft: Inflation " + ic + String.format("%.0f%%", infl*100) + " &7| Gesamtgeld: &6" + String.format("%.0f", plugin.getEconomyManager().getTotalSupply())));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("klassenplugin.changelog.admin")) return List.of("forceroll");
        return List.of();
    }
}
