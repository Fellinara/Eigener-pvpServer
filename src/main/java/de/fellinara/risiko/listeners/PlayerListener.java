package de.fellinara.risiko.listeners;

import de.fellinara.risiko.RisikoPlugin;
import de.fellinara.risiko.models.Kingdom;
import de.fellinara.risiko.models.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Lauscht auf Spieler-Events und verwaltet Herzen, Kampf-Tags und Logout-Dummies.
 */
public class PlayerListener implements Listener {

    private final RisikoPlugin plugin;

    // Orange für Herz-Farbe
    private static final TextColor HEART_ORANGE = TextColor.color(0xFF6600);

    public PlayerListener(RisikoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Wenn ein Spieler beitritt: Daten laden / erstellen, Action Bar initialisieren.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().getOrCreate(player.getUniqueId(), player.getName());

        // Herzen auf Maximum setzen falls 0 und nicht gebannt (berücksichtigt König-Status)
        if (data.getHearts() <= 0 && !data.isBanned()) {
            plugin.getHeartManager().resetHearts(data);
            plugin.getDataManager().save(player.getUniqueId());
        }

        // Action Bar sofort aktualisieren
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getHeartManager().updateActionBar(player);
            }
        }, 5L);

        // Willkommensnachricht mit Königreich-Info
        if (data.getKingdom() != null) {
            TextColor kingdomColor = data.getKingdom() == Kingdom.FICHTEN
                    ? TextColor.color(0xFFAA00)
                    : TextColor.color(0x00AA00);
            player.sendMessage(Component.text("Willkommen zurück! Du gehörst zum ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(data.getKingdom().getDisplayName()).color(kingdomColor))
                    .append(Component.text(". Herzen: ").color(NamedTextColor.YELLOW))
                    .append(Component.text("❤ " + data.getHearts()).color(HEART_ORANGE))
            );
        }
    }

    /**
     * Wenn ein Spieler das Spiel verlässt: Kampf-Logout prüfen.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getCombatManager().handleLogout(player);
    }

    /**
     * Wenn ein Spieler stirbt: Kampf-Tag entfernen, Respawn ermöglichen.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.getCombatManager().untag(player.getUniqueId());

        // Nachricht unterdrücken (eigene Nachricht)
        event.deathMessage(null);

        // Killer ermitteln
        Player killer = player.getKiller();
        if (killer != null) {
            // Kampf-Tag für Killer entfernen
            plugin.getCombatManager().untag(killer.getUniqueId());
        }
    }

    /**
     * Wenn ein Spieler respawnt: Action Bar aktualisieren.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getHeartManager().updateActionBar(player);
            }
        }, 5L);
    }

    /**
     * Kampf-Tag bei Schaden zwischen Spielern setzen.
     * Herz-Verlust bei Spieler-Kills.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // ArmorStand-Logout-Dummy prüfen
        if (event.getEntity() instanceof ArmorStand stand) {
            if (plugin.getCombatManager().isLogoutStand(stand.getUniqueId())) {
                event.setCancelled(true); // Normalen Schaden abbrechen

                if (event.getDamager() instanceof Player attacker) {
                    plugin.getCombatManager().handleStandAttack(stand.getUniqueId(), attacker);
                }
                return;
            }
        }

        // Spieler-zu-Spieler Kampf-Tag
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        // Beide Spieler taggen
        plugin.getCombatManager().tag(victim.getUniqueId());
        plugin.getCombatManager().tag(attacker.getUniqueId());

        // Prüfen ob das letzte Herz des Königs geschützt ist
        PlayerData victimData = plugin.getDataManager().get(victim.getUniqueId());
        PlayerData attackerData = plugin.getDataManager().get(attacker.getUniqueId());

        if (victimData != null && victimData.isKing() && victimData.getHearts() == 1) {
            if (attackerData == null || !attackerData.isKing()) {
                // Tödlichen Angriff vollständig blockieren:
                // Das letzte König-Herz kann nur von einem anderen König genommen werden
                if (event.getFinalDamage() >= victim.getHealth()) {
                    event.setCancelled(true);
                    attacker.sendMessage(Component.text("❌ Du kannst das letzte Herz des Königs nicht nehmen! Nur ein anderer König kann das!")
                            .color(NamedTextColor.RED));
                }
            }
        }
    }

    /**
     * Wird nach einem Spieler-Kill ausgeführt (durch den PlayerDeathEvent mit Killer).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKilledByPlayer(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return; // Nicht von einem Spieler getötet

        PlayerData victimData = plugin.getDataManager().get(victim.getUniqueId());
        PlayerData killerData = plugin.getDataManager().get(killer.getUniqueId());

        if (victimData == null) return;

        // Herzen vor der Entfernung merken, um zu prüfen ob tatsächlich ein Herz entfernt wurde
        int heartsBefore = victimData.getHearts();

        // Herz entfernen
        boolean outOfHearts = plugin.getHeartManager().removeHeartOnKill(victimData, killerData);
        boolean heartRemoved = victimData.getHearts() < heartsBefore;

        // König-Schutz: Herz wurde NICHT entfernt (letztes König-Herz durch Nicht-König)
        if (!heartRemoved) {
            killer.sendMessage(Component.text("⚔ Du hast " + victim.getName() + " getötet, aber das letzte Königs-Herz ist geschützt! Nur ein anderer König kann es nehmen.")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        plugin.getDataManager().save(victim.getUniqueId());

        // Kill-Nachricht senden
        Component killMessage = buildKillMessage(victim, killer, victimData, killerData);
        Bukkit.broadcast(killMessage);

        // Killer-Action-Bar immer aktualisieren
        plugin.getHeartManager().updateActionBar(killer);

        if (outOfHearts) {
            // Spieler eliminiert - auf nächsten Tick verschieben um Event-Handler zu vermeiden
            final String victimName = victim.getName();
            final UUID victimUuid = victim.getUniqueId();
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getGameManager().handlePlayerEliminated(victimUuid, victimName));
        }
    }

    /**
     * Erstellt eine schöne Kill-Nachricht.
     */
    private Component buildKillMessage(Player victim, Player killer, PlayerData victimData, PlayerData killerData) {
        TextColor killerColor = getKingdomColor(killerData);
        TextColor victimColor = getKingdomColor(victimData);

        String killerPrefix = killerData != null && killerData.isKing() ? "♔ " : "";
        String victimPrefix = victimData != null && victimData.isKing() ? "♔ " : "";

        Component msg = Component.text("☠ ")
                .color(NamedTextColor.DARK_RED)
                .append(Component.text(killerPrefix + killer.getName()).color(killerColor).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" hat ").color(NamedTextColor.GRAY))
                .append(Component.text(victimPrefix + victim.getName()).color(victimColor).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" getötet! ").color(NamedTextColor.GRAY));

        if (victimData != null) {
            msg = msg.append(Component.text("(" + victim.getName() + " hat noch " + victimData.getHearts() + " ❤)").color(HEART_ORANGE));
        }

        return msg;
    }

    private TextColor getKingdomColor(PlayerData data) {
        if (data == null || data.getKingdom() == null) return NamedTextColor.WHITE;
        return data.getKingdom() == Kingdom.FICHTEN
                ? TextColor.color(0xFFAA00)
                : TextColor.color(0x00AA00);
    }
}
