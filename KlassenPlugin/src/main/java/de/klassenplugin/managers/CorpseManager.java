package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Death-corpse system.
 *
 * <p>When a player dies a {@link Material#PLAYER_HEAD} skull is placed at the
 * death location (if a suitable air block exists there or directly above).
 * All items that would normally drop are stored inside a virtual inventory
 * linked to that block.  Any player can right-click the skull to retrieve the
 * items.  The skull is removed automatically once the inventory is emptied.
 *
 * <p>Only one player can access a corpse at a time to prevent item duplication.
 */
public class CorpseManager implements Listener {

    private static final String GUI_TITLE_PREFIX = "&8[&cLeiche&8] ";
    /** Substring used to identify a corpse inventory view. */
    private static final String GUI_TITLE_MARKER = "[&cLeiche&8]";

    private final KlassenPlugin plugin;

    /** Location key → remaining items stored in the corpse. */
    private final Map<String, List<ItemStack>> corpseItems = new HashMap<>();
    /** Location key → name of the player who died there. */
    private final Map<String, String> corpseOwners = new HashMap<>();
    /**
     * Location key → UUID of the player currently browsing this corpse.
     * {@code null} / absent means the corpse is free to access.
     */
    private final Map<String, UUID> activeSessions = new HashMap<>();
    /** Player UUID → location key of the corpse they have open. */
    private final Map<UUID, String> playerSession = new HashMap<>();

    public CorpseManager(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Called from DeathListener ─────────────────────────────────────────────

    /**
     * Intercepts all item drops, stores them inside a skull-corpse placed at
     * the nearest suitable block position.  If no placement is available the
     * items are left to drop normally (vanilla behaviour).
     */
    public void handleDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        if (drops.isEmpty()) return;

        Location placement = findPlacement(player.getLocation());
        if (placement == null) {
            // No suitable air block found – keep vanilla drops
            return;
        }

        // Suppress default item drops; we store them in the corpse
        event.getDrops().clear();

        // Place and skin the skull block
        Block block = placement.getBlock();
        block.setType(Material.PLAYER_HEAD);
        if (block.getState() instanceof Skull skull) {
            skull.setOwningPlayer(player);
            skull.update(true);
        }

        String key = key(placement);
        corpseItems.put(key, new ArrayList<>(drops));
        corpseOwners.put(key, player.getName());
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        // PlayerInteractEvent fires once per hand; ignore the off-hand event to
        // avoid opening the corpse GUI twice (which would trigger a close event
        // on the first GUI and cause item duplication).
        // Bedrock players via Geyser may send getHand() == null – allow those
        // through (they will never send a duplicate off-hand event).
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.PLAYER_HEAD) return;

        String key = key(block.getLocation());
        if (!corpseItems.containsKey(key)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        // Enforce single-access to prevent duplication
        UUID current = activeSessions.get(key);
        if (current != null && !current.equals(player.getUniqueId())) {
            player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&cDiese Leiche wird gerade von jemand anderem durchsucht!"));
            return;
        }

        openCorpse(player, key);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String key = playerSession.remove(player.getUniqueId());
        if (key == null) return;

        activeSessions.remove(key);

        // Collect items still in the inventory
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack is : event.getInventory().getContents()) {
            if (is != null && is.getType() != Material.AIR) remaining.add(is);
        }

        if (remaining.isEmpty()) {
            removeCorpse(key);
        } else {
            corpseItems.put(key, remaining);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!corpseItems.containsKey(key(event.getBlock().getLocation()))) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(KlassenPlugin.colorizeComponent(
                "&cDu kannst diese Leiche nicht abbauen!"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns {@code true} if the inventory view belongs to a corpse GUI. */
    public boolean isCorpseGui(org.bukkit.inventory.InventoryView view) {
        String title = LegacyComponentSerializer.legacyAmpersand().serialize(view.title());
        return title.contains(GUI_TITLE_MARKER);
    }

    private void openCorpse(Player player, String key) {
        List<ItemStack> items = corpseItems.get(key);
        String owner = corpseOwners.getOrDefault(key, "Unbekannt");

        // Choose inventory size (rows of 9, max 6 rows)
        int rows = Math.max(1, (int) Math.ceil(items.size() / 9.0));
        rows = Math.min(rows, 6);
        int size = rows * 9;

        Inventory inv = Bukkit.createInventory(null, size,
                LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(GUI_TITLE_PREFIX + owner));
        for (ItemStack item : items) {
            inv.addItem(item.clone());
        }

        activeSessions.put(key, player.getUniqueId());
        playerSession.put(player.getUniqueId(), key);
        player.openInventory(inv);
    }

    private void removeCorpse(String key) {
        Location loc = keyToLocation(key);
        if (loc != null && loc.getBlock().getType() == Material.PLAYER_HEAD) {
            loc.getBlock().setType(Material.AIR);
        }
        corpseItems.remove(key);
        corpseOwners.remove(key);
    }

    /**
     * Finds the block position for the corpse skull.
     * Tries the death block first (if passable), then one block above.
     * Returns {@code null} if neither position is suitable.
     */
    private Location findPlacement(Location deathLoc) {
        Block b = deathLoc.getBlock();
        if (b.getType().isAir() || b.isPassable()) return b.getLocation();
        Block above = b.getRelative(0, 1, 0);
        if (above.getType().isAir() || above.isPassable()) return above.getLocation();
        return null;
    }

    private String key(Location loc) {
        return loc.getWorld().getName()
                + ":" + loc.getBlockX()
                + ":" + loc.getBlockY()
                + ":" + loc.getBlockZ();
    }

    private Location keyToLocation(String key) {
        String[] p = key.split(":");
        if (p.length != 4) return null;
        World world = Bukkit.getWorld(p[0]);
        if (world == null) return null;
        try {
            return new Location(world,
                    Integer.parseInt(p[1]),
                    Integer.parseInt(p[2]),
                    Integer.parseInt(p[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
