package de.fellinara.risiko.managers;

import de.fellinara.risiko.RisikoPlugin;
import de.fellinara.risiko.models.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet den Kampf-Tag und das Kampf-Logout-System.
 * Wenn ein Spieler sich während eines Kampfes ausloggt, wird ein ArmorStand
 * an seiner Position gespawnt, der für 15 Sekunden angreifbar ist.
 */
public class CombatManager {

    private final RisikoPlugin plugin;

    // UUID des Spielers -> Timestamp des letzten Kampfes (in ms)
    private final Map<UUID, Long> combatTags = new HashMap<>();

    // UUID des ArmorStands -> UUID des Spielers
    private final Map<UUID, UUID> logoutStands = new HashMap<>();

    // UUID des Spielers -> BukkitTask für das automatische Entfernen des Stands
    private final Map<UUID, BukkitTask> standRemovalTasks = new HashMap<>();

    public CombatManager(RisikoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Markiert einen Spieler als im Kampf befindlich.
     */
    public void tag(UUID uuid) {
        int tagSeconds = plugin.getConfig().getInt("combat-tag-seconds", 30);
        combatTags.put(uuid, System.currentTimeMillis() + (tagSeconds * 1000L));
    }

    /**
     * Entfernt den Kampf-Tag eines Spielers.
     */
    public void untag(UUID uuid) {
        combatTags.remove(uuid);
    }

    /**
     * Prüft, ob ein Spieler gerade im Kampf ist.
     */
    public boolean isTagged(UUID uuid) {
        Long expiry = combatTags.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            combatTags.remove(uuid);
            return false;
        }
        return true;
    }

    /**
     * Wird aufgerufen, wenn sich ein Spieler ausloggt.
     * Falls er im Kampf ist, wird ein ArmorStand gespawnt.
     */
    public void handleLogout(Player player) {
        if (!isTagged(player.getUniqueId())) return;

        Location loc = player.getLocation();
        PlayerData playerData = plugin.getDataManager().get(player.getUniqueId());
        if (playerData == null) return;

        // ArmorStand spawnen
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class, as -> {
            as.setVisible(true);
            as.setArms(true);
            as.setBasePlate(false);
            as.setGravity(true);
            as.setInvulnerable(false);
            as.setCustomNameVisible(true);
            as.customName(Component.text("[AUSGELOGGT] " + player.getName())
                    .color(NamedTextColor.RED));

            // Spielerkopf als Helm setzen
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(player);
            skull.setItemMeta(meta);
            as.setHelmet(skull);

            // Spieler-Equipment kopieren
            as.getEquipment().setChestplate(player.getInventory().getChestplate());
            as.getEquipment().setLeggings(player.getInventory().getLeggings());
            as.getEquipment().setBoots(player.getInventory().getBoots());
            as.getEquipment().setItemInMainHand(player.getInventory().getItemInMainHand());
        });

        UUID standUuid = stand.getUniqueId();
        UUID playerUuid = player.getUniqueId();
        logoutStands.put(standUuid, playerUuid);

        // Ankündigung
        Bukkit.broadcast(Component.text(player.getName() + " hat sich während des Kampfes ausgeloggt! Der Dummy bleibt für "
                + plugin.getConfig().getInt("combat-logout-stand-seconds", 15) + " Sekunden!")
                .color(NamedTextColor.RED));

        // Automatisches Entfernen nach X Sekunden
        int standSeconds = plugin.getConfig().getInt("combat-logout-stand-seconds", 15);
        BukkitTask removalTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeStand(standUuid, false);
        }, standSeconds * 20L);

        standRemovalTasks.put(playerUuid, removalTask);
    }

    /**
     * Wird aufgerufen, wenn ein ArmorStand angegriffen wird.
     * Prüft, ob es ein Logout-Dummy ist und wendet ggf. die Kill-Logik an.
     *
     * @param standUuid  UUID des ArmorStands
     * @param attacker   Der Angreifer (muss ein Spieler sein)
     * @return true wenn der Dummy erfolgreich getötet wurde
     */
    public boolean handleStandAttack(UUID standUuid, Player attacker) {
        UUID victimUuid = logoutStands.get(standUuid);
        if (victimUuid == null) return false;

        PlayerData victimData = plugin.getDataManager().get(victimUuid);
        PlayerData attackerData = plugin.getDataManager().get(attacker.getUniqueId());

        if (victimData == null) {
            removeStand(standUuid, true);
            return true;
        }

        // Kill-Logik anwenden (Herz entfernen)
        boolean outOfHearts = plugin.getHeartManager().removeHeartOnKill(victimData, attackerData);

        Bukkit.broadcast(Component.text(attacker.getName() + " hat den ausgeloggten Spieler ")
                .color(NamedTextColor.RED)
                .append(Component.text(victimData.getName()).color(NamedTextColor.WHITE))
                .append(Component.text(" getötet! Verbleibende Herzen: " + victimData.getHearts()).color(NamedTextColor.RED))
        );

        plugin.getDataManager().save(victimUuid);

        // Dummy entfernen
        removeStand(standUuid, true);

        // Kein Herz mehr → Ban
        if (outOfHearts) {
            plugin.getGameManager().handlePlayerEliminated(victimUuid, victimData.getName());
        } else {
            // Wenn der Spieler wieder online ist, Action Bar aktualisieren
            Player victim = Bukkit.getPlayer(victimUuid);
            if (victim != null) {
                plugin.getHeartManager().updateActionBar(victim);
            }
        }

        return true;
    }

    /**
     * Entfernt einen Logout-Stand.
     *
     * @param standUuid  UUID des Stands
     * @param killed     Ob der Stand getötet wurde (true) oder die Zeit abgelaufen ist (false)
     */
    private void removeStand(UUID standUuid, boolean killed) {
        UUID playerUuid = logoutStands.remove(standUuid);

        // Entity entfernen
        Entity entity = Bukkit.getEntity(standUuid);
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }

        if (playerUuid != null) {
            // Geplanten Entfernungs-Task abbrechen, falls der Stand vorher getötet wurde
            // (wenn killed == false, wurde der Task selbst ausgeführt und braucht nicht abgebrochen zu werden)
            BukkitTask task = standRemovalTasks.remove(playerUuid);
            if (task != null && killed) {
                task.cancel();
            }

            if (!killed) {
                Bukkit.broadcast(Component.text(
                        getPlayerName(playerUuid) + "s Dummy ist verschwunden. Der Spieler hat überlebt!"
                ).color(NamedTextColor.GREEN));
            }
        }
    }

    /**
     * Prüft, ob eine Entity-UUID ein Logout-Stand ist.
     */
    public boolean isLogoutStand(UUID entityUuid) {
        return logoutStands.containsKey(entityUuid);
    }

    /**
     * Entfernt alle Logout-Stands (beim Plugin-Stop).
     */
    public void cleanup() {
        for (UUID standUuid : logoutStands.keySet()) {
            Entity entity = Bukkit.getEntity(standUuid);
            if (entity != null) entity.remove();
        }
        logoutStands.clear();
        for (BukkitTask task : standRemovalTasks.values()) {
            task.cancel();
        }
        standRemovalTasks.clear();
        combatTags.clear();
    }

    private String getPlayerName(UUID uuid) {
        PlayerData data = plugin.getDataManager().get(uuid);
        return data != null ? data.getName() : uuid.toString();
    }
}
