package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.EconomyManager;
import de.klassenplugin.managers.ShopManager;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class ShopCommand implements TabExecutor {
    private static final List<String> SUBS = Arrays.asList("list", "buy", "sell", "info", "admin");
    private final KlassenPlugin plugin;
    public ShopCommand(KlassenPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.shop")) { sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission"))); return true; }
        if (!(sender instanceof Player p)) { sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only"))); return true; }
        if (args.length == 0) { sendHelp(p); return true; }
        ShopManager shop = plugin.getShopManager();
        EconomyManager eco = plugin.getEconomyManager();
        switch (args[0].toLowerCase()) {
            case "list" -> handleList(p, shop, args);
            case "buy" -> handleBuy(p, shop, eco, args);
            case "sell" -> handleSell(p, shop, eco, args);
            case "info" -> handleInfo(p, shop, args);
            case "admin" -> handleAdmin(p, shop, args);
            default -> sendHelp(p);
        }
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(KlassenPlugin.colorizeComponent("&8[&6Shop&8] &e/shop list | buy <item> [n] | sell <item|hand> [n] | info <item>"));
    }

    private void handleList(Player p, ShopManager shop, String[] args) {
        List<String> itemList = new ArrayList<>(shop.getItemNames());
        double infl = plugin.getEconomyManager().getInflationMultiplier();
        String inflStr = (infl > 1.1 ? "&c" : infl < 0.9 ? "&a" : "&e") + String.format("%.0f%%", infl * 100);
        p.sendMessage(KlassenPlugin.colorizeComponent("&8[&6Shop&8] &eAlle Artikel &7(Inflation: " + inflStr + ")"));
        p.sendMessage(KlassenPlugin.colorizeComponent("  &8Item                          &7Kauf       Verkauf"));
        for (String name : itemList) {
            double buy = shop.getBuyPrice(name), sell = shop.getSellPrice(name);
            String buyStr = buy >= 0 ? "&6" + String.format("%.1f", buy) : "&cnicht kaufbar";
            String sellStr = sell >= 0 ? "&a" + String.format("%.1f", sell) : "&7n/a";
            p.sendMessage(KlassenPlugin.colorizeComponent("  &b" + name + " &7| Kauf: " + buyStr + " &7| Verk: " + sellStr));
        }
    }

    private void handleBuy(Player p, ShopManager shop, EconomyManager eco, String[] args) {
        if (args.length < 2) { p.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /shop buy <item> [menge]")); return; }
        String matName = args[1].toUpperCase();
        int amount = 1;
        if (args.length > 2) try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige Menge!")); return; }
        if (amount <= 0 || amount > 64) { p.sendMessage(KlassenPlugin.colorizeComponent("&cMenge: 1-64")); return; }
        if (!shop.hasItem(matName)) { p.sendMessage(KlassenPlugin.colorizeComponent("&cArtikel &e" + matName + " &cnicht im Shop!")); return; }
        double price = shop.getBuyPrice(matName);
        if (price < 0) { p.sendMessage(KlassenPlugin.colorizeComponent("&cKann nicht gekauft werden!")); return; }
        double total = price * amount;
        if (!eco.has(p.getUniqueId(), total)) { p.sendMessage(KlassenPlugin.colorizeComponent("&cNicht genug Geld! Preis: &6" + eco.format(total))); return; }
        Material mat = Material.matchMaterial(matName);
        if (mat == null) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUnbekanntes Material!")); return; }
        if (p.getInventory().firstEmpty() == -1) { p.sendMessage(KlassenPlugin.colorizeComponent("&cInventar voll!")); return; }
        eco.withdraw(p.getUniqueId(), total);
        eco.save();
        p.getInventory().addItem(new ItemStack(mat, amount));
        p.sendMessage(KlassenPlugin.colorizeComponent("&aGekauft: &e" + amount + "x " + matName + " &afür &6" + eco.format(total)));
    }

    private void handleSell(Player p, ShopManager shop, EconomyManager eco, String[] args) {
        if (args.length < 2) { p.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /shop sell <item|hand> [menge]")); return; }
        String matName;
        int amount;
        if (args[1].equalsIgnoreCase("hand")) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) { p.sendMessage(KlassenPlugin.colorizeComponent("&cNichts in der Hand!")); return; }
            matName = hand.getType().name();
            if (args.length > 2) {
                try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige Menge!")); return; }
            } else {
                amount = hand.getAmount();
            }
        } else {
            matName = args[1].toUpperCase();
            amount = 1;
            if (args.length > 2) try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige Menge!")); return; }
        }
        if (amount <= 0) { p.sendMessage(KlassenPlugin.colorizeComponent("&cMenge > 0!")); return; }
        if (!shop.hasItem(matName)) { p.sendMessage(KlassenPlugin.colorizeComponent("&cArtikel &e" + matName + " &cnicht im Shop!")); return; }
        double price = shop.getSellPrice(matName);
        if (price < 0) { p.sendMessage(KlassenPlugin.colorizeComponent("&cKann nicht verkauft werden!")); return; }
        Material mat = Material.matchMaterial(matName);
        if (mat == null) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUnbekanntes Material!")); return; }
        int has = 0;
        for (ItemStack is : p.getInventory().getContents()) if (is != null && is.getType() == mat) has += is.getAmount();
        if (has < amount) { p.sendMessage(KlassenPlugin.colorizeComponent("&cNicht genug &e" + matName + " &c(hast: " + has + ")")); return; }
        int rem = amount;
        for (ItemStack is : p.getInventory().getContents()) {
            if (is != null && is.getType() == mat && rem > 0) {
                if (is.getAmount() <= rem) { rem -= is.getAmount(); is.setAmount(0); }
                else { is.setAmount(is.getAmount() - rem); rem = 0; }
            }
        }
        double earned = price * amount;
        eco.deposit(p.getUniqueId(), earned);
        eco.save();
        p.sendMessage(KlassenPlugin.colorizeComponent("&aVerkauft: &e" + amount + "x " + matName + " &afür &6" + eco.format(earned)));
    }

    private void handleInfo(Player p, ShopManager shop, String[] args) {
        if (args.length < 2) { p.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /shop info <item>")); return; }
        String name = args[1].toUpperCase();
        if (!shop.hasItem(name)) { p.sendMessage(KlassenPlugin.colorizeComponent("&cNicht gefunden: &e" + name)); return; }
        double buy = shop.getBuyPrice(name), sell = shop.getSellPrice(name);
        double infl = plugin.getEconomyManager().getInflationMultiplier();
        p.sendMessage(KlassenPlugin.colorizeComponent("&8[&6Shop&8] &b" + name));
        p.sendMessage(KlassenPlugin.colorizeComponent("  &7Kauf: &6" + (buy >= 0 ? String.format("%.2f", buy) : "nicht kaufbar") + " &7| Verkauf: &a" + (sell >= 0 ? String.format("%.2f", sell) : "n/a")));
        p.sendMessage(KlassenPlugin.colorizeComponent("  &7Inflation: &e" + String.format("%.0f%%", infl * 100)));
    }

    private void handleAdmin(Player p, ShopManager shop, String[] args) {
        if (!p.hasPermission("klassenplugin.shop.admin")) { p.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission"))); return; }
        if (args.length < 2) { p.sendMessage(KlassenPlugin.colorizeComponent("&c/shop admin <set|remove> ...")); return; }
        switch (args[1].toLowerCase()) {
            case "set" -> {
                if (args.length < 5) { p.sendMessage(KlassenPlugin.colorizeComponent("&c/shop admin set <item> <kauf> <verkauf>")); return; }
                try { shop.setItem(args[2], Double.parseDouble(args[3]), Double.parseDouble(args[4])); p.sendMessage(KlassenPlugin.colorizeComponent("&aArtikel gesetzt: &e" + args[2].toUpperCase())); }
                catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige Preise!")); }
            }
            case "remove" -> { if (args.length < 3) { p.sendMessage(KlassenPlugin.colorizeComponent("&c/shop admin remove <item>")); return; } shop.removeItem(args[2]); p.sendMessage(KlassenPlugin.colorizeComponent("&aEntfernt: &e" + args[2].toUpperCase())); }
            default -> p.sendMessage(KlassenPlugin.colorizeComponent("&c/shop admin <set|remove>"));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.shop")) return List.of();
        if (args.length == 1) return filter(SUBS, args[0]);
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("buy") || sub.equals("info")) return filter(new ArrayList<>(plugin.getShopManager().getItemNames()), args[1]);
            if (sub.equals("sell")) { List<String> l = new ArrayList<>(plugin.getShopManager().getItemNames()); l.add("hand"); return filter(l, args[1]); }
            if (sub.equals("admin")) return filter(Arrays.asList("set", "remove"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) return filter(new ArrayList<>(plugin.getShopManager().getItemNames()), args[2]);
        return List.of();
    }

    private List<String> filter(List<String> list, String pref) {
        List<String> r = new ArrayList<>();
        for (String s : list) if (s.toLowerCase().startsWith(pref.toLowerCase())) r.add(s);
        return r;
    }
}
