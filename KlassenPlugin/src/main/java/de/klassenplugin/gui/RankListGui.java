package de.klassenplugin.gui;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.RankManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI that lists all configured ranks so an admin can pick one to edit.
 *
 * <p>Open with: {@code /rank gui}
 *
 * <p>Layout – 6 rows (54 slots):
 * <ul>
 *   <li>Slots 0–44: rank icons (NAME_TAG), up to 45 per page</li>
 *   <li>Slot 45: previous page (ARROW)</li>
 *   <li>Slot 49: page info (PAPER)</li>
 *   <li>Slot 53: next page (ARROW)</li>
 * </ul>
 *
 * <p>Clicking a rank icon opens {@link RankPermissionsGui} for that rank.
 */
public class RankListGui {

    private static final int PAGE_SIZE = 45;
    private static final String TITLE_KEY = "Rang-Verwaltung";

    private final KlassenPlugin plugin;
    /** Current page (0-based) per viewer. */
    private final Map<UUID, Integer> pages = new ConcurrentHashMap<>();

    public RankListGui(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void open(Player player) {
        pages.put(player.getUniqueId(), 0);
        build(player);
    }

    public void handleClick(Player player, int slot) {
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        List<String> ranks = sortedRankNames();
        int totalPages = Math.max(1, (int) Math.ceil(ranks.size() / (double) PAGE_SIZE));

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
            return;
        }

        int idx = page * PAGE_SIZE + slot;
        if (idx < 0 || idx >= ranks.size()) return;

        String rankName = ranks.get(idx);
        // Open the permissions GUI for the selected rank.
        plugin.getRankPermissionsGui().open(player, rankName);
    }

    public static boolean isRankListGui(InventoryView view) {
        String title = LegacyComponentSerializer.legacyAmpersand().serialize(view.title());
        return title.contains(TITLE_KEY);
    }

    public void cleanup(Player player) {
        pages.remove(player.getUniqueId());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void build(Player player) {
        RankManager rm = plugin.getRankManager();
        List<String> ranks = sortedRankNames();
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        int totalPages = Math.max(1, (int) Math.ceil(ranks.size() / (double) PAGE_SIZE));

        Inventory inv = Bukkit.createInventory(null, 54,
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&8[&b" + TITLE_KEY + "&8] &8(" + (page + 1) + "/" + totalPages + ")"));

        int start = page * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, ranks.size());
        for (int i = start; i < end; i++) {
            String rankName = ranks.get(i);
            String prefix   = rm.getPrefix(rankName);
            int permCount   = rm.getPermissions(rankName).size();

            ItemStack icon = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&e" + rankName));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&7Prefix: &r" + (prefix.isEmpty() ? "&7(leer)" : prefix)));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&7Berechtigungen: &e" + permCount));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&aKlicken zum Bearbeiten"));
            meta.lore(lore);
            icon.setItemMeta(meta);
            inv.setItem(i - start, icon);
        }

        if (ranks.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta m = empty.getItemMeta();
            m.displayName(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&cKeine Ränge vorhanden"));
            m.lore(List.of(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&7Benutze &e/rank create <Name> &7um einen Rang zu erstellen.")));
            empty.setItemMeta(m);
            inv.setItem(22, empty);
        }

        if (page > 0) {
            inv.setItem(45, navItem(Material.ARROW, "&aVorherige Seite"));
        }

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&eSeite &b" + (page + 1) + " &evon &b" + totalPages));
        infoMeta.lore(List.of(
                LegacyComponentSerializer.legacyAmpersand().deserialize("&7Klicken zum Schließen"),
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&7Ränge gesamt: &e" + ranks.size())
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        if (page < totalPages - 1) {
            inv.setItem(53, navItem(Material.ARROW, "&aNächste Seite"));
        }

        if (isRankListGui(player.getOpenInventory())) {
            player.getOpenInventory().getTopInventory().setContents(inv.getContents());
        } else {
            player.openInventory(inv);
        }
    }

    private List<String> sortedRankNames() {
        List<String> list = new ArrayList<>(plugin.getRankManager().getRankNames());
        list.sort(String::compareToIgnoreCase);
        return list;
    }

    private ItemStack navItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        item.setItemMeta(meta);
        return item;
    }
}
