package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.PlayerStats;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StatsManager {

    private final DuellPlugin plugin;
    private final Map<UUID, PlayerStats> statsMap;
    private final File statsFile;
    private FileConfiguration statsConfig;
    private final int kFactor;

    public StatsManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.statsMap = new HashMap<>();
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        this.kFactor = plugin.getConfig().getInt("elo.k-factor", 32);
        loadStats();
    }

    public PlayerStats getStats(UUID uuid) {
        return statsMap.get(uuid);
    }

    public PlayerStats getOrCreateStats(UUID uuid, String name) {
        return statsMap.computeIfAbsent(uuid, k -> new PlayerStats(uuid, name));
    }

    public void processWin(UUID winner, UUID loser) {
        PlayerStats winnerStats = statsMap.get(winner);
        PlayerStats loserStats = statsMap.get(loser);

        if (winnerStats == null || loserStats == null) return;

        int[] newElo = calculateElo(winnerStats.getElo(), loserStats.getElo(), true);
        winnerStats.setElo(newElo[0]);
        loserStats.setElo(newElo[1]);

        winnerStats.addWin();
        loserStats.addLoss();

        saveStats();
    }

    public void processBotWin(UUID player, int botLevel) {
        PlayerStats stats = statsMap.get(player);
        if (stats == null) return;

        stats.addBotWin();
        stats.setHighestBotLevel(botLevel);

        int eloGain = Math.max(1, botLevel / 5);
        stats.setElo(stats.getElo() + eloGain);

        saveStats();
    }

    public void processBotLoss(UUID player) {
        PlayerStats stats = statsMap.get(player);
        if (stats == null) return;

        stats.addBotLoss();
        saveStats();
    }

    private int[] calculateElo(int winnerElo, int loserElo, boolean winnerWon) {
        double expectedWinner = 1.0 / (1.0 + Math.pow(10, (loserElo - winnerElo) / 400.0));
        double expectedLoser = 1.0 / (1.0 + Math.pow(10, (winnerElo - loserElo) / 400.0));

        int newWinnerElo = (int) Math.round(winnerElo + kFactor * (1.0 - expectedWinner));
        int newLoserElo = (int) Math.round(loserElo + kFactor * (0.0 - expectedLoser));

        return new int[]{Math.max(0, newWinnerElo), Math.max(0, newLoserElo)};
    }

    public List<PlayerStats> getTopPlayers(int limit) {
        return statsMap.values().stream()
                .sorted(Comparator.comparingInt(PlayerStats::getElo).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void loadStats() {
        if (!statsFile.exists()) return;

        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        ConfigurationSection section = statsConfig.getConfigurationSection("players");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection ps = section.getConfigurationSection(uuidStr);
                if (ps == null) continue;

                PlayerStats stats = new PlayerStats(uuid, ps.getString("name", "Unknown"));
                stats.setElo(ps.getInt("elo", 1000));
                for (int i = 0; i < ps.getInt("wins", 0); i++) stats.addWin();
                // Reset killstreak after loading wins since addWin increments it
                for (int i = 0; i < ps.getInt("losses", 0); i++) stats.addLoss();
                stats.setSelectedKit(ps.getString("kit", "swordsman"));
                for (int i = 0; i < ps.getInt("bot-wins", 0); i++) stats.addBotWin();
                for (int i = 0; i < ps.getInt("bot-losses", 0); i++) stats.addBotLoss();
                stats.setHighestBotLevel(ps.getInt("highest-bot-level", 0));

                statsMap.put(uuid, stats);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ungültige UUID in stats.yml: " + uuidStr);
            }
        }
        plugin.getLogger().info(statsMap.size() + " Spieler-Statistiken geladen.");
    }

    public void saveStats() {
        statsConfig = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> entry : statsMap.entrySet()) {
            String path = "players." + entry.getKey().toString();
            PlayerStats stats = entry.getValue();

            statsConfig.set(path + ".name", stats.getName());
            statsConfig.set(path + ".elo", stats.getElo());
            statsConfig.set(path + ".wins", stats.getWins());
            statsConfig.set(path + ".losses", stats.getLosses());
            statsConfig.set(path + ".draws", stats.getDraws());
            statsConfig.set(path + ".kit", stats.getSelectedKit());
            statsConfig.set(path + ".best-streak", stats.getBestKillStreak());
            statsConfig.set(path + ".bot-wins", stats.getBotWins());
            statsConfig.set(path + ".bot-losses", stats.getBotLosses());
            statsConfig.set(path + ".highest-bot-level", stats.getHighestBotLevel());
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Statistiken: " + e.getMessage());
        }
    }
}
