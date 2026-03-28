package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Hopper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages the three special custom items:
 * <ul>
 *   <li><b>Verkaufsaxt</b> — breaks a chest and auto-sells its contents; chest respawns instantly.</li>
 *   <li><b>Turbo-Hopper</b> — 54-slot hopper that transfers items at vanilla speed (1 item / 8 ticks), supports all facing directions.</li>
 *   <li><b>Kapazitätskiste</b> — bulk single-type storage; starts at 10 000 items, upgradeable to 2 000 000.</li>
 * </ul>
 */
public class SpecialItemsManager {

    // ── Type constants ────────────────────────────────────────────────────────
    public static final String TYPE_VERKAUFSAXT      = "VERKAUFSAXT";
    public static final String TYPE_TURBO_HOPPER     = "TURBO_HOPPER";
    public static final String TYPE_KAPAZITAETSKISTE = "KAPAZITAETSKISTE";

    public static final Set<String> SHOP_KEYS = Set.of(
            TYPE_VERKAUFSAXT, TYPE_TURBO_HOPPER, TYPE_KAPAZITAETSKISTE);

    public static final long   KAPA_DEFAULT_CAPACITY = 10_000L;
    public static final long   KAPA_MAX_CAPACITY     = 2_000_000L;
    public static final double KAPA_UPGRADE_COST     = 1_000_000.0;

    /** PDC key used to tag all special items. */
    private static final NamespacedKey SPECIAL_KEY =
            java.util.Objects.requireNonNull(
                    NamespacedKey.fromString("klassenplugin:special_item"),
                    "NamespacedKey 'klassenplugin:special_item' must not be null");

    // ── State ─────────────────────────────────────────────────────────────────
    private final KlassenPlugin plugin;
    private final Map<Location, TurboHopperHolder> turboHoppers = new HashMap<>();
    private final Map<Location, KapaChestData>     kapaChests   = new HashMap<>();
    private final File dataFile;

    public SpecialItemsManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "special_items.yml");
        load();
        startTurboScheduler();
    }

    // ── Static helpers: shop display ──────────────────────────────────────────

    public static boolean isSpecialItemShopKey(String key) {
        return SHOP_KEYS.contains(key);
    }

    public static String getShopDisplayName(String key) {
        return switch (key) {
            case TYPE_VERKAUFSAXT      -> "Verkaufsaxt";
            case TYPE_TURBO_HOPPER     -> "Turbo-Hopper";
            case TYPE_KAPAZITAETSKISTE -> "Kapazitätskiste";
            default                    -> key;
        };
    }

    // ── Static item builders ──────────────────────────────────────────────────

    /** Creates the Verkaufsaxt ItemStack with all PDC tags and enchantments. */
    public static ItemStack buildVerkaufsaxt() {
        ItemStack item = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(c("&5✦ &d&lVerkaufsaxt &5✦"));
        meta.lore(List.of(
                c(""),
                c("&7Breche eine &bKiste &7auf, um den"),
                c("&7Inhalt automatisch zu &averkaufen&7."),
                c(""),
                c("&8⟳ Kiste respawnt &esofort&8."),
                c(""),
                c("&5&l✦ LEGENDÄR ✦")));
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(SPECIAL_KEY, PersistentDataType.STRING, TYPE_VERKAUFSAXT);
        item.setItemMeta(meta);
        return item;
    }

    /** Creates the Turbo-Hopper ItemStack. */
    public static ItemStack buildTurboHopper() {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(c("&b⚡ &3&lTurbo-Hopper &b⚡"));
        meta.lore(List.of(
                c(""),
                c("&7Speichert wie eine &bDoppelkiste &8(54 Slots)&7."),
                c("&7Funktioniert wie ein normaler Hopper,"),
                c("&7aber mit viel mehr Platz."),
                c(""),
                c("&3&l⚡ SELTEN ⚡")));
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(SPECIAL_KEY, PersistentDataType.STRING, TYPE_TURBO_HOPPER);
        item.setItemMeta(meta);
        return item;
    }

    /** Creates the Kapazitätskiste ItemStack (uses Barrel material for placement). */
    public static ItemStack buildKapaChest() {
        ItemStack item = new ItemStack(Material.BARREL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(c("&6📦 &e&lKapazitätskiste &6📦"));
        meta.lore(List.of(
                c(""),
                c("&7Speichert bis zu &e10.000 &7Items eines Typs."),
                c("&7Upgradebar bis zu &62.000.000 &7Items."),
                c(""),
                c("&6&l◆ SELTEN ◆")));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(SPECIAL_KEY, PersistentDataType.STRING, TYPE_KAPAZITAETSKISTE);
        item.setItemMeta(meta);
        return item;
    }

    /** Builds the correct item for a shop key. */
    public static ItemStack buildByShopKey(String key) {
        return switch (key) {
            case TYPE_VERKAUFSAXT      -> buildVerkaufsaxt();
            case TYPE_TURBO_HOPPER     -> buildTurboHopper();
            case TYPE_KAPAZITAETSKISTE -> buildKapaChest();
            default                    -> new ItemStack(Material.BARRIER);
        };
    }

    // ── PDC helpers ───────────────────────────────────────────────────────────

    /** Returns {@code true} if the item carries a special-item PDC tag. */
    public static boolean isSpecialItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                   .has(SPECIAL_KEY, PersistentDataType.STRING);
    }

    /** Returns {@code true} if the item is a specific special type. */
    public static boolean isSpecialItem(ItemStack item, String type) {
        if (!isSpecialItem(item)) return false;
        return type.equals(item.getItemMeta().getPersistentDataContainer()
                               .get(SPECIAL_KEY, PersistentDataType.STRING));
    }

    // ── Turbo Hopper – registration ────────────────────────────────────────

    public void registerTurboHopper(Location loc) {
        Location key = normalise(loc);
        TurboHopperHolder holder = new TurboHopperHolder(key);
        Inventory inv = Bukkit.createInventory(holder, 54, c("&b⚡ &3&lTurbo-Hopper &b⚡"));
        holder.inventory = inv;
        turboHoppers.put(key, holder);
        saveAsync();
    }

    public void removeTurboHopper(Location loc) {
        turboHoppers.remove(normalise(loc));
        saveAsync();
    }

    public boolean isTurboHopper(Location loc) {
        return turboHoppers.containsKey(normalise(loc));
    }

    public Inventory getTurboHopperInventory(Location loc) {
        TurboHopperHolder h = turboHoppers.get(normalise(loc));
        return h != null ? h.inventory : null;
    }

    // ── Turbo Hopper – scheduler ───────────────────────────────────────────

    private void startTurboScheduler() {
        // Run every 8 ticks = vanilla hopper rate (1 item per 8 game ticks).
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<Location, TurboHopperHolder> entry :
                    new HashMap<>(turboHoppers).entrySet()) {
                processTurboHopper(entry.getKey(), entry.getValue().inventory);
            }
        }, 1L, 8L);
    }

    private void processTurboHopper(Location loc, Inventory hopperInv) {
        Block block = loc.getBlock();
        if (block.getType() != Material.HOPPER) return;

        // Pull from container above.
        // IMPORTANT: check KapaChest FIRST because a Barrel is also a Container;
        // pulling from the real barrel inventory would always yield nothing (it is
        // kept empty – items are tracked in KapaChestData).
        Block above = block.getRelative(BlockFace.UP);
        if (above.getType() == Material.BARREL && isKapaChest(above.getLocation())) {
            KapaChestData kd = getKapaChestData(above.getLocation());
            if (kd != null && kd.itemType != null && kd.count > 0) {
                pullFromKapa(kd, hopperInv);
            }
        } else if (above.getState() instanceof Container c) {
            moveOne(c.getInventory(), hopperInv);
        }

        // Determine push direction from the hopper's facing data.
        // Vanilla hoppers push in the direction they face (down or a horizontal face).
        BlockFace pushFace = BlockFace.DOWN;
        if (block.getBlockData() instanceof Hopper hopperData) {
            pushFace = hopperData.getFacing();
        }

        // Push into container in the push direction.
        // IMPORTANT: check KapaChest FIRST – same reason as above.
        Block pushTarget = block.getRelative(pushFace);
        if (pushTarget.getType() == Material.BARREL && isKapaChest(pushTarget.getLocation())) {
            KapaChestData kd = getKapaChestData(pushTarget.getLocation());
            if (kd != null) {
                pushToKapa(hopperInv, kd);
            }
        } else if (pushTarget.getState() instanceof Container c) {
            moveOne(hopperInv, c.getInventory());
        }
    }

    /** Moves one item from {@code from} to {@code to}. */
    private static void moveOne(Inventory from, Inventory to) {
        for (int i = 0; i < from.getSize(); i++) {
            ItemStack item = from.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            ItemStack single = item.clone();
            single.setAmount(1);
            if (to.addItem(single).isEmpty()) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() == 0) from.setItem(i, null);
            }
            return;
        }
    }

    /** Pulls one item from a KapaChestData into the target inventory. */
    private static void pullFromKapa(KapaChestData kd, Inventory to) {
        if (kd.itemType == null || kd.count <= 0) return;
        ItemStack single = new ItemStack(kd.itemType, 1);
        if (to.addItem(single).isEmpty()) {
            kd.count--;
            if (kd.count == 0) kd.itemType = null;
        }
    }

    /**
     * Pushes one item from {@code from} into a {@link KapaChestData}.
     * Only transfers if the item type matches (or the kapa chest is empty).
     */
    public static void pushToKapa(Inventory from, KapaChestData kd) {
        if (kd.count >= kd.maxCapacity) return;
        for (int i = 0; i < from.getSize(); i++) {
            ItemStack item = from.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            if (kd.itemType != null && kd.itemType != item.getType()) continue;
            if (kd.itemType == null) kd.itemType = item.getType();
            kd.count++;
            item.setAmount(item.getAmount() - 1);
            if (item.getAmount() == 0) from.setItem(i, null);
            return;
        }
    }

    // ── Kapa Chest – registration ──────────────────────────────────────────

    public void registerKapaChest(Location loc) {
        kapaChests.put(normalise(loc), new KapaChestData(null, 0L, KAPA_DEFAULT_CAPACITY));
        saveAsync();
    }

    public void removeKapaChest(Location loc) {
        kapaChests.remove(normalise(loc));
        saveAsync();
    }

    public boolean isKapaChest(Location loc) {
        return kapaChests.containsKey(normalise(loc));
    }

    public KapaChestData getKapaChestData(Location loc) {
        return kapaChests.get(normalise(loc));
    }

    // ── Kapa Chest – GUI ───────────────────────────────────────────────────

    public void openKapaGui(Player player, Location loc) {
        KapaChestData data = getKapaChestData(loc);
        if (data == null) return;

        KapaChestHolder holder = new KapaChestHolder(normalise(loc));
        Inventory gui = Bukkit.createInventory(holder, 27, c("&6📦 &e&lKapazitätskiste"));

        // Slot 13 – stored item or empty indicator
        if (data.itemType != null) {
            ItemStack stored = new ItemStack(data.itemType);
            ItemMeta m = stored.getItemMeta();
            m.displayName(c("&e" + prettify(data.itemType)));
            m.lore(List.of(
                    c(""),
                    c("&7Gespeichert: &e" + fmtNum(data.count) + " &8/ &7" + fmtNum(data.maxCapacity)),
                    c(""),
                    c("&aLK &7— 64 entnehmen"),
                    c("&aShift+LK &7— einen Stack entnehmen")));
            stored.setItemMeta(m);
            gui.setItem(13, stored);
        } else {
            gui.setItem(13, glassPane("&7Leer", "&7Lagere ein Item ein, um zu beginnen."));
        }

        // Slot 11 – deposit all
        gui.setItem(11, makeButton(Material.HOPPER,
                "&aEinlagern",
                "&7Lagert alle passenden Items",
                "&7aus deinem Inventar ein."));

        // Slot 15 – withdraw 64
        gui.setItem(15, makeButton(Material.CHEST,
                "&eEntnehmen &8(64)",
                "&7Entnimmt bis zu 64 Items."));

        // Slot 22 – upgrade or maxed
        if (data.maxCapacity < KAPA_MAX_CAPACITY) {
            long next = Math.min(data.maxCapacity * 10L, KAPA_MAX_CAPACITY);
            gui.setItem(22, makeButton(Material.EMERALD,
                    "&6Upgraden",
                    "&7Kapazität: &e" + fmtNum(data.maxCapacity) + " &8→ &e" + fmtNum(next),
                    "&7Kosten: &6" + plugin.getEconomyManager().format(KAPA_UPGRADE_COST),
                    "",
                    "&aKlicke zum Upgraden"));
        } else {
            gui.setItem(22, makeButton(Material.NETHER_STAR,
                    "&6Maximum erreicht!",
                    "&7Kapazität: &e" + fmtNum(data.maxCapacity)));
        }

        // Fill remaining slots with gray glass panes
        ItemStack filler = glassPane("&8", "");
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }

        player.openInventory(gui);
    }

    public void handleKapaGuiClick(Player player, KapaChestHolder holder,
                                   int slot, ClickType click) {
        Location loc = holder.loc;
        KapaChestData data = getKapaChestData(loc);
        if (data == null) return;
        EconomyManager eco = plugin.getEconomyManager();

        if (slot == 13 && data.itemType != null && data.count > 0) {
            // Withdraw
            long amount = (click.isShiftClick())
                    ? data.itemType.getMaxStackSize()
                    : 64L;
            amount = Math.min(amount, data.count);
            if (amount <= 0) return;
            ItemStack give = new ItemStack(data.itemType, (int) amount);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(give);
            long notGiven = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            data.count -= (amount - notGiven);
            if (data.count <= 0) { data.count = 0; data.itemType = null; }
            saveAsync();
            openKapaGui(player, loc);

        } else if (slot == 11) {
            // Deposit all matching items
            if (data.itemType == null) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == Material.AIR || isSpecialItem(hand)) {
                    player.sendMessage(KlassenPlugin.colorizeComponent(
                            "&cHalte ein normales Item in der Hand, um den Typ festzulegen!"));
                    return;
                }
                data.itemType = hand.getType();
            }
            long deposited = depositAll(player, data);
            if (deposited > 0) {
                player.sendMessage(KlassenPlugin.colorizeComponent(
                        "&a" + fmtNum(deposited) + "× &7" + prettify(data.itemType) + " &aeingelagert."));
            } else {
                player.sendMessage(KlassenPlugin.colorizeComponent(
                        "&7Kein &e" + prettify(data.itemType) + " &7im Inventar oder Kiste voll."));
            }
            saveAsync();
            openKapaGui(player, loc);

        } else if (slot == 15) {
            // Withdraw 64
            if (data.itemType == null || data.count == 0) return;
            long amount = Math.min(64L, data.count);
            ItemStack give = new ItemStack(data.itemType, (int) amount);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(give);
            long notGiven = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            data.count -= (amount - notGiven);
            if (data.count <= 0) { data.count = 0; data.itemType = null; }
            saveAsync();
            openKapaGui(player, loc);

        } else if (slot == 22 && data.maxCapacity < KAPA_MAX_CAPACITY) {
            // Upgrade
            if (!eco.has(player.getUniqueId(), KAPA_UPGRADE_COST)) {
                player.sendMessage(KlassenPlugin.colorizeComponent(
                        "&cNicht genug Geld! Kosten: &6" + eco.format(KAPA_UPGRADE_COST)));
                return;
            }
            eco.withdraw(player.getUniqueId(), KAPA_UPGRADE_COST);
            eco.saveAsync();
            data.maxCapacity = Math.min(data.maxCapacity * 10L, KAPA_MAX_CAPACITY);
            player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&aKapazität auf &e" + fmtNum(data.maxCapacity) + " &aItems upgraded!"));
            plugin.getScoreboardManager().update(player);
            saveAsync();
            openKapaGui(player, loc);
        }
    }

    private long depositAll(Player player, KapaChestData data) {
        long space = data.maxCapacity - data.count;
        long deposited = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != data.itemType || space <= 0) continue;
            long canTake = Math.min(item.getAmount(), space);
            deposited += canTake;
            space -= canTake;
            if (canTake == item.getAmount()) {
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount((int) (item.getAmount() - canTake));
            }
        }
        data.count += deposited;
        return deposited;
    }

    // ── Verkaufsaxt ───────────────────────────────────────────────────────────

    /**
     * Sells the contents of a chest-like block or a Kapazitätskiste.
     * For normal containers: clears the block and restores it instantly with
     * the original block data (facing direction preserved).
     * For Kapazitätskiste: sells the stored items and resets the counter in-place
     * (the barrel block stays where it is).
     */
    public void applyVerkaufsaxt(Player player, Block block) {
        ShopManager shop = plugin.getShopManager();
        EconomyManager eco  = plugin.getEconomyManager();

        // ── Kapazitätskiste: sell stored count, keep the barrel block ──────
        if (block.getType() == Material.BARREL && isKapaChest(block.getLocation())) {
            KapaChestData data = getKapaChestData(block.getLocation());
            if (data == null || data.itemType == null || data.count == 0) {
                player.sendMessage(KlassenPlugin.colorizeComponent(
                        "&5✦ &d&lVerkaufsaxt &7Die Kapazitätskiste ist leer."));
                return;
            }
            double sellPrice = shop.getSellPrice(data.itemType.name());
            if (sellPrice <= 0) {
                player.sendMessage(KlassenPlugin.colorizeComponent(
                        "&5✦ &d&lVerkaufsaxt &cDas gespeicherte Item ist nicht verkäuflich!"));
                return;
            }
            long sold  = data.count;
            double total = sellPrice * sold;
            data.count    = 0;
            data.itemType = null;
            saveAsync();
            eco.deposit(player.getUniqueId(), total);
            eco.saveAsync();
            plugin.getScoreboardManager().update(player);
            player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&5✦ &d&lVerkaufsaxt &7Kapazitätskiste geleert! &6+"
                    + eco.format(total) + " &7(" + fmtNum(sold) + " Items)"));
            return;
        }

        // ── Normal container: sell inventory, restore block instantly ──────
        if (!(block.getState() instanceof Container container)) return;

        Inventory inv = container.getInventory();
        double total = 0.0;
        int soldCount = 0;
        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            // Do not sell other special items that happen to be in the chest
            if (isSpecialItem(item)) continue;
            double sellPrice = shop.getSellPrice(item.getType().name());
            if (sellPrice > 0) {
                total += sellPrice * item.getAmount();
                soldCount += item.getAmount();
            }
        }

        inv.clear();
        // Capture block data BEFORE clearing so facing is preserved
        BlockData savedData = block.getBlockData().clone();
        block.setType(Material.AIR, false);
        // Restore instantly on the same tick
        block.setBlockData(savedData, false);

        if (total > 0) {
            eco.deposit(player.getUniqueId(), total);
            eco.saveAsync();
            plugin.getScoreboardManager().update(player);
            player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&5✦ &d&lVerkaufsaxt &7Kiste geleert! &6+" + eco.format(total)
                    + " &7(" + soldCount + " Items)"));
        } else {
            player.sendMessage(KlassenPlugin.colorizeComponent(
                    "&5✦ &d&lVerkaufsaxt &7Die Kiste war leer oder enthielt keine verkäuflichen Items."));
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();

        // Turbo Hoppers
        ConfigurationSection thSec = cfg.createSection("turbo-hoppers");
        int i = 0;
        for (Map.Entry<Location, TurboHopperHolder> entry : turboHoppers.entrySet()) {
            String id = String.valueOf(i++);
            thSec.set(id + ".loc", locStr(entry.getKey()));
            Inventory inv = entry.getValue().inventory;
            if (inv != null) {
                for (int s = 0; s < inv.getSize(); s++) {
                    ItemStack it = inv.getItem(s);
                    if (it != null && it.getType() != Material.AIR) {
                        thSec.set(id + ".items." + s, it);
                    }
                }
            }
        }

        // Kapa Chests
        ConfigurationSection kcSec = cfg.createSection("kapa-chests");
        i = 0;
        for (Map.Entry<Location, KapaChestData> entry : kapaChests.entrySet()) {
            String id = String.valueOf(i++);
            kcSec.set(id + ".loc", locStr(entry.getKey()));
            KapaChestData d = entry.getValue();
            kcSec.set(id + ".item", d.itemType != null ? d.itemType.name() : "");
            kcSec.set(id + ".count", d.count);
            kcSec.set(id + ".max", d.maxCapacity);
        }

        try {
            dataFile.getParentFile().mkdirs();
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("special_items.yml save error: " + e.getMessage());
        }
    }

    public void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);

        // Turbo Hoppers
        if (cfg.contains("turbo-hoppers")) {
            ConfigurationSection thSec = cfg.getConfigurationSection("turbo-hoppers");
            for (String id : thSec.getKeys(false)) {
                Location loc = parseLoc(thSec.getString(id + ".loc"));
                if (loc == null) continue;
                TurboHopperHolder holder = new TurboHopperHolder(loc);
                Inventory inv = Bukkit.createInventory(holder, 54, c("&b⚡ &3&lTurbo-Hopper &b⚡"));
                holder.inventory = inv;
                ConfigurationSection itemsSec = thSec.getConfigurationSection(id + ".items");
                if (itemsSec != null) {
                    for (String slotStr : itemsSec.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);
                            ItemStack item = itemsSec.getItemStack(slotStr);
                            if (item != null) inv.setItem(slot, item);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                turboHoppers.put(loc, holder);
            }
        }

        // Kapa Chests
        if (cfg.contains("kapa-chests")) {
            ConfigurationSection kcSec = cfg.getConfigurationSection("kapa-chests");
            for (String id : kcSec.getKeys(false)) {
                Location loc = parseLoc(kcSec.getString(id + ".loc"));
                if (loc == null) continue;
                String matStr = kcSec.getString(id + ".item", "");
                Material mat = (matStr == null || matStr.isEmpty())
                        ? null : Material.matchMaterial(matStr);
                long count = kcSec.getLong(id + ".count", 0L);
                long max   = kcSec.getLong(id + ".max", KAPA_DEFAULT_CAPACITY);
                kapaChests.put(loc, new KapaChestData(mat, count, max));
            }
        }
    }

    // ── Location helpers ──────────────────────────────────────────────────────

    private static Location normalise(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private static String locStr(Location loc) {
        return loc.getWorld().getName()
               + "," + loc.getBlockX()
               + "," + loc.getBlockY()
               + "," + loc.getBlockZ();
    }

    private static Location parseLoc(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] p = s.split(",", 4);
        if (p.length != 4) return null;
        World world = Bukkit.getWorld(p[0]);
        if (world == null) return null;
        try {
            return new Location(world,
                    Integer.parseInt(p[1]),
                    Integer.parseInt(p[2]),
                    Integer.parseInt(p[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    private static Component c(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public static String fmtNum(long n) {
        return String.format("%,d", n).replace(',', '.');
    }

    private static String prettify(Material mat) {
        return mat == null ? "Leer"
                : mat.name().replace('_', ' ').toLowerCase(java.util.Locale.ROOT)
                     .substring(0, 1).toUpperCase(java.util.Locale.ROOT)
                  + mat.name().replace('_', ' ').toLowerCase(java.util.Locale.ROOT).substring(1);
    }

    private static ItemStack glassPane(String name, String... lore) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = item.getItemMeta();
        m.displayName(c(name));
        if (lore.length > 0) {
            m.lore(Arrays.stream(lore).map(SpecialItemsManager::c).toList());
        }
        item.setItemMeta(m);
        return item;
    }

    private static ItemStack makeButton(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        m.displayName(c(name));
        if (lore.length > 0) {
            m.lore(Arrays.stream(lore).map(SpecialItemsManager::c).toList());
        }
        item.setItemMeta(m);
        return item;
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    /** InventoryHolder for the turbo-hopper virtual inventory. */
    public static class TurboHopperHolder implements InventoryHolder {
        public final Location loc;
        public Inventory inventory;

        TurboHopperHolder(Location loc) { this.loc = loc; }

        @Override public Inventory getInventory() { return inventory; }
    }

    /** InventoryHolder for the kapa-chest GUI (used to identify the inventory). */
    public static class KapaChestHolder implements InventoryHolder {
        public final Location loc;

        KapaChestHolder(Location loc) { this.loc = loc; }

        @Override public Inventory getInventory() { return null; }
    }

    /** Runtime data for a placed Kapazitätskiste. */
    public static class KapaChestData {
        public Material itemType;
        public long count;
        public long maxCapacity;

        KapaChestData(Material itemType, long count, long maxCapacity) {
            this.itemType    = itemType;
            this.count       = count;
            this.maxCapacity = maxCapacity;
        }
    }
}
