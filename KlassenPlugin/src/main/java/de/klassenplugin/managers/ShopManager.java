package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopManager {

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
            // ── Nether & End ──
            {"NETHERRACK",0.5,0.2},{"NETHER_BRICKS",3.0,1.0},{"CRACKED_NETHER_BRICKS",2.5,0.8},
            {"MAGMA_BLOCK",5.0,2.0},{"NETHER_WART_BLOCK",4.0,1.5},{"SHROOMLIGHT",10.0,4.0},
            {"END_STONE",5.0,2.0},{"END_STONE_BRICKS",6.0,2.5},{"PURPUR_BLOCK",8.0,3.0},
            // ── Farming & Food ──
            {"WHEAT",1.5,0.5},{"WHEAT_SEEDS",0.5,0.2},
            {"CARROT",1.5,0.6},{"POTATO",1.0,0.4},{"BAKED_POTATO",2.0,0.8},
            {"BREAD",3.0,1.5},{"APPLE",2.0,1.0},
            {"COOKED_BEEF",5.0,2.5},{"BEEF",2.0,0.8},
            {"COOKED_PORKCHOP",5.0,2.5},{"PORKCHOP",2.0,0.8},
            {"COOKED_CHICKEN",4.0,2.0},{"CHICKEN",1.5,0.6},
            {"COOKED_MUTTON",4.5,2.0},{"MUTTON",2.0,0.8},
            {"COOKED_RABBIT",5.0,2.5},{"RABBIT",2.0,0.8},
            {"COOKED_SALMON",4.0,2.0},{"SALMON",1.5,0.6},
            {"COOKED_COD",3.5,1.5},{"COD",1.0,0.4},
            {"GOLDEN_APPLE",50.0,25.0},{"ENCHANTED_GOLDEN_APPLE",500.0,200.0},
            {"HONEY_BOTTLE",8.0,4.0},{"HONEYCOMB",6.0,3.0},{"CAKE",15.0,7.0},
            {"PUMPKIN",3.0,1.5},{"MELON",2.0,1.0},{"MELON_SLICE",0.8,0.3},
            {"BEETROOT",1.5,0.6},{"BEETROOT_SEEDS",0.5,0.2},
            {"NETHER_WART",3.0,1.2},{"COCOA_BEANS",2.0,1.0},{"SUGAR_CANE",4.0,2.0},{"SUGAR",0.5,0.2},
            {"SWEET_BERRIES",1.5,0.6},{"GLOW_BERRIES",2.0,1.0},
            {"MUSHROOM_STEW",5.0,2.5},{"RABBIT_STEW",8.0,4.0},{"SUSPICIOUS_STEW",6.0,3.0},
            // ── Dyes ──
            {"WHITE_DYE",2.0,0.8},{"ORANGE_DYE",2.0,0.8},{"MAGENTA_DYE",2.5,1.0},
            {"LIGHT_BLUE_DYE",2.5,1.0},{"YELLOW_DYE",2.0,0.8},{"LIME_DYE",2.0,0.8},
            {"PINK_DYE",2.5,1.0},{"GRAY_DYE",2.0,0.8},{"LIGHT_GRAY_DYE",2.0,0.8},
            {"CYAN_DYE",2.5,1.0},{"PURPLE_DYE",2.5,1.0},{"BLUE_DYE",2.5,1.0},
            {"BROWN_DYE",3.0,1.5},{"GREEN_DYE",2.5,1.0},{"RED_DYE",2.0,0.8},{"BLACK_DYE",3.0,1.5},
            // ── Mob drops ──
            {"STRING",1.5,0.5},{"FEATHER",1.5,0.5},{"BONE",1.5,0.5},{"BONE_MEAL",2.0,0.8},
            {"SPIDER_EYE",3.0,1.5},{"FERMENTED_SPIDER_EYE",8.0,4.0},
            {"GUNPOWDER",5.0,2.0},{"SLIME_BALL",6.0,3.0},{"MAGMA_CREAM",8.0,4.0},
            {"BLAZE_ROD",15.0,7.5},{"BLAZE_POWDER",8.0,4.0},
            {"GHAST_TEAR",20.0,10.0},{"PHANTOM_MEMBRANE",15.0,7.5},
            {"RABBIT_HIDE",2.0,1.0},{"RABBIT_FOOT",10.0,5.0},
            {"ROTTEN_FLESH",0.5,0.2},{"LEATHER",8.0,4.0},
            {"INK_SAC",4.0,2.0},{"GLOW_INK_SAC",8.0,4.0},
            {"PRISMARINE_SHARD",6.0,3.0},{"PRISMARINE_CRYSTALS",8.0,4.0},
            {"NAUTILUS_SHELL",30.0,15.0},{"HEART_OF_THE_SEA",200.0,100.0},
            {"ENDER_PEARL",20.0,10.0},{"ENDER_EYE",40.0,20.0},
            {"SHULKER_SHELL",50.0,25.0},{"DRAGON_BREATH",30.0,15.0},
            {"NETHER_STAR",1000.0,500.0},{"TOTEM_OF_UNDYING",300.0,150.0},
            {"WITHER_SKELETON_SKULL",200.0,100.0},
            {"TURTLE_SCUTE",15.0,7.5},{"ARMADILLO_SCUTE",12.0,6.0},
            // ── Plants & Nature ──
            {"OAK_LEAVES",2.0,1.0},{"SPRUCE_LEAVES",0.3,0.1},{"BIRCH_LEAVES",0.3,0.1},
            {"JUNGLE_LEAVES",0.3,0.1},{"ACACIA_LEAVES",0.3,0.1},{"DARK_OAK_LEAVES",0.3,0.1},
            {"VINE",2.0,1.0},{"KELP",0.5,0.2},{"DRIED_KELP",0.3,0.1},{"DRIED_KELP_BLOCK",2.0,0.8},
            {"SEA_PICKLE",2.0,0.8},{"LILY_PAD",3.0,1.0},{"CACTUS",3.0,1.0},
            {"FLOWER_POT",5.0,2.0},{"POPPY",2.0,1.0},{"DANDELION",2.0,1.0},
            {"FERN",1.0,0.0},{"GRASS",1.0,0.0},{"DEAD_BUSH",1.0,0.0},
            {"BROWN_MUSHROOM",4.0,2.0},{"RED_MUSHROOM",4.0,2.0},
            {"BROWN_MUSHROOM_BLOCK",3.0,1.0},{"RED_MUSHROOM_BLOCK",3.0,1.0},{"MUSHROOM_STEM",3.0,1.0},
            // ── Utility items ──
            {"BOOK",8.0,4.0},{"BOOKSHELF",20.0,10.0},{"PAPER",1.0,0.4},
            {"STICK",1.0,0.0},{"WOODEN_SWORD",3.0,1.0},{"STONE_SWORD",8.0,3.0},
            {"IRON_SWORD",25.0,10.0},{"GOLDEN_SWORD",30.0,12.0},{"DIAMOND_SWORD",150.0,60.0},
            {"IRON_PICKAXE",30.0,12.0},{"DIAMOND_PICKAXE",200.0,80.0},
            {"IRON_AXE",25.0,10.0},{"DIAMOND_AXE",180.0,72.0},
            {"IRON_SHOVEL",20.0,8.0},{"DIAMOND_SHOVEL",150.0,60.0},
            {"IRON_HOE",15.0,6.0},{"DIAMOND_HOE",120.0,48.0},
            {"IRON_HELMET",40.0,16.0},{"IRON_CHESTPLATE",60.0,24.0},
            {"IRON_LEGGINGS",55.0,22.0},{"IRON_BOOTS",35.0,14.0},
            {"DIAMOND_HELMET",200.0,80.0},{"DIAMOND_CHESTPLATE",300.0,120.0},
            {"DIAMOND_LEGGINGS",275.0,110.0},{"DIAMOND_BOOTS",175.0,70.0},
            {"LEATHER_HELMET",10.0,4.0},{"LEATHER_CHESTPLATE",15.0,6.0},
            {"LEATHER_LEGGINGS",12.0,5.0},{"LEATHER_BOOTS",8.0,3.0},
            {"GOLDEN_HELMET",25.0,10.0},{"GOLDEN_CHESTPLATE",40.0,16.0},
            {"GOLDEN_LEGGINGS",35.0,14.0},{"GOLDEN_BOOTS",20.0,8.0},
            {"SHIELD",30.0,12.0},{"BOW",25.0,10.0},{"CROSSBOW",40.0,16.0},
            {"ARROW",1.0,0.4},{"SPECTRAL_ARROW",3.0,1.2},
            {"FISHING_ROD",8.0,3.0},{"FLINT_AND_STEEL",10.0,4.0},
            {"BUCKET",8.0,3.0},{"WATER_BUCKET",10.0,4.0},{"LAVA_BUCKET",12.0,5.0},
            {"SADDLE",30.0,15.0},{"LEAD",8.0,4.0},{"NAME_TAG",20.0,10.0},
            {"CLOCK",20.0,8.0},{"COMPASS",10.0,4.0},
            {"TORCH",2.0,1.0},{"LANTERN",12.0,6.0},{"SOUL_LANTERN",14.0,7.0},
            {"GLOWSTONE",15.0,7.0},{"GLOWSTONE_DUST",2.5,1.0},{"SEA_LANTERN",20.0,10.0},
            {"CHEST",15.0,7.0},{"BARREL",20.0,10.0},{"FURNACE",15.0,7.0},
            {"CRAFTING_TABLE",10.0,5.0},{"ANVIL",80.0,35.0},
            {"ENCHANTING_TABLE",200.0,100.0},
            {"BREWING_STAND",40.0,20.0},{"CAULDRON",15.0,6.0},
            {"REPEATER",5.0,2.0},{"COMPARATOR",8.0,3.0},
            {"PISTON",8.0,3.0},{"STICKY_PISTON",12.0,5.0},{"OBSERVER",10.0,4.0},
            {"DISPENSER",12.0,5.0},{"DROPPER",8.0,3.0},{"HOPPER",15.0,6.0},
            {"TNT",10.0,4.0},{"FLINT",2.0,0.8},
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
            {"GLASS_BOTTLE",1.0,0.4},
            // sell-only (unlimited farm items)
            {"ROTTEN_FLESH",-1.0,0.2},{"BONE",-1.0,0.5},
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
