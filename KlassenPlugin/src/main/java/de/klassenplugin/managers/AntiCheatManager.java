package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class AntiCheatManager {

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
        int count = violations.merge(playerId, 1, Integer::sum);
        int threshold = plugin.getConfig().getInt("anticheat.violations-before-action", 10);

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

        String action = plugin.getConfig().getString("anticheat.action", "warn");
        String reason = "AntiCheat: " + checkName + " Verstoß";

        switch (action.toLowerCase()) {
            case "kick" -> player.kick(KlassenPlugin.colorizeComponent("&cDu wurdest vom Anti-Cheat gekickt!\n&e" + reason));
            case "ban" -> {
                // Use the name-based ban list (non-deprecated approach) via Bukkit BanList
                @SuppressWarnings("deprecation")
                org.bukkit.BanList<String> banList = Bukkit.getBanList(org.bukkit.BanList.Type.NAME);
                banList.addBan(player.getName(), reason, (java.util.Date) null, "AntiCheat");
                player.kick(KlassenPlugin.colorizeComponent("&cDu wurdest gebannt!\n&e" + reason));
            }
            default -> player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&c[AntiCheat] &eWarnung: &c" + reason));
        }
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
    }

    public Map<UUID, Integer> getViolationMap() {
        return Collections.unmodifiableMap(violations);
    }

    private String getPlayerName(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) return player.getName();
        return playerId.toString().substring(0, 8) + "...";
    }
}
