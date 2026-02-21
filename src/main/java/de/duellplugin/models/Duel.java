package de.duellplugin.models;

import java.util.UUID;

public class Duel {

    public enum DuelState {
        REQUESTED,
        COUNTDOWN,
        ACTIVE,
        ENDED
    }

    private final UUID player1;
    private final UUID player2;
    private final String arenaName;
    private final String kitName;
    private DuelState state;
    private final long startTime;
    private boolean botDuel;
    private int botLevel;

    public Duel(UUID player1, UUID player2, String arenaName, String kitName) {
        this.player1 = player1;
        this.player2 = player2;
        this.arenaName = arenaName;
        this.kitName = kitName;
        this.state = DuelState.REQUESTED;
        this.startTime = System.currentTimeMillis();
        this.botDuel = false;
        this.botLevel = 0;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public String getArenaName() {
        return arenaName;
    }

    public String getKitName() {
        return kitName;
    }

    public DuelState getState() {
        return state;
    }

    public void setState(DuelState state) {
        this.state = state;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean isBotDuel() {
        return botDuel;
    }

    public void setBotDuel(boolean botDuel) {
        this.botDuel = botDuel;
    }

    public int getBotLevel() {
        return botLevel;
    }

    public void setBotLevel(int botLevel) {
        this.botLevel = botLevel;
    }

    public boolean isParticipant(UUID uuid) {
        return player1.equals(uuid) || player2.equals(uuid);
    }

    public UUID getOpponent(UUID uuid) {
        if (player1.equals(uuid)) return player2;
        if (player2.equals(uuid)) return player1;
        return null;
    }
}
