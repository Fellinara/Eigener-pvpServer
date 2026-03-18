package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.AuctionManager;
import de.klassenplugin.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * /order — Auftragssystem (Anfragen stellen, erfüllen, auflisten, abbrechen).
 *
 * Subkommandos:
 *   /order list [seite]           – alle offenen Aufträge auflisten
 *   /order <item> <menge> <preis> – einen neuen Auftrag erstellen
 *   /order fulfill <id>           – einen Auftrag erfüllen
 *   /order cancel <id>            – eigenen Auftrag abbrechen (Geld zurück)
 */
public class OrderCommand implements TabExecutor {

    private static final List<String> SUBS = Arrays.asList("list", "fulfill", "cancel");
    private final KlassenPlugin plugin;

    public OrderCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.order")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only")));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            handleList(p, args);
            return true;
        }
        AuctionManager ah = plugin.getAuctionManager();
        EconomyManager eco = plugin.getEconomyManager();
        switch (args[0].toLowerCase()) {
            case "fulfill" -> handleFulfill(p, ah, eco, args);
            case "cancel"  -> handleCancel(p, ah, eco, args);
            default        -> handleCreate(p, ah, eco, args);
        }
        return true;
    }

    // /order list [page]
    private void handleList(Player p, String[] args) {
        AuctionManager ah = plugin.getAuctionManager();
        EconomyManager eco = plugin.getEconomyManager();
        int pageSize = 8, page = 1;
        if (args.length > 1) {
            try { page = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }
        List<AuctionManager.Listing> requests = ah.getRequestListings();
        int total = Math.max(1, (int) Math.ceil((double) requests.size() / pageSize));
        page = Math.max(1, Math.min(page, total));
        int start = (page - 1) * pageSize, end = Math.min(start + pageSize, requests.size());
        p.sendMessage(KlassenPlugin.colorizeComponent("&8[&6Aufträge&8] &eOffene Anfragen (S." + page + "/" + total + ")"));
        if (requests.isEmpty()) {
            p.sendMessage(KlassenPlugin.colorizeComponent("  &7Keine offenen Aufträge."));
            return;
        }
        for (int i = start; i < end; i++) {
            AuctionManager.Listing l = requests.get(i);
            p.sendMessage(KlassenPlugin.colorizeComponent(
                    "  &e#" + l.id + " &7| " + l.item.getAmount() + "x &f" + l.item.getType().name()
                    + " &7| &6" + eco.format(l.price) + " &7| &evon &b" + l.sellerName));
        }
        p.sendMessage(KlassenPlugin.colorizeComponent("&7Erfülle mit &e/order fulfill <id>"));
    }

    // /order <item> <menge> <preis>
    private void handleCreate(Player p, AuctionManager ah, EconomyManager eco, String[] args) {
        if (args.length < 3) {
            sendHelp(p);
            return;
        }
        Material mat = Material.matchMaterial(args[0].toUpperCase());
        if (mat == null) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cUnbekanntes Material: &e" + args[0]));
            return;
        }
        int amount;
        try { amount = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige Menge!")); return; }
        if (amount <= 0 || amount > 64) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cMenge muss zwischen 1 und 64 liegen!"));
            return;
        }
        double price;
        try { price = Double.parseDouble(args[2]); }
        catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültiger Preis!")); return; }
        if (price <= 0) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cPreis muss > 0 sein!"));
            return;
        }
        if (!eco.has(p.getUniqueId(), price)) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cNicht genug Geld! Benötigt: &6" + eco.format(price)));
            return;
        }
        int max = plugin.getConfig().getInt("auction.max-listings-per-player", 10);
        if (ah.getListingsBySeller(p.getUniqueId()).size() >= max) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cMaximum " + max + " aktive Aufträge!"));
            return;
        }
        eco.withdraw(p.getUniqueId(), price);
        eco.save();
        AuctionManager.Listing l = ah.createRequestListing(
                p.getUniqueId(), p.getName(), new ItemStack(mat, amount), price);
        p.sendMessage(KlassenPlugin.colorizeComponent(
                "&a[Auftrag] &eAnfrage erstellt: &f" + amount + "x " + mat.name()
                + " &afür &6" + eco.format(price) + " &a(ID: #" + l.id + ")"));
        for (Player op : Bukkit.getOnlinePlayers()) {
            op.sendMessage(KlassenPlugin.colorizeComponent(
                    "&8[&6Aufträge&8] &e" + p.getName() + " &asucht &e" + amount + "x " + mat.name()
                    + " → &6" + eco.format(price) + " &a(#" + l.id + ")"));
        }
    }

    // /order fulfill <id>
    private void handleFulfill(Player p, AuctionManager ah, EconomyManager eco, String[] args) {
        if (args.length < 2) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /order fulfill <ID>"));
            return;
        }
        int id;
        try { id = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige ID!")); return; }
        AuctionManager.Listing l = ah.getListing(id);
        if (l == null) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cAuftrag #" + id + " nicht gefunden!"));
            return;
        }
        if (l.type != AuctionManager.ListingType.REQUEST) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&c#" + id + " ist kein Auftrag. Nutze /ah buy."));
            return;
        }
        if (l.seller.equals(p.getUniqueId())) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cDu kannst deinen eigenen Auftrag nicht erfüllen!"));
            return;
        }
        Material mat = l.item.getType();
        int needed = l.item.getAmount();
        int has = 0;
        for (ItemStack is : p.getInventory().getContents()) if (is != null && is.getType() == mat) has += is.getAmount();
        if (has < needed) {
            p.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cNicht genug &e" + mat.name() + " &c(hast: " + has + ", braucht: " + needed + ")"));
            return;
        }
        int rem = needed;
        for (ItemStack is : p.getInventory().getContents()) {
            if (is != null && is.getType() == mat && rem > 0) {
                if (is.getAmount() <= rem) { rem -= is.getAmount(); is.setAmount(0); }
                else { is.setAmount(is.getAmount() - rem); rem = 0; }
            }
        }
        eco.deposit(p.getUniqueId(), l.price);
        eco.save();
        Player requester = Bukkit.getPlayer(l.seller);
        if (requester != null) {
            requester.getInventory().addItem(l.item.clone());
            requester.sendMessage(KlassenPlugin.colorizeComponent(
                    "&a[Auftrag] Dein Auftrag #" + id + " wurde von &e" + p.getName() + " &aerfüllt!"));
        }
        ah.removeListing(id);
        p.sendMessage(KlassenPlugin.colorizeComponent(
                "&aAuftrag #" + id + " erfüllt → &6" + eco.format(l.price) + " &aerhalten!"));
    }

    // /order cancel <id>
    private void handleCancel(Player p, AuctionManager ah, EconomyManager eco, String[] args) {
        if (args.length < 2) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /order cancel <ID>"));
            return;
        }
        int id;
        try { id = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige ID!")); return; }
        AuctionManager.Listing l = ah.getListing(id);
        if (l == null) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cAuftrag #" + id + " nicht gefunden!"));
            return;
        }
        if (!l.seller.equals(p.getUniqueId()) && !p.hasPermission("klassenplugin.order.admin")) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&cDas ist nicht dein Auftrag!"));
            return;
        }
        if (l.type != AuctionManager.ListingType.REQUEST) {
            p.sendMessage(KlassenPlugin.colorizeComponent("&c#" + id + " ist kein Auftrag. Nutze /ah cancel."));
            return;
        }
        eco.deposit(l.seller, l.price);
        eco.save();
        ah.removeListing(id);
        p.sendMessage(KlassenPlugin.colorizeComponent(
                "&aAuftrag #" + id + " abgebrochen. &6" + eco.format(l.price) + " &azurückerstattet!"));
    }

    private void sendHelp(Player p) {
        p.sendMessage(KlassenPlugin.colorizeComponent("&8[&6Aufträge&8] &eBefehle:"));
        p.sendMessage(KlassenPlugin.colorizeComponent("  &e/order list [seite]           &7– Aufträge anzeigen"));
        p.sendMessage(KlassenPlugin.colorizeComponent("  &e/order <item> <menge> <preis> &7– Auftrag erstellen"));
        p.sendMessage(KlassenPlugin.colorizeComponent("  &e/order fulfill <id>           &7– Auftrag erfüllen"));
        p.sendMessage(KlassenPlugin.colorizeComponent("  &e/order cancel <id>            &7– Auftrag abbrechen"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.order")) return List.of();
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(SUBS);
            // suggest materials
            for (Material m : Material.values()) {
                if (m.isItem() && m.name().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(m.name().toLowerCase());
                }
            }
            return filter(completions, args[0]);
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String pref) {
        List<String> r = new ArrayList<>();
        for (String s : list) if (s.toLowerCase().startsWith(pref.toLowerCase())) r.add(s);
        return r;
    }
}
