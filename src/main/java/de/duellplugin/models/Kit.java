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
    /** Optional item placed in the offhand slot; null means a shield is used by default. */
    private ItemStack offHandItem;
    /** If true, this kit can only be used in crystal arenas, and crystal arenas only allow this kit. */
    private boolean crystalOnly;
    /** If true, no item is placed in the offhand slot (not even the default shield). */
    private boolean noOffHand;

    public Kit(String name, String displayName, Material icon, String description,
               ItemStack[] armor, ItemStack[] contents) {
        this.name = name;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.armor = armor;
        this.contents = contents;
        this.offHandItem = null;
        this.crystalOnly = false;
    }

    /** Sets a custom offhand item for this kit (e.g. a totem). Returns {@code this} for chaining. */
    public Kit withOffHand(ItemStack item) {
        this.offHandItem = item;
        return this;
    }

    /** Marks this kit as crystal-only (playable only in crystal arenas). Returns {@code this} for chaining. */
    public Kit withCrystalOnly() {
        this.crystalOnly = true;
        return this;
    }

    /** Returns true if this kit requires a dedicated crystal arena. */
    public boolean isCrystalOnly() {
        return crystalOnly;
    }

    /** Marks this kit as having no offhand item (not even the default shield). Returns {@code this} for chaining. */
    public Kit withNoOffHand() {
        this.noOffHand = true;
        return this;
    }

    /** Returns true if this kit should have nothing in the offhand slot. */
    public boolean isNoOffHand() {
        return noOffHand;
    }

    /** Returns the kit's offhand item, or {@code null} if the default shield should be used. */
    public ItemStack getOffHandItem() {
        return offHandItem != null ? offHandItem.clone() : null;
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
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION, 4);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 4);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION, 4);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 4);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 4);
        contents[1] = enchant(new ItemStack(Material.DIAMOND_AXE), Enchantment.SHARPNESS, 2);
        contents[2] = new ItemStack(Material.ENDER_PEARL, 16);

        ItemStack healPotion = createSplashPotion(PotionType.STRONG_HEALING, "§cHeilungstrank");
        for (int i = 3; i < 36; i++) {
            contents[i] = healPotion.clone();
        }

        return new Kit("nodebuff", "§c⚔ NoDebuff", Material.POTION,
                "§7Diamant-Rüstung, Heiltränke & Pearls", armor, contents);
    }

    public static Kit createDebuff() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION, 4);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 4);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION, 4);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 4);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 4);
        contents[1] = enchant(new ItemStack(Material.DIAMOND_AXE), Enchantment.SHARPNESS, 2);
        contents[2] = new ItemStack(Material.ENDER_PEARL, 16);

        ItemStack healPotion = createSplashPotion(PotionType.STRONG_HEALING, "§cHeilungstrank");
        ItemStack poisonPotion = createSplashPotion(PotionType.STRONG_POISON, "§2Gifttrank");
        ItemStack slowPotion = createSplashPotion(PotionType.STRONG_SLOWNESS, "§9Langsamkeitstrank");

        contents[3] = poisonPotion.clone();
        contents[4] = poisonPotion.clone();
        contents[5] = slowPotion.clone();
        contents[6] = slowPotion.clone();
        for (int i = 7; i < 36; i++) {
            contents[i] = healPotion.clone();
        }

        return new Kit("debuff", "§2☠ Debuff", Material.SPLASH_POTION,
                "§7Heiltränke + Gift & Langsamkeit", armor, contents);
    }

    public static Kit createClassic() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION, 2);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 2);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION, 2);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 2);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 3);
        contents[1] = enchant(new ItemStack(Material.BOW), Enchantment.POWER, 2);
        contents[2] = new ItemStack(Material.GOLDEN_APPLE, 8);
        contents[3] = new ItemStack(Material.ENDER_PEARL, 16);
        contents[4] = new ItemStack(Material.COBWEB, 16);
        contents[5] = new ItemStack(Material.LAVA_BUCKET);
        contents[6] = new ItemStack(Material.WATER_BUCKET);
        contents[7] = new ItemStack(Material.COOKED_BEEF, 64);
        contents[8] = enchant(new ItemStack(Material.DIAMOND_AXE), Enchantment.SHARPNESS, 2);
        contents[9] = new ItemStack(Material.ARROW, 32);

        return new Kit("classic", "§6⚜ Classic", Material.DIAMOND_SWORD,
                "§7Diamant-Rüstung, Schwert, Bogen, Axt & Cobwebs", armor, contents);
    }

    public static Kit createGapple() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION, 4);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 4);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION, 4);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 4);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 4);
        contents[1] = enchant(new ItemStack(Material.DIAMOND_AXE), Enchantment.SHARPNESS, 2);
        contents[2] = new ItemStack(Material.GOLDEN_APPLE, 64);
        contents[3] = new ItemStack(Material.GOLDEN_APPLE, 64);
        contents[4] = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 3);
        contents[5] = new ItemStack(Material.COOKED_BEEF, 64);

        return new Kit("gapple", "§e🍎 Gapple", Material.GOLDEN_APPLE,
                "§7Volle Diamant-Rüstung, Axt & Goldene Äpfel", armor, contents);
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
                "§7Diamant-Rüstung, Bogen, Axt & Baumaterial", armor, contents);
    }

    public static Kit createUHC() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION, 3);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 3);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION, 3);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 3);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 3);
        contents[1] = enchant(new ItemStack(Material.BOW), Enchantment.POWER, 1);
        contents[2] = enchant(new ItemStack(Material.CROSSBOW), Enchantment.PIERCING, 1);
        contents[3] = new ItemStack(Material.GOLDEN_APPLE, 8);
        contents[4] = new ItemStack(Material.LAVA_BUCKET);
        contents[5] = new ItemStack(Material.LAVA_BUCKET);
        contents[6] = new ItemStack(Material.WATER_BUCKET);
        contents[7] = new ItemStack(Material.WATER_BUCKET);
        contents[8] = enchant(new ItemStack(Material.DIAMOND_AXE), Enchantment.SHARPNESS, 2);
        contents[9] = new ItemStack(Material.ARROW, 64);
        contents[10] = new ItemStack(Material.COBWEB, 8);
        contents[11] = new ItemStack(Material.OAK_PLANKS, 64);
        contents[12] = new ItemStack(Material.OAK_PLANKS, 64);
        contents[13] = new ItemStack(Material.WATER_BUCKET);
        contents[14] = new ItemStack(Material.WATER_BUCKET);
        contents[15] = enchant(new ItemStack(Material.DIAMOND_PICKAXE), Enchantment.EFFICIENCY, 3);

        return new Kit("uhc", "§3⛏ UHC", Material.DIAMOND_PICKAXE,
                "§7Diamant Prot 3, Axt, Bogen, Armbrust, Spitzhacke – kein Regen", armor, contents);
    }

    public static Kit createCombo() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION, 2);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 2);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION, 2);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 2);

        ItemStack[] contents = new ItemStack[36];
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.addEnchant(Enchantment.SHARPNESS, 2, true);
            swordMeta.addEnchant(Enchantment.KNOCKBACK, 2, true);
            sword.setItemMeta(swordMeta);
        }
        contents[0] = sword;
        contents[1] = enchant(new ItemStack(Material.DIAMOND_AXE), Enchantment.SHARPNESS, 1);
        contents[2] = new ItemStack(Material.ENDER_PEARL, 16);

        ItemStack speedPotion = createDrinkablePotion(PotionType.STRONG_SWIFTNESS, "§bSpeed II");
        contents[3] = speedPotion.clone();
        contents[4] = speedPotion.clone();
        contents[5] = new ItemStack(Material.GOLDEN_APPLE, 8);
        contents[6] = new ItemStack(Material.COOKED_BEEF, 64);

        return new Kit("combo", "§b⚡ Combo", Material.FEATHER,
                "§7Diamant-Rüstung, Knockback-Schwert, Axt & Speed", armor, contents);
    }

    public static Kit createMace() {
        // Full Netherite armor Prot 4 Unbreaking 3; boots also Feather Falling 4
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(enchant(new ItemStack(Material.NETHERITE_HELMET),
                Enchantment.PROTECTION, 4), Enchantment.UNBREAKING, 3);
        armor[2] = enchant(enchant(new ItemStack(Material.NETHERITE_CHESTPLATE),
                Enchantment.PROTECTION, 4), Enchantment.UNBREAKING, 3);
        armor[1] = enchant(enchant(new ItemStack(Material.NETHERITE_LEGGINGS),
                Enchantment.PROTECTION, 4), Enchantment.UNBREAKING, 3);
        ItemStack boots = enchant(enchant(new ItemStack(Material.NETHERITE_BOOTS),
                Enchantment.PROTECTION, 4), Enchantment.UNBREAKING, 3);
        boots = enchant(boots, Enchantment.FEATHER_FALLING, 4);
        armor[0] = boots;

        ItemStack[] contents = new ItemStack[36];
        // Netherite Sword Sharp 5 Unbreaking 3
        contents[0] = enchant(enchant(new ItemStack(Material.NETHERITE_SWORD),
                Enchantment.SHARPNESS, 5), Enchantment.UNBREAKING, 3);
        // Mace 1: Wind Burst 1, Density 5, Unbreaking 3
        ItemStack maceWind = new ItemStack(Material.MACE);
        maceWind = enchant(maceWind, Enchantment.WIND_BURST, 1);
        maceWind = enchant(maceWind, Enchantment.DENSITY, 5);
        maceWind = enchant(maceWind, Enchantment.UNBREAKING, 3);
        contents[1] = maceWind;
        // Mace 2: Breach 4, Unbreaking 3
        ItemStack maceBreach = new ItemStack(Material.MACE);
        maceBreach = enchant(maceBreach, Enchantment.BREACH, 4);
        maceBreach = enchant(maceBreach, Enchantment.UNBREAKING, 3);
        // Netherite Axe Sharp 5 Unbreaking 3
        contents[2] = enchant(enchant(new ItemStack(Material.NETHERITE_AXE),
                Enchantment.SHARPNESS, 5), Enchantment.UNBREAKING, 3);
        // 3 × 16 Ender Pearls
        contents[3] = new ItemStack(Material.ENDER_PEARL, 16);
        contents[4] = new ItemStack(Material.ENDER_PEARL, 16);
        contents[5] = new ItemStack(Material.ENDER_PEARL, 16);
        // 2 × 64 Golden Apples
        contents[6] = new ItemStack(Material.GOLDEN_APPLE, 64);
        contents[7] = new ItemStack(Material.GOLDEN_APPLE, 64);
        // 2nd Totem in inventory
        contents[8] = new ItemStack(Material.TOTEM_OF_UNDYING);
        // 2 × 64 Wind Charges
        contents[9] = new ItemStack(Material.WIND_CHARGE, 64);
        contents[10] = new ItemStack(Material.WIND_CHARGE, 64);
        // Elytra
        contents[11] = new ItemStack(Material.ELYTRA);
        // 11 Strength II splash potions
        ItemStack strengthPot = createSplashPotion(PotionType.STRONG_STRENGTH, "§cStärke II");
        for (int i = 12; i <= 22; i++) contents[i] = strengthPot.clone();
        // 10 Speed II splash potions (throwable)
        ItemStack speedPot = createSplashPotion(PotionType.STRONG_SWIFTNESS, "§bSpeed II");
        for (int i = 23; i <= 32; i++) contents[i] = speedPot.clone();
        // Second mace (Breach 4 Unbreaking 3) at slot 33
        contents[33] = maceBreach;
        // Shield at slot 34
        contents[34] = new ItemStack(Material.SHIELD);

        // First Totem of Undying goes in offhand
        return new Kit("mace", "§5💥 Mace", Material.MACE,
                "§7Netherite-Rüstung, Keule, Elytra, Totem & Windladungen", armor, contents)
                .withOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
    }

    public static Kit createAxe() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET),   Enchantment.PROTECTION, 2);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 2);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS),  Enchantment.PROTECTION, 2);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS),     Enchantment.PROTECTION, 2);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 2);
        contents[1] = enchant(new ItemStack(Material.BOW),           Enchantment.POWER, 1);
        contents[2] = enchant(new ItemStack(Material.DIAMOND_AXE),   Enchantment.SHARPNESS, 2);
        contents[3] = new ItemStack(Material.CROSSBOW);
        contents[4] = new ItemStack(Material.ARROW, 6);

        return new Kit("axe", "§6🪓 Axe", Material.DIAMOND_AXE,
                "§7Diamant Prot 2 – Schwert, Bogen, Axt, Armbrust & Pfeile", armor, contents);
    }

    public static Kit createCrystal() {
        ItemStack[] armor = new ItemStack[4];
        // Boots: Prot4 Unbreaking3 Feather Falling4 Mending
        ItemStack boots = enchant(enchant(enchant(enchant(
                new ItemStack(Material.NETHERITE_BOOTS),
                Enchantment.PROTECTION, 4), Enchantment.UNBREAKING, 3),
                Enchantment.FEATHER_FALLING, 4), Enchantment.MENDING, 1);
        armor[0] = boots;
        // Leggings: Blast Prot4 Unbreaking3 Mending
        ItemStack legs = enchant(enchant(enchant(
                new ItemStack(Material.NETHERITE_LEGGINGS),
                Enchantment.BLAST_PROTECTION, 4), Enchantment.UNBREAKING, 3), Enchantment.MENDING, 1);
        armor[1] = legs;
        // Chestplate: Prot4 Unbreaking3 Mending
        ItemStack chest = enchant(enchant(enchant(
                new ItemStack(Material.NETHERITE_CHESTPLATE),
                Enchantment.PROTECTION, 4), Enchantment.UNBREAKING, 3), Enchantment.MENDING, 1);
        armor[2] = chest;
        // Helmet: Prot4 Unbreaking3 Mending
        ItemStack helmet = enchant(enchant(enchant(
                new ItemStack(Material.NETHERITE_HELMET),
                Enchantment.PROTECTION, 4), Enchantment.UNBREAKING, 3), Enchantment.MENDING, 1);
        armor[3] = helmet;

        ItemStack[] contents = new ItemStack[36];
        // Slot 0: Sword Sharp5 Knockback1 Sweeping Edge3 Unbreaking3
        contents[0] = enchant(enchant(enchant(enchant(
                new ItemStack(Material.NETHERITE_SWORD),
                Enchantment.SHARPNESS, 5), Enchantment.KNOCKBACK, 1),
                Enchantment.SWEEPING_EDGE, 3), Enchantment.UNBREAKING, 3);
        // Slot 1: Netherite Axe Sharp5 Efficiency5 Unbreaking3
        contents[1] = enchant(enchant(enchant(
                new ItemStack(Material.NETHERITE_AXE),
                Enchantment.SHARPNESS, 5), Enchantment.EFFICIENCY, 5), Enchantment.UNBREAKING, 3);
        // Slots 2-3: 2×64 End Crystals
        contents[2] = new ItemStack(Material.END_CRYSTAL, 64);
        contents[3] = new ItemStack(Material.END_CRYSTAL, 64);
        // Slots 4-8: 5×16 Ender Pearls
        for (int i = 4; i <= 8; i++) {
            contents[i] = new ItemStack(Material.ENDER_PEARL, 16);
        }
        // Slot 9: 64 Respawn Anchors
        contents[9] = new ItemStack(Material.RESPAWN_ANCHOR, 64);
        // Slot 10: 64 Glowstone
        contents[10] = new ItemStack(Material.GLOWSTONE, 64);
        // Slots 11-12: 2×64 Experience Bottles
        contents[11] = new ItemStack(Material.EXPERIENCE_BOTTLE, 64);
        contents[12] = new ItemStack(Material.EXPERIENCE_BOTTLE, 64);
        // Slot 13: Shield Unbreaking3 Mending
        contents[13] = enchant(enchant(new ItemStack(Material.SHIELD),
                Enchantment.UNBREAKING, 3), Enchantment.MENDING, 1);
        // Slots 14-31: 18 Totems of Undying in inventory
        for (int i = 14; i <= 31; i++) {
            contents[i] = new ItemStack(Material.TOTEM_OF_UNDYING);
        }
        // Slots 32-33: 2×64 Obsidian
        contents[32] = new ItemStack(Material.OBSIDIAN, 64);
        contents[33] = new ItemStack(Material.OBSIDIAN, 64);
        // Slots 34-35: 2×64 Golden Apples
        contents[34] = new ItemStack(Material.GOLDEN_APPLE, 64);
        contents[35] = new ItemStack(Material.GOLDEN_APPLE, 64);

        // 1 Totem of Undying in offhand → 19 totems total
        return new Kit("crystal", "§d✦ Crystal", Material.END_CRYSTAL,
                "§7Netherite Prot 4, Axt, 2×64 Kristalle, 19 Totems – nur in Crystal-Arenen",
                armor, contents)
                .withCrystalOnly()
                .withOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
    }

    public static Kit createOnlySword() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET),    Enchantment.PROTECTION, 2);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 2);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS),   Enchantment.PROTECTION, 2);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS),      Enchantment.PROTECTION, 2);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = new ItemStack(Material.DIAMOND_SWORD);

        return new Kit("onlysword", "§f⚔ OnlySword", Material.DIAMOND_SWORD,
                "§7Nur ein Diamant-Schwert & volle Diamant-Rüstung", armor, contents)
                .withNoOffHand();
    }

    /**
     * The kit uses {@code Material.CHEST} as placeholder icon.
     */
    public static Kit fromCustom(CustomKit ck) {
        Kit k = new Kit(ck.getName(), "§f" + ck.getName(), Material.CHEST, "§7Eigenes Kit",
                ck.getArmor(), ck.getContents());
        ItemStack oh = ck.getOffHandItem();
        if (oh != null) k.withOffHand(oh);
        return k;
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
