package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EconomyManager {

    private final KlassenPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, Double> balances = new HashMap<>();
    private double totalSupply = 0.0;
    private int registeredPlayers = 0;

    public EconomyManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "economy.yml");
        load();
    }

    public void load() {
        if (!dataFile.exists()) {
            try { plugin.getDataFolder().mkdirs(); dataFile.createNewFile(); }
            catch (IOException e) { plugin.getLogger().severe("Fehler economy.yml: " + e.getMessage()); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        balances.clear();
        totalSupply = dataConfig.getDouble("total-supply", 0.0);
        registeredPlayers = dataConfig.getInt("registered-players", 0);
        if (dataConfig.contains("balances")) {
            for (String key : dataConfig.getConfigurationSection("balances").getKeys(false)) {
                try { balances.put(UUID.fromString(key), dataConfig.getDouble("balances." + key, 0.0)); }
                catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        dataConfig.set("total-supply", totalSupply);
        dataConfig.set("registered-players", registeredPlayers);
        dataConfig.set("balances", null);
        for (Map.Entry<UUID, Double> e : balances.entrySet()) {
            dataConfig.set("balances." + e.getKey(), e.getValue());
        }
        try { dataConfig.save(dataFile); }
        catch (IOException e) { plugin.getLogger().severe("Fehler Economy save: " + e.getMessage()); }
    }

    public double getBalance(UUID id) {
        if (!balances.containsKey(id)) {
            double start = plugin.getConfig().getDouble("economy.starting-balance", 500.0);
            balances.put(id, start);
            totalSupply += start;
            registeredPlayers++;
            save();
        }
        return balances.get(id);
    }

    public boolean withdraw(UUID id, double amount) {
        if (amount <= 0) return false;
        double cur = getBalance(id);
        if (cur < amount) return false;
        balances.put(id, cur - amount);
        totalSupply -= amount;
        return true;
    }

    public void deposit(UUID id, double amount) {
        if (amount <= 0) return;
        balances.put(id, getBalance(id) + amount);
        totalSupply += amount;
    }

    public boolean has(UUID id, double amount) { return getBalance(id) >= amount; }

    /** Save to disk asynchronously. */
    public void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public double getInflationMultiplier() {
        if (registeredPlayers <= 0) return 1.0;
        double target = plugin.getConfig().getDouble("economy.inflation.target-balance-per-player", 1000.0);
        if (target <= 0) return 1.0;
        double raw = totalSupply / (registeredPlayers * target);
        return Math.max(0.5, Math.min(3.0, raw));
    }

    public double getTotalSupply() { return totalSupply; }
    public int getRegisteredPlayers() { return registeredPlayers; }
    public Map<UUID, Double> getAllBalances() { return Collections.unmodifiableMap(balances); }

    public String format(double amount) {
        return String.format("%.2f %s", amount, plugin.getConfig().getString("economy.currency-name", "Coins"));
    }
}
