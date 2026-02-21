package de.duellplugin.listeners;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Duel;
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
        if (!event.getPlayer().hasPermission("duell.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.getPlayer().hasPermission("duell.admin")) {
            event.setCancelled(true);
        }
    }
}
