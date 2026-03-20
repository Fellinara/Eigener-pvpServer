package de.klassenplugin.listeners;

import de.klassenplugin.KlassenPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Always-active Bukkit event listener for hack-client detection.
 *
 * <p>Unlike {@link HackClientDetector}, this listener does <em>not</em> require
 * ProtocolLib.  It is registered unconditionally in {@code KlassenPlugin} and
 * provides two independent detection methods:
 *
 * <ol>
 *   <li><b>Channel registration</b> – handles {@link PlayerRegisterChannelEvent}
 *       to catch Wurst ({@code wurst:hacks}), Meteor ({@code meteor-client:addon}),
 *       and other clients that register proprietary plugin channels.</li>
 *   <li><b>Paper API brand check</b> – 40 ticks after join, reads
 *       {@code Player#getClientBrandName()} (available in Paper 1.16+ without
 *       ProtocolLib) and blocks recognised hack-client brand strings.</li>
 * </ol>
 *
 * <p>When ProtocolLib is also present, {@link HackClientDetector} adds a third
 * layer: it intercepts the raw {@code minecraft:brand} CUSTOM_PAYLOAD packet at
 * the protocol level, which fires earlier than the Paper API value becomes
 * available.  All three detections respect the same config keys.
 *
 * <h3>Config keys used</h3>
 * <ul>
 *   <li>{@code anticheat.enabled}</li>
 *   <li>{@code anticheat.hack-client.enabled}</li>
 *   <li>{@code anticheat.hack-client.action} ({@code kick} or {@code ban})</li>
 *   <li>{@code anticheat.hack-client.kick-message}</li>
 *   <li>{@code anticheat.hack-client.blocked-brands}</li>
 *   <li>{@code anticheat.hack-client.blocked-channels}</li>
 *   <li>{@code anticheat.hack-client.brand-whitelist.enabled} (opt-in)</li>
 *   <li>{@code anticheat.hack-client.brand-whitelist.allowed-brands}</li>
 * </ul>
 */
public class HackClientBukkitListener implements Listener {

    private final KlassenPlugin plugin;

    /**
     * UUIDs of players that have already been acted on in this session.
     * Prevents duplicate kick/ban from multiple simultaneous detections
     * (e.g., channel detection AND brand detection both firing within the same tick).
     */
    private final Set<UUID> actedOn = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public HackClientBukkitListener(KlassenPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Channel registration detection ───────────────────────────────────────

    /**
     * Fires whenever the client sends a {@code REGISTER} plugin-message to
     * announce its custom channels.  Both Wurst and Meteor Client register
     * proprietary channels ({@code wurst:hacks}, {@code meteor-client:addon},
     * etc.) with their default configurations.
     *
     * <p>This event is a standard Bukkit API event – no ProtocolLib required.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        if (!isEnabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        String channel = event.getChannel().toLowerCase(Locale.ROOT);
        plugin.getLogger().info("[HackClient] " + player.getName()
                + " registrierte Kanal: " + event.getChannel());

        List<String> blockedChannels = plugin.getConfig()
                .getStringList("anticheat.hack-client.blocked-channels");

        for (String entry : blockedChannels) {
            String lc = entry.toLowerCase(Locale.ROOT);
            if (channel.startsWith(lc) || channel.contains(lc)) {
                plugin.getLogger().warning("[HackClient] " + player.getName()
                        + " – verdächtiger Kanal erkannt: " + event.getChannel());
                actOnPlayer(player, "Kanal: " + event.getChannel());
                return;
            }
        }
    }

    // ── Paper API brand check + full channel audit ────────────────────────────

    /**
     * Schedules two independent delayed checks after the player has joined.
     *
     * <h4>40-tick brand check</h4>
     * <p>{@code Player#getClientBrandName()} (Paper API, no ProtocolLib required)
     * is {@code null} immediately at join because the {@code minecraft:brand}
     * packet has not yet been received.  Delaying by 40 ticks (≈2 s) reliably
     * ensures the value has been set for all normally-connecting clients.
     *
     * <h4>80-tick full channel audit</h4>
     * <p>{@link PlayerRegisterChannelEvent} fires once per channel as the
     * client sends its {@code REGISTER} plugin-message.  When Wurst or Meteor
     * Client are loaded as a <em>Fabric mod</em> under a different launcher
     * (e.g. Feather), the mod still registers its proprietary channels, but a
     * race condition or batched {@code REGISTER} packet can cause those events
     * to fire before the listener is fully attached.
     *
     * <p>At 80 ticks (≈4 s) we call {@code Player#getListeningPluginChannels()},
     * which returns <em>all</em> channels the client has declared so far,
     * regardless of when they were registered.  This is the definitive audit
     * that catches mod-loaded hack clients running under a clean launcher brand.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isEnabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        // 40 ticks: brand check.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            checkBrandPaperApi(player);
        }, 40L);

        // 80 ticks: full channel audit.
        // Must run AFTER the brand check so the dedup set is already populated
        // if the brand check already acted, and so the brand is available for
        // the mismatch log.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            auditAllChannels(player);
        }, 80L);
    }

    /**
     * Audits the full set of channels the player has registered so far.
     *
     * <p>This is the primary detection path for <strong>mod-loaded hack clients</strong>
     * (e.g. Meteor Client or Wurst running as a Fabric mod under Feather).
     * In that scenario the client's brand is the launcher's name (e.g.
     * {@code "feather"}, {@code "fabric"}), which passes the brand check.
     * However, the Fabric mod still registers its proprietary plugin channels
     * (e.g. {@code meteor-client:main}, {@code wurst:hacks}) — these will
     * appear in {@code Player#getListeningPluginChannels()}.
     *
     * <p>A secondary benefit: if the {@link PlayerRegisterChannelEvent} for a
     * particular channel was somehow missed (batched packet race), this audit
     * catches it.
     */
    private void auditAllChannels(Player player) {
        if (!isEnabled()) return;
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        Set<String> channels;
        try {
            channels = player.getListeningPluginChannels();
        } catch (Exception ignored) {
            return; // API not available in this Paper build.
        }

        if (channels == null || channels.isEmpty()) return;

        List<String> blockedChannels = plugin.getConfig()
                .getStringList("anticheat.hack-client.blocked-channels");

        for (String raw : channels) {
            String channel = raw.toLowerCase(Locale.ROOT);
            for (String entry : blockedChannels) {
                String lc = entry.toLowerCase(Locale.ROOT);
                if (channel.startsWith(lc) || channel.contains(lc)) {
                    // Retrieve the brand so admins understand the "mod-under-legit-client" scenario.
                    String brand = getBrandSafe(player);
                    String detail = raw + (brand != null && !brand.isEmpty()
                            ? " (Brand: " + brand + ")" : "");
                    plugin.getLogger().warning("[HackClient/Audit] " + player.getName()
                            + " – Hack-Mod erkannt: " + detail
                            + " | Alle Kanäle: " + channels.size());
                    actOnPlayer(player, "Kanal (Mod): " + raw);
                    return;
                }
            }
        }
    }

    /**
     * Reads the client brand via the Paper API and compares it against the
     * blocked-brands list (and optionally the brand whitelist).
     */
    private void checkBrandPaperApi(Player player) {
        if (!isEnabled()) return;
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        String brand = getBrandSafe(player);
        if (brand.isEmpty()) return;

        plugin.getLogger().info("[HackClient/Paper] Brand von "
                + player.getName() + ": " + brand);

        // ── Brand whitelist (opt-in) ──────────────────────────────────────
        if (plugin.getConfig().getBoolean(
                "anticheat.hack-client.brand-whitelist.enabled", false)) {
            List<String> allowed = plugin.getConfig()
                    .getStringList("anticheat.hack-client.brand-whitelist.allowed-brands");
            boolean isAllowed = false;
            for (String a : allowed) {
                if (brand.contains(a.toLowerCase(Locale.ROOT))) {
                    isAllowed = true;
                    break;
                }
            }
            if (!isAllowed) {
                plugin.getLogger().warning("[HackClient/Paper] Unbekannter Brand bei "
                        + player.getName() + ": " + brand);
                actOnPlayer(player, brand + " (unbekannter Client)");
                return;
            }
        }

        // ── Blocked-brands list ───────────────────────────────────────────
        List<String> blockedBrands = plugin.getConfig()
                .getStringList("anticheat.hack-client.blocked-brands");
        for (String entry : blockedBrands) {
            if (brand.contains(entry.toLowerCase(Locale.ROOT))) {
                plugin.getLogger().warning("[HackClient/Paper] Hack-Client erkannt bei "
                        + player.getName() + ": " + brand);
                actOnPlayer(player, brand);
                return;
            }
        }
    }

    // ── Shared action ─────────────────────────────────────────────────────────

    /**
     * Kicks or bans the player on the main thread and alerts online admins.
     *
     * <p>A per-session deduplication set prevents the player from being acted on
     * more than once (e.g., when both channel detection and brand detection fire
     * simultaneously, or when both this listener and {@link HackClientDetector}
     * detect the same player within the same tick).
     */
    void actOnPlayer(Player player, String detectedClient) {
        if (!actedOn.add(player.getUniqueId())) return; // already acted

        String rawMsg = plugin.getConfig().getString(
                "anticheat.hack-client.kick-message",
                "&c&lHack-Client erkannt!\n\n&7Dein Client ist auf diesem Server nicht erlaubt.\n&eDu verwendest: &c{client}");
        String finalMsg = rawMsg.replace("{client}", detectedClient);
        Component kickComp = LegacyComponentSerializer.legacyAmpersand().deserialize(finalMsg);

        // Alert admins immediately (before the scheduler fires).
        String alert = "&c[AntiCheat] &e" + player.getName()
                + " &cwurde wegen Hack-Client &e(" + detectedClient + ") &centfernt.";
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("klassenplugin.anticheat.alert"))
                .forEach(p -> p.sendMessage(KlassenPlugin.colorizeComponent(alert)));

        String action = plugin.getConfig()
                .getString("anticheat.hack-client.action", "ban")
                .toLowerCase(Locale.ROOT);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if ("ban".equals(action)) {
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME)
                        .addBan(player.getName(), "Hack-Client: " + detectedClient, null, null);
                plugin.getLogger().warning("[AntiCheat] " + player.getName()
                        + " wurde gebannt: " + detectedClient);
            }
            player.kick(kickComp);
        });
    }

    /**
     * Clears the deduplication entry when the player disconnects so a fresh
     * connection attempt is checked normally.
     */
    public void clearPlayer(UUID uuid) {
        actedOn.remove(uuid);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private boolean isEnabled() {
        return plugin.getAntiCheatManager().isEnabled()
                && plugin.getConfig().getBoolean("anticheat.hack-client.enabled", true);
    }

    /**
     * Safely reads the client brand via the Paper API.
     * Returns an empty string when the API is unavailable or the brand is unset.
     */
    private String getBrandSafe(Player player) {
        try {
            String b = player.getClientBrandName();
            if (b == null) return "";
            return b.toLowerCase(Locale.ROOT).trim()
                    .replaceAll("[\\x00-\\x1F\\x7F]", "").trim();
        } catch (NoSuchMethodError | Exception ignored) {
            return "";
        }
    }
}
