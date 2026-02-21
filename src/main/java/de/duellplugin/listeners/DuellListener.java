package de.duellplugin.listeners;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Duel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class DuellListener implements Listener {

    private final DuellPlugin plugin;

    public DuellListener(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();

        if (plugin.getBotManager().isInBotFight(dead.getUniqueId())) {
            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);
            plugin.getBotManager().handlePlayerDeathInBotFight(dead);
            return;
        }

        if (plugin.getDuellManager().isInDuel(dead.getUniqueId())) {
            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);

            Duel duel = plugin.getDuellManager().getDuel(dead.getUniqueId());
            if (duel != null) {
                java.util.UUID winner = duel.getOpponent(dead.getUniqueId());
                plugin.getDuellManager().endDuel(winner, dead.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Zombie && entity.hasMetadata("duell_bot")) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            plugin.getBotManager().handleBotDeath(entity.getUniqueId());
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player victim) {
            boolean attackerInDuel = plugin.getDuellManager().isInDuel(attacker.getUniqueId());
            boolean victimInDuel = plugin.getDuellManager().isInDuel(victim.getUniqueId());

            if (!attackerInDuel && !victimInDuel) {
                event.setCancelled(true);
                return;
            }

            if (attackerInDuel && victimInDuel) {
                Duel duel = plugin.getDuellManager().getDuel(attacker.getUniqueId());
                if (duel != null && duel.getState() != Duel.DuelState.ACTIVE) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!plugin.getDuellManager().isInDuel(event.getPlayer().getUniqueId())
                && !plugin.getBotManager().isInBotFight(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!plugin.getDuellManager().isInDuel(player.getUniqueId())
                    && !plugin.getBotManager().isInBotFight(player.getUniqueId())) {
                event.setCancelled(true);
                player.setFoodLevel(20);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        boolean inDuel = plugin.getDuellManager().isInDuel(player.getUniqueId());
        boolean inBot = plugin.getBotManager().isInBotFight(player.getUniqueId());

        if (!player.hasPermission("duell.admin") && !inDuel && !inBot) {
            event.setCancelled(true);
            return;
        }

        // Record block state before it is broken so we can restore it on arena reset
        if (inDuel) {
            Duel duel = plugin.getDuellManager().getDuel(player.getUniqueId());
            if (duel != null) {
                plugin.getArenaManager().recordBlockChange(duel.getArenaName(), event.getBlock().getState());
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        boolean inDuel = plugin.getDuellManager().isInDuel(player.getUniqueId());
        boolean inBot = plugin.getBotManager().isInBotFight(player.getUniqueId());

        if (!player.hasPermission("duell.admin") && !inDuel && !inBot) {
            event.setCancelled(true);
            return;
        }

        // Record the old state (air) of the block being placed so we can remove the placed block on reset
        if (inDuel) {
            Duel duel = plugin.getDuellManager().getDuel(player.getUniqueId());
            if (duel != null) {
                plugin.getArenaManager().recordBlockChange(duel.getArenaName(), event.getBlock().getState());
            }
        }
    }

    /** Prevents natural mob spawning in the lobby world. */
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        var lobbySpawn = plugin.getLobbyManager().getLobbySpawn();
        if (lobbySpawn == null) return;
        if (!event.getLocation().getWorld().equals(lobbySpawn.getWorld())) return;

        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason != CreatureSpawnEvent.SpawnReason.CUSTOM
                && reason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            event.setCancelled(true);
        }
    }

    /** Disables natural health regeneration for UHC kit players during a duel or bot fight. */
    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();
        if (reason != EntityRegainHealthEvent.RegainReason.SATIATED
                && reason != EntityRegainHealthEvent.RegainReason.REGEN) {
            return;
        }

        if (plugin.getDuellManager().isInDuel(player.getUniqueId())) {
            Duel duel = plugin.getDuellManager().getDuel(player.getUniqueId());
            if (duel != null && "uhc".equals(duel.getKitName())) {
                event.setCancelled(true);
            }
            return;
        }

        if (plugin.getBotManager().isInBotFight(player.getUniqueId())) {
            var stats = plugin.getStatsManager().getStats(player.getUniqueId());
            if (stats != null && "uhc".equals(stats.getSelectedKit())) {
                event.setCancelled(true);
            }
        }
    }
}
