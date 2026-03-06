package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Kit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /adminkit create <name> [displayname] – creates a kit from current inventory (OP)
 * /adminkit delete <name>               – deletes an admin kit (OP)
 * /adminkit list                        – lists all kits and marks admin ones
 * /adminkit reload                      – reloads admin kits from kits.yml
 */
public class AdminKitCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public AdminKitCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (!sender.isOp() && !sender.hasPermission("duell.admin")) {
            sender.sendMessage(prefix + "§cDu hast keine Berechtigung!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, prefix);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create", "erstellen" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(prefix + "§cDieser Befehl ist nur für Spieler!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(prefix + "§cBenutze: /adminkit create <name> [§eAnzeigename§c]");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (name.isEmpty()) {
                    sender.sendMessage(prefix + "§cDer Kit-Name darf nicht leer sein!");
                    return true;
                }
                // Refuse reserved built-in names
                if (plugin.getKitManager().kitExists(name) && !plugin.getKitManager().isAdminKit(name)) {
                    sender.sendMessage(prefix + "§cDas Kit §6" + name + " §cist ein Standard-Kit und kann nicht überschrieben werden!");
                    return true;
                }
                // Build display name from remaining args, or default to capitalized name
                String displayName;
                if (args.length >= 3) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        if (i > 2) sb.append(' ');
                        sb.append(args[i]);
                    }
                    displayName = de.duellplugin.ChatUtils.color(sb.toString());
                } else {
                    displayName = "§f" + (name.length() > 0
                            ? name.substring(0, 1).toUpperCase() + name.substring(1)
                            : name);
                }

                // Snapshot current inventory
                ItemStack[] contents = player.getInventory().getStorageContents().clone();
                ItemStack[] armor    = player.getInventory().getArmorContents().clone();
                ItemStack offhand    = player.getInventory().getItemInOffHand().clone();

                // Pick icon: first non-null content item, else CHEST
                Material icon = Material.CHEST;
                for (ItemStack is : contents) {
                    if (is != null && is.getType() != Material.AIR) { icon = is.getType(); break; }
                }

                Kit kit = new Kit(name, displayName, icon, "§7Admin Kit", armor, contents);
                if (offhand.getType() != Material.AIR) kit.withOffHand(offhand);

                boolean added = plugin.getKitManager().addAdminKit(kit);
                if (!added) {
                    sender.sendMessage(prefix + "§cKit §6" + name + " §ckonnte nicht erstellt werden (Standard-Kit?).");
                } else {
                    sender.sendMessage(prefix + "§aAdmin-Kit §6" + displayName + " §a(§f" + name + "§a) erstellt und gespeichert!");
                    sender.sendMessage(prefix + "§7Inventar-Inhalt, Rüstung und Offhand wurden gespeichert.");
                }
            }

            case "delete", "loeschen" -> {
                if (args.length < 2) {
                    sender.sendMessage(prefix + "§cBenutze: /adminkit delete <name>");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (!plugin.getKitManager().isAdminKit(name)) {
                    sender.sendMessage(prefix + "§cAdmin-Kit §6" + name + " §cnicht gefunden (Standard-Kits können nicht gelöscht werden).");
                    return true;
                }
                plugin.getKitManager().removeAdminKit(name);
                sender.sendMessage(prefix + "§aAdmin-Kit §6" + name + " §agelöscht!");
            }

            case "list", "liste" -> {
                sender.sendMessage("§6§l━━━ Kit-Liste ━━━");
                for (Kit k : plugin.getKitManager().getAllKits()) {
                    String tag = plugin.getKitManager().isAdminKit(k.getName()) ? " §d[Admin]" : " §7[Standard]";
                    sender.sendMessage("§e" + k.getName() + tag + " §7– " + k.getDisplayName());
                }
                sender.sendMessage("§6§l━━━━━━━━━━━━━━━");
            }

            case "reload" -> {
                plugin.getKitManager().loadAdminKits();
                sender.sendMessage(prefix + "§aAdmin-Kits aus kits.yml neu geladen!");
            }

            default -> sendHelp(sender, prefix);
        }
        return true;
    }

    private void sendHelp(CommandSender sender, String prefix) {
        sender.sendMessage("§6§l━━━ AdminKit-Hilfe ━━━");
        sender.sendMessage("§e/adminkit create <name> [Anzeigename] §7- Kit aus Inventar erstellen");
        sender.sendMessage("§e/adminkit delete <name> §7- Admin-Kit löschen");
        sender.sendMessage("§e/adminkit list §7- Alle Kits anzeigen");
        sender.sendMessage("§e/adminkit reload §7- Kits aus Datei neu laden");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("duell.admin")) return List.of();
        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("create", "delete", "list", "reload"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("loeschen"))) {
            List<String> adminKits = new ArrayList<>();
            for (Kit k : plugin.getKitManager().getAllKits()) {
                if (plugin.getKitManager().isAdminKit(k.getName())) adminKits.add(k.getName());
            }
            return filterStartsWith(adminKits, args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("erstellen"))) {
            return List.of("<name>");
        }
        return List.of();
    }

    private List<String> filterStartsWith(List<String> options, String input) {
        List<String> result = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase().startsWith(input.toLowerCase())) result.add(o);
        }
        return result;
    }
}
