package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AuctionManager {
    public enum ListingType { SELL, REQUEST }

    public static class Listing {
        public final int id;
        public final UUID seller;
        public final String sellerName;
        public final ItemStack item;
        public final double price;
        public final long timestamp;
        public final ListingType type;
        public Listing(int id, UUID seller, String sellerName, ItemStack item, double price, long timestamp, ListingType type) {
            this.id = id; this.seller = seller; this.sellerName = sellerName;
            this.item = item; this.price = price; this.timestamp = timestamp; this.type = type;
        }
    }

    /** Pending item returns for players who were offline when their listing expired. */
    private final Map<UUID, List<ItemStack>> pendingItems = new HashMap<>();

    private final KlassenPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<Integer, Listing> listings = new LinkedHashMap<>();
    private int nextId = 1;

    /** Duration of a listing in milliseconds (default: 24 h). */
    private long listingDurationMs() {
        return plugin.getConfig().getLong("auction.listing-duration-hours", 24L) * 3_600_000L;
    }

    public AuctionManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "auction.yml");
        load();
        // Expire old listings every 5 minutes.
        Bukkit.getScheduler().runTaskTimer(plugin, this::expireListings,
                20L * 60L, 20L * 60L * 5L);
    }

    public void load() {
        if (!dataFile.exists()) {
            try { plugin.getDataFolder().mkdirs(); dataFile.createNewFile(); }
            catch (IOException e) { plugin.getLogger().severe("auction.yml error: " + e.getMessage()); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        listings.clear();
        nextId = dataConfig.getInt("next-id", 1);

        // Load pending item returns.
        pendingItems.clear();
        ConfigurationSection pendingSec = dataConfig.getConfigurationSection("pending-items");
        if (pendingSec != null) {
            for (String uuidStr : pendingSec.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<Map<?, ?>> rawList = pendingSec.getMapList(uuidStr);
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<?, ?> raw : rawList) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) raw;
                        items.add(ItemStack.deserialize(map));
                    }
                    pendingItems.put(uuid, items);
                } catch (Exception e) {
                    plugin.getLogger().warning("pending-items load error for " + uuidStr + ": " + e.getMessage());
                }
            }
        }

        ConfigurationSection sec = dataConfig.getConfigurationSection("listings");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    int id = Integer.parseInt(key);
                    UUID seller = UUID.fromString(Objects.requireNonNull(sec.getString(key + ".seller")));
                    String sellerName = sec.getString(key + ".seller-name", "Unknown");
                    double price = sec.getDouble(key + ".price");
                    long ts = sec.getLong(key + ".timestamp");
                    ListingType type = ListingType.valueOf(sec.getString(key + ".type", "SELL"));
                    ConfigurationSection itemSec = sec.getConfigurationSection(key + ".item");
                    if (itemSec == null) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) itemSec.getValues(true);
                    ItemStack item = ItemStack.deserialize(map);
                    listings.put(id, new Listing(id, seller, sellerName, item, price, ts, type));
                    if (id >= nextId) nextId = id + 1;
                } catch (Exception e) { plugin.getLogger().warning("AH load error for " + key + ": " + e.getMessage()); }
            }
        }
    }

    public void save() {
        dataConfig.set("next-id", nextId);
        dataConfig.set("listings", null);
        for (Listing l : listings.values()) {
            String p = "listings." + l.id;
            dataConfig.set(p + ".seller", l.seller.toString());
            dataConfig.set(p + ".seller-name", l.sellerName);
            dataConfig.set(p + ".price", l.price);
            dataConfig.set(p + ".timestamp", l.timestamp);
            dataConfig.set(p + ".type", l.type.name());
            ConfigurationSection s = dataConfig.createSection(p + ".item");
            for (Map.Entry<String, Object> e : l.item.serialize().entrySet()) s.set(e.getKey(), e.getValue());
        }
        // Save pending item returns.
        dataConfig.set("pending-items", null);
        for (Map.Entry<UUID, List<ItemStack>> e : pendingItems.entrySet()) {
            String path = "pending-items." + e.getKey();
            List<Map<String, Object>> serialized = new ArrayList<>();
            for (ItemStack is : e.getValue()) serialized.add(is.serialize());
            dataConfig.set(path, serialized);
        }
        try { dataConfig.save(dataFile); }
        catch (IOException e) { plugin.getLogger().severe("auction.yml save error: " + e.getMessage()); }
    }

    // ── Expiry ────────────────────────────────────────────────────────────────

    /**
     * Checks all listings and expires those older than {@code listingDurationMs()}.
     * Called every 5 minutes by the scheduler and on server start after load.
     */
    public void expireListings() {
        long now = System.currentTimeMillis();
        List<Integer> expired = new ArrayList<>();
        for (Listing l : listings.values()) {
            if (now - l.timestamp >= listingDurationMs()) {
                expired.add(l.id);
            }
        }
        for (int id : expired) {
            expireListing(id);
        }
        if (!expired.isEmpty()) {
            save();
        }
    }

    private void expireListing(int id) {
        Listing l = listings.remove(id);
        if (l == null) return;

        EconomyManager eco = plugin.getEconomyManager();

        if (l.type == ListingType.SELL) {
            // Return the item to the seller.
            Player seller = Bukkit.getPlayer(l.seller);
            long durationHours = plugin.getConfig().getLong("auction.listing-duration-hours", 24L);
            if (seller != null && seller.isOnline()) {
                if (seller.getInventory().firstEmpty() != -1) {
                    seller.getInventory().addItem(l.item.clone());
                } else {
                    seller.getWorld().dropItemNaturally(seller.getLocation(), l.item.clone());
                }
                seller.sendMessage(KlassenPlugin.colorizeComponent(
                        "&e[AH] &7Dein Angebot &b#" + id + " &7(" + l.item.getAmount() + "x "
                                + l.item.getType().name() + ") &eist nach " + durationHours
                                + " Stunden abgelaufen &7– Artikel zurückgegeben."));
            } else {
                // Store for later delivery when the player logs back in.
                pendingItems.computeIfAbsent(l.seller, k -> new ArrayList<>()).add(l.item.clone());
            }
        } else {
            // REQUEST: refund the money to the requester's account (works even offline).
            long durationHours = plugin.getConfig().getLong("auction.listing-duration-hours", 24L);
            eco.deposit(l.seller, l.price);
            eco.saveAsync();
            Player req = Bukkit.getPlayer(l.seller);
            if (req != null) {
                req.sendMessage(KlassenPlugin.colorizeComponent(
                        "&e[AH] &7Deine Anfrage &b#" + id + " &7ist nach " + durationHours
                                + " Stunden abgelaufen &7– &6"
                                + eco.format(l.price) + " &7zurückerstattet."));
            }
        }

        plugin.getLogger().info("[AH] Listing #" + id + " expired (" + l.sellerName + ", " + l.type + ").");
    }

    // ── Pending item delivery ─────────────────────────────────────────────────

    /**
     * Delivers any pending (from expired listings) items to the player.
     * Call this on player join.
     */
    public void deliverPendingItems(Player player) {
        List<ItemStack> items = pendingItems.remove(player.getUniqueId());
        if (items == null || items.isEmpty()) return;

        for (ItemStack item : items) {
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(item);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
        player.sendMessage(KlassenPlugin.colorizeComponent(
                "&e[AH] &7Du hast &b" + items.size()
                        + " &7Artikel aus abgelaufenen Angeboten zurückerhalten."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    // ── Standard CRUD ─────────────────────────────────────────────────────────

    public Listing createSellListing(UUID seller, String sellerName, ItemStack item, double price) {
        Listing l = new Listing(nextId++, seller, sellerName, item.clone(), price, System.currentTimeMillis(), ListingType.SELL);
        listings.put(l.id, l);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
        return l;
    }

    public Listing createRequestListing(UUID requester, String name, ItemStack item, double price) {
        Listing l = new Listing(nextId++, requester, name, item.clone(), price, System.currentTimeMillis(), ListingType.REQUEST);
        listings.put(l.id, l);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
        return l;
    }

    public Listing getListing(int id) { return listings.get(id); }
    public void removeListing(int id) { listings.remove(id); Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save); }
    public List<Listing> getAllListings() { return new ArrayList<>(listings.values()); }
    public List<Listing> getSellListings() { List<Listing> r = new ArrayList<>(); for (Listing l : listings.values()) if (l.type == ListingType.SELL) r.add(l); return r; }
    public List<Listing> getRequestListings() { List<Listing> r = new ArrayList<>(); for (Listing l : listings.values()) if (l.type == ListingType.REQUEST) r.add(l); return r; }
    public List<Listing> getListingsBySeller(UUID s) { List<Listing> r = new ArrayList<>(); for (Listing l : listings.values()) if (l.seller.equals(s)) r.add(l); return r; }
}
