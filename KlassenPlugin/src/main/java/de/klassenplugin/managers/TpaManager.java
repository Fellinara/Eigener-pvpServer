package de.klassenplugin.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages pending /tpa teleport requests.
 * Each request stores the requester, the target, and the time it was sent.
 * Requests expire after {@code expirySeconds}.
 */
public class TpaManager {

    private final int expirySeconds;

    // targetUUID -> requesterUUID (one pending request per target at a time)
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private final Map<UUID, Long> requestTimes = new HashMap<>();

    public TpaManager(int expirySeconds) {
        this.expirySeconds = expirySeconds;
    }

    /** Send a TPA request from {@code requester} to {@code target}. */
    public void sendRequest(UUID requester, UUID target) {
        pendingRequests.put(target, requester);
        requestTimes.put(target, System.currentTimeMillis());
    }

    /** Returns true if {@code target} has a non-expired TPA request waiting. */
    public boolean hasRequest(UUID target) {
        UUID requester = pendingRequests.get(target);
        if (requester == null) return false;
        Long time = requestTimes.get(target);
        if (time == null) return false;
        if ((System.currentTimeMillis() - time) / 1000 > expirySeconds) {
            pendingRequests.remove(target);
            requestTimes.remove(target);
            return false;
        }
        return true;
    }

    /** Returns the UUID of the player who sent a request to {@code target}, or null. */
    public UUID getRequester(UUID target) {
        if (!hasRequest(target)) return null;
        return pendingRequests.get(target);
    }

    /** Removes the pending request for {@code target}. */
    public void clearRequest(UUID target) {
        pendingRequests.remove(target);
        requestTimes.remove(target);
    }

    /**
     * Returns the UUID of the player that {@code requester} sent a request to, or null.
     * Used by /tpcancel to find the target whose queue should be cleared.
     */
    public UUID getTarget(UUID requester) {
        for (Map.Entry<UUID, UUID> entry : pendingRequests.entrySet()) {
            if (entry.getValue().equals(requester)) {
                Long time = requestTimes.get(entry.getKey());
                if (time != null && (System.currentTimeMillis() - time) / 1000 <= expirySeconds) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /** Clean up any request that involves the given player (e.g. on disconnect). */
    public void removePlayer(UUID playerId) {
        pendingRequests.remove(playerId);
        requestTimes.remove(playerId);
        pendingRequests.entrySet().removeIf(e -> e.getValue().equals(playerId));
    }
}
