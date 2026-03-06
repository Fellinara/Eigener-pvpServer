package de.duellplugin.listeners;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.SkyWarsGame;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SkyWarsListener implements Listener {

    private final DuellPlugin plugin;

    public SkyWarsListener(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        if (!plugin.getSkyWarsManager().isInSkyWars(dead.getUniqueId())) return;

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player killer = dead.getKiller();
        plugin.getSkyWarsManager().handlePlayerDeath(dead, killer);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Zombie && entity.hasMetadata("skywars_bot")) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            plugin.getSkyWarsManager().handleBotDeath(entity.getUniqueId());
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only allow PvP during an active SkyWars game
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        boolean attackerInSW = plugin.getSkyWarsManager().isInSkyWars(attacker.getUniqueId());
        boolean victimInSW = plugin.getSkyWarsManager().isInSkyWars(victim.getUniqueId());

        if (!attackerInSW && !victimInSW) return; // not our concern

        // Both must be in SkyWars and game must be ACTIVE
        if (!attackerInSW || !victimInSW) {
            event.setCancelled(true);
            return;
        }

        SkyWarsGame game = plugin.getSkyWarsManager().getGameForPlayer(attacker.getUniqueId());
        if (game == null || game.getState() != SkyWarsGame.State.ACTIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // Prevent item drops in SkyWars waiting room
        if (!plugin.getSkyWarsManager().isInSkyWars(event.getPlayer().getUniqueId())) return;

        SkyWarsGame game = plugin.getSkyWarsManager().getGameForPlayer(event.getPlayer().getUniqueId());
        if (game != null && game.getState() == SkyWarsGame.State.WAITING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getSkyWarsManager().handleDisconnect(event.getPlayer().getUniqueId());
    }
}
