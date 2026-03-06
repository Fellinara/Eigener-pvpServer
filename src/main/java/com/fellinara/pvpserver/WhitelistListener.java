package com.fellinara.pvpserver;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class WhitelistListener implements Listener {

    private static final String KICK_MESSAGE =
            "Fick dich fett, versuch nicht auf den Server drauf zu kommen!";

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, KICK_MESSAGE);
        }
    }
}
