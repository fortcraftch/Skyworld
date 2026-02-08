package Fortcraft.skyworld.menu;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.utils.HotbarSlot;
import Fortcraft.skyworld.utils.PlayerMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class LoadoutGUI {

    public static final String PREFIX = "§8Configurar: ";
    public static final String SELECTOR_PREFIX = "§8Seleccionar para ";

    public static final NamespacedKey KEY_SLOT_INDEX = new NamespacedKey("fortcraft", "loadout_slot_idx");
    public static final NamespacedKey KEY_ITEM_ID = new NamespacedKey("fortcraft", "loadout_item_id");

    public static void open(Player player) {
        var managerHandler = Skyworld.getInstance().getManagerHandler();
        var data = managerHandler.getDataManager().getPlayerData(player.getUniqueId());
        PlayerMode currentMode = managerHandler.getHotbarManager().getMode(player);

        Inventory inv = Bukkit.createInventory(null, 9, Component.text(PREFIX + currentMode.getDisplayName()));

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.empty());
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);

        for (HotbarSlot slot : HotbarSlot.values()) {
            String currentItemId = data.getLoadoutItem(currentMode, slot.getSlotIndex());
            ItemStack icon = null;

            if (currentItemId != null && !currentItemId.isEmpty()) {
                icon = ItemRegistry.build(currentItemId);
            }

            if (icon == null) {
                icon = new ItemStack(Material.BARRIER);
            }

            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(LegacyComponentSerializer.legacySection()
                        .deserialize("§e" + slot.getDisplayName())
                        .decoration(TextDecoration.ITALIC, false));

                List<Component> lore = new ArrayList<>();
                lore.add(LegacyComponentSerializer.legacySection()
                        .deserialize("§7Estado: " + (currentItemId == null ? "§cVacío" : "§aEquipado"))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                lore.add(LegacyComponentSerializer.legacySection()
                        .deserialize("§e▶ Click para cambiar")
                        .decoration(TextDecoration.ITALIC, false));

                meta.lore(lore);
                meta.getPersistentDataContainer().set(KEY_SLOT_INDEX, PersistentDataType.INTEGER, slot.getSlotIndex());
                icon.setItemMeta(meta);
            }

            inv.setItem(slot.getSlotIndex(), icon);
        }
        player.openInventory(inv);
    }

    public static void openSelector(Player player, int slotIndex) {
        HotbarSlot slotType = HotbarSlot.fromIndex(slotIndex);
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8Seleccionar para " + slotType.getDisplayName()));

        // Mapeo: Qué categoría de ítem permite cada slot de la Hotbar
        String filterCategory = switch (slotType) {
            case PRIMARY, SECONDARY -> "WEAPON";
            case SUPPORT -> "TOOL";
            case CONSUMABLE_1, CONSUMABLE_2 -> "CONSUMABLE";
            default -> "ANY";
        };

        ItemRegistry.getTemplates().forEach((id, data) -> {
            // Filtrar por categoría
            if (filterCategory.equals("ANY") || data.category().equalsIgnoreCase(filterCategory)) {
                ItemStack icon = ItemRegistry.build(id); // Construimos el ítem real
                ItemMeta meta = icon.getItemMeta();

                // Inyectamos datos para el MenuListener
                meta.getPersistentDataContainer().set(KEY_ITEM_ID, PersistentDataType.STRING, id);
                meta.getPersistentDataContainer().set(KEY_SLOT_INDEX, PersistentDataType.INTEGER, slotIndex);
                icon.setItemMeta(meta);

                inv.addItem(icon);
            }
        });

        // Botones de control (Desequipar/Volver)
        inv.setItem(22, createControlItem(Material.CAULDRON, "§c✖ Desequipar", "REMOVE", slotIndex));
        inv.setItem(18, createControlItem(Material.ARROW, "§7Volver", "BACK", slotIndex));

        player.openInventory(inv);
    }

    private static ItemStack createControlItem(Material mat, String name, String id, int slotIdx) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.getPersistentDataContainer().set(KEY_ITEM_ID, PersistentDataType.STRING, id);
        meta.getPersistentDataContainer().set(KEY_SLOT_INDEX, PersistentDataType.INTEGER, slotIdx);
        item.setItemMeta(meta);
        return item;
    }
}