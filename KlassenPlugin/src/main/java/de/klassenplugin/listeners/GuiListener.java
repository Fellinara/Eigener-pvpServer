package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.gui.AuctionGui;
import de.klassenplugin.gui.OrderGui;
import de.klassenplugin.gui.RankListGui;
import de.klassenplugin.gui.RankPermissionsGui;
import de.klassenplugin.gui.SellGui;
import de.klassenplugin.gui.ShopGui;
import de.klassenplugin.gui.ViolationsGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

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
        if (AuctionGui.isAhGui(event.getView())) {
            event.setCancelled(true);
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                plugin.getAuctionGui().handleClick(player, event.getSlot());
            }
            return;
        }

        // Order GUI
        if (OrderGui.isOrderGui(event.getView())) {
            event.setCancelled(true);
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                plugin.getOrderGui().handleClick(player, event.getSlot());
            }
            return;
        }

        // Shop GUI
        if (ShopGui.isShopGui(event.getView())) {
            event.setCancelled(true);
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                plugin.getShopGui().handleClick(player, event.getSlot(), event.getClick());
            }
            return;
        }

        // Sell GUI – allow items to be placed freely; only block the info icon slot
        if (SellGui.isSellGui(event.getView())) {
            if (event.getClickedInventory().equals(event.getView().getTopInventory())
                    && event.getSlot() == 35) {
                event.setCancelled(true);
            }
            return;
        }

        // Rank List GUI (rank selection screen)
        if (RankListGui.isRankListGui(event.getView())) {
            event.setCancelled(true);
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                plugin.getRankListGui().handleClick(player, event.getSlot());
            }
            return;
        }

        // Rank Permissions GUI
        if (RankPermissionsGui.isRankGui(event.getView())) {
            event.setCancelled(true);
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                plugin.getRankPermissionsGui().handleClick(player, event.getSlot());
            }
            return;
        }

        // Violations GUI
        if (ViolationsGui.isViolationsGui(event.getView())) {
            event.setCancelled(true);
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                boolean shift = event.getClick().isShiftClick();
                plugin.getViolationsGui().handleClick(player, event.getSlot(), shift);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (ShopGui.isShopGui(event.getView())) {
            plugin.getShopGui().cleanup(player);
        } else if (SellGui.isSellGui(event.getView())) {
            plugin.getSellGui().processSell(player, event.getView().getTopInventory());
        } else if (RankListGui.isRankListGui(event.getView())) {
            plugin.getRankListGui().cleanup(player);
        } else if (RankPermissionsGui.isRankGui(event.getView())) {
            plugin.getRankPermissionsGui().cleanup(player);
        } else if (ViolationsGui.isViolationsGui(event.getView())) {
            plugin.getViolationsGui().cleanup(player);
        }
    }
}
