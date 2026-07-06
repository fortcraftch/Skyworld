package Fortcraft.skyworld.quests;

import java.util.ArrayList;
import java.util.List;

public class Quest {
    private final String id;
    private final String title;
    private final List<QuestStage> stages;

    private int rewardExp;
    private double rewardMoney; // NUEVO: Almacenamiento de dinero de recompensa
    private final List<QuestRewardEntry> rewardItems; // Para la Armería
    private final List<QuestRewardEntry> rewardDrops; // Para la Infinibag

    public Quest(String id, String title) {
        this.id = id;
        this.title = title;
        this.stages = new ArrayList<>();
        this.rewardItems = new ArrayList<>();
        this.rewardDrops = new ArrayList<>();
    }

    public void addStage(QuestStage stage) { stages.add(stage); }

    public void setRewardExp(int rewardExp) { this.rewardExp = rewardExp; }
    public int getRewardExp() { return rewardExp; }

    public void setRewardMoney(double rewardMoney) { this.rewardMoney = rewardMoney; }
    public double getRewardMoney() { return rewardMoney; }

    public void addRewardItem(String itemId, int amount) { this.rewardItems.add(new QuestRewardEntry(itemId, amount)); }
    public List<QuestRewardEntry> getRewardItems() { return rewardItems; }

    public void addRewardDrop(String dropId, int amount) { this.rewardDrops.add(new QuestRewardEntry(dropId, amount)); }
    public List<QuestRewardEntry> getRewardDrops() { return rewardDrops; }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public List<QuestStage> getStages() { return stages; }

    public static record QuestRewardEntry(String id, int amount) {}
}