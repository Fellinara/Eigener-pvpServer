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
 * Inventory-based GUI for the Auction House (/ah).
 *
 * Two modes:
 *   "all"   – all listings, SELL → buy, REQUEST → fulfill  (slots 0-44)
 *   "meine" – only the player's own listings, click → cancel
 *
 * Layout (6 rows = 54 slots):
 *   Rows 0-4 (slots 0-44): up to 45 listing icons
 *   Row 5: [← slot 45 | info slot 49 | switch-view slot 50 | → slot 53]
 */
public class AuctionGui {

    /** Number of listing slots per page. */
    private static final int PAGE_SIZE = 45;

    private final KlassenPlugin plugin;
    /** Current page index (0-based) per player. */
    private final Map<UUID, Integer> pages = new HashMap<>();
    /** Cached listing so the click handler can map slot → listing. */
    private final Map<UUID, List<AuctionManager.Listing>> pageListings = new HashMap<>();
    /** "all" or "meine" per player. */
    private final Map<UUID, String> modes = new HashMap<>();

    public AuctionGui(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Open (or refresh) the AH GUI for a player (all listings). */
    public void open(Player player, int page) {
        modes.put(player.getUniqueId(), "all");
        List<AuctionManager.Listing> all = plugin.getAuctionManager().getAllListings();
        openPage(player, all, page, "&8[&6Auktionshaus&8] &7Seite ", "all");
    }

    /** Convenience: open page 0 (all listings). */
    public void open(Player player) {
        open(player, 0);
    }

    /** Open the "Meine Angebote" view – only the player's own listings. */
    public void openMeine(Player player, int page) {
        modes.put(player.getUniqueId(), "meine");
        List<AuctionManager.Listing> mine = plugin.getAuctionManager().getListingsBySeller(player.getUniqueId());
        openPage(player, mine, page, "&8[&6Meine Angebote&8] &7Seite ", "meine");
    }

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
            inv.setItem(i, buildListingItem(current.get(i), mode));
        }

        // Navigation bar (row 6)
        if (page > 0) inv.setItem(45, buildNavItem(Material.ARROW, "&a\u2190 Vorherige Seite", ""));
        if ("meine".equals(mode)) {
            inv.setItem(49, buildNavItem(Material.BOOK, "&6Meine Angebote", "&7Klick zum Stornieren"));
            inv.setItem(50, buildNavItem(Material.COMPASS, "&eAlle Angebote anzeigen", "&7Wechselt zur \u00f6ffentlichen Liste"));
        } else {
            inv.setItem(49, buildNavItem(Material.BOOK, "&6Auktionshaus",
                    "&7/ah sell <Preis> \u2013 Item verkaufen\n&7/ah request <Item> <Menge> <Preis>"));
            inv.setItem(50, buildNavItem(Material.CHEST, "&eMeine Angebote anzeigen", "&7Zeigt nur deine eigenen Angebote"));
        }
        if (page < totalPages - 1) inv.setItem(53, buildNavItem(Material.ARROW, "&aN\u00e4chste Seite \u2192", ""));

        player.openInventory(inv);
    }

    // -----------------------------------------------------------------------
    // Click handling (called by GuiListener)
    // -----------------------------------------------------------------------

    /**
     * Handle a click inside the AH inventory.
     * @return true if the event was consumed (should be cancelled).
     */
    public boolean handleClick(Player player, int slot) {
        if (slot < 0) return false;

        int currentPage = pages.getOrDefault(player.getUniqueId(), 0);
        String mode = modes.getOrDefault(player.getUniqueId(), "all");

        // Navigation
        if (slot == 45 && currentPage > 0) {
            if ("meine".equals(mode)) openMeine(player, currentPage - 1);
            else open(player, currentPage - 1);
            return true;
        }
        if (slot == 53) {
            if ("meine".equals(mode)) openMeine(player, currentPage + 1);
            else open(player, currentPage + 1);
            return true;
        }
        if (slot == 49) {
            return true; // info slot — just consume
        }
        // Switch-view button
        if (slot == 50) {
            if ("meine".equals(mode)) open(player, 0);
            else openMeine(player, 0);
            return true;
        }

        // Listing click
        if (slot >= PAGE_SIZE) return false;
        List<AuctionManager.Listing> listings = pageListings.get(player.getUniqueId());
        if (listings == null || slot >= listings.size()) return true;

        AuctionManager.Listing listing = listings.get(slot);
        player.closeInventory();

        if ("meine".equals(mode)) {
            executeCancel(player, listing);
        } else if (listing.type == AuctionManager.ListingType.SELL) {
            executeBuy(player, listing);
        } else {
            executeFulfill(player, listing);
        }
        return true;
    }

    /**
     * @return true if this inventory view is one of our AH GUIs (title check).
     */
    public static boolean isAhGui(InventoryView view) {
        if (view == null || view.getTopInventory().getSize() != 54) return false;
        String title = LegacyComponentSerializer.legacyAmpersand().serialize(view.title());
        return title.contains("Auktionshaus") || title.contains("Meine Angebote");
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void executeCancel(Player player, AuctionManager.Listing listing) {
        EconomyManager eco = plugin.getEconomyManager();
        AuctionManager ah = plugin.getAuctionManager();

        if (ah.getListing(listing.id) == null) {
            player.sendMessage(component("&cDieses Angebot existiert nicht mehr."));
            return;
        }
        if (!listing.seller.equals(player.getUniqueId()) && !player.hasPermission("klassenplugin.auction.admin")) {
            player.sendMessage(component("&cDas ist nicht dein Angebot!"));
            return;
        }
        ah.removeListing(listing.id);
        if (listing.type == AuctionManager.ListingType.SELL) {
            giveItemsSplit(player, listing.item.getType(), listing.item.getAmount());
            player.sendMessage(component("&aAngebot #" + listing.id + " storniert. Artikel zur\u00fcck!"));
        } else {
            eco.deposit(listing.seller, listing.price);
            eco.saveAsync();
            player.sendMessage(component("&aAnfrage #" + listing.id + " storniert. &6"
                    + eco.format(listing.price) + " &azur\u00fcckerstattet!"));
        }
        plugin.getScoreboardManager().update(player);
    }

    private void executeBuy(Player player, AuctionManager.Listing listing) {
        EconomyManager eco = plugin.getEconomyManager();
        AuctionManager ah = plugin.getAuctionManager();

        // Revalidate: listing might have been bought already
        if (ah.getListing(listing.id) == null) {
            player.sendMessage(component("&cDieses Angebot ist nicht mehr verf\u00fcgbar."));
            return;
        }
        if (listing.seller.equals(player.getUniqueId())) {
            player.sendMessage(component("&cDu kannst dein eigenes Angebot nicht kaufen!"));
            return;
        }
        if (!eco.has(player.getUniqueId(), listing.price)) {
            player.sendMessage(component("&cNicht genug Geld! Preis: &6" + eco.format(listing.price)));
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(component("&cDein Inventar ist voll!"));
            return;
        }
        eco.withdraw(player.getUniqueId(), listing.price);
        eco.deposit(listing.seller, listing.price);
        eco.saveAsync();
        player.getInventory().addItem(listing.item.clone());
        ah.removeListing(listing.id);

        player.sendMessage(component("&aGekauft: &e"
                + listing.item.getAmount() + "x " + listing.item.getType().name()
                + " &af\u00fcr &6" + eco.format(listing.price)));
        Player seller = Bukkit.getPlayer(listing.seller);
        if (seller != null) {
            seller.sendMessage(component("&a[AH] &e" + player.getName()
                    + " &ahat dein Angebot #" + listing.id
                    + " f\u00fcr &6" + eco.format(listing.price) + " &agekauft!"));
        }
        plugin.getScoreboardManager().update(player);
    }

    private void executeFulfill(Player player, AuctionManager.Listing listing) {
        EconomyManager eco = plugin.getEconomyManager();
        AuctionManager ah = plugin.getAuctionManager();

        if (ah.getListing(listing.id) == null) {
            player.sendMessage(component("&cDiese Anfrage ist nicht mehr verf\u00fcgbar."));
            return;
        }
        if (listing.seller.equals(player.getUniqueId())) {
            player.sendMessage(component("&cDas ist deine eigene Anfrage!"));
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
        // Remove items
        int rem = needed;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat && rem > 0) {
                if (is.getAmount() <= rem) { rem -= is.getAmount(); is.setAmount(0); }
                else { is.setAmount(is.getAmount() - rem); rem = 0; }
            }
        }
        eco.deposit(player.getUniqueId(), listing.price);
        eco.saveAsync();
        Player requester = Bukkit.getPlayer(listing.seller);
        if (requester != null) {
            giveItemsSplit(requester, listing.item.getType(), listing.item.getAmount());
            requester.sendMessage(component("&a[AH] Anfrage #" + listing.id
                    + " erf\u00fcllt von &e" + player.getName()));
        }
        ah.removeListing(listing.id);
        player.sendMessage(component("&aAnfrage #" + listing.id
                + " erf\u00fcllt \u2192 &6" + eco.format(listing.price) + " &aerhalten!"));
        plugin.getScoreboardManager().update(player);
    }

    // -----------------------------------------------------------------------
    // Item builders
    // -----------------------------------------------------------------------

    private ItemStack buildListingItem(AuctionManager.Listing listing, String mode) {
        EconomyManager eco = plugin.getEconomyManager();
        boolean isSell = listing.type == AuctionManager.ListingType.SELL;
        // Cap display amount to 64 so the item renders without warnings
        Material mat = listing.item.getType();
        int displayAmt = Math.min(listing.item.getAmount(), mat.getMaxStackSize());
        ItemStack display = new ItemStack(mat, Math.max(1, displayAmt));
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        String typeLabel = isSell ? "&a[Verkauf]" : "&e[Anfrage]";
        meta.displayName(component(typeLabel + " &f"
                + listing.item.getAmount() + "x " + mat.name()));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(component("&7Preis: &6" + eco.format(listing.price)));
        lore.add(component("&7Verk\u00e4ufer: &e" + listing.sellerName));
        lore.add(component("&7ID: &b#" + listing.id));
        lore.add(component(""));
        if ("meine".equals(mode)) {
            lore.add(component("&cKlick zum Stornieren"));
        } else {
            lore.add(component(isSell ? "&aKlick zum Kaufen" : "&eKlick zum Erf\u00fcllen"));
        }
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

    /** Give potentially >64 items to a player, splitting into stacks automatically. */
    public static void giveItemsSplit(Player player, Material mat, int amount) {
        int maxStack = mat.getMaxStackSize();
        if (maxStack < 1) maxStack = 64;
        while (amount > 0) {
            int batch = Math.min(amount, maxStack);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(mat, batch));
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
            amount -= batch;
        }
    }

    private static net.kyori.adventure.text.Component component(String legacy) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy);
    }
}
