package de.duellplugin.gui;

import de.duellplugin.DuellPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.Collections;

public class DuellGUI {

    private final DuellPlugin plugin;

    public DuellGUI(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        int onlineCount = Bukkit.getOnlinePlayers().size() - 1;
        int rows = Math.max(3, Math.min(6, (int) Math.ceil((double) (onlineCount + 1) / 9.0) + 1));
        Inventory inv = Bukkit.createInventory(null, rows * 9, "§6⚔ Duell - Spieler auswählen");

        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player)) continue;
            if (slot >= (rows * 9 - 9)) break;

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName("§e" + target.getName());

                var stats = plugin.getStatsManager().getOrCreateStats(target.getUniqueId(), target.getName());
                meta.setLore(Arrays.asList(
                        "§7ELO: §f" + stats.getElo(),
                        "§7Siege: §a" + stats.getWins(),
                        "§7Niederlagen: §c" + stats.getLosses(),
                        "",
                        "§aKlicke um herauszufordern!"
                ));
                skull.setItemMeta(meta);
            }
            inv.setItem(slot++, skull);
        }

        if (onlineCount == 0) {
            ItemStack noPlayers = createItem(Material.BARRIER, "§cKein Spieler online",
                    "§7Warte auf andere Spieler...");
            inv.setItem(13, noPlayers);
        }

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = rows * 9 - 9; i < rows * 9; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(Collections.singletonList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
