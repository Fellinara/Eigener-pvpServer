package de.duellplugin.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class Kit {

    private final String name;
    private final String displayName;
    private final Material icon;
    private final ItemStack[] armor;
    private final ItemStack[] contents;
    private final String description;

    public Kit(String name, String displayName, Material icon, String description,
               ItemStack[] armor, ItemStack[] contents) {
        this.name = name;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.armor = armor;
        this.contents = contents;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public ItemStack[] getArmor() {
        return armor.clone();
    }

    public ItemStack[] getContents() {
        return contents.clone();
    }

    public static Kit createSwordsman() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.IRON_HELMET), Enchantment.PROTECTION, 2);
        armor[2] = enchant(new ItemStack(Material.IRON_CHESTPLATE), Enchantment.PROTECTION, 2);
        armor[1] = enchant(new ItemStack(Material.IRON_LEGGINGS), Enchantment.PROTECTION, 2);
        armor[0] = enchant(new ItemStack(Material.IRON_BOOTS), Enchantment.PROTECTION, 2);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 3);
        contents[1] = new ItemStack(Material.GOLDEN_APPLE, 3);
        contents[2] = new ItemStack(Material.COOKED_BEEF, 16);

        return new Kit("swordsman", "§c⚔ Schwertkämpfer", Material.DIAMOND_SWORD,
                "§7Klassischer Nahkampf mit Schwert und Rüstung", armor, contents);
    }

    public static Kit createArcher() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.LEATHER_HELMET), Enchantment.PROTECTION, 3);
        armor[2] = enchant(new ItemStack(Material.LEATHER_CHESTPLATE), Enchantment.PROTECTION, 3);
        armor[1] = enchant(new ItemStack(Material.LEATHER_LEGGINGS), Enchantment.PROTECTION, 3);
        armor[0] = enchant(new ItemStack(Material.LEATHER_BOOTS), Enchantment.PROTECTION, 3);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.BOW), Enchantment.POWER, 3);
        contents[1] = new ItemStack(Material.IRON_SWORD);
        contents[2] = new ItemStack(Material.GOLDEN_APPLE, 2);
        contents[9] = new ItemStack(Material.ARROW, 64);

        return new Kit("archer", "§a🏹 Bogenschütze", Material.BOW,
                "§7Fernkampf-Spezialist mit Bogen und Pfeilen", armor, contents);
    }

    public static Kit createTank() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION, 4);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 4);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION, 4);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 4);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = new ItemStack(Material.IRON_SWORD);
        contents[1] = new ItemStack(Material.SHIELD);
        contents[2] = new ItemStack(Material.GOLDEN_APPLE, 5);
        contents[3] = new ItemStack(Material.COOKED_BEEF, 32);

        return new Kit("tank", "§9🛡 Tank", Material.DIAMOND_CHESTPLATE,
                "§7Maximale Rüstung und Ausdauer", armor, contents);
    }

    public static Kit createBerserker() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.CHAINMAIL_HELMET), Enchantment.PROTECTION, 1);
        armor[2] = enchant(new ItemStack(Material.CHAINMAIL_CHESTPLATE), Enchantment.PROTECTION, 1);
        armor[1] = enchant(new ItemStack(Material.CHAINMAIL_LEGGINGS), Enchantment.PROTECTION, 1);
        armor[0] = enchant(new ItemStack(Material.CHAINMAIL_BOOTS), Enchantment.PROTECTION, 1);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.NETHERITE_AXE), Enchantment.SHARPNESS, 5);
        contents[1] = new ItemStack(Material.GOLDEN_APPLE, 2);

        return new Kit("berserker", "§4🔥 Berserker", Material.NETHERITE_AXE,
                "§7Maximaler Schaden, wenig Rüstung", armor, contents);
    }

    public static Kit createAlchemist() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.GOLDEN_HELMET), Enchantment.PROTECTION, 3);
        armor[2] = enchant(new ItemStack(Material.GOLDEN_CHESTPLATE), Enchantment.PROTECTION, 3);
        armor[1] = enchant(new ItemStack(Material.GOLDEN_LEGGINGS), Enchantment.PROTECTION, 3);
        armor[0] = enchant(new ItemStack(Material.GOLDEN_BOOTS), Enchantment.PROTECTION, 3);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = new ItemStack(Material.IRON_SWORD);
        contents[1] = new ItemStack(Material.SPLASH_POTION, 3);
        contents[2] = new ItemStack(Material.SPLASH_POTION, 3);
        contents[3] = new ItemStack(Material.GOLDEN_APPLE, 4);

        return new Kit("alchemist", "§5⚗ Alchemist", Material.BREWING_STAND,
                "§7Tränke und goldene Ausrüstung", armor, contents);
    }

    public static Kit createKnight() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.IRON_HELMET), Enchantment.PROTECTION, 3);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 3);
        armor[1] = enchant(new ItemStack(Material.IRON_LEGGINGS), Enchantment.PROTECTION, 3);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 3);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.IRON_SWORD), Enchantment.SHARPNESS, 2);
        contents[1] = new ItemStack(Material.SHIELD);
        contents[2] = new ItemStack(Material.CROSSBOW);
        contents[3] = new ItemStack(Material.GOLDEN_APPLE, 3);
        contents[9] = new ItemStack(Material.ARROW, 32);

        return new Kit("knight", "§6⚜ Ritter", Material.IRON_SWORD,
                "§7Ausgewogener Kämpfer mit Schild", armor, contents);
    }

    private static ItemStack enchant(ItemStack item, Enchantment enchantment, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
            item.setItemMeta(meta);
        }
        return item;
    }
}
