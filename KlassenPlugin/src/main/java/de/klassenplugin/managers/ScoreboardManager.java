package de.klassenplugin.managers;

import de.klassenplugin.KlassenPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * Manages per-player sidebar scoreboards.
 * All lines and the title are read from config.yml (scoreboard section).
 * Updates every second.
 */
public class ScoreboardManager {

    private final KlassenPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public ScoreboardManager(KlassenPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L);
    }

    /** Give a fresh scoreboard to a newly joining player. */
    public void setup(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        String titleRaw = plugin.getConfig().getString("scoreboard.title", "&6&lKlassenPlugin");
        Objective obj = board.registerNewObjective(
                "kp_side",
                Criteria.DUMMY,
                LegacyComponentSerializer.legacyAmpersand().deserialize(titleRaw));
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

    /** Rebuild the sidebar lines for one player from config. */
    public void update(Player player) {
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) return;
        Objective obj = board.getObjective("kp_side");
        if (obj == null) return;

        // Clear all existing entries first.
        for (String entry : new ArrayList<>(board.getEntries())) {
            board.resetScores(entry);
        }

        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        // Update title from config (live reload friendly).
        String titleRaw = plugin.getConfig().getString("scoreboard.title", "&6&lKlassenPlugin");
        obj.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(titleRaw));

        // Resolve placeholders.
        EconomyManager eco = plugin.getEconomyManager();
        double balance = eco.getBalance(player.getUniqueId());
        int online = Bukkit.getOnlinePlayers().size();
        String rankName = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        String prefix = plugin.getRankManager().getPlayerPrefix(player.getUniqueId());

        String rankDisplay;
        if (!prefix.isEmpty()) {
            rankDisplay = prefix + (rankName != null ? " " + rankName : "");
        } else if (rankName != null && !rankName.isEmpty()) {
            rankDisplay = rankName;
        } else {
            rankDisplay = "&7Kein Rang";
        }

        List<String> configLines = plugin.getConfig().getStringList("scoreboard.lines");
        if (configLines.isEmpty()) {
            // Fallback if lines are empty.
            configLines = Arrays.asList(
                " ", "&7Online: &e" + online, " ",
                "&7Geld: &6" + eco.format(balance), " ",
                "&7Rang: &f" + (rankName != null ? rankName : "Kein Rang"), " ",
                "&8play.server.de");
        }

        List<String> resolved = new ArrayList<>();
        for (String line : configLines) {
            resolved.add(line
                    .replace("{player}", player.getName())
                    .replace("{rank}", rankDisplay)
                    .replace("{balance}", eco.format(balance))
                    .replace("{online}", String.valueOf(online))
                    .replace("{server}", Bukkit.getServer().getName()));
        }

        // Scoreboard entries must be unique – make duplicates unique by appending
        // invisible reset codes without changing the visual output.
        Set<String> used = new HashSet<>();
        int slot = resolved.size();
        for (String line : resolved) {
            String unique = uniquify(line, used);
            used.add(unique);
            obj.getScore(unique).setScore(slot--);
        }
    }

    private static String uniquify(String line, Set<String> used) {
        if (!used.contains(line)) return line;
        String base = line;
        int i = 1;
        while (used.contains(base)) {
            base = line + "\u00a7r".repeat(i++);
        }
        return base;
    }

    /** Call when a player leaves to free the board reference. */
    public void remove(Player player) {
        boards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
