package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Manages maintenance mode.
 *
 * <p>While maintenance is active:
 * <ul>
 *   <li>All non-admin players are immediately kicked.</li>
 *   <li>Any player without {@code klassenplugin.maintenance.bypass} who tries to join
 *       is kicked with a formatted maintenance screen.</li>
 * </ul>
 */
public class MaintenanceManager {

    private final KlassenPlugin plugin;
    private boolean active;

    public MaintenanceManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        // Persist across restarts via config.
        this.active = plugin.getConfig().getBoolean("maintenance.enabled", false);
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Enables maintenance mode: kicks all non-admins and saves state.
     */
    public void enable(String enabledBy) {
        if (active) return;
        active = true;
        plugin.getConfig().set("maintenance.enabled", true);
        plugin.saveConfig();

        String kickMessage = buildKickMessage();

        int kicked = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("klassenplugin.maintenance.bypass")) {
                player.kick(KlassenPlugin.colorizeComponent(kickMessage));
                kicked++;
            }
        }

        Bukkit.broadcast(KlassenPlugin.colorizeComponent(
                "&e&l[Wartung] &cWartungsarbeiten wurden von &e" + enabledBy
                        + " &cgestartet! &7" + kicked + " Spieler wurden gekickt."));
        plugin.getLogger().info("[Maintenance] Enabled by " + enabledBy + ". Kicked " + kicked + " players.");
    }

    /**
     * Disables maintenance mode.
     */
    public void disable(String disabledBy) {
        if (!active) return;
        active = false;
        plugin.getConfig().set("maintenance.enabled", false);
        plugin.saveConfig();

        Bukkit.broadcast(KlassenPlugin.colorizeComponent(
                "&a&l[Wartung] &aWartungsarbeiten wurden beendet von &e" + disabledBy
                        + "&a! Der Server ist wieder für alle offen."));
        plugin.getLogger().info("[Maintenance] Disabled by " + disabledBy + ".");
    }

    /**
     * Returns the formatted kick message for non-admins trying to join.
     */
    public String buildKickMessage() {
        String raw = plugin.getConfig().getString("maintenance.kick-message", null);
        if (raw != null && !raw.isBlank()) return raw;

        return "\n&8" + "▀".repeat(40) + "\n"
                + "\n"
                + "  &e&l⚙  WARTUNGSARBEITEN  ⚙\n"
                + "\n"
                + "  &7Der Server befindet sich gerade in der Wartung.\n"
                + "  &7Bitte versuche es später erneut.\n"
                + "\n"
                + "&8" + "▄".repeat(40);
    }
}
