package de.fellinara.risiko.managers;

import de.fellinara.risiko.RisikoPlugin;
import de.fellinara.risiko.models.Kingdom;
import de.fellinara.risiko.models.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Verwaltet die visuellen Herzen der Spieler und zeigt sie in der Action Bar an.
 */
public class HeartManager {

    // Orange für das coole Herz-Symbol
    private static final TextColor HEART_COLOR_FULL = TextColor.color(0xFF6600);       // Orange
    private static final TextColor HEART_COLOR_EMPTY = TextColor.color(0x555555);      // Dunkelgrau
    private static final TextColor KING_HEART_COLOR = TextColor.color(0xFFD700);       // Gold für König-Herzen

    // Fichten Königreich: Gold
    private static final TextColor FICHTEN_COLOR = TextColor.color(0xFFAA00);
    // Dschungel Königreich: Grün
    private static final TextColor DSCHUNGEL_COLOR = TextColor.color(0x00AA00);

    private static final String FULL_HEART = "❤";
    private static final String EMPTY_HEART = "♡";
    private static final String CROWN = "♔ ";

    private final RisikoPlugin plugin;

    public HeartManager(RisikoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Startet den Action-Bar-Task, der regelmäßig die Herzen anzeigt.
     */
    public void startActionBarTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllActionBars, 0L, 20L);
    }

    /**
     * Aktualisiert die Action Bar für alle Online-Spieler.
     */
    public void updateAllActionBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateActionBar(player);
        }
    }

    /**
     * Aktualisiert die Action Bar für einen bestimmten Spieler.
     */
    public void updateActionBar(Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        if (data == null) return;

        Component message = buildActionBarComponent(data);
        player.sendActionBar(message);
    }

    /**
     * Erstellt die Action-Bar-Komponente für einen Spieler.
     * Format: [♔ ]❤❤ [Königreich-Name]
     * Normal: 1 oranges Herz; König: 2 goldene Herzen.
     */
    public Component buildActionBarComponent(PlayerData data) {
        int currentHearts = data.getHearts();
        int maxHearts = getMaxHearts(data);
        Kingdom kingdom = data.getKingdom();

        Component component = Component.empty();

        // Krone für König
        if (data.isKing()) {
            component = component.append(
                    Component.text(CROWN)
                            .color(KING_HEART_COLOR)
                            .decoration(TextDecoration.BOLD, true)
            );
        }

        // Herzen anzeigen: volle Herzen in der jeweiligen Farbe, leere in grau
        // König-Herzen sind immer gold, normale Herzen sind orange
        TextColor fullColor = data.isKing() ? KING_HEART_COLOR : HEART_COLOR_FULL;

        for (int i = 0; i < maxHearts; i++) {
            boolean isFull = i < currentHearts;

            TextColor color = isFull ? fullColor : HEART_COLOR_EMPTY;
            String symbol = isFull ? FULL_HEART : EMPTY_HEART;
            component = component.append(Component.text(symbol).color(color));

            // Leerzeichen zwischen Herzen für bessere Lesbarkeit
            if (i < maxHearts - 1) {
                component = component.append(Component.text(" ").color(HEART_COLOR_EMPTY));
            }
        }

        // Königreich-Name anzeigen
        if (kingdom != null) {
            TextColor kingdomColor = kingdom == Kingdom.FICHTEN ? FICHTEN_COLOR : DSCHUNGEL_COLOR;
            component = component
                    .append(Component.text("  "))
                    .append(Component.text("[" + kingdom.getDisplayName() + "]")
                            .color(kingdomColor));
        }

        return component;
    }

    /**
     * Gibt die maximale Herzanzahl für einen Spieler zurück.
     * Normal: default-hearts (1). König: king-hearts (2) — ersetzt default, nicht addiert.
     */
    public int getMaxHearts(PlayerData data) {
        if (data.isKing()) {
            return plugin.getConfig().getInt("king-hearts", 2);
        }
        return plugin.getConfig().getInt("default-hearts", 1);
    }

    /**
     * Entfernt ein Herz von einem Spieler (durch Töten).
     * Gibt true zurück, wenn der Spieler keine Herzen mehr hat.
     *
     * @param victim  Der getötete Spieler
     * @param killer  Der Killer (kann null sein)
     * @return true wenn keine Herzen mehr
     */
    public boolean removeHeartOnKill(PlayerData victim, PlayerData killer) {
        // Schutz: Letztes König-Herz kann nur von einem anderen König genommen werden
        if (victim.isKing() && victim.getHearts() == 1) {
            if (killer == null || !killer.isKing()) {
                // Normaler Spieler kann das letzte König-Herz nicht nehmen
                return false;
            }
        }

        return victim.removeHeart();
    }

    /**
     * Gibt einem Spieler Herzen (bis zum Maximum für den Spieler).
     */
    public void addHearts(PlayerData data, int amount) {
        int maxHearts = getMaxHearts(data);
        int newAmount = Math.min(data.getHearts() + amount, maxHearts);
        data.setHearts(newAmount);
    }

    /**
     * Entfernt Herzen von einem Spieler (Admin-Befehl, ohne König-Schutz).
     * Gibt true zurück, wenn keine Herzen mehr vorhanden.
     */
    public boolean removeHearts(PlayerData data, int amount) {
        data.setHearts(data.getHearts() - amount);
        return data.getHearts() <= 0;
    }

    /**
     * Setzt die Herzen auf den Standardwert zurück (basierend auf ob Spieler König ist).
     */
    public void resetHearts(PlayerData data) {
        data.setHearts(getMaxHearts(data));
    }
}
