package Fortcraft.skyworld.stats;

public enum CustomStat {
    // Stats de Minería
    MINING_FORTUNE("mining_fortune", "☘ Fortuna de Minería", 0.0),
    MINING_LUCK("mining_luck", "🍀 Suerte de Minería", 0.0),

    // Stats de Farmeo
    FARMING_FORTUNE("farming_fortune", "☘ Fortuna de Granja", 0.0),
    FARMING_LUCK("farming_luck", "🍀 Suerte de Granja", 0.0),

    // Stats de Tala (Foraging)
    FORAGING_FORTUNE("foraging_fortune", "☘ Fortuna de Tala", 0.0),
    FORAGING_LUCK("foraging_luck", "🍀 Suerte de Tala", 0.0),

    // Stats de Pesca (Fishing)
    FISHING_FORTUNE("fishing_fortune", "☘ Fortuna de Pesca", 0.0),
    FISHING_LUCK("fishing_luck", "🍀 Suerte de Pesca", 0.0),

    // Stats de Excavación (Excavation)
    EXCAVATION_FORTUNE("excavation_fortune", "☘ Fortuna de Excavación", 0.0),
    EXCAVATION_LUCK("excavation_luck", "🍀 Suerte de Excavación", 0.0),

    // Stats de Combate/General
    CRIT_CHANCE("crit_chance", "☣ Probabilidad Crítica", 10.0), // 10% base
    CRIT_DAMAGE("crit_damage", "☠ Daño Crítico", 50.0),         // 50% base
    DEFENSE("defense", "🛡 Defensa", 0.0),

    // Stats de Utilidad / Progresión
    WISDOM("wisdom", "🧠 Sabiduría", 0.0); // Aumenta la XP ganada en un %

    private final String key;
    private final String displayName;
    private final double defaultValue;

    CustomStat(String key, String displayName, double defaultValue) {
        this.key = key;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public double getDefaultValue() { return defaultValue; }

    public static CustomStat fromKey(String key) {
        for (CustomStat stat : values()) {
            if (stat.key.equalsIgnoreCase(key)) return stat;
        }
        return null;
    }
}