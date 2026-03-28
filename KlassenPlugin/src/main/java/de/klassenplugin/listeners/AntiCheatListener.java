package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.AntiCheatManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.enchantments.Enchantment;

import java.util.Set;
import java.util.UUID;

public class AntiCheatListener implements Listener {

    /** Ice-type materials that allow extreme skating speeds. */
    private static final Set<Material> ICE_MATERIALS = Set.of(
            Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE,
            Material.FROSTED_ICE);

    /** Materials that are non-passable (solid, collidable, not transparent). */
    private static final Set<Material> PASSABLE_MATERIALS = Set.of(
            Material.AIR, Material.CAVE_AIR, Material.VOID_AIR,
            Material.WATER, Material.LAVA,
            Material.TALL_GRASS, Material.SHORT_GRASS, Material.FERN,
            Material.DEAD_BUSH, Material.SEAGRASS, Material.TALL_SEAGRASS,
            Material.VINE, Material.LADDER, Material.SCAFFOLDING,
            Material.SNOW, Material.LIGHT);

    private final KlassenPlugin plugin;
    private final AntiCheatManager manager;

    public AntiCheatListener(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getAntiCheatManager();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Detects Bedrock players proxied through GeyserMC / Floodgate.
     *
     * <p>Floodgate assigns Bedrock players UUIDs whose most-significant bytes are
     * all zero ({@code 00000000-0000-0000-xxxx-xxxxxxxxxxxx}), and prepends their
     * username with a period ({@code ".Steve"}).  Both heuristics are checked so
     * Bedrock players are not falsely flagged for movement checks that require
     * Java-Edition physics (Jesus, Phase, Speed).
     */
    private static boolean isBedrockPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        // GeyserMC / Floodgate: MSBs are 0  →  first 3 dash-groups are all zeros.
        if (uuid.getMostSignificantBits() == 0L) return true;
        // Floodgate username prefix ("." by default, configurable by server owner).
        String name = player.getName();
        return name.startsWith(".");
    }

    /**
     * Server-side ground check: verifies there is actually a solid/liquid block
     * in the 0.0–0.6 range below the given location, regardless of what the
     * client's "on-ground" flag says.
     */
    private static boolean isOnGroundServerSide(Location loc) {
        for (double offset = 0.05; offset <= 0.6; offset += 0.1) {
            Block b = loc.clone().subtract(0, offset, 0).getBlock();
            Material t = b.getType();
            if ((t.isSolid() && !b.isPassable()) || t == Material.WATER || t == Material.LAVA) {
                return true;
            }
        }
        return false;
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

    /**
     * Whitelists legitimate plugin teleports (home, lobby, warp, tpa, …) so the
     * TeleportHack check does not fire immediately after a sanctioned teleport.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!manager.isEnabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;
        // Whitelist for 2 seconds after any legitimate teleport.
        manager.notifyTeleport(player.getUniqueId());
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
        if (manager.isCheckEnabled("speed") && !isBedrockPlayer(player)) {
            double distSq = from.distanceSquared(to);
            if (distSq > 0.01) {
                Long lastTime = manager.getLastMoveTime(uuid);
                if (lastTime != null) {
                    long elapsed = System.currentTimeMillis() - lastTime;
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
        // Use a server-side ground check so a hack that spams isOnGround=true
        // does not bypass the air-tick counter.
        boolean inLiquid   = player.isInWater() || player.isInLava();
        boolean inVehicle  = player.isInsideVehicle();
        boolean gliding    = player.isGliding();
        boolean serverOnGround = inLiquid || inVehicle || gliding || isOnGroundServerSide(to);

        if (serverOnGround) {
            manager.resetAirTicks(uuid);
        } else if (!player.getAllowFlight() && !isBedrockPlayer(player)) {
            manager.incrementAirTicks(uuid);
            int maxAirTicks = plugin.getConfig().getInt("anticheat.fly.max-air-ticks", 40);
            if (manager.isCheckEnabled("fly") && manager.getAirTicks(uuid) > maxAirTicks) {
                if (manager.canAddViolation(uuid)) {
                    manager.addViolation(uuid, "Fly");
                }
                manager.resetAirTicks(uuid);
            }
        }

        // ── BoatFly check ─────────────────────────────────────────────────────
        // Detects players using a boat to gain altitude illegitimately.
        if (manager.isCheckEnabled("boatfly") && inVehicle && !isBedrockPlayer(player)) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof Boat) {
                Location boatLoc = vehicle.getLocation();
                boolean boatOnSurface = isOnGroundServerSide(boatLoc) ||
                        boatLoc.clone().subtract(0, 0.3, 0).getBlock().getType() == Material.WATER;
                // Flag if boat is moving upward while not near any surface.
                if (!boatOnSurface && to.getY() > from.getY() + 0.1
                        && manager.canAddViolation(uuid)) {
                    manager.addViolation(uuid, "BoatFly");
                }
            }
        }

        // ── Phase / NoClip check ──────────────────────────────────────────────
        if (manager.isCheckEnabled("phase") && !isBedrockPlayer(player)) {
            Block headBlock = to.clone().add(0, 0.5, 0).getBlock();
            Block feetBlock = to.getBlock();
            if (isInsideSolidBlock(headBlock) && isInsideSolidBlock(feetBlock)
                    && !player.isInsideVehicle()
                    && manager.canAddViolation(uuid)) {
                manager.addViolation(uuid, "Phase");
            }
        }

        // ── Jesus / Water-walk check ──────────────────────────────────────────
        // Detect walking on top of water without the correct potion effect or
        // special item (Frost-Walker boots handled by checking for enchantment).
        // Bedrock players are exempt: Bedrock Edition physics differ and can
        // produce false positives that look identical to water-walking.
        if (manager.isCheckEnabled("jesus") && !isBedrockPlayer(player)) {
            Block below = to.clone().subtract(0, 0.1, 0).getBlock();
            if (below.getType() == Material.WATER && player.isOnGround()
                    && !player.isInWater()
                    && !player.isInsideVehicle()
                    && !player.isGliding()
                    && !hasFrostWalker(player)
                    && manager.canAddViolation(uuid)) {
                manager.addViolation(uuid, "Jesus");
            }
        }

        // ── Position-Jump / Teleport-Hack check ──────────────────────────────
        // Detect impossible position jumps (far larger than any legitimate speed).
        // Exempt players who were recently teleported by a plugin command.
        if (manager.isCheckEnabled("teleport") && !manager.wasRecentlyTeleported(uuid)) {
            double distSq = from.distanceSquared(to);
            double teleportThreshold = plugin.getConfig()
                    .getDouble("anticheat.teleport.max-distance", 20.0);
            if (distSq > teleportThreshold * teleportThreshold
                    && !player.isInsideVehicle()
                    && !player.getAllowFlight()
                    && !player.isGliding()
                    && manager.canAddViolation(uuid)) {
                manager.addViolation(uuid, "TeleportHack");
            }
        }

        // ── InvalidSprint check ───────────────────────────────────────────────
        // In vanilla Minecraft a player cannot sprint when food level == 0 or
        // while affected by Blindness.  Hack clients override these constraints.
        if (manager.isCheckEnabled("invalidsprint") && !isBedrockPlayer(player)) {
            if (player.isSprinting()) {
                boolean blindness = player.hasPotionEffect(PotionEffectType.BLINDNESS);
                // Food 0 is the absolute floor: at that point the game engine
                // forcibly cancels any sprint.  We flag only this definitive case
                // to avoid false positives while food ticks down from 6.
                if (player.getFoodLevel() == 0 || blindness) {
                    if (manager.canAddViolation(uuid)) {
                        manager.addViolation(uuid, "InvalidSprint");
                    }
                }
            }
        }

        // ── NoFall Y-drop tracking ────────────────────────────────────────────
        // Track the peak Y during every airborne phase.  When the player lands,
        // compare the drop height to expected fall-damage threshold.  If the
        // player dropped far enough to take damage but no fall-damage event was
        // fired, flag NoFall.
        //
        // Exemptions: liquid landing, elytra gliding, vehicles, slow falling,
        // Feather-Falling boots.
        //
        // The violation check is deliberately deferred by 2 ticks via
        // runTaskLater so that EntityDamageEvent (fall damage) fires BEFORE we
        // conclude the player bypassed it.  Without the delay, PlayerMoveEvent
        // fires in the same tick as – but BEFORE – EntityDamageEvent, causing
        // every legitimate fall to produce a false NoFall violation.
        if (manager.isCheckEnabled("nofall") && !isBedrockPlayer(player)) {
            boolean onGround = serverOnGround;
            boolean prevAir = manager.wasInAir(uuid);
            manager.updateAirPeak(uuid, to.getY(), onGround);

            if (onGround && prevAir) {
                // Player just landed.
                Double peakY = manager.getAirPeakY(uuid);
                if (peakY != null) {
                    double drop = peakY - to.getY();
                    // Vanilla fall damage begins at > 3 blocks of drop.
                    // We use 4.5 as threshold to avoid false positives from
                    // stair/slab landings and small jumps.
                    if (drop > 4.5
                            && !inLiquid
                            && !gliding
                            && !inVehicle
                            && !player.hasPotionEffect(PotionEffectType.SLOW_FALLING)
                            && !hasFeatherFalling(player)
                            && !landingNegatesFallDamage(to)) {
                        // Defer the actual violation check by 2 ticks so that
                        // the EntityDamageEvent (fall damage) gets a chance to
                        // fire and record the damage before we draw conclusions.
                        final long landTimestamp = System.currentTimeMillis();
                        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (!player.isOnline()) return;
                            // If fall damage was recorded AFTER we landed, the
                            // player legitimately took fall damage → not NoFall.
                            if (manager.getLastFallDamageTime(uuid) >= landTimestamp) return;
                            if (manager.canAddViolation(uuid)) {
                                manager.addViolation(uuid, "NoFall");
                            }
                        }, 2L);
                    }
                }
                manager.clearAirPeakY(uuid);
            }
        }
    }

    // ── Combat tag via projectile (arrow / trident / crossbow bolt) ──────────
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile proj)) return;
        if (!(proj.getShooter() instanceof Player shooter)) return;
        if (!(event.getEntity() instanceof Player victimPlayer)) return;
        plugin.getCombatManager().tag(shooter.getUniqueId());
        plugin.getCombatManager().tag(victimPlayer.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!manager.isEnabled()) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        // ── Combat tag both players – runs before the bypass check so that OP
        //    players are tagged just like everyone else. ───────────────────────
        if (victim instanceof Player victimPlayer) {
            plugin.getCombatManager().tag(damager.getUniqueId());
            plugin.getCombatManager().tag(victimPlayer.getUniqueId());
        }

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

        // ── KillAura checks ───────────────────────────────────────────────────
        if (manager.isCheckEnabled("killaura")) {
            // Layer 1: hits-per-second rate (existing check).
            manager.recordHit(uuid);
            int maxHits = plugin.getConfig().getInt("anticheat.killaura.max-hits-per-second", 12);
            if (manager.getHitsInLastSecond(uuid) > maxHits && manager.canAddViolation(uuid)) {
                manager.addViolation(uuid, "KillAura");
            }

            // Layer 2: multi-target in 500 ms.
            // KillAura auto-switches targets far faster than a human can re-aim.
            // A legitimate PvP player very rarely attacks more than 2 different
            // entities within 500 ms.
            manager.recordAttackTarget(uuid, victim.getUniqueId());
            int maxTargets = plugin.getConfig()
                    .getInt("anticheat.killaura.max-targets-per-500ms", 3);
            if (manager.getDistinctTargetsInWindow(uuid) > maxTargets
                    && manager.canAddViolation(uuid)) {
                manager.addViolation(uuid, "KillAura (Multi-Target)");
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isInsideSolidBlock(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        if (!type.isSolid()) return false;
        if (PASSABLE_MATERIALS.contains(type)) return false;
        return block.getType().isOccluding();
    }

    private static boolean hasFrostWalker(Player player) {
        for (org.bukkit.inventory.ItemStack is : player.getEquipment().getArmorContents()) {
            if (is == null) continue;
            if (is.containsEnchantment(org.bukkit.enchantments.Enchantment.FROST_WALKER)) return true;
        }
        return false;
    }

    private static boolean hasFeatherFalling(Player player) {
        org.bukkit.inventory.ItemStack boots = player.getEquipment().getBoots();
        if (boots == null) return false;
        return boots.containsEnchantment(Enchantment.FEATHER_FALLING);
    }

    /**
     * Returns {@code true} when the block the player lands on negates or fully
     * absorbs fall damage, so no {@code EntityDamageEvent} with cause FALL will
     * be fired.  These blocks must be exempted from the NoFall check, otherwise
     * a legitimate landing produces a false violation.
     *
     * <ul>
     *   <li><b>Slime block</b> – bounces the player; no fall damage.</li>
     *   <li><b>Honey block</b> – reduces fall velocity; no fall damage.</li>
     *   <li><b>Beds</b> – bounce the player; no fall damage.</li>
     *   <li><b>Haybale</b> – reduces fall damage by 80 %; large falls still
     *       deal some damage, but small ones over the 4.5-block threshold
     *       may produce no damage event, causing a false positive.</li>
     *   <li><b>Cobweb</b> – slows the player; no fall damage on entry.</li>
     *   <li><b>Powder snow</b> – slows the player; no fall damage.</li>
     * </ul>
     */
    private static boolean landingNegatesFallDamage(Location loc) {
        // Check the block at the player's feet and one block below.
        for (double offset : new double[]{0.0, -0.5, -1.0}) {
            Block b = loc.clone().add(0, offset, 0).getBlock();
            Material t = b.getType();
            if (t == Material.SLIME_BLOCK
                    || t == Material.HONEY_BLOCK
                    || t == Material.COBWEB
                    || t == Material.POWDER_SNOW
                    || t == Material.HAY_BLOCK
                    || isBedBlock(t)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBedBlock(Material type) {
        String name = type.name();
        return name.endsWith("_BED");
    }
}

