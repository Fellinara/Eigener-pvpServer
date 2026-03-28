package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.managers.SpecialItemsManager;
import de.klassenplugin.managers.SpecialItemsManager.KapaChestData;
import de.klassenplugin.managers.SpecialItemsManager.KapaChestHolder;
import de.klassenplugin.managers.SpecialItemsManager.TurboHopperHolder;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Handles all game-play events for the three special items
 * (Verkaufsaxt, Turbo-Hopper, Kapazitätskiste).
 */
public class SpecialItemsListener implements Listener {

    private final KlassenPlugin plugin;

    public SpecialItemsListener(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // ── BlockBreakEvent ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block   = event.getBlock();
        SpecialItemsManager sim = plugin.getSpecialItemsManager();

        // ── Verkaufsaxt: sell chest / kapa-chest contents ─────────────────
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (SpecialItemsManager.isSpecialItem(hand, SpecialItemsManager.TYPE_VERKAUFSAXT)
                && isChestLike(block.getType())) {
            event.setCancelled(true);
            sim.applyVerkaufsaxt(player, block);
            return;
        }

        // ── Turbo Hopper: drop custom item + virtual inventory contents ────
        if (block.getType() == Material.HOPPER && sim.isTurboHopper(block.getLocation())) {
            event.setCancelled(true);
            Inventory inv = sim.getTurboHopperInventory(block.getLocation());
            if (inv != null) {
                for (ItemStack item : inv.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        block.getWorld().dropItemNaturally(block.getLocation(), item);
                    }
                }
            }
            block.getWorld().dropItemNaturally(
                    block.getLocation(), SpecialItemsManager.buildTurboHopper());
            block.setType(Material.AIR, false);
            sim.removeTurboHopper(block.getLocation());
            return;
        }

        // ── Kapazitätskiste: prevent breaking if items stored ─────────────
        if (block.getType() == Material.BARREL && sim.isKapaChest(block.getLocation())) {
            KapaChestData data = sim.getKapaChestData(block.getLocation());
            if (data != null && data.count > 0) {
                event.setCancelled(true);
                player.sendMessage(KlassenPlugin.colorizeComponent(
                        "&cDie Kapazitätskiste enthält noch &e"
                        + SpecialItemsManager.fmtNum(data.count)
                        + " &cItems! Leere sie zuerst."));
                return;
            }
            // Empty → let the player break it; drop the special item instead
            event.setCancelled(true);
            block.getWorld().dropItemNaturally(
                    block.getLocation(), SpecialItemsManager.buildKapaChest());
            block.setType(Material.AIR, false);
            sim.removeKapaChest(block.getLocation());
        }
    }

    // ── BlockPlaceEvent ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item  = event.getItemInHand();
        Block block     = event.getBlockPlaced();
        SpecialItemsManager sim = plugin.getSpecialItemsManager();

        if (SpecialItemsManager.isSpecialItem(item, SpecialItemsManager.TYPE_TURBO_HOPPER)) {
            sim.registerTurboHopper(block.getLocation());
        } else if (SpecialItemsManager.isSpecialItem(item, SpecialItemsManager.TYPE_KAPAZITAETSKISTE)) {
            sim.registerKapaChest(block.getLocation());
        }
    }

    // ── PlayerInteractEvent: open GUIs ────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        // Ignore the off-hand duplicate event
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        SpecialItemsManager sim = plugin.getSpecialItemsManager();

        // Turbo Hopper → open virtual 54-slot inventory
        if (block.getType() == Material.HOPPER && sim.isTurboHopper(block.getLocation())) {
            event.setCancelled(true);
            Inventory inv = sim.getTurboHopperInventory(block.getLocation());
            if (inv != null) player.openInventory(inv);
            return;
        }

        // Kapazitätskiste → open kapa-chest GUI
        if (block.getType() == Material.BARREL && sim.isKapaChest(block.getLocation())) {
            event.setCancelled(true);
            sim.openKapaGui(player, block.getLocation());
        }
    }

    // ── InventoryClickEvent: kapa-chest GUI ───────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInv = event.getView().getTopInventory();

        if (topInv.getHolder() instanceof KapaChestHolder holder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == topInv) {
                plugin.getSpecialItemsManager()
                      .handleKapaGuiClick(player, holder, event.getSlot(), event.getClick());
            }
        }
    }

    // ── InventoryMoveItemEvent ────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        SpecialItemsManager sim = plugin.getSpecialItemsManager();

        // Suppress vanilla hopper transfers for turbo-hopper inventories
        if (isTurboHopperInv(event.getSource(),      sim)
         || isTurboHopperInv(event.getDestination(), sim)
         || isTurboHopperInv(event.getInitiator(),   sim)) {
            event.setCancelled(true);
            return;
        }

        // Intercept vanilla hopper pushing INTO a Kapazitätskiste barrel.
        // We cancel the event and handle the transfer ourselves so the kapa
        // chest data (not the real barrel inventory) receives the item.
        Inventory dest = event.getDestination();
        if (isKapaChestInv(dest, sim)) {
            KapaChestData kd = getKapaDataForInv(dest, sim);
            if (kd != null) {
                event.setCancelled(true);
                ItemStack moving = event.getItem().clone();
                moving.setAmount(1);
                // Check type compatibility
                if (kd.itemType != null && kd.itemType != moving.getType()) return;
                if (kd.count >= kd.maxCapacity) return;
                if (kd.itemType == null) kd.itemType = moving.getType();
                kd.count++;
                // Remove the item from the source inventory
                Inventory src = event.getSource();
                for (int i = 0; i < src.getSize(); i++) {
                    ItemStack s = src.getItem(i);
                    if (s != null && s.isSimilar(event.getItem())) {
                        s.setAmount(s.getAmount() - 1);
                        if (s.getAmount() <= 0) src.setItem(i, null);
                        break;
                    }
                }
                sim.saveAsync();
            }
        }
    }

    private boolean isTurboHopperInv(Inventory inv, SpecialItemsManager sim) {
        if (inv == null) return false;
        if (inv.getHolder() instanceof TurboHopperHolder) return true;
        if (inv.getHolder() instanceof org.bukkit.block.Hopper hopper) {
            return sim.isTurboHopper(hopper.getLocation());
        }
        return false;
    }

    private boolean isKapaChestInv(Inventory inv, SpecialItemsManager sim) {
        if (inv == null) return false;
        InventoryHolder holder = inv.getHolder();
        if (holder instanceof BlockState bs) {
            return bs.getType() == Material.BARREL
                    && sim.isKapaChest(bs.getLocation());
        }
        return false;
    }

    private KapaChestData getKapaDataForInv(Inventory inv, SpecialItemsManager sim) {
        InventoryHolder holder = inv.getHolder();
        if (holder instanceof BlockState bs) {
            return sim.getKapaChestData(bs.getLocation());
        }
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isChestLike(Material mat) {
        return mat == Material.CHEST
            || mat == Material.TRAPPED_CHEST
            || mat == Material.BARREL
            || mat.name().endsWith("_SHULKER_BOX");
    }
}
