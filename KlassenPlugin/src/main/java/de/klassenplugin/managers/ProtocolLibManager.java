package de.klassenplugin.managers;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import de.klassenplugin.KlassenPlugin;
import de.klassenplugin.listeners.PacketAntiCheatListener;

/**
 * Manages the optional ProtocolLib integration.
 *
 * <p>ProtocolLib is declared as a {@code softdepend} in {@code plugin.yml}. This
 * manager is only created when the dependency is actually present on the server.
 * If it is absent the plugin continues to work without packet-level detections.
 */
public class ProtocolLibManager {

    private final KlassenPlugin plugin;
    private final ProtocolManager protocolManager;
    private PacketAntiCheatListener packetListener;

    public ProtocolLibManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /** Registers all packet listeners. Called from {@code KlassenPlugin.onEnable()}. */
    public void enable() {
        packetListener = new PacketAntiCheatListener(plugin, protocolManager);
        plugin.getLogger().info("[ProtocolLib] Paket-basiertes Anti-Cheat aktiviert.");
    }

    /**
     * Removes all registered packet listeners.
     * Must be called from {@code KlassenPlugin.onDisable()} so ProtocolLib can
     * clean up before it itself is disabled.
     */
    public void disable() {
        if (packetListener != null) {
            protocolManager.removePacketListeners(plugin);
            packetListener = null;
        }
        plugin.getLogger().info("[ProtocolLib] Paket-basiertes Anti-Cheat deaktiviert.");
    }

    public PacketAntiCheatListener getPacketListener() {
        return packetListener;
    }
}
