package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WeeklyChangelogManager {

    private final KlassenPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<String, Double> currentBoosts = new HashMap<>();
    private final Map<String, Double> lastWeekBoosts = new HashMap<>();
    private long currentWeekStart = 0L;
    private int weekNumber = 0;

    public WeeklyChangelogManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "changelog.yml");
        load();
        scheduleWeeklyCheck();
    }

    public void load() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Fehler beim Erstellen von changelog.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        currentWeekStart = dataConfig.getLong("current-week-start", 0L);
        weekNumber = dataConfig.getInt("week-number", 0);
        currentBoosts.clear(); lastWeekBoosts.clear();
        if (dataConfig.contains("current-boosts"))
            for (String k : dataConfig.getConfigurationSection("current-boosts").getKeys(false))
                currentBoosts.put(k, dataConfig.getDouble("current-boosts." + k, 1.0));
        if (dataConfig.contains("last-week-boosts"))
            for (String k : dataConfig.getConfigurationSection("last-week-boosts").getKeys(false))
                lastWeekBoosts.put(k, dataConfig.getDouble("last-week-boosts." + k, 1.0));
        if (currentWeekStart == 0L) rollNewWeek(false);
        else {
            long dur = plugin.getConfig().getLong("changelog.week-duration-hours", 168L) * 3600_000L;
            if (System.currentTimeMillis() - currentWeekStart > dur) rollNewWeek(false);
        }
    }

    public void save() {
        dataConfig.set("current-week-start", currentWeekStart);
        dataConfig.set("week-number", weekNumber);
        dataConfig.set("current-boosts", null);
        for (Map.Entry<String, Double> e : currentBoosts.entrySet()) dataConfig.set("current-boosts." + e.getKey(), e.getValue());
        dataConfig.set("last-week-boosts", null);
        for (Map.Entry<String, Double> e : lastWeekBoosts.entrySet()) dataConfig.set("last-week-boosts." + e.getKey(), e.getValue());
        try { dataConfig.save(dataFile); } catch (IOException e) { plugin.getLogger().severe("changelog save: " + e.getMessage()); }
    }

    public void rollNewWeek(boolean broadcast) {
        lastWeekBoosts.clear(); lastWeekBoosts.putAll(currentBoosts); currentBoosts.clear();
        currentWeekStart = System.currentTimeMillis(); weekNumber++;
        List<String> shopItems = new ArrayList<>(plugin.getShopManager().getItemNames());
        Collections.shuffle(shopItems, new Random());
        int count = Math.min(plugin.getConfig().getInt("changelog.boosts-per-week", 4), shopItems.size());
        double minB = plugin.getConfig().getDouble("changelog.min-boost", 1.3);
        double maxB = plugin.getConfig().getDouble("changelog.max-boost", 2.5);
        Random rnd = new Random();
        for (int i = 0; i < count; i++) {
            double b = Math.round((minB + (maxB - minB) * rnd.nextDouble()) * 10.0) / 10.0;
            currentBoosts.put(shopItems.get(i), b);
        }
        save();
        if (broadcast) {
            Bukkit.broadcast(KlassenPlugin.colorizeComponent("&8[&6Wöchentlicher Changelog&8] &eWoche &b" + weekNumber));
            Bukkit.broadcast(KlassenPlugin.colorizeComponent("&eNeue Preisboosts:"));
            for (Map.Entry<String, Double> e : currentBoosts.entrySet())
                Bukkit.broadcast(KlassenPlugin.colorizeComponent("  &b" + e.getKey() + " &7: &a+" + String.format("%.0f%%", (e.getValue()-1)*100) + " Verkaufsbonus"));
            if (!lastWeekBoosts.isEmpty())
                Bukkit.broadcast(KlassenPlugin.colorizeComponent("&7Letzte Woche zurückgesetzt: &c" + String.join(", ", lastWeekBoosts.keySet())));
        }
    }

    private void scheduleWeeklyCheck() {
        long tph = 20L * 60L * 60L;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long dur = plugin.getConfig().getLong("changelog.week-duration-hours", 168L) * 3600_000L;
            if (System.currentTimeMillis() - currentWeekStart > dur) rollNewWeek(true);
        }, tph, tph);
    }

    public double getBoostMultiplier(String mat) { return currentBoosts.getOrDefault(mat.toUpperCase(), 1.0); }
    public Map<String, Double> getCurrentBoosts() { return Collections.unmodifiableMap(currentBoosts); }
    public Map<String, Double> getLastWeekBoosts() { return Collections.unmodifiableMap(lastWeekBoosts); }
    public int getWeekNumber() { return weekNumber; }
    public long getCurrentWeekStart() { return currentWeekStart; }
    public void forceRollNewWeek() { rollNewWeek(true); }
}
