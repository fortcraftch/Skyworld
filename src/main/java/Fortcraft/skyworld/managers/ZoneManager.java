package Fortcraft.skyworld.zones;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.Manager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.Map;

public class ZoneManager implements Manager {

    private final Map<String, Zone> zones = new HashMap<>();

    @Override
    public void load() {
        loadZones();
        startSpawnerTask();
    }

    @Override
    public void unload() {
        zones.clear();
    }

    private void loadZones() {
        ConfigurationSection section = Skyworld.getInstance()
                .getConfig()
                .getConfigurationSection("zones");

        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection z = section.getConfigurationSection(id);

            World world = Bukkit.getWorld(z.getString("world"));
            int maxMobs = z.getInt("max-mobs");

            BoundingBox box = new BoundingBox(
                    z.getDouble("pos1.x"),
                    z.getDouble("pos1.y"),
                    z.getDouble("pos1.z"),
                    z.getDouble("pos2.x"),
                    z.getDouble("pos2.y"),
                    z.getDouble("pos2.z")
            );

            zones.put(id, new Zone(id, world, box, maxMobs));
        }
    }

    private void startSpawnerTask() {
        Bukkit.getScheduler().runTaskTimer(
                Skyworld.getInstance(),
                new ZoneSpawnerTask(this),
                20L,
                20L * 5
        );
    }

    public Map<String, Zone> getZones() {
        return zones;
    }
}
