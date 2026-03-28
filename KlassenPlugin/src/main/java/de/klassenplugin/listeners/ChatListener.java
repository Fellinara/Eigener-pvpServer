package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final KlassenPlugin plugin;

    public ChatListener(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.isPluginEnabled()) return;

        Player player = event.getPlayer();
        String prefix = plugin.getRankManager().getPlayerPrefix(player.getUniqueId());
        if (prefix.isEmpty()) return;

        Component prefixComp = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix);
        event.renderer((source, displayName, message, viewer) ->
                Component.text()
                        .append(prefixComp)
                        .append(Component.text(" "))
                        .append(displayName)
                        .append(Component.text(": "))
                        .append(message)
                        .build()
        );
    }
}
