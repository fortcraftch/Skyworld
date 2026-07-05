package Fortcraft.skyworld.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    // Definición de colores para las rarezas animadas
    private static final String LEGENDARY_COLORS = "#FFD700:#FFA500:#FFFFE0:#FFA500:#FFD700"; // Oro -> Naranja -> Blanco -> Oro
    private static final String EXOTIC_COLORS = "#FF00FF:#00FFFF:#FF00FF"; // Magenta -> Cyan -> Magenta

    public static Component format(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return parseInternal(text);
    }

    /**
     * Genera un Component animado basado en el tiempo actual del sistema.
     * @param text El texto base (nombre del item).
     * @param rarity La rareza para determinar la paleta de colores.
     * @return El Component con el gradiente aplicado en la fase correcta.
     */
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

        // Usamos un divisor más grande para que sea lento (puedes subirlo a 7000 o 8000 si quieres más lentitud)
        long duration = 6000;
        long time = System.currentTimeMillis() % duration;
        double rawPhase = (double) time / duration;

        // --- EL TRUCO PARA LA FLUIDEZ ---
        // En lugar de ir de 0 a 1 (salto), vamos de 0 a 1 y de 1 a 0.
        // Esto hace que el gradiente "vuelva" por donde vino, eliminando la costura.
        double mirroredPhase = rawPhase < 0.5 ? rawPhase * 2 : (1 - rawPhase) * 2;

        // Usamos el mirroredPhase en el tag de MiniMessage
        String dynamicText = "<gradient:" + colors + ":" + mirroredPhase + ">" + text + "</gradient>";

        return MINI_MESSAGE.deserialize(dynamicText)
                .decoration(TextDecoration.ITALIC, false);
    }

    private static Component parseInternal(String text) {
        if (text.contains("&") || text.contains("§")) {
            String legacyText = text.replace("&", "§");
            return LEGACY_SERIALIZER.deserialize(legacyText)
                    .decoration(TextDecoration.ITALIC, false);
        }
        try {
            return MINI_MESSAGE.deserialize(text)
                    .decoration(TextDecoration.ITALIC, false);
        } catch (Exception e) {
            return Component.text(text).decoration(TextDecoration.ITALIC, false);
        }
    }

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) return "";
        return LegacyComponentSerializer.legacySection().serialize(format(text));
    }
}