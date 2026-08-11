package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.menu.LoadoutGUI;
import Fortcraft.skyworld.utils.PlayerMode;
import Fortcraft.skyworld.logbook.LogbookGUI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class GUIListener implements Listener {

    private final NamespacedKey KEY_BIOME_ID;
    private final NamespacedKey KEY_BACK_BUTTON;

    public GUIListener() {
        // Inicializamos las keys una sola vez para mejorar rendimiento
        this.KEY_BIOME_ID = new NamespacedKey(Skyworld.getInstance(), "skyworld_biome_id");
        this.KEY_BACK_BUTTON = new NamespacedKey(Skyworld.getInstance(), "back_button");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // Filtros básicos de seguridad
        if (e.getCurrentItem() == null) return;
        if (e.getClickedInventory() == null) return;

        // Obtenemos el título
        String title = LegacyComponentSerializer.legacySection().serialize(e.getView().title());

        // -----------------------------------------------------------
        // CASO 1: BITÁCORA (Navegación y Biomas)
        // Añadimos "Yacimientos:" para el sistema de excavación
        // -----------------------------------------------------------
        if (title.contains("Bitácora") || title.contains("Bioma:") || title.contains("Capa:") || title.contains("Cultivos:") || title.contains("Árboles:") || title.contains("Yacimientos:")) {
            e.setCancelled(true); // Nadie puede robar items de la bitácora
            handleLogbookClick(e, title); // Pasamos el título para detectar el modo
        }
    }

    private void handleLogbookClick(InventoryClickEvent e, String title) {
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PlayerMode currentMode = PlayerMode.GLOBAL;
        if (title.contains("Pesca") || title.contains("Bioma:")) {
            currentMode = PlayerMode.FISHING;
        } else if (title.contains("Minería") || title.contains("Capa:")) {
            currentMode = PlayerMode.MINING;
        } else if (title.contains("Granja") || title.contains("Cultivos:")) {
            currentMode = PlayerMode.FARMING;
        } else if (title.contains("Foraging") || title.contains("Árboles:")) {
            currentMode = PlayerMode.FORAGING;
        } else if (title.contains("Arqueologia") || title.contains("Yacimientos:")) {
            currentMode = PlayerMode.EXCAVATION;
        }

        // A) CAMBIAR DE MODO (Desde Global a Profesiones)
        if (meta.getPersistentDataContainer().has(Skyworld.getKey("change_mode"), PersistentDataType.STRING)) {
            String modeStr = meta.getPersistentDataContainer().get(Skyworld.getKey("change_mode"), PersistentDataType.STRING);
            PlayerMode targetMode = PlayerMode.valueOf(modeStr);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            LogbookGUI.open(p, targetMode, null);
            return;
        }

        // B) CLICK EN UN BIOMA (Entrar al nivel 2)
        if (meta.getPersistentDataContainer().has(KEY_BIOME_ID, PersistentDataType.STRING)) {
            String biomeId = meta.getPersistentDataContainer().get(KEY_BIOME_ID, PersistentDataType.STRING);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            LogbookGUI.open(p, currentMode, biomeId);
            return;
        }

        // C) BOTÓN VOLVER (Navegación hacia atrás)
        if (meta.getPersistentDataContainer().has(KEY_BACK_BUTTON, PersistentDataType.BYTE)) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

            // Si estamos dentro de un bioma específico, volvemos a la lista de biomas
            if (title.contains("Bioma:") || title.contains("Capa:") || title.contains("Cultivos:") || title.contains("Árboles:") || title.contains("Yacimientos:")) {
                LogbookGUI.open(p, currentMode, null);
            } else {
                // Si estamos en la lista de biomas, volvemos al menú GLOBAL
                LogbookGUI.open(p, PlayerMode.GLOBAL, null);
            }
        }
    }
}