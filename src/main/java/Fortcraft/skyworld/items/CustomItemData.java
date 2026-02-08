package Fortcraft.skyworld.items;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import java.util.List;
import java.util.Map;

public record CustomItemData(
        String id,
        Material material,
        String displayName,
        List<String> lore,
        String category, // WEAPON, TOOL, CONSUMABLE
        Map<Attribute, Double> stats,
        Map<String, Double> customStats
) {}