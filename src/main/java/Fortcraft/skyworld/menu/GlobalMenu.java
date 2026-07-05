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

    private static ItemStack createQuestMenuItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§l📖 Diario de Misiones");
            meta.setLore(List.of(
                    "§7Revisa tus misiones en progreso,",
                    "§7tus objetivos completados y activa",
                    "§7el rastreador visual de partículas.",
                    "",
                    "§e▶ Click para abrir el diario"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}