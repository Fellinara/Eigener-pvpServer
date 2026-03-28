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

import java.util.*;

/**
 * GUI that lets admins toggle which permissions a rank has.
 *
 * <p>Open with: {@code /rank permissions <rankname>}
 *
 * <p>Layout – 6 rows (54 slots):
 * <ul>
 *   <li>Slots 0–44 (up to 45): permission icons</li>
 *   <li>Slot 45: previous page</li>
 *   <li>Slot 49: current rank info / close</li>
 *   <li>Slot 53: next page</li>
 * </ul>
 *
 * <p>Green wool = rank HAS permission; Red wool = rank does NOT have it.
 * Click any permission to toggle it.
 */
public class RankPermissionsGui {

    /** All plugin-defined permissions that can be toggled via GUI. */
    private static final List<String> AVAILABLE_PERMISSIONS = List.of(
            "klassenplugin.lobby",
            "klassenplugin.home",
            "klassenplugin.back",
            "klassenplugin.warp",
            "klassenplugin.setwarp",
            "klassenplugin.delwarp",
            "klassenplugin.tpa",
            "klassenplugin.kit",
            "klassenplugin.createkit",
            "klassenplugin.heal",
            "klassenplugin.feed",
            "klassenplugin.fly",
            "klassenplugin.msg",
            "klassenplugin.spawn",
            "klassenplugin.setspawn",
            "klassenplugin.admin",
            "klassenplugin.anticheat.admin",
            "klassenplugin.anticheat.bypass",
            "klassenplugin.anticheat.alert",
            "klassenplugin.rank.admin",
            "klassenplugin.economy",
            "klassenplugin.economy.pay",
            "klassenplugin.economy.admin",
            "klassenplugin.shop",
            "klassenplugin.shop.admin",
            "klassenplugin.order",
            "klassenplugin.order.admin",
            "klassenplugin.auction",
            "klassenplugin.auction.admin",
            "klassenplugin.changelog",
            "klassenplugin.changelog.admin",
            "klassenplugin.ally",
            "klassenplugin.ban",
            "klassenplugin.vanish",
            "klassenplugin.vanish.see",
            "klassenplugin.maintenance",
            "klassenplugin.maintenance.bypass"
    );

    private static final int PAGE_SIZE = 45;

    private final KlassenPlugin plugin;
    /** Rank name currently being edited, per viewer. */
    private final Map<UUID, String> openRanks = new HashMap<>();
    /** Current page (0-based) per viewer. */
    private final Map<UUID, Integer> pages = new HashMap<>();

    public RankPermissionsGui(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void open(Player player, String rankName) {
        openRanks.put(player.getUniqueId(), rankName);
        pages.put(player.getUniqueId(), 0);
        build(player);
    }

    public void handleClick(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        String rankName = openRanks.get(uuid);
        if (rankName == null) return;

        int currentPage = pages.getOrDefault(uuid, 0);
        int totalPages = Math.max(1, (int) Math.ceil(AVAILABLE_PERMISSIONS.size() / (double) PAGE_SIZE));

        if (slot == 45) {
            if (currentPage > 0) {
                pages.put(uuid, currentPage - 1);
                build(player);
            }
            return;
        }
        if (slot == 53) {
            if (currentPage < totalPages - 1) {
                pages.put(uuid, currentPage + 1);
                build(player);
            }
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        int permIndex = currentPage * PAGE_SIZE + slot;
        if (permIndex >= AVAILABLE_PERMISSIONS.size()) return;

        String perm = AVAILABLE_PERMISSIONS.get(permIndex);
        RankManager rm = plugin.getRankManager();
        List<String> currentPerms = new ArrayList<>(rm.getPermissions(rankName));
        if (currentPerms.contains(perm)) {
            rm.removePermission(rankName, perm);
            player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&c[-] Berechtigung &e" + perm + " &cvon Rang &e" + rankName + " &centfernt."));
        } else {
            rm.addPermission(rankName, perm);
            player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&a[+] Berechtigung &e" + perm + " &azum Rang &e" + rankName + " &ahinzugefügt."));
        }
        build(player);
    }

    public static boolean isRankGui(InventoryView view) {
        String title = LegacyComponentSerializer.legacyAmpersand().serialize(view.title());
        return title.contains("Rang-Berechtigungen");
    }

    public void cleanup(Player player) {
        openRanks.remove(player.getUniqueId());
        pages.remove(player.getUniqueId());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void build(Player player) {
        UUID uuid = player.getUniqueId();
        String rankName = openRanks.get(uuid);
        if (rankName == null) return;

        RankManager rm = plugin.getRankManager();
        List<String> currentPerms = new ArrayList<>(rm.getPermissions(rankName));
        int currentPage = pages.getOrDefault(uuid, 0);
        int totalPages = Math.max(1, (int) Math.ceil(AVAILABLE_PERMISSIONS.size() / (double) PAGE_SIZE));

        Inventory inv = Bukkit.createInventory(null, 54,
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&8[&bRang-Berechtigungen&8] &e" + rankName
                                + " &8(" + (currentPage + 1) + "/" + totalPages + ")"));

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, AVAILABLE_PERMISSIONS.size());

        for (int i = start; i < end; i++) {
            String perm = AVAILABLE_PERMISSIONS.get(i);
            boolean has = currentPerms.contains(perm);
            ItemStack icon = new ItemStack(has ? Material.GREEN_WOOL : Material.RED_WOOL);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize((has ? "&a" : "&c") + perm));
            meta.lore(List.of(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(
                            has ? "&7Klicken um zu entfernen" : "&7Klicken um hinzuzufügen")
            ));
            icon.setItemMeta(meta);
            inv.setItem(i - start, icon);
        }

        // Navigation buttons.
        if (currentPage > 0) {
            inv.setItem(45, navItem(Material.ARROW, "&aVorherige Seite", ""));
        }
        // Info / close.
        ItemStack info = new ItemStack(Material.NAME_TAG);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&eRang: &b" + rankName));
        infoMeta.lore(List.of(
                LegacyComponentSerializer.legacyAmpersand().deserialize("&7Klicken um zu schließen"),
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&7Berechtigungen: &e" + currentPerms.size())
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        if (currentPage < totalPages - 1) {
            inv.setItem(53, navItem(Material.ARROW, "&aNächste Seite", ""));
        }

        if (player.getOpenInventory().getTopInventory().getSize() == 54
                && isRankGui(player.getOpenInventory())) {
            player.getOpenInventory().getTopInventory().setContents(inv.getContents());
        } else {
            player.openInventory(inv);
        }
    }

    private ItemStack navItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        if (!lore.isEmpty()) {
            meta.lore(List.of(LegacyComponentSerializer.legacyAmpersand().deserialize(lore)));
        }
        item.setItemMeta(meta);
        return item;
    }
}
