package Fortcraft.skyworld.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();

    private static final String LEGENDARY_COLORS = "#FFD700:#FFA500:#FFFFE0:#FFA500:#FFD700";
    private static final String EXOTIC_COLORS = "#8C0808:#E31818:#FF7E7E:#E31818:#8C0808";

    public static Component format(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // 1. Si contiene etiquetas de MiniMessage (<red>, <gradient:...>)
        if (text.contains("<") && text.contains(">")) {
            try {
                return MINI_MESSAGE.deserialize(text)
                        .decoration(TextDecoration.ITALIC, false);
            } catch (Exception ignored) {
                // Si falla el parseo de MiniMessage, continúa con legacy
            }
        }

        // 2. Unificar los códigos '&' y '§' reemplazando '&' por '§'
        // Esto permite que SECTION_SERIALIZER procese ambos formatos y los colores hex nativos (§x§f...)
        // eliminando cualquier carácter '§' residual del contenido del Component.
        String legacyText = text.replace('&', '§');

        return SECTION_SERIALIZER.deserialize(legacyText)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static Component getAnimatedName(String text, Rarity rarity) {
        if (rarity == null) return format(text);

        String colors = switch (rarity.name().toUpperCase()) {
            case "LEGENDARIO" -> LEGENDARY_COLORS;
            case "EXOTICO" -> EXOTIC_COLORS;
            default -> null;
        };

        if (colors == null) {
            return rarity.format(text).decoration(TextDecoration.ITALIC, false);
        }

        long duration = 6000;
        long time = System.currentTimeMillis() % duration;
        double rawPhase = (double) time / duration;
        double mirroredPhase = rawPhase < 0.5 ? rawPhase * 2 : (1 - rawPhase) * 2;

        String dynamicText = "<gradient:" + colors + ":" + mirroredPhase + ">" + text + "</gradient>";

        return MINI_MESSAGE.deserialize(dynamicText)
                .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Si necesitas un String con símbolos '§' para métodos legacy de Bukkit.
     */
    public static String colorize(String text) {
        if (text == null || text.isEmpty()) return "";
        return SECTION_SERIALIZER.serialize(format(text));
    }
}