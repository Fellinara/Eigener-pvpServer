package de.duellplugin.models;

/**
 * ELO-based purchasable ranks.
 * <p>
 * {@code eloRequired} is the minimum ELO a player must have reached to be eligible to buy a rank.
 * {@code eloCost} is the amount of ELO that will be deducted when the rank is purchased.
 * The threshold is intentionally higher than the cost so that players must earn their way to a rank
 * tier before spending some of that ELO to permanently unlock it.
 */
public enum Rank {

    SPIELER("§7Spieler", 0, 0),
    VIP("§aVIP", 200, 500),
    VIP_PLUS("§6VIP§c+", 400, 1000),
    ELITE("§5Elite", 600, 1500),
    LEGENDE("§4§lLegende", 1000, 2000);

    private final String displayName;
    /** ELO cost to purchase this rank. */
    private final int eloCost;
    /** Minimum ELO required to be eligible. */
    private final int eloRequired;

    Rank(String displayName, int eloCost, int eloRequired) {
        this.displayName = displayName;
        this.eloCost = eloCost;
        this.eloRequired = eloRequired;
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
