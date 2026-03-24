package de.klassenplugin.gui;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.AntiCheatManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paginated GUI showing the last 100 AntiCheat violation entries.
 *
 * <p>Open with: {@code /adminpanel} → click Verwarnungs-Log, or directly via
 * {@code /anticheat log} (future command alias).
 *
 * <p>Layout – 6 rows (54 slots):
 * <ul>
 *   <li>Slots 0–44: violation entries (paper icons)</li>
 *   <li>Slot 45: previous page</li>
 *   <li>Slot 49: info / close</li>
 *   <li>Slot 53: next page</li>
 * </ul>
 */
public class WarningsLogGui {

    private static final int PAGE_SIZE = 45;
    private static final String TITLE_MARKER = "Verwarnungs-Log";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    private final KlassenPlugin plugin;
    private final Map<UUID, Integer> pages = new ConcurrentHashMap<>();

    public WarningsLogGui(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void open(Player player) {
        pages.put(player.getUniqueId(), 0);
        build(player);
    }

    public void handleClick(Player player, int slot) {
        List<AntiCheatManager.ViolationEntry> log =
                plugin.getAntiCheatManager().getRecentViolations();
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        int totalPages = Math.max(1, (int) Math.ceil(log.size() / (double) PAGE_SIZE));

        if (slot == 45) {
            if (page > 0) { pages.put(player.getUniqueId(), page - 1); build(player); }
            return;
        }
        if (slot == 53) {
            if (page < totalPages - 1) { pages.put(player.getUniqueId(), page + 1); build(player); }
            return;
        }
        if (slot == 49) {
            player.closeInventory();
        }
    }

    public static boolean isWarningsLogGui(InventoryView view) {
        String title = LegacyComponentSerializer.legacyAmpersand().serialize(view.title());
        return title.contains(TITLE_MARKER);
    }

    public void cleanup(Player player) {
        pages.remove(player.getUniqueId());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void build(Player player) {
        List<AntiCheatManager.ViolationEntry> log =
                plugin.getAntiCheatManager().getRecentViolations(); // newest first
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        int totalPages = Math.max(1, (int) Math.ceil(log.size() / (double) PAGE_SIZE));

        Inventory inv = Bukkit.createInventory(null, 54,
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&8[&c" + TITLE_MARKER + "&8] &7(" + (page + 1) + "/" + totalPages + ")"));

        int start = page * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, log.size());

        for (int i = start; i < end; i++) {
            AntiCheatManager.ViolationEntry entry = log.get(i);
            String time = SDF.format(new Date(entry.timestamp()));
            ItemStack icon = new ItemStack(Material.PAPER);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&e" + entry.playerName()));
            meta.lore(List.of(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(
                            "&7Check: &c" + entry.check()),
                    LegacyComponentSerializer.legacyAmpersand().deserialize(
                            "&7Zeit: &f" + time)
            ));
            icon.setItemMeta(meta);
            inv.setItem(i - start, icon);
        }

        if (log.isEmpty()) {
            ItemStack none = new ItemStack(Material.GREEN_WOOL);
            ItemMeta m = none.getItemMeta();
            m.displayName(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&aKeine Verstöße vorhanden"));
            none.setItemMeta(m);
            inv.setItem(22, none);
        }

        if (page > 0) {
            inv.setItem(45, navItem(Material.ARROW, "&aVorherige Seite"));
        }

        ItemStack info = new ItemStack(Material.COMPARATOR);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&8[&bInfo&8]"));
        infoMeta.lore(List.of(
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&7Einträge gesamt: &e" + log.size()),
                LegacyComponentSerializer.legacyAmpersand().deserialize("&7LK: &eSchließen")
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        if (page < totalPages - 1) {
            inv.setItem(53, navItem(Material.ARROW, "&aNächste Seite"));
        }

        if (isWarningsLogGui(player.getOpenInventory())) {
            player.getOpenInventory().getTopInventory().setContents(inv.getContents());
        } else {
            player.openInventory(inv);
        }
    }

    private ItemStack navItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        item.setItemMeta(meta);
        return item;
    }
}
