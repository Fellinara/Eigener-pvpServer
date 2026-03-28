package de.klassenplugin.commands;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

/**
 * /scoreboard – lets admins configure the sidebar scoreboard at runtime.
 *
 * <p>Subcommands:
 * <ul>
 *   <li>{@code toggle} – enable / disable the scoreboard</li>
 *   <li>{@code title <text...>} – set the title (&-colour codes supported)</li>
 *   <li>{@code setline <num> <text...>} – set/add a line (1-based index)</li>
 *   <li>{@code clearline <num>} – remove a specific line</li>
 *   <li>{@code addline <text...>} – append a line at the bottom</li>
 *   <li>{@code clearlines} – remove all lines</li>
 *   <li>{@code info} – show current title and lines</li>
 * </ul>
 *
 * <p>Requires {@code klassenplugin.admin} permission.
 */
public class ScoreboardCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "toggle", "title", "setline", "clearline", "addline", "clearlines", "info");

    private final KlassenPlugin plugin;

    public ScoreboardCommand(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.admin")) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(plugin.getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "toggle"     -> handleToggle(sender);
            case "title"      -> handleTitle(sender, args);
            case "setline"    -> handleSetLine(sender, args);
            case "clearline"  -> handleClearLine(sender, args);
            case "addline"    -> handleAddLine(sender, args);
            case "clearlines" -> handleClearLines(sender);
            case "info"       -> handleInfo(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleToggle(CommandSender sender) {
        boolean current = plugin.getConfig().getBoolean("scoreboard.enabled", true);
        plugin.getConfig().set("scoreboard.enabled", !current);
        plugin.saveConfig();
        plugin.getScoreboardManager().updateAll();
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bScoreboard&8] Scoreboard " + (!current ? "&aaktiviert" : "&cdeaktiviert") + "&7."));
    }

    private void handleTitle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /scoreboard title <Titel...>"));
            return;
        }
        String title = joinArgs(args, 1);
        plugin.getConfig().set("scoreboard.title", title);
        plugin.saveConfig();
        plugin.getScoreboardManager().updateAll();
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bScoreboard&8] &7Titel auf &r" + title + " &7gesetzt."));
    }

    private void handleSetLine(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cBenutzung: /scoreboard setline <Nummer> <Text...>"));
            return;
        }
        int lineNum;
        try {
            lineNum = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige Zeilennummer: " + args[1]));
            return;
        }
        if (lineNum < 1) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cZeilennummer muss ≥ 1 sein."));
            return;
        }

        List<String> lines = new ArrayList<>(plugin.getConfig().getStringList("scoreboard.lines"));
        String text = joinArgs(args, 2);
        int idx = lineNum - 1;
        while (lines.size() < idx) lines.add(" ");
        if (idx < lines.size()) {
            lines.set(idx, text);
        } else {
            lines.add(text);
        }
        plugin.getConfig().set("scoreboard.lines", lines);
        plugin.saveConfig();
        plugin.getScoreboardManager().updateAll();
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bScoreboard&8] &7Zeile " + lineNum + " gesetzt: &r" + text));
    }

    private void handleClearLine(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cBenutzung: /scoreboard clearline <Nummer>"));
            return;
        }
        int lineNum;
        try {
            lineNum = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cUngültige Zeilennummer: " + args[1]));
            return;
        }
        List<String> lines = new ArrayList<>(plugin.getConfig().getStringList("scoreboard.lines"));
        int idx = lineNum - 1;
        if (idx < 0 || idx >= lines.size()) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cZeile " + lineNum + " existiert nicht (max " + lines.size() + ")."));
            return;
        }
        lines.remove(idx);
        plugin.getConfig().set("scoreboard.lines", lines);
        plugin.saveConfig();
        plugin.getScoreboardManager().updateAll();
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bScoreboard&8] &7Zeile " + lineNum + " gelöscht."));
    }

    private void handleAddLine(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(KlassenPlugin.colorizeComponent("&cBenutzung: /scoreboard addline <Text...>"));
            return;
        }
        String text = joinArgs(args, 1);
        List<String> lines = new ArrayList<>(plugin.getConfig().getStringList("scoreboard.lines"));
        lines.add(text);
        plugin.getConfig().set("scoreboard.lines", lines);
        plugin.saveConfig();
        plugin.getScoreboardManager().updateAll();
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bScoreboard&8] &7Zeile " + lines.size() + " hinzugefügt: &r" + text));
    }

    private void handleClearLines(CommandSender sender) {
        plugin.getConfig().set("scoreboard.lines", new ArrayList<String>());
        plugin.saveConfig();
        plugin.getScoreboardManager().updateAll();
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bScoreboard&8] &7Alle Zeilen gelöscht."));
    }

    private void handleInfo(CommandSender sender) {
        boolean enabled = plugin.getConfig().getBoolean("scoreboard.enabled", true);
        String title = plugin.getConfig().getString("scoreboard.title", "&6&lKlassenPlugin");
        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");

        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bScoreboard&8] &eAktuelle Konfiguration:"));
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "  &7Status: " + (enabled ? "&aAktiviert" : "&cDeaktiviert")));
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "  &7Titel: &r" + title));
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "  &7Zeilen (" + lines.size() + "):"));
        for (int i = 0; i < lines.size(); i++) {
            sender.sendMessage(KlassenPlugin.colorizeComponent(
                    "    &8" + (i + 1) + ". &r" + lines.get(i)));
        }
        sender.sendMessage(KlassenPlugin.colorizeComponent(
                "  &7Platzhalter: &e{player} {rank} {balance} {online} {server} {combat}"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(KlassenPlugin.colorizeComponent("&8[&bScoreboard&8] &eBefehle:"));
        sender.sendMessage(KlassenPlugin.colorizeComponent("  &e/scoreboard toggle &7– an/aus"));
        sender.sendMessage(KlassenPlugin.colorizeComponent("  &e/scoreboard title <Text> &7– Titel setzen"));
        sender.sendMessage(KlassenPlugin.colorizeComponent("  &e/scoreboard setline <Nr> <Text> &7– Zeile setzen"));
        sender.sendMessage(KlassenPlugin.colorizeComponent("  &e/scoreboard clearline <Nr> &7– Zeile löschen"));
        sender.sendMessage(KlassenPlugin.colorizeComponent("  &e/scoreboard addline <Text> &7– Zeile anhängen"));
        sender.sendMessage(KlassenPlugin.colorizeComponent("  &e/scoreboard clearlines &7– alle Zeilen löschen"));
        sender.sendMessage(KlassenPlugin.colorizeComponent("  &e/scoreboard info &7– aktuelle Konfiguration"));
    }

    // ── Tab completion ─────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("klassenplugin.admin")) return List.of();
        if (args.length == 1) return filterStart(SUBCOMMANDS, args[0]);
        return List.of();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String joinArgs(String[] args, int from) {
        StringJoiner sj = new StringJoiner(" ");
        for (int i = from; i < args.length; i++) sj.add(args[i]);
        return sj.toString();
    }

    private static List<String> filterStart(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) result.add(s);
        }
        return result;
    }
}
