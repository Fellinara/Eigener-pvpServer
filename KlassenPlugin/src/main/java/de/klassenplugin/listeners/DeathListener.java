package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.BackManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Handles player death:
 * <ul>
 *   <li>Natural death → saves location for /back.</li>
 *   <li>PvP death (killed by a player) → marks PvP flag; /back is blocked.</li>
 *   <li>Triggers the corpse system in every case.</li>
 * </ul>
 */
public class DeathListener implements Listener {

    private final KlassenPlugin plugin;

    public DeathListener(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.isPluginEnabled()) return;

        Player player = event.getEntity();
        BackManager back = plugin.getBackManager();

        boolean pvp = player.getKiller() != null;
        if (pvp) {
            // PvP kill – block /back
            back.markPvpDeath(player.getUniqueId());
        } else {
            // Natural / environmental death – allow /back
            if (plugin.getConfig().getBoolean("back.on-death", true)) {
                back.setLastLocation(player.getUniqueId(), player.getLocation());
            }
            back.clearPvpDeath(player.getUniqueId());
        }

        // Spawn corpse skull and store dropped items
        plugin.getCorpseManager().handleDeath(event);
    }
}
