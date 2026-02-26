package de.duellplugin.managers;

import de.duellplugin.DuellPlugin;
import de.duellplugin.models.SkyWarsArena;
import de.duellplugin.models.SkyWarsGame;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SkyWarsManager {

    private final DuellPlugin plugin;
    private final Map<String, SkyWarsArena> arenas;
    private final Map<String, SkyWarsGame> games;         // arenaName → active game
    private final Map<UUID, String> playerArenaMap;       // playerUUID → arenaName
    private final Map<UUID, String> botArenaMap;          // botEntityUUID → arenaName
    private final Map<String, BukkitTask> countdownTasks; // arenaName → countdown task
    private final File arenasFile;

    private static final int VOID_Y_THRESHOLD = -20;
    private static final String BOT_METADATA = "skywars_bot";

    public SkyWarsManager(DuellPlugin plugin) {
        this.plugin = plugin;
        this.arenas = new LinkedHashMap<>();
        this.games = new HashMap<>();
        this.playerArenaMap = new HashMap<>();
        this.botArenaMap = new HashMap<>();
        this.countdownTasks = new HashMap<>();
        this.arenasFile = new File(plugin.getDataFolder(), "skywars-arenas.yml");
        loadArenas();
        startVoidWatcher();
    }

    // ── Arena CRUD ─────────────────────────────────────────────────────────

    public boolean createArena(String name) {
        if (arenas.containsKey(name.toLowerCase())) return false;
        arenas.put(name.toLowerCase(), new SkyWarsArena(name.toLowerCase()));
        saveArenas();
        return true;
    }

    public boolean deleteArena(String name) {
        SkyWarsArena arena = arenas.remove(name.toLowerCase());
        if (arena == null) return false;
        saveArenas();
        return true;
    }

    public SkyWarsArena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Collection<SkyWarsArena> getAllArenas() {
        return arenas.values();
    }

    public boolean addSpawn(String arenaName, Location loc) {
        SkyWarsArena arena = arenas.get(arenaName.toLowerCase());
        if (arena == null) return false;
        arena.addSpawn(loc);
        saveArenas();
        return true;
    }

    public boolean addChest(String arenaName, Location loc) {
        SkyWarsArena arena = arenas.get(arenaName.toLowerCase());
        if (arena == null) return false;
        arena.addChestLocation(loc);
        saveArenas();
        return true;
    }

    public boolean setMinPlayers(String arenaName, int n) {
        SkyWarsArena arena = arenas.get(arenaName.toLowerCase());
        if (arena == null) return false;
        arena.setMinPlayers(n);
        saveArenas();
        return true;
    }

    // ── Player join/leave ──────────────────────────────────────────────────

    /**
     * Joins the first available arena with a WAITING game, or creates one.
     * If arenaName is null, auto-selects.
     */
    public void joinGame(Player player, String arenaName) {
        if (playerArenaMap.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + "§cDu bist bereits in einer SkyWars-Arena!");
            return;
        }

        SkyWarsArena arena;
        if (arenaName != null) {
            arena = arenas.get(arenaName.toLowerCase());
            if (arena == null) {
                player.sendMessage(plugin.getPrefix() + "§cArena nicht gefunden!");
                return;
            }
        } else {
            // Auto-select: find waiting game or use first ready arena
            arena = arenas.values().stream()
                    .filter(a -> a.isReady() && !a.isInUse())
                    .findFirst().orElse(null);
            if (arena == null) {
                player.sendMessage(plugin.getPrefix() + "§cKeine freie SkyWars-Arena verfügbar!");
                return;
            }
        }

        if (arena.isInUse() && !games.containsKey(arena.getName())) {
            player.sendMessage(plugin.getPrefix() + "§cDiese Arena ist nicht verfügbar!");
            return;
        }

        // Get or create waiting game
        SkyWarsGame game = games.computeIfAbsent(arena.getName(),
                k -> new SkyWarsGame(arena.getName(), false));

        if (game.getState() != SkyWarsGame.State.WAITING) {
            player.sendMessage(plugin.getPrefix() + "§cDas Spiel in dieser Arena hat bereits begonnen!");
            return;
        }

        if (game.getPlayerCount() >= arena.getMaxPlayers()) {
            player.sendMessage(plugin.getPrefix() + "§cDiese Arena ist voll!");
            return;
        }

        game.addPlayer(player.getUniqueId());
        playerArenaMap.put(player.getUniqueId(), arena.getName());
        arena.setInUse(true);

        // TP to lobby/wait area near spawn 0
        Location waitLoc = arena.getSpawns().get(0).clone().add(0, 1, 0);
        player.teleport(waitLoc);
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();

        String msg = plugin.getPrefix() + "§6" + player.getName()
                + " §ehat die Arena §6" + arena.getName() + " §ebetreten! §7("
                + game.getPlayerCount() + "/" + arena.getMaxPlayers() + ")";
        broadcastToGame(game, msg);

        // Start countdown when enough players
        if (game.getPlayerCount() >= arena.getMinPlayers() && game.getState() == SkyWarsGame.State.WAITING) {
            startCountdown(arena.getName());
        }
    }

    public void leaveGame(Player player) {
        String arenaName = playerArenaMap.remove(player.getUniqueId());
        if (arenaName == null) {
            player.sendMessage(plugin.getPrefix() + "§cDu bist in keiner SkyWars-Arena!");
            return;
        }

        SkyWarsGame game = games.get(arenaName);
        if (game != null) {
            game.removePlayer(player.getUniqueId());
            broadcastToGame(game, plugin.getPrefix() + "§6" + player.getName() + " §chat die Arena verlassen!");

            if (game.getPlayerCount() == 0) {
                endGame(arenaName, null);
            } else if (game.getState() == SkyWarsGame.State.ACTIVE) {
                checkGameEnd(arenaName);
            } else if (game.getState() == SkyWarsGame.State.WAITING
                    && game.getPlayerCount() < getArena(arenaName).getMinPlayers()) {
                // Cancel countdown if not enough players
                cancelCountdown(arenaName);
            }
        }

        plugin.getLobbyManager().sendToLobby(player);
    }

    // ── Countdown + game start ─────────────────────────────────────────────

    private void startCountdown(String arenaName) {
        SkyWarsGame game = games.get(arenaName);
        if (game == null || game.getState() != SkyWarsGame.State.WAITING) return;

        game.setState(SkyWarsGame.State.COUNTDOWN);
        cancelCountdown(arenaName); // cancel any running task

        BukkitTask task = new BukkitRunnable() {
            int remaining = 30;

            @Override
            public void run() {
                SkyWarsGame g = games.get(arenaName);
                if (g == null || g.getState() == SkyWarsGame.State.ENDED) { cancel(); return; }

                if (remaining <= 0) {
                    cancel();
                    startGame(arenaName);
                    return;
                }
                if (remaining == 30 || remaining == 20 || remaining == 10 || remaining <= 5) {
                    broadcastToGame(g, plugin.getPrefix() + "§eSpiel startet in §6" + remaining + "§e Sekunden!");
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        countdownTasks.put(arenaName, task);
    }

    private void cancelCountdown(String arenaName) {
        BukkitTask t = countdownTasks.remove(arenaName);
        if (t != null) t.cancel();
        SkyWarsGame game = games.get(arenaName);
        if (game != null && game.getState() == SkyWarsGame.State.COUNTDOWN) {
            game.setState(SkyWarsGame.State.WAITING);
        }
    }

    /** Admin force-start or auto-start after countdown. */
    public void startGame(String arenaName) {
        SkyWarsGame game = games.get(arenaName);
        SkyWarsArena arena = arenas.get(arenaName);
        if (game == null || arena == null) return;

        cancelCountdown(arenaName);
        game.setState(SkyWarsGame.State.ACTIVE);

        List<UUID> players = new ArrayList<>(game.getPlayerList());
        List<Location> spawns = arena.getSpawns();

        // Teleport each player to their spawn island
        for (int i = 0; i < players.size(); i++) {
            Player p = Bukkit.getPlayer(players.get(i));
            if (p == null) continue;
            Location spawnLoc = spawns.get(i % spawns.size());
            p.teleport(spawnLoc);
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.setHealth(20.0);
            p.setFoodLevel(20);
            // Give starter kit
            giveStarterKit(p);
        }

        // Fill all chest blocks with random loot
        fillChests(game, arena);

        broadcastToGame(game, plugin.getPrefix() + "§a§lSkyWars gestartet! Viel Erfolg!");
        for (UUID uid : game.getPlayerList()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) p.sendTitle("§a§lSKYWARS!", "§eDu hast 30 Sekunden Schutz!", 10, 60, 10);
        }
    }

    // ── Bot game ───────────────────────────────────────────────────────────

    public void startBotGame(Player player, String arenaName, int numBots) {
        if (playerArenaMap.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + "§cDu bist bereits in einer Arena!");
            return;
        }

        SkyWarsArena arena = arenas.get(arenaName.toLowerCase());
        if (arena == null || !arena.isReady()) {
            player.sendMessage(plugin.getPrefix() + "§cArena nicht bereit!");
            return;
        }
        if (arena.isInUse()) {
            player.sendMessage(plugin.getPrefix() + "§cDiese Arena ist bereits in Benutzung!");
            return;
        }

        int botCount = Math.min(numBots, arena.getSpawns().size() - 1);
        if (botCount < 1) {
            player.sendMessage(plugin.getPrefix() + "§cNicht genug Spawn-Punkte für Bots!");
            return;
        }

        arena.setInUse(true);
        SkyWarsGame game = new SkyWarsGame(arena.getName(), true);
        game.addPlayer(player.getUniqueId());
        game.setState(SkyWarsGame.State.ACTIVE);
        games.put(arena.getName(), game);
        playerArenaMap.put(player.getUniqueId(), arena.getName());

        // Teleport player to spawn 0
        player.teleport(arena.getSpawns().get(0));
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        giveStarterKit(player);

        // Spawn bots at remaining spawns
        List<Location> spawns = arena.getSpawns();
        for (int i = 1; i <= botCount && i < spawns.size(); i++) {
            Location spawnLoc = spawns.get(i);
            if (spawnLoc.getWorld() == null) continue;
            Zombie bot = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
            configureSkywarsBot(bot, game.getPlayerCount() + i);
            game.addBot(bot.getUniqueId());
            botArenaMap.put(bot.getUniqueId(), arena.getName());
        }

        // Fill chests
        fillChests(game, arena);

        player.sendMessage(plugin.getPrefix() + "§aSkyWars Bot-Spiel gestartet mit §6" + botCount + " §aBots!");
        player.sendTitle("§a§lSKYWARS!", "§eBesiege alle §6" + botCount + " §eBots!", 10, 60, 10);
    }

    // ── Death handling ─────────────────────────────────────────────────────

    public void handlePlayerDeath(Player player, Player killer) {
        String arenaName = playerArenaMap.get(player.getUniqueId());
        if (arenaName == null) return;
        SkyWarsGame game = games.get(arenaName);
        if (game == null) return;

        game.eliminatePlayer(player.getUniqueId());

        String msg = plugin.getPrefix() + "§6" + player.getName() + " §cist ausgeschieden!";
        if (killer != null && !killer.getUniqueId().equals(player.getUniqueId())) {
            msg += " §7(von §6" + killer.getName() + "§7)";
        }
        broadcastToGame(game, msg);

        // Respawn as spectator
        final Player deadPlayer = player;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!deadPlayer.isOnline()) return;
            if (deadPlayer.isDead()) deadPlayer.spigot().respawn();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (deadPlayer.isOnline()) {
                    deadPlayer.setGameMode(GameMode.SPECTATOR);
                    deadPlayer.sendMessage(plugin.getPrefix() + "§cDu bist ausgeschieden! Warte auf das Spielende.");
                }
            });
        }, 1L);

        checkGameEnd(arenaName);
    }

    public void handleBotDeath(UUID botUUID) {
        String arenaName = botArenaMap.remove(botUUID);
        if (arenaName == null) return;
        SkyWarsGame game = games.get(arenaName);
        if (game == null) return;

        game.removeBot(botUUID);
        broadcastToGame(game, plugin.getPrefix() + "§7Ein Bot wurde besiegt!");
        checkGameEnd(arenaName);
    }

    public void handleDisconnect(UUID playerUUID) {
        String arenaName = playerArenaMap.remove(playerUUID);
        if (arenaName == null) return;
        SkyWarsGame game = games.get(arenaName);
        if (game == null) return;

        game.removePlayer(playerUUID);
        if (game.getState() == SkyWarsGame.State.ACTIVE) {
            checkGameEnd(arenaName);
        } else if (game.getPlayerCount() == 0) {
            endGame(arenaName, null);
        }
    }

    // ── Game end ───────────────────────────────────────────────────────────

    private void checkGameEnd(String arenaName) {
        SkyWarsGame game = games.get(arenaName);
        if (game == null || game.getState() != SkyWarsGame.State.ACTIVE) return;

        long aliveHumans = game.getAliveHumanCount();
        long aliveBots = game.getAliveBotCount();

        if (aliveHumans == 0) {
            // All humans eliminated
            endGame(arenaName, null);
        } else if (aliveHumans == 1 && aliveBots == 0) {
            // Last one standing wins
            UUID winner = game.getAlivePlayers().stream()
                    .filter(u -> !game.getBotUuids().contains(u))
                    .findFirst().orElse(null);
            endGame(arenaName, winner);
        }
    }

    private void endGame(String arenaName, UUID winnerUUID) {
        SkyWarsGame game = games.remove(arenaName);
        SkyWarsArena arena = arenas.get(arenaName);
        if (game == null) return;

        cancelCountdown(arenaName);
        game.setState(SkyWarsGame.State.ENDED);

        String winnerName = "Niemand";
        if (winnerUUID != null) {
            Player winner = Bukkit.getPlayer(winnerUUID);
            winnerName = winner != null ? winner.getName() : winnerUUID.toString();
            if (winner != null) {
                winner.sendTitle("§6§lSIEG!", "§eDu hast SkyWars gewonnen!", 10, 60, 10);
                plugin.getStatsManager().processBotWin(winnerUUID); // grant ELO for winning
            }
        }

        // Kill all remaining bots
        for (UUID botUUID : game.getBotUuids()) {
            var entity = Bukkit.getEntity(botUUID);
            if (entity != null) entity.remove();
        }
        botArenaMap.values().removeIf(a -> a.equals(arenaName));

        String finalWinnerName = winnerName;
        broadcastToAll(plugin.getPrefix() + "§6SkyWars " + arenaName + "§e: §6"
                + finalWinnerName + " §ehat gewonnen!");

        // Send all players to lobby after delay
        List<UUID> allPlayers = new ArrayList<>(game.getPlayerList());
        for (UUID uid : allPlayers) {
            playerArenaMap.remove(uid);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uid : allPlayers) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null && p.isOnline()) {
                    plugin.getLobbyManager().sendToLobby(p);
                }
            }
            if (arena != null) arena.setInUse(false);
        }, 100L);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    public boolean isInSkyWars(UUID uuid) {
        return playerArenaMap.containsKey(uuid);
    }

    public String getPlayerArena(UUID uuid) {
        return playerArenaMap.get(uuid);
    }

    public SkyWarsGame getGameForPlayer(UUID uuid) {
        String arenaName = playerArenaMap.get(uuid);
        return arenaName != null ? games.get(arenaName) : null;
    }

    public boolean isSkyWarsBot(UUID botUUID) {
        return botArenaMap.containsKey(botUUID);
    }

    private void broadcastToGame(SkyWarsGame game, String message) {
        for (UUID uid : game.getPlayerList()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) p.sendMessage(message);
        }
    }

    private void broadcastToAll(String message) {
        Bukkit.broadcastMessage(message);
    }

    /** Gives a minimal starter kit to a skywars player (they must loot chests for better gear). */
    private void giveStarterKit(Player player) {
        player.getInventory().clear();
        // Wooden sword + 5 apples + 32 blocks
        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
        ItemStack food = new ItemStack(Material.APPLE, 5);
        ItemStack blocks = new ItemStack(Material.OAK_PLANKS, 32);
        player.getInventory().setItem(0, sword);
        player.getInventory().setItem(1, food);
        player.getInventory().setItem(2, blocks);
    }

    /** Pre-fills all registered chest blocks with random tiered loot. */
    private void fillChests(SkyWarsGame game, SkyWarsArena arena) {
        for (Location loc : arena.getChestLocations()) {
            if (loc.getWorld() == null) continue;
            Block block = loc.getBlock();
            if (block.getState() instanceof Chest chest) {
                Inventory inv = chest.getInventory();
                inv.clear();
                List<ItemStack> loot = generateLoot();
                // Shuffle available slot indices and place items in them to avoid collisions
                List<Integer> slots = new ArrayList<>();
                for (int i = 0; i < inv.getSize(); i++) slots.add(i);
                Collections.shuffle(slots, new Random());
                for (int i = 0; i < Math.min(loot.size(), slots.size()); i++) {
                    inv.setItem(slots.get(i), loot.get(i));
                }
            }
        }
    }

    /** Generates a random list of loot items (2–6 items) using tiered drop tables. */
    private List<ItemStack> generateLoot() {
        Random rng = new Random();
        List<ItemStack> loot = new ArrayList<>();

        // Always: 1–2 food + 1 weapon
        loot.add(new ItemStack(Material.BREAD, 3 + rng.nextInt(5)));
        addWeapon(loot, rng);
        addArmor(loot, rng);

        // 60%: arrows or bow
        if (rng.nextDouble() < 0.6) loot.add(new ItemStack(Material.ARROW, 8 + rng.nextInt(16)));
        if (rng.nextDouble() < 0.3) loot.add(new ItemStack(Material.BOW));

        // 40%: golden apple
        if (rng.nextDouble() < 0.4) loot.add(new ItemStack(Material.GOLDEN_APPLE, 1 + rng.nextInt(2)));

        // 20%: ender pearls
        if (rng.nextDouble() < 0.2) loot.add(new ItemStack(Material.ENDER_PEARL, 4));

        // 10%: potion
        if (rng.nextDouble() < 0.1) {
            org.bukkit.inventory.ItemStack pot = new org.bukkit.inventory.ItemStack(Material.POTION);
            org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) pot.getItemMeta();
            if (meta != null) {
                meta.setBasePotionType(org.bukkit.potion.PotionType.HEALING);
                pot.setItemMeta(meta);
            }
            loot.add(pot);
        }

        return loot;
    }

    private void addWeapon(List<ItemStack> loot, Random rng) {
        double roll = rng.nextDouble();
        ItemStack weapon;
        if (roll < 0.10) {
            weapon = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 1);
        } else if (roll < 0.35) {
            weapon = new ItemStack(Material.IRON_SWORD);
        } else {
            weapon = new ItemStack(Material.STONE_SWORD);
        }
        loot.add(weapon);
    }

    private void addArmor(List<ItemStack> loot, Random rng) {
        double roll = rng.nextDouble();
        if (roll < 0.15) {
            loot.add(new ItemStack(Material.IRON_CHESTPLATE));
            loot.add(new ItemStack(Material.IRON_HELMET));
        } else if (roll < 0.50) {
            loot.add(new ItemStack(Material.LEATHER_CHESTPLATE));
            loot.add(new ItemStack(Material.LEATHER_HELMET));
        }
        // 35%: no armor in this chest
    }

    private ItemStack enchant(ItemStack item, Enchantment ench, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.addEnchant(ench, level, true); item.setItemMeta(meta); }
        return item;
    }

    /** Configures a bot zombie for SkyWars with scaling difficulty. */
    private void configureSkywarsBot(Zombie bot, int index) {
        bot.setBaby(false);
        bot.setRemoveWhenFarAway(false);
        bot.setCanPickupItems(false);
        bot.setCustomName("§c⚔ SkyWars Bot #" + index);
        bot.setCustomNameVisible(true);
        bot.setMetadata(BOT_METADATA, new FixedMetadataValue(plugin, true));

        var healthAttr = bot.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) healthAttr.setBaseValue(20.0);
        bot.setHealth(20.0);

        // Give bot iron sword + leather armor
        var eq = bot.getEquipment();
        if (eq != null) {
            eq.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            eq.setHelmet(new ItemStack(Material.LEATHER_HELMET));
            eq.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
            eq.setItemInMainHandDropChance(0f);
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
        }
    }

    /** Periodic task: removes players/bots that fall below VOID_Y_THRESHOLD. */
    private void startVoidWatcher() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, String> e : new HashMap<>(playerArenaMap).entrySet()) {
                    Player p = Bukkit.getPlayer(e.getKey());
                    if (p == null) continue;
                    SkyWarsGame game = games.get(e.getValue());
                    if (game == null || game.getState() != SkyWarsGame.State.ACTIVE) continue;
                    if (p.getLocation().getY() < VOID_Y_THRESHOLD && game.isAlive(p.getUniqueId())) {
                        p.damage(1000.0); // instant kill → triggers PlayerDeathEvent
                    }
                }
                // Also check bots
                for (Map.Entry<UUID, String> e : new HashMap<>(botArenaMap).entrySet()) {
                    var entity = Bukkit.getEntity(e.getKey());
                    if (entity == null) continue;
                    if (entity.getLocation().getY() < VOID_Y_THRESHOLD) {
                        entity.remove();
                        handleBotDeath(e.getKey());
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ── Persistence ────────────────────────────────────────────────────────

    public void saveArenas() {
        FileConfiguration config = new YamlConfiguration();
        for (SkyWarsArena arena : arenas.values()) {
            String path = "arenas." + arena.getName();
            config.set(path + ".min-players", arena.getMinPlayers());
            config.set(path + ".max-players", arena.getMaxPlayers());

            List<Map<String, Object>> spawnList = new ArrayList<>();
            for (Location loc : arena.getSpawns()) {
                spawnList.add(serializeLocation(loc));
            }
            config.set(path + ".spawns", spawnList);

            List<Map<String, Object>> chestList = new ArrayList<>();
            for (Location loc : arena.getChestLocations()) {
                chestList.add(serializeLocation(loc));
            }
            config.set(path + ".chests", chestList);
        }
        try {
            config.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der SkyWars-Arenen: " + e.getMessage());
        }
    }

    private void loadArenas() {
        if (!arenasFile.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(arenasFile);
        ConfigurationSection section = config.getConfigurationSection("arenas");
        if (section == null) return;

        for (String name : section.getKeys(false)) {
            ConfigurationSection as = section.getConfigurationSection(name);
            if (as == null) continue;

            SkyWarsArena arena = new SkyWarsArena(name);
            arena.setMinPlayers(as.getInt("min-players", 5));
            arena.setMaxPlayers(as.getInt("max-players", 12));

            List<?> spawnList = as.getList("spawns", new ArrayList<>());
            for (Object obj : spawnList) {
                if (obj instanceof Map<?, ?> map) {
                    Location loc = deserializeLocation(map);
                    if (loc != null) arena.addSpawn(loc);
                }
            }

            List<?> chestList = as.getList("chests", new ArrayList<>());
            for (Object obj : chestList) {
                if (obj instanceof Map<?, ?> map) {
                    Location loc = deserializeLocation(map);
                    if (loc != null) arena.addChestLocation(loc);
                }
            }

            arenas.put(name, arena);
        }
        plugin.getLogger().info(arenas.size() + " SkyWars-Arenen geladen.");
    }

    private Map<String, Object> serializeLocation(Location loc) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (loc.getWorld() != null) map.put("world", loc.getWorld().getName());
        map.put("x", loc.getX());
        map.put("y", loc.getY());
        map.put("z", loc.getZ());
        map.put("yaw", (double) loc.getYaw());
        map.put("pitch", (double) loc.getPitch());
        return map;
    }

    private Location deserializeLocation(Map<?, ?> map) {
        try {
            String worldName = (String) map.get("world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) return null;
            double x = ((Number) map.get("x")).doubleValue();
            double y = ((Number) map.get("y")).doubleValue();
            double z = ((Number) map.get("z")).doubleValue();
            float yaw = ((Number) map.getOrDefault("yaw", 0.0)).floatValue();
            float pitch = ((Number) map.getOrDefault("pitch", 0.0)).floatValue();
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }
}
