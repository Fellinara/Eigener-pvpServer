package de.duellplugin.models;

import org.bukkit.Location;

public class Arena {

    private final String name;
    private Location spawn1;
    private Location spawn2;
    private boolean inUse;

    public Arena(String name, Location spawn1, Location spawn2) {
        this.name = name;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
        this.inUse = false;
    }

    public String getName() {
        return name;
    }

    public Location getSpawn1() {
        return spawn1;
    }

    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1;
    }

    public Location getSpawn2() {
        return spawn2;
    }

    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public boolean isReady() {
        return spawn1 != null && spawn2 != null;
    }
}
