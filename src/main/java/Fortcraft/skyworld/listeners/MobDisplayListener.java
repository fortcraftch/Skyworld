package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.MobDisplayManager;
import Fortcraft.skyworld.mobdisplay.MobDisplay;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataType;

public class MobDisplayListener implements Listener {

    private final MobDisplayManager manager;

    public MobDisplayListener(MobDisplayManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob)) return;

        Bukkit.getScheduler().runTaskLater(
                Skyworld.getInstance(),
                () -> manager.updateDisplay(mob),
                1L
        );
    }

    @EventHandler
    public void onMobDespawn(EntitiesUnloadEvent e) {
        for (Entity entity : e.getEntities()) {

            if (entity instanceof LivingEntity living) {

                MobDisplay display = manager.getDisplay(living);

                if (display != null) {
                    manager.removeDisplay(living);
                }
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        manager.removeDisplay(event.getEntity());
    }

    @EventHandler
    public void onMobLoad(EntitiesLoadEvent e) {
        NamespacedKey key = new NamespacedKey(Skyworld.getInstance(), "has_mob_display");

        for (Entity entity : e.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living instanceof Player || living instanceof TextDisplay) continue;

            if (!living.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) continue;

            if (manager.getDisplay(living) != null) continue;

            manager.createDisplay(living);
        }
    }
}

