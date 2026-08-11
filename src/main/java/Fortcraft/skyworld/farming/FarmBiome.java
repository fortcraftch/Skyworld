package Fortcraft.skyworld.farming;

import Fortcraft.skyworld.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FarmBiome {
    private final String id;
    private final String displayName;
    private final Material icon;

    // Guarda los drops mapeados por Material de cada bloque individual
    private final Map<Material, List<FarmDrop>> drops = new HashMap<>();
    private final Set<String> uniqueSourceIds = new HashSet<>();

    public FarmBiome(String id, ConfigurationSection config) {
        this.id = id;
        this.displayName = config.getString("display_name", id);
        this.icon = Material.valueOf(config.getString("icon", "GOLDEN_HOE"));

        ConfigurationSection cropsSec = config.getConfigurationSection("crops");
        if (cropsSec != null) {
            for (String cropKey : cropsSec.getKeys(false)) {
                ConfigurationSection cropSec = cropsSec.getConfigurationSection(cropKey);
                if (cropSec == null) continue;

                String sourceId = cropKey.toLowerCase(); // ID único ("cactus", "wheat", etc.)
                String sourceName = cropSec.getString("name", cropKey);
                int defRegen = cropSec.getInt("regen_time", 5);

                ConfigurationSection blocksSec = cropSec.getConfigurationSection("blocks");

                if (blocksSec != null) {
                    // Cultivo compuesto por múltiples bloques (Cactus)
                    for (String blockMatKey : blocksSec.getKeys(false)) {
                        Material sourceMat = Material.valueOf(blockMatKey.toUpperCase());
                        ConfigurationSection blockSec = blocksSec.getConfigurationSection(blockMatKey);
                        if (blockSec == null) continue;

                        List<FarmDrop> cropDrops = parseDrops(blockSec, sourceMat, sourceId, sourceName, defRegen);
                        if (!cropDrops.isEmpty()) {
                            drops.put(sourceMat, cropDrops);
                        }
                    }
                } else {
                    // Cultivo simple de un solo bloque (Trigo, Zanahoria, etc.)
                    Material sourceMat = Material.valueOf(cropKey.toUpperCase());
                    List<FarmDrop> cropDrops = parseDrops(cropSec, sourceMat, sourceId, sourceName, defRegen);
                    if (!cropDrops.isEmpty()) {
                        drops.put(sourceMat, cropDrops);
                    }
                }

                // Guardamos la ID del cultivo para el conteo global
                uniqueSourceIds.add(sourceId);
            }
        }
    }

    private List<FarmDrop> parseDrops(ConfigurationSection section, Material sourceMat, String sourceId, String sourceName, int defRegen) {
        List<FarmDrop> cropDrops = new ArrayList<>();
        ConfigurationSection dropsListSec = section.getConfigurationSection("drops");
        if (dropsListSec != null) {
            for (String dropId : dropsListSec.getKeys(false)) {
                ConfigurationSection singleDropSec = dropsListSec.getConfigurationSection(dropId);
                if (singleDropSec == null) continue;

                String itemId = singleDropSec.getString("item_id", dropId);
                double weight = singleDropSec.getDouble("weight", 10.0);
                int amount = singleDropSec.getInt("amount", 1);

                FarmDrop drop = new FarmDrop(
                        sourceMat,
                        sourceId,
                        sourceName,
                        itemId,
                        weight,
                        amount,
                        defRegen
                );
                cropDrops.add(drop);
            }
        }
        return cropDrops;
    }

    public FarmDrop getWeightedDrop(Material source) {
        List<FarmDrop> possibleDrops = drops.get(source);
        if (possibleDrops == null || possibleDrops.isEmpty()) return null;

        double totalWeight = possibleDrops.stream().mapToDouble(FarmDrop::getWeight).sum();
        double randomValue = ThreadLocalRandom.current().nextDouble() * totalWeight;

        double currentWeight = 0;
        for (FarmDrop drop : possibleDrops) {
            currentWeight += drop.getWeight();
            if (currentWeight >= randomValue) {
                return drop;
            }
        }
        return possibleDrops.getFirst();
    }

    public int getTotalUniqueSources() { return uniqueSourceIds.size(); }
    public Set<String> getUniqueSourceIds() { return uniqueSourceIds; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    /**
     * Devuelve todos los drops únicos por rareza/ítem del bioma.
     */
    public Collection<FarmDrop> getAllDrops() {
        Map<String, FarmDrop> uniqueDrops = new LinkedHashMap<>();
        for (List<FarmDrop> dropList : drops.values()) {
            for (FarmDrop drop : dropList) {
                String key = drop.getSourceId() + ":" + drop.itemId();
                uniqueDrops.putIfAbsent(key, drop);
            }
        }
        return uniqueDrops.values();
    }

    public ItemStack getGuiIcon(int discoveredCount) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(ColorUtils.format(getDisplayName())
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();

            lore.add(ColorUtils.format("<gray>Descubiertos: <yellow>" + discoveredCount + "/" + getTotalUniqueSources())
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());

            lore.add(ColorUtils.format("<yellow>Click para ver colección")
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }
}