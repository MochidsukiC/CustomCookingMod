package jp.houlab.mochidsuki.customcookingmod.cooking;

/**
 * Enum for cooking actions
 * Each action represents a cooking method
 */
public enum CookingAction {
    STIR_FRY("stir_fry", "Stir-Frying", 200),      // 10 seconds (20 ticks/sec)
    SIMMER("simmer", "Simmering", 600),            // 30 seconds
    BOIL("boil", "Boiling", 300),                  // 15 seconds
    GRILL("grill", "Grilling", 200),               // 10 seconds
    BAKE("bake", "Baking", 400),                   // 20 seconds
    STEAM("steam", "Steaming", 300),               // 15 seconds
    MIX("mix", "Mixing", 100),                     // 5 seconds
    CHOP("chop", "Chopping", 40),                  // 2 seconds
    FRY("fry", "Frying", 200),                     // 10 seconds
    NONE("none", "None", 0);

    private final String id;
    private final String displayName;
    private final int defaultDurationTicks;

    CookingAction(String id, String displayName, int defaultDurationTicks) {
        this.id = id;
        this.displayName = displayName;
        this.defaultDurationTicks = defaultDurationTicks;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultDurationTicks() {
        return defaultDurationTicks;
    }

    /**
     * Get action by ID
     */
    public static CookingAction fromId(String id) {
        for (CookingAction action : values()) {
            if (action.id.equals(id)) {
                return action;
            }
        }
        return NONE;
    }

    /**
     * Check if this action requires heat
     */
    public boolean requiresHeat() {
        return this != NONE && this != MIX && this != CHOP;
    }

    /**
     * Get the intermediate result prefix for this action
     * Used to create intermediate items like "Stir-fried Vegetables"
     */
    public String getResultPrefix() {
        switch (this) {
            case STIR_FRY: return "Stir-fried";
            case SIMMER: return "Simmered";
            case BOIL: return "Boiled";
            case GRILL: return "Grilled";
            case BAKE: return "Baked";
            case STEAM: return "Steamed";
            case MIX: return "Mixed";
            case CHOP: return "Chopped";
            case FRY: return "Fried";
            default: return "";
        }
    }
}
