package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
        if (!(event.getEntity() instanceof Player victim)) return;

        // Resolve attacker: direct hit or projectile (bow, crossbow, trident).
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }

        if (attacker == null) return;
        if (plugin.getAllianceManager().areAllied(attacker.getUniqueId(), victim.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage(KlassenPlugin.colorizeComponent("&cDu kannst deinen Verbündeten nicht angreifen!"));
        }
    }
}
