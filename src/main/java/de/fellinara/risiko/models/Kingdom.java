package de.fellinara.risiko.models;

/**
 * Repräsentiert ein Königreich im Risiko-Plugin.
 */
public enum Kingdom {

    FICHTEN("Fichten Königreich", "§6", "🌲", "FICHTEN"),
    DSCHUNGEL("Dschungel Königreich", "§a", "🌿", "DSCHUNGEL");

    private final String displayName;
    private final String colorCode;
    private final String emoji;
    private final String id;

    Kingdom(String displayName, String colorCode, String emoji, String id) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.emoji = emoji;
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getId() {
        return id;
    }

    /**
     * Gibt ein Kingdom anhand seines ID-Strings zurück (case-insensitive).
     */
    public static Kingdom fromString(String input) {
        if (input == null) return null;
        for (Kingdom k : values()) {
            if (k.id.equalsIgnoreCase(input) || k.displayName.equalsIgnoreCase(input)) {
                return k;
            }
        }
        // Kurzformen
        if (input.equalsIgnoreCase("fichten") || input.equalsIgnoreCase("f")) return FICHTEN;
        if (input.equalsIgnoreCase("dschungel") || input.equalsIgnoreCase("d")) return DSCHUNGEL;
        return null;
    }
}
