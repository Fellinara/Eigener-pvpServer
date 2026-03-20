package de.klassenplugin.gui;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.EconomyManager;
import de.klassenplugin.managers.ShopManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Chest-GUI for the shop ({@code /shop}).
 *
 * <p>Layout – 6 rows (54 slots):
 * <ul>
 *   <li>Rows 0–4 (slots 0–44): item icons (up to 45 per page)</li>
 *   <li>Slot 45: previous page</li>
 *   <li>Slot 48: close / info</li>
 *   <li>Slot 53: next page</li>
 * </ul>
 *
 * <p>Click actions:
 * <ul>
 *   <li>Left-click → buy 1</li>
 *   <li>Shift+Left-click → buy 64</li>
 *   <li>Right-click → sell all of that item from inventory</li>
 * </ul>
 */
public class ShopGui {

    private static final String GUI_TITLE_PREFIX = "\u00a78[\u00a76Shop\u00a78] ";
    private static final int PAGE_SIZE = 45;

    private final KlassenPlugin plugin;
    /** Current page (0-based) per player. */
    private final Map<UUID, Integer> pages = new HashMap<>();
    /** Item names visible on the current page, mapped to slot index. */
    private final Map<UUID, List<String>> pageItems = new HashMap<>();

    public ShopGui(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public void open(Player player) {
        pages.putIfAbsent(player.getUniqueId(), 0);
        build(player);
    }

    public void handleClick(Player player, int slot, ClickType clickType) {
        UUID uuid = player.getUniqueId();
        List<String> items = pageItems.get(uuid);
        int currentPage = pages.getOrDefault(uuid, 0);
        ShopManager shop = plugin.getShopManager();
        List<String> allItems = new ArrayList<>(shop.getItemNames());
        int totalPages = Math.max(1, (int) Math.ceil(allItems.size() / (double) PAGE_SIZE));

        if (slot == 45) {
            // Previous page
            if (currentPage > 0) {
                pages.put(uuid, currentPage - 1);
                build(player);
            }
            return;
        }
        if (slot == 53) {
            // Next page
            if (currentPage < totalPages - 1) {
                pages.put(uuid, currentPage + 1);
                build(player);
            }
            return;
        }
        if (slot == 48) {
            player.closeInventory();
            return;
        }

        if (items == null || slot >= items.size() || slot < 0) return;
        String matName = items.get(slot);
        if (matName == null) return;

        EconomyManager eco = plugin.getEconomyManager();

        if (clickType == ClickType.LEFT) {
            buyItem(player, shop, eco, matName, 1);
        } else if (clickType == ClickType.SHIFT_LEFT) {
            buyItem(player, shop, eco, matName, 64);
        } else if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            sellAllOfItem(player, shop, eco, matName);
        }

        // Refresh to show updated balance/info.
        build(player);
    }

    public static boolean isShopGui(InventoryView view) {
        String title = LegacyComponentSerializer.legacyAmpersand().serialize(view.title());
        return title.contains("[&6Shop&8]");
    }

    public void cleanup(Player player) {
        pages.remove(player.getUniqueId());
        pageItems.remove(player.getUniqueId());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void build(Player player) {
        UUID uuid = player.getUniqueId();
        ShopManager shop = plugin.getShopManager();
        EconomyManager eco = plugin.getEconomyManager();
        List<String> allItems = new ArrayList<>(shop.getItemNames());
        int currentPage = pages.getOrDefault(uuid, 0);
        int totalPages = Math.max(1, (int) Math.ceil(allItems.size() / (double) PAGE_SIZE));

        Inventory inv = Bukkit.createInventory(null, 54,
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&8[&6Shop&8] &7Seite " + (currentPage + 1) + "/" + totalPages));

        List<String> currentItems = new ArrayList<>();
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allItems.size());

        for (int i = start; i < end; i++) {
            String matName = allItems.get(i);
            Material mat = Material.matchMaterial(matName);
            ItemStack icon;
            if (mat == null || mat == Material.AIR) {
                icon = new ItemStack(Material.BARRIER);
            } else {
                icon = new ItemStack(mat);
            }
            ItemMeta meta = icon.getItemMeta();
            double buy = shop.getBuyPrice(matName);
            double sell = shop.getSellPrice(matName);
            meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&b" + matName));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(buy >= 0
                    ? "&7Kauf: &6" + eco.format(buy) + " &8(LK=1 / Shift+LK=64)"
                    : "&7Kauf: &cnicht verfügbar");
            lore.add(sell >= 0
                    ? "&7Verkauf: &a" + eco.format(sell) + " &8(RK=alle verkaufen)"
                    : "&7Verkauf: &7n/a");
            lore.add("");
            lore.add("&7Dein Guthaben: &6" + eco.format(eco.getBalance(player.getUniqueId())));
            meta.lore(lore.stream()
                    .map(l -> LegacyComponentSerializer.legacyAmpersand().deserialize(l))
                    .toList());
            icon.setItemMeta(meta);
            inv.setItem(i - start, icon);
            currentItems.add(matName);
        }
        // Pad with null so index = slot.
        while (currentItems.size() < PAGE_SIZE) currentItems.add(null);
        pageItems.put(uuid, currentItems);

        // Navigation buttons.
        if (currentPage > 0) {
            inv.setItem(45, navItem(Material.ARROW, "&aVorige Seite", "&7Klicke um zurück zu gehen"));
        }
        // Close button.
        inv.setItem(48, navItem(Material.BARRIER, "&cSchließen", "&7Klicke um den Shop zu schließen"));
        if (currentPage < totalPages - 1) {
            inv.setItem(53, navItem(Material.ARROW, "&aNächste Seite", "&7Klicke um zur nächsten Seite zu gehen"));
        }

        // Re-open inventory (or open fresh).
        if (player.getOpenInventory().getTopInventory().getSize() == 54
                && isShopGui(player.getOpenInventory())) {
            player.getOpenInventory().getTopInventory().setContents(inv.getContents());
        } else {
            player.openInventory(inv);
        }
    }

    private void buyItem(Player player, ShopManager shop, EconomyManager eco,
                         String matName, int amount) {
        double price = shop.getBuyPrice(matName);
        if (price < 0) {
            player.sendMessage(KlassenPlugin.colorizeComponent("&cDieser Artikel kann nicht gekauft werden!"));
            return;
        }
        double total = price * amount;
        if (!eco.has(player.getUniqueId(), total)) {
            player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cNicht genug Geld! Preis: &6" + eco.format(total)));
            return;
        }
        Material mat = Material.matchMaterial(matName);
        if (mat == null) return;
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(KlassenPlugin.colorizeComponent("&cInventar voll!"));
            return;
        }
        eco.withdraw(player.getUniqueId(), total);
        eco.saveAsync();
        player.getInventory().addItem(new ItemStack(mat, amount));
        player.sendMessage(KlassenPlugin.colorizeComponent(
                "&aGekauft: &e" + amount + "x " + matName + " &afür &6" + eco.format(total)));
        plugin.getScoreboardManager().update(player);
    }

    private void sellAllOfItem(Player player, ShopManager shop, EconomyManager eco,
                               String matName) {
        double price = shop.getEffectiveSellPrice(matName);
        if (price < 0) {
            player.sendMessage(KlassenPlugin.colorizeComponent("&cDieser Artikel kann nicht verkauft werden!"));
            return;
        }
        Material mat = Material.matchMaterial(matName);
        if (mat == null) return;

        int total = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat) total += is.getAmount();
        }
        if (total == 0) {
            player.sendMessage(KlassenPlugin.colorizeComponent("&cDu hast kein &e" + matName + " &cim Inventar!"));
            return;
        }
        // Remove items.
        int rem = total;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat && rem > 0) {
                if (is.getAmount() <= rem) {
                    rem -= is.getAmount();
                    is.setAmount(0);
                } else {
                    is.setAmount(is.getAmount() - rem);
                    rem = 0;
                }
            }
        }
        double earned = price * total;
        eco.deposit(player.getUniqueId(), earned);
        eco.saveAsync();
        player.sendMessage(KlassenPlugin.colorizeComponent(
                "&aVerkauft: &e" + total + "x " + matName + " &afür &6" + eco.format(earned)));
        plugin.getScoreboardManager().update(player);
    }

    private ItemStack navItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        meta.lore(List.of(LegacyComponentSerializer.legacyAmpersand().deserialize(lore)));
        item.setItemMeta(meta);
        return item;
    }
}
