package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopManager {

    // ── Category definitions (order matches the CSV sent by user) ─────────────

    /** Ordered map: category display name → item material keys. */
    public static final LinkedHashMap<String, String[]> CATEGORIES = new LinkedHashMap<>();

    /** Representative icon Material per category shown in the category overview. */
    public static final Map<String, Material> CATEGORY_ICONS = new LinkedHashMap<>();

    static {
        CATEGORIES.put("Mob Drops", new String[]{
            "BONE","BONE_MEAL","BONE_BLOCK","ARROW","FEATHER","STRING",
            "SPIDER_EYE","SLIMEBALL","BLAZE_ROD","BLAZE_POWDER","SLIME_BLOCK",
            "LEATHER","RABBIT_HIDE","RABBIT_FOOT","INK_SAC","GLOW_INK_SAC",
            "PHANTOM_MEMBRANE","GHAST_TEAR","MAGMA_CREAM","GUNPOWDER",
            "ENDER_PEARL","ENDER_EYE","DRAGON_BREATH","SHULKER_SHELL",
            "NAUTILUS_SHELL","HEART_OF_THE_SEA","TURTLE_EGG","SCUTE",
            "AXOLOTL_BUCKET","PRISMARINE_CRYSTALS","PRISMARINE_SHARD",
            "SPONGE","WET_SPONGE","GLOWSTONE_DUST","NETHER_STAR",
            "CREEPER_HEAD","SKELETON_SKULL","ZOMBIE_HEAD"
        });
        CATEGORY_ICONS.put("Mob Drops", Material.BONE);

        CATEGORIES.put("Nether", new String[]{
            "NETHERRACK","NETHER_BRICK","NETHER_BRICKS","SOUL_SAND","SOUL_SOIL",
            "GLOWSTONE","NETHER_WART","NETHER_QUARTZ_ORE",
            "CRIMSON_FUNGUS","WARPED_FUNGUS","CRIMSON_STEM","WARPED_STEM",
            "CRIMSON_PLANKS","WARPED_PLANKS","NETHER_SPROUTS",
            "WARPED_ROOTS","CRIMSON_ROOTS","TWISTING_VINES","WEEPING_VINES",
            "MAGMA_BLOCK","NETHER_WART_BLOCK","WARPED_WART_BLOCK","SHROOMLIGHT"
        });
        CATEGORY_ICONS.put("Nether", Material.NETHERRACK);

        CATEGORIES.put("End", new String[]{
            "END_STONE","END_STONE_BRICKS","END_ROD",
            "PURPUR_BLOCK","PURPUR_PILLAR",
            "CHORUS_FLOWER","CHORUS_FRUIT","POPPED_CHORUS_FRUIT"
        });
        CATEGORY_ICONS.put("End", Material.END_STONE);

        CATEGORIES.put("Farben", new String[]{
            "WHITE_DYE","BLACK_DYE","RED_DYE","BLUE_DYE","GREEN_DYE",
            "YELLOW_DYE","ORANGE_DYE","GRAY_DYE","LIGHT_GRAY_DYE",
            "PURPLE_DYE","MAGENTA_DYE","CYAN_DYE","BROWN_DYE",
            "LIME_DYE","LIGHT_BLUE_DYE","PINK_DYE"
        });
        CATEGORY_ICONS.put("Farben", Material.RED_DYE);

        CATEGORIES.put("Werkzeuge", new String[]{
            "WOODEN_PICKAXE","STONE_PICKAXE","IRON_PICKAXE","GOLDEN_PICKAXE","DIAMOND_PICKAXE",
            "WOODEN_AXE","STONE_AXE","IRON_AXE","GOLDEN_AXE","DIAMOND_AXE",
            "WOODEN_SHOVEL","STONE_SHOVEL","IRON_SHOVEL","GOLDEN_SHOVEL","DIAMOND_SHOVEL",
            "WOODEN_HOE","STONE_HOE","IRON_HOE","GOLDEN_HOE","DIAMOND_HOE",
            "FISHING_ROD","SHEARS","MAP","COMPASS","CLOCK","FLINT_AND_STEEL",
            "BUCKET","WATER_BUCKET","LAVA_BUCKET","MILK_BUCKET","POWDER_SNOW_BUCKET"
        });
        CATEGORY_ICONS.put("Werkzeuge", Material.IRON_PICKAXE);

        CATEGORIES.put("Waffen", new String[]{
            "WOODEN_SWORD","STONE_SWORD","IRON_SWORD","GOLDEN_SWORD","DIAMOND_SWORD",
            "BOW","CROSSBOW","TRIDENT"
        });
        CATEGORY_ICONS.put("Waffen", Material.IRON_SWORD);

        CATEGORIES.put("Rüstung", new String[]{
            "LEATHER_HELMET","LEATHER_CHESTPLATE","LEATHER_LEGGINGS","LEATHER_BOOTS",
            "IRON_HELMET","IRON_CHESTPLATE","IRON_LEGGINGS","IRON_BOOTS",
            "GOLDEN_HELMET","GOLDEN_CHESTPLATE","GOLDEN_LEGGINGS","GOLDEN_BOOTS",
            "DIAMOND_HELMET","DIAMOND_CHESTPLATE","DIAMOND_LEGGINGS","DIAMOND_BOOTS",
            "TURTLE_HELMET","SHIELD"
        });
        CATEGORY_ICONS.put("Rüstung", Material.IRON_CHESTPLATE);

        CATEGORIES.put("Brauen", new String[]{
            "GLASS_BOTTLE","POTION","NETHER_WART","GLOWSTONE_DUST","REDSTONE",
            "FERMENTED_SPIDER_EYE","SPIDER_EYE","MAGMA_CREAM","GHAST_TEAR",
            "RABBIT_FOOT","PUFFERFISH","GLISTERING_MELON_SLICE"
        });
        CATEGORY_ICONS.put("Brauen", Material.GLASS_BOTTLE);

        CATEGORIES.put("Redstone", new String[]{
            "REDSTONE","REDSTONE_BLOCK","REDSTONE_TORCH","LEVER",
            "OAK_PRESSURE_PLATE","STONE_PRESSURE_PLATE",
            "HEAVY_WEIGHTED_PRESSURE_PLATE","LIGHT_WEIGHTED_PRESSURE_PLATE",
            "STONE_BUTTON","OAK_BUTTON","OBSERVER","PISTON","STICKY_PISTON",
            "DISPENSER","DROPPER","HOPPER","COMPARATOR","REPEATER",
            "RAIL","POWERED_RAIL","DETECTOR_RAIL","ACTIVATOR_RAIL",
            "MINECART","CHEST_MINECART","HOPPER_MINECART",
            "TNT","FIREWORK_ROCKET","BELL",
            "IRON_TRAPDOOR","OAK_TRAPDOOR","IRON_DOOR","OAK_DOOR"
        });
        CATEGORY_ICONS.put("Redstone", Material.REDSTONE);
    }


    private final KlassenPlugin plugin;
    private final File shopFile;
    private FileConfiguration shopConfig;
    private final Map<String, double[]> items = new LinkedHashMap<>();

    public ShopManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.shopFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!shopFile.exists()) createDefaultShop();
        load();
    }

    private void createDefaultShop() {
        try { plugin.getDataFolder().mkdirs(); shopFile.createNewFile(); } catch (IOException e) { plugin.getLogger().severe("shop.yml error: " + e.getMessage()); return; }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(shopFile);
        // Format: {MATERIAL, buyPrice, sellPrice}  (buy=-1 → not buyable / sell-only)
        // Excluded: spawn eggs, spawners, command blocks, bedrock, structure blocks.
        Object[][] defaults = {
            // ── Precious minerals ──
            {"DIAMOND",150.0,75.0},{"DIAMOND_ORE",-1.0,120.0},{"DEEPSLATE_DIAMOND_ORE",-1.0,120.0},
            {"EMERALD",100.0,50.0},{"EMERALD_ORE",-1.0,80.0},{"DEEPSLATE_EMERALD_ORE",-1.0,80.0},
            {"NETHERITE_INGOT",500.0,250.0},{"NETHERITE_SCRAP",120.0,60.0},
            {"ANCIENT_DEBRIS",400.0,200.0},
            // ── Metals ──
            {"GOLD_INGOT",20.0,10.0},{"GOLD_NUGGET",3.0,1.5},
            {"GOLD_ORE",-1.0,16.0},{"DEEPSLATE_GOLD_ORE",-1.0,16.0},{"NETHER_GOLD_ORE",-1.0,14.0},
            {"IRON_INGOT",8.0,4.0},{"IRON_NUGGET",1.5,0.8},
            {"IRON_ORE",-1.0,6.0},{"DEEPSLATE_IRON_ORE",-1.0,6.0},
            {"COPPER_INGOT",4.0,2.0},{"RAW_COPPER",2.0,1.0},{"COPPER_ORE",-1.0,3.0},{"DEEPSLATE_COPPER_ORE",-1.0,3.0},
            // ── Coal & Redstone ──
            {"COAL",3.0,1.5},{"COAL_ORE",-1.0,2.5},{"DEEPSLATE_COAL_ORE",-1.0,2.5},{"CHARCOAL",2.5,1.2},
            {"REDSTONE",5.0,2.5},{"REDSTONE_ORE",-1.0,4.0},{"DEEPSLATE_REDSTONE_ORE",-1.0,4.0},
            // ── Lapis & Quartz ──
            {"LAPIS_LAZULI",8.0,4.0},{"LAPIS_ORE",-1.0,6.0},{"DEEPSLATE_LAPIS_ORE",-1.0,6.0},
            {"QUARTZ",4.0,2.0},{"NETHER_QUARTZ_ORE",-1.0,3.0},
            // ── Wood (all types) ──
            {"OAK_LOG",8.0,4.0},{"OAK_PLANKS",3.0,1.0},{"OAK_SAPLING",4.0,2.0},
            {"SPRUCE_LOG",8.0,4.0},{"SPRUCE_PLANKS",3.0,1.0},{"SPRUCE_SAPLING",4.0,2.0},
            {"BIRCH_LOG",8.0,4.0},{"BIRCH_PLANKS",3.0,1.0},{"BIRCH_SAPLING",4.0,2.0},
            {"JUNGLE_LOG",8.0,4.0},{"JUNGLE_PLANKS",3.0,1.0},{"JUNGLE_SAPLING",5.0,2.0},
            {"ACACIA_LOG",8.0,4.0},{"ACACIA_PLANKS",3.0,1.0},{"ACACIA_SAPLING",4.0,2.0},
            {"DARK_OAK_LOG",8.0,4.0},{"DARK_OAK_PLANKS",3.0,1.0},{"DARK_OAK_SAPLING",5.0,2.0},
            {"CHERRY_LOG",10.0,5.0},{"CHERRY_PLANKS",1.5,0.8},{"CHERRY_SAPLING",8.0,4.0},
            {"MANGROVE_LOG",8.0,4.0},{"MANGROVE_PLANKS",1.2,0.6},{"MANGROVE_PROPAGULE",3.0,1.5},
            {"BAMBOO",2.0,1.0},{"BAMBOO_BLOCK",1.5,0.7},{"BAMBOO_PLANKS",0.8,0.4},
            // ── Stone & Building blocks ──
            {"STONE",4.0,2.0},{"COBBLESTONE",2.0,1.0},{"SMOOTH_STONE",5.0,2.0},
            {"COBBLESTONE_SLAB",0.5,0.1},{"STONE_BRICKS",6.0,3.0},{"CRACKED_STONE_BRICKS",5.0,2.0},
            {"MOSSY_COBBLESTONE",6.0,3.0},{"MOSSY_STONE_BRICKS",8.0,4.0},
            {"GRANITE",5.0,2.0},{"POLISHED_GRANITE",7.0,3.0},
            {"DIORITE",5.0,2.0},{"POLISHED_DIORITE",7.0,3.0},
            {"ANDESITE",5.0,2.0},{"POLISHED_ANDESITE",7.0,3.0},
            {"CALCITE",4.0,2.0},{"TUFF",4.0,2.0},{"DRIPSTONE_BLOCK",2.0,0.5},{"POINTED_DRIPSTONE",2.0,0.6},
            {"DEEPSLATE",5.0,2.0},{"COBBLED_DEEPSLATE",1.2,0.3},{"POLISHED_DEEPSLATE",7.0,3.0},
            {"DEEPSLATE_BRICKS",8.0,4.0},{"DEEPSLATE_TILES",3.5,1.0},
            {"BASALT",5.0,2.0},{"SMOOTH_BASALT",6.0,3.0},
            {"BLACKSTONE",5.0,2.0},{"POLISHED_BLACKSTONE",7.0,3.0},{"POLISHED_BLACKSTONE_BRICKS",8.0,4.0},
            {"SAND",4.0,2.0},{"RED_SAND",5.0,3.0},{"SANDSTONE",6.0,3.0},{"RED_SANDSTONE",7.0,4.0},{"SMOOTH_SANDSTONE",8.0,4.0},
            {"GRAVEL",3.0,1.0},{"FLINT",1.5,0.8},
            {"DIRT",2.0,1.0},{"GRASS_BLOCK",4.0,2.0},{"COARSE_DIRT",3.0,1.0},{"PODZOL",4.0,2.0},{"MYCELIUM",6.0,3.0},{"MUD",3.0,1.0},
            {"MUDDY_MANGROVE_ROOTS",4.0,2.0},
            {"CLAY",5.0,3.0},{"CLAY_BALL",0.6,0.3},{"CLAY_BLOCK",8.0,4.0},
            {"TERRACOTTA",6.0,3.0},{"WHITE_GLAZED_TERRACOTTA",8.0,4.0},
            {"BRICK",3.0,1.5},{"BRICKS",5.0,2.0},
            {"GLASS",5.0,2.0},{"GLASS_PANE",2.0,1.0},
            {"OBSIDIAN",20.0,10.0},{"CRYING_OBSIDIAN",25.0,12.0},
            {"SOUL_SAND",6.0,3.0},{"SOUL_SOIL",5.0,2.0},
            {"SNOWBALL",2.0,1.0},{"SNOW_BLOCK",4.0,2.0},{"ICE",6.0,3.0},{"PACKED_ICE",10.0,5.0},{"BLUE_ICE",15.0,8.0},
            // ── Wool & Carpet ──
            {"WHITE_WOOL",6.0,3.0},{"BLACK_WOOL",8.0,4.0},{"RED_WOOL",7.0,3.0},{"BLUE_WOOL",7.0,3.0},
            {"GREEN_WOOL",7.0,3.0},{"YELLOW_WOOL",7.0,3.0},{"ORANGE_WOOL",7.0,3.0},{"GRAY_WOOL",7.0,3.0},
            {"LIGHT_GRAY_WOOL",7.0,3.0},{"PURPLE_WOOL",8.0,4.0},{"MAGENTA_WOOL",8.0,4.0},{"CYAN_WOOL",8.0,4.0},
            {"BROWN_WOOL",8.0,4.0},{"LIME_WOOL",7.0,3.0},{"LIGHT_BLUE_WOOL",7.0,3.0},{"PINK_WOOL",7.0,3.0},
            {"WHITE_CARPET",3.0,1.0},{"SPONGE",40.0,20.0},
            // ── Nether ──
            {"NETHERRACK",2.0,1.0},{"NETHER_BRICK",5.0,2.0},{"NETHER_BRICKS",8.0,4.0},{"CRACKED_NETHER_BRICKS",2.5,0.8},
            {"GLOWSTONE",15.0,7.0},{"NETHER_WART",12.0,6.0},{"NETHER_QUARTZ_ORE",15.0,7.0},
            {"CRIMSON_FUNGUS",8.0,4.0},{"WARPED_FUNGUS",8.0,4.0},{"CRIMSON_STEM",8.0,4.0},{"WARPED_STEM",8.0,4.0},
            {"CRIMSON_PLANKS",4.0,2.0},{"WARPED_PLANKS",4.0,2.0},
            {"NETHER_SPROUTS",3.0,1.0},{"WARPED_ROOTS",3.0,1.0},{"CRIMSON_ROOTS",3.0,1.0},
            {"TWISTING_VINES",4.0,2.0},{"WEEPING_VINES",4.0,2.0},
            {"MAGMA_BLOCK",10.0,5.0},{"NETHER_WART_BLOCK",10.0,5.0},{"WARPED_WART_BLOCK",10.0,5.0},{"SHROOMLIGHT",20.0,10.0},
            // ── End ──
            {"END_STONE",8.0,4.0},{"END_STONE_BRICKS",10.0,5.0},{"END_ROD",15.0,7.0},
            {"PURPUR_BLOCK",12.0,6.0},{"PURPUR_PILLAR",14.0,7.0},
            {"CHORUS_FLOWER",20.0,10.0},{"CHORUS_FRUIT",15.0,7.0},{"POPPED_CHORUS_FRUIT",20.0,10.0},
            // ── Ores & Metals ──
            {"COAL",8.0,4.0},{"COAL_BLOCK",64.0,32.0},
            {"RAW_IRON",12.0,6.0},{"IRON_INGOT",20.0,10.0},{"IRON_BLOCK",160.0,80.0},{"IRON_NUGGET",3.0,1.0},
            {"RAW_COPPER",8.0,4.0},{"COPPER_INGOT",12.0,6.0},{"COPPER_BLOCK",90.0,45.0},
            {"RAW_GOLD",20.0,10.0},{"GOLD_INGOT",35.0,18.0},{"GOLD_BLOCK",280.0,140.0},{"GOLD_NUGGET",5.0,2.0},
            {"REDSTONE",10.0,5.0},{"REDSTONE_BLOCK",80.0,40.0},
            {"LAPIS_LAZULI",15.0,7.0},{"LAPIS_BLOCK",120.0,60.0},
            {"EMERALD",50.0,25.0},{"EMERALD_BLOCK",400.0,200.0},
            {"DIAMOND",100.0,50.0},{"DIAMOND_BLOCK",800.0,400.0},
            {"NETHERITE_SCRAP",120.0,60.0},{"NETHERITE_INGOT",500.0,250.0},{"ANCIENT_DEBRIS",400.0,200.0},
            {"QUARTZ",10.0,5.0},{"QUARTZ_BLOCK",80.0,40.0},{"SMOOTH_QUARTZ",90.0,45.0},
            {"AMETHYST_SHARD",12.0,6.0},{"AMETHYST_BLOCK",80.0,40.0},{"BUDDING_AMETHYST",60.0,30.0},
            // ── Farming & Food ──
            {"WHEAT",5.0,2.0},{"WHEAT_SEEDS",2.0,1.0},
            {"CARROT",5.0,2.0},{"GOLDEN_CARROT",25.0,12.0},{"POTATO",5.0,2.0},{"BAKED_POTATO",8.0,4.0},{"POISONOUS_POTATO",3.0,1.0},
            {"BEETROOT",5.0,2.0},{"BEETROOT_SEEDS",2.0,1.0},{"BEETROOT_SOUP",12.0,6.0},
            {"PUMPKIN",8.0,4.0},{"PUMPKIN_SEEDS",3.0,1.0},{"PUMPKIN_PIE",12.0,6.0},{"JACK_O_LANTERN",10.0,5.0},
            {"MELON",5.0,2.0},{"MELON_SLICE",3.0,1.0},{"MELON_SEEDS",3.0,1.0},
            {"BREAD",8.0,4.0},{"APPLE",6.0,3.0},{"GOLDEN_APPLE",60.0,30.0},{"ENCHANTED_GOLDEN_APPLE",2000.0,1000.0},
            {"SWEET_BERRIES",4.0,2.0},{"GLOW_BERRIES",5.0,3.0},{"COCOA_BEANS",5.0,2.0},
            {"MUSHROOM_STEW",12.0,6.0},{"SUSPICIOUS_STEW",15.0,7.0},{"RABBIT_STEW",20.0,10.0},
            {"HONEY_BOTTLE",15.0,7.0},{"HONEY_BLOCK",50.0,25.0},{"HONEYCOMB",10.0,5.0},{"HONEYCOMB_BLOCK",80.0,40.0},
            {"EGG",2.0,1.0},{"SUGAR",3.0,1.0},{"CAKE",25.0,12.0},{"COOKIE",5.0,2.0},
            {"NETHER_WART",12.0,6.0},{"SUGAR_CANE",4.0,2.0},
            // ── Meat & Fish ──
            {"BEEF",8.0,4.0},{"COOKED_BEEF",14.0,7.0},
            {"PORKCHOP",7.0,3.0},{"COOKED_PORKCHOP",12.0,6.0},
            {"CHICKEN",6.0,3.0},{"COOKED_CHICKEN",10.0,5.0},
            {"MUTTON",7.0,3.0},{"COOKED_MUTTON",12.0,6.0},
            {"RABBIT",8.0,4.0},{"COOKED_RABBIT",13.0,6.0},
            {"SALMON",8.0,4.0},{"RAW_SALMON",8.0,4.0},{"COOKED_SALMON",14.0,7.0},
            {"COD",6.0,3.0},{"COOKED_COD",10.0,5.0},
            {"TROPICAL_FISH",5.0,2.0},{"PUFFERFISH",8.0,4.0},
            // ── Dyes ──
            {"WHITE_DYE",5.0,2.0},{"ORANGE_DYE",5.0,2.0},{"MAGENTA_DYE",8.0,4.0},
            {"LIGHT_BLUE_DYE",6.0,3.0},{"YELLOW_DYE",5.0,2.0},{"LIME_DYE",6.0,3.0},
            {"PINK_DYE",6.0,3.0},{"GRAY_DYE",5.0,2.0},{"LIGHT_GRAY_DYE",5.0,2.0},
            {"CYAN_DYE",8.0,4.0},{"PURPLE_DYE",8.0,4.0},{"BLUE_DYE",6.0,3.0},
            {"BROWN_DYE",8.0,4.0},{"GREEN_DYE",5.0,2.0},{"RED_DYE",5.0,2.0},{"BLACK_DYE",8.0,4.0},
            // ── Mob drops ──
            {"BONE",5.0,2.0},{"BONE_MEAL",3.0,1.0},{"BONE_BLOCK",20.0,10.0},
            {"ARROW",3.0,1.0},{"FEATHER",5.0,2.0},{"STRING",5.0,2.0},
            {"SPIDER_EYE",8.0,4.0},{"FERMENTED_SPIDER_EYE",15.0,7.0},
            {"GUNPOWDER",10.0,5.0},{"SLIMEBALL",10.0,5.0},{"SLIME_BLOCK",80.0,40.0},{"MAGMA_CREAM",15.0,7.0},
            {"BLAZE_ROD",25.0,12.0},{"BLAZE_POWDER",15.0,7.0},
            {"GHAST_TEAR",50.0,25.0},{"PHANTOM_MEMBRANE",30.0,15.0},
            {"RABBIT_HIDE",8.0,4.0},{"RABBIT_FOOT",30.0,15.0},
            {"ROTTEN_FLESH",1.0,0.0},{"LEATHER",15.0,7.0},
            {"INK_SAC",8.0,4.0},{"GLOW_INK_SAC",20.0,10.0},
            {"PRISMARINE_SHARD",12.0,6.0},{"PRISMARINE_CRYSTALS",15.0,7.0},
            {"NAUTILUS_SHELL",30.0,15.0},{"HEART_OF_THE_SEA",200.0,100.0},
            {"ENDER_PEARL",25.0,12.0},{"ENDER_EYE",40.0,20.0},
            {"SHULKER_SHELL",150.0,75.0},{"DRAGON_BREATH",80.0,40.0},
            {"NETHER_STAR",1000.0,500.0},{"TOTEM_OF_UNDYING",300.0,150.0},
            {"WITHER_SKELETON_SKULL",200.0,100.0},
            {"TURTLE_SCUTE",15.0,7.5},{"ARMADILLO_SCUTE",12.0,6.0},
            {"TURTLE_EGG",20.0,10.0},{"SCUTE",30.0,15.0},{"AXOLOTL_BUCKET",40.0,20.0},
            {"SPONGE",40.0,20.0},{"WET_SPONGE",35.0,17.0},{"GLOWSTONE_DUST",12.0,6.0},
            {"CREEPER_HEAD",80.0,40.0},{"SKELETON_SKULL",80.0,40.0},{"ZOMBIE_HEAD",80.0,40.0},
            // ── Plants & Nature ──
            {"OAK_LEAVES",2.0,1.0},{"SPRUCE_LEAVES",0.3,0.1},{"BIRCH_LEAVES",0.3,0.1},
            {"JUNGLE_LEAVES",0.3,0.1},{"ACACIA_LEAVES",0.3,0.1},{"DARK_OAK_LEAVES",0.3,0.1},
            {"VINE",2.0,1.0},{"KELP",0.5,0.2},{"DRIED_KELP",0.3,0.1},{"DRIED_KELP_BLOCK",2.0,0.8},
            {"SEA_PICKLE",2.0,0.8},{"LILY_PAD",3.0,1.0},{"CACTUS",3.0,1.0},
            {"FLOWER_POT",5.0,2.0},{"POPPY",2.0,1.0},{"DANDELION",2.0,1.0},
            {"FERN",1.0,0.0},{"GRASS",1.0,0.0},{"DEAD_BUSH",1.0,0.0},
            {"BROWN_MUSHROOM",4.0,2.0},{"RED_MUSHROOM",4.0,2.0},
            {"BROWN_MUSHROOM_BLOCK",3.0,1.0},{"RED_MUSHROOM_BLOCK",3.0,1.0},{"MUSHROOM_STEM",3.0,1.0},
            // ── Tools ──
            {"WOODEN_PICKAXE",12.0,5.0},{"STONE_PICKAXE",20.0,8.0},{"IRON_PICKAXE",60.0,25.0},{"GOLDEN_PICKAXE",50.0,20.0},{"DIAMOND_PICKAXE",300.0,120.0},
            {"WOODEN_AXE",12.0,5.0},{"STONE_AXE",20.0,8.0},{"IRON_AXE",60.0,25.0},{"GOLDEN_AXE",50.0,20.0},{"DIAMOND_AXE",300.0,120.0},
            {"WOODEN_SHOVEL",10.0,4.0},{"STONE_SHOVEL",16.0,6.0},{"IRON_SHOVEL",50.0,20.0},{"GOLDEN_SHOVEL",40.0,16.0},{"DIAMOND_SHOVEL",250.0,100.0},
            {"WOODEN_HOE",10.0,4.0},{"STONE_HOE",16.0,6.0},{"IRON_HOE",50.0,20.0},{"GOLDEN_HOE",40.0,16.0},{"DIAMOND_HOE",250.0,100.0},
            {"FISHING_ROD",20.0,8.0},{"SHEARS",20.0,8.0},{"MAP",10.0,4.0},
            {"COMPASS",25.0,10.0},{"CLOCK",40.0,18.0},{"FLINT_AND_STEEL",20.0,8.0},
            {"BUCKET",15.0,6.0},{"WATER_BUCKET",20.0,8.0},{"LAVA_BUCKET",25.0,12.0},{"MILK_BUCKET",20.0,10.0},{"POWDER_SNOW_BUCKET",15.0,6.0},
            // ── Weapons ──
            {"WOODEN_SWORD",15.0,6.0},{"STONE_SWORD",25.0,10.0},{"IRON_SWORD",70.0,30.0},{"GOLDEN_SWORD",55.0,22.0},{"DIAMOND_SWORD",350.0,140.0},
            {"BOW",30.0,12.0},{"CROSSBOW",50.0,20.0},{"TRIDENT",400.0,180.0},
            // ── Armor ──
            {"LEATHER_HELMET",20.0,8.0},{"LEATHER_CHESTPLATE",30.0,12.0},{"LEATHER_LEGGINGS",25.0,10.0},{"LEATHER_BOOTS",18.0,7.0},
            {"IRON_HELMET",60.0,25.0},{"IRON_CHESTPLATE",100.0,40.0},{"IRON_LEGGINGS",80.0,32.0},{"IRON_BOOTS",50.0,20.0},
            {"GOLDEN_HELMET",50.0,20.0},{"GOLDEN_CHESTPLATE",80.0,32.0},{"GOLDEN_LEGGINGS",70.0,28.0},{"GOLDEN_BOOTS",40.0,16.0},
            {"DIAMOND_HELMET",280.0,110.0},{"DIAMOND_CHESTPLATE",450.0,180.0},{"DIAMOND_LEGGINGS",380.0,150.0},{"DIAMOND_BOOTS",250.0,100.0},
            {"TURTLE_HELMET",200.0,90.0},{"SHIELD",40.0,16.0},
            // ── Brewing ──
            {"GLASS_BOTTLE",5.0,2.0},{"POTION",8.0,3.0},{"GLISTERING_MELON_SLICE",15.0,7.0},
            // ── Redstone ──
            {"REDSTONE_TORCH",5.0,2.0},{"LEVER",4.0,2.0},
            {"OAK_PRESSURE_PLATE",5.0,2.0},{"STONE_PRESSURE_PLATE",6.0,3.0},
            {"HEAVY_WEIGHTED_PRESSURE_PLATE",15.0,7.0},{"LIGHT_WEIGHTED_PRESSURE_PLATE",20.0,10.0},
            {"STONE_BUTTON",4.0,2.0},{"OAK_BUTTON",3.0,1.0},
            {"OBSERVER",20.0,10.0},{"PISTON",25.0,12.0},{"STICKY_PISTON",35.0,17.0},
            {"DISPENSER",30.0,15.0},{"DROPPER",20.0,10.0},{"HOPPER",40.0,20.0},
            {"COMPARATOR",20.0,10.0},{"REPEATER",15.0,7.0},
            {"RAIL",8.0,4.0},{"POWERED_RAIL",20.0,10.0},{"DETECTOR_RAIL",15.0,7.0},{"ACTIVATOR_RAIL",15.0,7.0},
            {"MINECART",25.0,12.0},{"CHEST_MINECART",40.0,20.0},{"HOPPER_MINECART",60.0,30.0},
            {"TNT",50.0,25.0},{"FIREWORK_ROCKET",15.0,7.0},{"BELL",60.0,30.0},
            {"IRON_TRAPDOOR",20.0,10.0},{"OAK_TRAPDOOR",8.0,4.0},{"IRON_DOOR",20.0,10.0},{"OAK_DOOR",8.0,4.0},
            {"SADDLE",30.0,15.0},{"LEAD",8.0,4.0},{"NAME_TAG",20.0,10.0},
            {"TORCH",2.0,1.0},{"LANTERN",12.0,6.0},{"SOUL_LANTERN",14.0,7.0},
            {"GLOWSTONE_DUST",12.0,6.0},{"SEA_LANTERN",20.0,10.0},
            {"CHEST",15.0,7.0},{"BARREL",20.0,10.0},{"FURNACE",15.0,7.0},
            {"CRAFTING_TABLE",10.0,5.0},{"ANVIL",80.0,35.0},
            {"ENCHANTING_TABLE",200.0,100.0},
            {"BREWING_STAND",40.0,20.0},{"CAULDRON",15.0,6.0},
            {"FLINT",2.0,0.8},{"SPECTRAL_ARROW",3.0,1.2},
            {"BOOK",8.0,4.0},{"BOOKSHELF",20.0,10.0},{"PAPER",1.0,0.4},
            {"STICK",1.0,0.0},
            // ── Decoration & Furniture ──
            {"TRAPPED_CHEST",20.0,10.0},{"SHULKER_BOX",200.0,100.0},
            {"BLAST_FURNACE",35.0,17.0},{"SMOKER",30.0,15.0},
            {"CHIPPED_ANVIL",50.0,20.0},{"DAMAGED_ANVIL",25.0,10.0},
            {"WHITE_CANDLE",8.0,4.0},{"BLACK_CANDLE",8.0,4.0},{"RED_CANDLE",8.0,4.0},{"BLUE_CANDLE",8.0,4.0},
            {"SOUL_TORCH",4.0,2.0},
            {"OCHRE_FROGLIGHT",20.0,10.0},{"VERDANT_FROGLIGHT",20.0,10.0},{"PEARLESCENT_FROGLIGHT",20.0,10.0},
            {"ITEM_FRAME",8.0,4.0},{"GLOW_ITEM_FRAME",15.0,7.0},{"PAINTING",10.0,5.0},
            {"OAK_SIGN",6.0,3.0},{"WRITABLE_BOOK",10.0,5.0},
            {"JUKEBOX",40.0,20.0},{"NOTE_BLOCK",20.0,10.0},
            {"WHITE_BANNER",10.0,5.0},{"COBBLESTONE_WALL",5.0,2.0},
            // ── Flowers & Plants Deco ──
            {"BLUE_ORCHID",3.0,1.0},{"ALLIUM",3.0,1.0},{"AZURE_BLUET",2.0,1.0},
            {"RED_TULIP",2.0,1.0},{"ORANGE_TULIP",2.0,1.0},{"WHITE_TULIP",2.0,1.0},{"PINK_TULIP",2.0,1.0},
            {"OXEYE_DAISY",2.0,1.0},{"CORNFLOWER",3.0,1.0},{"LILY_OF_THE_VALLEY",3.0,1.0},
            {"WITHER_ROSE",10.0,5.0},
            {"SUNFLOWER",4.0,2.0},{"LILAC",4.0,2.0},{"TALL_GRASS",2.0,1.0},{"LARGE_FERN",2.0,1.0},
            {"ROSE_BUSH",4.0,2.0},{"PEONY",4.0,2.0},
            {"FLOWERING_AZALEA",8.0,4.0},{"AZALEA",6.0,3.0},
            {"GLOW_LICHEN",5.0,2.0},{"MOSS_BLOCK",6.0,3.0},{"MOSS_CARPET",3.0,1.0},
            // ── Music Discs ──
            {"MUSIC_DISC_13",80.0,40.0},{"MUSIC_DISC_CAT",80.0,40.0},
            {"MUSIC_DISC_BLOCKS",100.0,50.0},{"MUSIC_DISC_CHIRP",100.0,50.0},
            {"MUSIC_DISC_FAR",100.0,50.0},{"MUSIC_DISC_MALL",100.0,50.0},
            {"MUSIC_DISC_MELLOHI",100.0,50.0},{"MUSIC_DISC_STAL",100.0,50.0},
            {"MUSIC_DISC_STRAD",100.0,50.0},{"MUSIC_DISC_WARD",100.0,50.0},
            {"MUSIC_DISC_11",120.0,60.0},{"MUSIC_DISC_WAIT",100.0,50.0},
            {"MUSIC_DISC_OTHERSIDE",150.0,75.0},{"MUSIC_DISC_5",120.0,60.0},
            {"MUSIC_DISC_PIGSTEP",200.0,100.0},{"MUSIC_DISC_RELIC",200.0,100.0},
            // ── Potions ──
            {"GLASS_BOTTLE",5.0,2.0},
        };
        for (Object[] d : defaults) { cfg.set("items." + d[0] + ".buy", d[1]); cfg.set("items." + d[0] + ".sell", d[2]); }
        try { cfg.save(shopFile); } catch (IOException e) { plugin.getLogger().severe("shop.yml save error: " + e.getMessage()); }
    }

    public void load() {
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
        items.clear();
        if (shopConfig.contains("items")) {
            for (String key : shopConfig.getConfigurationSection("items").getKeys(false)) {
                double buy = shopConfig.getDouble("items." + key + ".buy", -1);
                double sell = shopConfig.getDouble("items." + key + ".sell", -1);
                items.put(key.toUpperCase(), new double[]{buy, sell});
            }
        }
    }

    public void save() {
        shopConfig.set("items", null);
        for (Map.Entry<String, double[]> e : items.entrySet()) {
            shopConfig.set("items." + e.getKey() + ".buy", e.getValue()[0]);
            shopConfig.set("items." + e.getKey() + ".sell", e.getValue()[1]);
        }
        try { shopConfig.save(shopFile); } catch (IOException e) { plugin.getLogger().severe("shop.yml save error: " + e.getMessage()); }
    }

    public double getBuyPrice(String mat) {
        double[] p = items.get(mat.toUpperCase());
        if (p == null || p[0] < 0) return -1;
        double boost = plugin.getWeeklyChangelogManager() != null ? plugin.getWeeklyChangelogManager().getBoostMultiplier(mat.toUpperCase()) : 1.0;
        return p[0] * plugin.getEconomyManager().getInflationMultiplier() * boost;
    }

    public double getSellPrice(String mat) {
        double[] p = items.get(mat.toUpperCase());
        if (p == null || p[1] < 0) return -1;
        double boost = plugin.getWeeklyChangelogManager() != null ? plugin.getWeeklyChangelogManager().getBoostMultiplier(mat.toUpperCase()) : 1.0;
        double inflation = plugin.getEconomyManager().getInflationMultiplier();
        // Sell revenue goes DOWN when inflation is high (inverse relationship):
        // if there is too much money, selling earns less; if scarce, earns more.
        double sellMultiplier = Math.max(0.25, Math.min(2.0, 1.0 / inflation));
        double sellPrice = p[1] * sellMultiplier * boost;
        // Sell price must never be >= buy price (no risk-free arbitrage).
        if (p[0] > 0) {
            double maxSell = p[0] * inflation * boost * 0.8;
            sellPrice = Math.min(sellPrice, maxSell);
        }
        return sellPrice;
    }

    public double getBaseBuyPrice(String mat) { double[] p = items.get(mat.toUpperCase()); return p == null ? -1 : p[0]; }
    public double getBaseSellPrice(String mat) { double[] p = items.get(mat.toUpperCase()); return p == null ? -1 : p[1]; }
    public boolean hasItem(String mat) { return items.containsKey(mat.toUpperCase()); }
    public Set<String> getItemNames() { return Collections.unmodifiableSet(items.keySet()); }

    /**
     * Returns the fallback sell price (per item) for materials not in shop.yml.
     * Uses {@code economy.default-sell-price} from config (default: 0.5 per item).
     * A value of -1 means selling unknown items is disabled.
     */
    public double getDefaultSellPrice() {
        return plugin.getConfig().getDouble("economy.default-sell-price", 0.5);
    }

    /**
     * Returns the effective sell price per item for {@code mat}, applying
     * inflation and checking the shop file.  Falls back to the configured
     * {@code economy.default-sell-price} for materials not in shop.yml.
     * Returns -1 only when selling is fully disabled for this material.
     */
    public double getEffectiveSellPrice(String mat) {
        double shopSell = getSellPrice(mat);
        if (shopSell >= 0) return shopSell;
        double def = getDefaultSellPrice();
        if (def < 0) return -1;
        // Apply inverse-inflation also to the default fallback price.
        double inflation = plugin.getEconomyManager().getInflationMultiplier();
        double sellMultiplier = Math.max(0.25, Math.min(2.0, 1.0 / inflation));
        return def * sellMultiplier;
    }

    public void setItem(String mat, double buy, double sell) {
        items.put(mat.toUpperCase(), new double[]{buy, sell});
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }
    public void removeItem(String mat) {
        items.remove(mat.toUpperCase());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }
}
