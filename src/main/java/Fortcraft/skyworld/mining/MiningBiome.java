package Fortcraft.skyworld.mining;

import Fortcraft.skyworld.foraging.ForagingDrop;
import Fortcraft.skyworld.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class MiningBiome {
    private final String id;
    private final String displayName;
    private final Material icon;

    private final Map<Material, List<MiningDrop>> drops = new HashMap<>();
    private final Set<String> uniqueSourceIds = new HashSet<>();

    public MiningBiome(String id, ConfigurationSection config) {
        this.id = id;
        this.displayName = config.getString("display_name", id);
        this.icon = Material.valueOf(config.getString("icon", "STONE_PICKAXE"));

        ConfigurationSection blocksSec = config.getConfigurationSection("blocks");
        if (blocksSec != null) {
            for (String blockKey : blocksSec.getKeys(false)) {
                Material sourceMat;
                try {
                    sourceMat = Material.valueOf(blockKey.toUpperCase());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                ConfigurationSection blockSec = blocksSec.getConfigurationSection(blockKey);
                if (blockSec == null) continue;

                String sourceId = blockKey.toLowerCase();
                String sourceName = blockSec.getString("name", blockKey);
                Material defTransform = Material.valueOf(blockSec.getString("transform-to", "BEDROCK"));
                int defRegen = blockSec.getInt("regen-time", 5);

                // NUEVOS ATRIBUTOS DEL BLOQUE
                double defRequiredPower = blockSec.getDouble("breaking_power", 1);
                double defHardness = blockSec.getDouble("hardness", 50.0);

                List<MiningDrop> blockDrops = new ArrayList<>();
                ConfigurationSection dropsListSec = blockSec.getConfigurationSection("drops");

                if (dropsListSec != null) {
                    for (String dropId : dropsListSec.getKeys(false)) {
                        ConfigurationSection singleDropSec = dropsListSec.getConfigurationSection(dropId);
                        if (singleDropSec == null) continue;

                        String itemId = singleDropSec.getString("item_id", dropId);
                        double weight = singleDropSec.getDouble("weight", 10.0);
                        int amount = singleDropSec.getInt("amount", 1);
                        int customRegen = singleDropSec.getInt("regen-time", defRegen);

                        // Permite sobreescribir atributos por drop si se desea, o usar los del bloque base
                        double requiredPower = singleDropSec.getDouble("required-power", defRequiredPower);
                        double hardness = singleDropSec.getDouble("hardness", defHardness);

                        MiningDrop drop = new MiningDrop(
                                sourceMat,
                                sourceId,
                                sourceName,
                                itemId,
                                weight,
                                amount,
                                defTransform,
                                customRegen,
                                requiredPower,
                                hardness
                        );
                        blockDrops.add(drop);
                    }
                }

                if (!blockDrops.isEmpty()) {
                    drops.put(sourceMat, blockDrops);
                    uniqueSourceIds.add(sourceId);
                }
            }
        }
    }

    public MiningDrop getWeightedDrop(Material source) {
        return getWeightedDrop(source, 0.0);
    }

    public MiningDrop getWeightedDrop(Material source, double playerLuck) {
        List<MiningDrop> possibleDrops = drops.get(source);
        if (possibleDrops == null || possibleDrops.isEmpty()) return null;

        if (possibleDrops.size() == 1 || playerLuck <= 0) {
            double totalWeight = possibleDrops.stream().mapToDouble(MiningDrop::getWeight).sum();
            double randomValue = ThreadLocalRandom.current().nextDouble() * totalWeight;

            double currentWeight = 0;
            for (MiningDrop drop : possibleDrops) {
                currentWeight += drop.getWeight();
                if (currentWeight >= randomValue) {
                    return drop;
                }
            }
            return possibleDrops.getFirst();
        }

        double highestWeight = 0;
        for (MiningDrop drop : possibleDrops) {
            if (drop.getWeight() > highestWeight) {
                highestWeight = drop.getWeight();
            }
        }

        double totalAdjustedWeight = 0.0;
        Map<MiningDrop, Double> adjustedWeights = new HashMap<>();

        for (MiningDrop drop : possibleDrops) {
            double currentWeight = drop.getWeight();
            if (currentWeight < highestWeight) {
                currentWeight = currentWeight * (1.0 + (playerLuck / 100.0));
            }
            adjustedWeights.put(drop, currentWeight);
            totalAdjustedWeight += currentWeight;
        }

        double randomValue = ThreadLocalRandom.current().nextDouble() * totalAdjustedWeight;
        double currentSum = 0;

        for (MiningDrop drop : possibleDrops) {
            currentSum += adjustedWeights.get(drop);
            if (currentSum >= randomValue) {
                return drop;
            }
        }

        return possibleDrops.getFirst();
    }

    // Método para obtener un drop de referencia o el primero configurado para ese material (útil para validar antes de minar)
    public MiningDrop getFirstDropFor(Material source) {
        List<MiningDrop> list = drops.get(source);
        if (list != null && !list.isEmpty()) {
            return list.getFirst();
        }
        return null;
    }

    public int getTotalUniqueSources() { return uniqueSourceIds.size(); }
    public Set<String> getUniqueSourceIds() { return uniqueSourceIds; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    public Collection<MiningDrop> getAllDrops() {
        return drops.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    public ItemStack getGuiIcon(int discoveredCount) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.format(getDisplayName()).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(ColorUtils.format("<gray>Descubiertos: <yellow>" + discoveredCount + "/" + getTotalUniqueSources()).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(ColorUtils.format("<yellow>Click para ver colección").decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}