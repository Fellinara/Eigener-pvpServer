package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Cleans up TPA requests, private-message state, vanish, and scoreboard when a player leaves.
 */
public class QuitListener implements Listener {

    private final KlassenPlugin plugin;

    public QuitListener(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();

        // Suppress the real quit message for vanished players – a fake quit was
        // already broadcast when they activated /vanish, so showing the real one
        // would reveal that they were still on the server.
        if (plugin.getVanishManager().isVanished(player.getUniqueId())) {
            event.quitMessage(null);
        }

        plugin.getTpaManager().removePlayer(player.getUniqueId());
        plugin.getMsgManager().removePlayer(player.getUniqueId());
        plugin.getRankManager().removeRankFromPlayer(player);
        plugin.getScoreboardManager().remove(player);
        plugin.getVanishManager().onQuit(player);

        // Clean up ProtocolLib packet-listener state if it is loaded.
        if (plugin.getProtocolLibManager() != null
                && plugin.getProtocolLibManager().getPacketListener() != null) {
            plugin.getProtocolLibManager().getPacketListener().removePlayer(player.getUniqueId());
        }
    }
}
