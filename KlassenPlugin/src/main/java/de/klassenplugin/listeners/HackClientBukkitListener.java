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

    /**
     * Players detected during the Minecraft Configuration phase (MC 1.20.2+)
     * before their {@code PlayerJoinEvent} fires.
     *
     * <p>Key = UUID, Value = String array where {@code [0]} = playerName
     * (needed for the NAME ban) and {@code [1]} = detection reason.
     */
    private final java.util.concurrent.ConcurrentHashMap<UUID, String[]> pendingDetections
            = new java.util.concurrent.ConcurrentHashMap<>();

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
     * Schedules multiple independent delayed checks after the player has joined.
     *
     * <h4>Pending pre-join detections (Configuration phase, MC 1.20.2+)</h4>
     * <p>If {@link de.klassenplugin.listeners.HackClientDetector} detected a
     * hack client during the Configuration phase (before {@code PlayerJoinEvent}
     * fires), the detection is stored in {@link #pendingDetections}.  We act on
     * it immediately at join — no delays needed because the brand/channel was
     * already confirmed.
     *
     * <h4>20 / 40 / 60-tick brand check</h4>
     * <p>{@code Player#getClientBrandName()} (Paper API, no ProtocolLib required)
     * is {@code null} immediately at join because the {@code minecraft:brand}
     * packet has not yet been received (or, in MC 1.20.2+, was received during
     * the Configuration phase and may take a tick to be accessible via the API).
     * Running checks at 20, 40, and 60 ticks (≈1 s, 2 s, 3 s) ensures the value
     * is available for all connection speeds, and the {@link #actedOn} set
     * prevents duplicate actions.
     *
     * <h4>80-tick full channel audit</h4>
     * <p>{@link org.bukkit.event.player.PlayerRegisterChannelEvent} fires once
     * per channel as the client sends its {@code REGISTER} plugin-message.
     * When Wurst or Meteor Client are loaded as a <em>Fabric mod</em> under a
     * different launcher, the mod still registers its proprietary channels but a
     * race condition can cause those events to fire before the listener is ready.
     *
     * <p>At 80 ticks (≈4 s) we call {@code Player#getListeningPluginChannels()},
     * which returns <em>all</em> channels the client has declared so far,
     * regardless of when they were registered.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isEnabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        // ── Configuration-phase pre-join detection (MC 1.20.2+) ──────────────
        // If the ProtocolLib listener caught a hack-client brand or channel
        // during the Configuration phase, we act immediately on join.
        String[] pending = pendingDetections.remove(player.getUniqueId());
        if (pending != null) {
            // pending[0] = playerName, pending[1] = detectedClient reason
            actOnPlayer(player, pending[1]);
            return; // No need for further scheduled checks.
        }

        // ── Delayed brand checks (multiple intervals for resilience) ──────────
        // 20 ticks: fast connections may have the brand available early.
        scheduleBrandCheck(player, 20L);
        // 40 ticks: standard timing for most connections.
        scheduleBrandCheck(player, 40L);
        // 60 ticks: retry for slow connections (actedOn prevents double-action).
        scheduleBrandCheck(player, 60L);

        // 80 ticks: full channel audit.
        // Must run AFTER the brand checks so the dedup set is already populated
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
     *
     * <p>If the player is not yet "online" (e.g. {@link PlayerRegisterChannelEvent}
     * fired during the MC 1.20.2+ Configuration phase, before
     * {@link PlayerJoinEvent}), we cannot kick them immediately.  We delegate to
     * {@link #markPending} so the kick is applied the instant
     * {@link PlayerJoinEvent} fires.  The NAME ban is applied by
     * {@link #markPending} immediately, so the player stays banned even if they
     * disconnect during the Configuration phase.
     */
    void actOnPlayer(Player player, String detectedClient) {
        // If the player hasn't fully joined yet (Configuration-phase detection),
        // we can't kick them now. Defer via markPending.
        if (!player.isOnline()) {
            markPending(player.getUniqueId(), player.getName(), detectedClient);
            return;
        }

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

        // Apply the NAME ban immediately so the player stays banned even if they
        // disconnect before the scheduler fires (e.g., during the configuration
        // phase, player.isOnline() can return false when the scheduled task runs).
        if ("ban".equals(action)) {
            @SuppressWarnings("deprecation")
            var ignored = Bukkit.getBanList(org.bukkit.BanList.Type.NAME)
                    .addBan(player.getName(), "Hack-Client: " + detectedClient, null, "AntiCheat");
            plugin.getLogger().warning("[AntiCheat] " + player.getName()
                    + " wurde gebannt: Hack-Client (" + detectedClient + ")");
        }

        // Kick on the next tick (safe from any event-handler context).
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.kick(kickComp);
            }
        });
    }

    /**
     * Clears the deduplication entry when the player disconnects so a fresh
     * connection attempt is checked normally.
     */
    public void clearPlayer(UUID uuid) {
        actedOn.remove(uuid);
        pendingDetections.remove(uuid);
    }

    /**
     * Stores a pre-join detection so that the kick/ban is applied the instant
     * the player fires {@link org.bukkit.event.player.PlayerJoinEvent}.
     *
     * <p>Called by {@link HackClientDetector} when a hack-client brand or
     * channel is detected during the Minecraft 1.20.2+ Configuration phase
     * (before the player is "online" in the Bukkit sense).  The NAME ban is
     * applied immediately so the player stays banned even if they disconnect
     * before entering the Play phase.
     *
     * @param uuid            the player's UUID
     * @param playerName      the player's name (needed for the NAME ban)
     * @param detectedClient  human-readable detection reason
     */
    public void markPending(UUID uuid, String playerName, String detectedClient) {
        // Atomic put: if another detection already stored a pending entry for
        // this player (e.g. both ProtocolLib and Bukkit listeners fire in the
        // same Configuration phase), the second call is a no-op.
        if (pendingDetections.putIfAbsent(uuid, new String[]{playerName, detectedClient}) != null) {
            return; // already pending – first detection wins
        }

        // Apply the name-ban immediately so reconnection is blocked even if the
        // player never fully enters the Play phase.
        String action = plugin.getConfig()
                .getString("anticheat.hack-client.action", "ban")
                .toLowerCase(Locale.ROOT);
        if ("ban".equals(action)) {
            @SuppressWarnings("deprecation")
            var ignored = Bukkit.getBanList(org.bukkit.BanList.Type.NAME)
                    .addBan(playerName, "Hack-Client: " + detectedClient, null, "AntiCheat");
            plugin.getLogger().warning("[AntiCheat] " + playerName
                    + " wurde vorläufig gebannt (Konfigurationsphase): "
                    + detectedClient);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Schedules a single brand check after {@code delayTicks} ticks. */
    private void scheduleBrandCheck(Player player, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || actedOn.contains(player.getUniqueId())) return;
            checkBrandPaperApi(player);
        }, delayTicks);
    }

    private boolean isEnabled() {
        return plugin.getAntiCheatManager().isEnabled()
                && plugin.getConfig().getBoolean("anticheat.hack-client.enabled", true);
    }

    /**
     * Safely reads the client brand via the Paper API.
     * Returns an empty string when the API is unavailable or the brand is unset.
     *
     * <p>Some launchers embed Minecraft colour-code sequences
     * ({@code §b…§r}) directly in the brand string.  Both the legacy {@code §x}
     * format and the modern ampersand-prefixed {@code &x} format (after
     * lowercasing) are stripped so that comparison against the blocked-brands
     * list is reliable and the logged brand is human-readable.
     */
    private String getBrandSafe(Player player) {
        try {
            String b = player.getClientBrandName();
            if (b == null) return "";
            return b.toLowerCase(Locale.ROOT).trim()
                    .replaceAll("[\\x00-\\x1F\\x7F]", "") // ASCII control chars
                    .replaceAll("§.", "")                  // §x Minecraft colour codes
                    .replaceAll("&[0-9a-fk-or]", "")      // &x legacy colour codes
                    .trim();
        } catch (NoSuchMethodError | Exception ignored) {
            return "";
        }
    }
}
