package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.farming.FarmBiome;
import Fortcraft.skyworld.fishing.FishingBiome;
import Fortcraft.skyworld.fishing.FishingDrop;
import Fortcraft.skyworld.foraging.ForagingBiome;
import Fortcraft.skyworld.mining.MiningBiome;
import Fortcraft.skyworld.zones.Zone;
import Fortcraft.skyworld.zones.ZoneLoader;
import org.bukkit.Bukkit;

import java.util.*;

public class ZoneManager implements Manager {

    private final List<Zone> zones = new ArrayList<>();
    private final Map<String, FishingBiome> fishingBiomes = new HashMap<>();
    private final Map<String, MiningBiome> miningBiomes = new HashMap<>();
    private final Map<String, FarmBiome> farmBiomes = new HashMap<>();
    private final Map<String, ForagingBiome> foragingBiomes = new HashMap<>();
    private int taskId = -1;

    @Override
    public void load() {
        zones.clear();

        ZoneLoader.loadAll();
        startTicker();
    }

    @Override
    public void unload() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        // Si tienes otros métodos de limpieza en MobZone o MiningZone, llámalos aquí
        zones.clear();
    }

    public void registerZone(Zone zone) {
        zones.add(zone);
    }

    public List<Zone> getZones() {
        return zones;
    }

    public void addFishingBiome(String id, FishingBiome biome) {
        fishingBiomes.put(id, biome);
    }

    public FishingBiome getFishingBiome(String id) {
        return fishingBiomes.get(id);
    }

    public Collection<FishingBiome> getAllFishingBiomes() {
        return fishingBiomes.values();
    }

    public List<FishingDrop> getAllFishingDrops() {
        Map<String, FishingDrop> uniqueDrops = new HashMap<>();

        getAllFishingBiomes().forEach(biome -> {
            for (FishingDrop drop : biome.getDrops()) {
                uniqueDrops.putIfAbsent(drop.getName(), drop);
            }
        });
        return new ArrayList<>(uniqueDrops.values());
    }

    public void addMiningBiome(String id, MiningBiome biome) {
        miningBiomes.put(id, biome);
    }

    public MiningBiome getMiningBiome(String id) {
        return miningBiomes.get(id);
    }

    public Collection<MiningBiome> getAllMiningBiomes() {
        return miningBiomes.values();
    }

    public void addFarmingBiome(String id, FarmBiome biome) {
        farmBiomes.put(id, biome);
    }

    public FarmBiome getFarmingBiome(String id) {
        return farmBiomes.get(id);
    }

    public Collection<FarmBiome> getAllFarmingBiomes() {
        return farmBiomes.values();
    }

    public void addForagingBiome(String id, ForagingBiome biome) {
        foragingBiomes.put(id, biome);
    }

    public ForagingBiome getForagingBiome(String id) {
        return foragingBiomes.get(id);
    }

    public Collection<ForagingBiome> getAllForagingBiomes() {
        return foragingBiomes.values();
    }

    private void startTicker() {
        taskId = Bukkit.getScheduler().runTaskTimer(
                Skyworld.getInstance(),
                () -> zones.forEach(Zone::tick),
                20L,
                20L * 5
        ).getTaskId();
    }
}
