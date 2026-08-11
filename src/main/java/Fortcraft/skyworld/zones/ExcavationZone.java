package Fortcraft.skyworld.zones;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.excavation.ExcavationBiome;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.BoundingBox;

import java.util.Random;

public class ExcavationZone extends Zone {

    private final ExcavationBiome biome;
    private final int maxConcurrentNodes;

    // Coordenadas límite en enteros para optimizar búsquedas aleatorias
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    private final Random random = new Random();

    public ExcavationZone(String id, World world, BoundingBox box, ConfigurationSection config) {
        super(id, world, box);

        this.minX = (int) box.getMinX();
        this.minY = (int) box.getMinY();
        this.minZ = (int) box.getMinZ();
        this.maxX = (int) box.getMaxX();
        this.maxY = (int) box.getMaxY();
        this.maxZ = (int) box.getMaxZ();

        if (config != null) {
            String biomeId = config.getString("biome", "");
            this.biome = Skyworld.getInstance().getManagerHandler().getZoneManager().getExcavationBiome(biomeId);
            this.maxConcurrentNodes = config.getInt("max_concurrent_nodes", 5);
        } else {
            this.biome = null;
            this.maxConcurrentNodes = 1;
        }
    }

    public String getId() {
        return id;
    }

    public ExcavationBiome getBiome() {
        return biome;
    }

    public int getMaxConcurrentNodes() {
        return maxConcurrentNodes;
    }

    @Override
    public void tick() {
        // Reservado para lógica periódica si es requerida
    }

    /**
     * Busca un bloque válido al azar en las 3 dimensiones dentro de la zona.
     * Requisito: Debe ser un material permitido por el bioma y tener al menos una cara al aire libre.
     */
    public Block getRandomValidSurfaceBlock() {
        if (biome == null) return null;

        int attempts = 0;
        while (attempts < 50) {
            int x = random.nextInt((maxX - minX) + 1) + minX;
            int y = random.nextInt((maxY - minY) + 1) + minY;
            int z = random.nextInt((maxZ - minZ) + 1) + minZ;

            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();

            if (biome.isAllowedMaterial(type) && isExposedToAir(block)) {
                return block;
            }
            attempts++;
        }
        return null;
    }

    /**
     * Comprueba si el bloque tiene al menos una cara adyacente que sea aire.
     */
    private boolean isExposedToAir(Block block) {
        BlockFace[] faces = {
                BlockFace.UP, BlockFace.DOWN,
                BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST
        };

        for (BlockFace face : faces) {
            if (block.getRelative(face).getType().isAir()) {
                return true;
            }
        }
        return false;
    }
}