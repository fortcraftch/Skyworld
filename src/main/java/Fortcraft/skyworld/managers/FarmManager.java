package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.farming.FarmDrop;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.zones.FarmZone;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FarmManager implements Manager {

    private static final BlockFace[] CHAIN_FACES = {
            BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private static final BlockFace[] SUPPORT_FACES = {
            BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

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
        FarmDrop initialDrop = zone.getBiome().getWeightedDrop(block.getType());
        if (initialDrop == null) return false;

        if (isChainableMaterial(block.getType())) {
            harvestChain(p, block, zone);
        } else {
            processSingleHarvest(p, block, initialDrop);
        }

        return true;
    }

    private void harvestChain(Player p, Block startBlock, FarmZone zone) {
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new LinkedHashSet<>();

        queue.add(startBlock);
        visited.add(startBlock);

        while (!queue.isEmpty()) {
            Block current = queue.poll();
            FarmDrop drop = zone.getBiome().getWeightedDrop(current.getType());

            if (drop != null) {
                processSingleHarvest(p, current, drop);
            }

            for (BlockFace face : CHAIN_FACES) {
                Block neighbor = current.getRelative(face);

                if (!visited.contains(neighbor) && zone.contains(neighbor)) {
                    if (isChainableMaterial(neighbor.getType()) && zone.getBiome().getWeightedDrop(neighbor.getType()) != null) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    private void processSingleHarvest(Player p, Block block, FarmDrop drop) {
        drop.giveToStorage(p);

        var template = ItemRegistry.getDropTemplates().get(drop.itemId());
        if (template != null && template.customStats() != null) {
            double expGiven = template.customStats().getOrDefault("exp_given", 0.0);
            if (expGiven > 0) {
                Skyworld.getInstance().getManagerHandler().getSkillManager().giveXp(p, "farming", expGiven);
            }
        }

        this.scheduleRegen(block, drop);
    }

    private boolean isChainableMaterial(Material material) {
        return material == Material.CACTUS
                || material == Material.MOSS_BLOCK
                || material == Material.CACTUS_FLOWER
                || material == Material.BIG_DRIPLEAF;
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

                // 1. Obtener ubicaciones listas para regenerar
                List<Location> readyLocations = new ArrayList<>();
                for (Map.Entry<Location, Long> entry : pendingRegen.entrySet()) {
                    if (now >= entry.getValue()) {
                        readyLocations.add(entry.getKey());
                    }
                }

                if (readyLocations.isEmpty()) return;

                // 2. Ordenar por altura Y de menor a mayor (Base -> Tallo -> Copa)
                readyLocations.sort(Comparator.comparingInt(Location::getBlockY));

                // 3. Procesar regeneración garantizando soporte inferior y lateral
                for (Location loc : readyLocations) {
                    // Si no tiene ningún bloque sólido ni restaurado que le sirva de soporte (abajo o lados), se aplaza
                    if (!hasSupport(loc)) {
                        continue;
                    }

                    regenerate(loc);
                    pendingRegen.remove(loc);
                }
            }
        }.runTaskTimer(Skyworld.getInstance(), 20L, 20L);
    }

    /**
     * Comprueba si la ubicación tiene al menos un bloque de soporte válido (debajo o en los lados)
     * que ya esté presente en el mundo y no pendiente de regenerar.
     */
    private boolean hasSupport(Location loc) {
        for (BlockFace face : SUPPORT_FACES) {
            Location neighborLoc = loc.clone().add(face.getModX(), face.getModY(), face.getModZ());

            // Si el vecino está pendiente de regenerar, no sirve como soporte aún
            if (pendingRegen.containsKey(neighborLoc)) {
                continue;
            }

            // Si el vecino en el mundo no es AIRE, sirve como soporte
            if (!neighborLoc.getBlock().getType().isAir()) {
                return true;
            }
        }
        return false;
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