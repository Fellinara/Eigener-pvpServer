package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Arena;
import de.duellplugin.models.Duel;
import de.duellplugin.models.Kit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BotManager {

    private final DuellPlugin plugin;
    private final Map<UUID, UUID> playerBotMap;
    private final Set<UUID> activeBots;
    private final Map<UUID, String> playerArenaMap;

    public BotManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.playerBotMap = new HashMap<>();
        this.activeBots = new HashSet<>();
        this.playerArenaMap = new HashMap<>();
    }

    public void startBotFight(Player player, int level) {
        if (plugin.getDuellManager().isInDuel(player.getUniqueId())) {
            player.sendMessage("§cDu bist bereits in einem Kampf!");
            return;
        }

        Arena arena = plugin.getArenaManager().getAvailableArena();
        if (arena == null) {
            player.sendMessage("§cKeine Arena verfügbar!");
            return;
        }

        level = Math.max(1, Math.min(100, level));
        arena.setInUse(true);
        playerArenaMap.put(player.getUniqueId(), arena.getName());
        player.teleport(arena.getSpawn1());

        String kitName = plugin.getStatsManager()
                .getOrCreateStats(player.getUniqueId(), player.getName()).getSelectedKit();
        Kit kit = plugin.getKitManager().getKit(kitName);

        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        if (kit != null) {
            player.getInventory().setArmorContents(kit.getArmor());
            player.getInventory().setContents(kit.getContents());
        }

        final int botLevel = level;

        new BukkitRunnable() {
            @Override
            public void run() {
                Location spawnLoc = arena.getSpawn2();
                if (spawnLoc == null || spawnLoc.getWorld() == null) {
                    player.sendMessage("§cFehler: Arena-Spawn nicht gesetzt!");
                    arena.setInUse(false);
                    return;
                }

                Zombie bot = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
                configureBot(bot, botLevel);

                UUID botUUID = bot.getUniqueId();
                activeBots.add(botUUID);
                playerBotMap.put(player.getUniqueId(), botUUID);

                Duel duel = new Duel(player.getUniqueId(), botUUID, arena.getName(), kitName);
                duel.setBotDuel(true);
                duel.setBotLevel(botLevel);
                duel.setState(Duel.DuelState.ACTIVE);

                String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DuellPlugin§8] ");
                player.sendMessage(prefix + "§eBot-Kampf gestartet! §cLevel " + botLevel);
                player.sendTitle("§c⚔ KAMPF!", "§eBot Level " + botLevel, 10, 40, 10);
            }
        }.runTaskLater(plugin, 40L);
    }

    private void configureBot(Zombie bot, int level) {
        bot.setBaby(false);
        bot.setShouldBurnInDay(false);
        bot.setRemoveWhenFarAway(false);
        bot.setCanPickupItems(false);

        String botName = getBotName(level);
        bot.setCustomName(botName);
        bot.setCustomNameVisible(true);

        bot.setMetadata("duell_bot", new FixedMetadataValue(plugin, true));
        bot.setMetadata("bot_level", new FixedMetadataValue(plugin, level));

        double healthMultiplier = 1.0 + (level - 1) * 0.3;
        double maxHealth = Math.min(20.0 * healthMultiplier, 200.0);
        var healthAttr = bot.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(maxHealth);
        }
        bot.setHealth(maxHealth);

        double damageMultiplier = 1.0 + (level - 1) * 0.08;
        var damageAttr = bot.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(Math.min(3.0 * damageMultiplier, 30.0));
        }

        double speedMultiplier = 1.0 + (level - 1) * 0.005;
        var speedAttr = bot.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(Math.min(0.23 * speedMultiplier, 0.45));
        }

        var armorAttr = bot.getAttribute(Attribute.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(Math.min(level * 0.2, 20.0));
        }

        equipBot(bot, level);

        if (level >= 30) {
            int amplifier = Math.min((level - 30) / 20, 2);
            bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, amplifier, false, false));
        }
        if (level >= 50) {
            int amplifier = Math.min((level - 50) / 25, 2);
            bot.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, amplifier, false, false));
        }
        if (level >= 70) {
            bot.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        }
        if (level >= 90) {
            bot.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, false, false));
        }
    }

    private void equipBot(Zombie bot, int level) {
        if (level < 20) {
            bot.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
            bot.getEquipment().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
            bot.getEquipment().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
            bot.getEquipment().setBoots(new ItemStack(Material.LEATHER_BOOTS));
            bot.getEquipment().setItemInMainHand(new ItemStack(Material.WOODEN_SWORD));
        } else if (level < 40) {
            bot.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            bot.getEquipment().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
            bot.getEquipment().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
            bot.getEquipment().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
            bot.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
        } else if (level < 60) {
            bot.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            bot.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            bot.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            bot.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
            bot.getEquipment().setItemInMainHand(enchant(new ItemStack(Material.IRON_SWORD), Enchantment.SHARPNESS, 2));
        } else if (level < 80) {
            bot.getEquipment().setHelmet(enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION, 2));
            bot.getEquipment().setChestplate(enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 2));
            bot.getEquipment().setLeggings(enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION, 2));
            bot.getEquipment().setBoots(enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 2));
            bot.getEquipment().setItemInMainHand(enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 3));
        } else {
            bot.getEquipment().setHelmet(enchant(new ItemStack(Material.NETHERITE_HELMET), Enchantment.PROTECTION, 4));
            bot.getEquipment().setChestplate(enchant(new ItemStack(Material.NETHERITE_CHESTPLATE), Enchantment.PROTECTION, 4));
            bot.getEquipment().setLeggings(enchant(new ItemStack(Material.NETHERITE_LEGGINGS), Enchantment.PROTECTION, 4));
            bot.getEquipment().setBoots(enchant(new ItemStack(Material.NETHERITE_BOOTS), Enchantment.PROTECTION, 4));
            bot.getEquipment().setItemInMainHand(enchant(new ItemStack(Material.NETHERITE_SWORD), Enchantment.SHARPNESS, 5));
        }

        bot.getEquipment().setHelmetDropChance(0f);
        bot.getEquipment().setChestplateDropChance(0f);
        bot.getEquipment().setLeggingsDropChance(0f);
        bot.getEquipment().setBootsDropChance(0f);
        bot.getEquipment().setItemInMainHandDropChance(0f);
    }

    private String getBotName(int level) {
        String color;
        String rank;

        if (level <= 10) {
            color = "§7";
            rank = "Anfänger";
        } else if (level <= 25) {
            color = "§a";
            rank = "Lehrling";
        } else if (level <= 40) {
            color = "§e";
            rank = "Kämpfer";
        } else if (level <= 55) {
            color = "§6";
            rank = "Veteran";
        } else if (level <= 70) {
            color = "§c";
            rank = "Elite";
        } else if (level <= 85) {
            color = "§5";
            rank = "Meister";
        } else if (level <= 95) {
            color = "§d";
            rank = "Großmeister";
        } else {
            color = "§4";
            rank = "Legende";
        }

        return color + "⚔ Bot [Lv." + level + "] " + rank;
    }

    public void handleBotDeath(UUID botUUID) {
        activeBots.remove(botUUID);

        UUID playerUUID = null;
        for (Map.Entry<UUID, UUID> entry : playerBotMap.entrySet()) {
            if (entry.getValue().equals(botUUID)) {
                playerUUID = entry.getKey();
                break;
            }
        }

        if (playerUUID != null) {
            playerBotMap.remove(playerUUID);
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DuellPlugin§8] ");
                player.sendMessage(prefix + "§aDu hast den Bot besiegt!");
                player.sendTitle("§a§lSIEG!", "§eDu hast den Bot besiegt!", 10, 40, 10);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            plugin.getLobbyManager().sendToLobby(player);
                        }
                    }
                }.runTaskLater(plugin, 60L);
            }

            freeArenaForPlayer(playerUUID);
        }
    }

    public void handlePlayerDeathInBotFight(Player player) {
        UUID botUUID = playerBotMap.remove(player.getUniqueId());
        if (botUUID != null) {
            activeBots.remove(botUUID);

            var entity = Bukkit.getEntity(botUUID);
            if (entity != null) {
                entity.remove();
            }

            plugin.getStatsManager().processBotLoss(player.getUniqueId());

            String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DuellPlugin§8] ");
            player.sendMessage(prefix + "§cDu wurdest vom Bot besiegt!");

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        plugin.getLobbyManager().sendToLobby(player);
                    }
                }
            }.runTaskLater(plugin, 60L);

            freeArenaForPlayer(player.getUniqueId());
        }
    }

    private void freeArenaForPlayer(UUID playerUUID) {
        String arenaName = playerArenaMap.remove(playerUUID);
        if (arenaName != null) {
            Arena arena = plugin.getArenaManager().getArena(arenaName);
            if (arena != null) {
                arena.setInUse(false);
            }
        }
    }

    public boolean isInBotFight(UUID uuid) {
        return playerBotMap.containsKey(uuid);
    }

    public boolean isBot(UUID uuid) {
        return activeBots.contains(uuid);
    }

    public void cleanupBots() {
        for (UUID botUUID : new ArrayList<>(activeBots)) {
            var entity = Bukkit.getEntity(botUUID);
            if (entity != null) {
                entity.remove();
            }
        }
        activeBots.clear();
        playerBotMap.clear();
        playerArenaMap.clear();
    }

    private ItemStack enchant(ItemStack item, Enchantment enchantment, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
            item.setItemMeta(meta);
        }
        return item;
    }
}
