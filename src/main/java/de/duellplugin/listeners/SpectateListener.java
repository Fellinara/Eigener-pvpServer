package de.duellplugin.listeners;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Arena;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class SpectateListener implements Listener {

    private final DuellPlugin plugin;

    public SpectateListener(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevents spectators from flying outside the arena region bounds.
     * Only restricts movement when the arena has a defined region.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SPECTATOR) return;
        if (!plugin.getSpectateManager().isSpectating(player.getUniqueId())) return;

        Location to = event.getTo();
        if (to == null) return;

        String arenaName = plugin.getSpectateManager().getArenaName(player.getUniqueId());
        if (arenaName == null) return;

        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null || !arena.isRegionDefined()) return;

        Location p1 = arena.getRegionPos1();
        Location p2 = arena.getRegionPos2();

        // Allow some padding around the region so the spectator has a good view
        int padding = 15;
        int vertPaddingDown = 5;
        int vertPaddingUp = 25;

        int minX = Math.min(p1.getBlockX(), p2.getBlockX()) - padding;
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX()) + padding;
        int minY = Math.min(p1.getBlockY(), p2.getBlockY()) - vertPaddingDown;
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY()) + vertPaddingUp;
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ()) - padding;
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ()) + padding;

        if (to.getX() < minX || to.getX() > maxX
                || to.getY() < minY || to.getY() > maxY
                || to.getZ() < minZ || to.getZ() > maxZ) {
            event.setCancelled(true);
        }
    }
}
