package de.fellinara.risiko.models;

import java.util.UUID;

/**
 * Speichert alle Risiko-relevanten Daten eines Spielers.
 */
public class PlayerData {

    private final UUID uuid;
    private String name;
    private Kingdom kingdom;
    private boolean king;
    private int hearts;
    private boolean inGame; // Teilnehmer am aktuellen Finale
    private boolean banned; // Durch das Plugin gebannt

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.kingdom = null;
        this.king = false;
        this.hearts = 3; // Standardwert, wird durch Config überschrieben
        this.inGame = false;
        this.banned = false;
    }

    // --- Getter ---

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Kingdom getKingdom() {
        return kingdom;
    }

    public boolean isKing() {
        return king;
    }

    public int getHearts() {
        return hearts;
    }

    public boolean isInGame() {
        return inGame;
    }

    public boolean isBanned() {
        return banned;
    }

    // --- Setter ---

    public void setName(String name) {
        this.name = name;
    }

    public void setKingdom(Kingdom kingdom) {
        this.kingdom = kingdom;
    }

    public void setKing(boolean king) {
        this.king = king;
    }

    public void setHearts(int hearts) {
        this.hearts = Math.max(0, hearts);
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    /**
     * Entfernt ein Herz. Gibt true zurück, wenn der Spieler keine Herzen mehr hat.
     */
    public boolean removeHeart() {
        if (hearts > 0) {
            hearts--;
        }
        return hearts <= 0;
    }

    /**
     * Fügt Herzen hinzu.
     */
    public void addHearts(int amount) {
        this.hearts += amount;
    }

    @Override
    public String toString() {
        return "PlayerData{uuid=" + uuid + ", name=" + name + ", kingdom=" + kingdom
                + ", king=" + king + ", hearts=" + hearts + ", inGame=" + inGame + "}";
    }
}
