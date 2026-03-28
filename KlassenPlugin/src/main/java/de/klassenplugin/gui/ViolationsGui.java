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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * GUI showing all online players and their AntiCheat violation counts.
 *
 * <p>Open with: {@code /anticheat violations} (no player argument)
 *
 * <p>Click on a player head to reset their violations.
 * Shift-click to kick the player.
 */
public class ViolationsGui {

    private final KlassenPlugin plugin;
    private final Map<UUID, Integer> pages = new HashMap<>();

    private static final int PAGE_SIZE = 45;

    public ViolationsGui(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void open(Player admin) {
        pages.put(admin.getUniqueId(), 0);
        build(admin);
    }

    public void handleClick(Player admin, int slot, boolean shift) {
        UUID adminUuid = admin.getUniqueId();
        int currentPage = pages.getOrDefault(adminUuid, 0);
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        int totalPages = Math.max(1, (int) Math.ceil(online.size() / (double) PAGE_SIZE));

        if (slot == 45) {
            if (currentPage > 0) { pages.put(adminUuid, currentPage - 1); build(admin); }
            return;
        }
        if (slot == 53) {
            if (currentPage < totalPages - 1) { pages.put(adminUuid, currentPage + 1); build(admin); }
            return;
        }
        if (slot == 49) {
            admin.closeInventory();
            return;
        }

        int idx = currentPage * PAGE_SIZE + slot;
        if (idx >= online.size()) return;
        Player target = online.get(idx);

        AntiCheatManager ac = plugin.getAntiCheatManager();
        if (shift) {
            target.kick(KlassenPlugin.colorizeComponent("&cDu wurdest von einem Admin gekickt."));
            admin.sendMessage(KlassenPlugin.colorizeComponent(
                    "&8[&bAntiCheat&8] &e" + target.getName() + " &ewurde gekickt!"));
        } else {
            ac.resetViolations(target.getUniqueId());
            admin.sendMessage(KlassenPlugin.colorizeComponent(
                    "&8[&bAntiCheat&8] &eVerstöße von &b" + target.getName() + " &ezurückgesetzt!"));
        }
        build(admin);
    }

    public static boolean isViolationsGui(InventoryView view) {
        String title = LegacyComponentSerializer.legacyAmpersand().serialize(view.title());
        return title.contains("AntiCheat-Verstö");
    }

    public void cleanup(Player player) {
        pages.remove(player.getUniqueId());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void build(Player admin) {
        UUID adminUuid = admin.getUniqueId();
        int currentPage = pages.getOrDefault(adminUuid, 0);
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        int totalPages = Math.max(1, (int) Math.ceil(online.size() / (double) PAGE_SIZE));
        AntiCheatManager ac = plugin.getAntiCheatManager();

        Inventory inv = Bukkit.createInventory(null, 54,
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&8[&cAntiCheat-Verstöße&8] &7(" + (currentPage + 1) + "/" + totalPages + ")"));

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, online.size());

        for (int i = start; i < end; i++) {
            Player target = online.get(i);
            int violations = ac.getViolations(target.getUniqueId());

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize((violations > 0 ? "&c" : "&a") + target.getName()));
            meta.lore(List.of(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(
                            "&7Verstöße: " + (violations > 0 ? "&c" : "&a") + violations),
                    LegacyComponentSerializer.legacyAmpersand().deserialize(""),
                    LegacyComponentSerializer.legacyAmpersand().deserialize(
                            "&7LK: &eVerstöße zurücksetzen"),
                    LegacyComponentSerializer.legacyAmpersand().deserialize(
                            "&7Shift+LK: &cSpieler kicken")
            ));
            head.setItemMeta(meta);
            inv.setItem(i - start, head);
        }

        // Navigation.
        if (currentPage > 0) {
            inv.setItem(45, navItem(Material.ARROW, "&aVorherige Seite"));
        }
        // Info / close.
        ItemStack info = new ItemStack(Material.COMPARATOR);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&8[&bInfo&8]"));
        infoMeta.lore(List.of(
                LegacyComponentSerializer.legacyAmpersand().deserialize("&7Online: &e" + online.size()),
                LegacyComponentSerializer.legacyAmpersand().deserialize("&7LK: &eSchließen")
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        if (currentPage < totalPages - 1) {
            inv.setItem(53, navItem(Material.ARROW, "&aNächste Seite"));
        }

        if (admin.getOpenInventory().getTopInventory().getSize() == 54
                && isViolationsGui(admin.getOpenInventory())) {
            admin.getOpenInventory().getTopInventory().setContents(inv.getContents());
        } else {
            admin.openInventory(inv);
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
