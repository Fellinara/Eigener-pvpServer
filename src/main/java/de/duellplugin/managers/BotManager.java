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
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class BotManager {

    private static final int COBWEB_INTERVAL_TICKS = 100; // 5 seconds

    private final DuellPlugin plugin;
    private final Map<UUID, UUID> playerBotMap;
    private final Set<UUID> activeBots;
    private final Map<UUID, String> playerArenaMap;
    private final Map<UUID, BukkitRunnable> botAiTasks;
    private final Map<UUID, Integer> botLevelMap;

    public BotManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.playerBotMap = new HashMap<>();
        this.activeBots = new HashSet<>();
        this.playerArenaMap = new HashMap<>();
        this.botAiTasks = new HashMap<>();
        this.botLevelMap = new HashMap<>();
    }

    public void startBotFight(Player player, int level) {
        if (plugin.getDuellManager().isInDuel(player.getUniqueId())
                || isInBotFight(player.getUniqueId())) {
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
        Kit kit = plugin.getDuellManager().resolveKit(player.getUniqueId(), kitName);

        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        if (kit != null) {
            ItemStack[] contents = applySlotLayout(player, kit);
            player.getInventory().setStorageContents(contents);
            player.getInventory().setArmorContents(kit.getArmor());
        }
        ItemStack botOffHand = (kit != null && kit.getOffHandItem() != null)
                ? kit.getOffHandItem() : new ItemStack(Material.SHIELD);
        player.getInventory().setItemInOffHand(botOffHand);

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
                botLevelMap.put(botUUID, botLevel);

                Duel duel = new Duel(player.getUniqueId(), botUUID, arena.getName(), kitName);
                duel.setBotDuel(true);
                duel.setBotLevel(botLevel);
                duel.setState(Duel.DuelState.ACTIVE);

                startBotAi(bot, player, botLevel);

                String prefix = plugin.getPrefix();
                player.sendMessage(prefix + "§eBot-Kampf gestartet! §cLevel " + botLevel);
                player.sendTitle("§c⚔ KAMPF!", "§eBot Level " + botLevel, 10, 40, 10);
            }
        }.runTaskLater(plugin, 40L);
    }

    private void startBotAi(Zombie bot, Player player, int level) {
        // Arrow shoot interval: level 1-30 = 60 ticks (3s), 31-60 = 40 ticks, 61-100 = 20 ticks
        long shootInterval = level <= 30 ? 60L : (level <= 60 ? 40L : 20L);
        // Cobweb placement starts at level 20+ every 5 seconds
        boolean useCobwebs = level >= 20;
        // Golden apple healing: bot "heals" below 40% health at level 30+
        boolean useGapple = level >= 30;

        BukkitRunnable aiTask = new BukkitRunnable() {
            private int ticksSinceLastShot = 0;
            private int ticksSinceLastCobweb = 0;

            @Override
            public void run() {
                if (!bot.isValid() || bot.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }

                ticksSinceLastShot++;
                ticksSinceLastCobweb++;

                // Heal with golden apple effect when below 40% health
                if (useGapple) {
                    var maxHealthAttr = bot.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealthAttr != null) {
                        double healthPercent = bot.getHealth() / maxHealthAttr.getValue();
                        if (healthPercent < 0.4 && !bot.hasPotionEffect(PotionEffectType.REGENERATION)) {
                            bot.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, false, false));
                            bot.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0, false, false));
                        }
                    }
                }

                // Shoot arrow at player
                if (ticksSinceLastShot >= shootInterval) {
                    ticksSinceLastShot = 0;
                    double distance = bot.getLocation().distance(player.getLocation());
                    if (distance > 3.0 && distance < 30.0) {
                        Location eyeLoc = bot.getEyeLocation();
                        Location targetEye = player.getEyeLocation();
                        Vector direction = targetEye.toVector().subtract(eyeLoc.toVector());
                        if (direction.lengthSquared() > 0) {
                            direction.normalize().setY(direction.getY() + (distance * 0.025));
                            Arrow arrow = bot.getWorld().spawnArrow(eyeLoc, direction, 1.6f, 1.0f);
                            arrow.setShooter(bot);
                            arrow.setMetadata("duell_bot_arrow", new FixedMetadataValue(plugin, true));
                        }
                    }
                }

                // Place cobweb near player
                if (useCobwebs && ticksSinceLastCobweb >= COBWEB_INTERVAL_TICKS) {
                    ticksSinceLastCobweb = 0;
                    double distance = bot.getLocation().distance(player.getLocation());
                    if (distance < 6.0) {
                        Location cobwebLoc = player.getLocation().clone();
                        if (cobwebLoc.getWorld() != null
                                && cobwebLoc.getBlock().getType() == Material.AIR) {
                            cobwebLoc.getBlock().setType(Material.COBWEB);
                            // Remove cobweb after 3 seconds
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (cobwebLoc.getBlock().getType() == Material.COBWEB) {
                                        cobwebLoc.getBlock().setType(Material.AIR);
                                    }
                                }
                            }.runTaskLater(plugin, 60L);
                        }
                    }
                }
            }
        };
        aiTask.runTaskTimer(plugin, 20L, 1L);
        botAiTasks.put(bot.getUniqueId(), aiTask);
    }

    private void cancelBotAi(UUID botUUID) {
        BukkitRunnable task = botAiTasks.remove(botUUID);
        if (task != null) {
            task.cancel();
        }
    }

    private void configureBot(Zombie bot, int level) {
        bot.setBaby(false);
        bot.setRemoveWhenFarAway(false);
        bot.setCanPickupItems(false);

        String botName = getBotName(level);
        bot.setCustomName(botName);
        bot.setCustomNameVisible(true);

        bot.setMetadata("duell_bot", new FixedMetadataValue(plugin, true));
        bot.setMetadata("bot_level", new FixedMetadataValue(plugin, level));

        double healthMultiplier = 1.0 + (level - 1) * 0.3;
        double maxHealth = Math.min(20.0 * healthMultiplier, 200.0);
        var healthAttr = bot.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(maxHealth);
        }
        bot.setHealth(maxHealth);

        double damageMultiplier = 1.0 + (level - 1) * 0.08;
        var damageAttr = bot.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(Math.min(3.0 * damageMultiplier, 30.0));
        }

        double speedMultiplier = 1.0 + (level - 1) * 0.005;
        var speedAttr = bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(Math.min(0.23 * speedMultiplier, 0.45));
        }

        var armorAttr = bot.getAttribute(Attribute.GENERIC_ARMOR);
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
        cancelBotAi(botUUID);
        int botLevel = botLevelMap.getOrDefault(botUUID, 1);
        botLevelMap.remove(botUUID);

        UUID playerUUID = null;
        for (Map.Entry<UUID, UUID> entry : playerBotMap.entrySet()) {
            if (entry.getValue().equals(botUUID)) {
                playerUUID = entry.getKey();
                break;
            }
        }

        if (playerUUID != null) {
            playerBotMap.remove(playerUUID);
            plugin.getStatsManager().processBotWin(playerUUID, botLevel);

            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                String prefix = plugin.getPrefix();
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
            cancelBotAi(botUUID);
            botLevelMap.remove(botUUID);

            var entity = Bukkit.getEntity(botUUID);
            if (entity != null) {
                entity.remove();
            }

            plugin.getStatsManager().processBotLoss(player.getUniqueId());

            String prefix = plugin.getPrefix();
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
                plugin.getArenaManager().resetArena(arenaName);
            }
        }
    }

    public boolean isInBotFight(UUID uuid) {
        // playerArenaMap is set immediately on fight start; playerBotMap 40 ticks later.
        // Check both so block placement is allowed from the very first moment.
        return playerBotMap.containsKey(uuid) || playerArenaMap.containsKey(uuid);
    }

    /** Returns the arena name used by the given player's bot fight, or null. */
    public String getPlayerArenaName(UUID uuid) {
        return playerArenaMap.get(uuid);
    }

    public boolean isBot(UUID uuid) {
        return activeBots.contains(uuid);
    }

    public void cleanupBots() {
        for (UUID botUUID : new ArrayList<>(activeBots)) {
            cancelBotAi(botUUID);
            var entity = Bukkit.getEntity(botUUID);
            if (entity != null) {
                entity.remove();
            }
        }
        activeBots.clear();
        playerBotMap.clear();
        playerArenaMap.clear();
        botAiTasks.clear();
        botLevelMap.clear();
    }

    private ItemStack enchant(ItemStack item, Enchantment enchantment, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Returns kit contents rearranged according to the player's saved slot layout,
     * or the default contents if no layout is saved.
     */
    private ItemStack[] applySlotLayout(Player player, Kit kit) {
        var stats = plugin.getStatsManager().getStats(player.getUniqueId());
        if (stats == null) return kit.getContents();
        int[] layout = stats.getKitSlotLayout(kit.getName());
        if (layout == null) return kit.getContents();

        ItemStack[] defaultContents = kit.getContents();
        ItemStack[] result = new ItemStack[36];
        for (int src = 0; src < 36; src++) {
            if (defaultContents[src] == null) continue;
            int tgt = layout[src];
            if (tgt >= 0 && tgt < 36 && result[tgt] == null) {
                result[tgt] = defaultContents[src];
            } else {
                // Target slot occupied or invalid – find the first free slot
                boolean placed = false;
                for (int i = 0; i < 36; i++) {
                    if (result[i] == null) {
                        result[i] = defaultContents[src];
                        placed = true;
                        break;
                    }
                }
                if (!placed) result[src] = defaultContents[src];
            }
        }
        return result;
    }
}
