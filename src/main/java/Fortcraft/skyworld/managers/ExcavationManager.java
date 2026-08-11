package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.excavation.ExcavationDrop;
import Fortcraft.skyworld.excavation.ExcavationNode;
import Fortcraft.skyworld.listeners.ExcavationListener;
import Fortcraft.skyworld.zones.ExcavationZone;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExcavationManager implements Manager {

    private final Map<Location, ExcavationNode> activeNodes = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> activeBrushingSessions = new ConcurrentHashMap<>();
    private final List<ExcavationZone> zones = new ArrayList<>();

    private final int MAX_PROGRESS = 100;
    private final double GLOW_DISTANCE = 15.0;
    private final long RESPAWN_DELAY_TICKS = 1200L; // 1 minuto

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(new ExcavationListener(this), Skyworld.getInstance());
        startDistanceChecker();
    }

    @Override
    public void unload() {
        // Cancelar todas las sesiones de cepillado activas al recargar/apagar
        for (BukkitTask task : activeBrushingSessions.values()) {
            task.cancel();
        }
        activeBrushingSessions.clear();

        for (ExcavationNode node : activeNodes.values()) {
            node.cleanup();
        }
        activeNodes.clear();
        zones.clear();
    }

    public void registerZone(ExcavationZone zone) {
        zones.add(zone);
        fillZoneNodes(zone);
    }

    private void fillZoneNodes(ExcavationZone zone) {
        long currentNodesInZone = activeNodes.values().stream()
                .filter(node -> zone.contains(node.getLocation()))
                .count();

        for (int i = 0; i < (zone.getMaxConcurrentNodes() - currentNodesInZone); i++) {
            spawnNewNodeInZone(zone);
        }
    }

    public ExcavationNode getNode(Location loc) {
        return activeNodes.get(loc);
    }

    public int getMaxProgress() {
        return MAX_PROGRESS;
    }

    /**
     * Inicia una tarea repetitiva mientras el jugador mantenga presionado el clic derecho con el pincel.
     */
    public void startBrushingSession(Player player, ExcavationNode node, Block block) {
        UUID uuid = player.getUniqueId();
        if (activeBrushingSessions.containsKey(uuid)) return; // Ya tiene una sesión activa

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // 1. Validar que el jugador siga conectado y tenga el pincel en la mano principal
                if (!player.isOnline() || player.getInventory().getItemInMainHand().getType() != Material.BRUSH) {
                    stopBrushingSession(uuid);
                    return;
                }

                // 2. Verificar que mantenga el botón derecho presionado (mano levantada en Minecraft)
                if (!player.isHandRaised()) {
                    stopBrushingSession(uuid);
                    return;
                }

                // 3. Verificar que el jugador siga mirando exactamente al mismo nodo de excavación (distancia máxima de 5 bloques)
                Block targetBlock = player.getTargetBlockExact(5);
                if (targetBlock == null || !targetBlock.getLocation().equals(node.getLocation())) {
                    stopBrushingSession(uuid);
                    return;
                }

                Location loc = node.getLocation();
                if (!activeNodes.containsKey(loc)) {
                    stopBrushingSession(uuid);
                    return;
                }

                // 4. Preparación visual en el primer tick de la sesión si no se ha inicializado
                if (node.getItemDisplay() == null) {
                    BlockFace closestExposedFace = getClosestExposedFaceToPlayer(block, player);
                    ExcavationZone zone = getZoneAt(loc);
                    if (zone != null) {
                        ExcavationDrop drop = zone.getBiome().getWeightedDrop(block.getType());
                        if (drop != null) {
                            node.setPendingDrop(drop);
                            ItemStack previewItem = drop.getItemStack();
                            node.initRewardVisual(closestExposedFace, previewItem);
                        }
                    }
                }

                // 5. Efecto sonoro y partículas del bloque
                loc.getWorld().playSound(loc, Sound.ITEM_BRUSH_BRUSHING_SAND, 1.0f, 1.0f);
                loc.getWorld().spawnParticle(
                        Particle.BLOCK,
                        loc.clone().add(0.5, 0.5, 0.5),
                        10, 0.2, 0.2, 0.2,
                        block.getBlockData()
                );

                // 6. Incrementar progreso gradualmente (Ej: +10 cada 5 ticks = ~0.25s por incremento)
                node.addProgress(10);

                if (node.getProgress() >= MAX_PROGRESS) {
                    finishExcavation(player, node);
                    stopBrushingSession(uuid);
                }
            }
        }.runTaskTimer(Skyworld.getInstance(), 0L, 5L); // Ejecutar inmediatamente y repetir cada 5 ticks

        activeBrushingSessions.put(uuid, task);
    }

    public void stopBrushingSession(UUID uuid) {
        BukkitTask task = activeBrushingSessions.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void finishExcavation(Player p, ExcavationNode node) {
        Location loc = node.getLocation();
        ExcavationZone zone = getZoneAt(loc);

        // 1. Otorgar el botín
        ExcavationDrop drop = node.getPendingDrop();
        if (drop != null) {
            drop.giveToStorage(p);

            if (drop.getExpGiven() > 0) {
                Skyworld.getInstance().getManagerHandler().getSkillManager().giveXp(p, "excavation", drop.getExpGiven());
            }
        }

        // 2. Limpieza visual
        node.cleanup();
        activeNodes.remove(loc);

        // 3. Sonido final y restablecer el bloque al tipo de material original
        loc.getWorld().playSound(loc, Sound.ITEM_BRUSH_BRUSHING_SAND_COMPLETE, 1.0f, 1.0f);
        loc.getBlock().setType(node.getOriginalMaterial());

        // 4. Programar reaparición
        if (zone != null) {
            Bukkit.getScheduler().runTaskLater(Skyworld.getInstance(), () -> {
                spawnNewNodeInZone(zone);
            }, RESPAWN_DELAY_TICKS);
        }
    }

    private void spawnNewNodeInZone(ExcavationZone zone) {
        Block validBlock = zone.getRandomValidSurfaceBlock();
        if (validBlock != null && !activeNodes.containsKey(validBlock.getLocation())) {
            createNode(validBlock);
        } else {
            Bukkit.getScheduler().runTaskLater(Skyworld.getInstance(), () -> spawnNewNodeInZone(zone), 100L);
        }
    }

    public void createNode(Block block) {
        Location loc = block.getLocation();
        activeNodes.put(loc, new ExcavationNode(loc, block.getType()));
    }

    public BlockFace getClosestExposedFaceToPlayer(Block block, Player player) {
        Vector blockCenter = block.getLocation().add(0.5, 0.5, 0.5).toVector();
        Vector playerEyes = player.getEyeLocation().toVector();
        Vector directionToPlayer = playerEyes.subtract(blockCenter).normalize();

        BlockFace bestFace = BlockFace.UP;
        double maxDot = -Double.MAX_VALUE;

        BlockFace[] faces = {
                BlockFace.UP, BlockFace.DOWN,
                BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST
        };

        for (BlockFace face : faces) {
            if (block.getRelative(face).getType().isAir()) {
                double dot = directionToPlayer.dot(face.getDirection());
                if (dot > maxDot) {
                    maxDot = dot;
                    bestFace = face;
                }
            }
        }
        return bestFace;
    }

    private void startDistanceChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ExcavationNode node : activeNodes.values()) {
                    Location blockLoc = node.getLocation();

                    boolean playerNearby = false;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getWorld().equals(blockLoc.getWorld())
                                && p.getLocation().distanceSquared(blockLoc) <= (GLOW_DISTANCE * GLOW_DISTANCE)) {
                            playerNearby = true;
                            break;
                        }
                    }

                    if (playerNearby) {
                        Block block = blockLoc.getBlock();
                        BlockFace[] faces = {
                                BlockFace.UP, BlockFace.DOWN,
                                BlockFace.NORTH, BlockFace.SOUTH,
                                BlockFace.EAST, BlockFace.WEST
                        };

                        // Iterar por cada cara para verificar si está expuesta al aire
                        for (BlockFace face : faces) {
                            if (block.getRelative(face).getType().isAir()) {
                                // Calcular el centro exacto de la cara expuesta
                                Location faceLoc = blockLoc.clone().add(
                                        0.5 + (face.getModX() * 0.51),
                                        0.5 + (face.getModY() * 0.51),
                                        0.5 + (face.getModZ() * 0.51)
                                );

                                // Ajustar la dispersión de las partículas según la orientación de la cara
                                double offsetX = (face.getModX() == 0) ? 0.3 : 0.05;
                                double offsetY = (face.getModY() == 0) ? 0.3 : 0.05;
                                double offsetZ = (face.getModZ() == 0) ? 0.3 : 0.05;

                                blockLoc.getWorld().spawnParticle(
                                        Particle.WAX_ON,
                                        faceLoc,
                                        3,
                                        offsetX, offsetY, offsetZ,
                                        0.0
                                );
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(Skyworld.getInstance(), 20L, 40L);
    }

    public ExcavationZone getZoneAt(Location loc) {
        for (ExcavationZone zone : zones) {
            if (zone.contains(loc)) return zone;
        }
        return null;
    }
}