package Fortcraft.skyworld.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public enum Rarity {
    // Hexadecimales (&#FFFFFF)
    COMUN("<white>"),
    ESPECIAL("<gradient:#17F126:#32A52A>"),
    RARO("<gradient:#29CEDD:#334B9D>"),
    EPICO("<gradient:#BA3FD4:#7523A7>"),
    LEGENDARIO("<gradient:#FFAA00:#DD6E2E>"), // Naranja a Amarillo claro
    EXOTICO("<gradient:#E31818:#8C0808>");

    private final String colorCode;

    Rarity(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getColorCode() {
        return colorCode;
    }

    public Component format(String displayName) {
        String cleanName = displayName.replaceAll("(?i)§[0-9A-FK-ORX]", "")
                .replaceAll("(?i)&[0-9A-FK-ORX]", "");

        return ColorUtils.format(this.colorCode + cleanName)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static Rarity fromString(String text) {
        if (text == null) return COMUN;
        try {
            return Rarity.valueOf(text.toUpperCase().replace(" ", ""));
        } catch (IllegalArgumentException e) {
            return COMUN;
        }
    }
}