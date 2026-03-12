package de.klassenplugin.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public boolean isOnCooldown(UUID playerId, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return false;
        Long lastUsed = cooldowns.get(playerId);
        if (lastUsed == null) return false;
        long elapsed = (System.currentTimeMillis() - lastUsed) / 1000;
        return elapsed < cooldownSeconds;
    }

    public int getRemainingSeconds(UUID playerId, int cooldownSeconds) {
        Long lastUsed = cooldowns.get(playerId);
        if (lastUsed == null) return 0;
        long elapsed = (System.currentTimeMillis() - lastUsed) / 1000;
        return (int) Math.max(0, cooldownSeconds - elapsed);
    }

    public void setCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    public void clearCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
