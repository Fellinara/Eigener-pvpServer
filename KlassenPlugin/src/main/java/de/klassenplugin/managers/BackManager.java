package de.klassenplugin.managers;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stores the last known "return" location per player.
 * Updated on every teleport and on natural death.
 * Tracks whether the most recent death was caused by another player
 * so {@code /back} can be blocked after PvP deaths.
 */
public class BackManager {

    private final Map<UUID, Location> lastLocations = new HashMap<>();
    /** Players whose last death was caused by another player. /back is blocked for them. */
    private final Set<UUID> pvpDeaths = new HashSet<>();

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

    // ── PvP death tracking ────────────────────────────────────────────────────

    /** Mark that this player's last death was a PvP kill (blocks /back). */
    public void markPvpDeath(UUID playerId) {
        pvpDeaths.add(playerId);
    }

    /** Returns true if the player's most recent death was caused by another player. */
    public boolean isPvpDeath(UUID playerId) {
        return pvpDeaths.contains(playerId);
    }

    /** Clears the PvP death flag (called after a natural death saves a location). */
    public void clearPvpDeath(UUID playerId) {
        pvpDeaths.remove(playerId);
    }
}
