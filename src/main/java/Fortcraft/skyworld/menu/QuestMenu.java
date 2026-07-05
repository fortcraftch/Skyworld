package Fortcraft.skyworld.menu;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.QuestManager;
import Fortcraft.skyworld.quests.Quest;
import Fortcraft.skyworld.quests.PlayerQuestProgress;
import Fortcraft.skyworld.quests.QuestStage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestMenu {

    public static void open(Player player) {
        QuestManager questManager = Skyworld.getInstance().getManagerHandler().getQuestManager();

        // Creamos un menú con la base de tu clase SkyblockMenu (Tamaño: 27 slots)
        SkyblockMenu menu = new SkyblockMenu("quest_menu", "&9&lMis Misiones Activas", 27);

        Map<String, PlayerQuestProgress> playerQuests = questManager.getPlayerQuests(player.getUniqueId());
        String trackedQuestId = questManager.getTrackedQuestId(player.getUniqueId());

        int slot = 10; // Empezamos a rellenar en la fila central

        for (PlayerQuestProgress progress : playerQuests.values()) {
            if (slot > 16) break; // Evitamos desbordar la fila central del inventario

            Quest quest = questManager.getQuest(progress.getQuestId());
            if (quest == null) continue;

            boolean isTracked = quest.getId().equalsIgnoreCase(trackedQuestId);
            Material iconMaterial = progress.isCompleted() ? Material.WRITTEN_BOOK : Material.BOOK;
            String name = (isTracked ? "&a&l▶ " : "&e") + quest.getTitle();

            List<String> lore = new ArrayList<>();
            lore.add("&7-------------------");

            if (progress.isCompleted()) {
                lore.add("&a✔ Completada completamente.");
            } else {
                QuestStage stage = quest.getStages().get(progress.getCurrentStageIndex());
                lore.add("&7Objetivo actual:");
                lore.add("&f" + stage.getDescription());

                if (stage.getRequiredAmount() > 1) {
                    lore.add("&7Progreso: &e" + progress.getCurrentProgressAmount() + "&7/&e" + stage.getRequiredAmount());
                }
                lore.add("");
                lore.add(isTracked ? "&b⭐ Siguiendo esta ruta" : "&e▶ Click para rastrear");
            }
            lore.add("&7-------------------");

            // Construimos tu MenuItem adaptado
            MenuItem item = new MenuItem(
                    slot,
                    iconMaterial,
                    name,
                    lore,
                    progress.isCompleted() ? "NONE" : "COMMAND",
                    "pathtool goto " + quest.getId(), // Al hacer click ejecuta el comando de rastreo
                    1,
                    0.0
            );

            menu.addItem(item);
            slot++;
        }

        // Botón de cierre en el slot 22 usando tu sistema
        menu.addItem(new MenuItem(22, Material.BARRIER, "&cCerrar Menú", List.of("&7Salir de la interfaz"), "CLOSE", "", 1, 0.0));

        menu.open(player);
    }
}