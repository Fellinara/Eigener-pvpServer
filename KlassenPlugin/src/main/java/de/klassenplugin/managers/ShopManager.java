package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
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
        Object[][] defaults = {
            {"DIAMOND",150.0,80.0},{"GOLD_INGOT",20.0,10.0},{"IRON_INGOT",8.0,4.0},
            {"COAL",3.0,1.5},{"OAK_LOG",2.0,1.0},{"WHEAT",1.0,0.5},{"BREAD",3.0,1.5},
            {"APPLE",2.0,1.0},{"COBBLESTONE",0.5,0.2},{"SAND",1.0,0.5},{"GRAVEL",1.0,0.5},
            {"EMERALD",100.0,50.0},{"NETHERITE_INGOT",500.0,250.0},{"ANCIENT_DEBRIS",400.0,200.0},
            {"COOKED_BEEF",5.0,2.5},{"ARROW",0.5,0.2},{"STRING",1.0,0.5},{"FEATHER",1.0,0.5},{"BONE",0.5,0.2}
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
        return p[0] * plugin.getEconomyManager().getInflationMultiplier();
    }

    public double getSellPrice(String mat) {
        double[] p = items.get(mat.toUpperCase());
        if (p == null || p[1] < 0) return -1;
        double boost = plugin.getWeeklyChangelogManager() != null ? plugin.getWeeklyChangelogManager().getBoostMultiplier(mat.toUpperCase()) : 1.0;
        return p[1] * plugin.getEconomyManager().getInflationMultiplier() * boost;
    }

    public double getBaseBuyPrice(String mat) { double[] p = items.get(mat.toUpperCase()); return p == null ? -1 : p[0]; }
    public double getBaseSellPrice(String mat) { double[] p = items.get(mat.toUpperCase()); return p == null ? -1 : p[1]; }
    public boolean hasItem(String mat) { return items.containsKey(mat.toUpperCase()); }
    public Set<String> getItemNames() { return Collections.unmodifiableSet(items.keySet()); }
    public void setItem(String mat, double buy, double sell) { items.put(mat.toUpperCase(), new double[]{buy, sell}); save(); }
    public void removeItem(String mat) { items.remove(mat.toUpperCase()); save(); }
}
