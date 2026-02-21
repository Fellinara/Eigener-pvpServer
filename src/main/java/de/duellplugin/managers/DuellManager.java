package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Arena;
import de.duellplugin.models.Duel;
import de.duellplugin.models.Kit;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DuellManager {

    private final DuellPlugin plugin;
    private final Map<UUID, UUID> pendingRequests;
    private final Map<UUID, Duel> activeDuels;
    private final Map<UUID, Long> requestTimestamps;
    private final int requestTimeout;
    private final int countdown;

    /** Queued duel entries waiting for an arena to become free. */
    private final Queue<QueueEntry> duelQueue;

    /** A queued duel pair waiting for an arena. */
    private static final class QueueEntry {
        final List<UUID> team1;
        final List<UUID> team2;
        final String kitName;

        QueueEntry(List<UUID> team1, List<UUID> team2, String kitName) {
            this.team1 = team1;
            this.team2 = team2;
            this.kitName = kitName;
        }
    }

    public DuellManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.pendingRequests = new HashMap<>();
        this.activeDuels = new HashMap<>();
        this.requestTimestamps = new HashMap<>();
        this.requestTimeout = plugin.getConfig().getInt("duel.request-timeout", 30);
        this.countdown = plugin.getConfig().getInt("duel.countdown", 5);
        this.duelQueue = new LinkedList<>();
    }

    public boolean sendRequest(Player sender, Player target) {
        if (isInDuel(sender.getUniqueId()) || isInDuel(target.getUniqueId())) {
            sender.sendMessage("§cEiner der Spieler ist bereits in einem Duell!");
            return false;
        }

        if (pendingRequests.containsValue(sender.getUniqueId())) {
            sender.sendMessage("§cDu hast bereits eine ausstehende Anfrage!");
            return false;
        }

        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage("§cDu kannst dich nicht selbst herausfordern!");
            return false;
        }

        pendingRequests.put(target.getUniqueId(), sender.getUniqueId());
        requestTimestamps.put(target.getUniqueId(), System.currentTimeMillis());

        String prefix = plugin.getPrefix();
        String requestMsg = plugin.getMsg("duel-request",
                "&6%player% &ehat dich zu einem Duell herausgefordert! &a/duell annehmen");
        target.sendMessage(prefix + requestMsg.replace("%player%", sender.getName()));
        sender.sendMessage(prefix + "§aDu hast §6" + target.getName() + " §aherausgefordert!");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRequests.containsKey(target.getUniqueId())
                        && pendingRequests.get(target.getUniqueId()).equals(sender.getUniqueId())) {
                    pendingRequests.remove(target.getUniqueId());
                    requestTimestamps.remove(target.getUniqueId());
                    if (sender.isOnline()) {
                        sender.sendMessage(prefix + "§cDie Duellanfrage an §6" + target.getName() + " §cist abgelaufen.");
                    }
                    if (target.isOnline()) {
                        target.sendMessage(prefix + "§cDie Duellanfrage von §6" + sender.getName() + " §cist abgelaufen.");
                    }
                }
            }
        }.runTaskLater(plugin, requestTimeout * 20L);

        return true;
    }

    public boolean acceptRequest(Player accepter) {
        UUID challengerUUID = pendingRequests.remove(accepter.getUniqueId());
        requestTimestamps.remove(accepter.getUniqueId());

        if (challengerUUID == null) {
            accepter.sendMessage("§cDu hast keine offene Duellanfrage!");
            return false;
        }

        Player challenger = Bukkit.getPlayer(challengerUUID);
        if (challenger == null || !challenger.isOnline()) {
            accepter.sendMessage("§cDer Herausforderer ist nicht mehr online!");
            return false;
        }

        Arena arena = plugin.getArenaManager().getAvailableArena();
        if (arena == null) {
            String prefix = plugin.getPrefix();
            accepter.sendMessage(prefix + "§eKeine Arena frei – du wirst in die Warteschlange eingereiht!");
            challenger.sendMessage(prefix + "§eKeine Arena frei – Duell mit §6" + accepter.getName() + " §ewird in die Warteschlange eingereiht!");
            queueDuel(
                    Collections.singletonList(challenger.getUniqueId()),
                    Collections.singletonList(accepter.getUniqueId()),
                    kitName
            );
            return false;
        }

        String kitName = plugin.getStatsManager()
                .getOrCreateStats(accepter.getUniqueId(), accepter.getName()).getSelectedKit();

        startDuel(challenger, accepter, arena, kitName);
        return true;
    }

    public boolean declineRequest(Player decliner) {
        UUID challengerUUID = pendingRequests.remove(decliner.getUniqueId());
        requestTimestamps.remove(decliner.getUniqueId());

        if (challengerUUID == null) {
            decliner.sendMessage("§cDu hast keine offene Duellanfrage!");
            return false;
        }

        Player challenger = Bukkit.getPlayer(challengerUUID);
        String prefix = plugin.getPrefix();
        String declineMsg = plugin.getMsg("duel-declined", "&cDuell abgelehnt.");

        decliner.sendMessage(prefix + declineMsg);
        if (challenger != null && challenger.isOnline()) {
            challenger.sendMessage(prefix + "§6" + decliner.getName() + " §chat dein Duell abgelehnt.");
        }
        return true;
    }

    public void startDuel(Player player1, Player player2, Arena arena, String kitName) {
        startTeamDuel(
                Collections.singletonList(player1.getUniqueId()),
                Collections.singletonList(player2.getUniqueId()),
                arena,
                kitName
        );
    }

    /**
     * Starts a duel between two teams. Each team may have 1–4 members.
     * All team1 members teleport to spawn1; all team2 members teleport to spawn2.
     */
    public void startTeamDuel(List<UUID> team1UUIDs, List<UUID> team2UUIDs, Arena arena, String kitName) {
        Duel duel = new Duel(team1UUIDs, team2UUIDs, arena.getName(), kitName);
        arena.setInUse(true);
        duel.setState(Duel.DuelState.COUNTDOWN);

        // Register the duel for all participants
        for (UUID uid : duel.getAllMembers()) {
            activeDuels.put(uid, duel);
        }

        Kit kit = plugin.getKitManager().getKit(kitName);
        String prefix = plugin.getPrefix();

        // Teleport + kit for team 1
        Location spawn1 = arena.getSpawn1();
        for (int i = 0; i < team1UUIDs.size(); i++) {
            Player p = Bukkit.getPlayer(team1UUIDs.get(i));
            if (p == null || !p.isOnline()) continue;
            // Slight offset so players don't overlap
            Location tpLoc = spawn1.clone().add(i * TEAM_SPAWN_OFFSET, 0, 0);
            p.teleport(tpLoc);
            applyKit(p, kit);
        }

        // Teleport + kit for team 2
        Location spawn2 = arena.getSpawn2();
        for (int i = 0; i < team2UUIDs.size(); i++) {
            Player p = Bukkit.getPlayer(team2UUIDs.get(i));
            if (p == null || !p.isOnline()) continue;
            Location tpLoc = spawn2.clone().add(i * TEAM_SPAWN_OFFSET, 0, 0);
            p.teleport(tpLoc);
            applyKit(p, kit);
        }

        new BukkitRunnable() {
            int count = countdown;

            @Override
            public void run() {
                for (UUID uid : duel.getAllMembers()) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p == null || !p.isOnline()) continue;
                    if (count <= 0) {
                        duel.setState(Duel.DuelState.ACTIVE);
                        String startMsg = plugin.getMsg("duel-start", "&aKAMPF!");
                        p.sendMessage(prefix + startMsg);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    } else {
                        String color = count <= 3 ? "§c" : "§e";
                        p.sendTitle(color + count, "§7Bereite dich vor!", 0, 25, 5);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                }
                if (count <= 0) {
                    cancel();
                    return;
                }
                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Queues a team duel to start as soon as an arena becomes available.
     */
    public void queueDuel(List<UUID> team1, List<UUID> team2, String kitName) {
        duelQueue.add(new QueueEntry(team1, team2, kitName));
        plugin.getArenaManager().onNextArenaFree(this::checkQueue);
    }

    /** Starts the next queued duel if an arena is now available. */
    private void checkQueue() {
        if (duelQueue.isEmpty()) return;
        Arena arena = plugin.getArenaManager().getAvailableArena();
        if (arena == null) {
            // Re-register callback for next free arena
            plugin.getArenaManager().onNextArenaFree(this::checkQueue);
            return;
        }
        QueueEntry entry = duelQueue.poll();
        if (entry == null) return;

        String prefix = plugin.getPrefix();
        // Verify all players are still online; if not, skip
        if (!allPlayersOnline(entry.team1) || !allPlayersOnline(entry.team2)) {
            // Send message to whoever is online
            for (UUID uid : entry.team1) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) p.sendMessage(prefix + "§cEin Spieler des anderen Teams ist offline. Duell abgebrochen.");
            }
            for (UUID uid : entry.team2) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) p.sendMessage(prefix + "§cEin Spieler deines Teams ist offline. Duell abgebrochen.");
            }
            // Try next in queue
            checkQueue();
            return;
        }

        for (UUID uid : entry.team1) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) p.sendMessage(prefix + "§aEine Arena ist frei! Dein Duell beginnt...");
        }
        for (UUID uid : entry.team2) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) p.sendMessage(prefix + "§aEine Arena ist frei! Dein Duell beginnt...");
        }

        startTeamDuel(entry.team1, entry.team2, arena, entry.kitName);
    }

    public void endDuel(UUID winner, UUID loser) {
        Duel duel = activeDuels.get(winner);
        if (duel == null) return;

        duel.setState(Duel.DuelState.ENDED);

        Arena arena = plugin.getArenaManager().getArena(duel.getArenaName());
        if (arena != null) {
            arena.setInUse(false);
            plugin.getArenaManager().resetArena(duel.getArenaName());
        }

        // Remove all participants from activeDuels
        for (UUID uid : duel.getAllMembers()) {
            activeDuels.remove(uid);
        }

        Player winnerPlayer = Bukkit.getPlayer(winner);
        Player loserPlayer = Bukkit.getPlayer(loser);

        if (!duel.isBotDuel()) {
            plugin.getStatsManager().processWin(winner, loser);
        } else {
            plugin.getStatsManager().processBotWin(winner, duel.getBotLevel());
        }

        String prefix = plugin.getPrefix();
        String winMsg = plugin.getMsg("duel-win",
                "&6%winner% &ahat das Duell gegen &6%loser% &agewonnen!");

        String winnerName = winnerPlayer != null ? winnerPlayer.getName() : "Unbekannt";
        String loserName = loserPlayer != null ? loserPlayer.getName() : "Bot";

        String formattedMsg = prefix + winMsg
                .replace("%winner%", winnerName)
                .replace("%loser%", loserName);

        Bukkit.broadcastMessage(formattedMsg);

        new BukkitRunnable() {
            @Override
            public void run() {
                // Send all participants to lobby
                for (UUID uid : duel.getAllMembers()) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p != null && p.isOnline()) {
                        plugin.getLobbyManager().sendToLobby(p);
                    }
                }
            }
        }.runTaskLater(plugin, 60L);
    }

    public void handleDisconnect(UUID uuid) {
        Duel duel = activeDuels.get(uuid);
        if (duel == null) return;

        UUID opponent = duel.getOpponent(uuid);
        if (opponent != null) {
            endDuel(opponent, uuid);
        }
    }

    public boolean isInDuel(UUID uuid) {
        return activeDuels.containsKey(uuid);
    }

    public Duel getDuel(UUID uuid) {
        return activeDuels.get(uuid);
    }

    public boolean hasPendingRequest(UUID uuid) {
        return pendingRequests.containsKey(uuid);
    }

    /** Spacing between team members at the arena spawn point (blocks). */
    private static final double TEAM_SPAWN_OFFSET = 1.2;

    /** Returns true if every player in the list is online. */
    private static boolean allPlayersOnline(List<UUID> team) {
        for (UUID uid : team) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null || !p.isOnline()) return false;
        }
        return true;
    }

    private void applyKit(Player player, Kit kit) {
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));

        if (kit != null) {
            ItemStack[] contents = applySlotLayout(player, kit);
            player.getInventory().setStorageContents(contents);
            player.getInventory().setArmorContents(kit.getArmor());
        }
        // Always give a shield in the off-hand
        player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
    }

    /**
     * Returns the kit's contents rearranged according to the player's saved slot layout.
     * Falls back to the default layout if no custom layout is saved.
     */
    private ItemStack[] applySlotLayout(Player player, Kit kit) {
        var stats = plugin.getStatsManager().getStats(player.getUniqueId());
        if (stats == null) return kit.getContents();

        int[] layout = stats.getKitSlotLayout(kit.getName());
        if (layout == null) return kit.getContents();

        ItemStack[] defaultContents = kit.getContents();
        ItemStack[] result = new ItemStack[36];

        for (int src = 0; src < 36; src++) {
            if (defaultContents[src] == null) continue;
            int tgt = layout[src];
            if (tgt >= 0 && tgt < 36 && result[tgt] == null) {
                result[tgt] = defaultContents[src];
            } else {
                // Target slot occupied or invalid – find the first free slot
                boolean placed = false;
                for (int i = 0; i < 36; i++) {
                    if (result[i] == null) {
                        result[i] = defaultContents[src];
                        placed = true;
                        break;
                    }
                }
                // If no free slot at all, fall back to overwriting the original source slot
                if (!placed) result[src] = defaultContents[src];
            }
        }
        return result;
    }
}
