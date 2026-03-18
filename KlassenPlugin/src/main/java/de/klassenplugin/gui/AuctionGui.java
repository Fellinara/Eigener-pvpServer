package de.klassenplugin.gui;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.AuctionManager;
import de.klassenplugin.managers.EconomyManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
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
 * Layout (6 rows = 54 slots):
 *   Rows 0-4 (slots 0-44): up to 45 listing icons
 *   Row 5: [←Zurück | info | Weiter→] at slots 45, 49, 53
 */
public class AuctionGui {

    /** Number of listing slots per page. */
    private static final int PAGE_SIZE = 45;

    private final KlassenPlugin plugin;
    /** Current page index (0-based) per player. */
    private final Map<UUID, Integer> pages = new HashMap<>();
    /** Cached listing-index offset so the click handler can map slot → listing. */
    private final Map<UUID, List<AuctionManager.Listing>> pageListings = new HashMap<>();

    public AuctionGui(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Open (or refresh) the AH GUI for a player. */
    public void open(Player player, int page) {
        List<AuctionManager.Listing> all = plugin.getAuctionManager().getAllListings();
        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        pages.put(player.getUniqueId(), page);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, all.size());
        List<AuctionManager.Listing> current = all.subList(start, end);
        pageListings.put(player.getUniqueId(), new ArrayList<>(current));

        String title = "&8[&6Auktionshaus&8] &7Seite " + (page + 1) + "/" + totalPages;
        Inventory inv = Bukkit.createInventory(null, 54, component(title));

        // Listing items
        for (int i = 0; i < current.size(); i++) {
            inv.setItem(i, buildListingItem(current.get(i)));
        }

        // Navigation bar
        if (page > 0) inv.setItem(45, buildNavItem(Material.ARROW, "&a\u2190 Vorherige Seite", ""));
        inv.setItem(49, buildNavItem(Material.BOOK, "&6Auktionshaus",
                "&7/ah sell <Preis> \u2013 Item verkaufen\n&7/ah request <Item> <Menge> <Preis>"));
        if (page < totalPages - 1) inv.setItem(53, buildNavItem(Material.ARROW, "&aN\u00e4chste Seite \u2192", ""));

        player.openInventory(inv);
    }

    /** Convenience: open page 0. */
    public void open(Player player) {
        open(player, 0);
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

        // Navigation
        if (slot == 45 && currentPage > 0) {
            open(player, currentPage - 1);
            return true;
        }
        if (slot == 53) {
            open(player, currentPage + 1);
            return true;
        }
        if (slot == 49) {
            return true; // info slot — just consume
        }

        // Listing click
        if (slot >= PAGE_SIZE) return false;
        List<AuctionManager.Listing> listings = pageListings.get(player.getUniqueId());
        if (listings == null || slot >= listings.size()) return true;

        AuctionManager.Listing listing = listings.get(slot);
        player.closeInventory();

        if (listing.type == AuctionManager.ListingType.SELL) {
            executeBuy(player, listing);
        } else {
            executeFulfill(player, listing);
        }
        return true;
    }

    /**
     * @return true if this inventory is one of our AH GUIs (title check).
     */
    public static boolean isAhGui(Inventory inv) {
        if (inv == null || inv.getSize() != 54) return false;
        String title = LegacyComponentSerializer.legacyAmpersand().serialize(inv.title());
        return title.contains("Auktionshaus");
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

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
            requester.getInventory().addItem(listing.item.clone());
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

    private ItemStack buildListingItem(AuctionManager.Listing listing) {
        EconomyManager eco = plugin.getEconomyManager();
        boolean isSell = listing.type == AuctionManager.ListingType.SELL;
        ItemStack display = listing.item.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        String typeLabel = isSell ? "&a[Verkauf]" : "&e[Anfrage]";
        meta.displayName(component(typeLabel + " &f"
                + listing.item.getAmount() + "x " + listing.item.getType().name()));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(component("&7Preis: &6" + eco.format(listing.price)));
        lore.add(component("&7Verk\u00e4ufer: &e" + listing.sellerName));
        lore.add(component("&7ID: &b#" + listing.id));
        lore.add(component(""));
        lore.add(component(isSell ? "&aKlick zum Kaufen" : "&eKlick zum Erf\u00fcllen"));
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
