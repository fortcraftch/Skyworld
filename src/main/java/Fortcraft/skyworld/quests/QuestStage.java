package Fortcraft.skyworld.quests;

public class QuestStage {
    private final String description;
    private final QuestType type;
    private final String targetId; // ID del bloque, ID del destino de grafos, ID del NPC o Tipo de Mob
    private final int requiredAmount;

    public QuestStage(String description, QuestType type, String targetId, int requiredAmount) {
        this.description = description;
        this.type = type;
        this.targetId = targetId;
        this.requiredAmount = requiredAmount;
    }

    public String getDescription() { return description; }
    public QuestType getType() { return type; }
    public String getTargetId() { return targetId; }
    public int getRequiredAmount() { return requiredAmount; }
}