package de.duellplugin.gui;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;

public class ArenaGUI {

    private final DuellPlugin plugin;

    public ArenaGUI(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        int arenaCount = plugin.getArenaManager().getAllArenas().size();
        int rows = Math.max(3, Math.min(6, (int) Math.ceil((double) arenaCount / 7.0) + 2));
        Inventory inv = Bukkit.createInventory(null, rows * 9, "§a⚐ Arena-Auswahl");

        int slot = 10;
        for (Arena arena : plugin.getArenaManager().getAllArenas()) {
            Material material;
            String status;

            if (!arena.isReady()) {
                material = Material.RED_STAINED_GLASS_PANE;
                status = "§c✘ Nicht eingerichtet";
            } else if (arena.isInUse()) {
                material = Material.ORANGE_STAINED_GLASS_PANE;
                status = "§6⚔ Besetzt";
            } else {
                material = Material.LIME_STAINED_GLASS_PANE;
                status = "§a✔ Verfügbar";
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e" + arena.getName().toUpperCase());
                meta.setLore(Arrays.asList(
                        "§7Status: " + status,
                        "",
                        arena.isReady() && !arena.isInUse() ? "§aKlicke zum Beitreten!" : "§7Nicht verfügbar"
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
            if (slot % 9 == 8) slot += 3;
        }

        if (arenaCount == 0) {
            ItemStack noArenas = new ItemStack(Material.BARRIER);
            ItemMeta meta = noArenas.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§cKeine Arenen vorhanden");
                meta.setLore(Collections.singletonList("§7Ein Admin muss zuerst Arenen erstellen."));
                noArenas.setItemMeta(meta);
            }
            inv.setItem(13, noArenas);
        }

        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        for (int i = 0; i < rows * 9; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }

        player.openInventory(inv);
    }
}
