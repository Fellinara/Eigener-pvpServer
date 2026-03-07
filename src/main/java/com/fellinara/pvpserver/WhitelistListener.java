package com.fellinara.pvpserver;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class WhitelistListener implements Listener {

    private static final Component KICK_MESSAGE = Component.empty()
            .append(Component.text("⚙ Wartungsarbeiten ⚙", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.newline())
            .append(Component.newline())
            .append(Component.text("Wir befinden uns in Wartungsarbeiten", NamedTextColor.YELLOW))
            .append(Component.newline())
            .append(Component.text("und wir sind gleich wieder für Sie da.", NamedTextColor.YELLOW))
            .append(Component.newline())
            .append(Component.newline())
            .append(Component.text("Wir bitten um Ihr Verständnis.", NamedTextColor.GREEN));

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, KICK_MESSAGE);
        }
    }
}
