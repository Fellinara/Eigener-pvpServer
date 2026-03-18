package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.gui.AuctionGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Routes inventory clicks into the appropriate GUI handler.
 */
public class GuiListener implements Listener {

    private final KlassenPlugin plugin;

    public GuiListener(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        // Auction House GUI
        if (AuctionGui.isAhGui(event.getView().getTopInventory())) {
            // Only handle clicks inside the GUI itself (top inventory), not player's inv
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
                plugin.getAuctionGui().handleClick(player, event.getSlot());
            } else {
                // Prevent moving items from player inventory into the GUI
                event.setCancelled(true);
            }
        }
    }
}
