package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Kit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FfaManager {

    private final DuellPlugin plugin;
    private final Set<UUID> ffaPlayers;
    private Location ffaSpawn;

    public FfaManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.ffaPlayers = new HashSet<>();
        loadFfaSpawn();
    }

    // ── Query ────────────────────────────────────────

    public boolean isInFfa(UUID uuid) {
        return ffaPlayers.contains(uuid);
    }

    public int getPlayerCount() {
        return ffaPlayers.size();
    }

    public Location getFfaSpawn() {
        return ffaSpawn;
    }

    public void setFfaSpawn(Location location) {
        this.ffaSpawn = location;
        saveFfaSpawn();
    }

    // ── Join / Leave ─────────────────────────────────

    public void joinFfa(Player player) {
        if (ffaSpawn == null) {
            player.sendMessage(plugin.getPrefix() + "§cFFA-Spawn ist nicht gesetzt! Nutze §f/ffa setspawn§c.");
            return;
        }
        if (plugin.getDuellManager().isInDuel(player.getUniqueId())
                || plugin.getBotManager().isInBotFight(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + "§cDu bist bereits in einem Kampf!");
            return;
        }
        if (isInFfa(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + "§cDu bist bereits im FFA!");
            return;
        }

        ffaPlayers.add(player.getUniqueId());
        applyFfaKit(player);
        player.teleport(ffaSpawn);
        player.sendMessage(plugin.getPrefix() + "§aWillkommen im FFA! Kämpfe gegen alle anderen Spieler.");
        Bukkit.broadcastMessage(plugin.getPrefix() + "§6" + player.getName() + " §eist dem FFA beigetreten! §7("
                + ffaPlayers.size() + " Spieler)");
    }

    public void leaveFfa(Player player) {
        if (!isInFfa(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + "§cDu bist nicht im FFA!");
            return;
        }
        ffaPlayers.remove(player.getUniqueId());
        plugin.getLobbyManager().sendToLobby(player);
        player.sendMessage(plugin.getPrefix() + "§cDu hast das FFA verlassen.");
    }

    // ── Death / Respawn ──────────────────────────────

    /**
     * Called when an FFA player dies. Removes them from the active set so the respawn
     * handler can cleanly add them back via {@link #respawnInFfa(Player)}.
     */
    public void handleFfaDeath(Player dead) {
        ffaPlayers.remove(dead.getUniqueId());
    }

    /**
     * Called after a dead FFA player has been respawned. Teleports them to the FFA spawn
     * and restores their kit.
     */
    public void respawnInFfa(Player player) {
        if (ffaSpawn == null) {
            plugin.getLobbyManager().sendToLobby(player);
            return;
        }
        ffaPlayers.add(player.getUniqueId());
        applyFfaKit(player);
        player.teleport(ffaSpawn);
    }

    /** Applies the player's selected kit (delegates to DuellManager). */
    private void applyFfaKit(Player player) {
        String kitName = plugin.getStatsManager()
                .getOrCreateStats(player.getUniqueId(), player.getName()).getSelectedKit();
        Kit kit = plugin.getKitManager().getKit(kitName);
        plugin.getDuellManager().applyKit(player, kit);
    }

    /** Removes a player from FFA when they disconnect. */
    public void handleDisconnect(UUID uuid) {
        ffaPlayers.remove(uuid);
    }

    // ── Persistence ──────────────────────────────────

    private void loadFfaSpawn() {
        var config = plugin.getConfig();
        if (!config.contains("ffa.spawn.world")) return;
        String worldName = config.getString("ffa.spawn.world");
        if (worldName == null) return;
        var world = Bukkit.getWorld(worldName);
        if (world == null) return;
        ffaSpawn = new Location(
                world,
                config.getDouble("ffa.spawn.x"),
                config.getDouble("ffa.spawn.y"),
                config.getDouble("ffa.spawn.z"),
                (float) config.getDouble("ffa.spawn.yaw"),
                (float) config.getDouble("ffa.spawn.pitch")
        );
    }

    private void saveFfaSpawn() {
        if (ffaSpawn == null || ffaSpawn.getWorld() == null) return;
        var config = plugin.getConfig();
        config.set("ffa.spawn.world", ffaSpawn.getWorld().getName());
        config.set("ffa.spawn.x", ffaSpawn.getX());
        config.set("ffa.spawn.y", ffaSpawn.getY());
        config.set("ffa.spawn.z", ffaSpawn.getZ());
        config.set("ffa.spawn.yaw", (double) ffaSpawn.getYaw());
        config.set("ffa.spawn.pitch", (double) ffaSpawn.getPitch());
        plugin.saveConfig();
    }
}
