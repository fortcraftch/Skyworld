package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.mobdisplay.MobDisplay;
import org.bukkit.entity.LivingEntity;
import Fortcraft.skyworld.Skyworld;

import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MobDisplayManager implements Manager {

    private final Map<UUID, MobDisplay> displays = new HashMap<>();

    private int taskId = -1;

    @Override
    public void load() {

        Bukkit.getWorlds().forEach(world ->
                world.getLivingEntities().forEach(this::createDisplay)
        );

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                Skyworld.getInstance(),
                () -> displays.values().forEach(display -> {
                    if (display.getMob().isDead()) {
                        display.remove();
                        return;
                    }
                    display.teleportSmooth();
                }),
                1L,
                1L
        );
    }

    @Override
    public void unload() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        displays.values().forEach(MobDisplay::remove);
        displays.clear();
    }


    public void createDisplay(LivingEntity mob) {
        if (displays.containsKey(mob.getUniqueId())) return;

        MobDisplay display = new MobDisplay(mob);
        displays.put(mob.getUniqueId(), display);
    }

    public void updateDisplay(LivingEntity mob) {
        MobDisplay display = displays.get(mob.getUniqueId());
        if (display != null) {
            display.update();
        }
    }

    public void teleportDisplay(LivingEntity mob) {
        MobDisplay display = displays.get(mob.getUniqueId());
        if (display != null) {
            display.teleportSmooth();
        }
    }

    public void removeDisplay(LivingEntity mob) {
        MobDisplay display = displays.remove(mob.getUniqueId());
        if (display != null) {
            display.remove();
        }
    }
}

