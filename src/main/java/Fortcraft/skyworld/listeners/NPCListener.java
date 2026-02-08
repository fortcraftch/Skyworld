package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.MenuManager;
import Fortcraft.skyworld.managers.NPCManager;
import Fortcraft.skyworld.menu.SkyblockMenu;
import Fortcraft.skyworld.npcs.SkyblockNPC;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.MetadataValue;

public class NPCListener implements Listener {

    private final NPCManager manager;

    public NPCListener(NPCManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onNPCClick(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        Entity clicked = e.getRightClicked();

        // Buscamos el ID del NPC en los metadatos de la entidad
        if (!clicked.hasMetadata("NPC_DATA_ID")) return;

        String npcId = "";
        for (MetadataValue value : clicked.getMetadata("NPC_DATA_ID")) {
            npcId = value.asString();
            break;
        }

        SkyblockNPC npc = manager.getNPCById(npcId);

        String menuType = npc.getMenuType();

        // Obtener el gestor de menús
        MenuManager menuManager = Skyworld.getInstance().getManagerHandler().getMenuManager();
        SkyblockMenu menu = menuManager.getMenu(menuType);

        if (menu != null) {
            menu.open(e.getPlayer());
        } else {
            // Fallback por si el menú no existe o es "QUEST"
            if (menuType.equals("QUEST")) {
                e.getPlayer().sendMessage("§eNPC: §fPronto...");
            } else {
                e.getPlayer().sendMessage("§cError: Menú '" + menuType + "' no configurado.");
            }
        }
    }
}