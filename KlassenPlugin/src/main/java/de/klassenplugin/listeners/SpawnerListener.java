package de.klassenplugin.listeners;

import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

/**
 * Ensures that spawner items with a pre-configured mob type (set via
 * {@link BlockStateMeta}) retain their spawned entity type when placed
 * as a block in the world.
 *
 * <p>Without this listener a spawner item sold by the shop would default
 * to spawning pigs (Minecraft's built-in default) instead of the intended
 * mob type (Zombie or Skeleton).
 */
public class SpawnerListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawnerPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) return;

        ItemStack item = event.getItemInHand();
        if (!(item.getItemMeta() instanceof BlockStateMeta bsMeta)) return;
        if (!(bsMeta.getBlockState() instanceof CreatureSpawner itemCs)) return;

        EntityType spawnedType = itemCs.getSpawnedType();
        if (spawnedType == null || spawnedType == EntityType.PIG) return;

        // Apply the mob type from the item to the newly placed spawner block.
        if (event.getBlock().getState() instanceof CreatureSpawner blockCs) {
            blockCs.setSpawnedType(spawnedType);
            blockCs.update();
        }
    }
}
