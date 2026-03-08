package de.fellinara.risiko.managers;

import de.fellinara.risiko.RisikoPlugin;
import de.fellinara.risiko.models.Kingdom;
import de.fellinara.risiko.models.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

/**
 * Verwaltet das Risiko-Finale: Teleport, Weltgrenze und Sieg-Erkennung.
 */
public class GameManager {

    private final RisikoPlugin plugin;
    private boolean gameRunning = false;

    // Spawn-Positionen für das Finale
    private Location spawnFichten;
    private Location spawnDschungel;

    public GameManager(RisikoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Lädt die Spawn-Positionen aus der Konfiguration.
     */
    public void loadSpawns() {
        spawnFichten = getLocationFromConfig("spawn-fichten");
        spawnDschungel = getLocationFromConfig("spawn-dschungel");
    }

    private Location getLocationFromConfig(String path) {
        String worldName = plugin.getConfig().getString(path + ".world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        }
        if (world == null) {
            plugin.getLogger().warning("Welt '" + worldName + "' nicht gefunden für " + path);
            return null;
        }
        double x = plugin.getConfig().getDouble(path + ".x", 0);
        double y = plugin.getConfig().getDouble(path + ".y", 64);
        double z = plugin.getConfig().getDouble(path + ".z", 0);
        return new Location(world, x, y, z);
    }

    /**
     * Speichert einen Spawn-Punkt in der Konfiguration.
     */
    public void saveSpawn(Kingdom kingdom, Location location) {
        String path = kingdom == Kingdom.FICHTEN ? "spawn-fichten" : "spawn-dschungel";
        plugin.getConfig().set(path + ".world", location.getWorld().getName());
        plugin.getConfig().set(path + ".x", location.getX());
        plugin.getConfig().set(path + ".y", location.getY());
        plugin.getConfig().set(path + ".z", location.getZ());
        plugin.saveConfig();

        if (kingdom == Kingdom.FICHTEN) {
            spawnFichten = location.clone();
        } else {
            spawnDschungel = location.clone();
        }
    }

    /**
     * Startet das Finale.
     * Teleportiert alle Spieler zu ihren Spawn-Positionen und startet die Weltgrenze.
     *
     * @return Fehlermeldung oder null bei Erfolg
     */
    public String startGame() {
        if (gameRunning) return "Das Finale läuft bereits!";
        if (spawnFichten == null) return "Kein Spawn für das Fichten Königreich gesetzt! (/risiko setspawn fichten)";
        if (spawnDschungel == null) return "Kein Spawn für das Dschungel Königreich gesetzt! (/risiko setspawn dschungel)";

        // Prüfen ob beide Teams Spieler haben
        List<PlayerData> fichtenMembers = plugin.getTeamManager().getKingdomMembers(Kingdom.FICHTEN);
        List<PlayerData> dschungelMembers = plugin.getTeamManager().getKingdomMembers(Kingdom.DSCHUNGEL);

        if (fichtenMembers.isEmpty()) return "Das Fichten Königreich hat keine Mitglieder!";
        if (dschungelMembers.isEmpty()) return "Das Dschungel Königreich hat keine Mitglieder!";

        gameRunning = true;

        // Alle Finale-Teilnehmer markieren und teleportieren
        int teleported = 0;
        for (PlayerData data : fichtenMembers) {
            data.setInGame(true);
            plugin.getDataManager().save(data.getUuid());
            Player player = Bukkit.getPlayer(data.getUuid());
            if (player != null && player.isOnline()) {
                player.teleport(spawnFichten);
                teleported++;
            }
        }
        for (PlayerData data : dschungelMembers) {
            data.setInGame(true);
            plugin.getDataManager().save(data.getUuid());
            Player player = Bukkit.getPlayer(data.getUuid());
            if (player != null && player.isOnline()) {
                player.teleport(spawnDschungel);
                teleported++;
            }
        }

        // Weltgrenze setzen
        setupWorldBorder();

        // Countdown-Ankündigung
        int shrinkTime = plugin.getConfig().getInt("border-shrink-time", 600);
        Bukkit.broadcast(Component.text("")
                .append(Component.text("═══════════════════════════════", NamedTextColor.GOLD))
        );
        Bukkit.broadcast(Component.text("  ⚔ DAS RISIKO-FINALE BEGINNT! ⚔")
                .color(TextColor.color(0xFF6600))
                .decoration(TextDecoration.BOLD, true));
        Bukkit.broadcast(Component.text("  Fichten Königreich vs. Dschungel Königreich")
                .color(NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text("  " + teleported + " Spieler wurden teleportiert!")
                .color(NamedTextColor.WHITE));
        Bukkit.broadcast(Component.text("  Die Grenze beginnt in " + shrinkTime + " Sekunden zu schrumpfen!")
                .color(NamedTextColor.AQUA));
        Bukkit.broadcast(Component.text("═══════════════════════════════", NamedTextColor.GOLD));

        // Titel für alle Spieler
        Title title = Title.title(
                Component.text("⚔ RISIKO ⚔").color(TextColor.color(0xFF6600)).decoration(TextDecoration.BOLD, true),
                Component.text("Das Finale beginnt!").color(NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
        );
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
        }

        return null;
    }

    /**
     * Setzt die Weltgrenze für das Finale.
     */
    private void setupWorldBorder() {
        World world = spawnFichten.getWorld();
        WorldBorder border = world.getWorldBorder();

        double startSize = plugin.getConfig().getDouble("border-start-size", 500.0);
        double endSize = plugin.getConfig().getDouble("border-end-size", 50.0);
        int shrinkTime = plugin.getConfig().getInt("border-shrink-time", 600);

        // Mittelpunkt zwischen den beiden Spawns
        double centerX = (spawnFichten.getX() + spawnDschungel.getX()) / 2;
        double centerZ = (spawnFichten.getZ() + spawnDschungel.getZ()) / 2;

        border.setCenter(centerX, centerZ);
        border.setSize(startSize);
        border.setSize(endSize, shrinkTime);
        border.setDamageAmount(1.0);
        border.setDamageBuffer(5.0);
        border.setWarningDistance(10);
        border.setWarningTime(15);
    }

    /**
     * Stoppt das aktuelle Finale.
     */
    public void stopGame(String reason) {
        if (!gameRunning) return;
        gameRunning = false;

        // In-Game-Status aller Spieler zurücksetzen
        for (PlayerData data : plugin.getDataManager().getAll().values()) {
            if (data.isInGame()) {
                data.setInGame(false);
                plugin.getDataManager().save(data.getUuid());
            }
        }

        // Weltgrenze zurücksetzen
        for (World world : Bukkit.getWorlds()) {
            WorldBorder border = world.getWorldBorder();
            border.reset();
        }

        Bukkit.broadcast(Component.text("[Risiko] Das Finale wurde beendet. Grund: " + reason)
                .color(NamedTextColor.RED));
    }

    /**
     * Wird aufgerufen, wenn ein Spieler eliminiert wird (keine Herzen mehr).
     * Bannt den Spieler und prüft, ob das Spiel gewonnen wurde.
     */
    public void handlePlayerEliminated(UUID uuid, String playerName) {
        PlayerData data = plugin.getDataManager().get(uuid);
        if (data == null) return;

        data.setHearts(0);
        data.setBanned(true);
        data.setInGame(false);
        plugin.getDataManager().save(uuid);

        String banReason = plugin.getConfig().getString("messages.ban-reason", "Du hast alle deine Herzen verloren!");

        // Spieler über NAME-BanList bannen (kompatibel mit allen Versionen)
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(playerName, banReason, (java.util.Date) null, "Risiko Plugin");

        // Spieler kicken falls online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.kick(Component.text(banReason).color(NamedTextColor.RED));
        }

        // Ankündigung
        Bukkit.broadcast(Component.text("")
                .append(Component.text("☠ " + playerName + " hat alle Herzen verloren und wurde gebannt!", NamedTextColor.RED)
                        .decoration(TextDecoration.BOLD, true))
        );

        // Siegprüfung (nur wenn Finale läuft)
        if (gameRunning) {
            checkWinCondition();
        }
    }

    /**
     * Prüft, ob ein Team gewonnen hat.
     * Ein Team gewinnt, wenn alle Gegner keine Herzen mehr haben.
     */
    public void checkWinCondition() {
        if (!gameRunning) return;

        Map<Kingdom, Integer> aliveCounts = plugin.getTeamManager().getAliveCounts();

        int fichtenAlive = aliveCounts.getOrDefault(Kingdom.FICHTEN, 0);
        int dschungelAlive = aliveCounts.getOrDefault(Kingdom.DSCHUNGEL, 0);

        if (fichtenAlive == 0 && dschungelAlive == 0) {
            announceWinner(null); // Unentschieden
        } else if (fichtenAlive == 0) {
            announceWinner(Kingdom.DSCHUNGEL);
        } else if (dschungelAlive == 0) {
            announceWinner(Kingdom.FICHTEN);
        }
    }

    /**
     * Kündigt den Gewinner an und beendet das Spiel.
     */
    private void announceWinner(Kingdom winner) {
        gameRunning = false;

        // Weltgrenze zurücksetzen
        for (World world : Bukkit.getWorlds()) {
            world.getWorldBorder().reset();
        }

        // In-Game-Status zurücksetzen
        for (PlayerData data : plugin.getDataManager().getAll().values()) {
            data.setInGame(false);
            plugin.getDataManager().save(data.getUuid());
        }

        if (winner == null) {
            // Unentschieden
            Bukkit.broadcast(Component.text(""));
            Bukkit.broadcast(Component.text("══════════════════════════════════════════", NamedTextColor.GRAY));
            Bukkit.broadcast(Component.text("  ⚔ RISIKO FINALE BEENDET ⚔  ", NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, true));
            Bukkit.broadcast(Component.text("  Unentschieden! Kein Team hat gewonnen.", NamedTextColor.YELLOW));
            Bukkit.broadcast(Component.text("══════════════════════════════════════════", NamedTextColor.GRAY));

            Title drawTitle = Title.title(
                    Component.text("UNENTSCHIEDEN!").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, true),
                    Component.text("Kein Team hat gewonnen!").color(NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1))
            );
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showTitle(drawTitle);
            }
            return;
        }

        TextColor winnerColor = winner == Kingdom.FICHTEN
                ? TextColor.color(0xFFAA00)
                : TextColor.color(0x00AA00);
        String winnerName = winner.getDisplayName();

        // Gewinner-Mitglieder holen
        List<PlayerData> winners = plugin.getTeamManager().getKingdomMembers(winner);
        StringBuilder winnerNames = new StringBuilder();
        for (PlayerData w : winners) {
            if (!winnerNames.isEmpty()) winnerNames.append(", ");
            winnerNames.append(w.isKing() ? "♔ " : "").append(w.getName());
        }

        // Broadcast
        Bukkit.broadcast(Component.text(""));
        Bukkit.broadcast(Component.text("══════════════════════════════════════════").color(winnerColor));
        Bukkit.broadcast(
                Component.text("  🏆 " + winnerName.toUpperCase() + " HAT GEWONNEN! 🏆  ")
                        .color(winnerColor)
                        .decoration(TextDecoration.BOLD, true)
        );
        Bukkit.broadcast(
                Component.text("  Mitglieder: " + winnerNames)
                        .color(NamedTextColor.WHITE)
        );
        Bukkit.broadcast(Component.text("══════════════════════════════════════════").color(winnerColor));
        Bukkit.broadcast(Component.text(""));

        // Titel für alle Spieler
        Title winTitle = Title.title(
                Component.text("🏆 " + winnerName + " 🏆")
                        .color(winnerColor)
                        .decoration(TextDecoration.BOLD, true),
                Component.text("Hat das Risiko-Finale gewonnen!").color(NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(7), Duration.ofSeconds(2))
        );
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(winTitle);
        }

        // Gewinner feiern lassen
        for (PlayerData winnerData : winners) {
            Player winnerPlayer = Bukkit.getPlayer(winnerData.getUuid());
            if (winnerPlayer != null && winnerPlayer.isOnline()) {
                winnerPlayer.playSound(winnerPlayer.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public Location getSpawnFichten() {
        return spawnFichten;
    }

    public Location getSpawnDschungel() {
        return spawnDschungel;
    }
}
