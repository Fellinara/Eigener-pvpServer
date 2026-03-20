package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class JoinListener implements Listener {

    private final KlassenPlugin plugin;

    public JoinListener(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    /** Block non-admins while maintenance is active (login phase = before world load). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (plugin.getMaintenanceManager().isActive()
                && !event.getPlayer().hasPermission("klassenplugin.maintenance.bypass")) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    KlassenPlugin.colorizeComponent(plugin.getMaintenanceManager().buildKickMessage()));
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
    }
}
