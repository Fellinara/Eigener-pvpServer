package de.duellplugin;

import de.duellplugin.commands.*;
import de.duellplugin.listeners.*;
import de.duellplugin.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class DuellPlugin extends JavaPlugin {

    private ArenaManager arenaManager;
    private KitManager kitManager;
    private StatsManager statsManager;
    private DuellManager duellManager;
    private BotManager botManager;
    private LobbyManager lobbyManager;
    private FarmCodeManager farmCodeManager;
    private FriendManager friendManager;
    private PartyManager partyManager;
    private NpcManager npcManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        arenaManager = new ArenaManager(this);
        kitManager = new KitManager(this);
        statsManager = new StatsManager(this);
        duellManager = new DuellManager(this);
        botManager = new BotManager(this);
        lobbyManager = new LobbyManager(this);
        farmCodeManager = new FarmCodeManager(this);
        friendManager = new FriendManager(this);
        partyManager = new PartyManager(this);
        npcManager = new NpcManager(this);

        registerCommands();
        registerListeners();

        getLogger().info("DuellPlugin v" + getDescription().getVersion() + " aktiviert!");
        getLogger().info("Das ultimative PvP-Erlebnis ist bereit!");
    }

    @Override
    public void onDisable() {
        if (statsManager != null) statsManager.saveStats();
        if (arenaManager != null) arenaManager.saveArenas();
        if (botManager != null) botManager.cleanupBots();
        if (friendManager != null) friendManager.saveFriends();
        if (npcManager != null) npcManager.saveNpcs();
        getLogger().info("DuellPlugin deaktiviert. Daten gespeichert.");
    }

    private void registerCommands() {
        getCommand("duell").setExecutor(new DuellCommand(this));
        getCommand("duell").setTabCompleter(new DuellCommand(this));

        ArenaCommand arenaCmd = new ArenaCommand(this);
        getCommand("arena").setExecutor(arenaCmd);
        getCommand("arena").setTabCompleter(arenaCmd);

        KitCommand kitCmd = new KitCommand(this);
        getCommand("kit").setExecutor(kitCmd);
        getCommand("kit").setTabCompleter(kitCmd);

        StatsCommand statsCmd = new StatsCommand(this);
        getCommand("stats").setExecutor(statsCmd);
        getCommand("stats").setTabCompleter(statsCmd);

        getCommand("lobby").setExecutor(new LobbyCommand(this));
        getCommand("setlobby").setExecutor(new SetLobbyCommand(this));

        BotCommand botCmd = new BotCommand(this);
        getCommand("bot").setExecutor(botCmd);
        getCommand("bot").setTabCompleter(botCmd);

        FarmCodeCommand farmCodeCmd = new FarmCodeCommand(this);
        getCommand("farmcode").setExecutor(farmCodeCmd);
        getCommand("farmcode").setTabCompleter(farmCodeCmd);

        RankCommand rankCmd = new RankCommand(this);
        getCommand("rang").setExecutor(rankCmd);
        getCommand("rang").setTabCompleter(rankCmd);

        FriendCommand friendCmd = new FriendCommand(this);
        getCommand("freund").setExecutor(friendCmd);
        getCommand("freund").setTabCompleter(friendCmd);

        PartyCommand partyCmd = new PartyCommand(this);
        getCommand("party").setExecutor(partyCmd);
        getCommand("party").setTabCompleter(partyCmd);

        NpcCommand npcCmd = new NpcCommand(this);
        getCommand("npc").setExecutor(npcCmd);
        getCommand("npc").setTabCompleter(npcCmd);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyItemListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIClickListener(this), this);
        getServer().getPluginManager().registerEvents(new DuellListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new NpcListener(this), this);
    }

    public ArenaManager getArenaManager() { return arenaManager; }
    public KitManager getKitManager() { return kitManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public DuellManager getDuellManager() { return duellManager; }
    public BotManager getBotManager() { return botManager; }
    public LobbyManager getLobbyManager() { return lobbyManager; }
    public FarmCodeManager getFarmCodeManager() { return farmCodeManager; }
    public FriendManager getFriendManager() { return friendManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public NpcManager getNpcManager() { return npcManager; }

    /** Returns the configured message prefix with color codes translated. */
    public String getPrefix() {
        return ChatUtils.color(getConfig().getString("messages.prefix", "&8[&6DuellPlugin&8] "));
    }

    /** Returns a configured message string with color codes translated. */
    public String getMsg(String key, String def) {
        return ChatUtils.color(getConfig().getString("messages." + key, def));
    }
}
