package de.fellinara.risiko.commands;

import de.fellinara.risiko.RisikoPlugin;
import de.fellinara.risiko.managers.GameManager;
import de.fellinara.risiko.models.Kingdom;
import de.fellinara.risiko.models.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Verwaltet alle /risiko-Befehle.
 *
 * Unterbefehle:
 *   /risiko team <spieler> <fichten|dschungel>   - Spieler einem Königreich zuweisen
 *   /risiko king <spieler>                        - Spieler zum König seines Königreichs machen
 *   /risiko addherz <spieler> [anzahl]            - Herzen hinzufügen
 *   /risiko removeherz <spieler> [anzahl]         - Herzen entfernen
 *   /risiko unban <spieler>                        - Spieler entbannen
 *   /risiko start                                  - Finale starten
 *   /risiko stop                                   - Finale stoppen
 *   /risiko setspawn <fichten|dschungel>           - Spawn-Punkt setzen
 *   /risiko info [spieler]                         - Informationen anzeigen
 *   /risiko list                                   - Alle Spieler und ihre Teams anzeigen
 *   /risiko reload                                 - Konfiguration neu laden
 */
public class RisikoCommand implements CommandExecutor, TabCompleter {

    private final RisikoPlugin plugin;

    private static final TextColor HEADER_COLOR = TextColor.color(0xFF6600);
    private static final TextColor SUCCESS_COLOR = NamedTextColor.GREEN;
    private static final TextColor ERROR_COLOR = NamedTextColor.RED;
    private static final TextColor INFO_COLOR = NamedTextColor.YELLOW;
    private static final TextColor HEART_COLOR = TextColor.color(0xFF6600);

    public RisikoCommand(RisikoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("risiko.admin")) {
            sender.sendMessage(Component.text("Du hast keine Berechtigung für diesen Befehl!").color(ERROR_COLOR));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "team" -> handleTeam(sender, args);
            case "king" -> handleKing(sender, args);
            case "addherz", "herzgeben", "givehearts" -> handleAddHerz(sender, args);
            case "removeherz", "herzentfernen", "takehearts" -> handleRemoveHerz(sender, args);
            case "unban", "entbannen" -> handleUnban(sender, args);
            case "start" -> handleStart(sender);
            case "stop" -> handleStop(sender);
            case "setspawn" -> handleSetSpawn(sender, args);
            case "info" -> handleInfo(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    // ===== /risiko team <spieler> <fichten|dschungel> =====
    private void handleTeam(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Verwendung: /risiko team <spieler> <fichten|dschungel>").color(ERROR_COLOR));
            return;
        }

        String playerName = args[1];
        Kingdom kingdom = Kingdom.fromString(args[2]);

        if (kingdom == null) {
            sender.sendMessage(Component.text("Ungültiges Königreich! Verwende: fichten oder dschungel").color(ERROR_COLOR));
            return;
        }

        // Spieler suchen (online oder offline)
        OfflinePlayer target = findPlayer(playerName);
        if (target == null || target.getName() == null) {
            sender.sendMessage(Component.text("Spieler '" + playerName + "' wurde nicht gefunden!").color(ERROR_COLOR));
            return;
        }

        plugin.getTeamManager().assignKingdom(target.getUniqueId(), target.getName(), kingdom);

        TextColor kingdomColor = kingdom == Kingdom.FICHTEN
                ? TextColor.color(0xFFAA00)
                : TextColor.color(0x00AA00);

        sender.sendMessage(Component.text("✔ " + target.getName() + " wurde dem ")
                .color(SUCCESS_COLOR)
                .append(Component.text(kingdom.getDisplayName()).color(kingdomColor))
                .append(Component.text(" zugewiesen!").color(SUCCESS_COLOR))
        );

        // Online-Spieler benachrichtigen
        Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
        if (onlineTarget != null) {
            onlineTarget.sendMessage(Component.text("Du wurdest dem ")
                    .color(INFO_COLOR)
                    .append(Component.text(kingdom.getDisplayName()).color(kingdomColor))
                    .append(Component.text(" zugewiesen!").color(INFO_COLOR))
            );
            plugin.getHeartManager().updateActionBar(onlineTarget);
        }
    }

    // ===== /risiko king <spieler> =====
    private void handleKing(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Verwendung: /risiko king <spieler>").color(ERROR_COLOR));
            return;
        }

        OfflinePlayer target = findPlayer(args[1]);
        if (target == null || target.getName() == null) {
            sender.sendMessage(Component.text("Spieler '" + args[1] + "' wurde nicht gefunden!").color(ERROR_COLOR));
            return;
        }

        boolean success = plugin.getTeamManager().assignKing(target.getUniqueId(), target.getName());
        if (!success) {
            sender.sendMessage(Component.text("'" + target.getName() + "' ist keinem Königreich zugewiesen! Verwende zuerst /risiko team.").color(ERROR_COLOR));
            return;
        }

        PlayerData data = plugin.getDataManager().get(target.getUniqueId());
        String kingdomName = data != null && data.getKingdom() != null ? data.getKingdom().getDisplayName() : "Unbekannt";

        sender.sendMessage(Component.text("♔ " + target.getName() + " ist jetzt König des " + kingdomName + "!").color(SUCCESS_COLOR).decoration(TextDecoration.BOLD, true));

        // Online-Spieler benachrichtigen
        Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
        if (onlineTarget != null) {
            onlineTarget.sendMessage(Component.text("♔ Du bist jetzt König des " + kingdomName + "!")
                    .color(TextColor.color(0xFFD700))
                    .decoration(TextDecoration.BOLD, true)
            );
            plugin.getHeartManager().updateActionBar(onlineTarget);
        }

        // Ankündigung
        Bukkit.broadcast(Component.text("♔ " + target.getName() + " ist jetzt König des " + kingdomName + "!")
                .color(TextColor.color(0xFFD700))
                .decoration(TextDecoration.BOLD, true)
        );
    }

    // ===== /risiko addherz <spieler> [anzahl] =====
    private void handleAddHerz(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Verwendung: /risiko addherz <spieler> [anzahl]").color(ERROR_COLOR));
            return;
        }

        OfflinePlayer target = findPlayer(args[1]);
        if (target == null || target.getName() == null) {
            sender.sendMessage(Component.text("Spieler '" + args[1] + "' wurde nicht gefunden!").color(ERROR_COLOR));
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Ungültige Anzahl! Muss eine positive Zahl sein.").color(ERROR_COLOR));
                return;
            }
        }

        PlayerData data = plugin.getDataManager().getOrCreate(target.getUniqueId(), target.getName());
        plugin.getHeartManager().addHearts(data, amount);
        plugin.getDataManager().save(target.getUniqueId());

        sender.sendMessage(Component.text("❤ " + amount + " Herz(en) zu " + target.getName() + " hinzugefügt! (Jetzt: " + data.getHearts() + " ❤)").color(SUCCESS_COLOR));

        Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
        if (onlineTarget != null) {
            onlineTarget.sendMessage(Component.text("❤ Du hast " + amount + " Herz(en) erhalten! (Jetzt: " + data.getHearts() + " ❤)").color(HEART_COLOR));
            plugin.getHeartManager().updateActionBar(onlineTarget);
        }
    }

    // ===== /risiko removeherz <spieler> [anzahl] =====
    private void handleRemoveHerz(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Verwendung: /risiko removeherz <spieler> [anzahl]").color(ERROR_COLOR));
            return;
        }

        OfflinePlayer target = findPlayer(args[1]);
        if (target == null || target.getName() == null) {
            sender.sendMessage(Component.text("Spieler '" + args[1] + "' wurde nicht gefunden!").color(ERROR_COLOR));
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Ungültige Anzahl! Muss eine positive Zahl sein.").color(ERROR_COLOR));
                return;
            }
        }

        PlayerData data = plugin.getDataManager().getOrCreate(target.getUniqueId(), target.getName());
        boolean outOfHearts = plugin.getHeartManager().removeHearts(data, amount);
        plugin.getDataManager().save(target.getUniqueId());

        sender.sendMessage(Component.text("♡ " + amount + " Herz(en) von " + target.getName() + " entfernt! (Noch: " + data.getHearts() + " ❤)").color(INFO_COLOR));

        Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
        if (onlineTarget != null) {
            onlineTarget.sendMessage(Component.text("♡ Dir wurden " + amount + " Herz(en) entzogen! (Noch: " + data.getHearts() + " ❤)").color(ERROR_COLOR));
            plugin.getHeartManager().updateActionBar(onlineTarget);
        }

        if (outOfHearts) {
            plugin.getGameManager().handlePlayerEliminated(target.getUniqueId(), target.getName());
        }
    }

    // ===== /risiko unban <spieler> =====
    @SuppressWarnings("deprecation")
    private void handleUnban(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Verwendung: /risiko unban <spieler>").color(ERROR_COLOR));
            return;
        }

        String playerName = args[1];

        // Name-Ban entfernen
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(playerName);

        // Spieler in der Datenbank aktualisieren
        for (PlayerData data : plugin.getDataManager().getAll().values()) {
            if (data.getName().equalsIgnoreCase(playerName)) {
                data.setBanned(false);
                if (data.getHearts() <= 0) {
                    // Maximale Herzen wiederherstellen (berücksichtigt König-Status)
                    plugin.getHeartManager().resetHearts(data);
                }
                plugin.getDataManager().save(data.getUuid());
                break;
            }
        }

        sender.sendMessage(Component.text("✔ " + playerName + " wurde entbannt!").color(SUCCESS_COLOR));
        Bukkit.broadcast(Component.text(playerName + " wurde vom Admin entbannt und kann wieder mitspielen!").color(NamedTextColor.GREEN));
    }

    // ===== /risiko start =====
    private void handleStart(CommandSender sender) {
        String error = plugin.getGameManager().startGame();
        if (error != null) {
            sender.sendMessage(Component.text("❌ " + error).color(ERROR_COLOR));
        } else {
            sender.sendMessage(Component.text("✔ Finale gestartet!").color(SUCCESS_COLOR));
        }
    }

    // ===== /risiko stop =====
    private void handleStop(CommandSender sender) {
        if (!plugin.getGameManager().isGameRunning()) {
            sender.sendMessage(Component.text("Es läuft kein Finale!").color(ERROR_COLOR));
            return;
        }
        plugin.getGameManager().stopGame("Manuell gestoppt von " + sender.getName());
        sender.sendMessage(Component.text("✔ Finale gestoppt!").color(SUCCESS_COLOR));
    }

    // ===== /risiko setspawn <fichten|dschungel> =====
    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Dieser Befehl kann nur von Spielern verwendet werden!").color(ERROR_COLOR));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Verwendung: /risiko setspawn <fichten|dschungel>").color(ERROR_COLOR));
            return;
        }

        Kingdom kingdom = Kingdom.fromString(args[1]);
        if (kingdom == null) {
            sender.sendMessage(Component.text("Ungültiges Königreich! Verwende: fichten oder dschungel").color(ERROR_COLOR));
            return;
        }

        plugin.getGameManager().saveSpawn(kingdom, player.getLocation());
        sender.sendMessage(Component.text("✔ Spawn für " + kingdom.getDisplayName() + " gesetzt auf "
                + String.format("%.1f, %.1f, %.1f", player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()))
                .color(SUCCESS_COLOR));
    }

    // ===== /risiko info [spieler] =====
    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            // Info über bestimmten Spieler
            OfflinePlayer target = findPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Spieler nicht gefunden!").color(ERROR_COLOR));
                return;
            }
            PlayerData data = plugin.getDataManager().get(target.getUniqueId());
            if (data == null) {
                sender.sendMessage(Component.text("Keine Daten für " + args[1] + " gefunden!").color(ERROR_COLOR));
                return;
            }
            sendPlayerInfo(sender, data);
        } else {
            // Info über Finale
            sendGameInfo(sender);
        }
    }

    private void sendPlayerInfo(CommandSender sender, PlayerData data) {
        sender.sendMessage(Component.text("══════ Spieler-Info ══════").color(HEADER_COLOR));
        sender.sendMessage(Component.text("Name: ").color(NamedTextColor.GRAY)
                .append(Component.text(data.getName()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Königreich: ").color(NamedTextColor.GRAY)
                .append(data.getKingdom() != null
                        ? Component.text(data.getKingdom().getDisplayName()).color(
                                data.getKingdom() == Kingdom.FICHTEN ? TextColor.color(0xFFAA00) : TextColor.color(0x00AA00))
                        : Component.text("Keines").color(NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.text("König: ").color(NamedTextColor.GRAY)
                .append(Component.text(data.isKing() ? "♔ Ja" : "Nein")
                        .color(data.isKing() ? TextColor.color(0xFFD700) : NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("Herzen: ").color(NamedTextColor.GRAY)
                .append(Component.text("❤ " + data.getHearts()).color(HEART_COLOR)));
        sender.sendMessage(Component.text("Im Finale: ").color(NamedTextColor.GRAY)
                .append(Component.text(data.isInGame() ? "Ja" : "Nein")
                        .color(data.isInGame() ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("Gebannt: ").color(NamedTextColor.GRAY)
                .append(Component.text(data.isBanned() ? "Ja" : "Nein")
                        .color(data.isBanned() ? NamedTextColor.RED : NamedTextColor.GREEN)));
    }

    private void sendGameInfo(CommandSender sender) {
        GameManager gm = plugin.getGameManager();
        sender.sendMessage(Component.text("══════ Risiko-Finale Info ══════").color(HEADER_COLOR));
        sender.sendMessage(Component.text("Status: ").color(NamedTextColor.GRAY)
                .append(Component.text(gm.isGameRunning() ? "▶ Läuft" : "⏹ Gestoppt")
                        .color(gm.isGameRunning() ? NamedTextColor.GREEN : NamedTextColor.RED)));

        for (Kingdom k : Kingdom.values()) {
            TextColor kColor = k == Kingdom.FICHTEN ? TextColor.color(0xFFAA00) : TextColor.color(0x00AA00);
            int alive = plugin.getTeamManager().getAliveKingdomMembers(k).size();
            int total = plugin.getTeamManager().getKingdomMembers(k).size();
            sender.sendMessage(Component.text(k.getDisplayName() + ": ").color(kColor)
                    .append(Component.text(alive + "/" + total + " am Leben").color(NamedTextColor.WHITE)));
        }

        String spawnF = gm.getSpawnFichten() != null
                ? String.format("%.0f,%.0f,%.0f", gm.getSpawnFichten().getX(), gm.getSpawnFichten().getY(), gm.getSpawnFichten().getZ())
                : "Nicht gesetzt";
        String spawnD = gm.getSpawnDschungel() != null
                ? String.format("%.0f,%.0f,%.0f", gm.getSpawnDschungel().getX(), gm.getSpawnDschungel().getY(), gm.getSpawnDschungel().getZ())
                : "Nicht gesetzt";

        sender.sendMessage(Component.text("Spawn Fichten: ").color(NamedTextColor.GRAY)
                .append(Component.text(spawnF).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Spawn Dschungel: ").color(NamedTextColor.GRAY)
                .append(Component.text(spawnD).color(NamedTextColor.WHITE)));
    }

    // ===== /risiko list =====
    private void handleList(CommandSender sender) {
        sender.sendMessage(Component.text("══════ Risiko-Spieler-Liste ══════").color(HEADER_COLOR));

        for (Kingdom kingdom : Kingdom.values()) {
            TextColor kColor = kingdom == Kingdom.FICHTEN ? TextColor.color(0xFFAA00) : TextColor.color(0x00AA00);
            List<PlayerData> members = plugin.getTeamManager().getKingdomMembers(kingdom);

            sender.sendMessage(Component.text(kingdom.getDisplayName() + " (" + members.size() + " Spieler):").color(kColor).decoration(TextDecoration.BOLD, true));

            if (members.isEmpty()) {
                sender.sendMessage(Component.text("  (Keine Mitglieder)").color(NamedTextColor.DARK_GRAY));
            } else {
                for (PlayerData data : members) {
                    String prefix = data.isKing() ? "♔ " : "  ";
                    Component statusComp;
                    if (data.isBanned()) {
                        statusComp = Component.text(" [GEBANNT]").color(NamedTextColor.RED);
                    } else {
                        statusComp = Component.text(" [" + data.getHearts() + " ❤]").color(TextColor.color(0xFF6600));
                    }
                    sender.sendMessage(Component.text(prefix + data.getName()).color(NamedTextColor.WHITE).append(statusComp));
                }
            }
        }

        // Spieler ohne Königreich
        List<PlayerData> noKingdom = new ArrayList<>();
        for (PlayerData data : plugin.getDataManager().getAll().values()) {
            if (data.getKingdom() == null) noKingdom.add(data);
        }
        if (!noKingdom.isEmpty()) {
            sender.sendMessage(Component.text("Ohne Königreich:").color(NamedTextColor.GRAY));
            for (PlayerData data : noKingdom) {
                sender.sendMessage(Component.text("  " + data.getName()).color(NamedTextColor.DARK_GRAY));
            }
        }
    }

    // ===== /risiko reload =====
    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getGameManager().loadSpawns();
        plugin.getTeamManager().loadKingsFromData();
        sender.sendMessage(Component.text("✔ Konfiguration neu geladen!").color(SUCCESS_COLOR));
    }

    // ===== Hilfe =====
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("══════ Risiko-Befehle ══════").color(HEADER_COLOR));
        sender.sendMessage(Component.text("/risiko team <spieler> <fichten|dschungel>").color(INFO_COLOR)
                .append(Component.text(" - Spieler einem Königreich zuweisen").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/risiko king <spieler>").color(INFO_COLOR)
                .append(Component.text(" - Spieler zum König machen").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/risiko addherz <spieler> [anzahl]").color(INFO_COLOR)
                .append(Component.text(" - Herzen hinzufügen").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/risiko removeherz <spieler> [anzahl]").color(INFO_COLOR)
                .append(Component.text(" - Herzen entfernen").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/risiko unban <spieler>").color(INFO_COLOR)
                .append(Component.text(" - Spieler entbannen").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/risiko start").color(INFO_COLOR)
                .append(Component.text(" - Finale starten").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/risiko stop").color(INFO_COLOR)
                .append(Component.text(" - Finale stoppen").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/risiko setspawn <fichten|dschungel>").color(INFO_COLOR)
                .append(Component.text(" - Spawn-Punkt setzen").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/risiko info [spieler]").color(INFO_COLOR)
                .append(Component.text(" - Informationen anzeigen").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/risiko list").color(INFO_COLOR)
                .append(Component.text(" - Alle Spieler und Teams anzeigen").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/risiko reload").color(INFO_COLOR)
                .append(Component.text(" - Konfiguration neu laden").color(NamedTextColor.GRAY)));
    }

    // ===== Tab-Completion =====
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("risiko.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return filterStart(args[0], "team", "king", "addherz", "removeherz", "unban",
                    "start", "stop", "setspawn", "info", "list", "reload");
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "team", "king", "addherz", "removeherz", "info" -> {
                    List<String> names = new ArrayList<>();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        names.add(p.getName());
                    }
                    // Auch offline Spieler aus der Datenbank
                    for (PlayerData data : plugin.getDataManager().getAll().values()) {
                        if (!names.contains(data.getName())) {
                            names.add(data.getName());
                        }
                    }
                    return filterStart(args[1], names.toArray(new String[0]));
                }
                case "unban" -> {
                    List<String> banned = new ArrayList<>();
                    for (PlayerData data : plugin.getDataManager().getAll().values()) {
                        if (data.isBanned()) banned.add(data.getName());
                    }
                    return filterStart(args[1], banned.toArray(new String[0]));
                }
                case "setspawn" -> {
                    return filterStart(args[1], "fichten", "dschungel");
                }
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("team")) {
            return filterStart(args[2], "fichten", "dschungel");
        }

        return Collections.emptyList();
    }

    // ===== Hilfsmethoden =====

    private List<String> filterStart(String prefix, String... options) {
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(opt);
            }
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer findPlayer(String name) {
        // Zuerst Online-Spieler suchen
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;

        // Dann in der Datenbank suchen
        for (PlayerData data : plugin.getDataManager().getAll().values()) {
            if (data.getName().equalsIgnoreCase(name)) {
                return Bukkit.getOfflinePlayer(data.getUuid());
            }
        }

        // Letzter Versuch: Bukkit.getOfflinePlayer (kann API-Anfrage stellen)
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
        return offlinePlayer.hasPlayedBefore() ? offlinePlayer : null;
    }

    // Hilfsklasse wird nicht mehr benötigt (nutze org.bukkit.BanList direkt)
}
