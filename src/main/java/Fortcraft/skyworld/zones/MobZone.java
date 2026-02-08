package Fortcraft.skyworld.zones;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.MobDisplayManager;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;

import java.util.Map;

public class ZoneMob extends Zone {

    private final int maxMobs;
    private final Map<EntityType, Integer> spawnMap;

    public ZoneMob(
            String id,
            World world,
            BoundingBox box,
            int maxMobs,
            Map<EntityType, Integer> spawnMap
    ) {
        super(id, world, box);
        this.maxMobs = maxMobs;
        this.spawnMap = spawnMap;
    }

    @Override
    public void tick() {
        MobDisplayManager displayManager =
                Skyworld.getInstance().getManagerHandler().getMobDisplayManager();

        long totalMobs = world.getLivingEntities().stream()
                .filter(e -> box.contains(e.getLocation().toVector()))
                .count();

        if (totalMobs >= maxMobs) return;

        for (Map.Entry<EntityType, Integer> entry : spawnMap.entrySet()) {
            EntityType type = entry.getKey();
            int max = entry.getValue();

            long count = world.getLivingEntities().stream()
                    .filter(e -> e.getType() == type)
                    .filter(e -> box.contains(e.getLocation().toVector()))
                    .count();

            int toSpawn = Math.min(max - (int) count, maxMobs - (int) totalMobs);
            for (int i = 0; i < toSpawn; i++) {
                LivingEntity mob = (LivingEntity)
                        world.spawnEntity(getRandomLocation(), type);

                displayManager.createDisplay(mob);
            }
        }
    }
}
