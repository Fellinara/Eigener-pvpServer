package com.fellinara.pvpserver;

import org.bukkit.plugin.java.JavaPlugin;

public class WhitelistMessagePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new WhitelistListener(), this);
        getLogger().info("WhitelistMessage Plugin aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("WhitelistMessage Plugin deaktiviert.");
    }
}
