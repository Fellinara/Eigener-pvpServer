package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.AntiCheatManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.UUID;

public class AntiCheatListener implements Listener {

    /** Ice-type materials that allow extreme skating speeds. */
    private static final Set<Material> ICE_MATERIALS = Set.of(
            Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE,
            Material.FROSTED_ICE);

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

        // Skip if only rotation changed (no positional movement).
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // ── Speed check ──────────────────────────────────────────────────────
        if (manager.isCheckEnabled("speed")) {
            double distSq = from.distanceSquared(to);
            if (distSq > 0.01) {
                Long lastTime = manager.getLastMoveTime(uuid);
                if (lastTime != null) {
                    long elapsed = System.currentTimeMillis() - lastTime;
                    // Use at least 100 ms for a reliable measurement to avoid
                    // false positives from timing jitter on individual ticks.
                    if (elapsed >= 100) {
                        double distance = Math.sqrt(distSq);
                        double speed = distance * 1000.0 / elapsed;
                        double maxSpeed = plugin.getConfig()
                                .getDouble("anticheat.speed.max-blocks-per-second", 16.0);

                        // Add bonus for active Speed potion effect.
                        PotionEffect speedEffect = player.getPotionEffect(PotionEffectType.SPEED);
                        if (speedEffect != null) {
                            double bonus = plugin.getConfig()
                                    .getDouble("anticheat.speed.speed-potion-bonus-per-level", 3.5);
                            maxSpeed += (speedEffect.getAmplifier() + 1) * bonus;
                        }

                        // Skip when gliding (Elytra) – horizontal speed is unrestricted.
                        // Skip when on ice – skating allows very high speeds.
                        // Skip when inside a vehicle – not player-controlled movement.
                        Block blockBelow = to.clone().subtract(0, 0.1, 0).getBlock();
                        boolean onIce = ICE_MATERIALS.contains(blockBelow.getType());

                        if (speed > maxSpeed
                                && !player.isFlying()
                                && !player.isGliding()
                                && !player.isInsideVehicle()
                                && !player.isInWater()
                                && !onIce
                                && manager.canAddViolation(uuid)) {
                            manager.addViolation(uuid, "Speed");
                        }
                    }
                }
                manager.getAndUpdateLastPosition(uuid, to);
            }
        }

        // ── Fly / air-tick check ──────────────────────────────────────────────
        boolean onGround = player.isOnGround();
        boolean inLiquid = player.isInWater() || player.isInLava();
        boolean inVehicle = player.isInsideVehicle();
        boolean gliding = player.isGliding();

        if (onGround || inLiquid || inVehicle || gliding) {
            manager.resetAirTicks(uuid);
        } else if (!player.getAllowFlight()) {
            manager.incrementAirTicks(uuid);
            int maxAirTicks = plugin.getConfig().getInt("anticheat.fly.max-air-ticks", 120);
            if (manager.isCheckEnabled("fly") && manager.getAirTicks(uuid) > maxAirTicks) {
                if (manager.canAddViolation(uuid)) {
                    manager.addViolation(uuid, "Fly");
                }
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

        // ── Reach check ───────────────────────────────────────────────────────
        if (manager.isCheckEnabled("reach")) {
            double distance = damager.getLocation().distance(victim.getLocation());
            double maxReach = plugin.getConfig().getDouble("anticheat.reach.max-reach", 5.0);
            if (distance > maxReach && manager.canAddViolation(uuid)) {
                manager.addViolation(uuid, "Reach");
            }
        }

        // ── KillAura check ────────────────────────────────────────────────────
        if (manager.isCheckEnabled("killaura")) {
            manager.recordHit(uuid);
            int maxHits = plugin.getConfig().getInt("anticheat.killaura.max-hits-per-second", 15);
            if (manager.getHitsInLastSecond(uuid) > maxHits && manager.canAddViolation(uuid)) {
                manager.addViolation(uuid, "KillAura");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!manager.isEnabled() || !manager.isCheckEnabled("scaffold")) return;
        Player player = event.getPlayer();
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;
        UUID uuid = player.getUniqueId();
        manager.recordBlockPlace(uuid);
        int maxBlocks = plugin.getConfig().getInt("anticheat.scaffold.max-blocks-per-second", 8);
        if (manager.getBlockPlacesInLastSecond(uuid) > maxBlocks && manager.canAddViolation(uuid)) {
            manager.addViolation(uuid, "Scaffold");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!manager.isEnabled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            manager.recordFallDamage(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.removePlayer(event.getPlayer().getUniqueId());
    }
}
