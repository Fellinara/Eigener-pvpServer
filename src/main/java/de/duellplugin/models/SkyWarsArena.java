package de.duellplugin.models;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class SkyWarsArena {

    private final String name;
    private final List<Location> spawns;
    private final List<Location> chestLocations;
    private int minPlayers;
    private int maxPlayers;
    private boolean inUse;

    public SkyWarsArena(String name) {
        this.name = name;
        this.spawns = new ArrayList<>();
        this.chestLocations = new ArrayList<>();
        this.minPlayers = 5;
        this.maxPlayers = 12;
        this.inUse = false;
    }

    public String getName() { return name; }
    public List<Location> getSpawns() { return spawns; }
    public List<Location> getChestLocations() { return chestLocations; }

    public void addSpawn(Location loc) { spawns.add(loc.clone()); }
    public void addChestLocation(Location loc) { chestLocations.add(loc.clone()); }

    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int n) { this.minPlayers = Math.max(2, n); }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int n) { this.maxPlayers = Math.max(2, n); }

    public boolean isInUse() { return inUse; }
    public void setInUse(boolean inUse) { this.inUse = inUse; }

    /** Returns true if the arena has at least 2 spawns configured. */
    public boolean isReady() { return spawns.size() >= 2; }
}
