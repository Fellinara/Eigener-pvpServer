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

        // ── FFA death ────────────────────────────────────────────────────
        if (plugin.getFfaManager().isInFfa(dead.getUniqueId())) {
            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);

            Player killer = dead.getKiller();
            if (killer != null) {
                plugin.getServer().broadcastMessage(plugin.getPrefix()
                        + "§6" + killer.getName() + " §ehat §6" + dead.getName() + " §eim FFA besiegt!");
            }

            plugin.getFfaManager().handleFfaDeath(dead);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (dead.isOnline()) {
                    // isDead() guard: Bedrock players auto-respawn, so only call if still dead
                    if (dead.isDead()) dead.spigot().respawn();
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (dead.isOnline()) plugin.getFfaManager().respawnInFfa(dead);
                    }, 2L);
                }
            }, 1L);
            return;
        }

        // ── Bot fight death ───────────────────────────────────────────────
        if (plugin.getBotManager().isInBotFight(dead.getUniqueId())) {
            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);
            plugin.getBotManager().handlePlayerDeathInBotFight(dead);
            return;
        }

        // ── Duel death ────────────────────────────────────────────────────
        if (plugin.getDuellManager().isInDuel(dead.getUniqueId())) {
            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);

            Duel duel = plugin.getDuellManager().getDuel(dead.getUniqueId());
            if (duel == null) return;

            if (duel.isTeamDuel()) {
                // Mark player as eliminated
                duel.eliminatePlayer(dead.getUniqueId());
                int myTeam = duel.getTeamNumber(dead.getUniqueId());
                int otherTeam = (myTeam == 1) ? 2 : 1;

                if (!duel.isTeamAlive(myTeam)) {
                    // All of this player's team is out → the other team wins
                    java.util.List<java.util.UUID> winners = (otherTeam == 1)
                            ? duel.getAliveTeam1() : duel.getAliveTeam2();
                    java.util.UUID winnerUUID = winners.isEmpty()
                            ? duel.getPlayer1() : winners.get(0);
                    plugin.getDuellManager().endDuel(winnerUUID, dead.getUniqueId());
                } else {
                    // Team still has alive members; respawn dead player into spectator mode
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (dead.isOnline()) {
                            if (dead.isDead()) dead.spigot().respawn();
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                dead.setGameMode(org.bukkit.GameMode.SPECTATOR);
                                dead.sendMessage(plugin.getPrefix() + "§cDu bist ausgeschieden! Warte auf das Spielende.");
                            });
                        }
                    }, 1L);
                }
            } else {
                // Standard 1v1
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
        // Bot shield-blocking: reduce damage by 75% when the bot is actively blocking
        if (event.getDamager() instanceof Player
                && event.getEntity() instanceof Zombie bot
                && bot.hasMetadata("duell_bot")
                && plugin.getBotManager().isBlocking(bot.getUniqueId())) {
            event.setDamage(event.getDamage() * 0.25);
        }

        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player victim) {
            boolean attackerInDuel = plugin.getDuellManager().isInDuel(attacker.getUniqueId());
            boolean victimInDuel = plugin.getDuellManager().isInDuel(victim.getUniqueId());
            boolean attackerInBot = plugin.getBotManager().isInBotFight(attacker.getUniqueId());
            boolean victimInBot = plugin.getBotManager().isInBotFight(victim.getUniqueId());
            boolean attackerInFfa = plugin.getFfaManager().isInFfa(attacker.getUniqueId());
            boolean victimInFfa = plugin.getFfaManager().isInFfa(victim.getUniqueId());

            // FFA players may always attack each other
            if (attackerInFfa && victimInFfa) return;

            if (!attackerInDuel && !victimInDuel && !attackerInBot && !victimInBot) {
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
                && !plugin.getBotManager().isInBotFight(event.getPlayer().getUniqueId())
                && !plugin.getFfaManager().isInFfa(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!plugin.getDuellManager().isInDuel(player.getUniqueId())
                    && !plugin.getBotManager().isInBotFight(player.getUniqueId())
                    && !plugin.getFfaManager().isInFfa(player.getUniqueId())) {
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
        boolean inSkyWars = plugin.getSkyWarsManager().isInSkyWars(player.getUniqueId());

        if (!player.hasPermission("duell.admin") && !inDuel && !inBot && !inSkyWars
                && !plugin.getFfaManager().isInFfa(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // During a duel/bot fight, only blocks placed during this fight may be broken
        if (inDuel || inBot) {
            String arenaName = getArenaName(player.getUniqueId(), inDuel);
            if (arenaName != null
                    && !plugin.getArenaManager().isPlacedBlock(arenaName, event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        boolean inDuel = plugin.getDuellManager().isInDuel(player.getUniqueId());
        boolean inBot = plugin.getBotManager().isInBotFight(player.getUniqueId());
        boolean inSkyWars = plugin.getSkyWarsManager().isInSkyWars(player.getUniqueId());

        if (!player.hasPermission("duell.admin") && !inDuel && !inBot && !inSkyWars
                && !plugin.getFfaManager().isInFfa(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Record the placed block so it can later be broken or removed on reset
        if (inDuel || inBot) {
            String arenaName = getArenaName(player.getUniqueId(), inDuel);
            if (arenaName != null) {
                plugin.getArenaManager().addPlacedBlock(arenaName, event.getBlock().getLocation());
            }
        }
    }

    /** Helper: returns the arena name for a player in duel or bot fight. */
    private String getArenaName(java.util.UUID uuid, boolean inDuel) {
        if (inDuel) {
            Duel duel = plugin.getDuellManager().getDuel(uuid);
            return duel != null ? duel.getArenaName() : null;
        }
        return plugin.getBotManager().getPlayerArenaName(uuid);
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
