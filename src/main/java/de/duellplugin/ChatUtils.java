package de.duellplugin;

import org.bukkit.ChatColor;

public final class ChatUtils {

    private ChatUtils() {}

    /**
     * Translates {@code &} color codes to the Minecraft {@code §} format.
     */
    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
