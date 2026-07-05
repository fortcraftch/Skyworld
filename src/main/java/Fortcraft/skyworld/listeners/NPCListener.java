package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.MenuManager;
import Fortcraft.skyworld.managers.NPCManager;
import Fortcraft.skyworld.menu.SkyblockMenu;
import Fortcraft.skyworld.npcs.SkyblockNPC;
import Fortcraft.skyworld.quests.QuestType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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
        if (!clicked.hasMetadata("NPC_DATA_ID")) return;

        String npcId = "";
        for (MetadataValue value : clicked.getMetadata("NPC_DATA_ID")) {
            npcId = value.asString();
            break;
        }

        Player player = e.getPlayer();
        SkyblockNPC npc = manager.getNPCById(npcId);
        if (npc == null) return;

        // INYECCIÓN DE MISIÓN: Hablar con el NPC
        Skyworld.getInstance().getManagerHandler().getQuestManager()
                .handleProgress(player, QuestType.TALK_NPC, npc.getId(), 1);

        String menuType = npc.getMenuType();
        MenuManager menuManager = Skyworld.getInstance().getManagerHandler().getMenuManager();
        SkyblockMenu menu = menuManager.getMenu(menuType);

        if (menu != null) {
            menu.open(player);
        } else {
            if (menuType.equals("QUEST")) {
                player.sendMessage("§eNPC: §fNo tengo más tareas para ti por ahora.");
            } else {
                player.sendMessage("§cError: Menú '" + menuType + "' no configurado.");
            }
        }
    }
}