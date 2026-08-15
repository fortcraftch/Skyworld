package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.foraging.ForagingDrop;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.zones.ForagingZone;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ForagingManager implements Manager {

    private final List<ForagingZone> zones = new ArrayList<>();
    private final Map<Set<Block>, Long> pendingTrees = new ConcurrentHashMap<>();
    private final Map<Set<Block>, Material> treeMaterials = new ConcurrentHashMap<>();
    private final Map<Block, Set<Block>> blockToTreeMap = new ConcurrentHashMap<>();

    @Override
    public void load() { startTask(); }

    @Override
    public void unload() {
        pendingTrees.keySet().forEach(blocks -> {
            Material mat = treeMaterials.get(blocks);
            if (mat != null) {
                blocks.forEach(b -> b.setType(mat, false));
            }
        });
        zones.clear();
        pendingTrees.clear();
        treeMaterials.clear();
        blockToTreeMap.clear();
    }

    public boolean handleBreak(Player p, Block block, ForagingZone zone) {
        // 1. Obtenemos las estadísticas del caché del jugador
        double luck = StatManager.getStat(p, "foraging_luck");
        double fortune = StatManager.getStat(p, "foraging_fortune");
        double wisdom = StatManager.getStat(p, "wisdom");

        // 2. Obtenemos el drop aplicando la Suerte de Tala
        ForagingDrop drop = zone.getBiome().getWeightedDrop(block.getType(), luck);
        if (drop == null) return false;

        var template = ItemRegistry.getDropTemplates().get(drop.itemId());

        Set<Block> targetTree;

        // 3. Gestión de la estructura del árbol
        if (blockToTreeMap.containsKey(block)) {
            targetTree = blockToTreeMap.get(block);
        } else {
            targetTree = detectTree(block);
            for (Block b : targetTree) {
                blockToTreeMap.put(b, targetTree);
            }
        }

        // 4. Calculamos los drops aplicando la Fortuna de Tala
        int baseAmount = drop.getAmount();
        int finalAmount = StatManager.calculateFortuneDrops(baseAmount, fortune);
        drop.giveToStorage(p, finalAmount);

        // 5. Calculamos la experiencia aplicando la Sabiduría (Wisdom)
        if (template != null && template.stats() != null) {
            double baseExp = template.stats().getOrDefault("exp_given", 0.0);
            if (baseExp > 0) {
                double multiplier = 1.0 + (wisdom / 100.0);
                double finalExp = baseExp * multiplier;

                Skyworld.getInstance().getManagerHandler().getSkillManager().giveXp(p, "foraging", finalExp);
            }
        }

        block.setType(Material.AIR, false);
        scheduleTreeRegen(targetTree, drop);

        return true;
    }

    private void scheduleTreeRegen(Set<Block> blocks, ForagingDrop drop) {
        long newRegenTime = System.currentTimeMillis() + (drop.getRegenTime() * 1000L);
        pendingTrees.put(blocks, newRegenTime);
        treeMaterials.putIfAbsent(blocks, drop.getSourceMaterial());
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingTrees.isEmpty()) return;

                long now = System.currentTimeMillis();
                Iterator<Map.Entry<Set<Block>, Long>> it = pendingTrees.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry<Set<Block>, Long> entry = it.next();

                    if (now >= entry.getValue()) {
                        Set<Block> blocks = entry.getKey();
                        Material mat = treeMaterials.remove(blocks);

                        if (mat != null) {
                            for (Block b : blocks) {
                                b.setType(mat, false);
                                blockToTreeMap.remove(b);
                            }
                            playRegenEffect(blocks);
                        }
                        it.remove();
                    }
                }
            }
        }.runTaskTimer(Skyworld.getInstance(), 20L, 20L);
    }

    private void playRegenEffect(Set<Block> blocks) {
        if (blocks.isEmpty()) return;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (Block b : blocks) {
            minX = Math.min(minX, b.getX());
            maxX = Math.max(maxX, b.getX());
            minY = Math.min(minY, b.getY());
            maxY = Math.max(maxY, b.getY());
            minZ = Math.min(minZ, b.getZ());
            maxZ = Math.max(maxZ, b.getZ());
        }

        Location center = new Location(
                blocks.iterator().next().getWorld(),
                (minX + maxX) / 2.0 + 0.5,
                (minY + maxY) / 2.0 + 0.5,
                (minZ + maxZ) / 2.0 + 0.5
        );

        double offsetX = (maxX - minX) / 2.0 + 0.7;
        double offsetY = (maxY - minY) / 2.0 + 0.7;
        double offsetZ = (maxZ - minZ) / 2.0 + 0.7;

        org.bukkit.World world = center.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.HAPPY_VILLAGER, center, 40, offsetX, offsetY, offsetZ, 0.05);

            float randomPitch = 1.2f + (new Random().nextFloat() * 0.4f);
            world.playSound(center, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, randomPitch);
            world.playSound(center, org.bukkit.Sound.BLOCK_CHORUS_FLOWER_GROW, 0.5f, 0.8f);
        }
    }

    private Set<Block> detectTree(Block start) {
        Set<Block> tree = new HashSet<>();
        Stack<Block> stack = new Stack<>();
        stack.push(start);
        Material target = start.getType();

        while (!stack.isEmpty() && tree.size() < 150) {
            Block current = stack.pop();
            if (tree.contains(current)) continue;

            if (current.getType() == target || current.getType() == Material.AIR) {
                if (current.getType() == target) {
                    tree.add(current);
                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                if (x == 0 && y == 0 && z == 0) continue;
                                stack.push(current.getRelative(x, y, z));
                            }
                        }
                    }
                }
            }
        }
        return tree;
    }

    public void registerZone(ForagingZone zone) { zones.add(zone); }

    public ForagingZone getZoneAt(Location loc) {
        for (ForagingZone zone : zones) {
            if (zone.contains(loc.getBlock())) return zone;
        }
        return null;
    }
}