package Fortcraft.skyworld.menu;

import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.utils.PlayerMode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GlobalMenu {

    public static void open(Player player, PlayerMode mode) {
        String title = mode.getLegacyColor() + "Menú " + mode.getDisplayName();
        Inventory inv = Bukkit.createInventory(null, 27, ColorUtils.format(title));

        // --- BOTÓN UNIVERSAL: CONFIGURAR EQUIPAMIENTO ---
        inv.setItem(22, createConfigItem(mode));

        // Lógica para llenar el inventario según el modo
        switch (mode) {
            case GLOBAL -> {
                inv.setItem(13, createQuestMenuItem());
            }
            case MINING -> {
                // Tus items de minería...
            }
            // etc...
        }
        player.openInventory(inv);
    }

    private static ItemStack createConfigItem(PlayerMode mode) {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.format("&6Configurar Equipamiento"));
            meta.lore(List.of(
                    ColorUtils.format("&7Configura tus 6 slots de"),
                    ColorUtils.format("&7acceso rápido para el modo " + mode.getDisplayName()),
                    ColorUtils.format(""),
                    ColorUtils.format("&e▶ Click para editar")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createQuestMenuItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.format("&bDiario de Misiones"));
            meta.lore(List.of(
                    ColorUtils.format("&7Revisa tus misiones en progreso,"),
                    ColorUtils.format("&7tus objetivos completados y activa"),
                    ColorUtils.format("&7el rastreador visual de partículas."),
                    ColorUtils.format(""),
                    ColorUtils.format("&e▶ Click para abrir el diario")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}