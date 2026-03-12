package de.klassenplugin;

import de.klassenplugin.commands.*;
import de.klassenplugin.listeners.DeathListener;
import de.klassenplugin.listeners.JoinListener;
import de.klassenplugin.listeners.QuitListener;
import de.klassenplugin.managers.*;
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
    private BackManager backManager;
    private TpaManager tpaManager;
    private KitManager kitManager;
    private MsgManager msgManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        pluginEnabled = getConfig().getBoolean("plugin-enabled", true);

        lobbyManager = new LobbyManager(this);
        homeManager = new HomeManager(this);
        warpManager = new WarpManager(this);
        cooldownManager = new CooldownManager();
        backManager = new BackManager();
        tpaManager = new TpaManager(getConfig().getInt("tpa.expire-seconds", 60));
        kitManager = new KitManager(this);
        msgManager = new MsgManager();

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
        if (kitManager != null) kitManager.saveKits();
        getLogger().info("KlassenPlugin wurde deaktiviert!");
    }

    private void registerCommands() {
        // Lobby & Spawn
        getCommand("lobby").setExecutor(new LobbyCommand(this));
        getCommand("setlobby").setExecutor(new SetLobbyCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));

        // Homes
        getCommand("home").setExecutor(new HomeCommand(this));
        getCommand("sethome").setExecutor(new SetHomeCommand(this));
        getCommand("delhome").setExecutor(new DelHomeCommand(this));
        getCommand("homes").setExecutor(new HomesCommand(this));

        // Warps
        getCommand("setwarp").setExecutor(new SetWarpCommand(this));
        getCommand("warp").setExecutor(new WarpCommand(this));
        getCommand("delwarp").setExecutor(new DelWarpCommand(this));
        getCommand("warps").setExecutor(new WarpsCommand(this));

        // Back
        getCommand("back").setExecutor(new BackCommand(this));

        // TPA
        TpaCommand tpaCommand = new TpaCommand(this);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpa").setTabCompleter(tpaCommand);
        getCommand("tpaccept").setExecutor(new TpAcceptCommand(this));
        getCommand("tpdeny").setExecutor(new TpDenyCommand(this));
        getCommand("tpcancel").setExecutor(new TpCancelCommand(this));

        // Kits
        KitCommand kitCommand = new KitCommand(this);
        getCommand("kit").setExecutor(kitCommand);
        getCommand("kit").setTabCompleter(kitCommand);
        getCommand("kits").setExecutor(new KitsCommand(this));
        CreateKitCommand createKitCommand = new CreateKitCommand(this);
        getCommand("createkit").setExecutor(createKitCommand);
        DelKitCommand delKitCommand = new DelKitCommand(this);
        getCommand("delkit").setExecutor(delKitCommand);
        getCommand("delkit").setTabCompleter(delKitCommand);

        // Admin commands
        HealCommand healCommand = new HealCommand(this);
        getCommand("heal").setExecutor(healCommand);
        getCommand("heal").setTabCompleter(healCommand);
        FeedCommand feedCommand = new FeedCommand(this);
        getCommand("feed").setExecutor(feedCommand);
        getCommand("feed").setTabCompleter(feedCommand);
        FlyCommand flyCommand = new FlyCommand(this);
        getCommand("fly").setExecutor(flyCommand);
        getCommand("fly").setTabCompleter(flyCommand);

        // Private messages
        MsgCommand msgCommand = new MsgCommand(this);
        getCommand("msg").setExecutor(msgCommand);
        getCommand("msg").setTabCompleter(msgCommand);
        getCommand("r").setExecutor(new ReplyCommand(this));

        // Plugin toggle
        getCommand("plugintoggle").setExecutor(new PluginToggleCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new QuitListener(this), this);
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

    public LobbyManager getLobbyManager() { return lobbyManager; }
    public HomeManager getHomeManager() { return homeManager; }
    public WarpManager getWarpManager() { return warpManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public BackManager getBackManager() { return backManager; }
    public TpaManager getTpaManager() { return tpaManager; }
    public KitManager getKitManager() { return kitManager; }
    public MsgManager getMsgManager() { return msgManager; }

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

