package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Kit;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class KitManager {

    private final DuellPlugin plugin;
    private final Map<String, Kit> kits;

    public KitManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.kits = new LinkedHashMap<>();
        loadDefaultKits();
    }

    private void loadDefaultKits() {
        Kit nodebuff = Kit.createNoDebuff();
        Kit debuff = Kit.createDebuff();
        Kit classic = Kit.createClassic();
        Kit gapple = Kit.createGapple();
        Kit builduhc = Kit.createBuildUHC();
        Kit combo = Kit.createCombo();

        kits.put(nodebuff.getName(), nodebuff);
        kits.put(debuff.getName(), debuff);
        kits.put(classic.getName(), classic);
        kits.put(gapple.getName(), gapple);
        kits.put(builduhc.getName(), builduhc);
        kits.put(combo.getName(), combo);

        plugin.getLogger().info(kits.size() + " Kits geladen.");
    }

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
