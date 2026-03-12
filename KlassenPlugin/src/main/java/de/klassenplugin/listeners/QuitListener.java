package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Cleans up TPA requests and private-message state when a player leaves.
 */
public class QuitListener implements Listener {

    private final KlassenPlugin plugin;

    public QuitListener(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getTpaManager().removePlayer(event.getPlayer().getUniqueId());
        plugin.getMsgManager().removePlayer(event.getPlayer().getUniqueId());
        plugin.getRankManager().removeRankFromPlayer(event.getPlayer());
    }
}
