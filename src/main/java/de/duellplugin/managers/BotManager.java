package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.Arena;
import de.duellplugin.models.Duel;
import de.duellplugin.models.Kit;
import de.duellplugin.models.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class BotManager {

    private static final int COBWEB_INTERVAL_TICKS = 100; // 5 seconds
    private static final int GAPPLE_ANIMATION_TICKS = 32; // 1.6 s (simulates eating animation)
    private static final int MACE_COOLDOWN_TICKS = 40;    // 2 s before switching back to primary

    private final DuellPlugin plugin;
    private final Map<UUID, UUID> playerBotMap;
    private final Set<UUID> activeBots;
    private final Map<UUID, String> playerArenaMap;
    private final Map<UUID, BukkitRunnable> botAiTasks;
    private final Map<UUID, Integer> botLevelMap;
    private final Set<UUID> blockingBots;

    public BotManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.playerBotMap = new HashMap<>();
        this.activeBots = new HashSet<>();
        this.playerArenaMap = new HashMap<>();
        this.botAiTasks = new HashMap<>();
        this.botLevelMap = new HashMap<>();
        this.blockingBots = new HashSet<>();
    }

    public void startBotFight(Player player) {
        if (plugin.getDuellManager().isInDuel(player.getUniqueId())
                || isInBotFight(player.getUniqueId())) {
            player.sendMessage("§cDu bist bereits in einem Kampf!");
            return;
        }

        String kitName = plugin.getStatsManager()
                .getOrCreateStats(player.getUniqueId(), player.getName()).getSelectedKit();
        Kit kit = plugin.getDuellManager().resolveKit(player.getUniqueId(), kitName);
        boolean needsCrystal = kit != null && kit.isCrystalOnly();

        Arena arena = plugin.getArenaManager().getAvailableArena(needsCrystal);
        if (arena == null) {
            player.sendMessage("§cKeine Arena verfügbar!");
            return;
        }

        // Read the player's adaptive bot rating as the bot's difficulty level
        int level = plugin.getStatsManager()
                .getOrCreateStats(player.getUniqueId(), player.getName()).getBotRating();

        arena.setInUse(true);
        playerArenaMap.put(player.getUniqueId(), arena.getName());
        player.teleport(arena.getSpawn1());

        plugin.getDuellManager().applyKit(player, kit);

        final int botLevel = level;
        final Kit botKit = kit;

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
                // Override bot's visual equipment with the player's kit items
                equipBotWithKit(bot, botKit, botLevel);

                UUID botUUID = bot.getUniqueId();
                activeBots.add(botUUID);
                playerBotMap.put(player.getUniqueId(), botUUID);
                botLevelMap.put(botUUID, botLevel);

                Duel duel = new Duel(player.getUniqueId(), botUUID, arena.getName(), kitName);
                duel.setBotDuel(true);
                duel.setBotLevel(botLevel);
                duel.setState(Duel.DuelState.ACTIVE);

                startBotAi(bot, player, botLevel, botKit);

                String prefix = plugin.getPrefix();
                player.sendMessage(prefix + "§eBot-Kampf gestartet! §7(Bot-Stärke: §6" + botLevel + "§7)");
                player.sendTitle("§c⚔ KAMPF!", "§eBot-Stärke: §6" + botLevel, 10, 40, 10);
            }
        }.runTaskLater(plugin, 40L);
    }

    private void startBotAi(Zombie bot, Player player, int level, Kit kit) {
        // Arrow shoot interval scales with level
        long shootInterval = level <= 30 ? 60L : (level <= 60 ? 40L : 20L);
        boolean useCobwebs = kitContainsMaterial(kit, Material.COBWEB);
        boolean hasGapple = kitContainsMaterial(kit, Material.GOLDEN_APPLE)
                || kitContainsMaterial(kit, Material.ENCHANTED_GOLDEN_APPLE);
        boolean hasPearls = kitContainsMaterial(kit, Material.ENDER_PEARL);
        boolean hasSplashPot = kitContainsMaterial(kit, Material.SPLASH_POTION)
                || kitContainsMaterial(kit, Material.LINGERING_POTION);
        boolean hasBow = kitContainsMaterial(kit, Material.BOW);

        // Identify primary weapon (slot 0) and optional mace for close-combat switching
        final ItemStack primaryWeapon = (kit != null && kit.getContents().length > 0
                && kit.getContents()[0] != null)
                ? kit.getContents()[0].clone() : new ItemStack(Material.DIAMOND_SWORD);
        ItemStack foundMace = null;
        if (kit != null) {
            for (ItemStack item : kit.getContents()) {
                if (item != null && item.getType() == Material.MACE) {
                    foundMace = item.clone();
                    break;
                }
            }
        }
        final ItemStack maceItem = foundMace;

        BukkitRunnable aiTask = new BukkitRunnable() {
            private int ticksSinceLastShot = 0;
            private int ticksSinceLastCobweb = 0;
            private int ticksSinceLastPearl = 0;
            private int ticksSinceLastPot = 0;
            private int ticksSinceLastGapple = 0;
            private int ticksSinceLastBlock = 0;
            private int blockingTicksLeft = 0;

            @Override
            public void run() {
                if (!bot.isValid() || bot.isDead() || !player.isOnline()) {
                    cancel();
                    blockingBots.remove(bot.getUniqueId());
                    return;
                }

                // Counters increment by 2 (task runs every 2 ticks)
                ticksSinceLastShot += 2;
                ticksSinceLastCobweb += 2;
                ticksSinceLastPearl += 2;
                ticksSinceLastPot += 2;
                ticksSinceLastGapple += 2;
                ticksSinceLastBlock += 2;

                var eq = bot.getEquipment();
                double distance = bot.getLocation().distance(player.getLocation());

                // ── Shield blocking simulation ────────────────────────────────
                if (blockingTicksLeft > 0) {
                    blockingTicksLeft -= 2;
                    if (blockingTicksLeft <= 0) {
                        blockingBots.remove(bot.getUniqueId());
                        if (eq != null && !bot.isDead()) eq.setItemInMainHand(primaryWeapon.clone());
                    }
                } else if (level >= 30 && ticksSinceLastBlock >= Math.max(60, 120 - level)) {
                    ticksSinceLastBlock = 0;
                    blockingTicksLeft = 10 + level / 10; // 11–20 ticks (0.55–1 s)
                    blockingBots.add(bot.getUniqueId());
                }

                // ── Golden apple heal with visual animation ───────────────────
                if (ticksSinceLastGapple >= 120) { // 6-second gapple cooldown
                    var maxHealthAttr = bot.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealthAttr != null) {
                        double healthPercent = bot.getHealth() / maxHealthAttr.getValue();
                        boolean wantsHeal = (hasGapple && healthPercent < 0.5)
                                || (!hasGapple && level >= 30 && healthPercent < 0.3);
                        if (wantsHeal && !bot.hasPotionEffect(PotionEffectType.REGENERATION)) {
                            ticksSinceLastGapple = 0;
                            // Visual: briefly show golden apple in main hand
                            if (hasGapple && eq != null && blockingTicksLeft <= 0) {
                                eq.setItemInMainHand(new ItemStack(Material.GOLDEN_APPLE));
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if (bot.isValid() && !bot.isDead() && eq != null)
                                            eq.setItemInMainHand(primaryWeapon.clone());
                                    }
                                }.runTaskLater(plugin, GAPPLE_ANIMATION_TICKS);
                                bot.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0, false, false));
                            }
                            bot.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, false, false));
                        }
                    }
                }

                // ── Potion use (strength / speed) ─────────────────────────────
                if (hasSplashPot && ticksSinceLastPot >= 200) {
                    ticksSinceLastPot = 0;
                    if (!bot.hasPotionEffect(PotionEffectType.STRENGTH))
                        bot.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 400, 0, false, false));
                    if (!bot.hasPotionEffect(PotionEffectType.SPEED))
                        bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 0, false, false));
                }

                // ── Shoot arrow (only if kit has bow; min. 4 blocks = outside mace range) ─
                if (hasBow && blockingTicksLeft <= 0 && ticksSinceLastShot >= shootInterval) {
                    ticksSinceLastShot = 0;
                    if (distance > 4.0 && distance < 30.0) {
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

                // ── Mace close-combat switch ──────────────────────────────────
                if (maceItem != null && distance < 3.0 && level >= 40 && blockingTicksLeft <= 0
                        && ticksSinceLastShot >= MACE_COOLDOWN_TICKS) {
                    if (eq != null) {
                        eq.setItemInMainHand(maceItem.clone());
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (bot.isValid() && !bot.isDead() && eq != null)
                                    eq.setItemInMainHand(primaryWeapon.clone());
                            }
                        }.runTaskLater(plugin, 20L);
                        ticksSinceLastShot = 0;
                    }
                }

                // ── Ender pearl teleport behind player ────────────────────────
                if (hasPearls && ticksSinceLastPearl >= 120 && distance > 8.0) {
                    ticksSinceLastPearl = 0;
                    Location playerLoc = player.getLocation();
                    Location teleportTo = playerLoc.clone().add(
                            playerLoc.getDirection().normalize().multiply(-1.5));
                    if (teleportTo.getWorld() != null) bot.teleport(teleportTo);
                }

                // ── Cobweb placement with hand animation + arena recording ────
                if (useCobwebs && blockingTicksLeft <= 0
                        && ticksSinceLastCobweb >= COBWEB_INTERVAL_TICKS && distance < 6.0) {
                    ticksSinceLastCobweb = 0;
                    Location cobwebLoc = player.getLocation().clone();
                    if (cobwebLoc.getWorld() != null
                            && cobwebLoc.getBlock().getType() == Material.AIR) {
                        // Visual: show cobweb in hand briefly
                        if (eq != null) {
                            eq.setItemInMainHand(new ItemStack(Material.COBWEB));
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (bot.isValid() && !bot.isDead() && eq != null)
                                        eq.setItemInMainHand(primaryWeapon.clone());
                                }
                            }.runTaskLater(plugin, 20L);
                        }
                        // Record for proper arena reset
                        UUID pUUID = getPlayerForBot(bot.getUniqueId());
                        if (pUUID != null) {
                            String arenaName = playerArenaMap.get(pUUID);
                            if (arenaName != null)
                                plugin.getArenaManager().addPlacedBlock(arenaName, cobwebLoc);
                        }
                        cobwebLoc.getBlock().setType(Material.COBWEB);
                        Location finalLoc = cobwebLoc;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (finalLoc.getBlock().getType() == Material.COBWEB)
                                    finalLoc.getBlock().setType(Material.AIR);
                            }
                        }.runTaskLater(plugin, 60L);
                    }
                }
            }
        };
        aiTask.runTaskTimer(plugin, 20L, 2L);
        botAiTasks.put(bot.getUniqueId(), aiTask);
    }

    /** Returns true if the given bot is currently simulating a shield block. */
    public boolean isBlocking(UUID botUUID) {
        return blockingBots.contains(botUUID);
    }

    /** Returns the player UUID whose bot fight this bot belongs to, or null. */
    private UUID getPlayerForBot(UUID botUUID) {
        for (Map.Entry<UUID, UUID> entry : playerBotMap.entrySet()) {
            if (entry.getValue().equals(botUUID)) return entry.getKey();
        }
        return null;
    }

    /** Returns true if the kit's main inventory contains at least one item of the given material. */
    private boolean kitContainsMaterial(Kit kit, Material material) {
        if (kit == null) return false;
        for (ItemStack item : kit.getContents()) {
            if (item != null && item.getType() == material) return true;
        }
        return false;
    }

    private void cancelBotAi(UUID botUUID) {
        BukkitRunnable task = botAiTasks.remove(botUUID);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Visually equips the bot with the same armor and main-hand weapon as the player's kit.
     * Level-based attributes (health, damage, speed) set by {@link #configureBot} are kept.
     */
    private void equipBotWithKit(Zombie bot, Kit kit, int level) {
        if (kit == null) return;
        var eq = bot.getEquipment();
        if (eq == null) return;

        // Armor: kit.getArmor() is [boots, leggings, chestplate, helmet]
        ItemStack[] armor = kit.getArmor();
        if (armor[0] != null) eq.setBoots(armor[0]);
        if (armor[1] != null) eq.setLeggings(armor[1]);
        if (armor[2] != null) eq.setChestplate(armor[2]);
        if (armor[3] != null) eq.setHelmet(armor[3]);

        // Main hand: use slot 0 of kit contents (the primary weapon)
        ItemStack[] contents = kit.getContents();
        if (contents[0] != null && contents[0].getType() != Material.AIR) {
            eq.setItemInMainHand(contents[0]);
        }

        // Offhand: use kit's designated offhand item (totem, shield, etc.)
        ItemStack offhand = kit.getOffHandItem();
        if (offhand != null) {
            eq.setItemInOffHand(offhand);
        } else {
            eq.setItemInOffHand(new ItemStack(Material.SHIELD));
        }

        // Ensure no drops
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
        eq.setItemInMainHandDropChance(0f);
        eq.setItemInOffHandDropChance(0f);
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

        // Always 10 hearts (20 HP) – same as a player; difficulty comes from AI, not inflated stats
        var healthAttr = bot.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(20.0);
        }
        bot.setHealth(20.0);

        // Scale attack damage with level; 2.0 HP (level 1) → ~8.0 HP (level 100, capped)
        double damageMultiplier = 1.0 + (level - 1) * 0.03;
        var damageAttr = bot.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(Math.min(2.0 * damageMultiplier, 8.0));
        }

        // Scale movement speed slightly with level; 0.23 (level 1) → ~0.32 (level 50+, capped)
        double speedMultiplier = 1.0 + (level - 1) * 0.002;
        var speedAttr = bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(Math.min(0.23 * speedMultiplier, 0.32));
        }

        // High-level bots get a permanent speed boost
        if (level >= 70) {
            bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        }
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
        blockingBots.remove(botUUID);
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
            Rank earned = plugin.getStatsManager().processBotWin(playerUUID);

            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                String prefix = plugin.getPrefix();
                player.sendMessage(prefix + "§aDu hast den Bot besiegt!");
                player.sendTitle("§a§lSIEG!", "§eDu hast den Bot besiegt!", 10, 40, 10);

                // Notify about an auto-earned rank
                if (earned != null) {
                    player.sendMessage(prefix + "§aGlückwunsch! Du hast den Rang §6"
                            + earned.getDisplayName() + " §adurch Bot-Siege freigeschaltet!");
                    player.setPlayerListName(earned.getDisplayName() + " §f" + player.getName());
                }

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
            blockingBots.remove(botUUID);
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
        blockingBots.clear();
    }

}
