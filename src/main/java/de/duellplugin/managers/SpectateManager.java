package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Arena;
import de.duellplugin.models.Duel;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpectateManager {

    private final DuellPlugin plugin;
    /** spectator UUID → arena name they are watching */
    private final Map<UUID, String> spectatorArenas;

    public SpectateManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.spectatorArenas = new HashMap<>();
    }

    /**
     * Puts the spectator into SPECTATOR mode and teleports them to watch the target player's duel.
     * Returns false (with an error message to the spectator) if the target is not in a duel.
     */
    public boolean startSpectating(Player spectator, UUID targetUUID) {
        Duel duel = plugin.getDuellManager().getDuel(targetUUID);
        if (duel == null) {
            spectator.sendMessage(plugin.getPrefix() + "§cDieser Spieler ist aktuell in keinem Duell!");
            return false;
        }

        String arenaName = duel.getArenaName();
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null || arena.getSpawn1() == null) {
            spectator.sendMessage(plugin.getPrefix() + "§cArena nicht gefunden!");
            return false;
        }

        // Stop any existing spectating session first
        if (isSpectating(spectator.getUniqueId())) {
            stopSpectating(spectator, false);
        }

        spectatorArenas.put(spectator.getUniqueId(), arenaName);
        spectator.setGameMode(GameMode.SPECTATOR);
        // Teleport between the two spawns (rough center of the arena)
        spectator.teleport(arena.getSpawn1());
        spectator.sendMessage(plugin.getPrefix() + "§aDu schaust jetzt dem Duell in §6" + arenaName + " §azu.");
        spectator.sendMessage(plugin.getPrefix() + "§7Benutze §e/unspectate §7oder §e/spectate §7um das Zuschauen zu beenden.");
        return true;
    }

    /** Stops spectating and sends the player back to lobby. */
    public void stopSpectating(Player spectator, boolean sendToLobby) {
        spectatorArenas.remove(spectator.getUniqueId());
        if (sendToLobby) {
            plugin.getLobbyManager().sendToLobby(spectator);
        }
        spectator.sendMessage(plugin.getPrefix() + "§aDu bist nicht mehr im Zuschauer-Modus.");
    }

    public boolean isSpectating(UUID uuid) {
        return spectatorArenas.containsKey(uuid);
    }

    /** Returns the arena name being spectated by the given player, or null. */
    public String getArenaName(UUID uuid) {
        return spectatorArenas.get(uuid);
    }

    /** Called when a duel ends – automatically removes all spectators from that arena. */
    public void onDuelEnd(String arenaName) {
        if (arenaName == null) return;
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, String> e : spectatorArenas.entrySet()) {
            if (arenaName.equalsIgnoreCase(e.getValue())) {
                toRemove.add(e.getKey());
            }
        }
        for (UUID uid : toRemove) {
            spectatorArenas.remove(uid);
            Player p = plugin.getServer().getPlayer(uid);
            if (p != null && p.isOnline()) {
                plugin.getLobbyManager().sendToLobby(p);
                p.sendMessage(plugin.getPrefix() + "§eDas Duell ist beendet – du wurdest zur Lobby gebracht.");
            }
        }
    }

    /** Called on player disconnect – cleans up spectator state. */
    public void handleDisconnect(UUID uuid) {
        spectatorArenas.remove(uuid);
    }
}
