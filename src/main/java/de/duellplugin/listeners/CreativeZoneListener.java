package de.duellplugin.listeners;

import de.duellplugin.DuellPlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class CreativeZoneListener implements Listener {

    private final DuellPlugin plugin;

    public CreativeZoneListener(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Ignore players in duels or bot fights – they have their own game mode management
        if (plugin.getDuellManager().isInDuel(player.getUniqueId())
                || plugin.getBotManager().isInBotFight(player.getUniqueId())) {
            return;
        }

        // Only track meaningful block-boundary crossings for efficiency
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        boolean wasInZone = plugin.getCreativeZoneManager().isPlayerInZone(player.getUniqueId());
        boolean nowInZone = plugin.getCreativeZoneManager().isInZone(event.getTo());

        if (!wasInZone && nowInZone) {
            plugin.getCreativeZoneManager().enterZone(player);
        } else if (wasInZone && !nowInZone) {
            plugin.getCreativeZoneManager().exitZone(player);
        }
    }
}
