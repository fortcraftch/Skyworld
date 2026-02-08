package Fortcraft.skyworld.zones;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.farming.FarmBiome;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.BoundingBox;

public class FarmZone extends Zone {

    private FarmBiome biome;

    public FarmZone(String id, World world, BoundingBox box, ConfigurationSection config) {
        super(id, world, box);
        load(config);
    }

    private void load(ConfigurationSection config) {
        if (config == null) return;
        String biomeId = config.getString("biome");

        if (biomeId != null) {
            this.biome = Skyworld.getInstance().getManagerHandler().getZoneManager().getFarmingBiome(biomeId);
        }

        if (this.biome == null) {
            Skyworld.getInstance().getLogger().warning("¡Atención! Bioma de granja '" + biomeId + "' no encontrado para la zona: ");
        }
    }

    @Override
    public void tick() {
        // No se requiere lógica por tick actualmente.
    }

    public boolean contains(Block block) {
        return getBox().contains(block.getX(), block.getY(), block.getZ());
    }

    public FarmBiome getBiome(){
        return biome;
    }
}