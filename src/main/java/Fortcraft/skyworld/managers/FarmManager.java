package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.farming.FarmDrop;
import Fortcraft.skyworld.zones.FarmZone;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FarmManager implements Manager {

    private final Map<Location, Long> pendingRegen = new ConcurrentHashMap<>();
    private final Map<Location, FarmDrop> context = new ConcurrentHashMap<>();
    private final List<FarmZone> zones = new ArrayList<>();
    private BukkitTask regenTask;

    @Override
    public void load() {
        startTask();
    }

    @Override
    public void unload() {
        if (regenTask != null && !regenTask.isCancelled()) {
            regenTask.cancel();
        }

        for (Location loc : pendingRegen.keySet()) {
            regenerate(loc);
        }

        pendingRegen.clear();
        context.clear();
        zones.clear();
    }

    public boolean handleHarvest(Player p, Block block, FarmZone zone) {
        FarmDrop drop = zone.getBiome().getWeightedDrop(block.getType());

        if (drop == null) return false;

        drop.giveToStorage(p);
        if (drop.getExp() > 0) {
            p.giveExp(drop.getExp());
        }
        this.scheduleRegen(block, drop);

        return true;
    }

    private void scheduleRegen(Block block, FarmDrop drop) {
        Location loc = block.getLocation();
        long respawnTime = System.currentTimeMillis() + (drop.getRegenTime() * 1000L);

        pendingRegen.put(loc, respawnTime);
        context.put(loc, drop);

        block.setType(Material.AIR, false);
    }

    private void startTask() {
        regenTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRegen.isEmpty()) return;

                long now = System.currentTimeMillis();
                Iterator<Map.Entry<Location, Long>> it = pendingRegen.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry<Location, Long> entry = it.next();
                    if (now >= entry.getValue()) {
                        regenerate(entry.getKey());
                        it.remove();
                    }
                }
            }
        }.runTaskTimer(Skyworld.getInstance(), 20L, 20L);
    }

    private void regenerate(Location loc) {
        FarmDrop drop = context.remove(loc);
        if (drop == null) return;

        Block block = loc.getBlock();
        block.setType(drop.getSourceBlock(), false);

        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(ageable.getMaximumAge());
            block.setBlockData(ageable);
        }

        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 0.3, 0.5), 5, 0.2, 0.2, 0.2, 0.02);
        loc.getWorld().playSound(loc.clone(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
    }

    public void registerZone(FarmZone zone) {
        zones.add(zone);
    }

    public FarmZone getZoneAt(Location loc) {
        for (FarmZone zone : zones) {
            if (zone.contains(loc.getBlock())) return zone;
        }
        return null;
    }
}