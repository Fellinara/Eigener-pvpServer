package de.duellplugin.listeners;

import de.duellplugin.DuellPlugin;
import de.duellplugin.gui.*;
import de.duellplugin.managers.NpcManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.UUID;

public class NpcListener implements Listener {

    private final DuellPlugin plugin;

    public NpcListener(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;

        UUID npcUUID = villager.getUniqueId();
        if (!plugin.getNpcManager().isNpc(npcUUID)) return;

        event.setCancelled(true); // prevent trading GUI

        Player player = event.getPlayer();
        NpcManager.NpcType type = plugin.getNpcManager().getNpcType(npcUUID);
        if (type == null) return;

        switch (type) {
            case MAIN -> openMainMenu(player);
            case KITS -> new KitGUI(plugin).open(player);
            case STATS -> new StatsGUI(plugin).open(player);
            case DUELL -> new DuellGUI(plugin).open(player);
            case BOT -> new BotGUI(plugin).open(player);
            case FREUND -> player.sendMessage(plugin.getPrefix() + "§7Nutze §e/freund §7für Freunde-Verwaltung.");
            case PARTY -> player.sendMessage(plugin.getPrefix() + "§7Nutze §e/party §7für Party-Verwaltung.");
        }
    }

    /** Opens a main menu that links to all features. */
    private void openMainMenu(Player player) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(
                null, 27, "§6§l◆ PvP Server");

        // Slot 10 – Duell
        inv.setItem(10, makeItem(org.bukkit.Material.DIAMOND_SWORD, "§c§l⚔ Duell starten",
                "§7Fordere einen Spieler heraus", "§eKlick zum Öffnen"));
        // Slot 11 – Kits
        inv.setItem(11, makeItem(org.bukkit.Material.CHEST, "§e§l🎒 Kit auswählen",
                "§7Wähle dein Kampf-Kit", "§eKlick zum Öffnen"));
        // Slot 12 – Bot
        inv.setItem(12, makeItem(org.bukkit.Material.ZOMBIE_HEAD, "§4§l☠ Bot-Kampf",
                "§7Kämpfe gegen einen Bot", "§eKlick zum Öffnen"));
        // Slot 13 – Stats
        inv.setItem(13, makeItem(org.bukkit.Material.BOOK, "§b§l📊 Statistiken",
                "§7Deine Kampf-Statistiken", "§eKlick zum Öffnen"));
        // Slot 14 – Party
        inv.setItem(14, makeItem(org.bukkit.Material.PLAYER_HEAD, "§d§l♜ Party",
                "§7Party-System verwalten", "§7Nutze /party"));
        // Slot 15 – Freunde
        inv.setItem(15, makeItem(org.bukkit.Material.PAPER, "§a§l☺ Freunde",
                "§7Freundesliste verwalten", "§7Nutze /freund"));
        // Slot 16 – Rang
        inv.setItem(16, makeItem(org.bukkit.Material.NETHER_STAR, "§6§l★ Rang",
                "§7Ränge mit ELO kaufen", "§7Nutze /rang info"));

        // Border glass
        org.bukkit.inventory.ItemStack border = makeItem(org.bukkit.Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }

        player.openInventory(inv);
    }

    private org.bukkit.inventory.ItemStack makeItem(org.bukkit.Material mat, String name, String... lore) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(java.util.Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
