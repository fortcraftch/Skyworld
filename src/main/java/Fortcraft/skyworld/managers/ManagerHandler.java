package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.listeners.ZoneInteractionListener;
import org.bukkit.Bukkit;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ManagerHandler {

    private final List<Manager> managers = new ArrayList<>();

    private MobDisplayManager mobDisplayManager;
    private ZoneManager zoneManager;
    private FishingManager fishingManager;
    private MiningManager miningManager;
    private FarmManager farmManager;
    private ForagingManager foragingManager;
    private RegionManager regionManager;
    private DataManager dataManager;
    private HotbarManager hotbarManager;
    private EconomyManager economyManager;
    private ScoreboardManager scoreboardManager;
    private NPCManager npcManager;
    private MenuManager menuManager;
    private NavigationManager navigationManager;
    private QuestManager questManager;
    private StorageManager storageManager;
    private PartyManager partyManager;


    public void loadManagers() {
        mobDisplayManager = new MobDisplayManager();
        zoneManager = new ZoneManager();
        fishingManager = new FishingManager();
        miningManager = new MiningManager();
        farmManager = new FarmManager();
        foragingManager = new ForagingManager();
        regionManager = new RegionManager();
        dataManager = new DataManager();
        hotbarManager = new HotbarManager();
        economyManager = new EconomyManager();
        scoreboardManager = new ScoreboardManager();
        npcManager = new NPCManager();
        menuManager = new MenuManager();
        navigationManager = new NavigationManager();
        questManager = new QuestManager();
        storageManager = new StorageManager();
        RespawnManager respawnManager = new RespawnManager(zoneManager);
        MenuAnimationManager menuAnimationManager = new MenuAnimationManager();
        partyManager = new PartyManager();

        managers.add(mobDisplayManager);
        managers.add(zoneManager);
        managers.add(fishingManager);
        managers.add(miningManager);
        managers.add(farmManager);
        managers.add(foragingManager);
        managers.add(regionManager);
        managers.add(dataManager);
        managers.add(hotbarManager);
        managers.add(economyManager);
        managers.add(scoreboardManager);
        managers.add(npcManager);
        managers.add(menuManager);
        managers.add(navigationManager);
        managers.add(questManager);
        managers.add(storageManager);
        managers.add(respawnManager);
        managers.add(menuAnimationManager);
        managers.add(partyManager);

        managers.forEach(Manager::load);

        Bukkit.getPluginManager().registerEvents(
                new ZoneInteractionListener(this),
                Skyworld.getInstance()
        );

    }

    public void unloadManagers() {
        managers.forEach(Manager::unload);
        managers.clear();
    }

    public MobDisplayManager getMobDisplayManager() {
        return mobDisplayManager;
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public MenuManager getMenuManager() { return menuManager; }

    public FishingManager getFishingManager() {
        return fishingManager;
    }

    public MiningManager getMiningManager() { return miningManager; }

    public FarmManager getFarmManager() { return farmManager; }

    public ForagingManager getForagingManager() { return foragingManager; }

    public RegionManager getRegionManager() { return regionManager; }

    public DataManager getDataManager() { return dataManager; }

    public HotbarManager getHotbarManager() { return hotbarManager; }

    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }

    public EconomyManager getEconomyManager() { return economyManager; }

    public NPCManager getNpcManager() { return npcManager; }

    public NavigationManager getNavigationManager() { return navigationManager; }

    public QuestManager getQuestManager() { return questManager; }

    public StorageManager getStorageManager() { return storageManager; }

    public PartyManager getPartyManager() {return partyManager; }
}

