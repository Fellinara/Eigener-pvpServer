package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Kit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitManager {

    private final DuellPlugin plugin;
    private final Map<String, Kit> kits;
    /** Names of kits that were loaded from / should be saved to kits.yml (admin-created). */
    private final Set<String> adminKitNames;
    private final File kitsFile;

    public KitManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.kits = new LinkedHashMap<>();
        this.adminKitNames = new LinkedHashSet<>();
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        loadDefaultKits();
        loadAdminKits();
    }

    private void loadDefaultKits() {
        Kit nodebuff = Kit.createNoDebuff();
        Kit debuff   = Kit.createDebuff();
        Kit classic  = Kit.createClassic();
        Kit gapple   = Kit.createGapple();
        Kit builduhc = Kit.createBuildUHC();
        Kit uhc      = Kit.createUHC();
        Kit combo    = Kit.createCombo();
        Kit mace      = Kit.createMace();
        Kit axe       = Kit.createAxe();
        Kit crystal   = Kit.createCrystal();
        Kit onlySword = Kit.createOnlySword();

        kits.put(nodebuff.getName(), nodebuff);
        kits.put(debuff.getName(),   debuff);
        kits.put(classic.getName(),  classic);
        kits.put(gapple.getName(),   gapple);
        kits.put(builduhc.getName(), builduhc);
        kits.put(uhc.getName(),      uhc);
        kits.put(combo.getName(),    combo);
        kits.put(mace.getName(),      mace);
        kits.put(axe.getName(),       axe);
        kits.put(crystal.getName(),   crystal);
        kits.put(onlySword.getName(), onlySword);

        plugin.getLogger().info(kits.size() + " Standard-Kits geladen.");
    }

    // ── Admin kit persistence ──────────────────────────────────────────────

    /** Loads admin-created kits from kits.yml. */
    public void loadAdminKits() {
        if (!kitsFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(kitsFile);
        ConfigurationSection section = config.getConfigurationSection("kits");
        if (section == null) return;

        int count = 0;
        for (String name : section.getKeys(false)) {
            try {
                ConfigurationSection cs = section.getConfigurationSection(name);
                if (cs == null) continue;
                String displayName = cs.getString("displayname", "§f" + name);
                String iconName = cs.getString("icon", "CHEST");
                Material icon;
                try { icon = Material.valueOf(iconName); } catch (Exception e) { icon = Material.CHEST; }
                String description = cs.getString("description", "§7Admin Kit");

                ItemStack[] armor = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    armor[i] = cs.getItemStack("armor." + i);
                }
                ItemStack[] contents = new ItemStack[36];
                for (int i = 0; i < 36; i++) {
                    contents[i] = cs.getItemStack("contents." + i);
                }
                ItemStack offhand = cs.getItemStack("offhand");

                Kit kit = new Kit(name.toLowerCase(), displayName, icon, description, armor, contents);
                if (offhand != null) kit.withOffHand(offhand);
                kits.put(name.toLowerCase(), kit);
                adminKitNames.add(name.toLowerCase());
                count++;
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Laden von Admin-Kit '" + name + "': " + e.getMessage());
            }
        }
        if (count > 0) plugin.getLogger().info(count + " Admin-Kits aus kits.yml geladen.");
    }

    /** Saves all admin-created kits to kits.yml. */
    public void saveAdminKits() {
        YamlConfiguration config = new YamlConfiguration();
        for (String name : adminKitNames) {
            Kit kit = kits.get(name);
            if (kit == null) continue;
            String path = "kits." + name;
            config.set(path + ".displayname", kit.getDisplayName());
            config.set(path + ".icon", kit.getIcon().name());
            config.set(path + ".description", kit.getDescription());
            ItemStack[] armor = kit.getArmor();
            for (int i = 0; i < armor.length; i++) {
                if (armor[i] != null) config.set(path + ".armor." + i, armor[i]);
            }
            ItemStack[] contents = kit.getContents();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null) config.set(path + ".contents." + i, contents[i]);
            }
            ItemStack offhand = kit.getOffHandItem();
            if (offhand != null) config.set(path + ".offhand", offhand);
        }
        try {
            config.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Admin-Kits: " + e.getMessage());
        }
    }

    /**
     * Adds an admin-created kit and persists it to kits.yml.
     * Cannot overwrite a built-in kit.
     * @return false if the name is reserved by a built-in kit
     */
    public boolean addAdminKit(Kit kit) {
        String name = kit.getName().toLowerCase();
        // Refuse to overwrite built-in kits
        if (kits.containsKey(name) && !adminKitNames.contains(name)) return false;
        kits.put(name, kit);
        adminKitNames.add(name);
        saveAdminKits();
        return true;
    }

    /**
     * Removes an admin-created kit by name.
     * @return false if the kit doesn't exist or is a built-in kit
     */
    public boolean removeAdminKit(String name) {
        name = name.toLowerCase();
        if (!adminKitNames.contains(name)) return false;
        kits.remove(name);
        adminKitNames.remove(name);
        saveAdminKits();
        return true;
    }

    public boolean isAdminKit(String name) {
        return adminKitNames.contains(name.toLowerCase());
    }

    // ── Public API ─────────────────────────────────────────────────────────

    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public Collection<Kit> getAllKits() {
        return kits.values();
    }

    public boolean kitExists(String name) {
        return kits.containsKey(name.toLowerCase());
    }
}
