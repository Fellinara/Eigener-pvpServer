package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages per-player sidebar scoreboards that display economy balance,
 * online player count and rank. Updates every second.
 */
public class ScoreboardManager {

    // Unique spacer strings so the Bukkit API never gets duplicate entries.
    // (The scoreboard requires every "team" entry to be unique.)
    private static final String[] SPACERS = {
        "\u00a7r", "\u00a7r\u00a7a", "\u00a7r\u00a7b", "\u00a7r\u00a7c",
        "\u00a7r\u00a7d", "\u00a7r\u00a7e", "\u00a7r\u00a7f"
    };

    private final KlassenPlugin plugin;
    // One scoreboard per player so each player sees their own balance.
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public ScoreboardManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        // Refresh every second (20 ticks).
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L);
    }

    /** Give a fresh scoreboard to a newly joining player. */
    public void setup(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective(
                "kp_side",
                Criteria.DUMMY,
                LegacyComponentSerializer.legacyAmpersand()
                        .deserialize("&6&lKlassenPlugin"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        update(player);
    }

    /** Refresh every player's sidebar. */
    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            update(p);
        }
    }

    /** Rebuild the sidebar lines for one player. */
    public void update(Player player) {
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) return;
        Objective obj = board.getObjective("kp_side");
        if (obj == null) return;

        // Clear all existing entries first.
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        EconomyManager eco = plugin.getEconomyManager();
        double balance = eco.getBalance(player.getUniqueId());
        int online = Bukkit.getOnlinePlayers().size();
        String rankName = plugin.getRankManager().getPlayerRank(player.getUniqueId());

        // Lines are ordered by score (higher score = higher on screen).
        int slot = 9;

        setLine(obj, SPACERS[0], slot--);
        setLine(obj, "\u00a77Server: \u00a7e" + Bukkit.getServer().getName(), slot--);
        setLine(obj, "\u00a77Online: \u00a7e" + online, slot--);
        setLine(obj, SPACERS[1], slot--);
        setLine(obj, "\u00a76\u00a7lEconomy", slot--);
        setLine(obj, "\u00a77Geld: \u00a76" + eco.format(balance), slot--);
        setLine(obj, SPACERS[2], slot--);
        if (rankName != null && !rankName.isEmpty()) {
            setLine(obj, "\u00a77Rang: \u00a7f" + rankName, slot--);
            setLine(obj, SPACERS[3], slot--);
        }
        setLine(obj, "\u00a78play.server.de", slot);
    }

    private void setLine(Objective obj, String text, int score) {
        obj.getScore(text).setScore(score);
    }

    /** Call when a player leaves to free the board reference. */
    public void remove(Player player) {
        boards.remove(player.getUniqueId());
        // Restore the main scoreboard so no lingering custom board stays.
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
