package Fortcraft.skyworld.hotbar;

public enum PlayerMode {
    GLOBAL("Global", "§f"),
    COMBAT("Combate", "§c"),
    FARMING("Granja", "§e"),
    MINING("Minería", "§6"),
    FISHING("Pesca", "§b"),
    FORAGING("Foraging", "§2");

    private final String displayName;
    private final String legacyColor;

    PlayerMode(String displayName, String legacyColor) {
        this.displayName = displayName;
        this.legacyColor = legacyColor;
    }

    public String getDisplayName() { return displayName; }
    public String getLegacyColor() { return legacyColor; }

    public PlayerMode next() {
        int nextIndex = (this.ordinal() + 1) % values().length;
        return values()[nextIndex];
    }
}