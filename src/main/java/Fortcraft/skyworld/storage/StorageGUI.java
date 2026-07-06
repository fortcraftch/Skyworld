package Fortcraft.skyworld.storage;

import Fortcraft.skyworld.utils.AnimatedHolder;
import Fortcraft.skyworld.utils.PlayerMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StorageGUI {

    private static final int INV_SIZE = 54;
    private static final int NAV_ROW_START = 45;
    private static final int PAGE_SIZE = 45; // 5 filas de 9

    public static final String KEY_PAGE = "gui_page";
    public static final String KEY_ACTION = "gui_action"; // NEXT, PREV, EXIT

    public static void open(Player player, StorageBag bag, PlayerMode mode, int page) {
        String titleString = mode.getLegacyColor() + "Almacén: " + mode.getDisplayName() + " (Pág. " + (page + 1) + ")";

        Component titleComponent = LegacyComponentSerializer.legacySection().deserialize(titleString);

        Inventory inv = Bukkit.createInventory(new AnimatedHolder(), INV_SIZE, titleComponent);
        Map<String, StorageBag.StorageItemData> rawItems = bag.getItemsForMode(mode);

        fillInventoryWithPage(inv, rawItems, page);
        renderNavigationBar(inv, page, calculateMaxPages(rawItems));

        player.openInventory(inv);
    }

    private static void fillInventoryWithPage(Inventory inv, Map<String, StorageBag.StorageItemData> rawItems, int page) {
        List<StorageBag.StorageItemData> sortedData = new ArrayList<>(rawItems.values());

        // ORDENADO INTELIGENTE
        sortedData.sort((a, b) -> {
            // 1. Comparar por rareza (El ordinal más alto es Exótico = más importante)
            // Usamos b.ordinal - a.ordinal para que los más altos salgan PRIMERO
            int rarityCompare = Integer.compare(b.getRarity().ordinal(), a.getRarity().ordinal());

            if (rarityCompare != 0) {
                return rarityCompare;
            }

            // 2. Si tienen la misma rareza, ordenar alfabéticamente
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });

        int startIndex = page * PAGE_SIZE;
        int endIndex = startIndex + PAGE_SIZE;
        int currentGlobalSlot = 0;

        for (StorageBag.StorageItemData data : sortedData) {
            Material material = data.getMaterial();
            int totalAmount = data.getAmount();
            int maxStack = material.getMaxStackSize();
            int stacksNeeded = (totalAmount + maxStack - 1) / maxStack;

            if (currentGlobalSlot + stacksNeeded > startIndex && currentGlobalSlot < endIndex) {
                int remaining = totalAmount;

                for (int i = 0; i < stacksNeeded; i++) {
                    int stackAmount = Math.min(remaining, maxStack);

                    if (currentGlobalSlot >= startIndex && currentGlobalSlot < endIndex) {
                        ItemStack item = createVisualStack(data, stackAmount);
                        inv.setItem(currentGlobalSlot - startIndex, item);
                    }

                    remaining -= stackAmount;
                    currentGlobalSlot++;

                    if (currentGlobalSlot >= endIndex) return;
                }
            } else {
                currentGlobalSlot += stacksNeeded;
            }
        }
    }

    private static ItemStack createVisualStack(StorageBag.StorageItemData data, int amount) {
        ItemStack item = new ItemStack(data.getMaterial(), amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection()
                    .deserialize(data.getDisplayName())
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection()
                    .deserialize("§7Total almacenado: §f" + data.getAmount())
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());
            lore.add(LegacyComponentSerializer.legacySection()
                    .deserialize("§eClick para ver usos/craftear")
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            var pdc = meta.getPersistentDataContainer();

            // ID del item para la lógica del almacenamiento
            pdc.set(
                    new org.bukkit.NamespacedKey("fortcraft", "storage_id"),
                    PersistentDataType.STRING,
                    data.getDisplayName()
            );

            String cleanName = stripColorCodes(data.getDisplayName());

            pdc.set(Fortcraft.skyworld.Skyworld.getKey("rarity"), PersistentDataType.STRING, data.getRarity().name());
            pdc.set(Fortcraft.skyworld.Skyworld.getKey("original_name"), PersistentDataType.STRING, cleanName);

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Helper para limpiar los prefijos de color y aislar el nombre base del ítem
     */
    private static String stripColorCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "") // Remueve tags MiniMessage
                .replaceAll("§[0-9a-fklmnorx]", "") // Remueve colores legacy
                .replace("&", "")
                .trim();
    }

    private static int calculateMaxPages(Map<String, StorageBag.StorageItemData> rawItems) {
        int totalStacks = 0;
        for (StorageBag.StorageItemData data : rawItems.values()) {
            int maxStack = data.getMaterial().getMaxStackSize();
            totalStacks += (data.getAmount() + maxStack - 1) / maxStack;
        }
        return (int) Math.ceil((double) totalStacks / PAGE_SIZE);
    }

    private static void renderNavigationBar(Inventory inv, int currentPage, int maxPages) {
        ItemStack panel = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta panelMeta = panel.getItemMeta();
        if (panelMeta != null) {
            panelMeta.displayName(Component.empty());
            panel.setItemMeta(panelMeta);
        }

        for (int i = NAV_ROW_START; i < INV_SIZE; i++) {
            inv.setItem(i, panel);
        }

        // --- NAVEGACIÓN ESTÁNDAR ---
        if (currentPage > 0) {
            inv.setItem(45, createNavButton(Material.ARROW, "§e← Página Anterior", "PREV", currentPage));
        }

        inv.setItem(49, createNavButton(Material.BARRIER, "§cCerrar Almacén", "EXIT", currentPage));

        if (currentPage < maxPages - 1) {
            inv.setItem(53, createNavButton(Material.ARROW, "§ePágina Siguiente →", "NEXT", currentPage));
        }

        if (maxPages >= 10) {
            // Retroceder 5 páginas
            if (currentPage >= 5) {
                inv.setItem(47, createNavButton(Material.FEATHER, "§b« Retroceder 5 págs.", "PREV_5", currentPage));
            }

            // Avanzar 5 páginas
            if (currentPage + 5 < maxPages) {
                inv.setItem(51, createNavButton(Material.FEATHER, "§bAvanzar 5 págs. »", "NEXT_5", currentPage));
            }
        }
    }

    private static ItemStack createNavButton(Material mat, String name, String action, int currentPage) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection()
                    .deserialize(name)
                    .decoration(TextDecoration.ITALIC, false));

            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey("fortcraft", KEY_ACTION),
                    PersistentDataType.STRING,
                    action
            );
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey("fortcraft", KEY_PAGE),
                    PersistentDataType.INTEGER,
                    currentPage
            );

            item.setItemMeta(meta);
        }
        return item;
    }
}