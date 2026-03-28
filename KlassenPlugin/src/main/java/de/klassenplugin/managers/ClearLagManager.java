package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import java.util.ArrayList;
import java.util.List;

/**
 * Periodically removes dropped items and hostile mobs to reduce server lag.
 *
 * <p>Entities that are within {@code safe-radius-chunks} chunks of any player
 * are left alone to avoid disrupting active gameplay.
 *
 * <p>Configuration keys (all under {@code clearlag.*}):
 * <ul>
 *   <li>{@code enabled} – whether the task runs at all</li>
 *   <li>{@code interval-minutes} – how often to clean (default 30)</li>
 *   <li>{@code warnings-before-seconds} – list of warning countdown points</li>
 *   <li>{@code safe-radius-chunks} – minimum chunk distance from a player (default 3)</li>
 *   <li>{@code remove-mobs} – also remove hostile mobs (default true)</li>
 * </ul>
 */
public class ClearLagManager {

    private final KlassenPlugin plugin;
    private int taskId = -1;

    public ClearLagManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        if (plugin.getConfig().getBoolean("clearlag.enabled", false)) {
            schedule();
        }
    }

    /** Re-read config and reschedule. */
    public void reload() {
        cancel();
        if (plugin.getConfig().getBoolean("clearlag.enabled", false)) {
            schedule();
        }
    }

    public void cancel() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void schedule() {
        long intervalMinutes = plugin.getConfig().getLong("clearlag.interval-minutes", 30);
        long intervalTicks = intervalMinutes * 60L * 20L;

        // Schedule the initial delay equal to the interval, then repeat.
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,
                this::beginCountdown, intervalTicks, intervalTicks);
    }

    /**
     * Schedules warning messages and eventually triggers the cleanup.
     */
    private void beginCountdown() {
        List<Integer> warningSeconds = new ArrayList<>();
        for (Object o : plugin.getConfig().getList("clearlag.warnings-before-seconds",
                List.of(300, 60, 30, 10))) {
            if (o instanceof Number n) warningSeconds.add(n.intValue());
        }

        // Sort descending so we schedule them in order.
        warningSeconds.sort((a, b) -> b - a);

        for (int seconds : warningSeconds) {
            long delay = calculateDelay(warningSeconds, seconds);
            final int sec = seconds;
            Bukkit.getScheduler().runTaskLater(plugin, () -> broadcastWarning(sec), delay);
        }

        // Actual cleanup at the end (after all warnings).
        int maxWarning = warningSeconds.isEmpty() ? 0 : warningSeconds.get(0);
        Bukkit.getScheduler().runTaskLater(plugin, this::doClearLag, maxWarning * 20L);
    }

    /**
     * Delay (in ticks) from now until the warning for {@code targetSeconds} should fire.
     * The largest warning fires first (it's the earliest before the event).
     */
    private long calculateDelay(List<Integer> sorted, int targetSeconds) {
        // Largest value is fired first, so delay = 0 for the largest,
        // then increment for each smaller one.
        int max = sorted.isEmpty() ? 0 : sorted.get(0);
        return (long) (max - targetSeconds) * 20L;
    }

    private void broadcastWarning(int secondsLeft) {
        final String unit;
        if (secondsLeft >= 60) {
            int minutes = secondsLeft / 60;
            unit = minutes + " Minute" + (minutes > 1 ? "n" : "");
        } else {
            unit = secondsLeft + " Sekunde" + (secondsLeft != 1 ? "n" : "");
        }
        String msg = "&e&l[ClearLag] &7ClearLag in &c" + unit + "&7! Items und Mobs werden gelöscht!";
        Bukkit.broadcast(KlassenPlugin.colorizeComponent(msg));
    }

    private void doClearLag() {
        int safeChunks = plugin.getConfig().getInt("clearlag.safe-radius-chunks", 3);
        boolean removeMobs = plugin.getConfig().getBoolean("clearlag.remove-mobs", true);

        int itemsRemoved = 0;
        int mobsRemoved = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : new ArrayList<>(world.getEntities())) {
                if (isNearPlayer(entity, safeChunks)) continue;

                if (entity instanceof Item) {
                    entity.remove();
                    itemsRemoved++;
                } else if (removeMobs && entity instanceof Mob mob) {
                    // Don't remove tamed/named entities.
                    if (mob.getCustomName() != null) continue;
                    if (mob instanceof Tameable t && t.isTamed()) continue;
                    entity.remove();
                    mobsRemoved++;
                }
            }
        }

        String msg = "&a&l[ClearLag] &7Bereinigung abgeschlossen! &c" + itemsRemoved
                + " &7Items und &c" + mobsRemoved + " &7Mobs entfernt.";
        Bukkit.broadcast(KlassenPlugin.colorizeComponent(msg));
        plugin.getLogger().info("[ClearLag] Removed " + itemsRemoved + " items and " + mobsRemoved + " mobs.");
    }

    /**
     * Returns {@code true} if the entity is within {@code safeChunks} chunks of any online player.
     */
    private boolean isNearPlayer(Entity entity, int safeChunks) {
        int safeBlocks = safeChunks * 16;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(entity.getWorld())) continue;
            if (player.getLocation().distanceSquared(entity.getLocation()) <= (double) safeBlocks * safeBlocks) {
                return true;
            }
        }
        return false;
    }
}
