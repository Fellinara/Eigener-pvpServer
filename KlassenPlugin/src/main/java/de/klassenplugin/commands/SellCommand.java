package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.EconomyManager;
import de.klassenplugin.managers.ShopManager;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /sell [menge] — verkauft das Item in der Hand zum aktuellen Shop-Preis.
 *
 * Zeigt den Changelog-Boost an, wenn ein Wochenboost aktiv ist.
 */
public class SellCommand implements CommandExecutor {

    private final KlassenPlugin plugin;

    public SellCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.shop")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only")));
            return true;
        }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cNichts in der Hand!"));
            return true;
        }

        int amount = hand.getAmount();
        if (args.length > 0) {
            try {
                amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige Menge!"));
                return true;
            }
        }
        if (amount <= 0) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cMenge muss > 0 sein!"));
            return true;
        }

        String matName = hand.getType().name();
        ShopManager shop = plugin.getShopManager();
        EconomyManager eco = plugin.getEconomyManager();

        if (!shop.hasItem(matName)) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&c" + matName + " &cwird vom Shop nicht angekauft!"));
            return true;
        }
        double unitPrice = shop.getSellPrice(matName);
        if (unitPrice < 0) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&c" + matName + " &ckann nicht verkauft werden!"));
            return true;
        }

        // Check player has enough in inventory
        Material mat = hand.getType();
        int has = 0;
        for (ItemStack is : p.getInventory().getContents()) {
            if (is != null && is.getType() == mat) has += is.getAmount();
        }
        if (has < amount) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cNicht genug &e" + matName + " &c(hast: " + has + ")"));
            return true;
        }

        // Remove items from inventory
        int rem = amount;
        for (ItemStack is : p.getInventory().getContents()) {
            if (is != null && is.getType() == mat && rem > 0) {
                if (is.getAmount() <= rem) { rem -= is.getAmount(); is.setAmount(0); }
                else { is.setAmount(is.getAmount() - rem); rem = 0; }
            }
        }

        double earned = unitPrice * amount;
        eco.deposit(p.getUniqueId(), earned);
        eco.save();

        // Show boost info if active
        double boost = plugin.getWeeklyChangelogManager() != null
                ? plugin.getWeeklyChangelogManager().getBoostMultiplier(matName) : 1.0;
        String boostHint = boost > 1.0
                ? " &e(★ +" + String.format("%.0f%%", (boost - 1.0) * 100) + " Changelog-Bonus!)"
                : "";

        p.sendMessage(KlassenPlugin.colorizeComponent(
                "&aVerkauft: &e" + amount + "x " + matName
                + " &afür &6" + eco.format(earned) + boostHint));
        return true;
    }
}
