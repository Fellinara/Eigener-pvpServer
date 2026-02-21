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
        Kit swordsman = Kit.createSwordsman();
        Kit archer = Kit.createArcher();
        Kit tank = Kit.createTank();
        Kit berserker = Kit.createBerserker();
        Kit alchemist = Kit.createAlchemist();
        Kit knight = Kit.createKnight();

        kits.put(swordsman.getName(), swordsman);
        kits.put(archer.getName(), archer);
        kits.put(tank.getName(), tank);
        kits.put(berserker.getName(), berserker);
        kits.put(alchemist.getName(), alchemist);
        kits.put(knight.getName(), knight);

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
