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


    public static Component format(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

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