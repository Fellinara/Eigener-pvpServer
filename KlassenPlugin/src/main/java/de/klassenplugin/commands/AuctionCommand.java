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

public class AuctionCommand implements TabExecutor {
    private static final List<String> SUBS = Arrays.asList("sell","list","buy","request","fulfill","cancel","search","meine");
    private final KlassenPlugin plugin;
    public AuctionCommand(KlassenPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.auction")) { sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission"))); return true; }
        if (!(sender instanceof Player p)) { sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only"))); return true; }
        if (args.length == 0) { handleList(p, new String[]{"list"}); return true; }
        AuctionManager ah = plugin.getAuctionManager();
        EconomyManager eco = plugin.getEconomyManager();
        switch (args[0].toLowerCase()) {
            case "sell" -> handleSell(p, ah, eco, args);
            case "list" -> handleList(p, args);
            case "buy" -> handleBuy(p, ah, eco, args);
            case "request" -> handleRequest(p, ah, eco, args);
            case "fulfill" -> handleFulfill(p, ah, eco, args);
            case "cancel" -> handleCancel(p, ah, eco, args);
            case "search" -> handleSearch(p, ah, args);
            case "meine" -> handleMeine(p, ah);
            default -> p.sendMessage(KlassenPlugin.colorizeComponent("&c/ah <sell|list|buy|request|fulfill|cancel|search|meine>"));
        }
        return true;
    }

    private void handleSell(Player p, AuctionManager ah, EconomyManager eco, String[] args) {
        if (args.length < 2) { p.sendMessage(KlassenPlugin.colorizeComponent("&c/ah sell <Preis> (Item in der Hand)")); return; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) { p.sendMessage(KlassenPlugin.colorizeComponent("&cNichts in der Hand!")); return; }
        double price; try { price = Double.parseDouble(args[1]); } catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültiger Preis!")); return; }
        if (price <= 0) { p.sendMessage(KlassenPlugin.colorizeComponent("&cPreis > 0!")); return; }
        int max = plugin.getConfig().getInt("auction.max-listings-per-player", 10);
        if (ah.getListingsBySeller(p.getUniqueId()).size() >= max) { p.sendMessage(KlassenPlugin.colorizeComponent("&cMaximum " + max + " Angebote!")); return; }
        ItemStack toSell = hand.clone();
        hand.setAmount(0);
        AuctionManager.Listing l = ah.createSellListing(p.getUniqueId(), p.getName(), toSell, price);
        p.sendMessage(KlassenPlugin.colorizeComponent("&a[AH] &e" + toSell.getAmount() + "x " + toSell.getType().name() + " &afür &6" + eco.format(price) + " &a(ID: #" + l.id + ")"));
        for (Player op : Bukkit.getOnlinePlayers()) op.sendMessage(KlassenPlugin.colorizeComponent("&8[&6AH&8] &e" + p.getName() + " &abietet &e" + toSell.getAmount() + "x " + toSell.getType().name() + " &afür &6" + eco.format(price) + " &a(#" + l.id + ")"));
    }

    private void handleList(Player p, String[] args) {
        AuctionManager ah = plugin.getAuctionManager(); EconomyManager eco = plugin.getEconomyManager();
        int pageSize = 8, page = 1;
        if (args.length > 1) try { page = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        List<AuctionManager.Listing> all = new ArrayList<>(ah.getSellListings()); all.addAll(ah.getRequestListings());
        int total = Math.max(1, (int) Math.ceil((double) all.size() / pageSize));
        page = Math.max(1, Math.min(page, total));
        int start = (page - 1) * pageSize, end = Math.min(start + pageSize, all.size());
        p.sendMessage(KlassenPlugin.colorizeComponent("&8[&6Auktionshaus&8] &eAngebote (S." + page + "/" + total + ")"));
        if (all.isEmpty()) { p.sendMessage(KlassenPlugin.colorizeComponent("  &7Keine Angebote.")); return; }
        for (int i = start; i < end; i++) {
            AuctionManager.Listing l = all.get(i);
            String t = l.type == AuctionManager.ListingType.SELL ? "&a[V]" : "&e[A]";
            p.sendMessage(KlassenPlugin.colorizeComponent("  " + t + " &b#" + l.id + " &7| " + l.item.getAmount() + "x &f" + l.item.getType().name() + " &7| &6" + eco.format(l.price) + " &7| &e" + l.sellerName));
        }
    }

    private void handleBuy(Player p, AuctionManager ah, EconomyManager eco, String[] args) {
        if (args.length < 2) { p.sendMessage(KlassenPlugin.colorizeComponent("&c/ah buy <ID>")); return; }
        int id; try { id = Integer.parseInt(args[1]); } catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige ID!")); return; }
        AuctionManager.Listing l = ah.getListing(id);
        if (l == null) { p.sendMessage(KlassenPlugin.colorizeComponent("&cID #" + id + " nicht gefunden!")); return; }
        if (l.type != AuctionManager.ListingType.SELL) { p.sendMessage(KlassenPlugin.colorizeComponent("&cDas ist eine Anfrage, nutze /ah fulfill")); return; }
        if (l.seller.equals(p.getUniqueId())) { p.sendMessage(KlassenPlugin.colorizeComponent("&cEigenes Angebot!")); return; }
        if (!eco.has(p.getUniqueId(), l.price)) { p.sendMessage(KlassenPlugin.colorizeComponent("&cNicht genug Geld! Preis: &6" + eco.format(l.price))); return; }
        if (p.getInventory().firstEmpty() == -1) { p.sendMessage(KlassenPlugin.colorizeComponent("&cInventar voll!")); return; }
        eco.withdraw(p.getUniqueId(), l.price); eco.deposit(l.seller, l.price); eco.save();
        p.getInventory().addItem(l.item.clone()); ah.removeListing(id);
        p.sendMessage(KlassenPlugin.colorizeComponent("&aGekauft: &e" + l.item.getAmount() + "x " + l.item.getType().name() + " &afür &6" + eco.format(l.price)));
        Player seller = Bukkit.getPlayer(l.seller);
        if (seller != null) seller.sendMessage(KlassenPlugin.colorizeComponent("&a[AH] &e" + p.getName() + " &ahat dein Angebot #" + id + " für &6" + eco.format(l.price) + " &agekauft!"));
    }

    private void handleRequest(Player p, AuctionManager ah, EconomyManager eco, String[] args) {
        if (args.length < 4) { p.sendMessage(KlassenPlugin.colorizeComponent("&c/ah request <item> <menge> <preis>")); return; }
        Material mat = Material.matchMaterial(args[1].toUpperCase());
        if (mat == null) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUnbekanntes Material!")); return; }
        int amount; try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige Menge!")); return; }
        if (amount <= 0 || amount > 64) { p.sendMessage(KlassenPlugin.colorizeComponent("&cMenge 1-64!")); return; }
        double price; try { price = Double.parseDouble(args[3]); } catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültiger Preis!")); return; }
        if (price <= 0) { p.sendMessage(KlassenPlugin.colorizeComponent("&cPreis > 0!")); return; }
        if (!eco.has(p.getUniqueId(), price)) { p.sendMessage(KlassenPlugin.colorizeComponent("&cNicht genug Geld! Preis: &6" + eco.format(price))); return; }
        eco.withdraw(p.getUniqueId(), price); eco.save();
        AuctionManager.Listing l = ah.createRequestListing(p.getUniqueId(), p.getName(), new ItemStack(mat, amount), price);
        p.sendMessage(KlassenPlugin.colorizeComponent("&a[AH] Anfrage für &e" + amount + "x " + mat.name() + " &abei &6" + eco.format(price) + " &a(ID: #" + l.id + ")"));
        for (Player op : Bukkit.getOnlinePlayers()) op.sendMessage(KlassenPlugin.colorizeComponent("&8[&6AH&8] &e" + p.getName() + " &asucht &e" + amount + "x " + mat.name() + " → &6" + eco.format(price) + " &a(#" + l.id + ")"));
    }

    private void handleFulfill(Player p, AuctionManager ah, EconomyManager eco, String[] args) {
        if (args.length < 2) { p.sendMessage(KlassenPlugin.colorizeComponent("&c/ah fulfill <ID>")); return; }
        int id; try { id = Integer.parseInt(args[1]); } catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige ID!")); return; }
        AuctionManager.Listing l = ah.getListing(id);
        if (l == null) { p.sendMessage(KlassenPlugin.colorizeComponent("&cID #" + id + " nicht gefunden!")); return; }
        if (l.type != AuctionManager.ListingType.REQUEST) { p.sendMessage(KlassenPlugin.colorizeComponent("&cKeine Anfrage, nutze /ah buy")); return; }
        if (l.seller.equals(p.getUniqueId())) { p.sendMessage(KlassenPlugin.colorizeComponent("&cEigene Anfrage!")); return; }
        Material mat = l.item.getType(); int needed = l.item.getAmount();
        int has = 0; for (ItemStack is : p.getInventory().getContents()) if (is != null && is.getType() == mat) has += is.getAmount();
        if (has < needed) { p.sendMessage(KlassenPlugin.colorizeComponent("&cNicht genug &e" + mat.name() + " &c(hast: " + has + ", braucht: " + needed + ")")); return; }
        int rem = needed;
        for (ItemStack is : p.getInventory().getContents()) {
            if (is != null && is.getType() == mat && rem > 0) {
                if (is.getAmount() <= rem) { rem -= is.getAmount(); is.setAmount(0); }
                else { is.setAmount(is.getAmount() - rem); rem = 0; }
            }
        }
        eco.deposit(p.getUniqueId(), l.price); eco.save();
        Player req = Bukkit.getPlayer(l.seller);
        if (req != null) { req.getInventory().addItem(l.item.clone()); req.sendMessage(KlassenPlugin.colorizeComponent("&a[AH] Anfrage #" + id + " erfüllt von &e" + p.getName())); }
        ah.removeListing(id);
        p.sendMessage(KlassenPlugin.colorizeComponent("&aAnfrage #" + id + " erfüllt → &6" + eco.format(l.price) + " &aerhalten!"));
    }

    private void handleCancel(Player p, AuctionManager ah, EconomyManager eco, String[] args) {
        if (args.length < 2) { p.sendMessage(KlassenPlugin.colorizeComponent("&c/ah cancel <ID>")); return; }
        int id; try { id = Integer.parseInt(args[1]); } catch (NumberFormatException e) { p.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige ID!")); return; }
        AuctionManager.Listing l = ah.getListing(id);
        if (l == null) { p.sendMessage(KlassenPlugin.colorizeComponent("&cID #" + id + " nicht gefunden!")); return; }
        if (!l.seller.equals(p.getUniqueId()) && !p.hasPermission("klassenplugin.auction.admin")) { p.sendMessage(KlassenPlugin.colorizeComponent("&cNicht dein Angebot!")); return; }
        if (l.type == AuctionManager.ListingType.SELL) {
            if (p.getInventory().firstEmpty() != -1) p.getInventory().addItem(l.item.clone()); else p.getWorld().dropItemNaturally(p.getLocation(), l.item.clone());
            p.sendMessage(KlassenPlugin.colorizeComponent("&aAngebot #" + id + " abgebrochen. Artikel zurück!"));
        } else {
            eco.deposit(l.seller, l.price); eco.save();
            p.sendMessage(KlassenPlugin.colorizeComponent("&aAnfrage #" + id + " abgebrochen. &6" + eco.format(l.price) + " &azurückerstattet!"));
        }
        ah.removeListing(id);
    }

    private void handleSearch(Player p, AuctionManager ah, String[] args) {
        if (args.length < 2) { p.sendMessage(KlassenPlugin.colorizeComponent("&c/ah search <Suchbegriff>")); return; }
        String q = args[1].toUpperCase(); EconomyManager eco = plugin.getEconomyManager();
        List<AuctionManager.Listing> res = new ArrayList<>();
        for (AuctionManager.Listing l : ah.getAllListings()) if (l.item.getType().name().contains(q)) res.add(l);
        if (res.isEmpty()) { p.sendMessage(KlassenPlugin.colorizeComponent("&cKeine Treffer für &e" + args[1])); return; }
        p.sendMessage(KlassenPlugin.colorizeComponent("&8[&6AH-Suche&8] &e" + args[1]));
        for (AuctionManager.Listing l : res) {
            String t = l.type == AuctionManager.ListingType.SELL ? "&a[V]" : "&e[A]";
            p.sendMessage(KlassenPlugin.colorizeComponent("  " + t + " &b#" + l.id + " &7| " + l.item.getAmount() + "x &f" + l.item.getType().name() + " &7| &6" + eco.format(l.price) + " &7| &e" + l.sellerName));
        }
    }

    private void handleMeine(Player p, AuctionManager ah) {
        EconomyManager eco = plugin.getEconomyManager();
        List<AuctionManager.Listing> mine = ah.getListingsBySeller(p.getUniqueId());
        if (mine.isEmpty()) { p.sendMessage(KlassenPlugin.colorizeComponent("&7Keine aktiven Angebote.")); return; }
        p.sendMessage(KlassenPlugin.colorizeComponent("&8[&6AH&8] &eDeine Angebote:"));
        for (AuctionManager.Listing l : mine) {
            String t = l.type == AuctionManager.ListingType.SELL ? "&a[V]" : "&e[A]";
            p.sendMessage(KlassenPlugin.colorizeComponent("  " + t + " &b#" + l.id + " &7| " + l.item.getAmount() + "x &f" + l.item.getType().name() + " &7| &6" + eco.format(l.price)));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.auction")) return List.of();
        if (args.length == 1) return filter(SUBS, args[0]);
        return List.of();
    }

    private List<String> filter(List<String> l, String pref) { List<String> r = new ArrayList<>(); for (String s : l) if (s.toLowerCase().startsWith(pref.toLowerCase())) r.add(s); return r; }
}
