package de.duellplugin.gui;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;

public class StatsGUI {

    private final DuellPlugin plugin;

    public StatsGUI(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, "§b📊 Statistiken");

        PlayerStats stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName("§6" + player.getName());
            skullMeta.setLore(Arrays.asList(
                    "§7━━━━━━━━━━━━━━━━━━",
                    "§7ELO-Rating: §f" + stats.getElo(),
                    "§7Rang: " + getEloRank(stats.getElo()),
                    "§7━━━━━━━━━━━━━━━━━━"
            ));
            skull.setItemMeta(skullMeta);
        }
        inv.setItem(4, skull);

        inv.setItem(19, createStatItem(Material.DIAMOND_SWORD, "§a⚔ Siege",
                "§7Gesamt: §a" + stats.getWins()));

        inv.setItem(20, createStatItem(Material.SKELETON_SKULL, "§c☠ Niederlagen",
                "§7Gesamt: §c" + stats.getLosses()));

        inv.setItem(21, createStatItem(Material.GOLDEN_SWORD, "§6📊 K/D Ratio",
                "§7Ratio: §f" + String.format("%.2f", stats.getKD())));

        inv.setItem(22, createStatItem(Material.EXPERIENCE_BOTTLE, "§b🎯 Gewinnrate",
                "§7Rate: §f" + String.format("%.1f%%", stats.getWinRate())));

        inv.setItem(23, createStatItem(Material.BLAZE_POWDER, "§e🔥 Kill-Streak",
                "§7Aktuell: §e" + stats.getKillStreak(),
                "§7Bestleistung: §6" + stats.getBestKillStreak()));

        inv.setItem(24, createStatItem(Material.NETHER_STAR, "§d⭐ Gesamte Spiele",
                "§7Spiele: §f" + stats.getTotalGames()));

        inv.setItem(25, createStatItem(Material.ZOMBIE_HEAD, "§c🤖 Bot-Statistiken",
                "§7Siege: §a" + stats.getBotWins(),
                "§7Niederlagen: §c" + stats.getBotLosses(),
                "§7Bot-Stärke: §6" + stats.getBotRating() + " §7/ 100"));

        List<PlayerStats> topPlayers = plugin.getStatsManager().getTopPlayers(5);
        ItemStack leaderboard = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta lbMeta = leaderboard.getItemMeta();
        if (lbMeta != null) {
            lbMeta.setDisplayName("§6🏆 Top 5 Spieler");
            String[] lore = new String[topPlayers.size() + 1];
            lore[0] = "§7━━━━━━━━━━━━━━━━━━";
            for (int i = 0; i < topPlayers.size(); i++) {
                PlayerStats ps = topPlayers.get(i);
                String medal = switch (i) {
                    case 0 -> "§6🥇";
                    case 1 -> "§f🥈";
                    case 2 -> "§e🥉";
                    default -> "§7#" + (i + 1);
                };
                lore[i + 1] = medal + " §e" + ps.getName() + " §7- §f" + ps.getElo() + " ELO";
            }
            lbMeta.setLore(Arrays.asList(lore));
            leaderboard.setItemMeta(lbMeta);
        }
        inv.setItem(40, leaderboard);

        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        for (int i = 0; i < 45; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createStatItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(loreLines));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String getEloRank(int elo) {
        if (elo >= 2500) return "§4§l⚔ Legende";
        if (elo >= 2000) return "§d§l★ Großmeister";
        if (elo >= 1700) return "§5★ Meister";
        if (elo >= 1400) return "§c★ Diamant";
        if (elo >= 1200) return "§6★ Gold";
        if (elo >= 1000) return "§f★ Silber";
        if (elo >= 800) return "§e★ Bronze";
        return "§7★ Eisen";
    }
}
