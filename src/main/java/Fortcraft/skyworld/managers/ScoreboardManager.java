package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.utils.PlayerMode;
import Fortcraft.skyworld.quests.Quest;
import Fortcraft.skyworld.quests.QuestStage;
import Fortcraft.skyworld.quests.PlayerQuestProgress;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class ScoreboardManager implements Manager {

    private int taskID = -1;

    @Override
    public void load() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Skyworld.getInstance(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
        }, 0L, 20L); // Cada segundo
    }

    @Override
    public void unload() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
        }
    }

    public void updateScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
        if (bukkitManager == null) return;

        Scoreboard board = bukkitManager.getNewScoreboard();
        Objective obj = board.registerNewObjective("skyworld", Criteria.DUMMY, "§6§lFORTCRAFT");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        var managerHandler = Skyworld.getInstance().getManagerHandler();
        PlayerMode mode = managerHandler.getHotbarManager().getMode(player);

        double coins = 0.0;
        try {
            coins = managerHandler.getEconomyManager().getBalance(player);
        } catch (NullPointerException ignored) {}

        int i = 15;
        addScore(obj, "§7" + java.time.LocalDate.now(), i--);
        addScore(obj, "§1 ", i--); // Espacio invisible 1

        addScore(obj, "§fModo:", i--);
        addScore(obj, mode.getLegacyColor() + mode.getDisplayName(), i--);

        addScore(obj, "§2 ", i--); // Espacio invisible 2

        addScore(obj, "§fMonedas:", i--);
        addScore(obj, "§e" + String.format("%,.1f", coins) + " ⛁", i--);

        addScore(obj, "§3 ", i--); // Espacio invisible 3

        // --- SISTEMA DINÁMICO DE MISIONES EN SCOREBOARD ---
        var questManager = managerHandler.getQuestManager();
        String trackedId = questManager.getTrackedQuestId(player.getUniqueId());

        if (trackedId != null) {
            Quest quest = questManager.getQuest(trackedId);
            var progressMap = questManager.getPlayerQuests(player.getUniqueId());
            PlayerQuestProgress progress = progressMap.get(trackedId);

            if (quest != null && progress != null && !progress.isCompleted()) {
                addScore(obj, "§d📖 Misión:", i--);

                // Cortamos el título si excede los límites estéticos
                String title = quest.getTitle();
                addScore(obj, (title.length() > 24 ? title.substring(0, 22) + ".." : title), i--);

                if (progress.getCurrentStageIndex() < quest.getStages().size()) {
                    QuestStage stage = quest.getStages().get(progress.getCurrentStageIndex());

                    // Descripción corta del objetivo
                    String desc = stage.getDescription();
                    addScore(obj, " §7» " + (desc.length() > 22 ? desc.substring(0, 20) + ".." : desc), i--);

                    // Lógica del progreso (Porcentaje vs Cantidad Única)
                    if (stage.getRequiredAmount() > 1) {
                        int current = progress.getCurrentProgressAmount();
                        int required = stage.getRequiredAmount();
                        // Ecuación de porcentaje: (actual / requerido) * 100
                        int percentage = (int) (((double) current / required) * 100);

                        addScore(obj, " §fProgreso: §a" + percentage + "%", i--);
                    } else {
                        // Si amount es 1, omitimos el porcentaje y solo mostramos que está pendiente
                        addScore(obj, " §fProgreso: §cIncompleto", i--);
                    }
                }
                addScore(obj, "§5 ", i--); // Espacio dinámico para separar
            }
        } else {
            // Si no está siguiendo nada, muestra la zona por defecto que ya tenías
            addScore(obj, "§fZona:", i--);
            addScore(obj, "§aGlobal", i--);
            addScore(obj, "§5 ", i--);
        }

        addScore(obj, "§eplay.fortcraft.net", i--);

        player.setScoreboard(board);
    }

    private void addScore(Objective obj, String text, int score) {
        Score s = obj.getScore(text);
        s.setScore(score);
    }
}