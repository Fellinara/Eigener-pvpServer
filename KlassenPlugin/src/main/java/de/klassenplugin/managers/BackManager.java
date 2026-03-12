package de.klassenplugin.managers;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores the last known "return" location per player.
 * Updated on every teleport and on death.
 */
public class BackManager {

    private final Map<UUID, Location> lastLocations = new HashMap<>();

    public void setLastLocation(UUID playerId, Location location) {
        if (location != null) {
            lastLocations.put(playerId, location.clone());
        }
    }

    public Location getLastLocation(UUID playerId) {
        return lastLocations.get(playerId);
    }

    public boolean hasLastLocation(UUID playerId) {
        return lastLocations.containsKey(playerId);
    }

    public void clearLastLocation(UUID playerId) {
        lastLocations.remove(playerId);
    }
}
