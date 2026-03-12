package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Saves a player's location when they die so /back can return them there.
 */
public class DeathListener implements Listener {

    private final KlassenPlugin plugin;

    public DeathListener(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.isPluginEnabled()) return;
        if (!plugin.getConfig().getBoolean("back.on-death", true)) return;

        plugin.getBackManager().setLastLocation(
                event.getEntity().getUniqueId(),
                event.getEntity().getLocation()
        );
    }
}
