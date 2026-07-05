package Fortcraft.skyworld.quests;

public class PlayerQuestProgress {
    private final String questId;
    private int currentStageIndex;
    private int currentProgressAmount;
    private boolean completed;

    public PlayerQuestProgress(String questId) {
        this.questId = questId;
        this.currentStageIndex = 0;
        this.currentProgressAmount = 0;
        this.completed = false;
    }

    public PlayerQuestProgress(String questId, int stageIndex, int progressAmount, boolean completed) {
        this.questId = questId;
        this.currentStageIndex = stageIndex;
        this.currentProgressAmount = progressAmount;
        this.completed = completed;
    }

    public String getQuestId() { return questId; }
    public int getCurrentStageIndex() { return currentStageIndex; }
    public int getCurrentProgressAmount() { return currentProgressAmount; }
    public boolean isCompleted() { return completed; }

    public void setStageIndex(int index) { this.currentStageIndex = index; }
    public void setProgressAmount(int amount) { this.currentProgressAmount = amount; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public void incrementProgress(int amount) {
        this.currentProgressAmount += amount;
    }
}