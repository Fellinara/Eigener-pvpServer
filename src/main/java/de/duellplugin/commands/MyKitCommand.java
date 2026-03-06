package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.CustomKit;
import de.duellplugin.models.PlayerStats;
import de.duellplugin.models.Rank;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * /mykit create <name>  – saves current inventory as a custom kit  (VIP+ required)
 * /mykit delete <name>  – removes a custom kit
 * /mykit list           – shows all saved custom kits
 */
public class MyKitCommand implements CommandExecutor, TabCompleter {

    /** Materials that are never allowed in custom kits. */
    private static final Set<Material> FORBIDDEN = Set.of(
            Material.TNT,
            Material.TNT_MINECART,
            Material.END_CRYSTAL,
            Material.FIRE_CHARGE,
            Material.FIREWORK_ROCKET,
            Material.FIREWORK_STAR,
            Material.RESPAWN_ANCHOR,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK,
            Material.JIGSAW,
            Material.BEDROCK
    );

    private final DuellPlugin plugin;

    public MyKitCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }

        String prefix = plugin.getPrefix();
        PlayerStats stats = plugin.getStatsManager().getOrCreateStats(player.getUniqueId(), player.getName());

        if (args.length == 0) {
            sendHelp(player, prefix);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create", "erstellen" -> {
                // Rank check: VIP or higher
                if (!hasKitPermission(stats.getRank())) {
                    player.sendMessage(prefix + "§cDu brauchst mindestens §aVIP§c, um eigene Kits erstellen zu können!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(prefix + "§cBenutzung: §f/mykit create <name>");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (!isValidName(name)) {
                    player.sendMessage(prefix + "§cUngültiger Kit-Name! (2-20 Zeichen, nur §fkleine §cBuchstaben/Zahlen/Unterstriche)");
                    return true;
                }
                int maxKits = maxKitsForRank(stats.getRank());
                if (stats.getCustomKits().size() >= maxKits && !stats.hasCustomKit(name)) {
                    player.sendMessage(prefix + "§cDu kannst maximal §6" + maxKits + " §ceigene Kits speichern!");
                    return true;
                }
                // Capture inventory (strip forbidden items)
                ItemStack[] rawContents = player.getInventory().getStorageContents();
                ItemStack[] rawArmor    = player.getInventory().getArmorContents();
                ItemStack   rawOffHand  = player.getInventory().getItemInOffHand();

                ItemStack[] contents = new ItemStack[36];
                for (int i = 0; i < rawContents.length && i < 36; i++) {
                    if (rawContents[i] != null && !isForbidden(rawContents[i])) {
                        contents[i] = rawContents[i].clone();
                    }
                }
                ItemStack[] armor = new ItemStack[4];
                for (int i = 0; i < rawArmor.length && i < 4; i++) {
                    if (rawArmor[i] != null && !isForbidden(rawArmor[i])) {
                        armor[i] = rawArmor[i].clone();
                    }
                }
                ItemStack offHand = (rawOffHand != null && rawOffHand.getType() != Material.AIR
                        && !isForbidden(rawOffHand)) ? rawOffHand.clone() : null;

                stats.putCustomKit(name, new CustomKit(name, contents, armor, offHand));
                plugin.getStatsManager().saveStats();
                player.sendMessage(prefix + "§aEigenes Kit §f" + name + " §aerstellt und gespeichert!");
                player.sendMessage(prefix + "§7Verbotene Items wurden entfernt. Wähle es mit §f/kit " + name + " §7aus.");
            }
            case "delete", "löschen" -> {
                if (args.length < 2) {
                    player.sendMessage(prefix + "§cBenutzung: §f/mykit delete <name>");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (!stats.hasCustomKit(name)) {
                    player.sendMessage(prefix + "§cKein eigenes Kit mit dem Namen '§f" + name + "§c' gefunden!");
                    return true;
                }
                stats.removeCustomKit(name);
                // If this kit was selected, fall back to nodebuff
                if (name.equals(stats.getSelectedKit())) {
                    stats.setSelectedKit("nodebuff");
                }
                plugin.getStatsManager().saveStats();
                player.sendMessage(prefix + "§aEigenes Kit §f" + name + " §agelöscht.");
            }
            case "list", "liste" -> {
                var kits = stats.getCustomKits();
                if (kits.isEmpty()) {
                    player.sendMessage(prefix + "§7Du hast noch keine eigenen Kits.");
                    return true;
                }
                player.sendMessage("§6§l━━━ Deine eigenen Kits (" + kits.size() + "/" + maxKitsForRank(stats.getRank()) + ") ━━━");
                for (String kitName : kits.keySet()) {
                    String selected = kitName.equals(stats.getSelectedKit()) ? " §a[ausgewählt]" : "";
                    player.sendMessage("§7- §f" + kitName + selected);
                }
                player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            }
            default -> sendHelp(player, prefix);
        }
        return true;
    }

    private void sendHelp(Player player, String prefix) {
        player.sendMessage("§6§l━━━ Eigene Kits ━━━");
        player.sendMessage("§e/mykit create <name> §7- Aktuelles Inventar als Kit speichern §c(VIP+)");
        player.sendMessage("§e/mykit delete <name> §7- Eigenes Kit löschen");
        player.sendMessage("§e/mykit list §7- Alle eigenen Kits anzeigen");
        player.sendMessage("§7Wähle ein Kit mit §f/kit <name> §7aus.");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━");
    }

    private static boolean hasKitPermission(Rank rank) {
        return rank.isStaff() || rank.ordinal() >= Rank.VIP.ordinal();
    }

    private static int maxKitsForRank(Rank rank) {
        if (rank.isStaff()) return 10;
        return switch (rank) {
            case LEGENDE -> 10;
            case ELITE   -> 5;
            case VIP_PLUS -> 3;
            case VIP     -> 1;
            default      -> 0;
        };
    }

    private static boolean isForbidden(ItemStack item) {
        return item != null && FORBIDDEN.contains(item.getType());
    }

    private static boolean isValidName(String name) {
        return name.length() >= 2 && name.length() <= 20 && name.matches("[a-z0-9_]+");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("create", "delete", "list");
        if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("löschen"))) {
            if (!(sender instanceof Player player)) return List.of();
            var stats = plugin.getStatsManager().getStats(player.getUniqueId());
            if (stats == null) return List.of();
            List<String> names = new ArrayList<>();
            String partial = args[1].toLowerCase();
            for (String n : stats.getCustomKits().keySet()) {
                if (n.startsWith(partial)) names.add(n);
            }
            return names;
        }
        return List.of();
    }
}
