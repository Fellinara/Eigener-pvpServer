package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Arena;
import de.duellplugin.models.Duel;
import de.duellplugin.models.Kit;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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

    public DuellManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.pendingRequests = new HashMap<>();
        this.activeDuels = new HashMap<>();
        this.requestTimestamps = new HashMap<>();
        this.requestTimeout = plugin.getConfig().getInt("duel.request-timeout", 30);
        this.countdown = plugin.getConfig().getInt("duel.countdown", 5);
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
            accepter.sendMessage("§cKeine Arena verfügbar! Bitte warte einen Moment.");
            challenger.sendMessage("§cKeine Arena verfügbar! Bitte warte einen Moment.");
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
        Duel duel = new Duel(player1.getUniqueId(), player2.getUniqueId(), arena.getName(), kitName);
        arena.setInUse(true);
        duel.setState(Duel.DuelState.COUNTDOWN);

        activeDuels.put(player1.getUniqueId(), duel);
        activeDuels.put(player2.getUniqueId(), duel);

        player1.teleport(arena.getSpawn1());
        player2.teleport(arena.getSpawn2());

        Kit kit = plugin.getKitManager().getKit(kitName);
        applyKit(player1, kit);
        applyKit(player2, kit);

        String prefix = plugin.getPrefix();

        new BukkitRunnable() {
            int count = countdown;

            @Override
            public void run() {
                if (count <= 0) {
                    duel.setState(Duel.DuelState.ACTIVE);
                    String startMsg = plugin.getMsg("duel-start", "&aKAMPF!");
                    player1.sendMessage(prefix + startMsg);
                    player2.sendMessage(prefix + startMsg);
                    player1.playSound(player1.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    player2.playSound(player2.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    cancel();
                    return;
                }

                String color = count <= 3 ? "§c" : "§e";
                player1.sendTitle(color + count, "§7Bereite dich vor!", 0, 25, 5);
                player2.sendTitle(color + count, "§7Bereite dich vor!", 0, 25, 5);
                player1.playSound(player1.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                player2.playSound(player2.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
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

        activeDuels.remove(winner);
        activeDuels.remove(loser);

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
                if (winnerPlayer != null && winnerPlayer.isOnline()) {
                    plugin.getLobbyManager().sendToLobby(winnerPlayer);
                }
                if (loserPlayer != null && loserPlayer.isOnline()) {
                    plugin.getLobbyManager().sendToLobby(loserPlayer);
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
