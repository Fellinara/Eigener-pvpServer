package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.time.LocalTime;

public class JoinListener implements Listener {

    private final KlassenPlugin plugin;

    public JoinListener(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Block non-admins when maintenance is active or during the night lock (00:00–07:00).
     * Maintenance takes priority: if it is active the night-lock check is skipped so
     * players only receive one clear kick message.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (plugin.getMaintenanceManager().isActive()
                && !event.getPlayer().hasPermission("klassenplugin.maintenance.bypass")) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    KlassenPlugin.colorizeComponent(plugin.getMaintenanceManager().buildKickMessage()));
            return; // Maintenance has priority; skip further checks.
        }

        // Night lock: server is closed from 00:00 to 07:00
        if (plugin.getConfig().getBoolean("night-lock.enabled", false)) {
            LocalTime now = LocalTime.now();
            LocalTime lockEnd = LocalTime.of(7, 0);
            if (now.isBefore(lockEnd) && !event.getPlayer().hasPermission("klassenplugin.nightlock.bypass")) {
                String msg = plugin.getConfig().getString("night-lock.kick-message",
                        "&c&lServer gesperrt!\n\n&7Der Server ist von &e00:00 &7bis &e07:00 Uhr &7gesperrt.\n&7Bitte versuche es sp\u00e4ter erneut.");
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                        KlassenPlugin.colorizeComponent(msg));
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.isPluginEnabled()) return;

        boolean joinMessageEnabled = plugin.getConfig().getBoolean("join-message.enabled", true);
        if (joinMessageEnabled) {
            String rawMessage = plugin.getConfig().getString("join-message.message",
                    "&aWillkommen auf dem Server, &e{player}&a!");
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&bKlassenPlugin&8] ");
            String message = (prefix + rawMessage).replace("{player}", event.getPlayer().getName());
            Component component = KlassenPlugin.colorizeComponent(message);
            event.getPlayer().sendMessage(component);
        }

        plugin.getRankManager().applyRankToPlayer(event.getPlayer());
        plugin.getScoreboardManager().setup(event.getPlayer());

        // Apply vanish state: hide any already-vanished players from this newcomer.
        plugin.getVanishManager().applyVanishOnJoin(event.getPlayer());

        // Deliver any items from auctions that expired while the player was offline.
        plugin.getAuctionManager().deliverPendingItems(event.getPlayer());
    }
}
