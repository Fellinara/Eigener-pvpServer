package de.duellplugin.listeners;

import de.duellplugin.DuellPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinLeaveListener implements Listener {

    private final DuellPlugin plugin;

    public PlayerJoinLeaveListener(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String serverName = plugin.getConfig().getString("server-name", "Bestes PvP Duo");

        String joinMsg = plugin.getMsg("join",
                "&8[&a+&8] &6%player% &eist der PvP Arena von &6%server% &ebeigetreten!");
        joinMsg = joinMsg.replace("%player%", player.getName()).replace("%server%", serverName);
        event.setJoinMessage(joinMsg);

        var stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());
        // Show rank prefix in the tab player list
        player.setPlayerListName(stats.getRank().getDisplayName() + " §7" + player.getName());

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getLobbyManager().sendToLobby(player);
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        String leaveMsg = plugin.getMsg("leave",
                "&8[&c-&8] &7%player% &7hat den Server verlassen");
        leaveMsg = leaveMsg.replace("%player%", player.getName());
        event.setQuitMessage(leaveMsg);

        plugin.getDuellManager().handleDisconnect(player.getUniqueId());
        plugin.getPartyManager().handleDisconnect(player.getUniqueId());
        plugin.getFfaManager().handleDisconnect(player.getUniqueId());

        if (plugin.getBotManager().isInBotFight(player.getUniqueId())) {
            plugin.getBotManager().handlePlayerDeathInBotFight(player);
        }
    }
}
