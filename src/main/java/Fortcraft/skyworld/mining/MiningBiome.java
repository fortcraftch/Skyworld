package Fortcraft.skyworld.mining;

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
    private final Set<String> uniqueSourceIds = new HashSet<>(); // Ahora guardará IDs

    public MiningBiome(String id, ConfigurationSection config) {
        this.id = id;
        this.displayName = config.getString("display_name", id);
        this.icon = Material.valueOf(config.getString("icon", "STONE_PICKAXE"));

        ConfigurationSection blocksSec = config.getConfigurationSection("blocks");
        if (blocksSec != null) {
            for (String blockKey : blocksSec.getKeys(false)) {
                Material sourceMat = Material.valueOf(blockKey.toUpperCase());
                ConfigurationSection blockSec = blocksSec.getConfigurationSection(blockKey);

                String sourceId = blockKey.toLowerCase(); // ID Único de la fuente
                String sourceName = blockSec.getString("name", blockKey);
                Material defTransform = Material.valueOf(blockSec.getString("transform-to", "BEDROCK"));
                int defRegen = blockSec.getInt("regen-time", 5);

                List<MiningDrop> blockDrops = new ArrayList<>();
                ConfigurationSection dropsListSec = blockSec.getConfigurationSection("drops");

                if (dropsListSec != null) {
                    for (String dropId : dropsListSec.getKeys(false)) {
                        ConfigurationSection singleDropSec = dropsListSec.getConfigurationSection(dropId);

                        String itemId = singleDropSec.getString("item_id", dropId);
                        double weight = singleDropSec.getDouble("weight", 10.0);
                        int amount = singleDropSec.getInt("amount", 1);
                        int customRegen = singleDropSec.getInt("regen-time", defRegen);

                        MiningDrop drop = new MiningDrop(
                                sourceMat,
                                sourceId,
                                sourceName,
                                itemId,
                                weight,
                                amount,
                                defTransform,
                                customRegen
                        );
                        blockDrops.add(drop);
                    }
                }

                if (!blockDrops.isEmpty()) {
                    drops.put(sourceMat, blockDrops);
                    uniqueSourceIds.add(sourceId); // Guardamos la ID estricta
                }
            }
        }
    }

    public MiningDrop getWeightedDrop(Material source) {
        List<MiningDrop> possibleDrops = drops.get(source);
        if (possibleDrops == null || possibleDrops.isEmpty()) return null;

        double totalWeight = possibleDrops.stream().mapToDouble(MiningDrop::getWeight).sum();
        double randomValue = ThreadLocalRandom.current().nextDouble() * totalWeight;

        double currentWeight = 0;
        for (MiningDrop drop : possibleDrops) {
            currentWeight += drop.getWeight();
            if (currentWeight >= randomValue) return drop;
        }
        return possibleDrops.getFirst();
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