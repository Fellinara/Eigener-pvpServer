package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the vanish system.
 *
 * <p>When a player vanishes:
 * <ul>
 *   <li>A fake quit message is broadcast.</li>
 *   <li>The player is hidden from all other players.</li>
 *   <li>The player is removed from the TAB list.</li>
 *   <li>The player is set to SPECTATOR game-mode.</li>
 *   <li>Admins with {@code klassenplugin.vanish.see} can still see them
 *       (they are shown a dimmed name-tag).</li>
 * </ul>
 *
 * <p>Toggles back on a second call: fake join, re-show, restore game-mode.
 */
public class VanishManager {

    private final KlassenPlugin plugin;
    private final Set<UUID> vanished = new HashSet<>();
    /** Previous game-mode before vanishing (so we can restore it). */
    private final java.util.Map<UUID, GameMode> previousGameMode = new java.util.HashMap<>();

    public VanishManager(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    public Set<UUID> getVanished() {
        return Collections.unmodifiableSet(vanished);
    }

    /**
     * Toggles vanish for the given player.
     *
     * @return {@code true} if the player is now vanished, {@code false} if unvanished.
     */
    public boolean toggle(Player player) {
        if (vanished.contains(player.getUniqueId())) {
            unvanish(player);
            return false;
        } else {
            vanish(player);
            return true;
        }
    }

    private void vanish(Player player) {
        vanished.add(player.getUniqueId());

        // Save game-mode then switch to spectator.
        previousGameMode.put(player.getUniqueId(), player.getGameMode());
        player.setGameMode(GameMode.SPECTATOR);

        // Fake quit message.
        String quitMsg = plugin.getConfig().getString("quit-message.message",
                "&e{player} hat den Server verlassen.");
        Bukkit.broadcast(KlassenPlugin.colorizeComponent(quitMsg.replace("{player}", player.getName())));

        // Hide from all players.
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(player.getUniqueId())) continue;
            if (other.hasPermission("klassenplugin.vanish.see")) {
                other.sendMessage(KlassenPlugin.colorizeComponent(
                        "&8[&bVanish&8] &7" + player.getName() + " &7ist jetzt unsichtbar."));
            }
            other.hidePlayer(plugin, player);
        }

        // Remove from tab-list by overriding list name to empty component.
        player.playerListName(Component.empty());

        player.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bVanish&8] &aDu bist jetzt &eunsichtbar&a. Benutze &e/vanish &aum sichtbar zu werden."));
    }

    private void unvanish(Player player) {
        vanished.remove(player.getUniqueId());

        // Restore game-mode.
        GameMode prev = previousGameMode.remove(player.getUniqueId());
        if (prev != null) {
            player.setGameMode(prev);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
        }

        // Show to all players again.
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(player.getUniqueId())) continue;
            other.showPlayer(plugin, player);
        }

        // Restore tab-list name.
        plugin.getRankManager().updateTabListName(player);

        // Fake rejoin message (simple – player was already online before vanishing).
        String rejoinMsg = "&e{player} hat den Server betreten.";
        Bukkit.broadcast(KlassenPlugin.colorizeComponent(rejoinMsg.replace("{player}", player.getName())));

        player.sendMessage(KlassenPlugin.colorizeComponent(
                "&8[&bVanish&8] &cDu bist jetzt &esichtbar&c."));
    }

    /**
     * Called when a player joins: if they were previously vanished (e.g., server restart)
     * just ensure other players cannot see them. Also hides already-vanished players
     * from the newly-joining player.
     */
    public void applyVanishOnJoin(Player joining) {
        // Hide already-vanished players from the newcomer.
        for (UUID vid : vanished) {
            Player vanishedPlayer = Bukkit.getPlayer(vid);
            if (vanishedPlayer != null && !joining.hasPermission("klassenplugin.vanish.see")) {
                joining.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }

    /**
     * Cleans up state when a player leaves while vanished.
     */
    public void onQuit(Player player) {
        if (vanished.contains(player.getUniqueId())) {
            vanished.remove(player.getUniqueId());
            previousGameMode.remove(player.getUniqueId());
            // Re-show to everyone (they can't see the player anyway after disconnect,
            // but this keeps the data clean for the next join).
        }
    }
}
