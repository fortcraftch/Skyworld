package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.commands.QuestAdminCommand;
import Fortcraft.skyworld.listeners.QuestJoinListener;
import Fortcraft.skyworld.quests.Quest;
import Fortcraft.skyworld.quests.QuestStage;
import Fortcraft.skyworld.quests.QuestType;
import Fortcraft.skyworld.quests.PlayerQuestProgress;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class QuestManager implements Manager {

    private final Map<String, Quest> registry = new HashMap<>();
    private final Map<UUID, Map<String, PlayerQuestProgress>> playerProfiles = new HashMap<>();
    private final Map<UUID, String> trackingQuest = new HashMap<>(); // Jugador -> QuestId seleccionada para rastrear

    private File file;
    private FileConfiguration config;

    @Override
    public void load() {
        setupFile();
        loadQuests();
        Bukkit.getPluginManager().registerEvents(
                new QuestJoinListener(this),
                Skyworld.getInstance()
        );

        PluginCommand command = Skyworld.getInstance().getCommand("questadmin");
        if (command != null) {
            command.setExecutor(new QuestAdminCommand(this));
        }
    }

    @Override
    public void unload() {
        saveAllPlayerProgress();
        registry.clear();
        playerProfiles.clear();
        trackingQuest.clear();
    }

    private void setupFile() {
        this.file = new File(Skyworld.getInstance().getDataFolder(), "quests.yml");
        if (!file.exists()) {
            Skyworld.getInstance().saveResource("quests.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    private void loadQuests() {
        registry.clear();
        ConfigurationSection section = config.getConfigurationSection("quests");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            String title = section.getString(id + ".title");
            Quest quest = new Quest(id, title);

            List<?> stageList = section.getList(id + ".stages");
            if (stageList != null) {
                for (Object obj : stageList) {
                    if (obj instanceof Map<?, ?> map) {
                        String desc = (String) map.get("description");
                        QuestType type = QuestType.valueOf((String) map.get("type"));
                        String target = String.valueOf(map.get("target"));
                        int amount = (int) map.get("amount");

                        quest.addStage(new QuestStage(desc, type, target, amount));
                    }
                }
            }
            registry.put(id.toLowerCase(), quest);
        }
        Skyworld.getInstance().getLogger().info("Sistema de Misiones: Cargadas " + registry.size() + " misiones base.");
    }

    /**
     * Avanza de forma genérica el progreso del objetivo de un jugador
     */
    public void handleProgress(Player player, QuestType type, String targetId, int amount) {
        UUID uuid = player.getUniqueId();
        Map<String, PlayerQuestProgress> activeQuests = playerProfiles.get(uuid);
        if (activeQuests == null) return;

        for (PlayerQuestProgress progress : activeQuests.values()) {
            if (progress.isCompleted()) continue;

            Quest quest = registry.get(progress.getQuestId().toLowerCase());
            if (quest == null || progress.getCurrentStageIndex() >= quest.getStages().size()) continue;

            QuestStage currentStage = quest.getStages().get(progress.getCurrentStageIndex());
            if (currentStage.getType() == type && currentStage.getTargetId().equalsIgnoreCase(targetId)) {

                progress.incrementProgress(amount);

                if (progress.getCurrentProgressAmount() >= currentStage.getRequiredAmount()) {
                    advanceStage(player, progress, quest);
                }
            }
        }
    }

    private void advanceStage(Player player, PlayerQuestProgress progress, Quest quest) {
        int nextIndex = progress.getCurrentStageIndex() + 1;
        progress.setProgressAmount(0);

        if (nextIndex >= quest.getStages().size()) {
            progress.setCompleted(true);
            progress.setStageIndex(nextIndex);
            player.sendMessage("§a§l[Misiones] §f¡Felicidades! Has completado la misión: §b" + quest.getTitle());

            if (quest.getId().equalsIgnoreCase(trackingQuest.get(player.getUniqueId()))) {
                Skyworld.getInstance().getManagerHandler().getNavigationManager().stopGuiding(player);
                trackingQuest.remove(player.getUniqueId());
            }
        } else {
            progress.setStageIndex(nextIndex);
            QuestStage nextStage = quest.getStages().get(nextIndex);
            player.sendMessage("§a§l[Misiones] §fSiguiente etapa: §e" + nextStage.getDescription());

            // Si es la misión actualmente seleccionada en el HUD/Guía y la nueva etapa requiere viajar, encendemos el mapa automáticamente
            if (quest.getId().equalsIgnoreCase(trackingQuest.get(player.getUniqueId()))) {
                updateNavigationGuide(player, nextStage);
            }
        }
    }

    /**
     * Define qué misión controlará el mapa de grafos del jugador
     */
    public void setTrackingQuest(Player player, String questId) {
        UUID uuid = player.getUniqueId();

        // CORRECCIÓN: Si pasamos null, significa que queremos dejar de seguir cualquier misión
        if (questId == null) {
            trackingQuest.remove(uuid);
            return;
        }

        Map<String, PlayerQuestProgress> activeQuests = playerProfiles.computeIfAbsent(uuid, k -> new HashMap<>());

        if (!activeQuests.containsKey(questId.toLowerCase())) {
            // Si el jugador no la tenía aceptada/iniciada, la inicializamos
            activeQuests.put(questId.toLowerCase(), new PlayerQuestProgress(questId));
        }

        trackingQuest.put(uuid, questId.toLowerCase());
        Quest quest = registry.get(questId.toLowerCase());
        PlayerQuestProgress progress = activeQuests.get(questId.toLowerCase());

        player.sendMessage("§b§l[Misiones] §fAhora estás siguiendo la misión: §a" + quest.getTitle());

        if (progress != null && !progress.isCompleted() && progress.getCurrentStageIndex() < quest.getStages().size()) {
            QuestStage currentStage = quest.getStages().get(progress.getCurrentStageIndex());
            updateNavigationGuide(player, currentStage);
        }
    }

    private void updateNavigationGuide(Player player, QuestStage stage) {
        NavigationManager nav = Skyworld.getInstance().getManagerHandler().getNavigationManager();
        if (stage.getType() == QuestType.VISIT_LOCATION) {
            nav.startGuiding(player, stage.getTargetId());
        } else {
            nav.stopGuiding(player); // Si la etapa cambia a minar/matar, limpiamos la estela de partículas
        }
    }

    // --- CARGA Y GUARDADO DE JUGADORES (Se llamará en el DataManager) ---

    public void loadPlayerProgress(UUID uuid, FileConfiguration playerConfig) {
        Map<String, PlayerQuestProgress> progressMap = new HashMap<>();
        String path = "quests-progress";

        if (playerConfig.contains(path)) {
            var section = playerConfig.getConfigurationSection(path);
            if (section != null) {
                for (String questId : section.getKeys(false)) {
                    int index = playerConfig.getInt(path + "." + questId + ".stage");
                    int amount = playerConfig.getInt(path + "." + questId + ".amount");
                    boolean done = playerConfig.getBoolean(path + "." + questId + ".completed");

                    progressMap.put(questId.toLowerCase(), new PlayerQuestProgress(questId, index, amount, done));
                }
            }
        }
        playerProfiles.put(uuid, progressMap);

        String tracked = playerConfig.getString("tracked-quest");
        if (tracked != null) trackingQuest.put(uuid, tracked.toLowerCase());
    }

    public void savePlayerProgress(UUID uuid, FileConfiguration playerConfig) {
        Map<String, PlayerQuestProgress> progressMap = playerProfiles.get(uuid);
        String path = "quests-progress";
        playerConfig.set(path, null);

        if (progressMap != null) {
            for (PlayerQuestProgress p : progressMap.values()) {
                playerConfig.set(path + "." + p.getQuestId() + ".stage", p.getCurrentStageIndex());
                playerConfig.set(path + "." + p.getQuestId() + ".amount", p.getCurrentProgressAmount());
                playerConfig.set(path + "." + p.getQuestId() + ".completed", p.isCompleted());
            }
        }
        playerConfig.set("tracked-quest", trackingQuest.get(uuid));
    }

    private void saveAllPlayerProgress() {
        // Método preventivo de volcado en unload()
        for (UUID uuid : playerProfiles.keySet()) {
            var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
            if (dataManager != null) {
                // Aquí delegarías el guardado regular a los archivos planos de tus usuarios
            }
        }
    }

    public Quest getQuest(String id) { return registry.get(id.toLowerCase()); }
    public Map<String, PlayerQuestProgress> getPlayerQuests(UUID uuid) { return playerProfiles.getOrDefault(uuid, Collections.emptyMap()); }
    public String getTrackedQuestId(UUID uuid) { return trackingQuest.get(uuid); }
}