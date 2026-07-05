package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.QuestManager;
import Fortcraft.skyworld.quests.Quest;
import Fortcraft.skyworld.quests.QuestStage;
import Fortcraft.skyworld.quests.QuestType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class QuestJoinListener implements Listener {

    private final QuestManager questManager;

    public QuestJoinListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        var handler = Skyworld.getInstance().getManagerHandler();

        // Ejecución inmediata sin delay
        String trackedQuestId = questManager.getTrackedQuestId(player.getUniqueId());
        if (trackedQuestId == null) return;

        var progressMap = questManager.getPlayerQuests(player.getUniqueId());
        var progress = progressMap.get(trackedQuestId);

        // Si tiene una misión activa y no está completada, reactivamos la guía
        if (progress != null && !progress.isCompleted()) {
            Quest quest = questManager.getQuest(trackedQuestId);
            if (quest != null && progress.getCurrentStageIndex() < quest.getStages().size()) {
                QuestStage currentStage = quest.getStages().get(progress.getCurrentStageIndex());

                // Si la etapa actual requiere viajar, encendemos el PathNavigationTask
                if (currentStage.getType() == QuestType.VISIT_LOCATION) {
                    handler.getNavigationManager().startGuiding(player, currentStage.getTargetId());
                }
            }
        }
    }
}