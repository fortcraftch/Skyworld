package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.farming.FarmDrop;
import Fortcraft.skyworld.items.ItemRegistry;
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

        // 1. Resolver los metadatos globales del item a partir de su ID único
        var template = ItemRegistry.getDropTemplates().get(drop.itemId());

        // 2. Dar el ítem al almacenamiento usando su identificador global unificado
        // (Ajusta 'giveToStorage' si requiere el itemId String o el ItemStack generado por la factoría)
        drop.giveToStorage(p);

        // 3. Dar la experiencia registrada estáticamente en el drops.yml centralizado
        if (template != null && template.customStats() != null) {
            double expGiven = template.customStats().getOrDefault("exp_given", 0.0);
            if (expGiven > 0) {
                p.giveExp((int) expGiven);
            }
        }

        // 4. Programar la regeneración del cultivo basado en los tiempos del bloque origen
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

        // Si el bloque implementa la interfaz Ageable (como el Trigo, Zanahorias, etc.), lo forzamos a su etapa final madura
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(ageable.getMaximumAge());
            block.setBlockData(ageable);
        }

        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 0.3, 0.5), 5, 0.2, 0.2, 0.2, 0.02);
        loc.getWorld().playSound(loc.clone(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
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