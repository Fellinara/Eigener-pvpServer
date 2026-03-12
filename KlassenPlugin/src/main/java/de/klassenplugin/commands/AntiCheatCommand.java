package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.AntiCheatManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AntiCheatCommand implements TabExecutor {

    private static final List<String> CHECKS = Arrays.asList("xray", "speed", "fly", "reach", "killaura");
    private static final List<String> SUBCOMMANDS = Arrays.asList("status", "enable", "disable", "check", "violations", "reset");

    private final KlassenPlugin plugin;
    private final AntiCheatManager manager;

    public AntiCheatCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getAntiCheatManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.anticheat.admin")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /anticheat <status|enable|disable|check|violations|reset>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status" -> handleStatus(sender);
            case "enable" -> handleEnable(sender);
            case "disable" -> handleDisable(sender);
            case "check" -> handleCheck(sender, args);
            case "violations" -> handleViolations(sender, args);
            case "reset" -> handleReset(sender, args);
            default -> sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cUnbekannter Unterbefehl. Benutze: /anticheat <status|enable|disable|check|violations|reset>"));
        }
        return true;
    }

    private void handleStatus(CommandSender sender) {
        boolean enabled = manager.isEnabled();
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bAntiCheat&8] &eStatus: " + (enabled ? "&aAktiviert" : "&cDeaktiviert")));
        for (String check : CHECKS) {
            boolean checkEnabled = manager.isCheckEnabled(check);
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "  &7" + check + ": " + (checkEnabled ? "&aAktiviert" : "&cDeaktiviert")));
        }
    }

    private void handleEnable(CommandSender sender) {
        plugin.getConfig().set("anticheat.enabled", true);
        plugin.saveConfig();
        sender.sendMessage(KlassenPlugin.colorizeComponent("&a[AntiCheat] &eAnti-Cheat wurde &aaktiviert&e!"));
    }

    private void handleDisable(CommandSender sender) {
        plugin.getConfig().set("anticheat.enabled", false);
        plugin.saveConfig();
        sender.sendMessage(KlassenPlugin.colorizeComponent("&c[AntiCheat] &eAnti-Cheat wurde &cdeaktiviert&e!"));
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cBenutzung: /anticheat check <xray|speed|fly|reach|killaura> <enable|disable>"));
            return;
        }
        String check = args[1].toLowerCase();
        if (!CHECKS.contains(check)) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cUnbekannter Check. Verfügbar: xray, speed, fly, reach, killaura"));
            return;
        }
        String action = args[2].toLowerCase();
        if (!action.equals("enable") && !action.equals("disable")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /anticheat check <check> <enable|disable>"));
            return;
        }
        boolean value = action.equals("enable");
        plugin.getConfig().set("anticheat." + check + ".enabled", value);
        plugin.saveConfig();
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bAntiCheat&8] &eCheck &b" + check + " &ewurde " + (value ? "&aaktiviert" : "&cdeaktiviert") + "&e!"));
    }

    private void handleViolations(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /anticheat violations <Spieler>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    plugin.getMessage("player-not-found").replace("{player}", args[1])));
            return;
        }
        int count = manager.getViolations(target.getUniqueId());
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bAntiCheat&8] &e" + target.getName() + " &ehat &c" + count + " &eVerstöße."));
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /anticheat reset <Spieler>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    plugin.getMessage("player-not-found").replace("{player}", args[1])));
            return;
        }
        manager.resetViolations(target.getUniqueId());
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bAntiCheat&8] &eVerstöße von &b" + target.getName() + " &ewurden zurückgesetzt!"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.anticheat.admin")) return List.of();

        if (args.length == 1) {
            return filterStart(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("check")) return filterStart(CHECKS, args[1]);
            if (sub.equals("violations") || sub.equals("reset")) return onlinePlayerNames(args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("check")) {
            return filterStart(Arrays.asList("enable", "disable"), args[2]);
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
