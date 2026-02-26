package de.duellplugin.models;

import java.util.*;

public class SkyWarsGame {

    public enum State { WAITING, COUNTDOWN, ACTIVE, ENDED }

    private final String arenaName;
    private final List<UUID> playerList;       // ordered list of joined human players
    private final Set<UUID> alivePlayers;      // currently alive (human + bots)
    private final Set<UUID> spectators;
    private final Set<UUID> botUuids;          // entity UUIDs of spawned SkyWars bots
    private final boolean botGame;
    private State state;
    private int countdownSeconds;

    public SkyWarsGame(String arenaName, boolean botGame) {
        this.arenaName = arenaName;
        this.playerList = new ArrayList<>();
        this.alivePlayers = new LinkedHashSet<>();
        this.spectators = new HashSet<>();
        this.botUuids = new HashSet<>();
        this.botGame = botGame;
        this.state = State.WAITING;
        this.countdownSeconds = 30;
    }

    public String getArenaName() { return arenaName; }
    public boolean isBotGame() { return botGame; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public int getCountdownSeconds() { return countdownSeconds; }
    public void setCountdownSeconds(int n) { this.countdownSeconds = n; }

    // ── Player management ──────────────────────────────────────────────────

    public void addPlayer(UUID uuid) {
        if (!playerList.contains(uuid)) {
            playerList.add(uuid);
            alivePlayers.add(uuid);
        }
    }

    public void removePlayer(UUID uuid) {
        playerList.remove(uuid);
        alivePlayers.remove(uuid);
        spectators.remove(uuid);
    }

    public void eliminatePlayer(UUID uuid) {
        alivePlayers.remove(uuid);
        spectators.add(uuid);
    }

    public boolean isAlive(UUID uuid) { return alivePlayers.contains(uuid); }
    public boolean isInGame(UUID uuid) { return playerList.contains(uuid) || botUuids.contains(uuid); }
    public boolean isSpectating(UUID uuid) { return spectators.contains(uuid); }

    public List<UUID> getPlayerList() { return Collections.unmodifiableList(playerList); }
    public Set<UUID> getAlivePlayers() { return Collections.unmodifiableSet(alivePlayers); }
    public int getAliveCount() { return alivePlayers.size(); }
    public int getPlayerCount() { return playerList.size(); }

    // ── Bot management ─────────────────────────────────────────────────────

    public void addBot(UUID botEntityUuid) {
        botUuids.add(botEntityUuid);
        alivePlayers.add(botEntityUuid);
    }

    public void removeBot(UUID botEntityUuid) {
        botUuids.remove(botEntityUuid);
        alivePlayers.remove(botEntityUuid);
    }

    public boolean isBot(UUID uuid) { return botUuids.contains(uuid); }
    public Set<UUID> getBotUuids() { return Collections.unmodifiableSet(botUuids); }

    /** Returns the number of alive human players (excludes bots). */
    public long getAliveHumanCount() {
        return alivePlayers.stream().filter(u -> !botUuids.contains(u)).count();
    }

    /** Returns the number of alive bot entities. */
    public long getAliveBotCount() {
        return alivePlayers.stream().filter(botUuids::contains).count();
    }
}
