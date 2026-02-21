package de.duellplugin.listeners;

import de.duellplugin.DuellPlugin;
import de.duellplugin.gui.BotGUI;
import de.duellplugin.models.Kit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class GUIClickListener implements Listener {

    private final DuellPlugin plugin;

    public GUIClickListener(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();

        if (title.contains("⚔ Duell") || title.contains("☠ Bot") || title.contains("Kit-Auswahl")
                || title.contains("Arena-Auswahl") || title.contains("Statistiken")
                || title.contains("◆ PvP Server")) {
            event.setCancelled(true);
        }

        if (clicked == null || clicked.getType() == Material.AIR
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        if (title.contains("◆ PvP Server")) {
            handleMainMenuClick(player, clicked);
        } else if (title.contains("⚔ Duell")) {
            handleDuellClick(player, clicked);
        } else if (title.contains("☠ Bot")) {
            handleBotClick(player, event.getSlot());
        } else if (title.contains("Kit-Auswahl")) {
            handleKitClick(player, clicked, event.getClick());
        } else if (title.contains("Arena-Auswahl")) {
            handleArenaClick(player, clicked);
        }
    }

    private void handleMainMenuClick(Player player, ItemStack clicked) {
        if (!clicked.hasItemMeta()) return;
        String name = clicked.getItemMeta().getDisplayName();
        player.closeInventory();
        if (name.contains("Duell")) {
            new de.duellplugin.gui.DuellGUI(plugin).open(player);
        } else if (name.contains("Kit")) {
            new de.duellplugin.gui.KitGUI(plugin).open(player);
        } else if (name.contains("Bot")) {
            new de.duellplugin.gui.BotGUI(plugin).open(player);
        } else if (name.contains("Statistiken")) {
            new de.duellplugin.gui.StatsGUI(plugin).open(player);
        } else if (name.contains("Rang")) {
            player.performCommand("rang info");
        } else if (name.contains("Party")) {
            player.performCommand("party");
        } else if (name.contains("Freunde")) {
            player.performCommand("freund liste");
        }
    }

    private void handleDuellClick(Player player, ItemStack clicked) {
        if (clicked.getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        if (meta == null || meta.getOwningPlayer() == null) return;

        Player target = meta.getOwningPlayer().getPlayer();
        if (target == null || !target.isOnline()) {
            player.sendMessage("§cDieser Spieler ist nicht mehr online!");
            player.closeInventory();
            return;
        }

        plugin.getDuellManager().sendRequest(player, target);
        player.closeInventory();
    }

    private void handleBotClick(Player player, int slot) {
        int level = BotGUI.getLevelFromSlot(slot);
        if (level == -1) return;

        player.closeInventory();
        plugin.getBotManager().startBotFight(player, level);
    }

    private void handleKitClick(Player player, ItemStack clicked, org.bukkit.event.inventory.ClickType clickType) {
        if (!clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String displayName = meta.getDisplayName();
        for (Kit kit : plugin.getKitManager().getAllKits()) {
            if (displayName.contains(kit.getName()) || displayName.contains(kit.getDisplayName())) {
                var stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());

                if (clickType.isShiftClick()) {
                    // Move kit up in personal order
                    List<String> order = new ArrayList<>(stats.getKitOrder());
                    // Ensure all kits are in the order list
                    plugin.getKitManager().getAllKits().forEach(k -> {
                        if (!order.contains(k.getName())) order.add(k.getName());
                    });
                    stats.setKitOrder(order);
                    stats.moveKitUp(kit.getName());
                    plugin.getStatsManager().saveStats();
                    player.sendMessage("§6Kit " + kit.getDisplayName() + " §6nach oben verschoben!");
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) new de.duellplugin.gui.KitGUI(plugin).open(player);
                    }, 1L);
                } else if (clickType == org.bukkit.event.inventory.ClickType.RIGHT) {
                    // Move kit down in personal order
                    List<String> order = new ArrayList<>(stats.getKitOrder());
                    plugin.getKitManager().getAllKits().forEach(k -> {
                        if (!order.contains(k.getName())) order.add(k.getName());
                    });
                    stats.setKitOrder(order);
                    stats.moveKitDown(kit.getName());
                    plugin.getStatsManager().saveStats();
                    player.sendMessage("§6Kit " + kit.getDisplayName() + " §6nach unten verschoben!");
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        new de.duellplugin.gui.KitGUI(plugin).open(player);
                    }, 1L);
                } else {
                    // Regular left-click: select kit
                    stats.setSelectedKit(kit.getName());
                    plugin.getStatsManager().saveStats();
                    player.sendMessage("§aKit §6" + kit.getDisplayName() + " §aausgewählt!");
                    player.closeInventory();
                }
                return;
            }
        }
    }

    private void handleArenaClick(Player player, ItemStack clicked) {
        if (clicked.getType() != Material.LIME_STAINED_GLASS_PANE) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String arenaName = meta.getDisplayName().replace("§e", "").toLowerCase();
        var arena = plugin.getArenaManager().getArena(arenaName);

        if (arena != null && arena.isReady() && !arena.isInUse()) {
            player.sendMessage("§aArena §6" + arena.getName() + " §aausgewählt!");
        }
        player.closeInventory();
    }
}
