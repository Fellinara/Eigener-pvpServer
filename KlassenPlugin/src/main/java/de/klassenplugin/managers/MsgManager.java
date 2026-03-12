package de.klassenplugin.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks private-message conversations so /reply works.
 * When player A messages player B, both A and B get each other recorded
 * as their last conversation partner.
 */
public class MsgManager {

    // playerUUID -> UUID of the last person they talked to (or received from)
    private final Map<UUID, UUID> lastConversation = new HashMap<>();

    public void setLastConversation(UUID player, UUID other) {
        lastConversation.put(player, other);
        lastConversation.put(other, player);
    }

    public UUID getLastConversation(UUID player) {
        return lastConversation.get(player);
    }

    public boolean hasLastConversation(UUID player) {
        return lastConversation.containsKey(player);
    }

    public void removePlayer(UUID playerId) {
        lastConversation.remove(playerId);
        lastConversation.entrySet().removeIf(e -> e.getValue().equals(playerId));
    }
}
