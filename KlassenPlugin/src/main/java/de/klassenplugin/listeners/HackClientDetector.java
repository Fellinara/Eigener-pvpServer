package de.klassenplugin.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.klassenplugin.KlassenPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Detects known hack clients (Wurst, Meteor, Impact, etc.) via client-brand
 * fingerprinting at the ProtocolLib packet level.
 *
 * <h3>Detection method</h3>
 * <p><b>Client Brand</b> – every Minecraft client sends a {@code minecraft:brand}
 * plugin-channel packet shortly after entering the PLAY state.  Hack clients
 * use their own name as the brand (e.g. {@code "wurst"}, {@code "meteor-client"})
 * rather than {@code "vanilla"} or a plain mod-loader name.  The brand list is
 * fully configurable under {@code anticheat.hack-client.blocked-brands}.</p>
 *
 * <p>This detector intercepts the raw {@code CUSTOM_PAYLOAD} packet via ProtocolLib,
 * which fires before the Paper API value ({@code Player#getClientBrandName()}) is
 * set — giving the earliest possible detection.
 *
 * <p>Channel-registration detection has been moved to {@link HackClientBukkitListener},
 * which is always registered regardless of ProtocolLib availability.
 *
 * <p>All checks respect:
 * <ul>
 *   <li>{@code anticheat.enabled} – global anti-cheat toggle</li>
 *   <li>{@code anticheat.hack-client.enabled} – feature-specific toggle</li>
 *   <li>{@code klassenplugin.anticheat.bypass} – per-player bypass permission</li>
 * </ul>
 */
public class HackClientDetector implements Listener {

    private static final String BRAND_CHANNEL    = "minecraft:brand";
    private static final String REGISTER_CHANNEL = "minecraft:register";

    private final KlassenPlugin plugin;

    public HackClientDetector(KlassenPlugin plugin, ProtocolManager pm) {
        this.plugin = plugin;
        registerPacketListener(pm);
    }

    // ── ProtocolLib: client-brand detection ───────────────────────────────────

    private void registerPacketListener(ProtocolManager pm) {
        // Always listen to the Play-phase CUSTOM_PAYLOAD.
        List<PacketType> types = new ArrayList<>();
        types.add(PacketType.Play.Client.CUSTOM_PAYLOAD);

        // Minecraft 1.20.2+ introduced a Configuration phase that runs BEFORE
        // Play. Both the minecraft:brand packet and the minecraft:register
        // (channel list) packet are sent during this phase, meaning the Play
        // listener above never fires for them.  ProtocolLib 5.1.0+ exposes
        // PacketType.Configuration.Client.CUSTOM_PAYLOAD for exactly this case.
        try {
            types.add(PacketType.Configuration.Client.CUSTOM_PAYLOAD);
        } catch (Exception | LinkageError ignored) {
            // ProtocolLib build that pre-dates Configuration-phase support –
            // fall back to Play-phase-only detection.
        }

        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOWEST,
                types.toArray(new PacketType[0])) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handleCustomPayload(event);
            }
        });
    }

    private void handleCustomPayload(PacketEvent event) {
        if (!isEnabled()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        // ── Read the channel name ─────────────────────────────────────────────
        // Try multiple ProtocolLib accessor patterns for Paper 1.21 compatibility.
        String channel = null;
        try { channel = event.getPacket().getStrings().read(0); } catch (Exception ignored) {}
        if (channel == null || channel.isEmpty()) {
            try {
                channel = event.getPacket().getMinecraftKeys().read(0).getFullKey();
            } catch (Exception ignored) {}
        }
        if (channel == null) return;

        if (!BRAND_CHANNEL.equals(channel)
                && !channel.endsWith(":brand")
                && !channel.equals("MC|Brand")) {
            // Not a brand packet – check whether it is a REGISTER packet so we
            // can detect hack-client channels at the ProtocolLib level.
            // This catches channels sent in the Configuration phase before
            // PlayerRegisterChannelEvent has a chance to fire.
            if (REGISTER_CHANNEL.equals(channel) || channel.endsWith(":register")) {
                handleRegisterPacket(event, player);
            }
            return;
        }

        // ── Read the brand bytes ──────────────────────────────────────────────
        // Try byte-array accessor first, then fall back to raw string.
        String brand = null;

        byte[] payload = null;
        try { payload = event.getPacket().getByteArrays().read(0); } catch (Exception ignored) {}

        if (payload != null && payload.length > 0) {
            // Primary: VarInt-prefixed UTF-8 string (standard Minecraft protocol).
            brand = readVarIntString(payload);
            // Fallback: ProtocolLib may strip the VarInt and give raw UTF-8 bytes.
            if (brand.isEmpty()) {
                brand = new String(payload, StandardCharsets.UTF_8).trim();
            }
        }

        // Last resort: some ProtocolLib builds expose brand as a second string.
        if (brand == null || brand.isEmpty()) {
            try { brand = event.getPacket().getStrings().read(1); } catch (Exception ignored) {}
        }

        if (brand == null || brand.isEmpty()) return;

        brand = brand.toLowerCase(Locale.ROOT).trim();
        // Strip NUL and control characters that some clients inject to hide brand.
        brand = brand.replaceAll("[\\x00-\\x1F\\x7F]", "").trim();
        if (brand.isEmpty()) return;

        plugin.getLogger().info("[HackClient] Brand von " + player.getName() + ": " + brand);
        checkBrand(player, brand);
    }

    // ── Bukkit: channel-registration detection ────────────────────────────────
    //
    // NOTE: Channel registration is now handled by HackClientBukkitListener,
    // which is always registered regardless of ProtocolLib availability.
    // HackClientDetector only performs the ProtocolLib-level brand-packet
    // detection (CUSTOM_PAYLOAD) which fires earlier and at a lower level.
    //
    // Both detections use the same blocked-channels / blocked-brands config
    // keys, and HackClientBukkitListener deduplicates actions so no player
    // is acted on twice even if both listeners fire.

    // ── REGISTER packet handler ───────────────────────────────────────────────

    /**
     * Parses a {@code minecraft:register} CUSTOM_PAYLOAD packet and checks each
     * declared channel against the blocked-channels list.
     *
     * <p>In Minecraft 1.20.2+, channel registration happens during the
     * Configuration phase (before Play), so {@link
     * org.bukkit.event.player.PlayerRegisterChannelEvent} may not fire.
     * Intercepting the raw packet here guarantees we see every channel
     * regardless of which protocol phase it was sent in.
     *
     * <p>Channels in the payload are NUL-byte separated UTF-8 strings.
     */
    private void handleRegisterPacket(PacketEvent event, Player player) {
        if (!isEnabled()) return;
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        byte[] payload = null;
        try { payload = event.getPacket().getByteArrays().read(0); } catch (Exception ignored) {}
        if (payload == null || payload.length == 0) return;

        List<String> blockedChannels = plugin.getConfig()
                .getStringList("anticheat.hack-client.blocked-channels");
        if (blockedChannels.isEmpty()) return;

        // Channels are NUL-separated in the payload.
        for (String raw : new String(payload, StandardCharsets.UTF_8).split("\0")) {
            String ch = raw.toLowerCase(Locale.ROOT).trim();
            if (ch.isEmpty()) continue;
            for (String entry : blockedChannels) {
                String lc = entry.toLowerCase(Locale.ROOT);
                if (ch.startsWith(lc) || ch.contains(lc)) {
                    plugin.getLogger().warning("[HackClient/Packet] " + player.getName()
                            + " – Hack-Client-Kanal registriert: " + raw);
                    actOnPlayer(player, "Kanal: " + raw);
                    return;
                }
            }
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private boolean isEnabled() {
        return plugin.getAntiCheatManager().isEnabled()
                && plugin.getConfig().getBoolean("anticheat.hack-client.enabled", true);
    }

    private void checkBrand(Player player, String brand) {
        List<String> blockedBrands = plugin.getConfig()
                .getStringList("anticheat.hack-client.blocked-brands");

        for (String entry : blockedBrands) {
            if (brand.contains(entry.toLowerCase(Locale.ROOT))) {
                plugin.getLogger().warning("[HackClient] Hack-Client erkannt bei "
                        + player.getName() + ": " + brand);
                actOnPlayer(player, brand);
                return;
            }
        }
    }

    /**
     * Delegates kick/ban to {@link HackClientBukkitListener#actOnPlayer} so that
     * the deduplication set and the timing-safe ban logic are shared.
     *
     * <p>During the Configuration phase (MC 1.20.2+) the player object is available
     * through ProtocolLib but the player has not yet entered the Play phase, so
     * {@code player.isOnline()} returns {@code false}.  We still apply the ban
     * immediately via the shared listener and mark the player as pending so the
     * kick is delivered the instant {@code PlayerJoinEvent} fires.
     */
    private void actOnPlayer(Player player, String detectedClient) {
        HackClientBukkitListener builtinListener = plugin.getHackClientBukkitListener();
        if (builtinListener == null) return;

        if (player.isOnline()) {
            builtinListener.actOnPlayer(player, detectedClient);
        } else {
            // Pre-join detection (Configuration phase): apply the ban now so the
            // next connection attempt is rejected even if the kick misses.
            builtinListener.markPending(player.getUniqueId(), player.getName(), detectedClient);
        }
    }

    // ── VarInt-prefixed string decoder ────────────────────────────────────────

    /**
     * Reads a Minecraft-protocol VarInt-length-prefixed UTF-8 string from a raw
     * byte array (as used in the {@code minecraft:brand} plugin-channel payload).
     * Returns an empty string on any parse error.
     */
    private static String readVarIntString(byte[] data) {
        if (data == null || data.length == 0) return "";

        // Read VarInt: up to 5 bytes, 7 bits per byte, MSB = "more bytes follow".
        int offset = 0;
        int numRead = 0;
        int length = 0;
        byte b;
        do {
            if (offset >= data.length) return "";
            b = data[offset++];
            length |= (b & 0x7F) << (7 * numRead++);
            if (numRead > 5) return ""; // Malformed VarInt.
        } while ((b & 0x80) != 0);

        if (length < 0 || offset + length > data.length) {
            // Fall back to reading the rest of the array as a best effort.
            return new String(data, offset, Math.max(0, data.length - offset),
                    StandardCharsets.UTF_8);
        }
        return new String(data, offset, length, StandardCharsets.UTF_8);
    }
}
