package de.duellplugin.models;

import org.bukkit.Location;
import org.bukkit.block.BlockState;

import java.util.List;

public class Arena {

    private final String name;
    private Location spawn1;
    private Location spawn2;
    private boolean inUse;
    /** Corner 1 of the region used for arena snapshots. */
    private Location regionPos1;
    /** Corner 2 of the region used for arena snapshots. */
    private Location regionPos2;
    /** In-memory snapshot of all block states in the region (populated by ArenaManager). */
    private List<BlockState> snapshot;

    public Arena(String name, Location spawn1, Location spawn2) {
        this.name = name;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
        this.inUse = false;
    }

    public String getName() { return name; }

    public Location getSpawn1() { return spawn1; }
    public void setSpawn1(Location spawn1) { this.spawn1 = spawn1; }

    public Location getSpawn2() { return spawn2; }
    public void setSpawn2(Location spawn2) { this.spawn2 = spawn2; }

    public boolean isInUse() { return inUse; }
    public void setInUse(boolean inUse) { this.inUse = inUse; }

    public Location getRegionPos1() { return regionPos1; }
    public void setRegionPos1(Location regionPos1) { this.regionPos1 = regionPos1; }

    public Location getRegionPos2() { return regionPos2; }
    public void setRegionPos2(Location regionPos2) { this.regionPos2 = regionPos2; }

    public boolean isRegionDefined() {
        return regionPos1 != null && regionPos2 != null
                && regionPos1.getWorld() != null
                && regionPos1.getWorld().equals(regionPos2.getWorld());
    }

    public List<BlockState> getSnapshot() { return snapshot; }
    public void setSnapshot(List<BlockState> snapshot) { this.snapshot = snapshot; }

    public boolean isReady() {
        return spawn1 != null && spawn2 != null;
    }
}
