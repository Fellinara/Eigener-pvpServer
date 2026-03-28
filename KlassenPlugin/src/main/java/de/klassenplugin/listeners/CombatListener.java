package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Tags players in combat whenever one player damages another.
 *
 * <p>Handles both direct (melee) hits and indirect (projectile) hits so that
 * the combat tag is always applied regardless of whether the anti-cheat system
 * is enabled and regardless of OP / bypass permissions.
 */
public class CombatListener implements Listener {

    private final KlassenPlugin plugin;

    public CombatListener(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        // Resolve the attacker for both direct hits and projectile hits.
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }

        if (attacker == null) return;

        // Tag both players so neither can teleport away mid-fight.
        plugin.getCombatManager().tag(attacker.getUniqueId());
        plugin.getCombatManager().tag(victim.getUniqueId());
    }
}
