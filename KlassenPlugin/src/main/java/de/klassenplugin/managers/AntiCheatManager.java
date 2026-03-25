package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class AntiCheatManager {

    /** A single violation entry for the recent-log. */
    public record ViolationEntry(String playerName, String check, long timestamp) {}

    private static final int MAX_LOG_SIZE = 100;

    private final KlassenPlugin plugin;

    private final Map<UUID, List<Long>> oreMineTimes = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, List<Long>> hitTimes = new HashMap<>();
    private final Map<UUID, Location> lastPosition = new HashMap<>();
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();
    private final Map<UUID, Integer> airTickCount = new HashMap<>();
    private final Map<UUID, List<Long>> blockPlaceTimes = new HashMap<>();
    private final Map<UUID, Long> lastFallDamageTime = new HashMap<>();
    private final Map<UUID, Long> lastViolationTime = new HashMap<>();

    /** Chronological log of recent violations (oldest first, max {@value #MAX_LOG_SIZE}). */
    private final java.util.ArrayDeque<ViolationEntry> violationLog = new java.util.ArrayDeque<>();

    // ── New: multi-target KillAura tracking ──────────────────────────────────
    /** Maps attacker UUID → (target UUID → last attack timestamp). */
    private final Map<UUID, Map<UUID, Long>> attackTargets = new HashMap<>();

    // ── New: Timer / AutoClicker packet-rate tracking ─────────────────────────
    /** POSITION / POSITION_LOOK packet arrival times (last 1 s). */
    private final Map<UUID, List<Long>> movePktTimes  = new HashMap<>();
    /** ARM_ANIMATION packet arrival times (last 1 s). */
    private final Map<UUID, List<Long>> armSwingTimes = new HashMap<>();

    // ── New: NoFall Y-drop tracking ───────────────────────────────────────────
    /** Highest Y recorded while the player was airborne in the current flight. */
    private final Map<UUID, Double> airPeakY = new HashMap<>();
    /** Whether the player was in the air in the last movement tick. */
    private final Map<UUID, Boolean> wasInAir = new HashMap<>();

    // ── Teleport whitelist (prevents false TeleportHack flags) ───────────────
    /** Timestamp of last legitimate teleport per player. */
    private final Map<UUID, Long> recentTeleport = new HashMap<>();

    public AntiCheatManager(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("anticheat.enabled", true);
    }

    public boolean isCheckEnabled(String check) {
        return plugin.getConfig().getBoolean("anticheat." + check + ".enabled", true);
    }

    public void addViolation(UUID playerId, String checkName) {
        // Skip players who are exempt from warnings.
        String playerName = getPlayerName(playerId);
        List<String> exempt = plugin.getConfig().getStringList("anticheat.exempt-players");
        if (exempt.stream().anyMatch(e -> e.equalsIgnoreCase(playerName))) return;

        int count = violations.merge(playerId, 1, Integer::sum);
        int threshold = plugin.getConfig().getInt("anticheat.violations-before-action", 10);

        // Append to violation log.
        synchronized (violationLog) {
            if (violationLog.size() >= MAX_LOG_SIZE) violationLog.pollFirst();
            violationLog.addLast(new ViolationEntry(playerName, checkName, System.currentTimeMillis()));
        }

        if (plugin.getConfig().getBoolean("anticheat.alert-admins", true)) {
            String alert = "&c[AntiCheat] &e" + getPlayerName(playerId) + " &chat einen Verstoß gegen &e" + checkName
                    + " &c(Verstoß #" + count + ")";
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("klassenplugin.anticheat.alert")) {
                    online.sendMessage(KlassenPlugin.colorizeComponent(alert));
                }
            }
        }

        if (count >= threshold) {
            takeAction(playerId, checkName);
            violations.put(playerId, 0);
        }
    }

    public void takeAction(UUID playerId, String checkName) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;

        String action = plugin.getConfig().getString("anticheat.action", "ban");
        String reason = checkName + " Hack/Exploit";

        switch (action.toLowerCase()) {
            case "kick" -> player.kick(buildKickScreen(
                    "&c&lGEKICKT", reason, "Du kannst dich sofort wieder verbinden."));
            case "ban" -> {
                String banMessage = buildBanReason(reason, "AntiCheat");
                @SuppressWarnings("deprecation")
                org.bukkit.BanEntry<?> ignored = player.ban(banMessage, (java.util.Date) null, "AntiCheat");
                player.kick(buildKickScreen("&4&lGEBANNT – CHEATING DETECTED",
                        reason,
                        "&7Grund: &c" + reason
                                + "\n&7Gebannt von: &cAntiCheat"
                                + "\n\n&7Um entbannt zu werden, wende dich an einen Admin."));
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("klassenplugin.anticheat.alert")) {
                        online.sendMessage(KlassenPlugin.colorizeComponent(
                                "&4&l[AntiCheat-BAN] &e" + player.getName()
                                        + " &cwurde automatisch gebannt! &8(" + checkName + ")"));
                    }
                }
                plugin.getLogger().warning("[AntiCheat] " + player.getName() + " wurde automatisch gebannt: " + checkName);
            }
            default -> player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&c[AntiCheat] &eWarnung: &c" + reason));
        }
    }

    /**
     * Builds a formatted multi-line kick/ban screen component.
     */
    public static net.kyori.adventure.text.Component buildKickScreen(
            String title, String subtitle, String details) {
        String msg = "\n&r"
                + "&8" + "▀".repeat(40) + "\n"
                + "\n"
                + "  " + title + "\n"
                + "\n"
                + "  &e" + subtitle + "\n"
                + "\n"
                + "  " + details + "\n"
                + "\n"
                + "&8" + "▄".repeat(40) + "\n";
        return KlassenPlugin.colorizeComponent(msg);
    }

    /**
     * Returns a short plain-text ban reason (stored in Bukkit's ban list).
     */
    private static String buildBanReason(String reason, String bannedBy) {
        return "[" + bannedBy + "] " + reason;
    }

    public void recordOreMine(UUID playerId) {
        long now = System.currentTimeMillis();
        List<Long> times = oreMineTimes.computeIfAbsent(playerId, k -> new ArrayList<>());
        times.add(now);

        long windowMs = plugin.getConfig().getLong("anticheat.xray.time-window", 60L) * 1000L;
        times.removeIf(t -> now - t > windowMs);
    }

    public int getOreCountInWindow(UUID playerId) {
        List<Long> times = oreMineTimes.get(playerId);
        if (times == null) return 0;

        long now = System.currentTimeMillis();
        long windowMs = plugin.getConfig().getLong("anticheat.xray.time-window", 60L) * 1000L;
        return (int) times.stream().filter(t -> now - t <= windowMs).count();
    }

    public Set<Material> getMonitoredOres() {
        Set<Material> result = new HashSet<>();
        List<String> oreNames = plugin.getConfig().getStringList("anticheat.xray.monitored-ores");
        for (String name : oreNames) {
            Material mat = Material.matchMaterial(name);
            if (mat == null) {
                plugin.getLogger().warning("[AntiCheat] Unbekanntes Material in monitored-ores: " + name);
            } else {
                result.add(mat);
            }
        }
        return result;
    }

    public void recordHit(UUID playerId) {
        long now = System.currentTimeMillis();
        List<Long> times = hitTimes.computeIfAbsent(playerId, k -> new ArrayList<>());
        times.add(now);
        times.removeIf(t -> now - t > 1000L);
    }

    public int getHitsInLastSecond(UUID playerId) {
        List<Long> times = hitTimes.get(playerId);
        if (times == null) return 0;
        long now = System.currentTimeMillis();
        return (int) times.stream().filter(t -> now - t <= 1000L).count();
    }

    public Location getAndUpdateLastPosition(UUID playerId, Location newLocation) {
        Location old = lastPosition.get(playerId);
        lastPosition.put(playerId, newLocation.clone());
        lastMoveTime.put(playerId, System.currentTimeMillis());
        return old;
    }

    public Long getLastMoveTime(UUID playerId) {
        return lastMoveTime.get(playerId);
    }

    public void incrementAirTicks(UUID playerId) {
        airTickCount.merge(playerId, 1, Integer::sum);
    }

    public void resetAirTicks(UUID playerId) {
        airTickCount.put(playerId, 0);
    }

    public int getAirTicks(UUID playerId) {
        return airTickCount.getOrDefault(playerId, 0);
    }

    public int getViolations(UUID playerId) {
        return violations.getOrDefault(playerId, 0);
    }

    public void resetViolations(UUID playerId) {
        violations.put(playerId, 0);
    }

    public boolean canAddViolation(UUID playerId) {
        long now = System.currentTimeMillis();
        Long last = lastViolationTime.get(playerId);
        if (last != null && now - last < 1000L) return false;
        lastViolationTime.put(playerId, now);
        return true;
    }

    public void recordBlockPlace(UUID playerId) {
        long now = System.currentTimeMillis();
        List<Long> times = blockPlaceTimes.computeIfAbsent(playerId, k -> new ArrayList<>());
        times.add(now);
        times.removeIf(t -> now - t > 1000L);
    }

    public int getBlockPlacesInLastSecond(UUID playerId) {
        List<Long> times = blockPlaceTimes.get(playerId);
        if (times == null) return 0;
        long now = System.currentTimeMillis();
        return (int) times.stream().filter(t -> now - t <= 1000L).count();
    }

    public void recordFallDamage(UUID playerId) {
        lastFallDamageTime.put(playerId, System.currentTimeMillis());
    }

    public boolean hadRecentFallDamage(UUID playerId) {
        Long t = lastFallDamageTime.get(playerId);
        if (t == null) return false;
        return System.currentTimeMillis() - t < 3000L;
    }

    /**
     * Returns the timestamp (ms) of the most recent fall-damage event for
     * {@code playerId}, or {@code -1} if no fall damage has been recorded.
     *
     * <p>Used by the delayed NoFall check to determine whether fall damage was
     * applied <em>after</em> a specific landing timestamp, which allows the
     * check to run correctly even when {@link
     * org.bukkit.event.entity.EntityDamageEvent} fires in a later tick than
     * {@link org.bukkit.event.player.PlayerMoveEvent}.
     */
    public long getLastFallDamageTime(UUID playerId) {
        Long t = lastFallDamageTime.get(playerId);
        return t != null ? t : -1L;
    }

    // ── Multi-target KillAura ─────────────────────────────────────────────────

    /**
     * Records that {@code attacker} has hit {@code target}.
     * Expires entries older than 500 ms automatically.
     */
    public void recordAttackTarget(UUID attacker, UUID target) {
        long now = System.currentTimeMillis();
        Map<UUID, Long> targets = attackTargets.computeIfAbsent(attacker, k -> new HashMap<>());
        targets.put(target, now);
        targets.entrySet().removeIf(e -> now - e.getValue() > 500L);
    }

    /**
     * Returns how many distinct entities {@code attacker} has hit within the
     * last 500 ms.  KillAura typically hits several targets faster than a human
     * can aim-switch (normally ≤ 1–2 per 500 ms in PvP).
     */
    public int getDistinctTargetsInWindow(UUID attacker) {
        Map<UUID, Long> targets = attackTargets.get(attacker);
        if (targets == null) return 0;
        long now = System.currentTimeMillis();
        return (int) targets.entrySet().stream()
                .filter(e -> now - e.getValue() <= 500L)
                .count();
    }

    // ── Timer hack / AutoClicker packet-rate helpers ──────────────────────────

    public void recordMovePkt(UUID playerId) {
        long now = System.currentTimeMillis();
        List<Long> times = movePktTimes.computeIfAbsent(playerId, k -> new ArrayList<>());
        times.add(now);
        times.removeIf(t -> now - t > 1000L);
    }

    public int getMovePktsInLastSecond(UUID playerId) {
        List<Long> times = movePktTimes.get(playerId);
        if (times == null) return 0;
        long now = System.currentTimeMillis();
        return (int) times.stream().filter(t -> now - t <= 1000L).count();
    }

    public void recordArmSwing(UUID playerId) {
        long now = System.currentTimeMillis();
        List<Long> times = armSwingTimes.computeIfAbsent(playerId, k -> new ArrayList<>());
        times.add(now);
        times.removeIf(t -> now - t > 1000L);
    }

    public int getArmSwingsInLastSecond(UUID playerId) {
        List<Long> times = armSwingTimes.get(playerId);
        if (times == null) return 0;
        long now = System.currentTimeMillis();
        return (int) times.stream().filter(t -> now - t <= 1000L).count();
    }

    // ── NoFall Y-drop tracking ────────────────────────────────────────────────

    /** Called every movement tick.  Keeps the highest Y seen while airborne. */
    public void updateAirPeak(UUID playerId, double currentY, boolean onGround) {
        if (onGround) {
            wasInAir.put(playerId, false);
            // Do not clear airPeakY here – the caller needs it after landing.
            return;
        }
        wasInAir.put(playerId, true);
        Double peak = airPeakY.get(playerId);
        if (peak == null || currentY > peak) {
            airPeakY.put(playerId, currentY);
        }
    }

    /** Returns {@code true} if the player was airborne in the previous tick. */
    public boolean wasInAir(UUID playerId) {
        return Boolean.TRUE.equals(wasInAir.get(playerId));
    }

    /**
     * Returns the peak Y recorded during the most recent airborne period, or
     * {@code null} if no airborne phase was tracked.
     */
    public Double getAirPeakY(UUID playerId) {
        return airPeakY.get(playerId);
    }

    /** Clears the airborne-peak Y after a NoFall check. */
    public void clearAirPeakY(UUID playerId) {
        airPeakY.remove(playerId);
    }

    // ── Teleport whitelist ────────────────────────────────────────────────────

    /**
     * Notifies the anti-cheat that {@code playerId} just performed a legitimate
     * teleport.  The TeleportHack check is suppressed for 2 seconds afterwards.
     */
    public void notifyTeleport(UUID playerId) {
        recentTeleport.put(playerId, System.currentTimeMillis());
        // Also reset air state so Fly/NoFall don't flag the landing after teleport.
        resetAirTicks(playerId);
        clearAirPeakY(playerId);
        // Reset ProtocolLib FreeCam stall counters so the new position doesn't
        // immediately trigger a FreeCam violation.
        if (plugin.getProtocolLibManager() != null
                && plugin.getProtocolLibManager().getPacketListener() != null) {
            plugin.getProtocolLibManager().getPacketListener().resetFreeCamState(playerId);
        }
    }

    /**
     * Returns {@code true} if the player teleported within the last 2 seconds
     * and should therefore be exempt from position-jump detection.
     */
    public boolean wasRecentlyTeleported(UUID playerId) {
        Long t = recentTeleport.get(playerId);
        if (t == null) return false;
        if (System.currentTimeMillis() - t > 2000L) {
            recentTeleport.remove(playerId);
            return false;
        }
        return true;
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    public void removePlayer(UUID playerId) {
        oreMineTimes.remove(playerId);
        violations.remove(playerId);
        hitTimes.remove(playerId);
        lastPosition.remove(playerId);
        lastMoveTime.remove(playerId);
        airTickCount.remove(playerId);
        blockPlaceTimes.remove(playerId);
        lastFallDamageTime.remove(playerId);
        lastViolationTime.remove(playerId);
        attackTargets.remove(playerId);
        movePktTimes.remove(playerId);
        armSwingTimes.remove(playerId);
        airPeakY.remove(playerId);
        wasInAir.remove(playerId);
        recentTeleport.remove(playerId);
    }

    public Map<UUID, Integer> getViolationMap() {
        return Collections.unmodifiableMap(violations);
    }

    /**
     * Returns a snapshot of the recent violation log (newest first).
     * At most {@value #MAX_LOG_SIZE} entries are kept.
     */
    public List<ViolationEntry> getRecentViolations() {
        synchronized (violationLog) {
            List<ViolationEntry> list = new ArrayList<>(violationLog);
            Collections.reverse(list); // newest first
            return list;
        }
    }

    private String getPlayerName(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) return player.getName();
        return playerId.toString().substring(0, 8) + "...";
    }
}
