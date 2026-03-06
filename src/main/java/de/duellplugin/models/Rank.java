package de.duellplugin.models;

/**
 * ELO-based purchasable ranks and staff-assigned ranks.
 * <p>
 * {@code eloRequired} is the minimum ELO a player must have reached to be eligible to buy a rank.
 * {@code eloCost} is the amount of ELO that will be deducted when the rank is purchased.
 * The threshold is intentionally higher than the cost so that players must earn their way to a rank
 * tier before spending some of that ELO to permanently unlock it.
 * <p>
 * Ranks with {@code isStaff=true} are not purchasable and must be assigned by an admin via
 * {@code /rang setrang <player> <rang>}.
 */
public enum Rank {

    SPIELER("§7Spieler", 0, 0, false),
    VIP("§aVIP", 200, 500, false),
    VIP_PLUS("§6VIP§c+", 400, 1000, false),
    ELITE("§5Elite", 600, 1500, false),
    LEGENDE("§4§lLegende", 1000, 2000, false),
    SUPPORTER("§bSupporter", 0, 0, true),
    MODERATOR("§3Moderator", 0, 0, true),
    ADMIN("§cAdmin", 0, 0, true),
    OWNER("§4§lOwner", 0, 0, true);

    private final String displayName;
    /** ELO cost to purchase this rank (0 for staff ranks). */
    private final int eloCost;
    /** Minimum ELO required to be eligible (0 for staff ranks). */
    private final int eloRequired;
    /** True if this rank is staff-assigned and cannot be purchased. */
    private final boolean isStaff;

    Rank(String displayName, int eloCost, int eloRequired, boolean isStaff) {
        this.displayName = displayName;
        this.eloCost = eloCost;
        this.eloRequired = eloRequired;
        this.isStaff = isStaff;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getEloCost() {
        return eloCost;
    }

    public int getEloRequired() {
        return eloRequired;
    }

    public boolean isStaff() {
        return isStaff;
    }

    /** Tries to parse a rank from a case-insensitive name. Returns null if not found. */
    public static Rank fromName(String name) {
        for (Rank rank : values()) {
            if (rank.name().equalsIgnoreCase(name)
                    || rank.name().replace("_", "").equalsIgnoreCase(name)
                    || rank.name().replace("_PLUS", "+").equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return null;
    }
}
