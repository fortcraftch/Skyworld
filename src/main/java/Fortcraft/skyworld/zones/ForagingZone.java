package Fortcraft.skyworld.zones;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.foraging.ForagingBiome;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.BoundingBox;


public class ForagingZone extends Zone {

    private ForagingBiome biome;

    public ForagingZone(String id, World world, BoundingBox box, ConfigurationSection config) {
        super(id, world, box);
        load(config);
    }

    private void load(ConfigurationSection config) {
        if (config == null) return;
        String biomeId = config.getString("biome");

        if (biomeId != null) {
            this.biome = Skyworld.getInstance().getManagerHandler().getZoneManager().getForagingBiome(biomeId);
        }

        if (this.biome == null) {
            Skyworld.getInstance().getLogger().warning("¡Atención! Bioma de foraging '" + biomeId + "' no encontrado para la zona: ");
        }
    }

    @Override
    public void tick() {

    }

    public boolean contains(Block block) {
        return getBox().contains(block.getX(), block.getY(), block.getZ());
    }

    public ForagingBiome getBiome(){
        return biome;
    }
}
