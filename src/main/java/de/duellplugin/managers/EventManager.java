package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages server events (tournaments, custom PvP events).
 * One event can be "active" at a time. OPs plan and control it; players join via /event join.
 */
public class EventManager {

    public enum EventState { WAITING, COUNTDOWN, ACTIVE, PAUSED, ENDED }
    public enum EventFormat { FFA, VS_1v1, VS_2v2, VS_4v4 }

    // ── Inner GameEvent ──────────────────────────────────────────────────────
    public static class GameEvent {
        private String name;
        private String motd = "§7Willkommen beim Event!";
        private String joinMessage = "§a%player% §7ist dem Event beigetreten!";
        private String winMessage  = "§6%player% §ahat das Event gewonnen!";
        private EventState state = EventState.WAITING;
        private EventFormat format = EventFormat.FFA;
        private String kitName = "nodebuff";
        private String arenaName;
        private Location spawnLocation;
        private int maxPlayers = 16;
        private int teamSize = 1;
        private int defaultLives = 1;
        private int round = 1;
        private int remainingSeconds = -1;
        private final Set<UUID> participants = new LinkedHashSet<>();
        private final Set<UUID> spectators   = new LinkedHashSet<>();
        private final Set<UUID> banned       = new LinkedHashSet<>();
        private final Map<UUID, Integer> scores = new LinkedHashMap<>();
        private final Map<UUID, Integer> lives  = new LinkedHashMap<>();
        private final Set<String> allowedKits    = new LinkedHashSet<>();
        private BukkitTask timerTask;

        GameEvent(String name) {
            this.name = name;
        }

        // Accessors
        public String getName()                     { return name; }
        public EventState getState()                { return state; }
        public EventFormat getFormat()              { return format; }
        public String getKitName()                  { return kitName; }
        public String getArenaName()                { return arenaName; }
        public Location getSpawnLocation()          { return spawnLocation; }
        public int getMaxPlayers()                  { return maxPlayers; }
        public int getTeamSize()                    { return teamSize; }
        public int getDefaultLives()                { return defaultLives; }
        public int getRound()                       { return round; }
        public int getRemainingSeconds()            { return remainingSeconds; }
        public String getMotd()                     { return motd; }
        public String getJoinMessage()              { return joinMessage; }
        public String getWinMessage()               { return winMessage; }
        public Set<UUID> getParticipants()          { return Collections.unmodifiableSet(participants); }
        public Set<UUID> getSpectators()            { return Collections.unmodifiableSet(spectators); }
        public Set<UUID> getBanned()                { return Collections.unmodifiableSet(banned); }
        public Map<UUID, Integer> getScores()       { return Collections.unmodifiableMap(scores); }
        public Map<UUID, Integer> getLives()        { return Collections.unmodifiableMap(lives); }
        public Set<String> getAllowedKits()         { return Collections.unmodifiableSet(allowedKits); }

        // Internal mutators used by EventManager
        void addAllowedKit(String kit)                  { allowedKits.add(kit); }
        void removeAllowedKit(String kit)               { allowedKits.remove(kit); }
        void setAllowedKitsInternal(List<String> kits)  { allowedKits.clear(); kits.forEach(k -> allowedKits.add(k.trim().toLowerCase())); }
        void setState(EventState s)                { this.state = s; }
        void setFormat(EventFormat f)              { this.format = f; }
        void setKitName(String k)                  { this.kitName = k; }
        void setArenaName(String a)                { this.arenaName = a; }
        void setSpawnLocation(Location l)          { this.spawnLocation = l; }
        void setMaxPlayers(int n)                  { this.maxPlayers = n; }
        void setTeamSize(int n)                    { this.teamSize = n; }
        void setDefaultLives(int n)                { this.defaultLives = n; }
        void setRound(int n)                       { this.round = n; }
        void setRemainingSeconds(int s)            { this.remainingSeconds = s; }
        void setMotd(String m)                     { this.motd = m; }
        void setJoinMessage(String m)              { this.joinMessage = m; }
        void setWinMessage(String m)               { this.winMessage = m; }
        void setTimerTask(BukkitTask t)            { this.timerTask = t; }
        BukkitTask getTimerTask()                   { return timerTask; }
    }

    // ── Manager fields ───────────────────────────────────────────────────────
    private final DuellPlugin plugin;
    /** All events, keyed by lower-case name. */
    private final Map<String, GameEvent> events = new LinkedHashMap<>();
    /** The event currently selected/active for admin commands. */
    private GameEvent currentEvent;

    public EventManager(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Event CRUD ───────────────────────────────────────────────────────────
    public GameEvent createEvent(String name) {
        GameEvent ev = new GameEvent(name.toLowerCase());
        events.put(name.toLowerCase(), ev);
        currentEvent = ev;
        return ev;
    }

    public boolean deleteEvent(String name) {
        GameEvent ev = events.remove(name.toLowerCase());
        if (ev == null) return false;
        if (currentEvent == ev) currentEvent = null;
        stopEvent(ev);
        return true;
    }

    public GameEvent getEvent(String name) {
        return events.get(name.toLowerCase());
    }

    public Collection<GameEvent> getAllEvents() {
        return events.values();
    }

    public GameEvent getCurrentEvent() {
        return currentEvent;
    }

    public void setCurrentEvent(GameEvent ev) {
        this.currentEvent = ev;
    }

    // ── Participation ────────────────────────────────────────────────────────
    /**
     * Adds a player to the event. Returns false if banned, full, or already in it.
     */
    public boolean joinEvent(Player player, GameEvent ev) {
        UUID uid = player.getUniqueId();
        if (ev.banned.contains(uid)) {
            player.sendMessage(plugin.getPrefix() + "§cDu bist von diesem Event gebannt!");
            return false;
        }
        if (ev.participants.size() >= ev.maxPlayers) {
            player.sendMessage(plugin.getPrefix() + "§cDas Event ist voll! (max. " + ev.maxPlayers + " Spieler)");
            return false;
        }
        if (ev.participants.contains(uid)) {
            player.sendMessage(plugin.getPrefix() + "§cDu bist bereits im Event!");
            return false;
        }
        ev.participants.add(uid);
        ev.spectators.remove(uid);
        ev.scores.put(uid, 0);
        ev.lives.put(uid, ev.defaultLives);

        String msg = plugin.getPrefix() + ev.joinMessage.replace("%player%", player.getName());
        broadcastToEvent(ev, msg);

        if (ev.spawnLocation != null) player.teleport(ev.spawnLocation);
        if (!ev.motd.isEmpty()) player.sendMessage(plugin.getPrefix() + ev.motd);
        return true;
    }

    public boolean leaveEvent(Player player, GameEvent ev) {
        boolean removed = ev.participants.remove(player.getUniqueId());
        ev.spectators.remove(player.getUniqueId());
        if (removed) {
            broadcastToEvent(ev, plugin.getPrefix() + "§c" + player.getName() + " §7hat das Event verlassen.");
        }
        return removed;
    }

    public void addSpectator(Player player, GameEvent ev) {
        ev.spectators.add(player.getUniqueId());
        ev.participants.remove(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(plugin.getPrefix() + "§7Du schaust jetzt als Zuschauer zu.");
        if (ev.spawnLocation != null) player.teleport(ev.spawnLocation);
    }

    public void removeSpectator(Player player, GameEvent ev) {
        ev.spectators.remove(player.getUniqueId());
        player.setGameMode(GameMode.SURVIVAL);
        player.sendMessage(plugin.getPrefix() + "§aDu bist kein Zuschauer mehr.");
    }

    public void kickPlayer(Player player, GameEvent ev) {
        ev.participants.remove(player.getUniqueId());
        ev.spectators.remove(player.getUniqueId());
        player.sendMessage(plugin.getPrefix() + "§cDu wurdest aus dem Event gekickt!");
        plugin.getLobbyManager().sendToLobby(player);
    }

    public void banPlayer(Player player, GameEvent ev) {
        ev.banned.add(player.getUniqueId());
        kickPlayer(player, ev);
    }

    public void unbanPlayer(UUID uid, GameEvent ev) {
        ev.banned.remove(uid);
    }

    // ── Start / Stop ─────────────────────────────────────────────────────────
    public void startEvent(GameEvent ev) {
        ev.setState(EventState.ACTIVE);
        broadcastToEvent(ev, plugin.getPrefix() + "§aEvent §6" + ev.getName() + " §astartet!");
        applyKitToAll(ev);
    }

    public void stopEvent(GameEvent ev) {
        if (ev == null) return;
        ev.setState(EventState.ENDED);
        cancelTimer(ev);
        for (UUID uid : ev.getParticipants()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) plugin.getLobbyManager().sendToLobby(p);
        }
        for (UUID uid : ev.getSpectators()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) plugin.getLobbyManager().sendToLobby(p);
        }
        broadcastToAll(plugin.getPrefix() + "§cEvent §6" + ev.getName() + " §cwurde beendet.");
    }

    public void pauseEvent(GameEvent ev) {
        ev.setState(EventState.PAUSED);
        broadcastToEvent(ev, plugin.getPrefix() + "§eEvent §6" + ev.getName() + " §ewurde pausiert.");
    }

    public void resumeEvent(GameEvent ev) {
        ev.setState(EventState.ACTIVE);
        broadcastToEvent(ev, plugin.getPrefix() + "§aEvent §6" + ev.getName() + " §afortgesetzt.");
    }

    public void startCountdown(GameEvent ev, int seconds) {
        ev.setState(EventState.COUNTDOWN);
        ev.setRemainingSeconds(seconds);
        cancelTimer(ev);
        BukkitTask task = new BukkitRunnable() {
            int remaining = seconds;
            @Override
            public void run() {
                if (ev.getState() != EventState.COUNTDOWN) { cancel(); return; }
                if (remaining <= 0) {
                    startEvent(ev);
                    cancel();
                    return;
                }
                if (remaining <= 5 || remaining % 10 == 0) {
                    broadcastToEvent(ev, plugin.getPrefix() + "§eEvent startet in §c" + remaining + " §eSekunden!");
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        ev.setTimerTask(task);
    }

    // ── Score / Lives ────────────────────────────────────────────────────────
    public void setScore(UUID uid, GameEvent ev, int score) {
        ev.scores.put(uid, score);
    }

    public void addScore(UUID uid, GameEvent ev, int delta) {
        ev.scores.merge(uid, delta, Integer::sum);
    }

    public void setPlayerLives(UUID uid, GameEvent ev, int lives) {
        ev.lives.put(uid, lives);
    }

    /** Returns top-N players sorted by score descending. */
    public List<Map.Entry<UUID, Integer>> getTopPlayers(GameEvent ev, int n) {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(ev.scores.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        return list.subList(0, Math.min(n, list.size()));
    }

    // ── Utility ──────────────────────────────────────────────────────────────
    public void broadcastToEvent(GameEvent ev, String message) {
        for (UUID uid : ev.participants) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) p.sendMessage(message);
        }
        for (UUID uid : ev.spectators) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) p.sendMessage(message);
        }
    }

    public void broadcastToAll(String message) {
        Bukkit.broadcastMessage(message);
    }

    public void tpAll(GameEvent ev) {
        if (ev.spawnLocation == null) return;
        for (UUID uid : ev.participants) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) p.teleport(ev.spawnLocation);
        }
    }

    public void healAll(GameEvent ev) {
        for (UUID uid : ev.participants) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null) continue;
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.setSaturation(20.0f);
        }
    }

    public void feedAll(GameEvent ev) {
        for (UUID uid : ev.participants) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null) continue;
            p.setFoodLevel(20);
            p.setSaturation(20.0f);
        }
    }

    public void applyKitToAll(GameEvent ev) {
        for (UUID uid : ev.participants) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null) continue;
            var kit = plugin.getDuellManager().resolveKit(uid, ev.kitName);
            plugin.getDuellManager().applyKit(p, kit);
        }
    }

    public void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
    }

    public void giveItem(Player player, Material mat, int amount) {
        player.getInventory().addItem(new ItemStack(mat, amount));
    }

    public void setWinner(Player winner, GameEvent ev) {
        String msg = plugin.getPrefix() + ev.winMessage.replace("%player%", winner.getName());
        broadcastToAll(msg);
        ev.setState(EventState.ENDED);
    }

    public boolean isParticipant(UUID uid, GameEvent ev) {
        return ev.participants.contains(uid);
    }

    public boolean isSpectator(UUID uid, GameEvent ev) {
        return ev.spectators.contains(uid);
    }

    public void addAllowedKit(GameEvent ev, String kit) { ev.addAllowedKit(kit); }
    public void removeAllowedKit(GameEvent ev, String kit) { ev.removeAllowedKit(kit); }
    public void setAllowedKits(GameEvent ev, List<String> kits) { ev.setAllowedKitsInternal(kits); }

    /** Returns the GameEvent the player is currently participating in, or null. */
    public GameEvent getEventForPlayer(UUID uid) {
        for (GameEvent ev : events.values()) {
            if (ev.participants.contains(uid) || ev.spectators.contains(uid)) return ev;
        }
        return null;
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private void cancelTimer(GameEvent ev) {
        BukkitTask t = ev.getTimerTask();
        if (t != null) { try { t.cancel(); } catch (Exception ignored) {} ev.setTimerTask(null); }
    }
}
