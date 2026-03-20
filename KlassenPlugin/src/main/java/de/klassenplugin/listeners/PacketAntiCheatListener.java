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
 *   <li><b>FreeCam</b> – player has not sent a single movement packet for
 *       {@code packet.freecam-idle-seconds} seconds while still sending other
 *       packets (chat, interactions). When triggered, the player is silently
 *       teleported back to their last known safe position to "snap" them back.</li>
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

    // ── State ─────────────────────────────────────────────────────────────────

    private final KlassenPlugin plugin;
    private final AntiCheatManager manager;

    /** Tracks per-player packet count within the current 1-second window. */
    private final ConcurrentHashMap<UUID, AtomicInteger> packetRate  = new ConcurrentHashMap<>();
    /** Timestamp (ms) when the current 1-second window started. */
    private final ConcurrentHashMap<UUID, Long>          rateWindowStart = new ConcurrentHashMap<>();

    /** Timestamp of the last POSITION or POSITION_LOOK packet per player. */
    private final ConcurrentHashMap<UUID, Long>    lastMovePkt   = new ConcurrentHashMap<>();
    /** Server-confirmed position (from last valid POSITION packet). */
    private final ConcurrentHashMap<UUID, double[]> lastSafePos  = new ConcurrentHashMap<>();

    /** Timestamp of the most-recent non-movement packet. Used for FreeCam. */
    private final ConcurrentHashMap<UUID, Long> lastActivityPkt = new ConcurrentHashMap<>();

    public PacketAntiCheatListener(KlassenPlugin plugin, ProtocolManager protocolManager) {
        this.plugin  = plugin;
        this.manager = plugin.getAntiCheatManager();
        register(protocolManager);
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
                handleMovement(event);
            }
        });

        // ── USE_ENTITY (right-click / attack entity) ──────────────────────
        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handleUseEntity(event);
            }
        });

        // ── BLOCK_DIG (break block / abort digging) ────────────────────────
        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handleBlockDig(event);
            }
        });

        // ── All other client packets – used for flood + FreeCam activity ───
        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR,
                PacketType.Play.Client.CHAT,
                PacketType.Play.Client.USE_ITEM,
                PacketType.Play.Client.SET_CREATIVE_SLOT,
                PacketType.Play.Client.WINDOW_CLICK,
                PacketType.Play.Client.ENTITY_ACTION,
                PacketType.Play.Client.ARM_ANIMATION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handleActivity(event);
            }
        });
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    /**
     * Handles {@code POSITION} and {@code POSITION_LOOK} packets.
     *
     * <ul>
     *   <li>Records the last movement packet time (used by FreeCam detection).</li>
     *   <li>Detects impossible single-packet movement distances (PacketMove).</li>
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

        PacketType type = event.getPacketType();
        if (type == PacketType.Play.Client.FLYING) {
            // FLYING packet has no position data – just a ground flag.
            return;
        }

        double x = event.getPacket().getDoubles().read(0);
        double y = event.getPacket().getDoubles().read(1);
        double z = event.getPacket().getDoubles().read(2);

        // ── PacketMove check ─────────────────────────────────────────────
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
                    // Cancel the packet so the illegal position isn't applied.
                    event.setCancelled(true);
                    // Teleport back to last safe position on the main thread.
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
     *   <li>Updates {@code lastActivityPkt} timestamp for FreeCam detection.</li>
     *   <li>Increments per-player packet rate counter for flood detection.</li>
     * </ul>
     */
    private void handleActivity(PacketEvent event) {
        if (!manager.isEnabled()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        UUID uuid = player.getUniqueId();
        long now  = System.currentTimeMillis();

        // Record activity time for FreeCam check.
        lastActivityPkt.put(uuid, now);

        // ── FreeCam check ──────────────────────────────────────────────────
        if (manager.isCheckEnabled("freecam")) {
            long idleMs = plugin.getConfig()
                    .getInt("anticheat.packet.freecam.idle-seconds", 8) * 1000L;
            Long lastMove = lastMovePkt.get(uuid);
            if (lastMove != null && (now - lastMove) > idleMs) {
                // Player has been idle (no movement packets) but is still active.
                if (manager.canAddViolation(uuid)) {
                    manager.addViolation(uuid, "FreeCam");
                }
                // Snap player back to last safe position (prevents FreeCam abuse).
                double[] safe = lastSafePos.get(uuid);
                if (safe != null) {
                    final double sx = safe[0], sy = safe[1], sz = safe[2];
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Location snap = new Location(player.getWorld(), sx, sy, sz,
                                player.getLocation().getYaw(),
                                player.getLocation().getPitch());
                        player.teleport(snap);
                        player.sendMessage(KlassenPlugin.colorizeComponent(
                                "&c[AntiCheat] &7Deine Position wurde zurückgesetzt."));
                    });
                }
                // Reset the timer so we don't spam violations.
                lastMovePkt.put(uuid, now);
            }
        }

        // ── Packet flood check ─────────────────────────────────────────────
        if (manager.isCheckEnabled("packetflood")) {
            long windowStart = rateWindowStart.computeIfAbsent(uuid, k -> now);
            AtomicInteger count = packetRate.computeIfAbsent(uuid, k -> new AtomicInteger(0));

            if (now - windowStart > 1000L) {
                // New second – reset window.
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
    }
}
