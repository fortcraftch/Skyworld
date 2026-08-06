package Fortcraft.skyworld.menu;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.utils.HotbarSlot;
import Fortcraft.skyworld.utils.PlayerMode;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class LoadoutGUI {

    // Holder personalizado para identificar el menú sin depender del título
    public static class LoadoutHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final LoadoutHolder HOLDER = new LoadoutHolder();

    public static final String PREFIX = "&8Configurar: ";
    public static final String SELECTOR_PREFIX = "&8Seleccionar para ";

    public static final NamespacedKey KEY_SLOT_INDEX = new NamespacedKey("fortcraft", "loadout_slot_idx");
    public static final NamespacedKey KEY_ITEM_ID = new NamespacedKey("fortcraft", "loadout_item_id");

    public static void open(Player player) {
        var managerHandler = Skyworld.getInstance().getManagerHandler();
        var data = managerHandler.getDataManager().getPlayerData(player.getUniqueId());
        PlayerMode currentMode = managerHandler.getHotbarManager().getMode(player);

        // Pasamos HOLDER como primer argumento
        Inventory inv = Bukkit.createInventory(HOLDER, 9, ColorUtils.format(PREFIX + currentMode.getDisplayName()));

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(Component.empty());
            glass.setItemMeta(glassMeta);
        }
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
                meta.displayName(ColorUtils.format("&e" + slot.getDisplayName()));

                List<Component> lore = new ArrayList<>();
                lore.add(ColorUtils.format("&7Estado: " + (currentItemId == null ? "&cVacío" : "&aEquipado")));
                lore.add(Component.empty());
                lore.add(ColorUtils.format("&e▶ Click para cambiar"));

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

        // Pasamos HOLDER como primer argumento
        Inventory inv = Bukkit.createInventory(HOLDER, 27, ColorUtils.format(SELECTOR_PREFIX + slotType.getDisplayName()));

        String filterCategory = switch (slotType) {
            case PRIMARY, SECONDARY -> "WEAPON";
            case SUPPORT -> "TOOL";
            case CONSUMABLE_1, CONSUMABLE_2 -> "CONSUMABLE";
            default -> "ANY";
        };

        ItemRegistry.getItemTemplates().forEach((id, data) -> {
            if (filterCategory.equals("ANY") || data.category().equalsIgnoreCase(filterCategory)) {
                ItemStack icon = ItemRegistry.build(id);
                ItemMeta meta = icon.getItemMeta();

                if (meta != null) {
                    meta.getPersistentDataContainer().set(KEY_ITEM_ID, PersistentDataType.STRING, id);
                    meta.getPersistentDataContainer().set(KEY_SLOT_INDEX, PersistentDataType.INTEGER, slotIndex);
                    icon.setItemMeta(meta);
                }

                inv.addItem(icon);
            }
        });

        inv.setItem(22, createControlItem(Material.CAULDRON, "&c✖ Desequipar", "REMOVE", slotIndex));
        inv.setItem(18, createControlItem(Material.ARROW, "&7Volver", "BACK", slotIndex));

        player.openInventory(inv);
    }

    private static ItemStack createControlItem(Material mat, String name, String id, int slotIdx) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.format(name));
            meta.getPersistentDataContainer().set(KEY_ITEM_ID, PersistentDataType.STRING, id);
            meta.getPersistentDataContainer().set(KEY_SLOT_INDEX, PersistentDataType.INTEGER, slotIdx);
            item.setItemMeta(meta);
        }
        return item;
    }
}