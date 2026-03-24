package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks combat state for each player.
 *
 * <p>A player is "combat-tagged" for {@code combat.duration-seconds} (default 15 s)
 * after dealing or receiving damage from another player.  While tagged:
 * <ul>
 *   <li>Teleport commands (/home, /lobby, /warp, /tpa, /tpaccept, /back) are blocked.</li>
 *   <li>If the player disconnects, they are killed.</li>
 * </ul>
 * The tag is cleared automatically on death or when the timer expires.
 */
public class CombatManager {

    private static final int DEFAULT_DURATION = 15;

    private final KlassenPlugin plugin;
    /** Player UUID → combat tag expiry timestamp (ms). */
    private final Map<UUID, Long> combatTagExpiry = new HashMap<>();

    public CombatManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        // Tick every second: expire tags and notify players when tag ends.
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    /**
     * Tags {@code playerId} as in combat, resetting the timer if already tagged.
     */
    public void tag(UUID playerId) {
        boolean wasTagged = isTagged(playerId);
        combatTagExpiry.put(playerId, System.currentTimeMillis() + getDurationMs());
        if (!wasTagged) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(KlassenPlugin.colorizeComponent(
                        "&c☠ Kampfmodus aktiv! Nicht ausloggen oder teleportieren!"));
                plugin.getScoreboardManager().update(player);
            }
        }
    }

    /**
     * Returns {@code true} if the player is currently combat-tagged.
     */
    public boolean isTagged(UUID playerId) {
        Long expiry = combatTagExpiry.get(playerId);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            combatTagExpiry.remove(playerId);
            return false;
        }
        return true;
    }

    /**
     * Returns the remaining combat time in whole seconds, or 0 if not tagged.
     */
    public int getRemainingSeconds(UUID playerId) {
        Long expiry = combatTagExpiry.get(playerId);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0) {
            combatTagExpiry.remove(playerId);
            return 0;
        }
        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Removes the combat tag (on death or server-side clear).
     */
    public void untag(UUID playerId) {
        combatTagExpiry.remove(playerId);
    }

    /**
     * Called when a player disconnects.  If they are combat-tagged, kills them
     * immediately (the PlayerQuitEvent fires while the player is still connected).
     */
    public void handleQuit(Player player) {
        if (!isTagged(player.getUniqueId())) return;
        untag(player.getUniqueId());
        // Kill right away – PlayerQuitEvent fires while the player is still on the
        // server, so setHealth(0) is processed before the connection is closed.
        player.setHealth(0);
        player.sendMessage(KlassenPlugin.colorizeComponent(
                "&cDu hast den Server während eines Kampfes verlassen und wurdest getötet!"));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /** Called every second to expire tags and send "combat ended" notifications. */
    private void tick() {
        long now = System.currentTimeMillis();
        combatTagExpiry.entrySet().removeIf(entry -> {
            if (now > entry.getValue()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    player.sendMessage(KlassenPlugin.colorizeComponent("&aKampfmodus beendet."));
                    plugin.getScoreboardManager().update(player);
                }
                return true;
            }
            return false;
        });
    }

    private long getDurationMs() {
        return plugin.getConfig().getInt("combat.duration-seconds", DEFAULT_DURATION) * 1000L;
    }
}
