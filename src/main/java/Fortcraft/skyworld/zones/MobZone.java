package Fortcraft.skyworld.zones;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.MobDisplayManager;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.util.EnumMap;
import java.util.Map;

public class MobZone extends Zone {

    private final int maxMobs;
    private final Map<EntityType, Integer> spawnMap;

    public MobZone(
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

    public static MobZone fromConfig(
            String id,
            World world,
            BoundingBox box,
            ConfigurationSection z
    ) {
        ConfigurationSection mob = z.getConfigurationSection("mob");

        int maxMobs = mob.getInt("max-mobs");

        Map<EntityType, Integer> spawnMap = new EnumMap<>(EntityType.class);
        ConfigurationSection spawns = mob.getConfigurationSection("spawns");

        for (String key : spawns.getKeys(false)) {
            spawnMap.put(
                    EntityType.valueOf(key.toUpperCase()),
                    spawns.getInt(key)
            );
        }

        return new MobZone(id, world, box, maxMobs, spawnMap);
    }

    public void markForDisplay(LivingEntity mob) {
        NamespacedKey key = new NamespacedKey(Skyworld.getInstance(), "has_mob_display");
        mob.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    @Override
    public void tick() {
        MobDisplayManager displayManager =
                Skyworld.getInstance().getManagerHandler().getMobDisplayManager();

        long totalMobs = world.getLivingEntities().stream()
                .filter(e -> box.contains(e.getLocation().toVector()))
                .count();

        if (totalMobs > 1) return;

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

                markForDisplay(mob);
                displayManager.createDisplay(mob);
            }
        }
    }
}
