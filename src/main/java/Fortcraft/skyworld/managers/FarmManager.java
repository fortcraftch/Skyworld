package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.farming.FarmDrop;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.listeners.FarmPhysicsListener;
import Fortcraft.skyworld.zones.FarmZone;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
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
    private final Map<Location, BlockData> savedBlockData = new ConcurrentHashMap<>();
    private final List<FarmZone> zones = new ArrayList<>();
    private BukkitTask regenTask;

    @Override
    public void load() {
        startTask();
        Bukkit.getPluginManager().registerEvents(new FarmPhysicsListener(this), Skyworld.getInstance());
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
        savedBlockData.clear();
        zones.clear();
    }

    public boolean handleHarvest(Player p, Block block, FarmZone zone) {
        // 1. Obtenemos las stats de la caché ultra-rápida UNA SOLA VEZ
        double wisdom = StatManager.getStat(p, "wisdom");
        double luck = StatManager.getStat(p, "farming_luck");
        double fortune = StatManager.getStat(p, "farming_fortune");

        // 2. Tiramos la probabilidad del drop base con la Suerte aplicada
        FarmDrop initialDrop = zone.getBiome().getWeightedDrop(block.getType(), luck);
        if (initialDrop == null) return false;

        // 3. Procesamos (pasando las stats ya calculadas en memoria)
        if (isChainableMaterial(block.getType())) {
            harvestChain(p, block, zone, luck, fortune, wisdom);
        } else {
            processSingleHarvest(p, block, initialDrop, fortune, wisdom);
        }

        return true;
    }

    private void harvestChain(Player p, Block startBlock, FarmZone zone, double luck, double fortune, double wisdom) {
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new LinkedHashSet<>();

        queue.add(startBlock);
        visited.add(startBlock);

        while (!queue.isEmpty()) {
            Block current = queue.poll();
            FarmDrop drop = zone.getBiome().getWeightedDrop(current.getType(), luck);

            if (drop != null) {
                processSingleHarvest(p, current, drop, fortune, wisdom);
            }

            for (BlockFace face : CHAIN_FACES) {
                Block neighbor = current.getRelative(face);

                if (!visited.contains(neighbor) && zone.contains(neighbor)) {
                    if (isChainableMaterial(neighbor.getType()) && zone.getBiome().hasDrops(neighbor.getType())) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    private void processSingleHarvest(Player p, Block block, FarmDrop drop, double fortune, double wisdom) {
        // --- CÁLCULO DE DROPS (FORTUNA) ---
        int baseAmount = drop.getAmount();
        int finalAmount = StatManager.calculateFortuneDrops(baseAmount, fortune);
        drop.giveToStorage(p, finalAmount);

        // --- CÁLCULO DE EXPERIENCIA (SABIDURÍA) ---
        var template = ItemRegistry.getDropTemplates().get(drop.itemId());
        if (template != null && template.stats() != null) {
            double baseExp = template.stats().getOrDefault("exp_given", 0.0);

            if (baseExp > 0) {
                double multiplier = 1.0 + (wisdom / 100.0);
                double finalExp = baseExp * multiplier;

                Skyworld.getInstance().getManagerHandler().getSkillManager().giveXp(p, "farming", finalExp);
            }
        }

        this.scheduleRegen(block, drop);
    }

    public boolean isChainableMaterial(Material material) {
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
        savedBlockData.put(loc, block.getBlockData().clone());

        block.setType(Material.AIR, false);
    }

    private void startTask() {
        regenTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRegen.isEmpty()) return;

                long now = System.currentTimeMillis();
                List<Location> readyLocations = new ArrayList<>();

                for (Map.Entry<Location, Long> entry : pendingRegen.entrySet()) {
                    if (now >= entry.getValue()) {
                        readyLocations.add(entry.getKey());
                    }
                }

                if (readyLocations.isEmpty()) return;

                readyLocations.sort(Comparator.comparingInt(Location::getBlockY));

                for (Location loc : readyLocations) {
                    if (!hasSupport(loc)) {
                        continue;
                    }

                    regenerate(loc);
                    pendingRegen.remove(loc);
                }
            }
        }.runTaskTimer(Skyworld.getInstance(), 20L, 20L);
    }

    private boolean hasSupport(Location loc) {
        for (BlockFace face : SUPPORT_FACES) {
            Location neighborLoc = loc.clone().add(face.getModX(), face.getModY(), face.getModZ());

            if (pendingRegen.containsKey(neighborLoc)) {
                continue;
            }

            if (!neighborLoc.getBlock().getType().isAir()) {
                return true;
            }
        }
        return false;
    }

    private void regenerate(Location loc) {
        FarmDrop drop = context.remove(loc);
        BlockData originalData = savedBlockData.remove(loc);
        if (drop == null) return;

        Block block = loc.getBlock();

        if (originalData != null) {
            if (originalData instanceof Ageable ageable) {
                ageable.setAge(ageable.getMaximumAge());
            }
            block.setBlockData(originalData, false);
        } else {
            block.setType(drop.getSourceBlock(), false);
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