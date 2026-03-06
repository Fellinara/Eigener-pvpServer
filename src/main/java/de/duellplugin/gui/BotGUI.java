package de.duellplugin.gui;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class BotGUI {

    private final DuellPlugin plugin;

    public BotGUI(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§c☠ Bot-Kampf");

        PlayerStats stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());
        int rating = stats.getBotRating();

        // Slot 11 – current bot rating info
        ItemStack ratingItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta ratingMeta = ratingItem.getItemMeta();
        if (ratingMeta != null) {
            ratingMeta.setDisplayName("§6⚡ Deine Bot-Stärke: §f" + rating);
            ratingMeta.setLore(Arrays.asList(
                    "§7Der Bot passt sich deiner Stärke an.",
                    "§aGewinn: §f+4 Stärke",
                    "§cVerliere: §f-3 Stärke",
                    "",
                    "§7Aktuell: " + getRatingBar(rating)
            ));
            ratingItem.setItemMeta(ratingMeta);
        }
        inv.setItem(11, ratingItem);

        // Slot 13 – start fight button
        ItemStack startItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta startMeta = startItem.getItemMeta();
        if (startMeta != null) {
            startMeta.setDisplayName("§a▶ Bot-Kampf starten");
            startMeta.setLore(Arrays.asList(
                    "§7Kit: §6" + stats.getSelectedKit(),
                    "§7Bot-Stärke: §f" + rating,
                    "",
                    "§eKlicke zum Starten!"
            ));
            startItem.setItemMeta(startMeta);
        }
        inv.setItem(13, startItem);

        // Slot 15 – stats
        ItemStack statsItem = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsItem.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName("§b📊 Bot-Statistiken");
            statsMeta.setLore(Arrays.asList(
                    "§7Siege: §a" + stats.getBotWins(),
                    "§7Niederlagen: §c" + stats.getBotLosses(),
                    "§7Höchste Stärke: §6" + stats.getHighestBotLevel()
            ));
            statsItem.setItemMeta(statsMeta);
        }
        inv.setItem(15, statsItem);

        // Fill borders
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }

        player.openInventory(inv);
    }

    private String getRatingBar(int rating) {
        int filled = rating / 10;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "§a█" : "§7█");
        }
        return bar.toString();
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Returns true if the clicked slot is the "start fight" button. */
    public static boolean isStartSlot(int slot) {
        return slot == 13;
    }
}
