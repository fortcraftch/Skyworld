package Fortcraft.skyworld.utils;

public enum HotbarSlot {
    PRIMARY(0, "Arma Principal"),
    SECONDARY(1, "Arma Secundaria"),
    SUPPORT(2, "Soporte/Herramienta"),
    CONSUMABLE_1(3, "Consumible 1"),
    CONSUMABLE_2(4, "Consumible 2"),
    CONSUMABLE_3(5, "Consumible 3");

    private final int slotIndex;
    private final String displayName;

    HotbarSlot(int slotIndex, String displayName) {
        this.slotIndex = slotIndex;
        this.displayName = displayName;
    }

    public int getSlotIndex() { return slotIndex; }
    public String getDisplayName() { return displayName; }

    public static HotbarSlot fromIndex(int index) {
        for (HotbarSlot slot : values()) {
            if (slot.slotIndex == index) return slot;
        }
        return null;
    }
}