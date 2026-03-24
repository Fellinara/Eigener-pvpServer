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

    // Serializers used to convert & color codes → § color codes for the
    // legacy Bukkit scoreboard String API, which requires § codes (not &).
    private static final LegacyComponentSerializer AMP  = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECT = LegacyComponentSerializer.legacySection();

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
                AMP.deserialize(titleRaw));
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
        obj.displayName(AMP.deserialize(titleRaw));

        // Resolve placeholders.
        EconomyManager eco = plugin.getEconomyManager();
        double balance = eco.getBalance(player.getUniqueId());
        int online = Bukkit.getOnlinePlayers().size();
        String rankName = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        String prefix = plugin.getRankManager().getPlayerPrefix(player.getUniqueId());
        int combatSeconds = plugin.getCombatManager().getRemainingSeconds(player.getUniqueId());
        String combatDisplay = combatSeconds > 0 ? "&c⚔ Kampf: &e" + combatSeconds + "s" : "&7Kein Kampf";

        // Use the prefix alone (e.g. "[Admin]") if available; the rank name is
        // already embedded in the prefix so we must not append it again.
        String rankDisplay;
        if (!prefix.isEmpty()) {
            rankDisplay = prefix;
        } else if (rankName != null && !rankName.isEmpty()) {
            rankDisplay = rankName;
        } else {
            rankDisplay = "&7Kein Rang";
        }

        List<String> configLines = plugin.getConfig().getStringList("scoreboard.lines");
        if (configLines.isEmpty()) {
            configLines = Arrays.asList(
                " ", "&7Online: &e" + online, " ",
                "&7Geld: &6" + eco.format(balance), " ",
                "&7Rang: &f" + (rankName != null ? rankName : "Kein Rang"), " ",
                "&8play.server.de");
        }

        // Replace placeholders, then convert & codes to § codes for the
        // legacy scoreboard String API (§ required, & not supported here).
        List<String> resolved = new ArrayList<>();
        for (String line : configLines) {
            String withPlaceholders = line
                    .replace("{player}", player.getName())
                    .replace("{rank}", rankDisplay)
                    .replace("{balance}", eco.format(balance))
                    .replace("{online}", String.valueOf(online))
                    .replace("{server}", Bukkit.getServer().getName())
                    .replace("{combat}", combatDisplay);
            // Convert &-codes → Adventure Component → §-codes
            resolved.add(SECT.serialize(AMP.deserialize(withPlaceholders)));
        }

        // Scoreboard entries must be unique – make duplicates unique by appending
        // invisible reset codes (§r) without changing the visual output.
        Set<String> used = new HashSet<>();
        int slot = resolved.size();
        for (String line : resolved) {
            String unique = uniquify(line, used);
            used.add(unique);
            obj.getScore(unique).setScore(slot--);
        }
    }

    /**
     * Makes {@code line} unique within {@code used} by appending §r reset
     * codes (invisible padding that doesn't change the rendered text).
     */
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
