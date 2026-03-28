package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages PvP kits (Klassen).
 * Each kit stores an array of {@link ItemStack}s that replace the player's inventory when claimed.
 * Kits are persisted in {@code kits.yml}.
 */
public class KitManager {

    private final KlassenPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    // kitName (lower-case) -> contents (inventory slots 0-35, armour slots 36-39, off-hand 40)
    private final Map<String, ItemStack[]> kits = new HashMap<>();

    public KitManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "kits.yml");
        loadKits();
    }

    @SuppressWarnings("unchecked")
    private void loadKits() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Fehler beim Erstellen von kits.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        org.bukkit.configuration.ConfigurationSection kitsSection = dataConfig.getConfigurationSection("kits");
        if (kitsSection == null) return;

        for (String kitName : kitsSection.getKeys(false)) {
            List<?> rawItems = dataConfig.getList("kits." + kitName + ".items");
            if (rawItems == null) continue;

            ItemStack[] items = new ItemStack[41];
            for (int i = 0; i < rawItems.size() && i < items.length; i++) {
                Object obj = rawItems.get(i);
                if (obj == null) {
                    items[i] = null;
                } else if (obj instanceof ItemStack item) {
                    items[i] = item;
                } else if (obj instanceof Map) {
                    try {
                        items[i] = ItemStack.deserialize((Map<String, Object>) obj);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Fehler beim Laden von Item-Slot " + i + " in Kit " + kitName);
                    }
                }
            }
            kits.put(kitName.toLowerCase(), items);
        }
    }

    public void saveKits() {
        dataConfig.set("kits", null);
        for (Map.Entry<String, ItemStack[]> entry : kits.entrySet()) {
            List<Object> serialized = new ArrayList<>();
            for (ItemStack item : entry.getValue()) {
                serialized.add(item != null ? item.serialize() : null);
            }
            dataConfig.set("kits." + entry.getKey() + ".items", serialized);
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Kits: " + e.getMessage());
        }
    }

    public void createKit(String name, ItemStack[] contents) {
        kits.put(name.toLowerCase(), contents.clone());
        saveKits();
    }

    public ItemStack[] getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public boolean hasKit(String name) {
        return kits.containsKey(name.toLowerCase());
    }

    public boolean deleteKit(String name) {
        boolean removed = kits.remove(name.toLowerCase()) != null;
        if (removed) saveKits();
        return removed;
    }

    public Set<String> getKitNames() {
        return kits.keySet();
    }
}
