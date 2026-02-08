package Fortcraft.skyworld.zones;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.farming.FarmBiome;
import Fortcraft.skyworld.fishing.FishingBiome;
import Fortcraft.skyworld.foraging.ForagingBiome;
import Fortcraft.skyworld.managers.*;
import Fortcraft.skyworld.mining.MiningBiome;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.util.Objects;

public class ZoneLoader {

    private static FileConfiguration zonesConfig;

    public static void loadAll() {
        // 1. Inicializar el archivo zones.yml
        setupFile();

        ConfigurationSection zonesSection = zonesConfig.getConfigurationSection("zones");
        if (zonesSection == null) return;

        // 2. Obtener Managers
        ManagerHandler handler = Skyworld.getInstance().getManagerHandler();
        ZoneManager zoneManager = handler.getZoneManager();
        MiningManager miningManager = handler.getMiningManager();
        FishingManager fishingManager = handler.getFishingManager();
        FarmManager farmingManager = handler.getFarmManager();
        ForagingManager foragingManager = handler.getForagingManager();
        RegionManager regionManager = handler.getRegionManager();

        // 3. Iterar y cargar
        loadBiomas(zoneManager);

        for (String id : zonesSection.getKeys(false)) {
            ConfigurationSection z = zonesSection.getConfigurationSection(id);
            if (z == null) continue;

            World world = Bukkit.getWorld(Objects.requireNonNull(z.getString("world"), "world"));
            ConfigurationSection area = z.getConfigurationSection("area");
            if (world == null || area == null) continue;

            BoundingBox box = new BoundingBox(
                    area.getDouble("pos1.x"), area.getDouble("pos1.y"), area.getDouble("pos1.z"),
                    area.getDouble("pos2.x"), area.getDouble("pos2.y"), area.getDouble("pos2.z")
            );

            String type = Objects.requireNonNull(z.getString("type")).toUpperCase();

            switch (type) {
                case "MOB" -> {
                    MobZone mobZone = MobZone.fromConfig(id, world, box, z);
                    zoneManager.registerZone(mobZone);
                }
                case "FISHING" -> {
                    // Pasamos la sección completa por si FishingZone necesita el 'world' o 'type'
                    FishingZone fishingZone = new FishingZone(id, world, box, z.getConfigurationSection("fishing"));
                    zoneManager.registerZone(fishingZone);
                    fishingManager.registerZone(fishingZone);
                }
                case "MINING" -> {
                    MiningZone miningZone = new MiningZone(id, world, box, z.getConfigurationSection("mining"));
                    zoneManager.registerZone(miningZone);
                    miningManager.registerZone(miningZone);
                }
                case "FARMING" -> {
                    FarmZone farmingZone = new FarmZone(id, world, box, z.getConfigurationSection("farming"));
                    zoneManager.registerZone(farmingZone);
                    farmingManager.registerZone(farmingZone);
                }
                case "FORAGING" -> {
                    ForagingZone foragingZone = new ForagingZone(id, world, box, z.getConfigurationSection("foraging"));
                    zoneManager.registerZone(foragingZone);
                    foragingManager.registerZone(foragingZone);
                }
                case "REGION" -> {
                    RegionZone regionZone = RegionZone.fromConfig(id, world, box, z.getConfigurationSection("region"));
                    zoneManager.registerZone(regionZone);
                    regionManager.registerZone(regionZone);
                }
            }
        }
    }

    private static void loadBiomas(ZoneManager zoneManager) {
        ConfigurationSection biomesSec = zonesConfig.getConfigurationSection("fishing_biomes");
        if (biomesSec == null) return;

        for (String key : biomesSec.getKeys(false)) {
            // Creamos el objeto Bioma y lo registramos en el Manager
            FishingBiome biome = new FishingBiome(key, biomesSec.getConfigurationSection(key));
            zoneManager.addFishingBiome(key, biome);
        }

        ConfigurationSection miningSec = zonesConfig.getConfigurationSection("mining_biomes");
        if (miningSec != null) {
            for (String key : miningSec.getKeys(false)) {
                MiningBiome biome = new MiningBiome(key, miningSec.getConfigurationSection(key));
                zoneManager.addMiningBiome(key, biome);
            }
        }

        ConfigurationSection farmSec = zonesConfig.getConfigurationSection("farming_biomes");
        if (farmSec != null) {
            for (String key : farmSec.getKeys(false)) {
                FarmBiome biome = new FarmBiome(key, farmSec.getConfigurationSection(key));
                zoneManager.addFarmingBiome(key, biome);
            }
        }

        ConfigurationSection foragingSec = zonesConfig.getConfigurationSection("foraging_biomes");
        if (foragingSec != null) {
            for (String key : foragingSec.getKeys(false)) {
                ForagingBiome biome = new ForagingBiome(key, foragingSec.getConfigurationSection(key));
                zoneManager.addForagingBiome(key, biome);
            }
        }
    }

    public static void setupFile() {
        File file = new File(Skyworld.getInstance().getDataFolder(), "zones.yml");
        if (!file.exists()) {
            Skyworld.getInstance().saveResource("zones.yml", false);
        }
        zonesConfig = YamlConfiguration.loadConfiguration(file);
    }
}