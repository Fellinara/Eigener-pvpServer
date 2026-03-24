package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

public class RankCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "delete", "setprefix", "addperm", "removeperm",
            "assign", "remove", "list", "info", "player", "permissions", "gui");

    private final KlassenPlugin plugin;
    private final RankManager manager;

    public RankCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getRankManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.rank.admin")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cBenutzung: /rank <create|delete|setprefix|addperm|removeperm|assign|remove|list|info|player|permissions|gui>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setprefix" -> handleSetPrefix(sender, args);
            case "addperm" -> handleAddPerm(sender, args);
            case "removeperm" -> handleRemovePerm(sender, args);
            case "assign" -> handleAssign(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "player" -> handlePlayer(sender, args);
            case "permissions" -> handlePermissionsGui(sender, args);
            case "gui" -> handleListGui(sender);
            default -> sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cUnbekannter Unterbefehl. Benutze /rank für Hilfe."));
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /rank create <Name>"));
            return;
        }
        String name = args[1].toLowerCase();
        if (!manager.createRank(name)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cRang &e" + name + " &cexistiert bereits!"));
            return;
        }
        sender.sendMessage(KlassenPlugin.colorizeComponent("&aRang &e" + name + " &awurde erstellt!"));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /rank delete <Name>"));
            return;
        }
        String name = args[1].toLowerCase();
        if (!manager.deleteRank(name)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cRang &e" + name + " &cwurde nicht gefunden!"));
            return;
        }
        sender.sendMessage(KlassenPlugin.colorizeComponent("&aRang &e" + name + " &awurde gelöscht!"));
    }

    private void handleSetPrefix(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /rank setprefix <Rang> <Prefix...>"));
            return;
        }
        String name = args[1].toLowerCase();
        if (!manager.rankExists(name)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cRang &e" + name + " &cwurde nicht gefunden!"));
            return;
        }
        StringJoiner sj = new StringJoiner(" ");
        for (int i = 2; i < args.length; i++) sj.add(args[i]);
        String prefix = sj.toString();
        manager.setPrefix(name, prefix);
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&aPrefix von Rang &e" + name + " &awurde auf &r" + prefix + " &agesetzt!"));
    }

    private void handleAddPerm(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /rank addperm <Rang> <Berechtigung>"));
            return;
        }
        String name = args[1].toLowerCase();
        if (!manager.rankExists(name)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cRang &e" + name + " &cwurde nicht gefunden!"));
            return;
        }
        manager.addPermission(name, args[2]);
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&aBerechtigung &e" + args[2] + " &awurde zu Rang &e" + name + " &ahinzugefügt!"));
    }

    private void handleRemovePerm(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /rank removeperm <Rang> <Berechtigung>"));
            return;
        }
        String name = args[1].toLowerCase();
        if (!manager.rankExists(name)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cRang &e" + name + " &cwurde nicht gefunden!"));
            return;
        }
        manager.removePermission(name, args[2]);
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&aBerechtigung &e" + args[2] + " &awurde von Rang &e" + name + " &aentfernt!"));
    }

    private void handleAssign(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /rank assign <Spieler> <Rang>"));
            return;
        }
        String rankName = args[2].toLowerCase();
        if (!manager.rankExists(rankName)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cRang &e" + rankName + " &cwurde nicht gefunden!"));
            return;
        }
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        manager.assignRank(target.getUniqueId(), rankName);
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&aSpieler &e" + args[1] + " &awurde der Rang &e" + rankName + " &azugewiesen!"));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /rank remove <Spieler>"));
            return;
        }
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        manager.removePlayerRank(target.getUniqueId());
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&aRang von Spieler &e" + args[1] + " &awurde entfernt!"));
    }

    private void handleList(CommandSender sender) {
        if (manager.getRankNames().isEmpty()) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cEs gibt noch keine Ränge!"));
            return;
        }
        sender.sendMessage(KlassenPlugin.colorizeComponent("&8[&bRänge&8] &eAlle Ränge:"));
        for (String rankName : manager.getRankNames()) {
            String prefix = manager.getPrefix(rankName);
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "  &7- &e" + rankName + " &7| Prefix: &r" + (prefix.isEmpty() ? "&7(leer)" : prefix)));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /rank info <Rang>"));
            return;
        }
        String name = args[1].toLowerCase();
        if (!manager.rankExists(name)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cRang &e" + name + " &cwurde nicht gefunden!"));
            return;
        }
        sender.sendMessage(KlassenPlugin.colorizeComponent("&8[&bRang&8] &eInfo für &b" + name + "&e:"));
        sender.sendMessage(KlassenPlugin.colorizeComponent("  &7Prefix: &r" + manager.getPrefix(name)));
        List<String> perms = manager.getPermissions(name);
        if (perms.isEmpty()) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("  &7Berechtigungen: &cnone"));
        } else {
            sender.sendMessage(KlassenPlugin.colorizeComponent("  &7Berechtigungen:"));
            for (String perm : perms) {
                sender.sendMessage(KlassenPlugin.colorizeComponent("    &a- " + perm));
            }
        }
    }

    private void handlePlayer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /rank player <Spieler>"));
            return;
        }
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String rank = manager.getPlayerRank(target.getUniqueId());
        if (rank == null) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&eSpieler &b" + args[1] + " &ehat keinen Rang zugewiesen."));
        } else {
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&eSpieler &b" + args[1] + " &ehat den Rang &b" + rank + "&e."));
        }
    }

    private void handlePermissionsGui(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /rank permissions <Rang>"));
            return;
        }
        String name = args[1].toLowerCase();
        if (!manager.rankExists(name)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cRang &e" + name + " &cwurde nicht gefunden!"));
            return;
        }
        plugin.getRankPermissionsGui().open(p, name);
    }

    private void handleListGui(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("player-only")));
            return;
        }
        plugin.getRankListGui().open(p);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.rank.admin")) return List.of();

        if (args.length == 1) {
            return filterStart(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "delete", "setprefix", "addperm", "removeperm", "info", "permissions" ->
                        { return filterStart(new ArrayList<>(manager.getRankNames()), args[1]); }
                case "assign", "remove", "player" -> { return onlinePlayerNames(args[1]); }
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("assign")) {
            return filterStart(new ArrayList<>(manager.getRankNames()), args[2]);
        }
        return List.of();
    }

    private List<String> filterStart(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) result.add(s);
        }
        return result;
    }

    private List<String> onlinePlayerNames(String prefix) {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(prefix.toLowerCase())) names.add(p.getName());
        }
        return names;
    }
}
