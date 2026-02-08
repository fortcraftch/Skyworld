package Fortcraft.skyworld.hotbar;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.utils.PlayerMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class HotbarItems {

    public static final NamespacedKey TYPE_KEY = new NamespacedKey(Skyworld.getInstance(), "hotbar_type");

    public static final String TYPE_MENU = "MENU";
    public static final String TYPE_STORAGE = "STORAGE";
    public static final String TYPE_LOGBOOK = "LOGBOOK";

    public static ItemStack getLogbook(PlayerMode mode) {
        return createItem(
                Material.BOOK,
                mode.getLegacyColor() + "§lBitácora [" + mode.getDisplayName() + "]",
                TYPE_LOGBOOK,
                List.of(
                        "§7Registro de progreso de " + mode.getDisplayName(),
                        "",
                        "§eWait click §7para cambiar modo",
                        "§eRight click §7para abrir"
                )
        );
    }

    public static ItemStack getStorage(PlayerMode mode) {
        return createItem(
                Material.BUNDLE,
                mode.getLegacyColor() + "§lBolsa [" + mode.getDisplayName() + "]",
                TYPE_STORAGE,
                List.of(
                        "§7Almacenamiento de " + mode.getDisplayName(),
                        "",
                        "§eLeft click §7para cambiar modo",
                        "§eRight click §7para abrir"
                )
        );
    }

    public static ItemStack getMenu(PlayerMode mode) {
        return createItem(
                Material.COMPASS,
                mode.getLegacyColor() + "§lMenú [" + mode.getDisplayName() + "]",
                TYPE_MENU,
                List.of(
                        "§7Opciones de " + mode.getDisplayName(),
                        "",
                        "§eLeft click §7para cambiar modo",
                        "§eRight click §7para abrir"
                )
        );
    }

    private static ItemStack createItem(Material mat, String name, String typeId, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection()
                    .deserialize(name)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                if (line.isEmpty()) {
                    lore.add(Component.empty());
                } else {
                    lore.add(LegacyComponentSerializer.legacySection()
                            .deserialize(line)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
            meta.lore(lore);

            meta.getPersistentDataContainer().set(TYPE_KEY, PersistentDataType.STRING, typeId);
            item.setItemMeta(meta);
        }

        return item;
    }

    public static boolean isHotbarItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(TYPE_KEY, PersistentDataType.STRING);
    }

    public static String getItemType(ItemStack item) {
        if (!isHotbarItem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(TYPE_KEY, PersistentDataType.STRING);
    }
}