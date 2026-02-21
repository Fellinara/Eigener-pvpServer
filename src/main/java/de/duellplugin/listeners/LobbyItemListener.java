package de.duellplugin.listeners;

import de.duellplugin.DuellPlugin;
import de.duellplugin.gui.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LobbyItemListener implements Listener {

    private final DuellPlugin plugin;

    public LobbyItemListener(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (plugin.getDuellManager().isInDuel(player.getUniqueId())
                || plugin.getBotManager().isInBotFight(player.getUniqueId())) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String name = meta.getDisplayName();
        event.setCancelled(true);

        if (name.contains("Duell-Menü")) {
            new DuellGUI(plugin).open(player);
        } else if (name.contains("Bot-Kampf")) {
            new BotGUI(plugin).open(player);
        } else if (name.contains("Kit-Auswahl")) {
            new KitGUI(plugin).open(player);
        } else if (name.contains("Arena-Auswahl")) {
            new ArenaGUI(plugin).open(player);
        } else if (name.contains("Statistiken")) {
            new StatsGUI(plugin).open(player);
        } else if (name.contains("Spieler verstecken") || name.contains("Spieler zeigen")) {
            plugin.getLobbyManager().togglePlayerVisibility(player);
        } else if (name.contains("Navigator")) {
            plugin.getLobbyManager().sendToLobby(player);
            player.sendMessage("§aDu wurdest zur Lobby teleportiert!");
        }
    }
}
