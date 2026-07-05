package Fortcraft.skyworld.quests;

import java.util.ArrayList;
import java.util.List;

public class Quest {
    private final String id;
    private final String title;
    private final List<QuestStage> stages;

    public Quest(String id, String title) {
        this.id = id;
        this.title = title;
        this.stages = new ArrayList<>();
    }

    public void addStage(QuestStage stage) {
        stages.add(stage);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public List<QuestStage> getStages() { return stages; }
}