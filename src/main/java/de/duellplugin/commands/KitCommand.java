package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.gui.KitGUI;
import de.duellplugin.models.Kit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KitCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public KitCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        String prefix = plugin.getPrefix();

        if (args.length == 0) {
            new KitGUI(plugin).open(player);
            return true;
        }

        // /kit savelayout <kitname> — save current inventory arrangement as preferred layout
        if (args[0].equalsIgnoreCase("savelayout") || args[0].equalsIgnoreCase("layout")) {
            if (args.length < 2) {
                player.sendMessage(prefix + "§cBenutzung: §f/kit savelayout <kit>");
                return true;
            }
            String kitName = args[1].toLowerCase();
            Kit kit = plugin.getKitManager().getKit(kitName);
            if (kit == null) {
                player.sendMessage(prefix + "§cKit '§6" + kitName + "§c' nicht gefunden!");
                return true;
            }
            int[] layout = buildLayoutFromInventory(kit, player);
            var stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());
            stats.setKitSlotLayout(kitName, layout);
            plugin.getStatsManager().saveStats();
            player.sendMessage(prefix + "§aInventar-Layout für Kit §6" + kit.getDisplayName() + " §agespeichert!");
            player.sendMessage(prefix + "§7Dieses Layout wird beim nächsten Kampf mit diesem Kit angewendet.");
            return true;
        }

        // /kit resetlayout <kitname> — remove custom layout for a kit
        if (args[0].equalsIgnoreCase("resetlayout")) {
            if (args.length < 2) {
                player.sendMessage(prefix + "§cBenutzung: §f/kit resetlayout <kit>");
                return true;
            }
            String kitName = args[1].toLowerCase();
            var stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());
            stats.removeKitSlotLayout(kitName);
            plugin.getStatsManager().saveStats();
            player.sendMessage(prefix + "§aLayout für Kit §6" + kitName + " §azurückgesetzt.");
            return true;
        }

        // /kit <name> — select kit (standard or custom)
        String kitName = args[0].toLowerCase();
        Kit kit = plugin.getKitManager().getKit(kitName);

        if (kit == null) {
            // Check custom kits
            var stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());
            if (stats.hasCustomKit(kitName)) {
                stats.setSelectedKit(kitName);
                plugin.getStatsManager().saveStats();
                player.sendMessage(prefix + "§aEigenes Kit §f" + kitName + " §aausgewählt!");
                return true;
            }
            player.sendMessage(prefix + "§cKit '§6" + kitName + "§c' nicht gefunden!");
            player.sendMessage(prefix + "§7Verfügbare Kits:");
            for (Kit k : plugin.getKitManager().getAllKits()) {
                player.sendMessage("§7 - §e" + k.getName() + " §7- " + k.getDisplayName());
            }
            if (!stats.getCustomKits().isEmpty()) {
                player.sendMessage(prefix + "§7Deine eigenen Kits:");
                for (String ckName : stats.getCustomKits().keySet()) {
                    player.sendMessage("§7 - §f" + ckName);
                }
            }
            return true;
        }

        var stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());
        stats.setSelectedKit(kit.getName());
        plugin.getStatsManager().saveStats();

        player.sendMessage(prefix + "§aKit §6" + kit.getDisplayName() + " §aausgewählt!");
        return true;
    }

    /**
     * Builds a slot-permutation array by comparing the player's current inventory to the
     * kit's default contents. layout[sourceSlot] = targetSlot (-1 = keep default).
     * Items are matched by type AND ItemMeta to correctly distinguish same-type stacks.
     */
    private int[] buildLayoutFromInventory(Kit kit, Player player) {
        ItemStack[] defaultContents = kit.getContents();
        ItemStack[] playerInv = player.getInventory().getStorageContents();
        int[] layout = new int[36];
        Arrays.fill(layout, -1);
        boolean[] targetUsed = new boolean[36];

        for (int src = 0; src < 36; src++) {
            ItemStack kitItem = defaultContents[src];
            if (kitItem == null) continue;
            // First pass: exact match (type + meta)
            for (int tgt = 0; tgt < 36; tgt++) {
                if (targetUsed[tgt]) continue;
                ItemStack pItem = playerInv[tgt];
                if (pItem != null && pItem.isSimilar(kitItem)) {
                    layout[src] = tgt;
                    targetUsed[tgt] = true;
                    break;
                }
            }
            // Second pass: type-only match if exact match not found
            if (layout[src] == -1) {
                for (int tgt = 0; tgt < 36; tgt++) {
                    if (targetUsed[tgt]) continue;
                    ItemStack pItem = playerInv[tgt];
                    if (pItem != null && pItem.getType() == kitItem.getType()) {
                        layout[src] = tgt;
                        targetUsed[tgt] = true;
                        break;
                    }
                }
            }
        }
        return layout;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("savelayout", "resetlayout"));
            for (Kit k : plugin.getKitManager().getAllKits()) {
                if (k.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    options.add(k.getName());
                }
            }
            // Add custom kits
            if (sender instanceof Player player) {
                var stats = plugin.getStatsManager().getStats(player.getUniqueId());
                if (stats != null) {
                    for (String ckName : stats.getCustomKits().keySet()) {
                        if (ckName.startsWith(args[0].toLowerCase())) options.add(ckName);
                    }
                }
            }
            return options;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("savelayout") || args[0].equalsIgnoreCase("layout")
                || args[0].equalsIgnoreCase("resetlayout"))) {
            List<String> kitNames = new ArrayList<>();
            for (Kit k : plugin.getKitManager().getAllKits()) {
                if (k.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    kitNames.add(k.getName());
                }
            }
            return kitNames;
        }
        return List.of();
    }
}
