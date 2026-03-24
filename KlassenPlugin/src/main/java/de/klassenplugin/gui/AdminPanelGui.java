package de.klassenplugin.gui;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.AntiCheatManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Central admin control panel.
 *
 * <p>Open with: {@code /adminpanel}
 *
 * <p>Layout (3 rows / 27 slots):
 * <ul>
 *   <li>Slot 10 – Violations overview → opens {@link ViolationsGui}</li>
 *   <li>Slot 12 – Recent warnings log → opens {@link WarningsLogGui}</li>
 *   <li>Slot 14 – Banned players list (info only)</li>
 *   <li>Slot 16 – Scoreboard settings info</li>
 * </ul>
 */
public class AdminPanelGui {

    private static final String TITLE = "&8[&4Admin-Panel&8]";
    private static final String TITLE_MARKER = "Admin-Panel";

    private final KlassenPlugin plugin;

    public AdminPanelGui(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
                LegacyComponentSerializer.legacyAmpersand().deserialize(TITLE));

        // Slot 10 – Player violations
        inv.setItem(10, buildIcon(Material.PLAYER_HEAD, player,
                "&c&lVerstöße &7(AntiCheat)",
                List.of("&7Zeigt alle Online-Spieler und",
                        "&7ihre aktuellen AntiCheat-Verstöße.",
                        "",
                        "&eKlicken zum Öffnen")));

        // Slot 12 – Recent warnings log
        int logSize = plugin.getAntiCheatManager().getRecentViolations().size();
        inv.setItem(12, buildSimpleIcon(Material.WRITABLE_BOOK,
                "&e&lVerwarnungs-Log",
                List.of("&7Die letzten " + logSize + " erkannten Verstöße.",
                        "&7Enthält Spieler, Check und Uhrzeit.",
                        "",
                        "&eKlicken zum Öffnen")));

        // Slot 14 – Ban list
        int banCount = Bukkit.getBanList(BanList.Type.NAME).getBanEntries().size();
        inv.setItem(14, buildSimpleIcon(Material.BARRIER,
                "&4&lBan-Liste",
                List.of("&7Aktuell gebannte Spieler: &c" + banCount,
                        "",
                        "&eKlicken für Details")));

        // Slot 16 – Scoreboard info
        boolean sbEnabled = plugin.getConfig().getBoolean("scoreboard.enabled", true);
        String sbTitle = plugin.getConfig().getString("scoreboard.title", "&6&lKlassenPlugin");
        inv.setItem(16, buildSimpleIcon(Material.COMPARATOR,
                "&b&lScoreboard",
                List.of("&7Status: " + (sbEnabled ? "&aAktiviert" : "&cDeaktiviert"),
                        "&7Titel: &r" + sbTitle,
                        "",
                        "&7Nutze &e/scoreboard &7zum Ändern.")));

        player.openInventory(inv);
    }

    public void handleClick(Player player, int slot) {
        switch (slot) {
            case 10 -> plugin.getViolationsGui().open(player);
            case 12 -> plugin.getWarningsLogGui().open(player);
            case 14 -> openBanList(player);
            default -> { /* ignore */ }
        }
    }

    public static boolean isAdminPanelGui(InventoryView view) {
        String title = LegacyComponentSerializer.legacyAmpersand().serialize(view.title());
        return title.contains(TITLE_MARKER);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Opens a simple text-only ban-list overview in chat (no extra GUI needed). */
    private void openBanList(Player player) {
        player.closeInventory();
        @SuppressWarnings("deprecation")
        Set<? extends org.bukkit.BanEntry<?>> entries =
                Bukkit.getBanList(BanList.Type.NAME).getBanEntries();
        if (entries.isEmpty()) {
            player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&8[&4Admin&8] &eKein Spieler ist derzeit gebannt."));
            return;
        }
        player.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&4Admin&8] &eGebannte Spieler (&c" + entries.size() + "&e):"));
        int shown = 0;
        for (org.bukkit.BanEntry<?> e : entries) {
            if (shown++ >= 20) {
                player.sendMessage(KlassenPlugin.colorizeComponent(
                        "  &8… und " + (entries.size() - 20) + " weitere. Nutze /banlist."));
                break;
            }
            String reason = e.getReason() != null ? e.getReason() : "Kein Grund";
            player.sendMessage(KlassenPlugin.colorizeComponent(
                    "  &c" + e.getTarget() + " &8| &7" + reason));
        }
    }

    private ItemStack buildIcon(Material mat, Player owner, String name, List<String> loreStr) {
        ItemStack item = mat == Material.PLAYER_HEAD ? new ItemStack(mat) : new ItemStack(mat);
        if (mat == Material.PLAYER_HEAD) {
            SkullMeta sm = (SkullMeta) item.getItemMeta();
            sm.setOwningPlayer(owner);
            sm.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
            sm.lore(loreComponents(loreStr));
            item.setItemMeta(sm);
        } else {
            buildSimpleMeta(item, name, loreStr);
        }
        return item;
    }

    private ItemStack buildSimpleIcon(Material mat, String name, List<String> loreStr) {
        ItemStack item = new ItemStack(mat);
        buildSimpleMeta(item, name, loreStr);
        return item;
    }

    private void buildSimpleMeta(ItemStack item, String name, List<String> loreStr) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        meta.lore(loreComponents(loreStr));
        item.setItemMeta(meta);
    }

    private List<net.kyori.adventure.text.Component> loreComponents(List<String> lines) {
        List<net.kyori.adventure.text.Component> list = new ArrayList<>();
        for (String l : lines) {
            list.add(LegacyComponentSerializer.legacyAmpersand().deserialize(l));
        }
        return list;
    }
}
