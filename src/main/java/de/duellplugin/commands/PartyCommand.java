package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Party;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PartyCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    public PartyCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }

        String prefix = plugin.getPrefix();

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create", "erstellen" -> plugin.getPartyManager().createParty(player);
            case "invite", "einladen" -> {
                if (args.length < 2) { player.sendMessage(prefix + "§cBenutzung: §f/party invite <spieler>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { player.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                plugin.getPartyManager().invitePlayer(player, target);
            }
            case "accept", "annehmen" -> plugin.getPartyManager().acceptInvite(player);
            case "decline", "ablehnen" -> plugin.getPartyManager().declineInvite(player);
            case "leave", "verlassen" -> plugin.getPartyManager().leaveParty(player);
            case "kick" -> {
                if (args.length < 2) { player.sendMessage(prefix + "§cBenutzung: §f/party kick <spieler>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { player.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                plugin.getPartyManager().kickMember(player, target);
            }
            case "disband", "auflösen" -> plugin.getPartyManager().disbandParty(player);
            case "list", "liste" -> {
                var party = plugin.getPartyManager().getParty(player.getUniqueId());
                if (party == null) { player.sendMessage(prefix + "§cDu bist in keiner Party!"); return true; }
                player.sendMessage("§6§l━━━ Party (" + party.size() + "/" + Party.MAX_SIZE + ") ━━━");
                for (var uid : party.getMembers()) {
                    Player m = Bukkit.getPlayer(uid);
                    String name = m != null ? m.getName() : uid.toString().substring(0, 8);
                    boolean isLeader = uid.equals(party.getLeader());
                    player.sendMessage("§7- §e" + name + (isLeader ? " §6[Leader]" : ""));
                }
                player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━");
            }
            case "duel" -> {
                if (args.length < 2) { player.sendMessage(prefix + "§cBenutzung: §f/party duel <leader der gegnerischen Party>"); return true; }
                Player targetLeader = Bukkit.getPlayer(args[1]);
                if (targetLeader == null) { player.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                if (!plugin.getPartyManager().isLeader(player.getUniqueId())) {
                    player.sendMessage(prefix + "§cNur der Party-Leader kann ein Duell starten!");
                    return true;
                }
                String kitName = plugin.getStatsManager()
                        .getOrCreateStats(player.getUniqueId(), player.getName()).getSelectedKit();
                plugin.getPartyManager().startPartyDuel(player, targetLeader, kitName);
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l━━━ Party-Befehle ━━━");
        player.sendMessage("§e/party create §7- Party erstellen");
        player.sendMessage("§e/party invite <spieler> §7- Einladen");
        player.sendMessage("§e/party accept §7- Einladung annehmen");
        player.sendMessage("§e/party decline §7- Einladung ablehnen");
        player.sendMessage("§e/party leave §7- Party verlassen");
        player.sendMessage("§e/party kick <spieler> §7- Rauswerfen");
        player.sendMessage("§e/party disband §7- Party auflösen");
        player.sendMessage("§e/party list §7- Mitglieder anzeigen");
        player.sendMessage("§e/party duel <leader> §7- Party-Duell starten");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "invite", "accept", "decline", "leave", "kick", "disband", "list", "duel");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick")
                || args[0].equalsIgnoreCase("duel"))) {
            List<String> names = new ArrayList<>();
            String partial = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }
}
