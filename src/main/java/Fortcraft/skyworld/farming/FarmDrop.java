package Fortcraft.skyworld.farm;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class FarmDrop {
    private final Material cropBlock; // El cultivo (WHEAT, CARROTS...)
    private final Material itemDrop;  // Lo que recibes
    private final Material soilBlock; // El suelo necesario (FARMLAND)
    private final int weight;
    private final int regenTime;      // Segundos para reaparecer

    // Constructor y Getters...

    public static FarmDrop fromConfig(String key, ConfigurationSection config) {
        return new FarmDrop(
                Material.valueOf(key),
                Material.valueOf(config.getString("drop")),
                Material.valueOf(config.getString("soil", "FARMLAND")),
                config.getInt("weight", 10),
                config.getInt("regen_time", 5)
        );
    }
}
