package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class CreativeZoneCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public CreativeZoneCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl nutzen!");
            return true;
        }

        if (!player.hasPermission("duell.admin")) {
            player.sendMessage(plugin.getConfig().getString("messages.no-permission", "§cDu hast keine Berechtigung!"));
            return true;
        }

        String prefix = plugin.getPrefix();

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setpos1", "pos1" -> {
                plugin.getCreativeZoneManager().setPos1(player.getLocation());
                player.sendMessage(prefix + "§aPosition 1 der Kit-Bauzone gesetzt: §e"
                        + player.getLocation().getBlockX() + ", "
                        + player.getLocation().getBlockY() + ", "
                        + player.getLocation().getBlockZ());
            }
            case "setpos2", "pos2" -> {
                plugin.getCreativeZoneManager().setPos2(player.getLocation());
                player.sendMessage(prefix + "§aPosition 2 der Kit-Bauzone gesetzt: §e"
                        + player.getLocation().getBlockX() + ", "
                        + player.getLocation().getBlockY() + ", "
                        + player.getLocation().getBlockZ());
            }
            case "info" -> {
                var mgr = plugin.getCreativeZoneManager();
                if (!mgr.isDefined()) {
                    player.sendMessage(prefix + "§cKeine Kit-Bauzone konfiguriert. Nutze §e/kitzone setpos1 §cund §e/kitzone setpos2§c.");
                } else {
                    var p1 = mgr.getPos1();
                    var p2 = mgr.getPos2();
                    player.sendMessage(prefix + "§aKit-Bauzone ist definiert:");
                    player.sendMessage("§7  Pos1: §e" + p1.getBlockX() + ", " + p1.getBlockY() + ", " + p1.getBlockZ()
                            + " §7(Welt: §e" + p1.getWorld().getName() + "§7)");
                    player.sendMessage("§7  Pos2: §e" + p2.getBlockX() + ", " + p2.getBlockY() + ", " + p2.getBlockZ());
                }
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l━━━ Kit-Bauzone ━━━");
        player.sendMessage("§e/kitzone setpos1 §7- Ecke 1 der Bauzone setzen");
        player.sendMessage("§e/kitzone setpos2 §7- Ecke 2 der Bauzone setzen");
        player.sendMessage("§e/kitzone info §7- Zone anzeigen");
        player.sendMessage("§7Spieler in der Zone erhalten §6Creative-Modus§7 und können §e/mykit create <Name> §7nutzen.");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("setpos1", "setpos2", "info"), args[0]);
        }
        return List.of();
    }

    private List<String> filterStartsWith(List<String> list, String prefix) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase())).toList();
    }
}
