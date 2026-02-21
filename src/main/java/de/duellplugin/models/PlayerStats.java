package de.duellplugin.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerStats {

    private final UUID uuid;
    private String name;
    private int elo;
    private int wins;
    private int losses;
    private int draws;
    private int killStreak;
    private int bestKillStreak;
    private int botWins;
    private int botLosses;
    private int highestBotLevel;
    private String selectedKit;
    private Rank rank;
    private List<String> kitOrder;
    /** Per-kit custom inventory slot layout. Key = kit name, value = 36-element slot permutation. */
    private final Map<String, int[]> kitSlotLayouts;

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.elo = 1000;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.killStreak = 0;
        this.bestKillStreak = 0;
        this.botWins = 0;
        this.botLosses = 0;
        this.highestBotLevel = 0;
        this.selectedKit = "nodebuff";
        this.rank = Rank.SPIELER;
        this.kitOrder = new ArrayList<>();
        this.kitSlotLayouts = new HashMap<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = Math.max(0, elo);
    }

    public int getWins() {
        return wins;
    }

    public void addWin() {
        this.wins++;
        this.killStreak++;
        if (this.killStreak > this.bestKillStreak) {
            this.bestKillStreak = this.killStreak;
        }
    }

    public void loadWins(int wins) {
        this.wins = wins;
    }

    public void loadLosses(int losses) {
        this.losses = losses;
    }

    public void loadBestKillStreak(int bestKillStreak) {
        this.bestKillStreak = bestKillStreak;
    }

    public void loadBotWins(int botWins) {
        this.botWins = botWins;
    }

    public void loadBotLosses(int botLosses) {
        this.botLosses = botLosses;
    }

    public int getLosses() {
        return losses;
    }

    public void addLoss() {
        this.losses++;
        this.killStreak = 0;
    }

    public int getDraws() {
        return draws;
    }

    public void addDraw() {
        this.draws++;
    }

    public int getKillStreak() {
        return killStreak;
    }

    public int getBestKillStreak() {
        return bestKillStreak;
    }

    public int getBotWins() {
        return botWins;
    }

    public void addBotWin() {
        this.botWins++;
    }

    public int getBotLosses() {
        return botLosses;
    }

    public void addBotLoss() {
        this.botLosses++;
    }

    public int getHighestBotLevel() {
        return highestBotLevel;
    }

    public void setHighestBotLevel(int level) {
        if (level > this.highestBotLevel) {
            this.highestBotLevel = level;
        }
    }

    public String getSelectedKit() {
        return selectedKit;
    }

    public void setSelectedKit(String selectedKit) {
        this.selectedKit = selectedKit;
    }

    public int getTotalGames() {
        return wins + losses + draws;
    }

    public double getWinRate() {
        if (getTotalGames() == 0) return 0.0;
        return (double) wins / getTotalGames() * 100.0;
    }

    public double getKD() {
        if (losses == 0) return wins;
        return (double) wins / losses;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public List<String> getKitOrder() {
        return kitOrder;
    }

    public void setKitOrder(List<String> kitOrder) {
        this.kitOrder = kitOrder != null ? kitOrder : new ArrayList<>();
    }

    /** Moves a kit name one position earlier in the preferred order. */
    public void moveKitUp(String kitName) {
        int idx = kitOrder.indexOf(kitName);
        if (idx > 0) {
            kitOrder.remove(idx);
            kitOrder.add(idx - 1, kitName);
        } else if (idx < 0 && !kitOrder.isEmpty()) {
            kitOrder.add(0, kitName);
        }
    }

    /** Moves a kit name one position later in the preferred order. */
    public void moveKitDown(String kitName) {
        int idx = kitOrder.indexOf(kitName);
        if (idx >= 0 && idx < kitOrder.size() - 1) {
            kitOrder.remove(idx);
            kitOrder.add(idx + 1, kitName);
        } else if (idx < 0) {
            kitOrder.add(kitName);
        }
    }

    // ── Kit slot layout ──────────────────────────────────────────

    /**
     * Returns the custom slot layout for a kit, or null if none has been saved.
     * The array has 36 entries: {@code layout[sourceSlot] = targetSlot}.
     * A value of -1 means "keep in the original slot".
     */
    public int[] getKitSlotLayout(String kitName) {
        return kitSlotLayouts.get(kitName.toLowerCase());
    }

    /** Stores a custom slot layout for a kit. */
    public void setKitSlotLayout(String kitName, int[] layout) {
        kitSlotLayouts.put(kitName.toLowerCase(), layout);
    }

    /** Removes the custom slot layout for a kit. */
    public void removeKitSlotLayout(String kitName) {
        kitSlotLayouts.remove(kitName.toLowerCase());
    }

    public Map<String, int[]> getKitSlotLayouts() {
        return kitSlotLayouts;
    }
}
