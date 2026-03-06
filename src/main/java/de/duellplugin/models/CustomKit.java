package de.duellplugin.models;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a player-created custom kit.
 * Custom kits are stored per-player and can only be created by VIP-ranked and higher players.
 */
public class CustomKit {

    private final String name;
    private final ItemStack[] contents;  // 36 inventory slots
    private final ItemStack[] armor;     // 4 armor slots (0=boots … 3=helmet)
    private final ItemStack offHandItem;

    public CustomKit(String name, ItemStack[] contents, ItemStack[] armor, ItemStack offHandItem) {
        this.name = name;
        this.contents = contents != null ? contents : new ItemStack[36];
        this.armor    = armor    != null ? armor    : new ItemStack[4];
        this.offHandItem = offHandItem;
    }

    public String getName() { return name; }

    public ItemStack[] getContents() { return contents.clone(); }

    public ItemStack[] getArmor() { return armor.clone(); }

    public ItemStack getOffHandItem() {
        return offHandItem != null ? offHandItem.clone() : null;
    }
}
