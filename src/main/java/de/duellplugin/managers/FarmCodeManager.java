package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class FarmCodeManager {

    public record FarmCode(String displayName, List<ItemStack> items) {}

    private final DuellPlugin plugin;
    private final Map<String, FarmCode> codes;

    public FarmCodeManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.codes = new LinkedHashMap<>();
        registerCodes();
    }

    private void registerCodes() {
        // NoDebuff farming: splash healing potions
        FarmCode nodebuffCode = new FarmCode("NoDebuff Starter-Paket",
                List.of(
                        enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 4),
                        new ItemStack(Material.SPLASH_POTION, 32),
                        new ItemStack(Material.ENDER_PEARL, 16)
                ));
        codes.put("nodebuff", nodebuffCode);

        // Cobweb pack
        FarmCode cobwebCode = new FarmCode("Cobweb-Paket",
                List.of(new ItemStack(Material.COBWEB, 64)));
        codes.put("cobweb", cobwebCode);
        codes.put("cobwebs", cobwebCode);

        // Golden apple pack
        FarmCode gappleCode = new FarmCode("Golden Apple-Paket",
                List.of(
                        new ItemStack(Material.GOLDEN_APPLE, 64),
                        new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 3)
                ));
        codes.put("gapple", gappleCode);
        codes.put("goldapfel", gappleCode);

        // Arrow pack for bow kits
        FarmCode arrowCode = new FarmCode("Pfeil-Paket",
                List.of(new ItemStack(Material.ARROW, 64)));
        codes.put("arrows", arrowCode);
        codes.put("pfeile", arrowCode);

        // Ender pearl pack
        FarmCode pearlCode = new FarmCode("Ender-Pearl-Paket",
                List.of(new ItemStack(Material.ENDER_PEARL, 16)));
        codes.put("pearls", pearlCode);
        codes.put("perlen", pearlCode);

        // Classic kit farm pack
        codes.put("classic", new FarmCode("Classic Farm-Paket",
                List.of(
                        new ItemStack(Material.GOLDEN_APPLE, 8),
                        new ItemStack(Material.COBWEB, 16),
                        new ItemStack(Material.ARROW, 32),
                        new ItemStack(Material.ENDER_PEARL, 8)
                )));

        // BuildUHC materials
        FarmCode buildCode = new FarmCode("BuildUHC Material-Paket",
                List.of(
                        new ItemStack(Material.OAK_PLANKS, 64),
                        new ItemStack(Material.COBBLESTONE, 64),
                        new ItemStack(Material.LAVA_BUCKET),
                        new ItemStack(Material.WATER_BUCKET)
                ));
        codes.put("build", buildCode);
        codes.put("builduhc", buildCode);

        // Combo farming: speed potions + pearls
        codes.put("combo", new FarmCode("Combo Farm-Paket",
                List.of(
                        new ItemStack(Material.ENDER_PEARL, 16),
                        new ItemStack(Material.POTION, 4)
                )));

        // GommeHD fan code – all-in-one starter
        codes.put("gomme", new FarmCode("GommeHD Starter-Paket",
                List.of(
                        enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 4),
                        new ItemStack(Material.GOLDEN_APPLE, 16),
                        new ItemStack(Material.ENDER_PEARL, 16),
                        new ItemStack(Material.COBWEB, 16),
                        new ItemStack(Material.ARROW, 32)
                )));
    }

    public boolean redeemCode(Player player, String code) {
        FarmCode farmCode = codes.get(code.toLowerCase());
        if (farmCode == null) {
            return false;
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DuellPlugin§8] ");
        player.sendMessage(prefix + "§aFarmcode §e" + code.toUpperCase() + " §aeingelöst: §6" + farmCode.displayName());

        for (ItemStack item : farmCode.items()) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        return true;
    }

    public Set<String> getCodeNames() {
        return codes.keySet();
    }

    private static ItemStack enchant(ItemStack item, Enchantment enchantment, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
            item.setItemMeta(meta);
        }
        return item;
    }
}
