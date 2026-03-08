package de.fellinara.risiko.managers;

import de.fellinara.risiko.RisikoPlugin;
import de.fellinara.risiko.models.Kingdom;
import de.fellinara.risiko.models.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Verwaltet Team-Zuweisungen und König-Mechanik.
 */
public class TeamManager {

    private final RisikoPlugin plugin;

    // Welcher Spieler ist König welches Königreichs (UUID)
    private final Map<Kingdom, UUID> kings = new EnumMap<>(Kingdom.class);

    public TeamManager(RisikoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Weist einen Spieler einem Königreich zu.
     */
    public void assignKingdom(UUID uuid, String playerName, Kingdom kingdom) {
        PlayerData data = plugin.getDataManager().getOrCreate(uuid, playerName);

        // Wenn der Spieler vorher König war, König-Status entfernen
        if (data.isKing() && data.getKingdom() != null) {
            removeKing(data.getKingdom());
        }

        data.setKingdom(kingdom);
        data.setKing(false);
        plugin.getDataManager().save(uuid);
    }

    /**
     * Macht einen Spieler zum König seines Königreichs.
     * Der Spieler muss bereits einem Königreich zugewiesen sein.
     *
     * @return true bei Erfolg, false wenn kein Königreich zugewiesen
     */
    public boolean assignKing(UUID uuid, String playerName) {
        PlayerData data = plugin.getDataManager().getOrCreate(uuid, playerName);
        if (data.getKingdom() == null) {
            return false;
        }

        // Alten König entfernen
        Kingdom kingdom = data.getKingdom();
        UUID oldKingUuid = kings.get(kingdom);
        if (oldKingUuid != null && !oldKingUuid.equals(uuid)) {
            PlayerData oldKing = plugin.getDataManager().get(oldKingUuid);
            if (oldKing != null) {
                oldKing.setKing(false);
                // Herzen auf maximal non-König-Wert kappen (behält verlorene Herzen bei)
                int defaultMax = plugin.getConfig().getInt("default-hearts", 3);
                oldKing.setHearts(Math.min(oldKing.getHearts(), defaultMax));
                plugin.getDataManager().save(oldKingUuid);
            }
        }

        // Neuen König setzen
        data.setKing(true);
        kings.put(kingdom, uuid);

        // Herzen für König setzen (max)
        int maxHearts = plugin.getHeartManager().getMaxHearts(data);
        if (data.getHearts() < maxHearts) {
            data.setHearts(maxHearts);
        }

        plugin.getDataManager().save(uuid);
        return true;
    }

    /**
     * Entfernt den König eines Königreichs.
     */
    public void removeKing(Kingdom kingdom) {
        UUID oldKingUuid = kings.remove(kingdom);
        if (oldKingUuid != null) {
            PlayerData oldKing = plugin.getDataManager().get(oldKingUuid);
            if (oldKing != null) {
                oldKing.setKing(false);
                // Herzen auf maximal non-König-Wert kappen
                int defaultMax = plugin.getConfig().getInt("default-hearts", 3);
                oldKing.setHearts(Math.min(oldKing.getHearts(), defaultMax));
                plugin.getDataManager().save(oldKingUuid);
            }
        }
    }

    /**
     * Gibt alle Spieler eines Königreichs zurück.
     */
    public List<PlayerData> getKingdomMembers(Kingdom kingdom) {
        List<PlayerData> members = new ArrayList<>();
        for (PlayerData data : plugin.getDataManager().getAll().values()) {
            if (kingdom.equals(data.getKingdom())) {
                members.add(data);
            }
        }
        return members;
    }

    /**
     * Gibt alle Spieler eines Königreichs zurück, die noch Herzen haben (nicht gebannt).
     */
    public List<PlayerData> getAliveKingdomMembers(Kingdom kingdom) {
        List<PlayerData> members = new ArrayList<>();
        for (PlayerData data : plugin.getDataManager().getAll().values()) {
            if (kingdom.equals(data.getKingdom()) && data.getHearts() > 0 && !data.isBanned()) {
                members.add(data);
            }
        }
        return members;
    }

    /**
     * Gibt den König eines Königreichs zurück (null wenn keiner zugewiesen).
     */
    public PlayerData getKing(Kingdom kingdom) {
        UUID kingUuid = kings.get(kingdom);
        if (kingUuid == null) return null;
        return plugin.getDataManager().get(kingUuid);
    }

    /**
     * Prüft, ob ein Spieler König ist.
     */
    public boolean isKing(UUID uuid) {
        return kings.containsValue(uuid);
    }

    /**
     * Initialisiert die König-Map aus den gespeicherten Daten.
     */
    public void loadKingsFromData() {
        kings.clear();
        for (Map.Entry<UUID, PlayerData> entry : plugin.getDataManager().getAll().entrySet()) {
            PlayerData data = entry.getValue();
            if (data.isKing() && data.getKingdom() != null) {
                // Nur einen König pro Königreich erlauben
                if (!kings.containsKey(data.getKingdom())) {
                    kings.put(data.getKingdom(), entry.getKey());
                } else {
                    // Duplikat: König-Status entfernen
                    data.setKing(false);
                }
            }
        }
    }

    /**
     * Sendet eine Nachricht an alle Mitglieder eines Königreichs.
     */
    public void broadcastToKingdom(Kingdom kingdom, Component message) {
        for (PlayerData data : getKingdomMembers(kingdom)) {
            Player player = Bukkit.getPlayer(data.getUuid());
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Gibt die Anzahl der lebenden Spieler pro Königreich zurück.
     */
    public Map<Kingdom, Integer> getAliveCounts() {
        Map<Kingdom, Integer> counts = new EnumMap<>(Kingdom.class);
        for (Kingdom k : Kingdom.values()) {
            counts.put(k, 0);
        }
        for (PlayerData data : plugin.getDataManager().getAll().values()) {
            if (data.getKingdom() != null && data.getHearts() > 0 && !data.isBanned() && data.isInGame()) {
                counts.merge(data.getKingdom(), 1, Integer::sum);
            }
        }
        return counts;
    }
}
