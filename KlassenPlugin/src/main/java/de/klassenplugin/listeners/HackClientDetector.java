package de.klassenplugin.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.klassenplugin.KlassenPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Detects known hack clients (Wurst, Meteor, Impact, etc.) via two independent
 * fingerprinting methods and kicks or bans them immediately.
 *
 * <h3>Detection methods</h3>
 * <ol>
 *   <li><b>Client Brand</b> – every Minecraft client sends a {@code minecraft:brand}
 *       plugin-channel packet shortly after entering the PLAY state.  Hack clients
 *       use their own name as the brand (e.g. {@code "wurst"}, {@code "meteor-client"})
 *       rather than {@code "vanilla"} or a plain mod-loader name.  The brand list is
 *       fully configurable under {@code anticheat.hack-client.blocked-brands}.</li>
 *   <li><b>Channel Registration</b> – several hack clients register proprietary
 *       plugin channels that vanilla clients never send (e.g. {@code "wurst:hacks"},
 *       {@code "meteor-client:mods"}).  These prefixes are configurable under
 *       {@code anticheat.hack-client.blocked-channels}.</li>
 * </ol>
 *
 * <p>Both checks respect:
 * <ul>
 *   <li>{@code anticheat.enabled} – global anti-cheat toggle</li>
 *   <li>{@code anticheat.hack-client.enabled} – feature-specific toggle</li>
 *   <li>{@code klassenplugin.anticheat.bypass} – per-player bypass permission</li>
 * </ul>
 *
 * <p>The configured action ({@code kick} or {@code ban}) is performed on the
 * main thread and accompanied by an alert message to all online admins with the
 * {@code klassenplugin.anticheat.alert} permission.
 */
public class HackClientDetector implements Listener {

    private static final String BRAND_CHANNEL = "minecraft:brand";

    private final KlassenPlugin plugin;

    public HackClientDetector(KlassenPlugin plugin, ProtocolManager pm) {
        this.plugin = plugin;
        registerPacketListener(pm);
    }

    // ── ProtocolLib: client-brand detection ───────────────────────────────────

    private void registerPacketListener(ProtocolManager pm) {
        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOWEST,
                PacketType.Play.Client.CUSTOM_PAYLOAD) {
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

    /** Called by Bukkit's event system when a client registers a plugin channel. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        if (!isEnabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("klassenplugin.anticheat.bypass")) return;

        String channel = event.getChannel().toLowerCase(Locale.ROOT);
        // Log every channel registration at INFO so the operator can investigate.
        plugin.getLogger().info("[HackClient] " + player.getName()
                + " registrierte Kanal: " + event.getChannel());

        List<String> blockedChannels = plugin.getConfig()
                .getStringList("anticheat.hack-client.blocked-channels");

        for (String entry : blockedChannels) {
            String lc = entry.toLowerCase(Locale.ROOT);
            if (channel.startsWith(lc) || channel.contains(lc)) {
                plugin.getLogger().warning("[HackClient] " + player.getName()
                        + " – verdächtiger Kanal: " + event.getChannel());
                actOnPlayer(player, "Kanal: " + event.getChannel());
                return;
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
     * Kicks or bans the player on the main thread, then alerts online admins.
     *
     * <p>Safe to call from both ProtocolLib packet listeners (main thread) and
     * Bukkit event handlers.
     */
    private void actOnPlayer(Player player, String detectedClient) {
        // Build the kick message.
        String rawMsg = plugin.getConfig().getString(
                "anticheat.hack-client.kick-message",
                "&c&lHack-Client erkannt!\n\n&7Verbindung abgelehnt.\n&eDu verwendest: &c{client}");
        String finalMsg = rawMsg.replace("{client}", detectedClient);
        Component kickComp = LegacyComponentSerializer.legacyAmpersand().deserialize(finalMsg);

        // Alert admins.
        String alert = "&c[AntiCheat] &e" + player.getName()
                + " &cwurde wegen Hack-Client &e(" + detectedClient + ") &centfernt.";
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("klassenplugin.anticheat.alert"))
                .forEach(p -> p.sendMessage(KlassenPlugin.colorizeComponent(alert)));

        // Perform the action on the main thread (kick/ban).
        String action = plugin.getConfig()
                .getString("anticheat.hack-client.action", "kick")
                .toLowerCase(Locale.ROOT);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if ("ban".equals(action)) {
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME)
                        .addBan(player.getName(), "Hack-Client: " + detectedClient, null, null);
            }
            player.kick(kickComp);
        });
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
