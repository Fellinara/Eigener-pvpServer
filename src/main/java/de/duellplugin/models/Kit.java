package de.duellplugin.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

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

    public static Kit createNoDebuff() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION, 2);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 2);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION, 2);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 2);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 3);
        contents[1] = new ItemStack(Material.ENDER_PEARL, 16);

        ItemStack healPotion = createSplashPotion(PotionType.STRONG_HEALING, "§cHeilungstrank");
        for (int i = 2; i < 36; i++) {
            contents[i] = healPotion.clone();
        }

        return new Kit("nodebuff", "§c⚔ NoDebuff", Material.POTION,
                "§7Diamant-Rüstung, Heiltränke & Pearls", armor, contents);
    }

    public static Kit createDebuff() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION, 2);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 2);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION, 2);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 2);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 3);
        contents[1] = new ItemStack(Material.ENDER_PEARL, 16);

        ItemStack healPotion = createSplashPotion(PotionType.STRONG_HEALING, "§cHeilungstrank");
        ItemStack poisonPotion = createSplashPotion(PotionType.STRONG_POISON, "§2Gifttrank");
        ItemStack slowPotion = createSplashPotion(PotionType.STRONG_SLOWNESS, "§9Langsamkeitstrank");

        contents[2] = poisonPotion.clone();
        contents[3] = poisonPotion.clone();
        contents[4] = slowPotion.clone();
        contents[5] = slowPotion.clone();
        for (int i = 6; i < 36; i++) {
            contents[i] = healPotion.clone();
        }

        return new Kit("debuff", "§2☠ Debuff", Material.SPLASH_POTION,
                "§7Heiltränke + Gift & Langsamkeit", armor, contents);
    }

    public static Kit createClassic() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.IRON_HELMET), Enchantment.PROTECTION, 1);
        armor[2] = enchant(new ItemStack(Material.IRON_CHESTPLATE), Enchantment.PROTECTION, 1);
        armor[1] = enchant(new ItemStack(Material.IRON_LEGGINGS), Enchantment.PROTECTION, 1);
        armor[0] = enchant(new ItemStack(Material.IRON_BOOTS), Enchantment.PROTECTION, 1);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 1);
        contents[1] = enchant(new ItemStack(Material.BOW), Enchantment.POWER, 2);
        contents[2] = new ItemStack(Material.GOLDEN_APPLE, 8);
        contents[3] = new ItemStack(Material.ENDER_PEARL, 8);
        contents[4] = new ItemStack(Material.LAVA_BUCKET);
        contents[5] = new ItemStack(Material.WATER_BUCKET);
        contents[6] = new ItemStack(Material.COOKED_BEEF, 64);
        contents[9] = new ItemStack(Material.ARROW, 32);

        return new Kit("classic", "§6⚜ Classic", Material.DIAMOND_SWORD,
                "§7Eisen-Rüstung, Schwert, Bogen & Pearls", armor, contents);
    }

    public static Kit createGapple() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION, 4);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 4);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION, 4);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 4);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 4);
        contents[1] = new ItemStack(Material.GOLDEN_APPLE, 64);
        contents[2] = new ItemStack(Material.GOLDEN_APPLE, 64);
        contents[3] = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 3);
        contents[4] = new ItemStack(Material.COOKED_BEEF, 64);

        return new Kit("gapple", "§e🍎 Gapple", Material.GOLDEN_APPLE,
                "§7Volle Diamant-Rüstung & Goldene Äpfel", armor, contents);
    }

    public static Kit createBuildUHC() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION, 2);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 2);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION, 2);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 2);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 3);
        contents[1] = enchant(new ItemStack(Material.BOW), Enchantment.POWER, 3);
        contents[2] = new ItemStack(Material.GOLDEN_APPLE, 16);
        contents[3] = new ItemStack(Material.OAK_PLANKS, 64);
        contents[4] = new ItemStack(Material.COBBLESTONE, 64);
        contents[5] = new ItemStack(Material.LAVA_BUCKET);
        contents[6] = new ItemStack(Material.WATER_BUCKET);
        contents[7] = enchant(new ItemStack(Material.DIAMOND_AXE), Enchantment.EFFICIENCY, 2);
        contents[8] = new ItemStack(Material.COOKED_BEEF, 64);
        contents[9] = new ItemStack(Material.ARROW, 64);

        return new Kit("builduhc", "§9🏗 BuildUHC", Material.OAK_PLANKS,
                "§7Diamant-Rüstung, Bogen & Baumaterial", armor, contents);
    }

    public static Kit createCombo() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.IRON_HELMET), Enchantment.PROTECTION, 2);
        armor[2] = enchant(new ItemStack(Material.IRON_CHESTPLATE), Enchantment.PROTECTION, 2);
        armor[1] = enchant(new ItemStack(Material.IRON_LEGGINGS), Enchantment.PROTECTION, 2);
        armor[0] = enchant(new ItemStack(Material.IRON_BOOTS), Enchantment.PROTECTION, 2);

        ItemStack[] contents = new ItemStack[36];
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.addEnchant(Enchantment.SHARPNESS, 2, true);
            swordMeta.addEnchant(Enchantment.KNOCKBACK, 2, true);
            sword.setItemMeta(swordMeta);
        }
        contents[0] = sword;
        contents[1] = new ItemStack(Material.ENDER_PEARL, 16);

        ItemStack speedPotion = createDrinkablePotion(PotionType.STRONG_SWIFTNESS, "§bSpeed II");
        contents[2] = speedPotion.clone();
        contents[3] = speedPotion.clone();
        contents[4] = new ItemStack(Material.GOLDEN_APPLE, 8);
        contents[5] = new ItemStack(Material.COOKED_BEEF, 64);

        return new Kit("combo", "§b⚡ Combo", Material.FEATHER,
                "§7Eisen-Rüstung, Knockback-Schwert & Speed", armor, contents);
    }

    private static ItemStack createSplashPotion(PotionType type, String name) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(type);
            meta.setDisplayName(name);
            potion.setItemMeta(meta);
        }
        return potion;
    }

    private static ItemStack createDrinkablePotion(PotionType type, String name) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(type);
            meta.setDisplayName(name);
            potion.setItemMeta(meta);
        }
        return potion;
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
