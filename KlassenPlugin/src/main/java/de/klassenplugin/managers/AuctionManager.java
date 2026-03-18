package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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

    private final KlassenPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<Integer, Listing> listings = new LinkedHashMap<>();
    private int nextId = 1;

    public AuctionManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "auction.yml");
        load();
    }

    public void load() {
        if (!dataFile.exists()) { try { plugin.getDataFolder().mkdirs(); dataFile.createNewFile(); } catch (IOException e) { plugin.getLogger().severe("auction.yml error: " + e.getMessage()); } }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        listings.clear();
        nextId = dataConfig.getInt("next-id", 1);
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
        try { dataConfig.save(dataFile); } catch (IOException e) { plugin.getLogger().severe("auction.yml save error: " + e.getMessage()); }
    }

    public Listing createSellListing(UUID seller, String sellerName, ItemStack item, double price) {
        Listing l = new Listing(nextId++, seller, sellerName, item.clone(), price, System.currentTimeMillis(), ListingType.SELL);
        listings.put(l.id, l); save(); return l;
    }

    public Listing createRequestListing(UUID requester, String name, ItemStack item, double price) {
        Listing l = new Listing(nextId++, requester, name, item.clone(), price, System.currentTimeMillis(), ListingType.REQUEST);
        listings.put(l.id, l); save(); return l;
    }

    public Listing getListing(int id) { return listings.get(id); }
    public void removeListing(int id) { listings.remove(id); save(); }
    public List<Listing> getAllListings() { return new ArrayList<>(listings.values()); }
    public List<Listing> getSellListings() { List<Listing> r = new ArrayList<>(); for (Listing l : listings.values()) if (l.type == ListingType.SELL) r.add(l); return r; }
    public List<Listing> getRequestListings() { List<Listing> r = new ArrayList<>(); for (Listing l : listings.values()) if (l.type == ListingType.REQUEST) r.add(l); return r; }
    public List<Listing> getListingsBySeller(UUID s) { List<Listing> r = new ArrayList<>(); for (Listing l : listings.values()) if (l.seller.equals(s)) r.add(l); return r; }
}
