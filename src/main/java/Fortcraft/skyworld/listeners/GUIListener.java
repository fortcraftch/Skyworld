package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.utils.PlayerMode;
import Fortcraft.skyworld.logbook.LogbookGUI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
    private final NamespacedKey KEY_CHANGE_MODE;
    private final NamespacedKey KEY_LOGBOOK_CONTEXT;

    public GUIListener() {
        // Inicializamos las keys una sola vez para mejorar rendimiento
        this.KEY_BIOME_ID = new NamespacedKey(Skyworld.getInstance(), "skyworld_biome_id");
        this.KEY_BACK_BUTTON = new NamespacedKey(Skyworld.getInstance(), "back_button");
        this.KEY_CHANGE_MODE = Skyworld.getKey("change_mode");
        this.KEY_LOGBOOK_CONTEXT = Skyworld.getKey("logbook_context");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // Filtros básicos de seguridad
        if (e.getCurrentItem() == null) return;
        if (e.getClickedInventory() == null) return;

        // Obtenemos el título
        String title = LegacyComponentSerializer.legacySection().serialize(e.getView().title());

        // -----------------------------------------------------------
        // CASO 1: BITÁCORA (Navegación, Habilidades y Biomas)
        // -----------------------------------------------------------
        if (title.contains("Bitácora") || title.contains("Ruta:") || title.contains("Habilidad:")
                || title.contains("Biomas:") || title.contains("Bioma:") || title.contains("Capa:")
                || title.contains("Cultivos:") || title.contains("Árboles:") || title.contains("Yacimientos:")) {

            e.setCancelled(true); // Cancela para evitar que se muevan items
            handleLogbookClick(e, title);
        }
    }

    private void handleLogbookClick(InventoryClickEvent e, String title) {
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Detección precisa del modo actual según el título de la interfaz
        PlayerMode currentMode = PlayerMode.GLOBAL;
        if (title.contains("Pesca")) {
            currentMode = PlayerMode.FISHING;
        } else if (title.contains("Minería") || title.contains("Minera")) {
            currentMode = PlayerMode.MINING;
        } else if (title.contains("Granja")) {
            currentMode = PlayerMode.FARMING;
        } else if (title.contains("Foraging")) {
            currentMode = PlayerMode.FORAGING;
        } else if (title.contains("Arqueología") || title.contains("Arqueologia")) {
            currentMode = PlayerMode.EXCAVATION;
        } else if (title.contains("Bioma:")) {
            currentMode = PlayerMode.FISHING;
        } else if (title.contains("Capa:")) {
            currentMode = PlayerMode.MINING;
        } else if (title.contains("Cultivos:")) {
            currentMode = PlayerMode.FARMING;
        } else if (title.contains("Árboles:")) {
            currentMode = PlayerMode.FORAGING;
        } else if (title.contains("Yacimientos:")) {
            currentMode = PlayerMode.EXCAVATION;
        }

        var pdc = meta.getPersistentDataContainer();

        // A) CAMBIAR DE MODO O NAVEGAR CON CONTEXTO
        if (pdc.has(KEY_CHANGE_MODE, PersistentDataType.STRING)) {
            String modeStr = pdc.get(KEY_CHANGE_MODE, PersistentDataType.STRING);
            String context = pdc.get(KEY_LOGBOOK_CONTEXT, PersistentDataType.STRING);

            try {
                PlayerMode targetMode = PlayerMode.valueOf(modeStr);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                LogbookGUI.open(p, targetMode, context);
            } catch (IllegalArgumentException ignored) { }
            return;
        }

        // B) CLICK EN UN BIOMA ESPECÍFICO (Entrar al detalle del bioma)
        if (pdc.has(KEY_BIOME_ID, PersistentDataType.STRING)) {
            String biomeId = pdc.get(KEY_BIOME_ID, PersistentDataType.STRING);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            LogbookGUI.open(p, currentMode, biomeId);
            return;
        }

        // C) BOTÓN VOLVER (Navegación contextual hacia atrás)
        if (pdc.has(KEY_BACK_BUTTON, PersistentDataType.BYTE)) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

            String context = pdc.get(KEY_LOGBOOK_CONTEXT, PersistentDataType.STRING);
            String modeStr = pdc.get(KEY_CHANGE_MODE, PersistentDataType.STRING);

            if (modeStr != null) {
                try {
                    PlayerMode targetMode = PlayerMode.valueOf(modeStr);
                    LogbookGUI.open(p, targetMode, context);
                    return;
                } catch (IllegalArgumentException ignored) { }
            }

            LogbookGUI.open(p, PlayerMode.GLOBAL, null);
        }
    }
}