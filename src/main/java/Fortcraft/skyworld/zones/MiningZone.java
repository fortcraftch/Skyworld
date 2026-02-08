package Fortcraft.skyworld.zones;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.mining.MiningBiome;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.BoundingBox;

public class MiningZone extends Zone {

    private MiningBiome biome;

    public MiningZone(String id, World world, BoundingBox box, ConfigurationSection config) {
        super(id, world, box);
        load(config);
    }

    private void load(ConfigurationSection config) {
        if (config == null) return;
        String biomeId = config.getString("biome");

        if (biomeId != null) {
            this.biome = Skyworld.getInstance().getManagerHandler().getZoneManager().getMiningBiome(biomeId);
        }
        if (this.biome == null) {
            Skyworld.getInstance().getLogger().warning("Bioma de mineria no encontrado para zona: ");
        }
    }

    @Override
    public void tick() {
        // Nada que hacer aquí
    }

    public boolean contains(Block block) {
        return getBox().contains(block.getX(), block.getY(), block.getZ());
    }

    public MiningBiome getBiome(){
        return biome;
    }
}