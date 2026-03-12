package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.AntiCheatManager;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class AntiCheatListener implements Listener {

    private final KlassenPlugin plugin;
    private final AntiCheatManager manager;

    public AntiCheatListener(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getAntiCheatManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!manager.isEnabled()) return;
        if (!manager.isCheckEnabled("xray")) return;

        Player player = event.getPlayer();
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        if (!manager.getMonitoredOres().contains(event.getBlock().getType())) return;

        UUID uuid = player.getUniqueId();
        manager.recordOreMine(uuid);

        int threshold = plugin.getConfig().getInt("anticheat.xray.ore-threshold", 10);
        if (manager.getOreCountInWindow(uuid) > threshold) {
            manager.addViolation(uuid, "XRay");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!manager.isEnabled()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Skip if only rotation changed
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // Speed check
        if (manager.isCheckEnabled("speed")) {
            double distSq = from.distanceSquared(to);
            if (distSq > 0.01) { // distance > 0.1
                Long lastTime = manager.getLastMoveTime(uuid);
                if (lastTime != null) {
                    long elapsed = System.currentTimeMillis() - lastTime;
                    if (elapsed >= 50) {
                        double distance = from.distance(to);
                        double speed = distance * 1000.0 / elapsed;
                        double maxSpeed = plugin.getConfig().getDouble("anticheat.speed.max-blocks-per-second", 12.0);
                        if (speed > maxSpeed
                                && !player.isFlying()
                                && !player.isInsideVehicle()
                                && !player.isInWater()) {
                            manager.addViolation(uuid, "Speed");
                        }
                    }
                }
                manager.getAndUpdateLastPosition(uuid, to);
            }
        }

        // Fly tracking
        boolean onGround = player.isOnGround();
        boolean inLiquid = player.isInWater() || player.isInLava();
        boolean inVehicle = player.isInsideVehicle();

        if (onGround || inLiquid || inVehicle) {
            manager.resetAirTicks(uuid);
        } else if (!player.getAllowFlight()) {
            manager.incrementAirTicks(uuid);
            int maxAirTicks = plugin.getConfig().getInt("anticheat.fly.max-air-ticks", 80);
            if (manager.isCheckEnabled("fly") && manager.getAirTicks(uuid) > maxAirTicks) {
                manager.addViolation(uuid, "Fly");
                manager.resetAirTicks(uuid);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!manager.isEnabled()) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (damager.hasPermission("klassenplugin.anticheat.bypass")) return;

        UUID uuid = damager.getUniqueId();

        // Reach check
        if (manager.isCheckEnabled("reach")) {
            double distance = damager.getLocation().distance(victim.getLocation());
            double maxReach = plugin.getConfig().getDouble("anticheat.reach.max-reach", 5.0);
            if (distance > maxReach) {
                manager.addViolation(uuid, "Reach");
            }
        }

        // KillAura check
        if (manager.isCheckEnabled("killaura")) {
            manager.recordHit(uuid);
            int maxHits = plugin.getConfig().getInt("anticheat.killaura.max-hits-per-second", 15);
            if (manager.getHitsInLastSecond(uuid) > maxHits) {
                manager.addViolation(uuid, "KillAura");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.removePlayer(event.getPlayer().getUniqueId());
    }
}
