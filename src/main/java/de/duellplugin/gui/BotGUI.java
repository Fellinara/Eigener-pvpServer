package de.duellplugin.gui;

import de.duellplugin.DuellPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;

public class BotGUI {

    private final DuellPlugin plugin;

    public BotGUI(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§c☠ Bot-Kampf - Level wählen");

        inv.setItem(10, createBotItem(1, Material.LEATHER_HELMET,
                "§7⚔ Level 1-10", "§7Anfänger", "§aEinfach"));
        inv.setItem(11, createBotItem(10, Material.LEATHER_CHESTPLATE,
                "§7⚔ Level 10", "§7Anfänger+", "§aEinfach"));
        inv.setItem(12, createBotItem(20, Material.CHAINMAIL_HELMET,
                "§a⚔ Level 20", "§aLehrling", "§eMittel"));
        inv.setItem(13, createBotItem(30, Material.CHAINMAIL_CHESTPLATE,
                "§a⚔ Level 30", "§aLehrling+", "§eMittel"));
        inv.setItem(14, createBotItem(40, Material.IRON_HELMET,
                "§e⚔ Level 40", "§eKämpfer", "§6Schwer"));
        inv.setItem(15, createBotItem(50, Material.IRON_CHESTPLATE,
                "§e⚔ Level 50", "§eKämpfer+", "§6Schwer"));
        inv.setItem(16, createBotItem(60, Material.DIAMOND_HELMET,
                "§6⚔ Level 60", "§6Veteran", "§cSehr Schwer"));

        inv.setItem(19, createBotItem(70, Material.DIAMOND_CHESTPLATE,
                "§c⚔ Level 70", "§cElite", "§4Extrem"));
        inv.setItem(20, createBotItem(80, Material.DIAMOND_SWORD,
                "§5⚔ Level 80", "§5Meister", "§4Extrem"));
        inv.setItem(21, createBotItem(90, Material.NETHERITE_HELMET,
                "§d⚔ Level 90", "§dGroßmeister", "§4§lAlbtraum"));
        inv.setItem(22, createBotItem(100, Material.NETHERITE_SWORD,
                "§4⚔ Level 100", "§4§lLegende", "§4§l☠ UNMÖGLICH"));

        ItemStack customLevel = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta customMeta = customLevel.getItemMeta();
        if (customMeta != null) {
            customMeta.setDisplayName("§b✎ Custom Level");
            customMeta.setLore(Arrays.asList(
                    "§7Wähle ein beliebiges Level",
                    "§7von 1 bis 100",
                    "",
                    "§eNutze: §f/bot <level>"
            ));
            customLevel.setItemMeta(customMeta);
        }
        inv.setItem(25, customLevel);

        var stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());
        ItemStack statsItem = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsItem.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName("§b📊 Deine Bot-Statistiken");
            statsMeta.setLore(Arrays.asList(
                    "§7Bot-Siege: §a" + stats.getBotWins(),
                    "§7Bot-Niederlagen: §c" + stats.getBotLosses(),
                    "§7Höchstes Level: §6" + stats.getHighestBotLevel()
            ));
            statsItem.setItemMeta(statsMeta);
        }
        inv.setItem(49, statsItem);

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createBotItem(int level, Material material, String name, String rank, String difficulty) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(
                    "§7Rang: " + rank,
                    "§7Schwierigkeit: " + difficulty,
                    "",
                    "§eKlicke für Level " + level + "!"
            ));
            item.setItemMeta(meta);
        }
        return item;
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

    public static int getLevelFromSlot(int slot) {
        return switch (slot) {
            case 10 -> 1;
            case 11 -> 10;
            case 12 -> 20;
            case 13 -> 30;
            case 14 -> 40;
            case 15 -> 50;
            case 16 -> 60;
            case 19 -> 70;
            case 20 -> 80;
            case 21 -> 90;
            case 22 -> 100;
            default -> -1;
        };
    }
}
