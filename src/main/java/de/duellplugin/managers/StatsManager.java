package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.CustomKit;
import de.duellplugin.models.PlayerStats;
import de.duellplugin.models.Rank;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
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

    /** Looks up player stats by display name (case-insensitive). Returns null if not found. */
    public PlayerStats getStatsByName(String name) {
        for (PlayerStats stats : statsMap.values()) {
            if (stats.getName().equalsIgnoreCase(name)) return stats;
        }
        return null;
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
                stats.loadWins(ps.getInt("wins", 0));
                stats.loadLosses(ps.getInt("losses", 0));
                stats.loadBestKillStreak(ps.getInt("best-streak", 0));
                stats.setSelectedKit(ps.getString("kit", "nodebuff"));
                stats.loadBotWins(ps.getInt("bot-wins", 0));
                stats.loadBotLosses(ps.getInt("bot-losses", 0));
                stats.setHighestBotLevel(ps.getInt("highest-bot-level", 0));

                String rankName = ps.getString("rank", "SPIELER");
                try {
                    stats.setRank(Rank.valueOf(rankName));
                } catch (IllegalArgumentException ignored) {
                    stats.setRank(Rank.SPIELER);
                }

                List<String> kitOrder = ps.getStringList("kit-order");
                stats.setKitOrder(kitOrder.isEmpty() ? new ArrayList<>() : new ArrayList<>(kitOrder));

                // Load kit slot layouts (stored as "kitname:0,1,2,...35" entries)
                ConfigurationSection layoutsSection = ps.getConfigurationSection("kit-slot-layouts");
                if (layoutsSection != null) {
                    for (String kitName : layoutsSection.getKeys(false)) {
                        List<Integer> slotList = layoutsSection.getIntegerList(kitName);
                        if (slotList.size() == 36) {
                            int[] arr = new int[36];
                            for (int i = 0; i < 36; i++) arr[i] = slotList.get(i);
                            stats.setKitSlotLayout(kitName, arr);
                        }
                    }
                }

                // Load custom kits
                ConfigurationSection customKitsSection = ps.getConfigurationSection("custom-kits");
                if (customKitsSection != null) {
                    for (String kitName : customKitsSection.getKeys(false)) {
                        ConfigurationSection ks = customKitsSection.getConfigurationSection(kitName);
                        if (ks == null) continue;
                        ItemStack[] contents = new ItemStack[36];
                        ItemStack[] armor    = new ItemStack[4];
                        ConfigurationSection csSection = ks.getConfigurationSection("contents");
                        if (csSection != null) {
                            for (String slotStr : csSection.getKeys(false)) {
                                try {
                                    int slot = Integer.parseInt(slotStr);
                                    if (slot >= 0 && slot < 36) contents[slot] = csSection.getItemStack(slotStr);
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                        ConfigurationSection arSection = ks.getConfigurationSection("armor");
                        if (arSection != null) {
                            for (String slotStr : arSection.getKeys(false)) {
                                try {
                                    int slot = Integer.parseInt(slotStr);
                                    if (slot >= 0 && slot < 4) armor[slot] = arSection.getItemStack(slotStr);
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                        ItemStack offHand = ks.getItemStack("offhand");
                        stats.putCustomKit(kitName, new CustomKit(kitName, contents, armor, offHand));
                    }
                }

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
            statsConfig.set(path + ".rank", stats.getRank().name());
            statsConfig.set(path + ".kit-order", stats.getKitOrder());

            // Save kit slot layouts
            for (Map.Entry<String, int[]> layoutEntry : stats.getKitSlotLayouts().entrySet()) {
                List<Integer> slotList = new ArrayList<>();
                for (int s : layoutEntry.getValue()) slotList.add(s);
                statsConfig.set(path + ".kit-slot-layouts." + layoutEntry.getKey(), slotList);
            }

            // Save custom kits
            for (Map.Entry<String, CustomKit> ckEntry : stats.getCustomKits().entrySet()) {
                String ckPath = path + ".custom-kits." + ckEntry.getKey();
                CustomKit ck = ckEntry.getValue();
                ItemStack[] ckContents = ck.getContents();
                for (int i = 0; i < 36; i++) {
                    if (ckContents[i] != null && ckContents[i].getType() != Material.AIR) {
                        statsConfig.set(ckPath + ".contents." + i, ckContents[i]);
                    }
                }
                ItemStack[] ckArmor = ck.getArmor();
                for (int i = 0; i < 4; i++) {
                    if (ckArmor[i] != null && ckArmor[i].getType() != Material.AIR) {
                        statsConfig.set(ckPath + ".armor." + i, ckArmor[i]);
                    }
                }
                if (ck.getOffHandItem() != null) {
                    statsConfig.set(ckPath + ".offhand", ck.getOffHandItem());
                }
            }
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Statistiken: " + e.getMessage());
        }
    }
}
