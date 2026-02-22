package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import de.duellplugin.managers.EventManager;
import de.duellplugin.managers.EventManager.GameEvent;
import de.duellplugin.managers.EventManager.EventFormat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * /event <subcommand> [args...]
 *
 * 50+ subcommands for planning and running server events.
 * Admin-only subcommands require OP or duell.admin permission.
 * Players can use: join, leave, info, list
 */
public class EventCommand implements CommandExecutor, TabCompleter {

    private final DuellPlugin plugin;

    private static final List<String> ADMIN_SUBS = Arrays.asList(
            "create", "delete", "select", "start", "stop", "pause", "resume",
            "broadcast", "announce", "setkit", "setarena", "setformat", "setmaxplayers",
            "setteamsize", "setlives", "setwinner", "setwinnermsg", "setjoinmsg", "setmotd",
            "setround", "settime", "tp", "tpall", "kick", "ban", "unban",
            "addplayer", "removeplayer", "spectate", "unspectate",
            "setscore", "addscore", "setplayerlives",
            "getplayers", "topplayers", "healall", "feedall",
            "giveitem", "clearinventory", "addkit", "removekit", "setkits",
            "countdown", "endmatch", "nextround", "setspawn"
    );
    private static final List<String> PLAYER_SUBS = Arrays.asList("join", "leave", "info", "list");

    public EventCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isAdmin(CommandSender s) {
        return s.isOp() || s.hasPermission("duell.admin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();
        EventManager em = plugin.getEventManager();

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // ── Player-accessible subcommands ────────────────────────────────────
        switch (sub) {
            case "list", "liste" -> {
                if (em.getAllEvents().isEmpty()) {
                    sender.sendMessage(prefix + "§7Keine Events vorhanden.");
                    return true;
                }
                sender.sendMessage("§6§l━━━ Events ━━━");
                for (GameEvent ev : em.getAllEvents()) {
                    sender.sendMessage("§e" + ev.getName() + " §7– " + ev.getState().name()
                            + " §7(" + ev.getParticipants().size() + "/" + ev.getMaxPlayers() + " Spieler)");
                }
                sender.sendMessage("§6§l━━━━━━━━━━━━━━━");
                return true;
            }
            case "info" -> {
                GameEvent ev = (args.length >= 2) ? em.getEvent(args[1]) : em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein aktives Event gefunden."); return true; }
                printEventInfo(sender, ev);
                return true;
            }
            case "join" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(prefix + "§cNur für Spieler!"); return true; }
                GameEvent ev = (args.length >= 2) ? em.getEvent(args[1]) : em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event gefunden."); return true; }
                em.joinEvent(player, ev);
                return true;
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(prefix + "§cNur für Spieler!"); return true; }
                GameEvent ev = em.getEventForPlayer(player.getUniqueId());
                if (ev == null) { sender.sendMessage(prefix + "§cDu bist in keinem Event."); return true; }
                em.leaveEvent(player, ev);
                player.sendMessage(prefix + "§aDu hast das Event verlassen.");
                return true;
            }
        }

        // ── Admin-only subcommands ────────────────────────────────────────────
        if (!isAdmin(sender)) {
            sender.sendMessage(prefix + "§cDu hast keine Berechtigung!");
            return true;
        }

        switch (sub) {
            // ── Event management ──────────────────────────────────────────────
            case "create", "erstellen" -> {
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event create <name>"); return true; }
                String name = args[1].toLowerCase();
                if (em.getEvent(name) != null) { sender.sendMessage(prefix + "§cEvent '" + name + "' existiert bereits!"); return true; }
                GameEvent ev = em.createEvent(name);
                sender.sendMessage(prefix + "§aEvent §6" + name + " §aerstellt und ausgewählt!");
                sender.sendMessage(prefix + "§7Konfiguriere es mit /event set* und starte es mit /event start.");
            }
            case "delete", "loeschen" -> {
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event delete <name>"); return true; }
                if (!em.deleteEvent(args[1])) { sender.sendMessage(prefix + "§cEvent nicht gefunden!"); return true; }
                sender.sendMessage(prefix + "§aEvent §6" + args[1] + " §agelöscht!");
            }
            case "select", "waehlen" -> {
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event select <name>"); return true; }
                GameEvent ev = em.getEvent(args[1]);
                if (ev == null) { sender.sendMessage(prefix + "§cEvent nicht gefunden!"); return true; }
                em.setCurrentEvent(ev);
                sender.sendMessage(prefix + "§aEvent §6" + ev.getName() + " §aausgewählt.");
            }
            case "start", "starten" -> {
                GameEvent ev = resolveEvent(sender, args, em);
                if (ev == null) return true;
                em.startEvent(ev);
                sender.sendMessage(prefix + "§aEvent §6" + ev.getName() + " §agestartet!");
            }
            case "stop", "stoppen" -> {
                GameEvent ev = resolveEvent(sender, args, em);
                if (ev == null) return true;
                em.stopEvent(ev);
                sender.sendMessage(prefix + "§cEvent §6" + ev.getName() + " §cgestoppt.");
            }
            case "pause" -> {
                GameEvent ev = resolveEvent(sender, args, em);
                if (ev == null) return true;
                em.pauseEvent(ev);
                sender.sendMessage(prefix + "§eEvent pausiert.");
            }
            case "resume", "fortsetzen" -> {
                GameEvent ev = resolveEvent(sender, args, em);
                if (ev == null) return true;
                em.resumeEvent(ev);
                sender.sendMessage(prefix + "§aEvent fortgesetzt.");
            }
            case "countdown" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                int secs = parseIntArg(sender, args, 1, prefix, 10);
                if (secs < 0) return true;
                em.startCountdown(ev, secs);
                sender.sendMessage(prefix + "§aCountdown §6" + secs + "s §agestartet.");
            }
            case "endmatch" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                em.broadcastToEvent(ev, prefix + "§cDas Match wurde beendet!");
                sender.sendMessage(prefix + "§aMatch beendet.");
            }
            case "nextround" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                ev.setRound(ev.getRound() + 1);
                em.broadcastToEvent(ev, prefix + "§aRunde §6" + ev.getRound() + " §astartet!");
                em.applyKitToAll(ev);
                sender.sendMessage(prefix + "§aRunde §6" + ev.getRound() + " §agestartet, Kits vergeben.");
            }

            // ── Messaging ──────────────────────────────────────────────────────
            case "broadcast", "senden" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event broadcast <Nachricht>"); return true; }
                String msg = de.duellplugin.ChatUtils.color(joinArgs(args, 1));
                em.broadcastToEvent(ev, prefix + "§6[Event] §f" + msg);
                sender.sendMessage(prefix + "§aNachricht gesendet.");
            }
            case "announce", "ankuendigen" -> {
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event announce <Nachricht>"); return true; }
                String msg = de.duellplugin.ChatUtils.color(joinArgs(args, 1));
                em.broadcastToAll(prefix + "§d[Event-Ankündigung] §f" + msg);
            }

            // ── Configuration ──────────────────────────────────────────────────
            case "setkit" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event setkit <kit>"); return true; }
                ev.setKitName(args[1].toLowerCase());
                sender.sendMessage(prefix + "§aKit §6" + args[1] + " §agesetzt.");
            }
            case "setarena" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event setarena <arena>"); return true; }
                ev.setArenaName(args[1].toLowerCase());
                sender.sendMessage(prefix + "§aArena §6" + args[1] + " §agesetzt.");
            }
            case "setformat" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event setformat <ffa|1v1|2v2|4v4>"); return true; }
                switch (args[1].toLowerCase()) {
                    case "ffa"  -> ev.setFormat(EventFormat.FFA);
                    case "1v1"  -> { ev.setFormat(EventFormat.VS_1v1); ev.setTeamSize(1); }
                    case "2v2"  -> { ev.setFormat(EventFormat.VS_2v2); ev.setTeamSize(2); }
                    case "4v4"  -> { ev.setFormat(EventFormat.VS_4v4); ev.setTeamSize(4); }
                    default     -> { sender.sendMessage(prefix + "§cUnbekanntes Format! Benutze: ffa, 1v1, 2v2, 4v4"); return true; }
                }
                sender.sendMessage(prefix + "§aFormat §6" + args[1] + " §agesetzt.");
            }
            case "setmaxplayers", "maxplayer" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                int n = parseIntArg(sender, args, 1, prefix, -1); if (n < 0) return true;
                ev.setMaxPlayers(n);
                sender.sendMessage(prefix + "§aMax. Spieler: §6" + n);
            }
            case "setteamsize", "teamgroesse" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                int n = parseIntArg(sender, args, 1, prefix, -1); if (n < 0) return true;
                ev.setTeamSize(n);
                sender.sendMessage(prefix + "§aTeamgröße: §6" + n);
            }
            case "setlives", "leben" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                int n = parseIntArg(sender, args, 1, prefix, -1); if (n < 0) return true;
                ev.setDefaultLives(n);
                sender.sendMessage(prefix + "§aStandard-Leben: §6" + n);
            }
            case "setround", "runde" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                int n = parseIntArg(sender, args, 1, prefix, -1); if (n < 0) return true;
                ev.setRound(n);
                sender.sendMessage(prefix + "§aRunde gesetzt: §6" + n);
            }
            case "settime", "zeit" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                int n = parseIntArg(sender, args, 1, prefix, -1); if (n < 0) return true;
                ev.setRemainingSeconds(n);
                sender.sendMessage(prefix + "§aVerbleibende Zeit: §6" + n + "s");
            }
            case "setwinnermsg", "gewinnnachricht" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event setwinnermsg <Nachricht>"); return true; }
                ev.setWinMessage(de.duellplugin.ChatUtils.color(joinArgs(args, 1)));
                sender.sendMessage(prefix + "§aGewinn-Nachricht gesetzt.");
            }
            case "setjoinmsg", "beitretnachricht" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event setjoinmsg <Nachricht>"); return true; }
                ev.setJoinMessage(de.duellplugin.ChatUtils.color(joinArgs(args, 1)));
                sender.sendMessage(prefix + "§aBeitritts-Nachricht gesetzt.");
            }
            case "setmotd" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event setmotd <Text>"); return true; }
                ev.setMotd(de.duellplugin.ChatUtils.color(joinArgs(args, 1)));
                sender.sendMessage(prefix + "§aMOTD gesetzt.");
            }
            case "setspawn" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(prefix + "§cNur für Spieler!"); return true; }
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                ev.setSpawnLocation(player.getLocation());
                sender.sendMessage(prefix + "§aEvent-Spawn auf deine Position gesetzt!");
            }

            // ── Allowed kits ───────────────────────────────────────────────────
            case "addkit" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event addkit <kit>"); return true; }
                em.addAllowedKit(ev, args[1].toLowerCase());
                sender.sendMessage(prefix + "§aKit §6" + args[1] + " §azum Event hinzugefügt.");
            }
            case "removekit" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event removekit <kit>"); return true; }
                em.removeAllowedKit(ev, args[1].toLowerCase());
                sender.sendMessage(prefix + "§aKit §6" + args[1] + " §aentfernt.");
            }
            case "setkits" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event setkits <kit1,kit2,...>"); return true; }
                em.setAllowedKits(ev, Arrays.asList(args[1].split(",")));
                sender.sendMessage(prefix + "§aErlaubte Kits gesetzt: §6" + args[1]);
            }

            // ── Player management ──────────────────────────────────────────────
            case "addplayer", "hinzufuegen" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event addplayer <spieler>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                em.joinEvent(target, ev);
                sender.sendMessage(prefix + "§a" + target.getName() + " §azum Event hinzugefügt.");
            }
            case "removeplayer", "entfernen" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event removeplayer <spieler>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                em.leaveEvent(target, ev);
                sender.sendMessage(prefix + "§a" + target.getName() + " §aaus Event entfernt.");
            }
            case "kick", "kicken" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event kick <spieler>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                em.kickPlayer(target, ev);
                sender.sendMessage(prefix + "§a" + target.getName() + " §agekickt.");
            }
            case "ban", "verbannen" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event ban <spieler>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                em.banPlayer(target, ev);
                sender.sendMessage(prefix + "§a" + target.getName() + " §agesperrt.");
            }
            case "unban", "entsperren" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event unban <spieler>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                em.unbanPlayer(target.getUniqueId(), ev);
                sender.sendMessage(prefix + "§a" + target.getName() + " §aentbannt.");
            }
            case "spectate", "zuschauen" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event spectate <spieler>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                em.addSpectator(target, ev);
                sender.sendMessage(prefix + "§a" + target.getName() + " §aist jetzt Zuschauer.");
            }
            case "unspectate" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event unspectate <spieler>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                em.removeSpectator(target, ev);
                sender.sendMessage(prefix + "§a" + target.getName() + " §aist kein Zuschauer mehr.");
            }
            case "tp" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null || ev.getSpawnLocation() == null) { sender.sendMessage(prefix + "§cKein Event-Spawn gesetzt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event tp <spieler>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                target.teleport(ev.getSpawnLocation());
                sender.sendMessage(prefix + "§a" + target.getName() + " §azu Event-Spawn teleportiert.");
            }
            case "tpall", "alltp" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null || ev.getSpawnLocation() == null) { sender.sendMessage(prefix + "§cKein Event-Spawn gesetzt!"); return true; }
                em.tpAll(ev);
                sender.sendMessage(prefix + "§aAlle §6" + ev.getParticipants().size() + " §aSpieler teleportiert.");
            }
            case "setwinner", "gewinner" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event setwinner <spieler>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                em.setWinner(target, ev);
                sender.sendMessage(prefix + "§a" + target.getName() + " §aals Gewinner gesetzt.");
            }

            // ── Scores ─────────────────────────────────────────────────────────
            case "setscore" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 3) { sender.sendMessage(prefix + "§c/event setscore <spieler> <wert>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                int n = parseIntArg(sender, args, 2, prefix, -999); if (n == -999) return true;
                em.setScore(target.getUniqueId(), ev, n);
                sender.sendMessage(prefix + "§aScore von §6" + target.getName() + " §aauf §6" + n + " §agesetzt.");
            }
            case "addscore" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 3) { sender.sendMessage(prefix + "§c/event addscore <spieler> <wert>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                int n = parseIntArg(sender, args, 2, prefix, -999); if (n == -999) return true;
                em.addScore(target.getUniqueId(), ev, n);
                sender.sendMessage(prefix + "§a" + n + " §aPunkte zu §6" + target.getName() + " §ahinzugefügt.");
            }
            case "setplayerlives" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 3) { sender.sendMessage(prefix + "§c/event setplayerlives <spieler> <n>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                int n = parseIntArg(sender, args, 2, prefix, -1); if (n < 0) return true;
                em.setPlayerLives(target.getUniqueId(), ev, n);
                sender.sendMessage(prefix + "§aLeben von §6" + target.getName() + " §aauf §6" + n + " §agesetzt.");
            }

            // ── Info / Stats ───────────────────────────────────────────────────
            case "getplayers", "spieler" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                sender.sendMessage("§6§l━━━ Event-Spieler (" + ev.getParticipants().size() + ") ━━━");
                for (UUID uid : ev.getParticipants()) {
                    Player p = Bukkit.getPlayer(uid);
                    String name = p != null ? p.getName() : uid.toString().substring(0, 8) + "...";
                    int score = ev.getScores().getOrDefault(uid, 0);
                    int lives = ev.getLives().getOrDefault(uid, ev.getDefaultLives());
                    sender.sendMessage("§e" + name + " §7– Score: §6" + score + " §7| Leben: §6" + lives);
                }
                sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━");
            }
            case "topplayers", "topliste" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                int n = (args.length >= 2) ? safeParseInt(args[1], 5) : 5;
                sender.sendMessage("§6§l━━━ Top §e" + n + " §6Spieler ━━━");
                int rank = 1;
                for (var entry : em.getTopPlayers(ev, n)) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    String name = p != null ? p.getName() : entry.getKey().toString().substring(0, 8);
                    sender.sendMessage("§e#" + rank++ + " §f" + name + " §7– §6" + entry.getValue() + " §7Punkte");
                }
                sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━");
            }

            // ── Item / Inventory ───────────────────────────────────────────────
            case "healall", "heilenalle" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                em.healAll(ev);
                sender.sendMessage(prefix + "§aAlle Spieler geheilt.");
            }
            case "feedall", "satigenalle" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                em.feedAll(ev);
                sender.sendMessage(prefix + "§aAlle Spieler gesättigt.");
            }
            case "giveitem", "gib" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 3) { sender.sendMessage(prefix + "§c/event giveitem <spieler> <material> [menge]"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                Material mat;
                try { mat = Material.valueOf(args[2].toUpperCase()); } catch (Exception e) { sender.sendMessage(prefix + "§cUnbekanntes Material: " + args[2]); return true; }
                int amount = args.length >= 4 ? safeParseInt(args[3], 1) : 1;
                em.giveItem(target, mat, amount);
                sender.sendMessage(prefix + "§a" + amount + "x §6" + args[2] + " §aan §6" + target.getName() + " §agegeben.");
            }
            case "clearinventory", "clearinv" -> {
                GameEvent ev = em.getCurrentEvent();
                if (ev == null) { sender.sendMessage(prefix + "§cKein Event ausgewählt!"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + "§c/event clearinventory <spieler>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix + "§cSpieler nicht gefunden!"); return true; }
                em.clearInventory(target);
                sender.sendMessage(prefix + "§aInventar von §6" + target.getName() + " §ageleert.");
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    // ── Helper: resolve current event, or named event if args[1] given ──────
    private GameEvent resolveEvent(CommandSender sender, String[] args, EventManager em) {
        GameEvent ev = (args.length >= 2) ? em.getEvent(args[1]) : em.getCurrentEvent();
        if (ev == null) sender.sendMessage(plugin.getPrefix() + "§cKein Event gefunden. Erstelle eines mit /event create <name>.");
        return ev;
    }

    private int parseIntArg(CommandSender sender, String[] args, int idx, String prefix, int fallback) {
        if (args.length <= idx) { sender.sendMessage(prefix + "§cZahl erwartet!"); return fallback; }
        try { return Integer.parseInt(args[idx]); } catch (NumberFormatException e) { sender.sendMessage(prefix + "§c'" + args[idx] + "' ist keine Zahl!"); return fallback; }
    }

    private int safeParseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private String joinArgs(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) { if (i > from) sb.append(' '); sb.append(args[i]); }
        return sb.toString();
    }

    private void printEventInfo(CommandSender sender, GameEvent ev) {
        sender.sendMessage("§6§l━━━ Event: " + ev.getName() + " ━━━");
        sender.sendMessage("§7Status: §e" + ev.getState().name());
        sender.sendMessage("§7Format: §e" + ev.getFormat().name());
        sender.sendMessage("§7Kit: §e" + ev.getKitName());
        sender.sendMessage("§7Arena: §e" + (ev.getArenaName() != null ? ev.getArenaName() : "§cnicht gesetzt"));
        sender.sendMessage("§7Spieler: §e" + ev.getParticipants().size() + " §7/ §e" + ev.getMaxPlayers());
        sender.sendMessage("§7Runde: §e" + ev.getRound());
        sender.sendMessage("§7Standard-Leben: §e" + ev.getDefaultLives());
        if (!ev.getAllowedKits().isEmpty()) sender.sendMessage("§7Erlaubte Kits: §e" + String.join(", ", ev.getAllowedKits()));
        sender.sendMessage("§7MOTD: §f" + ev.getMotd());
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━");
    }

    private void sendHelp(CommandSender sender) {
        boolean admin = isAdmin(sender);
        sender.sendMessage("§6§l━━━ Event-Befehle ━━━");
        sender.sendMessage("§e/event list §7- Alle Events anzeigen");
        sender.sendMessage("§e/event info [name] §7- Event-Details");
        sender.sendMessage("§e/event join [name] §7- Event beitreten");
        sender.sendMessage("§e/event leave §7- Event verlassen");
        if (admin) {
            sender.sendMessage("§7-- Admin-Befehle --");
            sender.sendMessage("§e/event create <name> §7- Neues Event erstellen");
            sender.sendMessage("§e/event delete <name> §7- Event löschen");
            sender.sendMessage("§e/event select <name> §7- Event auswählen");
            sender.sendMessage("§e/event start [name] §7- Event starten");
            sender.sendMessage("§e/event stop [name] §7- Event beenden");
            sender.sendMessage("§e/event pause §7- Event pausieren");
            sender.sendMessage("§e/event resume §7- Event fortsetzen");
            sender.sendMessage("§e/event countdown <sek> §7- Countdown starten");
            sender.sendMessage("§e/event setspawn §7- Spawn setzen");
            sender.sendMessage("§e/event setkit <kit> §7- Kit setzen");
            sender.sendMessage("§e/event setarena <arena> §7- Arena setzen");
            sender.sendMessage("§e/event setformat <ffa|1v1|2v2|4v4> §7- Format");
            sender.sendMessage("§e/event setmaxplayers <n> §7- Max. Spieler");
            sender.sendMessage("§e/event setteamsize <n> §7- Teamgröße");
            sender.sendMessage("§e/event setlives <n> §7- Standard-Leben");
            sender.sendMessage("§e/event setwinnermsg <txt> §7- Gewinn-Nachricht");
            sender.sendMessage("§e/event setjoinmsg <txt> §7- Beitritts-Nachricht");
            sender.sendMessage("§e/event setmotd <txt> §7- MOTD");
            sender.sendMessage("§e/event broadcast <txt> §7- Nachricht ans Event senden");
            sender.sendMessage("§e/event announce <txt> §7- Server-Ankündigung");
            sender.sendMessage("§e/event tp <spieler> §7- Spieler teleportieren");
            sender.sendMessage("§e/event tpall §7- Alle zum Spawn tp");
            sender.sendMessage("§e/event addplayer <s> §7- Spieler hinzufügen");
            sender.sendMessage("§e/event removeplayer <s> §7- Spieler entfernen");
            sender.sendMessage("§e/event kick <s> §7- Spieler kicken");
            sender.sendMessage("§e/event ban <s> §7- Spieler sperren");
            sender.sendMessage("§e/event unban <s> §7- Spieler entsperren");
            sender.sendMessage("§e/event spectate <s> §7- Zu Zuschauer machen");
            sender.sendMessage("§e/event unspectate <s> §7- Zuschauer entfernen");
            sender.sendMessage("§e/event setwinner <s> §7- Gewinner setzen");
            sender.sendMessage("§e/event setscore <s> <n> §7- Score setzen");
            sender.sendMessage("§e/event addscore <s> <n> §7- Score hinzufügen");
            sender.sendMessage("§e/event setplayerlives <s> <n> §7- Leben setzen");
            sender.sendMessage("§e/event getplayers §7- Spielerliste");
            sender.sendMessage("§e/event topplayers [n] §7- Top-Spieler");
            sender.sendMessage("§e/event healall §7- Alle heilen");
            sender.sendMessage("§e/event feedall §7- Alle sättigen");
            sender.sendMessage("§e/event giveitem <s> <mat> [n] §7- Item geben");
            sender.sendMessage("§e/event clearinventory <s> §7- Inventar leeren");
            sender.sendMessage("§e/event addkit <kit> §7- Erlaubtes Kit hinzufügen");
            sender.sendMessage("§e/event removekit <kit> §7- Kit entfernen");
            sender.sendMessage("§e/event setkits <k1,k2,...> §7- Kits setzen");
            sender.sendMessage("§e/event setround <n> §7- Runde setzen");
            sender.sendMessage("§e/event settime <sek> §7- Zeit setzen");
            sender.sendMessage("§e/event endmatch §7- Match beenden");
            sender.sendMessage("§e/event nextround §7- Nächste Runde");
        }
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(PLAYER_SUBS);
            if (isAdmin(sender)) subs.addAll(ADMIN_SUBS);
            return filterStartsWith(subs, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("delete") || sub.equals("select") || sub.equals("start") || sub.equals("stop") || sub.equals("info")) {
                List<String> names = new ArrayList<>();
                for (GameEvent ev : plugin.getEventManager().getAllEvents()) names.add(ev.getName());
                return filterStartsWith(names, args[1]);
            }
            if (sub.equals("join")) {
                List<String> names = new ArrayList<>();
                for (GameEvent ev : plugin.getEventManager().getAllEvents()) names.add(ev.getName());
                return filterStartsWith(names, args[1]);
            }
            if (sub.equals("setformat")) {
                return filterStartsWith(Arrays.asList("ffa", "1v1", "2v2", "4v4"), args[1]);
            }
            if (sub.equals("setkit") || sub.equals("addkit")) {
                List<String> kitNames = new ArrayList<>();
                for (var k : plugin.getKitManager().getAllKits()) kitNames.add(k.getName());
                return filterStartsWith(kitNames, args[1]);
            }
            if (sub.equals("setarena")) {
                List<String> arenas = new ArrayList<>();
                for (var a : plugin.getArenaManager().getAllArenas()) arenas.add(a.getName());
                return filterStartsWith(arenas, args[1]);
            }
            // Player-name args
            List<String> playerSubs = Arrays.asList("tp", "kick", "ban", "unban", "addplayer", "removeplayer",
                    "spectate", "unspectate", "setwinner", "setscore", "addscore", "setplayerlives",
                    "giveitem", "clearinventory");
            if (playerSubs.contains(sub)) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                return filterStartsWith(names, args[1]);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("giveitem")) {
            List<String> mats = new ArrayList<>();
            for (Material m : Material.values()) mats.add(m.name().toLowerCase());
            return filterStartsWith(mats, args[2]);
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
