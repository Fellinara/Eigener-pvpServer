package de.duellplugin.models;

import java.util.*;

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

    /** All members of team 1 (always includes player1). */
    private final List<UUID> team1;
    /** All members of team 2 (always includes player2). */
    private final List<UUID> team2;
    /** Players who have been eliminated (died) during this duel. */
    private final Set<UUID> eliminated;

    public Duel(UUID player1, UUID player2, String arenaName, String kitName) {
        this.player1 = player1;
        this.player2 = player2;
        this.arenaName = arenaName;
        this.kitName = kitName;
        this.state = DuelState.REQUESTED;
        this.startTime = System.currentTimeMillis();
        this.botDuel = false;
        this.botLevel = 0;
        this.team1 = new ArrayList<>(Collections.singletonList(player1));
        this.team2 = new ArrayList<>(Collections.singletonList(player2));
        this.eliminated = new HashSet<>();
    }

    /** Creates a team duel. team1[0] acts as the "player1" representative. */
    public Duel(List<UUID> team1, List<UUID> team2, String arenaName, String kitName) {
        this.team1 = new ArrayList<>(team1);
        this.team2 = new ArrayList<>(team2);
        this.player1 = team1.get(0);
        this.player2 = team2.get(0);
        this.arenaName = arenaName;
        this.kitName = kitName;
        this.state = DuelState.REQUESTED;
        this.startTime = System.currentTimeMillis();
        this.botDuel = false;
        this.botLevel = 0;
        this.eliminated = new HashSet<>();
    }

    public UUID getPlayer1() { return player1; }
    public UUID getPlayer2() { return player2; }
    public String getArenaName() { return arenaName; }
    public String getKitName() { return kitName; }
    public DuelState getState() { return state; }
    public void setState(DuelState state) { this.state = state; }
    public long getStartTime() { return startTime; }
    public boolean isBotDuel() { return botDuel; }
    public void setBotDuel(boolean botDuel) { this.botDuel = botDuel; }
    public int getBotLevel() { return botLevel; }
    public void setBotLevel(int botLevel) { this.botLevel = botLevel; }

    /** Returns true if this is a team duel (at least one team has more than 1 member). */
    public boolean isTeamDuel() {
        return team1.size() > 1 || team2.size() > 1;
    }

    /** Returns a combined list of all participants. */
    public List<UUID> getAllMembers() {
        List<UUID> all = new ArrayList<>(team1);
        all.addAll(team2);
        return all;
    }

    /**
     * Returns which team number (1 or 2) the given player is on, or 0 if not found.
     */
    public int getTeamNumber(UUID uuid) {
        if (team1.contains(uuid)) return 1;
        if (team2.contains(uuid)) return 2;
        return 0;
    }

    /**
     * Returns the UUIDs of the team opposing the given player, or empty list.
     */
    public List<UUID> getOpponentTeam(UUID uuid) {
        if (team1.contains(uuid)) return Collections.unmodifiableList(team2);
        if (team2.contains(uuid)) return Collections.unmodifiableList(team1);
        return Collections.emptyList();
    }

    /** Marks a player as eliminated. */
    public void eliminatePlayer(UUID uuid) {
        eliminated.add(uuid);
    }

    public boolean isEliminated(UUID uuid) {
        return eliminated.contains(uuid);
    }

    /** Returns true if at least one member of the given team (1 or 2) is still alive. */
    public boolean isTeamAlive(int teamNumber) {
        List<UUID> team = (teamNumber == 1) ? team1 : team2;
        for (UUID m : team) {
            if (!eliminated.contains(m)) return true;
        }
        return false;
    }

    /** Returns alive (non-eliminated) members of team 1. */
    public List<UUID> getAliveTeam1() {
        return getAliveMembers(team1);
    }

    /** Returns alive (non-eliminated) members of team 2. */
    public List<UUID> getAliveTeam2() {
        return getAliveMembers(team2);
    }

    private List<UUID> getAliveMembers(List<UUID> team) {
        List<UUID> alive = new ArrayList<>();
        for (UUID m : team) {
            if (!eliminated.contains(m)) alive.add(m);
        }
        return alive;
    }

    public boolean isParticipant(UUID uuid) {
        return team1.contains(uuid) || team2.contains(uuid);
    }

    /** For 1v1 backward compat – returns the single opponent UUID or null. */
    public UUID getOpponent(UUID uuid) {
        if (isTeamDuel()) {
            List<UUID> alive = new ArrayList<>(getOpponentTeam(uuid));
            alive.removeAll(eliminated);
            return alive.isEmpty() ? null : alive.get(0);
        }
        if (player1.equals(uuid)) return player2;
        if (player2.equals(uuid)) return player1;
        return null;
    }
}
