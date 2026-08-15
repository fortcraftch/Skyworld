package Fortcraft.skyworld.items;

import org.bukkit.Material;
import java.util.List;
import java.util.Map;

public record CustomItemData(
        String id,
        Material material,
        String displayName,
        List<String> lore,
        String category, // WEAPON, TOOL, CONSUMABLE
        String rarity,
        Map<String, Double> stats, // TODAS LAS STATS VAN AQUÍ
        boolean isEquipment
) {}