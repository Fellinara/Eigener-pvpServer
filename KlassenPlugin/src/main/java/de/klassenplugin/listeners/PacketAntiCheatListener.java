package de.klassenplugin.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.AntiCheatManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Packet-level anti-cheat checks using ProtocolLib.
 *
 * <h3>Checks performed</h3>
 * <ul>
 *   <li><b>PacketMove</b> – single movement packet carries the player an
 *       impossible distance (e.g. Teleport-hack sending raw position packets).</li>
 *   <li><b>PacketFlood</b> – client sends more than {@code packet.max-per-second}
 *       play packets in one second, indicative of packet bots or certain exploits.</li>
 *   <li><b>FreeCam (stall)</b> – server-side position has not changed for
 *       {@code freecam.stall-seconds} while movement packets are still arriving.
 *       Uses movement-packet timestamps (not chat/interaction) so pure FreeCam
 *       usage with no interactions is still detected.</li>
 *   <li><b>FreeCam (frozen-position)</b> – consecutive POSITION/POSITION_LOOK
 *       packets all carry exactly the same coordinates.  Real Java-Edition players
 *       always have micro-movement noise; perfectly frozen coordinates across
 *       {@code freecam.frozen-packets} packets is a reliable FreeCam signature.</li>
 *   <li><b>PacketReach</b> – {@code USE_ENTITY} interaction packet references an
 *       entity that is too far from the player's server-side position.</li>
 *   <li><b>PacketDig</b> – {@code BLOCK_DIG} (block break) packet targets a block
 *       that is too far away from the player.</li>
 * </ul>
 *
 * <p>All thresholds are configurable under {@code anticheat.packet} in
 * {@code config.yml}.  Every check respects {@code anticheat.enabled},
 * the per-check {@code anticheat.packet.<check>.enabled} flag, and the
 * {@code klassenplugin.anticheat.bypass} permission.
 */
public class PacketAntiCheatListener {

    // ── Constants ────────────────────────────────────────────────────────────

    /** Maximum distance (in blocks) allowed in a single movement packet. */
    private static final double DEFAULT_MAX_MOVE_DIST = 10.0;

    /**
     * Minimum coordinate delta (in blocks) considered "movement" in a POSITION packet.
     *
     * <p>Legitimate Java Edition players always exceed this threshold between
     * consecutive movement packets due to gravity, physics simulation, and server
     * friction – even when standing "still".  A sequence of packets where all
     * three axes change by less than this value means the client is sending a
     * perfectly frozen position, which is the definitive FreeCam packet signature.
     *
     * <p>0.002 blocks ≈ 0.0016 m, well below any natural physics micro-movement.
     */
    private static final double FROZEN_DELTA = 0.002;

    // ── State ─────────────────────────────────────────────────────────────────

    private final KlassenPlugin plugin;
    private final AntiCheatManager manager;

    /** Tracks per-player packet count within the current 1-second window. */
    private final ConcurrentHashMap<UUID, AtomicInteger> packetRate      = new ConcurrentHashMap<>();
    /** Timestamp (ms) when the current 1-second window started. */
    private final ConcurrentHashMap<UUID, Long>          rateWindowStart = new ConcurrentHashMap<>();

    /** Timestamp of the last POSITION / POSITION_LOOK / FLYING packet per player. */
    private final ConcurrentHashMap<UUID, Long>     lastMovePkt  = new ConcurrentHashMap<>();
    /** Server-confirmed position (from last valid POSITION packet). */
    private final ConcurrentHashMap<UUID, double[]> lastSafePos  = new ConcurrentHashMap<>();

    /**
     * Timestamp of the most-recent packet of ANY kind received from each player.
     * Updated by every packet handler, so it covers movement + interaction packets.
     * This is the primary "player is active" signal for FreeCam detection.
     */
    private final ConcurrentHashMap<UUID, Long> lastAnyPkt = new ConcurrentHashMap<>();

    /** Legacy: still updated by interaction-specific packets for flood detection. */
    private final ConcurrentHashMap<UUID, Long> lastActivityPkt = new ConcurrentHashMap<>();

    // ── FreeCam stall detection ───────────────────────────────────────────────

    /**
     * Last server-side position recorded by the Bukkit-tick stall check.
     * Initialized on first tick so the counter starts immediately.
     */
    private final ConcurrentHashMap<UUID, double[]> lastKnownPos  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer>  posStallTicks = new ConcurrentHashMap<>();

    // ── FreeCam frozen-position detection ────────────────────────────────────

    /**
     * Last position received in a POSITION/POSITION_LOOK packet.
     * Used to count consecutive identical-position packets.
     */
    private final ConcurrentHashMap<UUID, double[]> lastPktPos       = new ConcurrentHashMap<>();
    /**
     * How many consecutive POSITION/POSITION_LOOK packets had coordinates
     * within {@link #FROZEN_DELTA} of the previous packet's coordinates.
     */
    private final ConcurrentHashMap<UUID, AtomicInteger> frozenPktCount = new ConcurrentHashMap<>();

    public PacketAntiCheatListener(KlassenPlugin plugin, ProtocolManager protocolManager) {
        this.plugin  = plugin;
        this.manager = plugin.getAntiCheatManager();
        register(protocolManager);
        schedulePosStallCheck();
    }

    // ── FreeCam stall check (Bukkit scheduler, every second) ─────────────────

    /**
     * Every 20 ticks (≈1 second) compares each online player's server-side
     * position to the value recorded in the previous tick.
     *
     * <p>A violation is recorded when:
     * <ol>
     *   <li>The position has not changed (within 0.01 blocks) for
     *       {@code freecam.stall-seconds} consecutive seconds, AND</li>
     *   <li>A movement packet ({@code POSITION} / {@code POSITION_LOOK} /
     *       {@code FLYING}) was received within the last 2 seconds — this
     *       distinguishes an active FreeCam session from a player who is simply
     *       AFK and stopped sending packets.</li>
     * </ol>
     *
     * <p>Using {@code lastMovePkt} (not {@code lastActivityPkt}) for the
     * activity check is the critical fix: the previous code required a recent
     * chat or interaction packet, but FreeCam users typically do neither – they
     * just fly their camera.  FreeCam users DO keep sending {@code FLYING}
     * keep-alive packets because Meteor/Wurst's game loop keeps running, so
     * {@code lastMovePkt} is always fresh while the player is in FreeCam.
     */
    private void schedulePosStallCheck() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!manager.isEnabled()) return;
            if (!manager.isCheckEnabled("freecam")) return;
            int stallThreshold = plugin.getConfig()
                    .getInt("anticheat.packet.freecam.stall-seconds", 3);

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("klassenplugin.anticheat.bypass")) continue;
                if (player.getAllowFlight() || player.isFlying() || player.isInsideVehicle()) continue;
                // Skip Bedrock players – position stall happens legitimately.
                UUID uuid = player.getUniqueId();
                if (uuid.getMostSignificantBits() == 0L || player.getName().startsWith(".")) continue;

                double px = player.getLocation().getX();
                double py = player.getLocation().getY();
                double pz = player.getLocation().getZ();

                double[] knownPos = lastKnownPos.get(uuid);
                if (knownPos != null
                        && Math.abs(knownPos[0] - px) < 0.01
                        && Math.abs(knownPos[1] - py) < 0.01
                        && Math.abs(knownPos[2] - pz) < 0.01) {
                    int stall = posStallTicks.merge(uuid, 1, Integer::sum);
                    // Key fix: use lastMovePkt (movement packet arrival) as the
                    // "player is active" signal, NOT lastActivityPkt (chat/interaction).
                    // FreeCam keeps sending FLYING packets so lastMovePkt is always fresh.
                    Long lastMove = lastMovePkt.get(uuid);
                    boolean movingPkts = lastMove != null
                            && System.currentTimeMillis() - lastMove < 2000L;
                    if (stall >= stallThreshold && movingPkts
                            && manager.canAddViolation(uuid)) {
                        plugin.getLogger().warning("[AntiCheat/FreeCam] "
                                + player.getName() + " – Stall " + stall + "s");
                        manager.addViolation(uuid, "FreeCam");
                        posStallTicks.put(uuid, 0);
                    }
                } else {
                    lastKnownPos.put(uuid, new double[]{px, py, pz});
                    posStallTicks.put(uuid, 0);
                }
            }
        }, 20L, 20L);
    }

    // ── Registration ─────────────────────────────────────────────────────────

    private void register(ProtocolManager pm) {
        // ── Position / movement packets ───────────────────────────────────
        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.FLYING) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                touchAny(event);
                handleMovement(event);
            }
        });

        // ── USE_ENTITY (right-click / attack entity) ──────────────────────
        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                touchAny(event);
                handleUseEntity(event);
            }
        });

        // ── BLOCK_DIG (break block / abort digging) ────────────────────────
        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                touchAny(event);
                handleBlockDig(event);
            }
        });

        // ── All other client packets – flood + FreeCam activity ────────────
        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR,
                PacketType.Play.Client.CHAT,
                PacketType.Play.Client.USE_ITEM,
                PacketType.Play.Client.SET_CREATIVE_SLOT,
                PacketType.Play.Client.WINDOW_CLICK,
                PacketType.Play.Client.ENTITY_ACTION,
                PacketType.Play.Client.ARM_ANIMATION,
                PacketType.Play.Client.CLIENT_COMMAND,
                PacketType.Play.Client.KEEP_ALIVE,
                PacketType.Play.Client.BLOCK_PLACE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                touchAny(event);
                handleActivity(event);
            }
        });
    }

    /**
     * Updates the universal "last any packet" timestamp for a player.
     * Called from every packet handler so {@code lastAnyPkt} always reflects
     * the actual last packet time, regardless of type.
     */
    private void touchAny(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        lastAnyPkt.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    /**
     * Handles {@code POSITION}, {@code POSITION_LOOK}, and {@code FLYING} packets.
     *
     * <ul>
     *   <li>Records the last movement packet time (used by FreeCam stall check).</li>
     *   <li>Detects impossible single-packet movement distances (PacketMove).</li>
     *   <li>Counts consecutive packets with identical coordinates; a real Java
     *       player always has micro-movement noise – perfect freeze = FreeCam.</li>
     * </ul>
     */
    private void handleMovement(PacketEvent event) {
        if (!manager.isEnabled()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        UUID uuid = player.getUniqueId();
        long now  = System.currentTimeMillis();

        // Always record last movement packet time.
        lastMovePkt.put(uuid, now);

        // ── Timer hack check ─────────────────────────────────────────────────
        // Vanilla Minecraft sends exactly one POSITION / POSITION_LOOK packet
        // per game tick (≈ 20/s).  Timer hack speeds up the client clock,
        // causing 25–100 packets/s.  We record only POSITION and POSITION_LOOK
        // (not FLYING, which has no positional data and is not accelerated by
        // the Timer module) to keep the signal clean.
        PacketType type = event.getPacketType();
        if (type == PacketType.Play.Client.POSITION
                || type == PacketType.Play.Client.POSITION_LOOK) {
            if (manager.isCheckEnabled("timer")) {
                manager.recordMovePkt(uuid);
                int maxPkts = plugin.getConfig()
                        .getInt("anticheat.packet.timer.max-packets-per-second", 22);
                if (manager.getMovePktsInLastSecond(uuid) > maxPkts
                        && manager.canAddViolation(uuid)) {
                    plugin.getLogger().warning("[AntiCheat/Timer] " + player.getName()
                            + " – " + manager.getMovePktsInLastSecond(uuid) + " POSITION pkts/s");
                    manager.addViolation(uuid, "Timer");
                }
            }
        }

        if (type == PacketType.Play.Client.FLYING) {
            // FLYING packet has no position data – just a ground flag.
            // Still counts as "movement activity" for FreeCam stall detection.
            return;
        }

        double x, y, z;
        try {
            x = event.getPacket().getDoubles().read(0);
            y = event.getPacket().getDoubles().read(1);
            z = event.getPacket().getDoubles().read(2);
        } catch (Exception ignored) {
            return;
        }

        // ── Frozen-position FreeCam detection ────────────────────────────────
        // Real Java players always have tiny coordinate noise (physics, lag).
        // A perfectly frozen position across N consecutive movement packets means
        // the client is in FreeCam (body frozen, camera flying freely).
        if (manager.isCheckEnabled("freecam")) {
            // Skip Bedrock players.
            if (uuid.getMostSignificantBits() != 0L && !player.getName().startsWith(".")) {
                double[] lastPkt = lastPktPos.get(uuid);
                AtomicInteger frozenCtr = frozenPktCount.computeIfAbsent(uuid, k -> new AtomicInteger(0));
                if (lastPkt != null
                        && Math.abs(lastPkt[0] - x) < FROZEN_DELTA
                        && Math.abs(lastPkt[1] - y) < FROZEN_DELTA
                        && Math.abs(lastPkt[2] - z) < FROZEN_DELTA) {
                    int frozen = frozenCtr.incrementAndGet();
                    int threshold = plugin.getConfig()
                            .getInt("anticheat.packet.freecam.frozen-packets", 20);
                    if (frozen >= threshold && !player.getAllowFlight()
                            && !player.isInsideVehicle()
                            && manager.canAddViolation(uuid)) {
                        plugin.getLogger().warning("[AntiCheat/FreeCam] "
                                + player.getName() + " – frozen position ×" + frozen);
                        manager.addViolation(uuid, "FreeCam");
                        frozenCtr.set(0);
                    }
                } else {
                    frozenCtr.set(0);
                    lastPktPos.put(uuid, new double[]{x, y, z});
                }
            }
        }

        // ── PacketMove check ─────────────────────────────────────────────────
        if (manager.isCheckEnabled("packetmove")) {
            double[] last = lastSafePos.get(uuid);
            if (last != null) {
                double dx = x - last[0];
                double dy = y - last[1];
                double dz = z - last[2];
                double distSq = dx * dx + dy * dy + dz * dz;
                double maxDist = plugin.getConfig()
                        .getDouble("anticheat.packet.packetmove.max-distance",
                                DEFAULT_MAX_MOVE_DIST);

                if (distSq > maxDist * maxDist
                        && !player.getAllowFlight()
                        && !player.isInsideVehicle()
                        && manager.canAddViolation(uuid)) {
                    manager.addViolation(uuid, "PacketMove");
                    event.setCancelled(true);
                    final double sx = last[0], sy = last[1], sz = last[2];
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Location safe = new Location(player.getWorld(), sx, sy, sz,
                                player.getLocation().getYaw(),
                                player.getLocation().getPitch());
                        player.teleport(safe);
                    });
                    return;
                }
            }
            lastSafePos.put(uuid, new double[]{x, y, z});
        } else {
            lastSafePos.put(uuid, new double[]{x, y, z});
        }
    }

    /**
     * Handles {@code USE_ENTITY} packets.
     *
     * <p>Checks whether the target entity is within a configurable reach distance
     * of the player's <em>server-side</em> entity position (not the packet
     * position). This catches reach hacks that bypass the event-based check.
     */
    private void handleUseEntity(PacketEvent event) {
        if (!manager.isEnabled()) return;
        if (!manager.isCheckEnabled("packetreach")) return;
        Player player = event.getPlayer();
        if (player == null) return;
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        UUID uuid = player.getUniqueId();

        // Read the entity ID from the packet.
        int entityId = event.getPacket().getIntegers().read(0);

        // Resolve entity on main thread — USE_ENTITY is fired sync by ProtocolLib.
        Entity target = null;
        for (Entity e : player.getWorld().getEntities()) {
            if (e.getEntityId() == entityId) {
                target = e;
                break;
            }
        }
        if (target == null) return;

        double distance = player.getLocation().distance(target.getLocation());
        double maxReach  = plugin.getConfig()
                .getDouble("anticheat.packet.packetreach.max-reach", 6.0);

        if (distance > maxReach && manager.canAddViolation(uuid)) {
            manager.addViolation(uuid, "PacketReach");
            event.setCancelled(true);
        }
    }

    /**
     * Handles {@code BLOCK_DIG} packets.
     *
     * <p>Checks whether the targeted block is within a reasonable distance of
     * the player. This complements the event-based XRay check with a raw-packet
     * distance sanity check.
     */
    private void handleBlockDig(PacketEvent event) {
        if (!manager.isEnabled()) return;
        if (!manager.isCheckEnabled("packetdig")) return;
        Player player = event.getPlayer();
        if (player == null) return;
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        UUID uuid = player.getUniqueId();

        // Read block position from packet.
        com.comphenix.protocol.wrappers.BlockPosition pos =
                event.getPacket().getBlockPositionModifier().read(0);
        if (pos == null) return;

        Location playerLoc = player.getLocation();
        double dx = pos.getX() - playerLoc.getX();
        double dy = pos.getY() - (playerLoc.getY() + 1.5); // eye height approx.
        double dz = pos.getZ() - playerLoc.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        double maxReach = plugin.getConfig()
                .getDouble("anticheat.packet.packetdig.max-reach", 7.0);

        if (distSq > maxReach * maxReach && manager.canAddViolation(uuid)) {
            manager.addViolation(uuid, "PacketDig");
            event.setCancelled(true);
        }
    }

    /**
     * Handles non-movement activity packets.
     *
     * <ul>
     *   <li>Updates {@code lastActivityPkt} and {@code lastAnyPkt} timestamps.</li>
     *   <li>Increments per-player packet rate counter for flood detection.</li>
     * </ul>
     *
     * <p>The old "no movement packets + activity" FreeCam check has been removed
     * from here. That heuristic was wrong: FreeCam users DO keep sending
     * {@code FLYING} movement packets (their body is frozen but the game loop
     * still runs). The correct FreeCam detection is in the stall check and the
     * frozen-position counter in {@link #handleMovement}.
     */
    private void handleActivity(PacketEvent event) {
        if (!manager.isEnabled()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        UUID uuid = player.getUniqueId();
        long now  = System.currentTimeMillis();

        lastActivityPkt.put(uuid, now);

        // ── AutoClicker check ──────────────────────────────────────────────
        // ARM_ANIMATION is sent once per swing/click.  Vanilla clients are
        // capped by the attack cooldown and human CPS (~4–16 clicks/s).
        // AutoClicker modules bypass this, producing 20–40 swings/s.
        if (manager.isCheckEnabled("autoclicker")
                && event.getPacketType() == PacketType.Play.Client.ARM_ANIMATION) {
            manager.recordArmSwing(uuid);
            int maxSwings = plugin.getConfig()
                    .getInt("anticheat.packet.autoclicker.max-swings-per-second", 20);
            if (manager.getArmSwingsInLastSecond(uuid) > maxSwings
                    && manager.canAddViolation(uuid)) {
                plugin.getLogger().warning("[AntiCheat/AutoClicker] " + player.getName()
                        + " – " + manager.getArmSwingsInLastSecond(uuid) + " swings/s");
                manager.addViolation(uuid, "AutoClicker");
            }
        }

        // ── Packet flood check ─────────────────────────────────────────────
        if (manager.isCheckEnabled("packetflood")) {
            long windowStart = rateWindowStart.computeIfAbsent(uuid, k -> now);
            AtomicInteger count = packetRate.computeIfAbsent(uuid, k -> new AtomicInteger(0));

            if (now - windowStart > 1000L) {
                rateWindowStart.put(uuid, now);
                count.set(1);
            } else {
                int total = count.incrementAndGet();
                int maxPps = plugin.getConfig()
                        .getInt("anticheat.packet.packetflood.max-per-second", 200);
                if (total > maxPps && manager.canAddViolation(uuid)) {
                    manager.addViolation(uuid, "PacketFlood");
                }
            }
        }
    }

    // ── Player state cleanup ──────────────────────────────────────────────────

    /**
     * Cleans up per-player state. Call this when a player disconnects.
     */
    public void removePlayer(UUID uuid) {
        packetRate.remove(uuid);
        rateWindowStart.remove(uuid);
        lastMovePkt.remove(uuid);
        lastSafePos.remove(uuid);
        lastActivityPkt.remove(uuid);
        lastAnyPkt.remove(uuid);
        lastKnownPos.remove(uuid);
        posStallTicks.remove(uuid);
        lastPktPos.remove(uuid);
        frozenPktCount.remove(uuid);
    }
}
