package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Arena;
import de.duellplugin.models.Party;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyManager {

    private final DuellPlugin plugin;
    /** Leader UUID → Party */
    private final Map<UUID, Party> parties;
    /** Member UUID → leader UUID (for quick lookup) */
    private final Map<UUID, UUID> memberPartyMap;

    public PartyManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.parties = new HashMap<>();
        this.memberPartyMap = new HashMap<>();
    }

    // ── Query ────────────────────────────────────────

    /** Returns the party the given player belongs to, or null. */
    public Party getParty(UUID uuid) {
        UUID leader = memberPartyMap.get(uuid);
        if (leader == null) return null;
        return parties.get(leader);
    }

    /** Returns the party led by the given UUID, or null. */
    public Party getPartyByLeader(UUID leader) {
        return parties.get(leader);
    }

    public boolean isInParty(UUID uuid) {
        return memberPartyMap.containsKey(uuid);
    }

    public boolean isLeader(UUID uuid) {
        return parties.containsKey(uuid);
    }

    // ── Commands ─────────────────────────────────────

    /** Creates a new party for the player. Fails if already in a party. */
    public boolean createParty(Player leader) {
        if (isInParty(leader.getUniqueId())) {
            leader.sendMessage(plugin.getPrefix() + "§cDu bist bereits in einer Party!");
            return false;
        }
        Party party = new Party(leader.getUniqueId());
        parties.put(leader.getUniqueId(), party);
        memberPartyMap.put(leader.getUniqueId(), leader.getUniqueId());
        leader.sendMessage(plugin.getPrefix() + "§aParty erstellt! Lade Spieler mit §e/party invite <name> §aein.");
        return true;
    }

    /** Invites a player to the leader's party. */
    public boolean invitePlayer(Player leader, Player target) {
        if (!isLeader(leader.getUniqueId())) {
            leader.sendMessage(plugin.getPrefix() + "§cDu bist kein Party-Leader!");
            return false;
        }
        Party party = parties.get(leader.getUniqueId());
        // Determine max party size based on the leader's rank
        var leaderStats = plugin.getStatsManager().getStats(leader.getUniqueId());
        int maxSize = Party.getMaxSizeForRank(leaderStats != null ? leaderStats.getRank() : null);
        if (party.size() >= maxSize) {
            leader.sendMessage(plugin.getPrefix() + "§cDeine Party ist voll! (Max " + maxSize + " Spieler)");
            return false;
        }
        if (party.isMember(target.getUniqueId())) {
            leader.sendMessage(plugin.getPrefix() + "§c" + target.getName() + " ist bereits in deiner Party!");
            return false;
        }
        party.invite(target.getUniqueId());
        leader.sendMessage(plugin.getPrefix() + "§aEinladung an §6" + target.getName() + " §agesendet.");
        target.sendMessage(plugin.getPrefix() + "§6" + leader.getName() + " §einvites you to their party! "
                + "§a/party accept §7um beizutreten, §c/party ablehnen §7zum Ablehnen.");
        return true;
    }

    /** Accepts an invite to a party. */
    public boolean acceptInvite(Player player) {
        // Find any party with a pending invite for this player
        for (Party party : parties.values()) {
            if (party.hasInvite(player.getUniqueId())) {
                if (isInParty(player.getUniqueId())) {
                    player.sendMessage(plugin.getPrefix() + "§cDu bist bereits in einer Party!");
                    return false;
                }
                party.acceptInvite(player.getUniqueId());
                memberPartyMap.put(player.getUniqueId(), party.getLeader());

                Player leader = Bukkit.getPlayer(party.getLeader());
                if (leader != null) {
                    leader.sendMessage(plugin.getPrefix() + "§6" + player.getName() + " §aist deiner Party beigetreten!");
                }
                // Notify all party members
                broadcastToParty(party, plugin.getPrefix() + "§6" + player.getName() + " §aist der Party beigetreten! §7(" + party.size() + "/" + Party.MAX_SIZE + ")");
                return true;
            }
        }
        player.sendMessage(plugin.getPrefix() + "§cDu hast keine offene Party-Einladung!");
        return false;
    }

    /** Declines a pending party invite. */
    public boolean declineInvite(Player player) {
        for (Party party : parties.values()) {
            if (party.hasInvite(player.getUniqueId())) {
                party.cancelInvite(player.getUniqueId());
                Player leader = Bukkit.getPlayer(party.getLeader());
                if (leader != null) {
                    leader.sendMessage(plugin.getPrefix() + "§6" + player.getName() + " §chat deine Einladung abgelehnt.");
                }
                player.sendMessage(plugin.getPrefix() + "§cEinladung abgelehnt.");
                return true;
            }
        }
        player.sendMessage(plugin.getPrefix() + "§cDu hast keine offene Einladung.");
        return false;
    }

    /** Removes a player from their current party. */
    public boolean leaveParty(Player player) {
        Party party = getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getPrefix() + "§cDu bist in keiner Party!");
            return false;
        }
        if (party.getLeader().equals(player.getUniqueId())) {
            disbandParty(player);
            return true;
        }
        party.removeMember(player.getUniqueId());
        memberPartyMap.remove(player.getUniqueId());
        player.sendMessage(plugin.getPrefix() + "§cDu hast die Party verlassen.");
        broadcastToParty(party, plugin.getPrefix() + "§6" + player.getName() + " §chat die Party verlassen.");
        return true;
    }

    /** Leader kicks a member from the party. */
    public boolean kickMember(Player leader, Player target) {
        if (!isLeader(leader.getUniqueId())) {
            leader.sendMessage(plugin.getPrefix() + "§cDu bist kein Party-Leader!");
            return false;
        }
        Party party = parties.get(leader.getUniqueId());
        if (!party.isMember(target.getUniqueId()) || target.getUniqueId().equals(leader.getUniqueId())) {
            leader.sendMessage(plugin.getPrefix() + "§cDieser Spieler ist nicht in deiner Party!");
            return false;
        }
        party.removeMember(target.getUniqueId());
        memberPartyMap.remove(target.getUniqueId());
        target.sendMessage(plugin.getPrefix() + "§cDu wurdest aus der Party geworfen.");
        broadcastToParty(party, plugin.getPrefix() + "§6" + target.getName() + " §cwurde aus der Party geworfen.");
        return true;
    }

    /** Disbands the entire party (called when leader leaves). */
    public void disbandParty(Player leader) {
        Party party = parties.remove(leader.getUniqueId());
        if (party == null) return;
        for (UUID uid : party.getMembers()) {
            memberPartyMap.remove(uid);
            Player p = Bukkit.getPlayer(uid);
            if (p != null && !p.getUniqueId().equals(leader.getUniqueId())) {
                p.sendMessage(plugin.getPrefix() + "§cDie Party wurde vom Leader aufgelöst.");
            }
        }
        leader.sendMessage(plugin.getPrefix() + "§cParty aufgelöst.");
    }

    /** Removes a disconnected player from their party. */
    public void handleDisconnect(UUID uuid) {
        Party party = getParty(uuid);
        if (party == null) return;
        if (party.getLeader().equals(uuid)) {
            // Disband silently
            for (UUID uid : party.getMembers()) {
                memberPartyMap.remove(uid);
                Player p = Bukkit.getPlayer(uid);
                if (p != null && !p.getUniqueId().equals(uuid)) {
                    p.sendMessage(plugin.getPrefix() + "§cDer Party-Leader hat die Verbindung getrennt. Party aufgelöst.");
                }
            }
            parties.remove(uuid);
        } else {
            party.removeMember(uuid);
            memberPartyMap.remove(uuid);
            broadcastToParty(party, plugin.getPrefix() + "§7Ein Partymitglied hat die Verbindung getrennt und wurde entfernt.");
        }
    }

    /**
     * Starts a party-vs-party duel.
     * The challenger party's leader challenges the target party's leader.
     */
    public boolean startPartyDuel(Player challengerLeader, Player targetLeader, String kitName) {
        Party challenger = getPartyByLeader(challengerLeader.getUniqueId());
        Party target = getPartyByLeader(targetLeader.getUniqueId());

        if (challenger == null) {
            challengerLeader.sendMessage(plugin.getPrefix() + "§cDu hast keine Party! Erstelle eine mit §e/party create§c.");
            return false;
        }
        if (target == null) {
            challengerLeader.sendMessage(plugin.getPrefix() + "§c" + targetLeader.getName() + " hat keine Party!");
            return false;
        }
        if (challenger.getLeader().equals(target.getLeader())) {
            challengerLeader.sendMessage(plugin.getPrefix() + "§cDu kannst deine eigene Party nicht herausfordern!");
            return false;
        }

        Arena arena = plugin.getArenaManager().getAvailableArena();
        List<UUID> team1 = new ArrayList<>(challenger.getMembers());
        List<UUID> team2 = new ArrayList<>(target.getMembers());

        if (arena == null) {
            broadcastToParty(challenger, plugin.getPrefix() + "§eKeine Arena frei – Party-Duell in die Warteschlange eingereiht...");
            broadcastToParty(target, plugin.getPrefix() + "§eKeine Arena frei – Party-Duell in die Warteschlange eingereiht...");
            plugin.getDuellManager().queueDuel(team1, team2, kitName);
            return true;
        }

        broadcastToParty(challenger, plugin.getPrefix() + "§aParty-Duell gegen §6" + targetLeader.getName() + "§a's Team beginnt!");
        broadcastToParty(target, plugin.getPrefix() + "§aParty-Duell gegen §6" + challengerLeader.getName() + "§a's Team beginnt!");

        plugin.getDuellManager().startTeamDuel(team1, team2, arena, kitName);
        return true;
    }

    /** Broadcasts a message to all online party members. */
    public void broadcastToParty(Party party, String message) {
        for (UUID uid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) p.sendMessage(message);
        }
    }
}
