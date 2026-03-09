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
        // Konfiguration laden (erstellt config.yml nur wenn sie noch nicht existiert)
        saveDefaultConfig();

        // Konfiguration migrieren: veraltete oder fehlerhafte Werte korrigieren.
        // Dieser Schritt muss VOR dataManager.init() laufen, da dieser die Config-Werte liest.
        migrateConfig();

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

    // ===== Konfigurationsmigration =====

    /**
     * Korrigiert veraltete oder fehlerhafte Konfigurationswerte.
     * Wird beim Start VOR dem Laden der Spielerdaten aufgerufen, damit alle Manager
     * sofort die richtigen Werte aus der Config lesen.
     */
    private void migrateConfig() {
        boolean changed = false;

        // Herzen: default-hearts muss 1 sein (alter Default war 3 – zu viele)
        int defaultHearts = getConfig().getInt("default-hearts", 1);
        if (defaultHearts != 1) {
            getLogger().warning("default-hearts war " + defaultHearts + " – wird auf 1 korrigiert.");
            getConfig().set("default-hearts", 1);
            changed = true;
        }

        // König-Herzen: king-hearts muss 2 sein
        int kingHearts = getConfig().getInt("king-hearts", 2);
        if (kingHearts != 2) {
            getLogger().warning("king-hearts war " + kingHearts + " – wird auf 2 korrigiert.");
            getConfig().set("king-hearts", 2);
            changed = true;
        }

        // Weltgrenze: zu kleine Startwerte auf aktuelle Defaults anheben
        if (getConfig().getDouble("border-start-size", 2000.0) < 1000.0) {
            getConfig().set("border-start-size", 2000.0);
            changed = true;
        }
        if (getConfig().getDouble("border-end-size", 100.0) < 75.0) {
            getConfig().set("border-end-size", 100.0);
            changed = true;
        }
        if (getConfig().getInt("border-shrink-time", 3600) < 1800) {
            getConfig().set("border-shrink-time", 3600);
            changed = true;
        }

        if (changed) {
            saveConfig();
            getLogger().info("Konfiguration wurde automatisch auf aktuelle Werte migriert.");
        }
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
