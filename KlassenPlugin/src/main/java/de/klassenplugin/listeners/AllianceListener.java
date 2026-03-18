package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class AllianceListener implements Listener {
    private final KlassenPlugin plugin;
    public AllianceListener(KlassenPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("alliance.protect-pvp", true)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (plugin.getAllianceManager().areAllied(attacker.getUniqueId(), victim.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage(KlassenPlugin.colorizeComponent("&cDu kannst deinen Verbündeten nicht angreifen!"));
        }
    }
}
