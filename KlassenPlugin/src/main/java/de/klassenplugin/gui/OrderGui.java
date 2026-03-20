package de.klassenplugin.gui;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.AuctionManager;
import de.klassenplugin.managers.EconomyManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inventory GUI for the Order system (/order).
 *
 * Two views:
 *   "all"  – all open REQUEST listings from everyone  → click to fulfill
 *   "mine" – own REQUEST listings                     → click to cancel
 *
 * Layout (6 rows = 54 slots):
 *   Rows 0-4 (slots 0-44): order entries
 *   Row 5: [← 45 | info 49 | switch 50 | → 53]
 */
public class OrderGui {

    private static final int PAGE_SIZE = 45;

    private final KlassenPlugin plugin;
    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, List<AuctionManager.Listing>> pageListings = new HashMap<>();
    private final Map<UUID, String> modes = new HashMap<>();  // "all" or "mine"

    public OrderGui(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Open the public order list (all open requests, click to fulfill). */
    public void openAll(Player player, int page) {
        modes.put(player.getUniqueId(), "all");
        List<AuctionManager.Listing> all = plugin.getAuctionManager().getRequestListings();
        openPage(player, all, page, "&8[&6Auftragshaus&8] &7Seite ", "all");
    }

    /** Convenience: page 0. */
    public void openAll(Player player) { openAll(player, 0); }

    /** Open the player's own order list (click to cancel). */
    public void openMine(Player player, int page) {
        modes.put(player.getUniqueId(), "mine");
        List<AuctionManager.Listing> mine = new ArrayList<>();
        for (AuctionManager.Listing l : plugin.getAuctionManager().getRequestListings()) {
            if (l.seller.equals(player.getUniqueId())) mine.add(l);
        }
        openPage(player, mine, page, "&8[&6Meine Auftr\u00e4ge&8] &7Seite ", "mine");
    }

    /** Convenience: page 0. */
    public void openMine(Player player) { openMine(player, 0); }

    // -----------------------------------------------------------------------
    // Click handling (called by GuiListener)
    // -----------------------------------------------------------------------

    public boolean handleClick(Player player, int slot) {
        if (slot < 0) return false;

        int currentPage = pages.getOrDefault(player.getUniqueId(), 0);
        String mode = modes.getOrDefault(player.getUniqueId(), "all");

        // Navigation
        if (slot == 45 && currentPage > 0) {
            if ("mine".equals(mode)) openMine(player, currentPage - 1);
            else openAll(player, currentPage - 1);
            return true;
        }
        if (slot == 53) {
            if ("mine".equals(mode)) openMine(player, currentPage + 1);
            else openAll(player, currentPage + 1);
            return true;
        }
        if (slot == 49) return true; // info – consume only
        if (slot == 50) {
            if ("mine".equals(mode)) openAll(player, 0);
            else openMine(player, 0);
            return true;
        }

        // Order entry click
        if (slot >= PAGE_SIZE) return false;
        List<AuctionManager.Listing> listings = pageListings.get(player.getUniqueId());
        if (listings == null || slot >= listings.size()) return true;

        AuctionManager.Listing listing = listings.get(slot);
        player.closeInventory();

        if ("mine".equals(mode)) {
            executeCancel(player, listing);
        } else {
            executeFulfill(player, listing);
        }
        return true;
    }

    public static boolean isOrderGui(InventoryView view) {
        if (view == null || view.getTopInventory().getSize() != 54) return false;
        String title = LegacyComponentSerializer.legacyAmpersand().serialize(view.title());
        return title.contains("Auftragshaus") || title.contains("Meine Auftr");
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void openPage(Player player, List<AuctionManager.Listing> all, int page,
                          String titlePrefix, String mode) {
        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        pages.put(player.getUniqueId(), page);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, all.size());
        List<AuctionManager.Listing> current = all.subList(start, end);
        pageListings.put(player.getUniqueId(), new ArrayList<>(current));

        String title = titlePrefix + (page + 1) + "/" + totalPages;
        Inventory inv = Bukkit.createInventory(null, 54, component(title));

        for (int i = 0; i < current.size(); i++) {
            inv.setItem(i, buildOrderItem(current.get(i), mode));
        }

        // Navigation bar
        if (page > 0) inv.setItem(45, buildNavItem(Material.ARROW, "&a\u2190 Vorherige Seite", ""));
        if ("mine".equals(mode)) {
            inv.setItem(49, buildNavItem(Material.BOOK, "&6Meine Auftr\u00e4ge", "&7Klick zum Abbrechen"));
            inv.setItem(50, buildNavItem(Material.COMPASS, "&eAlle Auftr\u00e4ge anzeigen", "&7Zur \u00f6ffentlichen Liste"));
        } else {
            inv.setItem(49, buildNavItem(Material.BOOK, "&6Auftragshaus",
                    "&7/order <Item> <Menge> <Preis>\n&7Erstelle einen Kaufauftrag"));
            inv.setItem(50, buildNavItem(Material.CHEST, "&eMeine Auftr\u00e4ge anzeigen", "&7Zeigt deine eigenen Auftr\u00e4ge"));
        }
        if (page < totalPages - 1) inv.setItem(53, buildNavItem(Material.ARROW, "&aN\u00e4chste Seite \u2192", ""));

        player.openInventory(inv);
    }

    private void executeFulfill(Player player, AuctionManager.Listing listing) {
        EconomyManager eco = plugin.getEconomyManager();
        AuctionManager ah = plugin.getAuctionManager();

        if (ah.getListing(listing.id) == null) {
            player.sendMessage(component("&cDieser Auftrag ist nicht mehr verf\u00fcgbar."));
            return;
        }
        if (listing.seller.equals(player.getUniqueId())) {
            player.sendMessage(component("&cDu kannst deinen eigenen Auftrag nicht erf\u00fcllen!"));
            return;
        }
        Material mat = listing.item.getType();
        int needed = listing.item.getAmount();
        int has = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat) has += is.getAmount();
        }
        if (has < needed) {
            player.sendMessage(component("&cNicht genug &e" + mat.name()
                    + " &c(hast: " + has + ", braucht: " + needed + ")"));
            return;
        }
        // Remove items from fulfiller
        int rem = needed;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat && rem > 0) {
                if (is.getAmount() <= rem) { rem -= is.getAmount(); is.setAmount(0); }
                else { is.setAmount(is.getAmount() - rem); rem = 0; }
            }
        }
        eco.deposit(player.getUniqueId(), listing.price);
        eco.saveAsync();
        // Deliver items to requester (handles amounts > 64)
        Player requester = Bukkit.getPlayer(listing.seller);
        if (requester != null) {
            AuctionGui.giveItemsSplit(requester, mat, needed);
            requester.sendMessage(component("&a[Auftrag] Dein Auftrag #" + listing.id
                    + " wurde von &e" + player.getName() + " &aerf\u00fcllt!"));
        }
        ah.removeListing(listing.id);
        player.sendMessage(component("&aAuftrag #" + listing.id
                + " erf\u00fcllt \u2192 &6" + eco.format(listing.price) + " &aerhalten!"));
        plugin.getScoreboardManager().update(player);
    }

    private void executeCancel(Player player, AuctionManager.Listing listing) {
        EconomyManager eco = plugin.getEconomyManager();
        AuctionManager ah = plugin.getAuctionManager();

        if (ah.getListing(listing.id) == null) {
            player.sendMessage(component("&cDieser Auftrag existiert nicht mehr."));
            return;
        }
        if (!listing.seller.equals(player.getUniqueId()) && !player.hasPermission("klassenplugin.order.admin")) {
            player.sendMessage(component("&cDas ist nicht dein Auftrag!"));
            return;
        }
        eco.deposit(listing.seller, listing.price);
        eco.saveAsync();
        ah.removeListing(listing.id);
        player.sendMessage(component("&aAuftrag #" + listing.id
                + " abgebrochen. &6" + eco.format(listing.price) + " &azur\u00fcckerstattet!"));
        plugin.getScoreboardManager().update(player);
    }

    // -----------------------------------------------------------------------
    // Item builders
    // -----------------------------------------------------------------------

    private ItemStack buildOrderItem(AuctionManager.Listing listing, String mode) {
        EconomyManager eco = plugin.getEconomyManager();
        Material mat = listing.item.getType();
        int displayAmt = Math.min(listing.item.getAmount(), mat.getMaxStackSize());
        ItemStack display = new ItemStack(mat, Math.max(1, displayAmt));
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        meta.displayName(component("&e[Auftrag] &f" + listing.item.getAmount() + "x " + mat.name()));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(component("&7Preis: &6" + eco.format(listing.price)));
        lore.add(component("&7Von: &e" + listing.sellerName));
        lore.add(component("&7ID: &b#" + listing.id));
        lore.add(component(""));
        lore.add(component("mine".equals(mode) ? "&cKlick zum Abbrechen" : "&aKlick zum Erf\u00fcllen"));
        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack buildNavItem(Material mat, String name, String loreText) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(component(name));
        if (!loreText.isEmpty()) {
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : loreText.split("\n")) {
                lore.add(component(line));
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    private static net.kyori.adventure.text.Component component(String legacy) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy);
    }
}
