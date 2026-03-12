package de.klassenplugin;

import de.klassenplugin.commands.*;
import de.klassenplugin.listeners.JoinListener;
import de.klassenplugin.managers.CooldownManager;
import de.klassenplugin.managers.HomeManager;
import de.klassenplugin.managers.LobbyManager;
import de.klassenplugin.managers.WarpManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

public class KlassenPlugin extends JavaPlugin {

    private static KlassenPlugin instance;
    private boolean pluginEnabled;
    private LobbyManager lobbyManager;
    private HomeManager homeManager;
    private WarpManager warpManager;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        pluginEnabled = getConfig().getBoolean("plugin-enabled", true);

        lobbyManager = new LobbyManager(this);
        homeManager = new HomeManager(this);
        warpManager = new WarpManager(this);
        cooldownManager = new CooldownManager();

        registerCommands();
        registerListeners();

        getLogger().info("KlassenPlugin wurde erfolgreich gestartet!");
    }

    @Override
    public void onDisable() {
        getConfig().set("plugin-enabled", pluginEnabled);
        saveConfig();
        if (homeManager != null) homeManager.saveHomes();
        if (warpManager != null) warpManager.saveWarps();
        if (lobbyManager != null) lobbyManager.saveLobby();
        getLogger().info("KlassenPlugin wurde deaktiviert!");
    }

    private void registerCommands() {
        getCommand("lobby").setExecutor(new LobbyCommand(this));
        getCommand("setlobby").setExecutor(new SetLobbyCommand(this));
        getCommand("home").setExecutor(new HomeCommand(this));
        getCommand("sethome").setExecutor(new SetHomeCommand(this));
        getCommand("delhome").setExecutor(new DelHomeCommand(this));
        getCommand("homes").setExecutor(new HomesCommand(this));
        getCommand("setwarp").setExecutor(new SetWarpCommand(this));
        getCommand("warp").setExecutor(new WarpCommand(this));
        getCommand("delwarp").setExecutor(new DelWarpCommand(this));
        getCommand("warps").setExecutor(new WarpsCommand(this));
        getCommand("plugintoggle").setExecutor(new PluginToggleCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
    }

    public static KlassenPlugin getInstance() {
        return instance;
    }

    public boolean isPluginEnabled() {
        return pluginEnabled;
    }

    public void setPluginEnabled(boolean enabled) {
        this.pluginEnabled = enabled;
        getConfig().set("plugin-enabled", enabled);
        saveConfig();
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public WarpManager getWarpManager() {
        return warpManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    /**
     * Returns the message for the given key as a raw &-colour-coded string,
     * with the plugin prefix prepended. Use {@link #colorizeComponent(String)}
     * to convert it to a Component before sending to a player.
     */
    public String getMessage(String key) {
        String prefix = getConfig().getString("messages.prefix", "&8[&bKlassenPlugin&8] ");
        String message = getConfig().getString("messages." + key, "&cNachricht nicht gefunden: " + key);
        return prefix + message;
    }

    /**
     * Converts a string with {@code &} colour codes to a {@link Component}.
     */
    public static Component colorizeComponent(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
