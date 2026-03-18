package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.AllianceManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class AllianceCommand implements TabExecutor {
    private static final List<String> SUBS = Arrays.asList("invite","accept","deny","info","leave");
    private final KlassenPlugin plugin;
    public AllianceCommand(KlassenPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.ally")) { sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission"))); return true; }
        if (!(sender instanceof Player p)) { sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only"))); return true; }
        if (args.length == 0) { handleInfo(p); return true; }
        AllianceManager am = plugin.getAllianceManager();
        switch (args[0].toLowerCase()) {
            case "invite" -> handleInvite(p, am, args);
            case "accept" -> handleAccept(p, am);
            case "deny" -> handleDeny(p, am);
            case "info" -> handleInfo(p);
            case "leave" -> handleLeave(p, am);
            default -> p.sendMessage(KlassenPlugin.colorizeComponent("&c/ally <invite|accept|deny|info|leave>"));
        }
        return true;
    }

    private void handleInvite(Player p, AllianceManager am, String[] args) {
        if (args.length < 2) { p.sendMessage(KlassenPlugin.colorizeComponent("&c/ally invite <Spieler>")); return; }
        if (am.hasAlliance(p.getUniqueId())) {
            String name = Bukkit.getOfflinePlayer(am.getAlly(p.getUniqueId())).getName();
            p.sendMessage(KlassenPlugin.colorizeComponent("&cDu hast bereits ein Bündnis mit &e" + name + "&c! (/ally leave)")); return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { p.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-not-found").replace("{player}", args[1]))); return; }
        if (target.equals(p)) { p.sendMessage(KlassenPlugin.colorizeComponent("&cSelbst-Einladung!")); return; }
        if (am.hasAlliance(target.getUniqueId())) { p.sendMessage(KlassenPlugin.colorizeComponent("&e" + target.getName() + " &chat bereits ein Bündnis!")); return; }
        am.sendInvite(p.getUniqueId(), target.getUniqueId());
        p.sendMessage(KlassenPlugin.colorizeComponent("&aBündnisanfrage an &e" + target.getName() + " &ageschickt!"));
        target.sendMessage(KlassenPlugin.colorizeComponent("&e" + p.getName() + " &amöchte ein Bündnis! &7(/ally accept oder /ally deny)"));
    }

    private void handleAccept(Player p, AllianceManager am) {
        UUID inv = am.getInviter(p.getUniqueId());
        if (inv == null) { p.sendMessage(KlassenPlugin.colorizeComponent("&cKeine Anfrage!")); return; }
        String name = Bukkit.getOfflinePlayer(inv).getName();
        am.clearInvite(p.getUniqueId()); am.formAlliance(inv, p.getUniqueId());
        p.sendMessage(KlassenPlugin.colorizeComponent("&aBündnis mit &e" + name + " &ageschlossen!"));
        Player invP = Bukkit.getPlayer(inv);
        if (invP != null) invP.sendMessage(KlassenPlugin.colorizeComponent("&e" + p.getName() + " &ahat dein Bündnis angenommen!"));
    }

    private void handleDeny(Player p, AllianceManager am) {
        UUID inv = am.getInviter(p.getUniqueId());
        if (inv == null) { p.sendMessage(KlassenPlugin.colorizeComponent("&cKeine Anfrage!")); return; }
        String name = Bukkit.getOfflinePlayer(inv).getName();
        am.clearInvite(p.getUniqueId());
        p.sendMessage(KlassenPlugin.colorizeComponent("&cAnfrage von &e" + name + " &cabgelehnt."));
        Player invP = Bukkit.getPlayer(inv);
        if (invP != null) invP.sendMessage(KlassenPlugin.colorizeComponent("&e" + p.getName() + " &chat abgelehnt."));
    }

    private void handleInfo(Player p) {
        UUID ally = plugin.getAllianceManager().getAlly(p.getUniqueId());
        if (ally == null) { p.sendMessage(KlassenPlugin.colorizeComponent("&7Kein Bündnis. &e/ally invite <Spieler>")); return; }
        String name = Bukkit.getOfflinePlayer(ally).getName();
        boolean online = Bukkit.getPlayer(ally) != null;
        p.sendMessage(KlassenPlugin.colorizeComponent("&8[&5Bündnis&8] &eVerbündeter: &b" + name + (online ? " &a(Online)" : " &7(Offline)")));
    }

    private void handleLeave(Player p, AllianceManager am) {
        if (!am.hasAlliance(p.getUniqueId())) { p.sendMessage(KlassenPlugin.colorizeComponent("&cKein Bündnis!")); return; }
        UUID ally = am.getAlly(p.getUniqueId());
        String name = Bukkit.getOfflinePlayer(ally).getName();
        am.breakAlliance(p.getUniqueId());
        p.sendMessage(KlassenPlugin.colorizeComponent("&cBündnis mit &e" + name + " &caufgelöst."));
        Player ap = Bukkit.getPlayer(ally);
        if (ap != null) ap.sendMessage(KlassenPlugin.colorizeComponent("&e" + p.getName() + " &chat das Bündnis aufgelöst."));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.ally")) return List.of();
        if (args.length == 1) return filter(SUBS, args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            List<String> names = new ArrayList<>();
            for (Player op : Bukkit.getOnlinePlayers()) if (op.getName().toLowerCase().startsWith(args[1].toLowerCase())) names.add(op.getName());
            return names;
        }
        return List.of();
    }

    private List<String> filter(List<String> l, String p) { List<String> r = new ArrayList<>(); for (String s : l) if (s.toLowerCase().startsWith(p.toLowerCase())) r.add(s); return r; }
}
