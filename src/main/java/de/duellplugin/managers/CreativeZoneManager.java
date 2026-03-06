package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages a global creative zone where players can build custom kits.
 * When a player enters the region they receive Creative mode; when they leave they are
 * switched back to Survival and the lobby inventory is restored.
 */
public class CreativeZoneManager {

    private final DuellPlugin plugin;

    /** Corner 1 of the creative zone. */
    private Location pos1;
    /** Corner 2 of the creative zone. */
    private Location pos2;

    /** Players currently inside the creative zone. */
    private final Set<UUID> inZone;
    /** Saved inventories for players who entered the zone (restored on exit). */
    private final Map<UUID, ItemStack[]> savedContents;
    private final Map<UUID, ItemStack[]> savedArmor;
    private final Map<UUID, ItemStack>   savedOffHand;

    public CreativeZoneManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.inZone = new HashSet<>();
        this.savedContents = new HashMap<>();
        this.savedArmor    = new HashMap<>();
        this.savedOffHand  = new HashMap<>();
        loadFromConfig();
    }

    // ── Config persistence ──────────────────────────────────────────

    private void loadFromConfig() {
        var cfg = plugin.getConfig();
        if (cfg.contains("creative-zone.pos1")) {
            pos1 = deserializeLocation(
                    cfg.getString("creative-zone.pos1.world"),
                    cfg.getDouble("creative-zone.pos1.x"),
                    cfg.getDouble("creative-zone.pos1.y"),
                    cfg.getDouble("creative-zone.pos1.z"));
        }
        if (cfg.contains("creative-zone.pos2")) {
            pos2 = deserializeLocation(
                    cfg.getString("creative-zone.pos2.world"),
                    cfg.getDouble("creative-zone.pos2.x"),
                    cfg.getDouble("creative-zone.pos2.y"),
                    cfg.getDouble("creative-zone.pos2.z"));
        }
    }

    public void saveToConfig() {
        var cfg = plugin.getConfig();
        if (pos1 != null && pos1.getWorld() != null) {
            cfg.set("creative-zone.pos1.world", pos1.getWorld().getName());
            cfg.set("creative-zone.pos1.x", pos1.getX());
            cfg.set("creative-zone.pos1.y", pos1.getY());
            cfg.set("creative-zone.pos1.z", pos1.getZ());
        }
        if (pos2 != null && pos2.getWorld() != null) {
            cfg.set("creative-zone.pos2.world", pos2.getWorld().getName());
            cfg.set("creative-zone.pos2.x", pos2.getX());
            cfg.set("creative-zone.pos2.y", pos2.getY());
            cfg.set("creative-zone.pos2.z", pos2.getZ());
        }
        plugin.saveConfig();
    }

    private Location deserializeLocation(String worldName, double x, double y, double z) {
        if (worldName == null) return null;
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }

    // ── Setters ─────────────────────────────────────────────────────

    public void setPos1(Location loc) {
        this.pos1 = loc.getBlock().getLocation();
        saveToConfig();
    }

    public void setPos2(Location loc) {
        this.pos2 = loc.getBlock().getLocation();
        saveToConfig();
    }

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }

    public boolean isDefined() {
        return pos1 != null && pos2 != null
                && pos1.getWorld() != null
                && pos1.getWorld().equals(pos2.getWorld());
    }

    // ── Zone checks ──────────────────────────────────────────────────

    public boolean isInZone(Location loc) {
        if (!isDefined()) return false;
        if (!loc.getWorld().equals(pos1.getWorld())) return false;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();

        return bx >= minX && bx <= maxX
                && by >= minY && by <= maxY
                && bz >= minZ && bz <= maxZ;
    }

    // ── Zone entry / exit ────────────────────────────────────────────

    /**
     * Called when a player enters the creative zone.
     * Saves their current inventory and switches them to Creative mode.
     */
    public void enterZone(Player player) {
        if (inZone.contains(player.getUniqueId())) return;
        inZone.add(player.getUniqueId());

        // Save current inventory
        savedContents.put(player.getUniqueId(), player.getInventory().getStorageContents().clone());
        savedArmor.put(player.getUniqueId(),    player.getInventory().getArmorContents().clone());
        savedOffHand.put(player.getUniqueId(),  player.getInventory().getItemInOffHand().clone());

        player.getInventory().clear();
        player.setGameMode(GameMode.CREATIVE);
        player.sendMessage(plugin.getPrefix()
                + "§aDu betrittst die §6Kit-Bauzone§a! Du erhältst §6Creative-Modus§a.");
        player.sendMessage(plugin.getPrefix()
                + "§7Stelle dein Kit zusammen und nutze §e/mykit create <Name> §7zum Speichern.");
    }

    /**
     * Called when a player leaves the creative zone.
     * Restores their previous inventory and switches them back to Survival + lobby.
     */
    public void exitZone(Player player) {
        if (!inZone.remove(player.getUniqueId())) return;

        player.getInventory().clear();

        ItemStack[] contents = savedContents.remove(player.getUniqueId());
        ItemStack[] armor    = savedArmor.remove(player.getUniqueId());
        ItemStack   offHand  = savedOffHand.remove(player.getUniqueId());

        if (contents != null) player.getInventory().setStorageContents(contents);
        if (armor    != null) player.getInventory().setArmorContents(armor);
        if (offHand  != null) player.getInventory().setItemInOffHand(offHand);

        plugin.getLobbyManager().sendToLobby(player);
        player.sendMessage(plugin.getPrefix()
                + "§eDu hast die Kit-Bauzone verlassen. Dein Inventar wurde wiederhergestellt.");
    }

    public boolean isPlayerInZone(UUID uuid) {
        return inZone.contains(uuid);
    }

    /** Called on server stop / plugin disable to clean up all players in the zone. */
    public void shutdown() {
        for (UUID uid : new java.util.ArrayList<>(inZone)) {
            Player p = plugin.getServer().getPlayer(uid);
            if (p != null) exitZone(p);
        }
    }
}
