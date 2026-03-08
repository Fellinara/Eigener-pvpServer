package de.fellinara.risiko;

import de.fellinara.risiko.commands.RisikoCommand;
import de.fellinara.risiko.listeners.PlayerListener;
import de.fellinara.risiko.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Haupt-Plugin-Klasse für das Risiko-Plugin.
 *
 * Features:
 * - Visuelle Herzen in der Action Bar (oranges Herz-System)
 * - 2 Königreiche: Fichten Königreich vs. Dschungel Königreich
 * - König-Mechanik mit Herzschutz
 * - Kampf-Logout-System (15-Sekunden Dummy)
 * - Finale mit Weltgrenze und Sieg-Erkennung
 * - Ban-System bei 0 Herzen
 */
public class RisikoPlugin extends JavaPlugin {

    private DataManager dataManager;
    private HeartManager heartManager;
    private TeamManager teamManager;
    private CombatManager combatManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        // Konfiguration laden
        saveDefaultConfig();

        // Manager initialisieren
        dataManager = new DataManager(this);
        dataManager.init();

        heartManager = new HeartManager(this);
        teamManager = new TeamManager(this);
        combatManager = new CombatManager(this);
        gameManager = new GameManager(this);

        // Könige aus gespeicherten Daten laden
        teamManager.loadKingsFromData();

        // Spawn-Punkte laden
        gameManager.loadSpawns();

        // Listener registrieren
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Befehle registrieren
        RisikoCommand risikoCommand = new RisikoCommand(this);
        getCommand("risiko").setExecutor(risikoCommand);
        getCommand("risiko").setTabCompleter(risikoCommand);

        // Action-Bar-Task starten (aktualisiert alle 20 Ticks = 1 Sekunde)
        heartManager.startActionBarTask();

        getLogger().info("╔═══════════════════════════════════╗");
        getLogger().info("║  Risiko-Plugin wurde gestartet!   ║");
        getLogger().info("║  Fichten vs. Dschungel Königreich ║");
        getLogger().info("╚═══════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        // Alle Daten speichern
        if (dataManager != null) {
            dataManager.saveAll();
        }

        // Combat-Manager aufräumen (Dummies entfernen)
        if (combatManager != null) {
            combatManager.cleanup();
        }

        getLogger().info("Risiko-Plugin wurde deaktiviert. Daten gespeichert.");
    }

    // ===== Getter für Manager =====

    public DataManager getDataManager() {
        return dataManager;
    }

    public HeartManager getHeartManager() {
        return heartManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
