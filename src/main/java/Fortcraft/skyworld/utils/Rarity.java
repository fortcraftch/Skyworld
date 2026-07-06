package Fortcraft.skyworld.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public enum Rarity {
    // Hexadecimales (&#FFFFFF)
    COMUN("<white>", 1),
    ESPECIAL("<gradient:#17F126:#32A52A>", 2),
    RARO("<gradient:#29CEDD:#334B9D>", 3),
    EPICO("<gradient:#BA3FD4:#7523A7>", 4),
    LEGENDARIO("<gradient:#FFAA00:#DD6E2E>", 5), // Naranja a Amarillo claro
    EXOTICO("<gradient:#E31818:#8C0808>", 6);

    private final String colorCode;
    private final double numberToColor;

    Rarity(String colorCode, double numberToColor) {
        this.colorCode = colorCode;
        this.numberToColor = numberToColor;
    }

    public String getColorCode() {
        return colorCode;
    }

    public double getRarityNumber() {
        return numberToColor;
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