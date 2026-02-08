package Fortcraft.skyworld.fishing;

public enum FishingAbundance {
    REBOSANDO("§5Rebosando", 0.10),
    LLENO("§bLleno", 0.15),
    CASILLENO("§aCasi Lleno", 0.20),
    MEDIO("§eMedio", 0.25),
    ESCASO("§6Escaso", 0.25),
    VACIO("§cVacío", 0.5);

    private final String displayName;
    private final double spawnChance;

    FishingAbundance(String displayName, double spawnChance) {
        this.displayName = displayName;
        this.spawnChance = spawnChance;
    }

    public String getDisplayName() { return displayName; }

    public FishingAbundance getNextLower() {
        int nextOrdinal = this.ordinal() + 1;
        return (nextOrdinal < values().length) ? values()[nextOrdinal] : VACIO;
    }

    public static FishingAbundance rollInitial() {
        double roll = Math.random();
        double acc = 0;
        for (FishingAbundance state : values()) {
            acc += state.spawnChance;
            if (roll <= acc) return state;
        }
        return MEDIO;
    }
}
