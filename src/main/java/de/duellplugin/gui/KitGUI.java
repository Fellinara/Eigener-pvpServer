package de.duellplugin.gui;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Kit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;

public class KitGUI {

    private final DuellPlugin plugin;

    public KitGUI(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§e🎒 Kit-Auswahl");

        var stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());
        String selectedKit = stats.getSelectedKit();

        int slot = 10;
        for (Kit kit : plugin.getKitManager().getAllKits()) {
            boolean isSelected = kit.getName().equalsIgnoreCase(selectedKit);

            ItemStack item = new ItemStack(kit.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((isSelected ? "§a✔ " : "§e") + kit.getDisplayName());
                meta.setLore(Arrays.asList(
                        kit.getDescription(),
                        "",
                        isSelected ? "§a§l✔ Ausgewählt" : "§eKlicke zum Auswählen!"
                ));
                if (isSelected) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
            if (slot == 13) slot = 14;
        }

        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }

        player.openInventory(inv);
    }
}
