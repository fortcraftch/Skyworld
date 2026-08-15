package Fortcraft.skyworld.items;

import org.bukkit.Material;
import java.util.List;
import java.util.Map;

public record CustomDropData(
        String id,
        Material material,
        String displayName,
        List<String> lore,
        String rarity,
        String category,
        Map<String, Double> stats, // Cambiado a stats genérico
        boolean isEquipment
) {}