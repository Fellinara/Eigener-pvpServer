package de.duellplugin.listeners;

import de.duellplugin.DuellPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final DuellPlugin plugin;

    public ChatListener(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        var stats = plugin.getStatsManager().getStats(player.getUniqueId());
        if (stats == null) return;

        String rankPrefix = stats.getRank().getDisplayName();
        // Build a controlled format with the rank prefix, then the vanilla "name: message" structure
        event.setFormat(rankPrefix + " §7%1$s§f: %2$s");
    }
}
