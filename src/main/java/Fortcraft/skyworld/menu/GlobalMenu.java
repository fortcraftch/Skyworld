package Fortcraft.skyworld.menu;

import Fortcraft.skyworld.utils.PlayerMode;
import net.kyori.adventure.text.Component;
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
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(title));

        // --- BOTÓN UNIVERSAL: CONFIGURAR EQUIPAMIENTO ---
        // Lo ponemos en el slot 22 (centro inferior)
        inv.setItem(22, createConfigItem(mode));

        // Lógica para llenar el inventario según el modo
        switch (mode) {
            case GLOBAL -> {
                // Tus items de aventura...
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
            meta.setDisplayName("§6⚙ Configurar Equipamiento");
            meta.setLore(List.of(
                    "§7Configura tus 6 slots de",
                    "§7acceso rápido para el modo " + mode.getDisplayName(),
                    "",
                    "§e▶ Click para editar"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}