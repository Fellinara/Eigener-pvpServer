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
 * Category-based chest-GUI for the shop ({@code /shop}).
 *
 * <p><b>Category overview</b> (first screen): one icon per category; click to enter.
 *
 * <p><b>Item list</b> (after selecting a category):
 * <ul>
 *   <li>Rows 0–4 (slots 0–44): up to 45 items per page</li>
 *   <li>Slot 45: previous page</li>
 *   <li>Slot 46: back to categories</li>
 *   <li>Slot 48: close shop</li>
 *   <li>Slot 53: next page</li>
 * </ul>
 *
 * <p>Click actions on items:
 * <ul>
 *   <li>Left-click → buy 1</li>
 *   <li>Shift+Left-click → buy 64</li>
 *   <li>Right-click → sell all of that item from inventory</li>
 * </ul>
 */
public class ShopGui {

    private static final String GUI_TITLE_PREFIX = "&8[&6Shop&8] ";
    private static final int PAGE_SIZE = 45;

    private final KlassenPlugin plugin;

    // null = category overview; non-null = item list for that category name
    private final Map<UUID, String> selectedCategory = new HashMap<>();
    /** Current page (0-based) inside a category's item list. */
    private final Map<UUID, Integer> pages = new HashMap<>();
    /** Item material names for the currently displayed item-list page. */
    private final Map<UUID, List<String>> pageItems = new HashMap<>();

    public ShopGui(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /** Opens the shop at the category overview for {@code player}. */
    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        selectedCategory.remove(uuid);
        pages.remove(uuid);
        pageItems.remove(uuid);
        buildCategoryView(player);
    }

    public void handleClick(Player player, int slot, ClickType clickType) {
        if (selectedCategory.containsKey(player.getUniqueId())) {
            handleItemClick(player, slot, clickType);
        } else {
            handleCategoryClick(player, slot);
        }
    }

    public static boolean isShopGui(InventoryView view) {
        String title = LegacyComponentSerializer.legacyAmpersand().serialize(view.title());
        return title.contains("[&6Shop&8]");
    }

    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        selectedCategory.remove(uuid);
        pages.remove(uuid);
        pageItems.remove(uuid);
    }

    // ── Category overview ────────────────────────────────────────────────────

    private void buildCategoryView(Player player) {
        List<Map.Entry<String, String[]>> cats = new ArrayList<>(ShopManager.CATEGORIES.entrySet());

        Inventory inv = Bukkit.createInventory(null, 54,
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        GUI_TITLE_PREFIX + "&7Kategorien"));

        for (int i = 0; i < cats.size() && i < 45; i++) {
            String catName = cats.get(i).getKey();
            int itemCount  = cats.get(i).getValue().length;
            Material icon  = ShopManager.CATEGORY_ICONS.getOrDefault(catName, Material.CHEST);

            ItemStack item = new ItemStack(icon);
            ItemMeta meta  = item.getItemMeta();
            meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&e&l" + catName));
            meta.lore(List.of(
                    LegacyComponentSerializer.legacyAmpersand()
                            .deserialize("&7" + itemCount + " Artikel"),
                    LegacyComponentSerializer.legacyAmpersand().deserialize(""),
                    LegacyComponentSerializer.legacyAmpersand()
                            .deserialize("&aKlicke zum Öffnen")));
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        inv.setItem(49, navItem(Material.BARRIER, "&cSchließen", "&7Shop schließen"));

        openOrUpdate(player, inv);
    }

    private void handleCategoryClick(Player player, int slot) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        List<String> catNames = new ArrayList<>(ShopManager.CATEGORIES.keySet());
        if (slot < 0 || slot >= catNames.size()) return;

        UUID uuid = player.getUniqueId();
        selectedCategory.put(uuid, catNames.get(slot));
        pages.put(uuid, 0);
        buildItemView(player);
    }

    // ── Item list (per category) ──────────────────────────────────────────────

    private void buildItemView(Player player) {
        UUID uuid = player.getUniqueId();
        String cat = selectedCategory.get(uuid);
        if (cat == null) { buildCategoryView(player); return; }

        ShopManager shop = plugin.getShopManager();
        EconomyManager eco  = plugin.getEconomyManager();
        String[] catItems   = ShopManager.CATEGORIES.getOrDefault(cat, new String[0]);

        int currentPage = pages.getOrDefault(uuid, 0);
        int totalPages  = Math.max(1, (int) Math.ceil(catItems.length / (double) PAGE_SIZE));
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));
        pages.put(uuid, currentPage);

        Inventory inv = Bukkit.createInventory(null, 54,
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        GUI_TITLE_PREFIX + "&7" + cat
                                + " &8(&7" + (currentPage + 1) + "/" + totalPages + "&8)"));

        List<String> currentItems = new ArrayList<>();
        int start = currentPage * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, catItems.length);

        for (int i = start; i < end; i++) {
            String matName = catItems[i];

            // Determine icon and display name (special handling for enchanted books and spawners)
            final ItemStack icon;
            final String displayName;
            if (ShopManager.isEnchantedBookKey(matName)) {
                icon        = ShopManager.buildEnchantedBook(matName);
                displayName = ShopManager.getEnchantedBookDisplayName(matName);
            } else if (ShopManager.isSpawnerKey(matName)) {
                icon        = ShopManager.buildSpawner(matName);
                displayName = ShopManager.getSpawnerDisplayName(matName);
            } else {
                Material mat = Material.matchMaterial(matName);
                icon        = (mat != null && mat != Material.AIR)
                        ? new ItemStack(mat) : new ItemStack(Material.BARRIER);
                displayName = matName;
            }

            ItemMeta meta  = icon.getItemMeta();

            double buy  = shop.getBuyPrice(matName);
            double sell = shop.getSellPrice(matName);
            meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&b" + displayName));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(buy >= 0
                    ? "&7Kauf: &6" + eco.format(buy) + " &8(LK=1 / Shift+LK=64)"
                    : "&7Kauf: &cnicht verfügbar");
            lore.add(sell >= 0
                    ? "&7Verkauf: &a" + eco.format(sell) + " &8(RK=alle verkaufen)"
                    : "&7Verkauf: &7nicht möglich");
            lore.add("");
            lore.add("&7Dein Guthaben: &6" + eco.format(eco.getBalance(player.getUniqueId())));
            meta.lore(lore.stream()
                    .map(l -> LegacyComponentSerializer.legacyAmpersand().deserialize(l))
                    .toList());
            icon.setItemMeta(meta);
            inv.setItem(i - start, icon);
            currentItems.add(matName);
        }
        while (currentItems.size() < PAGE_SIZE) currentItems.add(null);
        pageItems.put(uuid, currentItems);

        // Navigation row (row 5, slots 45-53)
        if (currentPage > 0)
            inv.setItem(45, navItem(Material.ARROW, "&aVorige Seite", "&7Zurück blättern"));
        inv.setItem(46, navItem(Material.COMPASS, "&eKategorien", "&7Zurück zur Kategorieübersicht"));
        inv.setItem(48, navItem(Material.BARRIER, "&cSchließen", "&7Shop schließen"));
        if (currentPage < totalPages - 1)
            inv.setItem(53, navItem(Material.ARROW, "&aNächste Seite", "&7Weiter blättern"));

        openOrUpdate(player, inv);
    }

    private void handleItemClick(Player player, int slot, ClickType clickType) {
        UUID uuid        = player.getUniqueId();
        int currentPage  = pages.getOrDefault(uuid, 0);
        String cat       = selectedCategory.get(uuid);
        String[] catItems = ShopManager.CATEGORIES.getOrDefault(cat, new String[0]);
        int totalPages   = Math.max(1, (int) Math.ceil(catItems.length / (double) PAGE_SIZE));

        if (slot == 45) {
            if (currentPage > 0) { pages.put(uuid, currentPage - 1); buildItemView(player); }
            return;
        }
        if (slot == 46) {
            // Back to category overview
            selectedCategory.remove(uuid);
            pages.remove(uuid);
            pageItems.remove(uuid);
            buildCategoryView(player);
            return;
        }
        if (slot == 48) {
            player.closeInventory();
            return;
        }
        if (slot == 53) {
            if (currentPage < totalPages - 1) { pages.put(uuid, currentPage + 1); buildItemView(player); }
            return;
        }

        List<String> items = pageItems.get(uuid);
        if (items == null || slot < 0 || slot >= items.size()) return;
        String matName = items.get(slot);
        if (matName == null) return;

        ShopManager shop = plugin.getShopManager();
        EconomyManager eco = plugin.getEconomyManager();

        if (clickType == ClickType.LEFT) {
            buyItem(player, shop, eco, matName, 1);
        } else if (clickType == ClickType.SHIFT_LEFT) {
            buyItem(player, shop, eco, matName, 64);
        } else if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            sellAllOfItem(player, shop, eco, matName);
        }

        buildItemView(player);
    }

    // ── Buy / Sell ────────────────────────────────────────────────────────────

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
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(KlassenPlugin.colorizeComponent("&cInventar voll!"));
            return;
        }

        final ItemStack item;
        final String displayName;
        if (ShopManager.isEnchantedBookKey(matName)) {
            ItemStack book = ShopManager.buildEnchantedBook(matName);
            book.setAmount(amount);
            item        = book;
            displayName = ShopManager.getEnchantedBookDisplayName(matName);
        } else if (ShopManager.isSpawnerKey(matName)) {
            ItemStack spawner = ShopManager.buildSpawner(matName);
            spawner.setAmount(amount);
            item        = spawner;
            displayName = ShopManager.getSpawnerDisplayName(matName);
        } else {
            Material mat = Material.matchMaterial(matName);
            if (mat == null) return;
            item        = new ItemStack(mat, amount);
            displayName = matName;
        }

        eco.withdraw(player.getUniqueId(), total);
        eco.saveAsync();
        player.getInventory().addItem(item);
        player.sendMessage(KlassenPlugin.colorizeComponent(
                "&aGekauft: &e" + amount + "x " + displayName + " &afür &6" + eco.format(total)));
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
            player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cDu hast kein &e" + matName + " &cim Inventar!"));
            return;
        }
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void openOrUpdate(Player player, Inventory inv) {
        if (player.getOpenInventory().getTopInventory().getSize() == 54
                && isShopGui(player.getOpenInventory())) {
            player.getOpenInventory().getTopInventory().setContents(inv.getContents());
        } else {
            player.openInventory(inv);
        }
    }

    private ItemStack navItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        meta.lore(List.of(LegacyComponentSerializer.legacyAmpersand().deserialize(lore)));
        item.setItemMeta(meta);
        return item;
    }
}
