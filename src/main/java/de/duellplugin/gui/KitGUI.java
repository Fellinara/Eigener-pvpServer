package de.duellplugin.gui;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.CustomKit;
import de.duellplugin.models.Kit;
import de.duellplugin.models.PlayerStats;
import de.duellplugin.models.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KitGUI {

    private final DuellPlugin plugin;

    public KitGUI(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§e🎒 Kit-Auswahl");

        PlayerStats stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());
        String selectedKit = stats.getSelectedKit();

        // Build ordered kit list: personal order first, then remaining kits
        List<Kit> allKits = new ArrayList<>(plugin.getKitManager().getAllKits());
        List<String> kitOrder = stats.getKitOrder();
        List<Kit> ordered = new ArrayList<>();
        for (String kitName : kitOrder) {
            Kit found = null;
            for (Kit k : allKits) {
                if (k.getName().equals(kitName)) {
                    found = k;
                    break;
                }
            }
            if (found != null) {
                ordered.add(found);
                allKits.remove(found);
            }
        }
        ordered.addAll(allKits);

        // Add custom kits for VIP-ranked players
        Rank playerRank = stats.getRank();
        if (playerRank.isStaff() || playerRank.ordinal() >= Rank.VIP.ordinal()) {
            for (CustomKit ck : stats.getCustomKits().values()) {
                ordered.add(Kit.fromCustom(ck));
            }
        }

        // Place kits in the middle rows (slots 10-16 and 19-25)
        int[] kitSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < ordered.size() && i < kitSlots.length; i++) {
            Kit kit = ordered.get(i);
            boolean isSelected = kit.getName().equalsIgnoreCase(selectedKit);

            ItemStack item = new ItemStack(kit.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((isSelected ? "§a✔ " : "§e") + kit.getDisplayName());
                meta.setLore(Arrays.asList(
                        kit.getDescription(),
                        "",
                        isSelected ? "§a§l✔ Ausgewählt" : "§eKlick §7= auswählen",
                        "§6Shift+Klick §7= weiter oben sortieren",
                        "§cRechtsklick §7= weiter unten sortieren"
                ));
                if (isSelected) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
                item.setItemMeta(meta);
            }
            inv.setItem(kitSlots[i], item);
        }

        // Border glass panes
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }

        player.openInventory(inv);
    }
}
