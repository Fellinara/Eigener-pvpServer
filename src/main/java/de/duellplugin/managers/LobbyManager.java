package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LobbyManager {

    private final DuellPlugin plugin;
    private Location lobbySpawn;
    private final Set<UUID> hiddenPlayers;

    public LobbyManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.hiddenPlayers = new HashSet<>();
        loadLobbySpawn();
    }

    public void sendToLobby(Player player) {
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setGameMode(GameMode.ADVENTURE);
        player.setFireTicks(0);
        player.setExp(0);
        player.setLevel(0);

        if (lobbySpawn != null) {
            player.teleport(lobbySpawn);
        } else if (!Bukkit.getWorlds().isEmpty()) {
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }

        giveLobbyItems(player);
    }

    public void giveLobbyItems(Player player) {
        player.getInventory().clear();

        player.getInventory().setItem(0, createItem(Material.DIAMOND_SWORD,
                "§6⚔ Duell-Menü", "§7Klicke um einen Spieler herauszufordern"));

        player.getInventory().setItem(1, createItem(Material.ZOMBIE_HEAD,
                "§c☠ Bot-Kampf", "§7Kämpfe gegen KI-Bots"));

        player.getInventory().setItem(2, createItem(Material.CHEST,
                "§e🎒 Kit-Auswahl", "§7Wähle dein Kit"));

        player.getInventory().setItem(3, createItem(Material.GRASS_BLOCK,
                "§a⚐ Arena-Auswahl", "§7Wähle eine Arena"));

        player.getInventory().setItem(4, createItem(Material.PAPER,
                "§b📊 Statistiken", "§7Zeige deine Statistiken"));

        player.getInventory().setItem(7, createItem(Material.ENDER_EYE,
                "§d👁 Spieler verstecken", "§7Verstecke/Zeige andere Spieler"));

        player.getInventory().setItem(8, createItem(Material.COMPASS,
                "§f🧭 Navigator", "§7Zurück zur Lobby"));
    }

    public void togglePlayerVisibility(Player player) {
        if (hiddenPlayers.contains(player.getUniqueId())) {
            hiddenPlayers.remove(player.getUniqueId());
            for (Player online : Bukkit.getOnlinePlayers()) {
                player.showPlayer(plugin, online);
            }
            player.sendMessage("§a§lSpieler sind jetzt sichtbar!");
            player.getInventory().setItem(7, createItem(Material.ENDER_EYE,
                    "§d👁 Spieler verstecken", "§7Verstecke andere Spieler"));
        } else {
            hiddenPlayers.add(player.getUniqueId());
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    player.hidePlayer(plugin, online);
                }
            }
            player.sendMessage("§c§lSpieler sind jetzt versteckt!");
            player.getInventory().setItem(7, createItem(Material.ENDER_PEARL,
                    "§d👁 Spieler zeigen", "§7Zeige andere Spieler"));
        }
    }

    public boolean arePlayersHidden(UUID uuid) {
        return hiddenPlayers.contains(uuid);
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(Location location) {
        this.lobbySpawn = location;
        saveLobbySpawn(location);
    }

    private void loadLobbySpawn() {
        String worldName = plugin.getConfig().getString("lobby.world", "world");
        var world = Bukkit.getWorld(worldName);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
            plugin.getLogger().warning("Lobby-Welt '" + worldName + "' nicht gefunden! Verwende Standard-Welt: " + world.getName());
        }
        if (world != null) {
            lobbySpawn = new Location(
                    world,
                    plugin.getConfig().getDouble("lobby.x", 0.5),
                    plugin.getConfig().getDouble("lobby.y", 100.0),
                    plugin.getConfig().getDouble("lobby.z", 0.5),
                    (float) plugin.getConfig().getDouble("lobby.yaw", 0.0),
                    (float) plugin.getConfig().getDouble("lobby.pitch", 0.0)
            );
        } else {
            plugin.getLogger().severe("Keine Welt gefunden! Lobby-Spawn konnte nicht gesetzt werden.");
        }
    }

    private void saveLobbySpawn(Location loc) {
        plugin.getConfig().set("lobby.world", loc.getWorld().getName());
        plugin.getConfig().set("lobby.x", loc.getX());
        plugin.getConfig().set("lobby.y", loc.getY());
        plugin.getConfig().set("lobby.z", loc.getZ());
        plugin.getConfig().set("lobby.yaw", loc.getYaw());
        plugin.getConfig().set("lobby.pitch", loc.getPitch());
        plugin.saveConfig();
    }

    private ItemStack createItem(Material material, String displayName, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (lore != null) {
                meta.setLore(Collections.singletonList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
