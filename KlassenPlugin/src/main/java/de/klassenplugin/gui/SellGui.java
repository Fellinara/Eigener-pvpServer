package de.klassenplugin.gui;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.EconomyManager;
import de.klassenplugin.managers.ShopManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Drop-in sell GUI ({@code /sell}).
 *
 * <p>Opens a 4-row (36-slot) chest inventory.  Players place any items they
 * want to sell into the slots.  When the inventory is closed, all items are
 * sold at the current shop sell price (50 % / 60 % of buy price) and the
 * player receives coins.  Items that cannot be sold are returned to the
 * player's inventory (or dropped at their feet if full).
 *
 * <p>A read-only info icon occupies slot 35 (bottom-right corner) to remind
 * the player of the mechanic.
 */
public class SellGui {

    private static final String TITLE = "&8[&aVerkaufen&8] &7Lege Items rein & schließe";
    private static final String TITLE_MARKER = "[&aVerkaufen&8]";
    /** Slot used for the info icon — cannot have items placed there. */
    private static final int INFO_SLOT = 35;

    private final KlassenPlugin plugin;

    public SellGui(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36,
                LegacyComponentSerializer.legacyAmpersand().deserialize(TITLE));
        inv.setItem(INFO_SLOT, infoIcon());
        player.openInventory(inv);
    }

    /** Returns {@code true} when the view belongs to a SellGui. */
    public static boolean isSellGui(InventoryView view) {
        String title = LegacyComponentSerializer.legacyAmpersand().serialize(view.title());
        return title.contains(TITLE_MARKER);
    }

    /**
     * Called from {@link de.klassenplugin.listeners.GuiListener} when a
     * SellGui inventory is closed.  Sells all items left in the GUI and
     * deposits the proceeds.  Items that cannot be sold are returned to
     * the player.
     *
     * @param player the player who closed the inventory
     * @param inv    the top inventory (the sell GUI)
     */
    public void processSell(Player player, Inventory inv) {
        ShopManager shop = plugin.getShopManager();
        EconomyManager eco = plugin.getEconomyManager();

        double totalEarned = 0;
        int totalSold = 0;

        for (int slot = 0; slot < inv.getSize(); slot++) {
            if (slot == INFO_SLOT) continue;
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            String matName = item.getType().name();
            double price = shop.getEffectiveSellPrice(matName);

            if (price <= 0) {
                // Return unsellable items to the player
                returnItem(player, item);
                continue;
            }
            totalEarned += price * item.getAmount();
            totalSold   += item.getAmount();
        }

        if (totalSold > 0) {
            eco.deposit(player.getUniqueId(), totalEarned);
            eco.saveAsync();
            player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&aVerkauft: &e" + totalSold + " Items &afür &6"
                    + eco.format(totalEarned)));
            plugin.getScoreboardManager().update(player);
        } else {
            player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&7Keine verkaufbaren Items im Verkaufsfenster."));
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Returns the item to the player's inventory; drops at feet if full. */
    private void returnItem(Player player, ItemStack item) {
        var leftovers = player.getInventory().addItem(item.clone());
        leftovers.values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private ItemStack infoIcon() {
        ItemStack icon = new ItemStack(Material.EMERALD);
        ItemMeta meta  = icon.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&a&lVerkaufen"));
        meta.lore(List.of(
                LegacyComponentSerializer.legacyAmpersand().deserialize("&7Lege deine Items oben rein."),
                LegacyComponentSerializer.legacyAmpersand().deserialize("&7Beim Schließen werden sie verkauft."),
                LegacyComponentSerializer.legacyAmpersand().deserialize(""),
                LegacyComponentSerializer.legacyAmpersand().deserialize("&7Preis: &a50 % &7des Kaufpreises"),
                LegacyComponentSerializer.legacyAmpersand().deserialize("&7Wertvolle Items: &660 %")));
        icon.setItemMeta(meta);
        return icon;
    }
}
